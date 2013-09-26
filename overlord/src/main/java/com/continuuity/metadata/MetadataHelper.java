package com.continuuity.metadata;

import com.continuuity.data.metadata.MetaDataEntry;
import com.continuuity.metadata.thrift.MetadataServiceException;
import com.continuuity.metadata.thrift.Workflow;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Supporting class for managing MDS service.
 */
public class MetadataHelper {

  //-------------- Some utilities for list/string conversion -----------------

  static String listToString(List<String> list) {
    StringBuilder str = new StringBuilder();
    if (list != null) {
      for (String item : list) {
        str.append(item);
        str.append(' ');
      }
    }
    return str.toString();
  }

  static List<String> stringToList(String str) {
    if (str == null || str.isEmpty()) {
      return Collections.emptyList();
    }
    StringTokenizer tok = new StringTokenizer(str, " ");
    List<String> list = Lists.newArrayList();
    while (tok.hasMoreTokens()) {
      list.add(tok.nextToken());
    }
    return list;
  }

  //-------------- Some utilities to compare meta data -----------------------

  enum CompareStatus {
    EQUAL, DIFF, SUPER, SUB
  }

  // returns SUPER if the new value has more information than the existing one.
  static CompareStatus compareAlso(CompareStatus soFar,
                                   String newValue,
                                   String existingValue) {
    if (soFar.equals(CompareStatus.DIFF)) {
      return soFar;
    }

    if (newValue == null) {
      // both null, no change in status
      if (existingValue == null) {
        return soFar;
      }

      // new value has less info: incompatible if it had more info so far
      if (soFar.equals(CompareStatus.SUPER)) {
        return CompareStatus.DIFF;
      }

      // new value has less info and it did not have more so far -> sub
      return CompareStatus.SUB;
    } else { // new != null
      // both are the same, no change in status
      if (newValue.equals(existingValue)) {
        return soFar;
      }

      // both non-null but different
      if (existingValue != null) {
        return CompareStatus.DIFF;
      }

      // new value has more info: incompatible if it had less info so far
      if (soFar.equals(CompareStatus.SUB)) {
        return CompareStatus.DIFF;
      }
      // new value has more info and it did not have less so far -> super
      return CompareStatus.SUPER;
    }
  }

  // returns SUPER if the new value has more information than the existing one.
  static CompareStatus compareAlso(CompareStatus soFar,
                                   List<String> newValue,
                                   List<String> existingValue) {
    if (soFar.equals(CompareStatus.DIFF)) {
      return soFar;
    }

    if (newValue == null) {
      // both null, no change in status
      if (existingValue == null) {
        return soFar;
      }
      // new value has less info: incompatible if it had more info so far
      if (soFar.equals(CompareStatus.SUPER)) {
        return CompareStatus.DIFF;
      }
      // new value has less info and it did not have more so far -> sub
      return CompareStatus.SUB;
    } else { // new != null
      // both are the same, no change in status
      if (newValue.equals(existingValue)) {
        return soFar;
      }
      // both non-null but different
      if (existingValue != null) {
        return CompareStatus.DIFF;
      }
      // new value has more info: incompatible if it had less info so far
      if (soFar.equals(CompareStatus.SUB)) {
        return CompareStatus.DIFF;
      }

      // new value has more info and it did not have less so far -> super
      return CompareStatus.SUPER;
    }
  }

  // returns SUPER if the new value has more information than the existing one.
  static CompareStatus compareAlso(CompareStatus soFar, Long newValue, Long existingValue) {
    if (soFar.equals(CompareStatus.DIFF)) {
      return soFar;
    }

    if (newValue == null) {
      // both null, no change in status
      if (existingValue == null) {
        return soFar;
      }

      // new value has less info: incompatible if it had more info so far
      if (soFar.equals(CompareStatus.SUPER)) {
        return CompareStatus.DIFF;
      }
      // new value has less info and it did not have more so far -> sub
      return CompareStatus.SUB;
    } else { // new != null
      // both are the same, no change in status
      if (newValue.equals(existingValue)) {
        return soFar;
      }
      // both non-null but different
      if (existingValue != null) {
        return CompareStatus.DIFF;
      }
      // new value has more info: incompatible if it had less info so far
      if (soFar.equals(CompareStatus.SUB)) {
        return CompareStatus.DIFF;
      }
      // new value has more info and it did not have less so far -> super
      return CompareStatus.SUPER;
    }
  }

