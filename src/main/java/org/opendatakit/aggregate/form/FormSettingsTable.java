package org.opendatakit.aggregate.form;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.common.persistence.CommonFieldsBase;
import org.opendatakit.common.persistence.DataField;
import org.opendatakit.common.persistence.Datastore;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.security.User;
import org.opendatakit.common.web.CallingContext;

/**
 * Created by Anna on 2015-08-09.
 */
public class FormSettingsTable extends CommonFieldsBase {
    private static final Log logger = LogFactory.getLog(FormSettingsTable.class.getName());
    private static final String TABLE_NAME = "_form_settings";

    private static final DataField FORM_ID = new DataField("FORM_ID", DataField.DataType.STRING,
        false, IForm.MAX_FORM_ID_LENGTH)
        .setIndexable(DataField.IndexType.HASH);
    private static final DataField HAS_SETTINGS = new DataField("HAS_SETTINGS", DataField.DataType.BOOLEAN, false)
        .setIndexable(DataField.IndexType.NONE);

    public FormSettingsTable(String schemaName) {
        super(schemaName, TABLE_NAME);
        fieldList.add(FORM_ID);
        fieldList.add(HAS_SETTINGS);

        fieldValueMap.put(primaryKey, CommonFieldsBase.newMD5HashUri(FormInfo.FORM_ID));
        fieldValueMap.put(FORM_ID, FormInfo.FORM_ID);


    }
    private FormSettingsTable(FormSettingsTable ref, User user) {
        super(ref, user);
    }
    @Override
    public CommonFieldsBase getEmptyRow(User user) {
        return new FormSettingsTable(this, user);
    }

    private static FormSettingsTable relation = null;

    static synchronized final FormSettingsTable assertRelation(CallingContext cc) throws ODKDatastoreException {
        if ( relation == null ) {
            FormSettingsTable relationPrototype;
            Datastore ds = cc.getDatastore();
            User user = cc.getUserService().getDaemonAccountUser();
            relationPrototype = new FormSettingsTable(ds.getDefaultSchemaName());
            ds.assertRelation(relationPrototype, user); // may throw exception...
            // at this point, the prototype has become fully populated
            relation = relationPrototype; // set static variable only upon success...
        }
        return relation;
    }
}
