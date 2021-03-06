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

package io.cdap.cdap.etl.spark.plugin;

import io.cdap.cdap.api.spark.JavaSparkExecutionContext;
import io.cdap.cdap.api.spark.JavaSparkMain;
import io.cdap.cdap.etl.common.plugin.Caller;

import java.util.concurrent.Callable;

/**
 * Wrapper around a {@link JavaSparkMain} that makes sure logging, classloading, and other pipeline capabilities
 * are setup correctly.
 */
public class WrappedJavaSparkMain implements JavaSparkMain {
  private final JavaSparkMain delegate;
  private final Caller caller;

  public WrappedJavaSparkMain(JavaSparkMain delegate, Caller caller) {
    this.delegate = delegate;
    this.caller = caller;
  }

  @Override
  public void run(final JavaSparkExecutionContext sec) throws Exception {
    caller.call(new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        delegate.run(sec);
        return null;
      }
    });
  }
}
