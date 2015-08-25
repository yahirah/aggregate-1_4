package org.opendatakit.aggregate.settings;

import org.opendatakit.aggregate.datamodel.TopLevelDynamicBase;
import org.opendatakit.common.datamodel.BinaryContent;
import org.opendatakit.common.datamodel.BinaryContentManipulator;
import org.opendatakit.common.datamodel.BinaryContentRefBlob;
import org.opendatakit.common.datamodel.RefBlob;
import org.opendatakit.common.persistence.CommonFieldsBase;
import org.opendatakit.common.persistence.DataField;
import org.opendatakit.common.persistence.Datastore;
import org.opendatakit.common.persistence.PersistConsts;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.security.User;
import org.opendatakit.common.web.CallingContext;

/**
 * Created by Anna on 2015-08-22.
 */
public class AppSettingsFilesetTable extends TopLevelDynamicBase {
  static final String TABLE_NAME = "_app_settings";

  private static final String APP_SETTINGS_REF_BLOB = "_app_settings_blb";

  private static final String APP_SETTINGS_BINARY_CONTENT_REF_BLOB = "_app_settings_ref";

  private static final String APP_SETTINGS_BINARY_CONTENT = "_app_settings_bin";

  public static final DataField ROOT_ELEMENT_MODEL_VERSION = new DataField("ROOT_ELEMENT_MODEL_VERSION",
      DataField.DataType.INTEGER, true);

  public static final DataField IS_DOWNLOAD_ALLOWED = new DataField("IS_DOWNLOAD_ALLOWED",
      DataField.DataType.BOOLEAN, true);

  public static final DataField SETTINGS_NAME = new DataField("SETTINGS_NAME",
      DataField.DataType.STRING, false, PersistConsts.GUARANTEED_SEARCHABLE_LEN);

  public static final String URI_SETTINGS_ID_VALUE_APP_SETTINGS_FILE = "aggregate.opendatakit.org:AppSettingsFile";

  private AppSettingsFilesetTable(String databaseSchema) {
    super(databaseSchema, TABLE_NAME);
    fieldList.add(IS_DOWNLOAD_ALLOWED);
    fieldList.add(SETTINGS_NAME);
    fieldList.add(ROOT_ELEMENT_MODEL_VERSION);
    fieldValueMap.put(primaryKey, CommonFieldsBase.newMD5HashUri(AppSettingsFilesetTable
        .URI_SETTINGS_ID_VALUE_APP_SETTINGS_FILE));
    fieldValueMap.put(SETTINGS_NAME, AppSettingsFilesetTable
        .URI_SETTINGS_ID_VALUE_APP_SETTINGS_FILE);
  }

  private AppSettingsFilesetTable(AppSettingsFilesetTable ref, User user) {
    super(ref, user);
  }

  @Override
  public AppSettingsFilesetTable getEmptyRow(User user) {
    return new AppSettingsFilesetTable(this, user);
  }

  private static AppSettingsFilesetTable relation = null;

  private static BinaryContent settingsBinaryRelation = null;
  private static BinaryContentRefBlob settingsBinaryRefBlobRelation = null;
  private static RefBlob settingsRefBlobRelation = null;


  static synchronized final AppSettingsFilesetTable assertRelation(CallingContext cc) throws ODKDatastoreException {
    if ( relation == null ) {
      AppSettingsFilesetTable relationPrototype;
      Datastore ds = cc.getDatastore();
      User user = cc.getUserService().getDaemonAccountUser();
      relationPrototype = new AppSettingsFilesetTable(ds.getDefaultSchemaName());
      ds.assertRelation(relationPrototype, user); // may throw exception...
      // at this point, the prototype has become fully populated
      BinaryContent settingsBc = new BinaryContent(ds.getDefaultSchemaName(), APP_SETTINGS_BINARY_CONTENT);
      ds.assertRelation(settingsBc, user);
      BinaryContentRefBlob settingsBref = new BinaryContentRefBlob(ds.getDefaultSchemaName(), APP_SETTINGS_BINARY_CONTENT_REF_BLOB);
      ds.assertRelation(settingsBref, user);
      RefBlob settingsRef = new RefBlob(ds.getDefaultSchemaName(), APP_SETTINGS_REF_BLOB);
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