  //-------------------------- Account stuff ----------------------------------

  /**
   * Validates the account passed.
   *
   * @param account to be validated.
   * @throws MetadataServiceException thrown if account is null or empty.
   */
  void validateAccount(String account)
      throws MetadataServiceException {
    if (account == null || account.isEmpty()) {
      throw new MetadataServiceException("Account Id cannot be null or empty");
    }
  }

  //-------------------------- Generic stuff ----------------------------------

  /**
   * Generic class to manage meta data objects. It helps the meta data service
   * generic methods to deal with the type specific conversion, comparison etc.
   */
  interface Helper<T> {

    /** validate the completeness of a meta object to be written. */
    public void validate(T t) throws MetadataServiceException;

    /** convert a raw mds entry into a meta object of the specific type. */
    public MetaDataEntry makeEntry(String account, T t);

    /** convert a meta object into a raw mds entry. */
    public T makeFromEntry(MetaDataEntry entry);

    /** return an empty meta object with exists=false. */
    public T makeNonExisting(String app, String id);

    /** compare a meta object with an existing raw meta entry. */
    public CompareStatus compare(T t, MetaDataEntry existingEntry);

    /** get the id of a meta object. */
    public String getId(T t);

    /** get the application of a meta object. May return null. */
    public String getApplication(T t);

    /** get the name for this type of objects, e.g., "stream". */
    public String getName();

    /** get the type to use for the raw mds objects. */
    public String getFieldType();
  }

  // static helpers, one for each meta data type
  static Helper<Stream> streamHelper = new StreamHelper();
  static Helper<Dataset> datasetHelper = new DatasetHelper();
  static Helper<Application> applicationHelper = new ApplicationHelper();
  static Helper<Procedure> procedureHelper = new ProcedureHelper();
  static Helper<Flow> flowHelper = new FlowHelper();
  static Helper<Mapreduce> mapreduceHelper = new MapreduceHelper();
  static Helper<Workflow> workflowHelper = new WorkflowHelper();

  //-------------------------- Stream stuff ----------------------------------

  static class StreamHelper implements Helper<Stream> {

    @Override
    public void validate(Stream stream) throws MetadataServiceException {
      // When creating a stream, you need to have id, name and description
      if (stream.getId() == null || stream.getId().isEmpty()) {
        throw new MetadataServiceException("Stream id is empty or null.");
      }
      if (stream.getName() == null || stream.getName().isEmpty()) {
        throw new MetadataServiceException(
            "Stream name must not be null or empty");
      }
    }

    @Override
    public MetaDataEntry makeEntry(String account, Stream stream) {
      MetaDataEntry entry = new MetaDataEntry(
          account, null, FieldTypes.Stream.ID, stream.getId());
      if (stream.getName() != null) {
        entry.addField(FieldTypes.Stream.NAME, stream.getName());
      }

      if (stream.getDescription() != null) {
        entry.addField(FieldTypes.Stream.DESCRIPTION, stream.getDescription());
      }

      if (stream.getCapacityInBytes() != null) {
        entry.addField(FieldTypes.Stream.CAPACITY_IN_BYTES,
            String.format("%d", stream.getCapacityInBytes()));
      }

      if (stream.getExpiryInSeconds() != null) {
        entry.addField(FieldTypes.Stream.EXPIRY_IN_SECONDS,
            String.format("%d", stream.getExpiryInSeconds()));
      }

      return entry;
    }

