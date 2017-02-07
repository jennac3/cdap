/*
 * Copyright © 2017 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package co.cask.cdap.logging.framework;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusChecker;
import co.cask.cdap.api.common.Bytes;
import co.cask.cdap.common.conf.CConfiguration;
import co.cask.cdap.common.conf.Constants;
import co.cask.cdap.common.utils.DirUtils;
import co.cask.cdap.logging.pipeline.LogPipelineConfigurator;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * This class is responsible for loading configurations of log processing pipelines.
 */
public class LogPipelineLoader {

  private static final Logger LOG = LoggerFactory.getLogger(LogPipelineLoader.class);
  private static final String CDAP_LOG_PIPELINE_CONFIG = "cdap-log-pipeline.xml";
  private static final String CDAP_LOG_PIPELINE_NAME = "cdap";

  private final CConfiguration cConf;

  public LogPipelineLoader(CConfiguration cConf) {
    this.cConf = cConf;
  }

  /**
   * Validates the log pipeline configurations.
   *
   * @throws InvalidPipelineException if any of the pipeline configuration is invalid.
   */
  public void validate() throws InvalidPipelineException {
    Provider<LoggerContext> contextProvider = new Provider<LoggerContext>() {
      @Override
      public LoggerContext get() {
        return new LoggerContext();
      }
    };
    load(contextProvider, false);
  }

  /**
   * Loads the log pipeline configurations.
   *
   * @param contextProvider a guice {@link Provider} for creating new instance of {@link LoggerContext}.
   * @param <T> Type of the {@link LoggerContext}
   * @return a map from pipeline name to the {@link LogPipelineSpecification}
   */
  public <T extends LoggerContext> Map<String, LogPipelineSpecification<T>> load(Provider<T> contextProvider) {
    try {
      return load(contextProvider, true);
    } catch (InvalidPipelineException e) {
      // Since we called with ignoreOnError, the method should never throw this exception, hence should never reach here
      LOG.warn("Invalid pipeline configuration", e);
      return Collections.emptyMap();
    }
  }

  /**
   * Loads the log pipeline configurations
   *
   * @param contextProvider a guice {@link Provider} for creating new instance of {@link LoggerContext}.
   * @param ignoreOnError {@code true} to ignore pipeline configuration that has error.
   * @param <T> Type of the {@link LoggerContext}
   * @return a map from pipeline name to the {@link LogPipelineSpecification}
   * @throws InvalidPipelineException if any of the pipeline configuration is invalid.
   */
  private <T extends LoggerContext> Map<String, LogPipelineSpecification<T>> load(
    Provider<T> contextProvider, boolean ignoreOnError) throws InvalidPipelineException {

    Map<String, LogPipelineSpecification<T>> result = new HashMap<>();

    for (URL configURL : getPipelineConfigURLs()) {
      try {
        LogPipelineSpecification<T> spec = load(contextProvider, configURL);
        LogPipelineSpecification<T> existingSpec = result.get(spec.getName());
        if (existingSpec != null) {
          if (!ignoreOnError) {
            throw new InvalidPipelineException("Duplicate pipeline with name " + spec.getName() + " at " + configURL +
                                                 ". It was already defined at " + existingSpec.getSource());
          }
          LOG.warn("Pipeline {} already defined in {}. Ignore the duplicated one from {}",
                   spec.getName(), existingSpec.getSource(), configURL);
          continue;
        }
        result.put(spec.getName(), spec);
      } catch (JoranException e) {
        if (!ignoreOnError) {
          throw new InvalidPipelineException("Failed to parse log processing pipeline config at " + configURL, e);
        }
        LOG.warn("Ignore invalid log processing pipeline configuration in {} due to\n  {}", configURL, e.getMessage());
      }
    }

    Preconditions.checkState(result.containsKey(CDAP_LOG_PIPELINE_NAME),
                             "The CDAP system log processing pipeline is missing. " +
                               "Please check and fix any configuration error shown earlier in the log.");
    return result;
  }

