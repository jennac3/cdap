package com.continuuity.gateway.v2.handlers.v2;

import com.continuuity.common.http.core.HandlerContext;
import com.continuuity.common.http.core.HttpResponder;
import com.continuuity.gateway.auth.GatewayAuthenticator;
import com.continuuity.metadata.MetadataService;
import com.continuuity.metadata.Application;
import com.continuuity.metadata.Dataset;
import com.continuuity.metadata.Stream;
import com.continuuity.metadata.Flow;
import com.continuuity.metadata.Mapreduce;
import com.continuuity.metadata.Procedure;
import com.continuuity.metadata.thrift.Workflow;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.util.List;

/**
 *  {@link MetadataServiceHandler} is REST interface to MDS store.
 */
@Path("/v2")
public class MetadataServiceHandler extends AuthenticatedHttpHandler {
  private static final Logger LOG = LoggerFactory.getLogger(MetadataServiceHandler.class);
  private final MetadataService service;

  @Inject
  public MetadataServiceHandler(MetadataService service, GatewayAuthenticator authenticator) {
    super(authenticator);
    this.service = service;
  }

  @Override
  public void init(HandlerContext context) {
    LOG.info("Starting MetadataServiceHandler.");
  }

  @Override
  public void destroy(HandlerContext context) {
    LOG.info("Stopping MetadataServiceHandler.");
  }