    @Override
    public Stream makeFromEntry(MetaDataEntry entry) {
      Stream stream = new Stream(entry.getId());
      String name = entry.getTextField(FieldTypes.Stream.NAME);
      if (name != null) {
        stream.setName(name);
      }

      String description = entry.getTextField(FieldTypes.Stream.DESCRIPTION);
      if (description != null) {
        stream.setDescription(description);
      }

      String capacity = entry.getTextField(FieldTypes.Stream.CAPACITY_IN_BYTES);
      if (capacity != null) {
        stream.setCapacityInBytes(Integer.valueOf(capacity));
      }

      String expiry = entry.getTextField(FieldTypes.Stream.EXPIRY_IN_SECONDS);
      if (expiry != null) {
        stream.setExpiryInSeconds(Integer.valueOf(expiry));
      }
      return stream;
    }

    @Override
    public Stream makeNonExisting(String app, String str) {
      return null;
    }

    @Override
    public CompareStatus compare(Stream stream, MetaDataEntry existingEntry) {

      Stream existing = makeFromEntry(existingEntry);
      CompareStatus status = CompareStatus.EQUAL;

      status = compareAlso(status, stream.getId(), existing.getId());
      if (status.equals(CompareStatus.DIFF)) {
        return status;
      }

      status = compareAlso(status, stream.getName(), existing.getName());
      if (status.equals(CompareStatus.DIFF)) {
        return status;
      }

      status = compareAlso(
          status, stream.getDescription(), existing.getDescription());
      if (status.equals(CompareStatus.DIFF)) {
        return status;
      }

      status = compareAlso(status, stream.getCapacityInBytes(), existing.getCapacityInBytes());
      if (status.equals(CompareStatus.DIFF)) {
        return status;
      }

      status = compareAlso(status, stream.getExpiryInSeconds(), existing.getExpiryInSeconds());
      return status;
    }

    @Override
    public String getId(Stream stream) {
      return stream.getId();
    }

    @Override
    public String getApplication(Stream stream) {
      return null;
    }

    @Override
    public String getName() {
      return "stream";
    }

    @Override
    public String getFieldType() {
      return FieldTypes.Stream.ID;
    }

  } // end StreamHelper

  //-------------------------- Dataset stuff ---------------------------------

  static class DatasetHelper implements Helper<Dataset> {

    @Override
    public void validate(Dataset dataset) throws MetadataServiceException {
      if (dataset.getId() == null || dataset.getId().isEmpty()) {
        throw new MetadataServiceException("Dataset id is empty or null.");
      }
      if (dataset.getName() == null || dataset.getName().isEmpty()) {
        throw new MetadataServiceException(
            "Dataset name must not be empty or null for create.");
      }
      if (dataset.getType() == null || dataset.getType().isEmpty()) {
        throw new MetadataServiceException(
            "Dataset type must not be empty or null for create.");
      }
    }

    @Override
    public MetaDataEntry makeEntry(String account, Dataset dataset) {
      MetaDataEntry entry = new MetaDataEntry(
          account, null, FieldTypes.Dataset.ID, dataset.getId());
      if (dataset.getName() != null) {
        entry.addField(FieldTypes.Dataset.NAME, dataset.getName());
      }

      if (dataset.getDescription() != null) {
        entry.addField(FieldTypes.Dataset.DESCRIPTION, dataset.getDescription());
      }

      if (dataset.getType() != null) {
        entry.addField(FieldTypes.Dataset.TYPE, dataset.getType());
      }

      if (dataset.getSpecification() != null) {
        entry.addField(FieldTypes.Dataset.SPECIFICATION, dataset.getSpecification());
      }
      return entry;
    }

    @Override
    public Dataset makeFromEntry(MetaDataEntry entry) {
      Dataset dataset = new Dataset(entry.getId());

      String name = entry.getTextField(FieldTypes.Dataset.NAME);
      if (name != null) {
        dataset.setName(name);
      }

      String description = entry.getTextField(FieldTypes.Dataset.DESCRIPTION);
      if (description != null) {
        dataset.setDescription(description);
      }

      String type = entry.getTextField(FieldTypes.Dataset.TYPE);
      if (type != null) {
        dataset.setType(type);
      }

      String spec = entry.getTextField(FieldTypes.Dataset.SPECIFICATION);
      if (spec != null) {
        dataset.setSpecification(spec);
      }
      return dataset;
    }

