package org.opendatakit.aggregate.settings_app;

import org.opendatakit.aggregate.datamodel.TopLevelDynamicBase;
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
public class AppSettingsTable extends TopLevelDynamicBase {
  static final String TABLE_NAME = "_form_info";
  private static final String URI_SETTINGS_ID_VALUE = "aggregate.opendatakit.org:AppSettings";


  public static final DataField SETTINGS_NAME = new DataField("SETTINGS_NAME",
      DataField.DataType.STRING, false, PersistConsts.GUARANTEED_SEARCHABLE_LEN);

  /**
   * Construct a relation prototype.
   *
   * @param databaseSchema
   */
  private AppSettingsTable(String databaseSchema) {
    super(databaseSchema, TABLE_NAME);
    fieldList.add(SETTINGS_NAME);

    fieldValueMap.put(primaryKey, CommonFieldsBase.newMD5HashUri(URI_SETTINGS_ID_VALUE));
  }

  /**
   * Construct an empty entity.
   *
   * @param ref
   * @param user
   */
  private AppSettingsTable(AppSettingsTable ref, User user) {
    super(ref, user);
  }

  @Override
  public AppSettingsTable getEmptyRow(User user) {
    return new AppSettingsTable(this, user);
  }

  private static AppSettingsTable relation = null;

  static synchronized final AppSettingsTable assertRelation(CallingContext cc) throws ODKDatastoreException {
    if ( relation == null ) {
      AppSettingsTable relationPrototype;
      Datastore ds = cc.getDatastore();
      User user = cc.getUserService().getDaemonAccountUser();
      relationPrototype = new AppSettingsTable(ds.getDefaultSchemaName());
      ds.assertRelation(relationPrototype, user); // may throw exception...
      // at this point, the prototype has become fully populated
      relation = relationPrototype; // set static variable only upon success...
    }
    return relation;
  }
}
