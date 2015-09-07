package org.opendatakit.common.security.spring;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.common.persistence.CommonFieldsBase;
import org.opendatakit.common.persistence.DataField;
import org.opendatakit.common.persistence.Datastore;
import org.opendatakit.common.persistence.Query;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.security.User;
import org.opendatakit.common.web.CallingContext;

import java.util.List;

/**
 * @author: Anna
 * @created: 2015-09-07.
 */
public class AclTable extends CommonFieldsBase {



  public enum ProtectedClasses  {
    FORM ("Form");

    private final String name;

    ProtectedClasses(String txt) { name = txt; }
    public String getType() { return name; }
  }


  private static final Log logger = LogFactory.getLog(AclTable.class);

  private static final String TABLE_NAME = "_acl_entry";
  public static final Long DEFAULT_ID = 1L;



  public static final DataField ID = new DataField("ACL_ID",
      DataField.DataType.INTEGER, false, 1, 32);

  public static final DataField OBJECT_CLASS = new DataField("OBJECT_CLASS",
      DataField.DataType.STRING, false);

  public static final DataField OBJECT_IDENTITY = new DataField("OBJECT_ID_IDENTITY",
      DataField.DataType.INTEGER, false, 0, 32);

  public static final DataField SID = new DataField("SID",
      DataField.DataType.INTEGER, false, 0, 32);

  public static final DataField GRANTED = new DataField("GRANTED",
      DataField.DataType.BOOLEAN, false);


  /**
   * Construct a relation prototype. Only called via
   * {@link #assertRelation(Datastore, User)}
   *
   * @param schemaName
   */
  protected AclTable(String schemaName) {
    super(schemaName, TABLE_NAME);
    fieldList.add(ID);
    fieldList.add(OBJECT_CLASS);
    fieldList.add(OBJECT_IDENTITY);
    fieldList.add(SID);
    fieldList.add(GRANTED);
  }

  /**
   * Construct an empty entity. Only called via {@link #getEmptyRow(User)}
   *
   * @param ref
   * @param user
   */
  protected AclTable(AclTable ref, User user) {
    super(ref, user);
  }

  // Only called from within the persistence layer.
  @Override
  public CommonFieldsBase getEmptyRow(User user) {
    AclTable t = new AclTable(this, user);
    return t;
  }

  public static Query createQuery(Datastore ds, String loggingContextTag, User user)
      throws ODKDatastoreException {
    Query q = ds
        .createQuery(AclTable.assertRelation(ds, user), loggingContextTag, user);
    return q;
  }


  private static AclTable relation = null;

  /**
   * This is private because this table has a no-deletions policy represented by
   * the IS_REMOVED flag. Depending upon the semantics of the usage, return
   * values should be filtered by the value of that flag to retrieve only the
   * active users in the system.
   *
   * @param datastore
   * @param user
   * @return
   * @throws ODKDatastoreException
   */
  public static synchronized final AclTable assertRelation(Datastore datastore,
                                                                        User user) throws ODKDatastoreException {
    if (relation == null) {
      AclTable relationPrototype;
      relationPrototype = new AclTable(datastore.getDefaultSchemaName());
      datastore.assertRelation(relationPrototype, user);
      relation = relationPrototype;
    }
    return relation;
  }

  public static void addAdminAccess(long formAclId, CallingContext bootstrapCc) throws ODKDatastoreException {

    Datastore ds = bootstrapCc.getDatastore();
    User user = bootstrapCc.getCurrentUser();

    if (relation == null) assertRelation(ds, user);
    AclTable entryRow = ds.createEntityUsingRelation(relation,user);
    entryRow.setStringField(OBJECT_CLASS, ProtectedClasses.FORM.getType());
    entryRow.setLongField(OBJECT_IDENTITY, formAclId);
    entryRow.setLongField(SID, DEFAULT_ID);
    entryRow.setBooleanField(GRANTED, true);
    logger.info("Generating user rights to first Form");
    ds.putEntity(entryRow, user);
  }

  public static void deleteEntriesFor(Long id, CallingContext bootstrapCc) throws ODKDatastoreException {
    Datastore ds = bootstrapCc.getDatastore();
    User user = bootstrapCc.getCurrentUser();

    Query q = ds.createQuery(relation, "AclTable.deletingEntries", user);
    q.addFilter(AclTable.OBJECT_IDENTITY, Query.FilterOperation.EQUAL, id);
    List<AclTable> rows = (List<AclTable>) q.executeQuery();
    for (AclTable entry : rows) {
      ds.deleteEntity(entry.getEntityKey(), user);
    }
  }
}