    @Override
    public Dataset makeNonExisting(String app, String ds) {
      return null;
    }

    @Override
    public CompareStatus compare(Dataset dataset, MetaDataEntry existingEntry) {
      Dataset existing = makeFromEntry(existingEntry);
      CompareStatus status = CompareStatus.EQUAL;

      status = compareAlso(status, dataset.getId(), existing.getId());
      if (status.equals(CompareStatus.DIFF)) {
        return status;
      }

      status = compareAlso(status, dataset.getName(), existing.getName());
      if (status.equals(CompareStatus.DIFF)) {
        return status;
      }

      status = compareAlso(
          status, dataset.getDescription(), existing.getDescription());
      if (status.equals(CompareStatus.DIFF)) {
        return status;
      }

      status = compareAlso(status, dataset.getType(), existing.getType());
      if (status.equals(CompareStatus.DIFF)) {
        return status;
      }

      status = compareAlso(status, dataset.getSpecification(), existing.getSpecification());
      return status;
    }

    @Override
    public String getId(Dataset dataset) {
      return dataset.getId();
    }

    @Override
    public String getApplication(Dataset dataset) {
      return null;
    }

    @Override
    public String getName() {
      return "dataset";
    }

    @Override
    public String getFieldType() {
      return FieldTypes.Dataset.ID;
    }

  } // end DatasetHelper

  //-------------------------- Application stuff -----------------------------

  static class ApplicationHelper implements Helper<Application> {

    @Override
    public void validate(Application app) throws MetadataServiceException {
      if (app.getId() == null || app.getId().isEmpty()) {
        throw new MetadataServiceException("Application id is empty or null.");
      }
      if (app.getName() == null || app.getName().isEmpty()) {
        throw new MetadataServiceException("" +
            "Application name cannot be null or empty for create.");
      }
    }

    @Override
    public MetaDataEntry makeEntry(String account, Application app) {
      MetaDataEntry entry = new MetaDataEntry(
          account, null, FieldTypes.Application.ID, app.getId());
      if (app.getName() != null) {
        entry.addField(FieldTypes.Application.NAME, app.getName());
      }
      if (app.getDescription() != null) {
        entry.addField(FieldTypes.Application.DESCRIPTION,
            app.getDescription());
      }
      return entry;
    }

    @Override
    public Application makeFromEntry(MetaDataEntry entry) {
      Application app = new Application(entry.getId());

      String name = entry.getTextField(FieldTypes.Dataset.NAME);
      if (name != null) {
        app.setName(name);
      }

      String description = entry.getTextField(FieldTypes.Dataset.DESCRIPTION);
      if (description != null) {
        app.setDescription(description);
      }
      return app;
    }

    @Override
    public Application makeNonExisting(String app, String id) {
      return null;
    }

    @Override
    public CompareStatus compare(Application app, MetaDataEntry existingEntry) {
      Application existing = makeFromEntry(existingEntry);
      CompareStatus status = CompareStatus.EQUAL;

      status = compareAlso(status, app.getId(), existing.getId());
      if (status.equals(CompareStatus.DIFF)) {
        return status;
      }

      status = compareAlso(status, app.getName(), existing.getName());
      if (status.equals(CompareStatus.DIFF)) {
        return status;
      }

      status = compareAlso(
          status, app.getDescription(), existing.getDescription());
      return status;
    }

    @Override
    public String getId(Application application) {
      return application.getId();
    }

    @Override
    public String getApplication(Application application) {
      return null;
    }

    @Override
    public String getName() {
      return "application";
    }

    @Override
    public String getFieldType() {
      return FieldTypes.Application.ID;
    }

  } // end ApplicationHelper

  //-------------------------- Procedure stuff -----------------------------------

  static class ProcedureHelper implements Helper<Procedure> {

