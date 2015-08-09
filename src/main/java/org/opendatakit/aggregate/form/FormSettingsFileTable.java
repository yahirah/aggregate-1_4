package org.opendatakit.aggregate.form;

import org.opendatakit.common.datamodel.*;
import org.opendatakit.common.persistence.CommonFieldsBase;
import org.opendatakit.common.persistence.DataField;
import org.opendatakit.common.persistence.Datastore;
import org.opendatakit.common.persistence.PersistConsts;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.security.User;
import org.opendatakit.common.web.CallingContext;

/**
 * Created by Anna on 2015-08-09.
 */
public class FormSettingsFileTable extends DynamicBase {
  private final String TABLE_NAME = "_form_settings_file";

  private static final String FORM_SETTINGS_REF_BLOB = "_form_settings_blb";

  private static final String FORM_SETTINGS_BINARY_CONTENT_REF_BLOB = "_form_settings_ref";

  private static final String FORM_SETTINGS_BINARY_CONTENT = "_form_settings_bin";

  public static final DataField IS_DOWNLOAD_ALLOWED = new DataField("IS_DOWNLOAD_ALLOWED",
      DataField.DataType.BOOLEAN, true);

  public static final DataField FORM_NAME = new DataField("FORM_NAME",
      DataField.DataType.STRING, true, PersistConsts.GUARANTEED_SEARCHABLE_LEN);

  public static final DataField FORM_ID = new DataField("FORM_ID", DataField.DataType.STRING,
      false, IForm.MAX_FORM_ID_LENGTH)
      .setIndexable(DataField.IndexType.HASH);

  public static final String URI_FORM_ID_VALUE_FORM_SETTINGS_FILE = "aggregate.opendatakit.org:FormSettingsFile";
  
  private FormSettingsFileTable(String databaseSchema) {
    super(databaseSchema, TABLE_NAME);
    fieldList.add(FORM_ID);
    fieldList.add(IS_DOWNLOAD_ALLOWED);
    fieldList.add(FORM_NAME);
    fieldValueMap.put(primaryKey, FormSettingsFileTable.URI_FORM_ID_VALUE_FORM_SETTINGS_FILE);
  }

  private FormSettingsFileTable(FormSettingsFileTable ref, User user) {
    super(ref, user);
  }
  
  @Override
  public FormSettingsFileTable getEmptyRow(User user) {
      return new FormSettingsFileTable(this, user);
  }
  
  private static FormSettingsFileTable relation = null;

  private static BinaryContent settingsBinaryRelation = null;
  private static BinaryContentRefBlob settingsBinaryRefBlobRelation = null;
  private static RefBlob settingsRefBlobRelation = null;


  static synchronized final FormSettingsFileTable assertRelation(CallingContext cc) throws ODKDatastoreException {
    if ( relation == null ) {
      FormSettingsFileTable relationPrototype;
      Datastore ds = cc.getDatastore();
      User user = cc.getUserService().getDaemonAccountUser();
      relationPrototype = new FormSettingsFileTable(ds.getDefaultSchemaName());
      ds.assertRelation(relationPrototype, user); // may throw exception...
      // at this point, the prototype has become fully populated
      BinaryContent settingsBc = new BinaryContent(ds.getDefaultSchemaName(), FORM_SETTINGS_BINARY_CONTENT);
      ds.assertRelation(settingsBc, user);
      BinaryContentRefBlob settingsBref = new BinaryContentRefBlob(ds.getDefaultSchemaName(), FORM_SETTINGS_BINARY_CONTENT_REF_BLOB);
      ds.assertRelation(settingsBref, user);
      RefBlob settingsRef = new RefBlob(ds.getDefaultSchemaName(), FORM_SETTINGS_REF_BLOB);
      ds.assertRelation(settingsRef, user);

      relation = relationPrototype; // set static variable only upon success...
      settingsBinaryRelation = settingsBc;
      settingsBinaryRefBlobRelation = settingsBref;
      settingsRefBlobRelation = settingsRef;

    }
    return relation;
  }

  static final BinaryContentManipulator assertSettingsManipulator(String topLevelAuri, String uri, CallingContext cc)
      throws ODKDatastoreException {
    // make sure the relations are defined...
    assertRelation(cc);
    return new BinaryContentManipulator(uri, topLevelAuri, settingsBinaryRelation, settingsBinaryRefBlobRelation, settingsRefBlobRelation);
  }

}