  /**
   * Returns a list of streams associated with account.
   */
  @GET
  @Path("/streams")
  public void getStreams(HttpRequest request, HttpResponder responder) {
    try {
      String accountId = getAuthenticatedAccountId(request);
      List<Stream> streams = service.getStreams(accountId);
      JsonArray s = new JsonArray();
      for (Stream stream : streams) {
        JsonObject object = new JsonObject();
        object.addProperty("id", stream.getId());
        object.addProperty("name", stream.getName());
        object.addProperty("description", stream.getDescription());
        s.add(object);
      }
      responder.sendJson(HttpResponseStatus.OK, s);
    } catch (SecurityException e) {
      responder.sendString(HttpResponseStatus.FORBIDDEN, e.getMessage());
    } catch (IllegalArgumentException e) {
      responder.sendString(HttpResponseStatus.BAD_REQUEST, e.getMessage());
    } catch (Exception e) {
      responder.sendString(HttpResponseStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  /**
   * Returns a stream associated with account.
   */
  @GET
  @Path("/streams/{streamId}")
  public void getStreamSpecification(HttpRequest request, HttpResponder responder,
                                     @PathParam("streamId") final String streamId) {
    try {
      String accountId = getAuthenticatedAccountId(request);
      Stream stream = service.getStream(accountId, streamId);
      if (stream == null) {
        responder.sendStatus(HttpResponseStatus.NOT_FOUND);
        return;
      }
      JsonObject object = new JsonObject();
      object.addProperty("id", stream.getId());
      object.addProperty("name", stream.getName());
      object.addProperty("description", stream.getDescription());
      object.addProperty("capacityInBytes", stream.getCapacityInBytes());
      object.addProperty("expiryInSeconds", stream.getExpiryInSeconds());
      responder.sendJson(HttpResponseStatus.OK, object);
    } catch (SecurityException e) {
      responder.sendString(HttpResponseStatus.FORBIDDEN, e.getMessage());
    } catch (IllegalArgumentException e) {
      responder.sendString(HttpResponseStatus.BAD_REQUEST, e.getMessage());
    } catch (Exception e) {
      responder.sendString(HttpResponseStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  /**
   * Returns a list of streams associated with application.
   */
  @GET
  @Path("/apps/{app-id}/streams")
  public void getStreamsByApp(HttpRequest request, HttpResponder responder,
                              @PathParam("app-id") final String appId) {

    if (appId.isEmpty()) {
      responder.sendStatus(HttpResponseStatus.BAD_REQUEST);
      return;
    }

    try {
      String accountId = getAuthenticatedAccountId(request);
      List<Stream> streams = service.getStreamsByApplication(accountId, appId);
      JsonArray s = new JsonArray();
      for (Stream stream : streams) {
        JsonObject object = new JsonObject();
        object.addProperty("id", stream.getId());
        object.addProperty("name", stream.getName());
        object.addProperty("description", stream.getDescription());
        s.add(object);
      }
      responder.sendJson(HttpResponseStatus.OK, s);
    } catch (SecurityException e) {
      responder.sendString(HttpResponseStatus.FORBIDDEN, e.getMessage());
    } catch (IllegalArgumentException e) {
      responder.sendString(HttpResponseStatus.BAD_REQUEST, e.getMessage());
    } catch (Exception e) {
      responder.sendString(HttpResponseStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  /**
   * Returns a list of dataset associated with account.
   */
  @GET
  @Path("/datasets")
  public void getDatasets(HttpRequest request, HttpResponder responder) {
    try {
      String accountId = getAuthenticatedAccountId(request);
      List<Dataset> datasets = service.getDatasets(accountId);
      JsonArray s = new JsonArray();
      for (Dataset dataset : datasets) {
        JsonObject object = new JsonObject();
        object.addProperty("id", dataset.getId());
        object.addProperty("name", dataset.getName());
        object.addProperty("description", dataset.getDescription());
        object.addProperty("classname", dataset.getType());

        s.add(object);
      }
      responder.sendJson(HttpResponseStatus.OK, s);
    } catch (SecurityException e) {
      responder.sendString(HttpResponseStatus.FORBIDDEN, e.getMessage());
    } catch (IllegalArgumentException e) {
      responder.sendString(HttpResponseStatus.BAD_REQUEST, e.getMessage());
    } catch (Exception e) {
      responder.sendString(HttpResponseStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  /**
   * Returns a dataset associated with account.
   */
  @GET
  @Path("/datasets/{datasetId}")
  public void getDatasetSpecification(HttpRequest request, HttpResponder responder,
                                      @PathParam("datasetId") final String datasetId) {

    if (datasetId.isEmpty()) {
      responder.sendStatus(HttpResponseStatus.BAD_REQUEST);
      return;
    }

    try {
      String accountId = getAuthenticatedAccountId(request);

      Dataset dataset = service.getDataset(accountId, datasetId);
      if (dataset == null) {
        responder.sendStatus(HttpResponseStatus.NOT_FOUND);
        return;
      }
      JsonObject object = new JsonObject();
      object.addProperty("id", dataset.getId());
      object.addProperty("name", dataset.getName());
      object.addProperty("description", dataset.getDescription());
      object.addProperty("type", dataset.getType());
      object.addProperty("specification", dataset.getSpecification());
      responder.sendJson(HttpResponseStatus.OK, object);
    } catch (SecurityException e) {
      responder.sendString(HttpResponseStatus.FORBIDDEN, e.getMessage());
    } catch (IllegalArgumentException e) {
      responder.sendString(HttpResponseStatus.BAD_REQUEST, e.getMessage());
    } catch (Exception e) {
      responder.sendString(HttpResponseStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  /**
   * Returns a list of dataset associated with application.
   */
  @GET
  @Path("/apps/{app-id}/datasets")
  public void getDatasetsByApp(HttpRequest request, HttpResponder responder,
                               @PathParam("app-id") final String appId) {
    if (appId.isEmpty()) {
      responder.sendStatus(HttpResponseStatus.BAD_REQUEST);
      return;
    }

    try {
      String accountId = getAuthenticatedAccountId(request);
      List<Dataset> datasets = service.getDatasetsByApplication(accountId, appId);
      JsonArray s = new JsonArray();
      for (Dataset dataset : datasets) {
        JsonObject object = new JsonObject();
        object.addProperty("id", dataset.getId());
        object.addProperty("name", dataset.getName());
        object.addProperty("description", dataset.getDescription());
        object.addProperty("classname", dataset.getType());
        s.add(object);
      }
      responder.sendJson(HttpResponseStatus.OK, s);
    } catch (SecurityException e) {
      responder.sendString(HttpResponseStatus.FORBIDDEN, e.getMessage());
    } catch (IllegalArgumentException e) {
      responder.sendString(HttpResponseStatus.BAD_REQUEST, e.getMessage());
    } catch (Exception e) {
      responder.sendString(HttpResponseStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  /**
   * Returns a list of procedure associated with account.
   */
  @GET
  @Path("/procedures")
  public void getProcedures(HttpRequest request, HttpResponder responder) {
    try {
      String accountId = getAuthenticatedAccountId(request);
      List<Procedure> procedures = service.getProcedures(accountId);
      JsonArray s = new JsonArray();
      for (Procedure procedure : procedures) {
        JsonObject object = new JsonObject();
        object.addProperty("id", procedure.getId());
        object.addProperty("name", procedure.getName());
        object.addProperty("description", procedure.getDescription());
        object.addProperty("app", procedure.getApplication());
        s.add(object);
      }
      responder.sendJson(HttpResponseStatus.OK, s);
    } catch (SecurityException e) {
      responder.sendString(HttpResponseStatus.FORBIDDEN, e.getMessage());
    } catch (IllegalArgumentException e) {
      responder.sendString(HttpResponseStatus.BAD_REQUEST, e.getMessage());
    } catch (Exception e) {
      responder.sendString(HttpResponseStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  /**
   * Returns a procedure specification.
   */
  @GET
  @Path("/procedures/{procedure-id}")
  public void getProcedureSpecification(HttpRequest request, HttpResponder responder,
                                        @PathParam("procedure-id") final String procedureId) {
    try {
      String accountId = getAuthenticatedAccountId(request);
    } catch (SecurityException e) {
      responder.sendString(HttpResponseStatus.FORBIDDEN, e.getMessage());
    } catch (IllegalArgumentException e) {
      responder.sendString(HttpResponseStatus.BAD_REQUEST, e.getMessage());
    } catch (Exception e) {
      responder.sendString(HttpResponseStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  /**
   * Returns a list of procedure associated with account & application.
   */
  @GET
  @Path("/apps/{app-id}/procedures")
  public void getProceduresByApp(HttpRequest request, HttpResponder responder,
                              @PathParam("app-id") final String appId) {
    if (appId.isEmpty()) {
      responder.sendStatus(HttpResponseStatus.BAD_REQUEST);
      return;
    }

    try {
      String accountId = getAuthenticatedAccountId(request);
      List<Procedure> procedures = service.getProceduresByApplication(accountId, appId);
      if (procedures.size() < 1) {
        responder.sendJson(HttpResponseStatus.OK, new JsonArray());
        return;
      }
      JsonArray s = new JsonArray();
      for (Procedure procedure : procedures) {
        JsonObject object = new JsonObject();
        object.addProperty("id", procedure.getId());
        object.addProperty("name", procedure.getName());
        object.addProperty("description", procedure.getDescription());
        object.addProperty("app", procedure.getApplication());
        s.add(object);
      }
      responder.sendJson(HttpResponseStatus.OK, s);
    } catch (SecurityException e) {
      responder.sendString(HttpResponseStatus.FORBIDDEN, e.getMessage());
    } catch (IllegalArgumentException e) {
      responder.sendString(HttpResponseStatus.BAD_REQUEST, e.getMessage());
    } catch (Exception e) {
      responder.sendString(HttpResponseStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  /**
   * Returns a list of mapreduce jobs associated with account.
   */
  @GET
  @Path("/mapreduces")
  public void getMapReduces(HttpRequest request, HttpResponder responder) {
    try {
      String accountId = getAuthenticatedAccountId(request);
      List<Mapreduce> mapreduces = service.getMapreduces(accountId);
      JsonArray s = new JsonArray();
      for (Mapreduce mapreduce : mapreduces) {
        JsonObject object = new JsonObject();
        object.addProperty("id", mapreduce.getId());
        object.addProperty("name", mapreduce.getName());
        object.addProperty("description", mapreduce.getDescription());
        object.addProperty("app", mapreduce.getApplication());
        s.add(object);
      }
      responder.sendJson(HttpResponseStatus.OK, s);
    } catch (SecurityException e) {
      responder.sendString(HttpResponseStatus.FORBIDDEN, e.getMessage());
    } catch (IllegalArgumentException e) {
      responder.sendString(HttpResponseStatus.BAD_REQUEST, e.getMessage());
    } catch (Exception e) {
      responder.sendString(HttpResponseStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  /**
   * Returns a mapreduce specification.
   */
  @GET
  @Path("/mapreduces/{mapreduce-id}")
  public void getMapReduceSpecification(HttpRequest request, HttpResponder responder,
                                        @PathParam("mapreduce-id") final String mapreduceId) {
    try {
      String accountId = getAuthenticatedAccountId(request);
    } catch (SecurityException e) {
      responder.sendString(HttpResponseStatus.FORBIDDEN, e.getMessage());
    } catch (IllegalArgumentException e) {
      responder.sendString(HttpResponseStatus.BAD_REQUEST, e.getMessage());
    } catch (Exception e) {
      responder.sendString(HttpResponseStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  /**
   * Returns a list of mapreduce jobs associated with account & application.
   */
  @GET
  @Path("/apps/{app-id}/mapreduces")
  public void getMapReducesByApp(HttpRequest request, HttpResponder responder,
                                 @PathParam("app-id") final String appId) {

    if (appId.isEmpty()) {
      responder.sendStatus(HttpResponseStatus.BAD_REQUEST);
      return;
    }

    try {
      String accountId = getAuthenticatedAccountId(request);
      List<Mapreduce> mapreduces = service.getMapreducesByApplication(accountId, appId);
      JsonArray s = new JsonArray();
      for (Mapreduce mapreduce : mapreduces) {
        JsonObject object = new JsonObject();
        object.addProperty("id", mapreduce.getId());
        object.addProperty("name", mapreduce.getName());
        object.addProperty("description", mapreduce.getDescription());
        object.addProperty("app", mapreduce.getApplication());
        s.add(object);
      }
      responder.sendJson(HttpResponseStatus.OK, s);
    } catch (SecurityException e) {
      responder.sendString(HttpResponseStatus.FORBIDDEN, e.getMessage());
    } catch (IllegalArgumentException e) {
      responder.sendString(HttpResponseStatus.BAD_REQUEST, e.getMessage());
    } catch (Exception e) {
      responder.sendString(HttpResponseStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  /**
   * Returns a list of applications associated with account.
   */
  @GET
  @Path("/apps")
  public void getApps(HttpRequest request, HttpResponder responder) {
    try {
      String accountId = getAuthenticatedAccountId(request);

      List<Application> apps = service.getApplications(accountId);
      JsonArray s = new JsonArray();
      for (Application app : apps) {
        JsonObject object = new JsonObject();
        object.addProperty("id", app.getId());
        object.addProperty("name", app.getName());
        object.addProperty("description", app.getDescription());
        s.add(object);
      }
      responder.sendJson(HttpResponseStatus.OK, s);
    } catch (SecurityException e) {
      responder.sendString(HttpResponseStatus.FORBIDDEN, e.getMessage());
    } catch (IllegalArgumentException e) {
      responder.sendString(HttpResponseStatus.BAD_REQUEST, e.getMessage());
    } catch (Exception e) {
      responder.sendString(HttpResponseStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  /**
   * Returns a list of applications associated with account.
   */
  @GET
  @Path("/apps/{app-id}")
  public void getApps(HttpRequest request, HttpResponder responder,
                      @PathParam("app-id") final String appId) {

    if (appId.isEmpty()) {
      responder.sendStatus(HttpResponseStatus.BAD_REQUEST);
      return;
    }

    try {
      String accountId = getAuthenticatedAccountId(request);

      Application app = service.getApplication(accountId, appId);
      if (app == null) {
        responder.sendStatus(HttpResponseStatus.NOT_FOUND);
        return;
      }

      JsonObject object = new JsonObject();
      object.addProperty("id", app.getId());
      object.addProperty("name", app.getName());
      object.addProperty("description", app.getDescription());
      responder.sendJson(HttpResponseStatus.OK, object);
    } catch (SecurityException e) {
      responder.sendString(HttpResponseStatus.FORBIDDEN, e.getMessage());
    } catch (IllegalArgumentException e) {
      responder.sendString(HttpResponseStatus.BAD_REQUEST, e.getMessage());
    } catch (Exception e) {
      responder.sendString(HttpResponseStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  /**
   * Returns a list of flows associated with account.
   */
  @GET
  @Path("/flows")
  public void getFlows(HttpRequest request, HttpResponder responder) {
    try {
      String accountId = getAuthenticatedAccountId(request);

      List<Flow> flows = service.getFlows(accountId);
      JsonArray s = new JsonArray();
      for (Flow flow : flows) {
        JsonObject object = new JsonObject();
        object.addProperty("id", flow.getId());
        object.addProperty("name", flow.getName());
        object.addProperty("app", flow.getApplication());
        s.add(object);
      }
      responder.sendJson(HttpResponseStatus.OK, s);
    } catch (SecurityException e) {
      responder.sendString(HttpResponseStatus.FORBIDDEN, e.getMessage());
    } catch (IllegalArgumentException e) {
      responder.sendString(HttpResponseStatus.BAD_REQUEST, e.getMessage());
    } catch (Exception e) {
      responder.sendString(HttpResponseStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  /**
   * Returns a flow specification.
   */
  @GET
  @Path("/flows/{flow-id}")
  public void getFlowSpecification(HttpRequest request, HttpResponder responder,
                                   @PathParam("flow-id") final String flowId) {
    try {
      String accountId = getAuthenticatedAccountId(request);
    } catch (SecurityException e) {
      responder.sendString(HttpResponseStatus.FORBIDDEN, e.getMessage());
    } catch (IllegalArgumentException e) {
      responder.sendString(HttpResponseStatus.BAD_REQUEST, e.getMessage());
    } catch (Exception e) {
      responder.sendString(HttpResponseStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  /**
   * Returns a list of flow associated with account & application.
   */
  @GET
  @Path("/apps/{app-id}/flows")
  public void getFlowsByApp(HttpRequest request, HttpResponder responder,
                            @PathParam("app-id") final String appId) {

    if (appId.isEmpty()) {
      responder.sendStatus(HttpResponseStatus.BAD_REQUEST);
      return;
    }

    try {
      String accountId = getAuthenticatedAccountId(request);
      List<Flow> flows = service.getFlowsByApplication(accountId, appId);
      if (flows.size() < 1) {
        responder.sendJson(HttpResponseStatus.OK, new JsonArray());
        return;
      }
      JsonArray s = new JsonArray();
      for (Flow flow : flows) {
        JsonObject object = new JsonObject();
        object.addProperty("id", flow.getId());
        object.addProperty("name", flow.getName());
        object.addProperty("app", flow.getApplication());
        s.add(object);
      }
      responder.sendJson(HttpResponseStatus.OK, s);
    } catch (SecurityException e) {
      responder.sendString(HttpResponseStatus.FORBIDDEN, e.getMessage());
    } catch (IllegalArgumentException e) {
      responder.sendString(HttpResponseStatus.BAD_REQUEST, e.getMessage());
    } catch (Exception e) {
      responder.sendString(HttpResponseStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  /**
   * Returns all flows associated with a stream.
   */
  @GET
  @Path("/streams/{stream-id}/flows")
  public void getFlowsByStream(HttpRequest request, HttpResponder responder,
                               @PathParam("stream-id") final String streamId) {
    if (streamId.isEmpty()) {
      responder.sendStatus(HttpResponseStatus.BAD_REQUEST);
      return;
    }

    try {
      String accountId = getAuthenticatedAccountId(request);
      List<Flow> flows = service.getFlowsByStream(accountId, streamId);

      JsonArray s = new JsonArray();
      for (Flow flow : flows) {
        JsonObject object = new JsonObject();
        object.addProperty("id", flow.getId());
        object.addProperty("name", flow.getName());
        object.addProperty("app", flow.getApplication());
        s.add(object);
      }
      responder.sendJson(HttpResponseStatus.OK, s);
    } catch (SecurityException e) {
      responder.sendString(HttpResponseStatus.FORBIDDEN, e.getMessage());
    } catch (IllegalArgumentException e) {
      responder.sendString(HttpResponseStatus.BAD_REQUEST, e.getMessage());
    } catch (Exception e) {
      responder.sendString(HttpResponseStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  /**
   * Returns all flows associated with a dataset.
   */
  @GET
  @Path("/datasets/{dataset-id}/flows")
  public void getFlowsByDataset(HttpRequest request, HttpResponder responder,
                                   @PathParam("dataset-id") final String datasetId) {
    if (datasetId.isEmpty()) {
      responder.sendStatus(HttpResponseStatus.BAD_REQUEST);
      return;
    }

    try {
      String accountId = getAuthenticatedAccountId(request);
      List<Flow> flows = service.getFlowsByDataset(accountId, datasetId);
      JsonArray s = new JsonArray();
      for (Flow flow : flows) {
        JsonObject object = new JsonObject();
        object.addProperty("id", flow.getId());
        object.addProperty("name", flow.getName());
        object.addProperty("app", flow.getApplication());
        s.add(object);
      }
      responder.sendJson(HttpResponseStatus.OK, s);
    } catch (SecurityException e) {
      responder.sendString(HttpResponseStatus.FORBIDDEN, e.getMessage());
    } catch (IllegalArgumentException e) {
      responder.sendString(HttpResponseStatus.BAD_REQUEST, e.getMessage());
    } catch (Exception e) {
      responder.sendString(HttpResponseStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  /**
   * Returns a list of workflows associated with account.
   */
  @GET
  @Path("/workflows")
  public void getWorkflows(HttpRequest request, HttpResponder responder) {
    try {
      String accountId = getAuthenticatedAccountId(request);
      List<Workflow> workflows = service.getWorkflows(accountId);
      JsonArray s = new JsonArray();
      for (Workflow workflow : workflows) {
        JsonObject object = new JsonObject();
        object.addProperty("id", workflow.getId());
        object.addProperty("name", workflow.getName());
        object.addProperty("app", workflow.getApplication());
        s.add(object);
      }
      responder.sendJson(HttpResponseStatus.OK, s);
    } catch (SecurityException e) {
      responder.sendString(HttpResponseStatus.FORBIDDEN, e.getMessage());
    } catch (IllegalArgumentException e) {
      responder.sendString(HttpResponseStatus.BAD_REQUEST, e.getMessage());
    } catch (Exception e) {
      responder.sendString(HttpResponseStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  /**
   * Returns a list of workflow associated with account & application.
   */
  @GET
  @Path("/apps/{app-id}/workflows")
  public void getWorkflowsByApp(HttpRequest request, HttpResponder responder,
                            @PathParam("app-id") final String appId) {

    if (appId.isEmpty()) {
      responder.sendStatus(HttpResponseStatus.BAD_REQUEST);
      return;
    }

    try {
      String accountId = getAuthenticatedAccountId(request);
      List<Workflow> workflows = service.getWorkflowsByApplication(accountId, appId);
      JsonArray s = new JsonArray();
      for (Workflow workflow : workflows) {
        JsonObject object = new JsonObject();
        object.addProperty("id", workflow.getId());
        object.addProperty("name", workflow.getName());
        object.addProperty("app", workflow.getApplication());
        s.add(object);
      }
      responder.sendJson(HttpResponseStatus.OK, s);
    } catch (SecurityException e) {
      responder.sendString(HttpResponseStatus.FORBIDDEN, e.getMessage());
    } catch (IllegalArgumentException e) {
      responder.sendString(HttpResponseStatus.BAD_REQUEST, e.getMessage());
    } catch (Exception e) {
      responder.sendString(HttpResponseStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }
}