    @Override
    public void validate(Procedure procedure) throws MetadataServiceException {
      if (procedure.getId() == null || procedure.getId().isEmpty()) {
        throw new MetadataServiceException("Procedure id is empty or null.");
      }

      if (procedure.getName() == null || procedure.getName().isEmpty()) {
        throw new MetadataServiceException("Procedure name is empty or null.");
      }

      if (procedure.getApplication() == null || procedure.getApplication().isEmpty()) {
        throw new MetadataServiceException("Procedure's app name is empty or null.");
      }

      if (procedure.getServiceName() == null || procedure.getServiceName().isEmpty()) {
        throw new MetadataServiceException(
            "Procedure service name cannot be null or empty");
      }
    }

    @Override
    public MetaDataEntry makeEntry(String account, Procedure procedure) {
      MetaDataEntry entry = new MetaDataEntry(
        account, procedure.getApplication(), FieldTypes.Procedure.ID, procedure.getId());

      if (procedure.getName() != null) {
        entry.addField(FieldTypes.Procedure.NAME, procedure.getName());
      }

      if (procedure.getDescription() != null) {
        entry.addField(FieldTypes.Procedure.DESCRIPTION, procedure.getDescription());
      }

      if (procedure.getServiceName() != null) {
        entry.addField(FieldTypes.Procedure.SERVICE_NAME, procedure.getServiceName());
      }

      if (procedure.getDatasets() != null) {
        entry.addField(FieldTypes.Procedure.DATASETS,
                       listToString(procedure.getDatasets()));
      }

      return entry;
    }

    @Override
    public Procedure makeFromEntry(MetaDataEntry entry) {
      Procedure procedure = new Procedure(entry.getId(), entry.getApplication());

      String name = entry.getTextField(FieldTypes.Procedure.NAME);
      if (name != null) {
        procedure.setName(name);
      }

      String description = entry.getTextField(FieldTypes.Procedure.DESCRIPTION);
      if (description != null) {
        procedure.setDescription(description);
      }

      String service = entry.getTextField(FieldTypes.Procedure.SERVICE_NAME);
      if (service != null) {
        procedure.setServiceName(service);
      }

      String datasets = entry.getTextField(FieldTypes.Procedure.DATASETS);
      if (datasets != null) {
        procedure.setDatasets(stringToList(datasets));
      }
      return procedure;
    }

    @Override
    public Procedure makeNonExisting(String app, String procedure) {
      return null;
    }

    @Override
    public CompareStatus compare(Procedure procedure, MetaDataEntry existingEntry) {
      Procedure existing = makeFromEntry(existingEntry);
      CompareStatus status = CompareStatus.EQUAL;

      status = compareAlso(status, procedure.getId(), existing.getId());
      if (status.equals(CompareStatus.DIFF)) {
        return status;
      }

      status = compareAlso(status, procedure.getName(), existing.getName());
      if (status.equals(CompareStatus.DIFF)) {
        return status;
      }

      status = compareAlso(
          status, procedure.getDescription(), existing.getDescription());
      if (status.equals(CompareStatus.DIFF)) {
        return status;
      }

      status = compareAlso(status, procedure.getServiceName(),
          existing.getServiceName());
      if (status.equals(CompareStatus.DIFF)) {
        return status;
      }

      status = compareAlso(status, procedure.getDatasets(), existing.getDatasets());
      return status;
    }

    @Override
    public String getId(Procedure procedure) {
      return procedure.getId();
    }

    @Override
    public String getApplication(Procedure procedure) {
      return procedure.getApplication();
    }

    @Override
    public String getName() {
      return "procedure";
    }

    @Override
    public String getFieldType() {
      return FieldTypes.Procedure.ID;
    }

  } // end ProcedureHelper

  //-------------------------- Mapreduce stuff -----------------------------------

  static class MapreduceHelper implements Helper<Mapreduce> {

    @Override
    public void validate(Mapreduce mapreduce) throws MetadataServiceException {
      if (mapreduce.getId() == null || mapreduce.getId().isEmpty()) {
        throw new MetadataServiceException("mapreduce id is empty or null.");
      }
      if (mapreduce.getName() == null || mapreduce.getName().isEmpty()) {
        throw new MetadataServiceException("Mapreduce name is empty or null.");
      }
      if (mapreduce.getApplication() == null || mapreduce.getApplication().isEmpty()) {
        throw new MetadataServiceException("Mapreduce's app name is empty or null.");
      }
    }