  /**
   * Returns an {@link Iterable} of {@link URL} of pipeline configuration files.
   */
  private Iterable<URL> getPipelineConfigURLs() {
    URL systemPipeline = getClass().getClassLoader().getResource(CDAP_LOG_PIPELINE_CONFIG);
    // This shouldn't happen since the cdap pipeline is packaged in the jar.
    Preconditions.checkState(systemPipeline != null, "Missing cdap system pipeline configuration");

    List<File> files = DirUtils.listFiles(new File(cConf.get(Constants.Logging.PIPELINE_CONFIG_DIR)), "xml");
    return Iterables.concat(Collections.singleton(systemPipeline),
                            Iterables.filter(Iterables.transform(files, new Function<File, URL>() {
      @Nullable
      @Override
      public URL apply(File file) {
        try {
          return file.toURI().toURL();
        } catch (MalformedURLException e) {
          // This shouldn't happen
          LOG.warn("Ignore log pipeline config file {} due to {}", file, e.getMessage());
          return null;
        }
      }
    }), Predicates.notNull()));
  }

  /**
   * Creates the {@link LogPipelineSpecification} representing the given log pipeline configuration URL.
   */
  private <T extends LoggerContext> LogPipelineSpecification<T> load(Provider<T> contextProvider,
                                                                     URL configURL) throws JoranException {
    // Create one AppenderContext per config file
    T context = contextProvider.get();

    CConfiguration pipelineCConf = CConfiguration.copy(cConf);
    LogPipelineConfigurator configurator = new LogPipelineConfigurator(pipelineCConf);
    configurator.setContext(context);

    configurator.doConfigure(configURL);

    // Check if the configuration has any error in it.
    if (!new StatusChecker(context).isErrorFree(Status.ERROR)) {
      List<Status> errors = new ArrayList<>();
      for (Status status : context.getStatusManager().getCopyOfStatusList()) {
        if (status.getEffectiveLevel() == Status.ERROR) {
          errors.add(status);
        }
      }
      throw new JoranException("Configuration failed " + errors);
    }

    // Default the pipeline name to the config file name if it is not set
    if (context.getName() == null) {
      String path = configURL.getPath();
      int idx = path.lastIndexOf("/");
      context.setName(idx < 0 ? path : path.substring(idx + 1));
    }

    byte[] checkpointPrefix = Bytes.toBytes(context.getName());
    String prefixNum = configurator.getExecutionContext().getProperty(Constants.Logging.PIPELINE_CHECKPOINT_PREFIX_NUM);
    if (prefixNum != null) {
      try {
        checkpointPrefix = Bytes.toBytes(Integer.parseInt(prefixNum));
      } catch (NumberFormatException e) {
        LOG.warn("Ignore invalid {} setting for pipeline in {}",
                 Constants.Logging.PIPELINE_CHECKPOINT_PREFIX_NUM, configURL);
      }
    }

    return new LogPipelineSpecification<>(configURL, context,
                                          setupConfig(configurator, pipelineCConf), checkpointPrefix);
  }

  /**
   * Copies overridable configurations from the pipeline configuration into the given {@link CConfiguration}.
   */
  private CConfiguration setupConfig(JoranConfigurator configurator, CConfiguration cConf) {
    Context context = configurator.getContext();

    // The list of properties that can be overridden per pipeline configuration
    Set<String> keys = ImmutableSet.of(
      Constants.Logging.PIPELINE_BUFFER_SIZE,
      Constants.Logging.PIPELINE_EVENT_DELAY_MS,
      Constants.Logging.PIPELINE_KAFKA_FETCH_SIZE,
      Constants.Logging.PIPELINE_CHECKPOINT_INTERVAL_MS,
      Constants.Logging.PIPELINE_LOGGER_CACHE_SIZE,
      Constants.Logging.PIPELINE_LOGGER_CACHE_EXPIRATION_MS
    );

    for (String key : keys) {
      context.putProperty(key, cConf.get(key));
      cConf.set(key, configurator.getExecutionContext().subst("${" + key + "}"));
    }

    return cConf;
  }
}