    @Override
    public MetaDataEntry makeEntry(String account, Mapreduce mapreduce) {
      MetaDataEntry entry = new MetaDataEntry(account,
          mapreduce.getApplication(), FieldTypes.Mapreduce.ID, mapreduce.getId());
      if (mapreduce.getName() != null) {
        entry.addField(FieldTypes.Mapreduce.NAME, mapreduce.getName());
      }
      if (mapreduce.getDescription() != null) {
        entry.addField(FieldTypes.Mapreduce.DESCRIPTION, mapreduce.getDescription());
      }
      if (mapreduce.getDatasets() != null) {
        entry.addField(FieldTypes.Mapreduce.DATASETS,
            listToString(mapreduce.getDatasets()));
      }
      return entry;
    }

    @Override
    public Mapreduce makeFromEntry(MetaDataEntry entry) {
      Mapreduce mapreduce = new Mapreduce(entry.getId(), entry.getApplication());
      String name = entry.getTextField(FieldTypes.Mapreduce.NAME);
      if (name != null) {
        mapreduce.setName(name);
      }
      String description = entry.getTextField(FieldTypes.Mapreduce.DESCRIPTION);
      if (description != null) {
        mapreduce.setDescription(description);
      }
      String datasets = entry.getTextField(FieldTypes.Mapreduce.DATASETS);
      if (datasets != null) {
        mapreduce.setDatasets(stringToList(datasets));
      }
      return mapreduce;
    }

    @Override
    public Mapreduce makeNonExisting(String app, String mapreduce) {
      return null;
    }

    @Override
    public CompareStatus compare(Mapreduce mapreduce, MetaDataEntry existingEntry) {
      Mapreduce existing = makeFromEntry(existingEntry);
      CompareStatus status = CompareStatus.EQUAL;
      status = compareAlso(status, mapreduce.getId(), existing.getId());
      if (status.equals(CompareStatus.DIFF)) {
        return status;
      }
      status = compareAlso(status, mapreduce.getName(), existing.getName());
      if (status.equals(CompareStatus.DIFF)) {
        return status;
      }
      status = compareAlso(
          status, mapreduce.getDescription(), existing.getDescription());
      if (status.equals(CompareStatus.DIFF)) {
        return status;
      }
      status = compareAlso(status, mapreduce.getDatasets(), existing.getDatasets());
      return status;
    }

    @Override
    public String getId(Mapreduce mapreduce) {
      return mapreduce.getId();
    }

    @Override
    public String getApplication(Mapreduce mapreduce) {
      return mapreduce.getApplication();
    }

    @Override
    public String getName() {
      return "mapreduce";
    }

    @Override
    public String getFieldType() {
      return FieldTypes.Mapreduce.ID;
    }

  } // end MapreduceHelper

  //-------------------------- Flow stuff ------------------------------------

  static class FlowHelper implements Helper<Flow> {

    @Override
    public void validate(Flow flow) throws MetadataServiceException {
      if (flow.getId() == null || flow.getId().isEmpty()) {
        throw new MetadataServiceException("Flow id is empty or null.");
      }

      if (flow.getName() == null || flow.getName().isEmpty()) {
        throw new MetadataServiceException("Flow name is empty or null.");
      }

      if (flow.getApplication() == null || flow.getApplication().isEmpty()) {
        throw new MetadataServiceException("Flow's app name is empty or null.");
      }
    }

    @Override
    public MetaDataEntry makeEntry(String account, Flow flow) {
      // Create a new metadata entry.
      MetaDataEntry entry = new MetaDataEntry(account, flow.getApplication(), FieldTypes.Flow.ID, flow.getId());
      if (flow.getName() != null) {
        entry.addField(FieldTypes.Flow.NAME, flow.getName());
      }
      entry.addField(FieldTypes.Flow.STREAMS, listToString(flow.getStreams()));
      entry.addField(FieldTypes.Flow.DATASETS, listToString(flow.getDatasets()));
      return entry;
    }

    @Override
    public Flow makeFromEntry(MetaDataEntry entry) {
      Flow fl = new Flow(entry.getId(), entry.getApplication());
      if (entry.getTextField(FieldTypes.Flow.NAME) != null) {
        fl.setName(entry.getTextField(FieldTypes.Flow.NAME));
      }
      fl.setStreams(stringToList(entry.getTextField(FieldTypes.Flow.STREAMS)));
      fl.setDatasets(stringToList(entry.getTextField(FieldTypes.Flow.DATASETS)));
      return fl;
    }

    @Override
    public Flow makeNonExisting(String app, String fl) {
      return null;
    }

    @Override
    public CompareStatus compare(Flow flow, MetaDataEntry existingEntry) {
      Flow existing = makeFromEntry(existingEntry);
      CompareStatus status = CompareStatus.EQUAL;

      status = compareAlso(status, flow.getId(), existing.getId());
      if (status.equals(CompareStatus.DIFF)) {
        return status;
      }

      status = compareAlso(status, flow.getName(), existing.getName());
      if (status.equals(CompareStatus.DIFF)) {
        return status;
      }

      status = compareAlso(status, flow.getDatasets(), existing.getDatasets());
      if (status.equals(CompareStatus.DIFF)) {
        return status;
      }

      status = compareAlso(status, flow.getStreams(), existing.getStreams());
      return status;
    }

    @Override
    public String getId(Flow flow) {
      return flow.getId();
    }

    @Override
    public String getApplication(Flow flow) {
      return flow.getApplication();
    }

    @Override
    public String getName() {
      return "flow";
    }

    @Override
    public String getFieldType() {
      return FieldTypes.Flow.ID;
    }

  } // end FlowHelper

  //-------------------------- Workflow stuff -------------------------------

  static class WorkflowHelper implements Helper<Workflow> {

    @Override
    public void validate(Workflow workflow) throws MetadataServiceException {
      if (workflow.getId() == null || workflow.getId().isEmpty()) {
        throw new MetadataServiceException("Workflow id is empty or null.");
      }

      if (workflow.getName() == null || workflow.getName().isEmpty()) {
        throw new MetadataServiceException("Workflow name is empty or null.");
      }

      if (workflow.getApplication() == null || workflow.getApplication().isEmpty()) {
        throw new MetadataServiceException("Workflow's app name is empty or null.");
      }
    }

    @Override
    public MetaDataEntry makeEntry(String account, Workflow workflow) {
      // Create a new metadata entry.
      MetaDataEntry entry = new MetaDataEntry(account,
                                              workflow.getApplication(), FieldTypes.Workflow.ID, workflow.getId());
      entry.addField(FieldTypes.Workflow.NAME, workflow.getName());
      return entry;
    }

    @Override
    public Workflow makeFromEntry(MetaDataEntry entry) {
      Workflow fl = new Workflow(entry.getId(), entry.getApplication());
      fl.setName(entry.getTextField(FieldTypes.Workflow.NAME));
      return fl;
    }

    @Override
    public Workflow makeNonExisting(String app, String fl) {
      Workflow workflow = new Workflow(fl, app);
      workflow.setExists(false);
      return workflow;
    }

    @Override
    public CompareStatus compare(Workflow workflow, MetaDataEntry existingEntry) {
      Workflow existing = makeFromEntry(existingEntry);
      CompareStatus status = CompareStatus.EQUAL;

      status = compareAlso(status, workflow.getId(), existing.getId());
      if (status.equals(CompareStatus.DIFF)) {
        return status;
      }

      status = compareAlso(status, workflow.getName(), workflow.getName());
      if (status.equals(CompareStatus.DIFF)) {
        return status;
      }

      return status;
    }

    @Override
    public String getId(Workflow flow) {
      return flow.getId();
    }

    @Override
    public String getApplication(Workflow flow) {
      return flow.getApplication();
    }

    @Override
    public String getName() {
      return "workflow";
    }

    @Override
    public String getFieldType() {
      return FieldTypes.Workflow.ID;
    }
  }

}
