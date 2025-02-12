/**
 * Copyright (C) 2010 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.common.persistence.engine.pgres;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.common.persistence.CommonFieldsBase;
import org.opendatakit.common.persistence.DataField;
import org.opendatakit.common.persistence.DataField.IndexType;
import org.opendatakit.common.persistence.Datastore;
import org.opendatakit.common.persistence.EntityKey;
import org.opendatakit.common.persistence.PersistConsts;
import org.opendatakit.common.persistence.Query;
import org.opendatakit.common.persistence.Query.FilterOperation;
import org.opendatakit.common.persistence.TaskLock;
import org.opendatakit.common.persistence.engine.DatastoreAccessMetrics;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.persistence.exception.ODKEntityNotFoundException;
import org.opendatakit.common.persistence.exception.ODKEntityPersistException;
import org.opendatakit.common.security.User;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 *
 * @author wbrunette@gmail.com
 * @author mitchellsundt@gmail.com
 *
 */
public class DatastoreImpl implements Datastore, InitializingBean {

 // private static final Log logger = LogFactory.getLog(DatastoreImpl.class.getName());
  // issue 868 - PostgreSQL apparently has a 63-character limit on its column
  // names.
  private static final int MAX_COLUMN_NAME_LEN = 63;
  // issue 868 - assume this is also true of table names...
  private static final int MAX_TABLE_NAME_LEN = 59; // reserve 4 char for idx
                                                    // name

  private final DatastoreAccessMetrics dam = new DatastoreAccessMetrics();
  private DataSource dataSource = null;
  private DataSourceTransactionManager tm = null;

  private static final Long MAX_BLOB_SIZE = 65536 * 4096L;
  private String schemaName = null;

  public DatastoreImpl() throws ODKDatastoreException {
  }

  public void setDataSource(DataSource dataSource) {
    this.dataSource = dataSource;
    this.tm = new DataSourceTransactionManager(dataSource);
  }

  public void setSchemaName(String schemaName) {
    this.schemaName = schemaName;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    if (dataSource == null) {
      throw new IllegalStateException("dataSource property must be set!");
    }
    if (schemaName == null) {
      JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
      List<?> databaseNames = jdbcTemplate.queryForList("SELECT current_database()", String.class);
      schemaName = (String) databaseNames.get(0);
    }
  }

  public static final String K_CREATE_TABLE = "CREATE TABLE ";
  public static final String K_DROP_TABLE = "DROP TABLE ";

  public static final String K_OPEN_PAREN = " ( ";
  public static final String K_CLOSE_PAREN = " ) ";
  public static final String K_SELECT = "SELECT ";
  public static final String K_SELECT_DISTINCT = "SELECT DISTINCT ";
  public static final String K_CS = ", ";
  public static final String K_COLON = ";";
  public static final String K_BQ = "\"";
  public static final String K_FROM = " FROM ";
  public static final String K_WHERE = " WHERE ";
  public static final String K_AND = " AND ";
  public static final String K_EQ = " = ";
  public static final String K_BIND_VALUE = "?";
  public static final String K_CREATE_INDEX = "CREATE INDEX ";
  public static final String K_ON = " ON ";
  public static final String K_USING_HASH = " USING HASH ";
  public static final String K_INSERT_INTO = "INSERT INTO ";
  public static final String K_VALUES = " VALUES ";
  public static final String K_UPDATE = "UPDATE ";
  public static final String K_SET = " SET ";
  public static final String K_DELETE_FROM = "DELETE FROM ";
  public static final String K_DEFAULT = " DEFAULT ";

  public static final Integer DEFAULT_DBL_NUMERIC_SCALE = 10;
  public static final Integer DEFAULT_DBL_NUMERIC_PRECISION = 38;
  public static final Integer DEFAULT_INT_NUMERIC_PRECISION = 9;

  private static final class TableDefinition {

    public DataField.DataType getDataType() {
      return dataType;
    }

    public void setDataType(DataField.DataType dataType) {
      this.dataType = dataType;
    }

    public String getColumnName() {
      return columnName;
    }

    public boolean isNullable() {
      return isNullable;
    }

    public Long getMaxCharLen() {
      return maxCharLen;
    }

    public Integer getNumericScale() {
      return numericScale;
    }

    public Integer getNumericPrecision() {
      return numericPrecision;
    }

    public static final String COLUMN_NAME = "column_name";
    public static final String TABLE_NAME = "table_name";
    public static final String TABLE_SCHEMA = "table_schema";
    public static final String CHARACTER_MAXIMUM_LENGTH = "character_maximum_length";
    public static final String NUMERIC_PRECISION = "numeric_precision";
    public static final String NUMERIC_SCALE = "numeric_scale";
    public static final String DATA_TYPE = "data_type";
    public static final String IS_NULLABLE = "is_nullable";
    public static final String INFORMATION_SCHEMA_COLUMNS = "information_schema.columns";
    public static final String K_COUNT_ONE = "COUNT(1)";

    public static final String TABLE_DEF_QUERY = K_SELECT + COLUMN_NAME + K_CS + IS_NULLABLE + K_CS
        + CHARACTER_MAXIMUM_LENGTH + K_CS + NUMERIC_PRECISION + K_CS + NUMERIC_SCALE + K_CS
        + DATA_TYPE + K_FROM + INFORMATION_SCHEMA_COLUMNS + K_WHERE + TABLE_SCHEMA + K_EQ
        + K_BIND_VALUE + K_AND + TABLE_NAME + K_EQ + K_BIND_VALUE;

    public static final String TABLE_EXISTS_QUERY = K_SELECT + K_COUNT_ONE + K_FROM
        + INFORMATION_SCHEMA_COLUMNS + K_WHERE + TABLE_SCHEMA + K_EQ + K_BIND_VALUE + K_AND
        + TABLE_NAME + K_EQ + K_BIND_VALUE;

    private static final String YES = "YES";
    private static final String TEXT = "text"; // lower case!
    private static final String CHAR = "char";
    private static final String BLOB = "blob";
    private static final String BYTEA = "bytea"; // different in pgres
    private static final String DATE = "date";
    private static final String BOOLEAN = "boolean";
    // private static final String DATETIME = "datetime";
    private static final String TIME = "time";
    private static final Long MAX_ROW_SIZE = 65000L; // to allow PK room

    private String columnName;
    private boolean isNullable;
    private Long maxCharLen = null;
    private Integer numericScale = null;
    private Integer numericPrecision = null;
    private DataField.DataType dataType;

    TableDefinition(ResultSet rs) throws SQLException {
      columnName = rs.getString(COLUMN_NAME);
      String s = rs.getString(IS_NULLABLE);
      isNullable = YES.equalsIgnoreCase(s);
      String type = rs.getString(DATA_TYPE);
      BigDecimal num = rs.getBigDecimal(CHARACTER_MAXIMUM_LENGTH);
      if (type.equalsIgnoreCase(BOOLEAN)) {
        dataType = DataField.DataType.BOOLEAN;
      } else if (type.equalsIgnoreCase(BYTEA)) {
        dataType = DataField.DataType.BINARY;
        maxCharLen = MAX_BLOB_SIZE;
      } else if (type.equalsIgnoreCase(TEXT)) {
        dataType = DataField.DataType.LONG_STRING;
        maxCharLen = MAX_BLOB_SIZE;
      } else if (num != null) {
        maxCharLen = num.longValueExact();
        if (type.contains(TEXT) || type.contains(CHAR)) {
          if (maxCharLen.compareTo(MAX_ROW_SIZE) <= 0) {
            dataType = DataField.DataType.STRING;
          } else {
            dataType = DataField.DataType.LONG_STRING;
          }
        } else if (type.contains(BLOB) || type.contains(BYTEA)) {
          dataType = DataField.DataType.BINARY;
        } else {
          throw new IllegalArgumentException("unrecognized data type in schema: " + type);
        }
      } else {
        // must be date or numeric...
        num = rs.getBigDecimal(NUMERIC_SCALE);
        if (num == null) {
          // better be a date...
          if (type.contains(DATE) || type.contains(TIME)) {
            dataType = DataField.DataType.DATETIME;
          } else {
            throw new IllegalArgumentException("unrecognized data type in schema: " + type);
          }
        } else {
          // discriminate between decimal and integer by looking at value...
          // We assume that nobody is going crazy with the scale here...
          if (BigDecimal.ZERO.equals(num)) {
            dataType = DataField.DataType.INTEGER;
            numericScale = 0;
          } else {
            numericScale = num.intValueExact();
            dataType = DataField.DataType.DECIMAL;
          }
          num = rs.getBigDecimal(NUMERIC_PRECISION);
          numericPrecision = num.intValueExact();
        }
      }
    }
  };

  private static RowMapper<TableDefinition> tableDef = new RowMapper<TableDefinition>() {
    @Override
    public TableDefinition mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new TableDefinition(rs);
    }
  };

  public static void buildArgumentList(Object[] ol, int[] il, int idx, CommonFieldsBase entity,
      DataField f) {
    switch (f.getDataType()) {
    case BOOLEAN:
      ol[idx] = entity.getBooleanField(f);
      il[idx] = java.sql.Types.BOOLEAN;
      break;
    case STRING:
    case URI:
      ol[idx] = entity.getStringField(f);
      il[idx] = java.sql.Types.VARCHAR;
      break;
    case INTEGER:
      ol[idx] = entity.getLongField(f);
      il[idx] = java.sql.Types.BIGINT;
      break;
    case DECIMAL:
      ol[idx] = entity.getNumericField(f);
      il[idx] = java.sql.Types.DECIMAL;
      break;
    case DATETIME:
      ol[idx] = entity.getDateField(f);
      il[idx] = java.sql.Types.TIMESTAMP;
      break;
    case BINARY:
      ol[idx] = entity.getBlobField(f);
      il[idx] = java.sql.Types.LONGVARBINARY;
      break;
    case LONG_STRING:
      ol[idx] = entity.getStringField(f);
      il[idx] = java.sql.Types.LONGVARCHAR;
      break;

    default:
      throw new IllegalStateException("Unexpected data type");
    }
  }

  void recordQueryUsage(CommonFieldsBase relation, int recCount) {
    dam.recordQueryUsage(relation, recCount);
  }

  @Override
  public String getDefaultSchemaName() {
    return schemaName;
  }

  JdbcTemplate getJdbcConnection() {
    return new JdbcTemplate(dataSource);
  }

  @Override
  public int getMaxLenColumnName() {
    return MAX_COLUMN_NAME_LEN;
  }

  @Override
  public int getMaxLenTableName() {
    return MAX_TABLE_NAME_LEN;
  }

  private final boolean updateRelation(JdbcTemplate jc, CommonFieldsBase relation,
      String originalStatement) {

    String qs = TableDefinition.TABLE_DEF_QUERY;
    List<?> columns;
    columns = jc.query(qs, new Object[] { relation.getSchemaName(), relation.getTableName() },
        tableDef);
    dam.recordQueryUsage(TableDefinition.INFORMATION_SCHEMA_COLUMNS, columns.size());

    if (columns.size() > 0) {
      Map<String, TableDefinition> map = new HashMap<String, TableDefinition>();
      for (Object o : columns) {
        TableDefinition t = (TableDefinition) o;
        map.put(t.getColumnName(), t);
      }

      // we may have gotten some results into columns -- go through the fields
      // and
      // assemble the results... we don't care about additional columns in the
      // map...
      for (DataField f : relation.getFieldList()) {
        TableDefinition d = map.get(f.getName());
        if (d == null) {
          StringBuilder b = new StringBuilder();
          if (originalStatement == null) {
            b.append(" Retrieving expected definition (");
            boolean first = true;
            for (DataField field : relation.getFieldList()) {
              if (!first) {
                b.append(K_CS);
              }
              first = false;
              b.append(field.getName());
            }
            b.append(")");
          } else {
            b.append(" Created with: ");
            b.append(originalStatement);
          }
          throw new IllegalStateException("did not find expected column " + f.getName()
              + " in table " + relation.getSchemaName() + "." + relation.getTableName()
              + b.toString());
        }
        if (f.getDataType() == DataField.DataType.BOOLEAN
            && d.getDataType() == DataField.DataType.STRING) {
          d.setDataType(DataField.DataType.BOOLEAN);
          // don't care about size...
        }

        if (d.getDataType() == DataField.DataType.STRING && f.getMaxCharLen() != null
            && f.getMaxCharLen().compareTo(d.getMaxCharLen()) > 0) {
          throw new IllegalStateException("column " + f.getName() + " in table "
              + relation.getSchemaName() + "." + relation.getTableName()
              + " stores string-valued keys but is shorter than required by Aggregate "
              + d.getMaxCharLen().toString() + " < " + f.getMaxCharLen().toString());
        }

        if (f.getDataType() == DataField.DataType.URI) {
          if (d.getDataType() != DataField.DataType.STRING) {
            throw new IllegalStateException("column " + f.getName() + " in table "
                + relation.getSchemaName() + "." + relation.getTableName()
                + " stores URIs but is not a string field");
          }
          d.setDataType(DataField.DataType.URI);
        }

        if (d.getDataType() != f.getDataType()) {
          throw new IllegalStateException("column " + f.getName() + " in table "
              + relation.getSchemaName() + "." + relation.getTableName()
              + " is not of the expected type " + f.getDataType().toString());
        }

        // it is OK for the data model to be more strict than the data store.
        if (!d.isNullable() && f.getNullable()) {
          throw new IllegalStateException("column " + f.getName() + " in table "
              + relation.getSchemaName() + "." + relation.getTableName()
              + " is defined as NOT NULL but the data model requires NULL");
        }
        f.setMaxCharLen(d.getMaxCharLen());
        f.setNumericPrecision(d.getNumericPrecision());
        f.setNumericScale(d.getNumericScale());
      }
      return true;
    } else {
      return false;
    }
  }

  /**
   * Relation manipulation APIs
   */
  @Override
  public void assertRelation(CommonFieldsBase relation, User user) throws ODKDatastoreException {
    JdbcTemplate jc = getJdbcConnection();
    TransactionStatus status = null;
    try {
      DefaultTransactionDefinition paramTransactionDefinition = new DefaultTransactionDefinition();

      // do serializable read on the information schema...
      paramTransactionDefinition
          .setIsolationLevel(DefaultTransactionDefinition.ISOLATION_SERIALIZABLE);
      paramTransactionDefinition.setReadOnly(true);
      status = tm.getTransaction(paramTransactionDefinition);

      // see if relation already is defined and update it with dimensions...
      if (updateRelation(jc, relation, null)) {
        // it exists -- we're done!
        tm.commit(status);
        status = null;
        return;
      } else {
        tm.commit(status);
        // Try a new transaction to create the table
        paramTransactionDefinition
            .setIsolationLevel(DefaultTransactionDefinition.ISOLATION_SERIALIZABLE);
        paramTransactionDefinition.setReadOnly(false);
        status = tm.getTransaction(paramTransactionDefinition);

        // need to create the table...
        StringBuilder b = new StringBuilder();
        b.append(K_CREATE_TABLE);
        b.append(K_BQ);
        b.append(relation.getSchemaName());
        b.append(K_BQ);
        b.append(".");
        b.append(K_BQ);
        b.append(relation.getTableName());
        b.append(K_BQ);
        b.append(K_OPEN_PAREN);
        boolean firstTime = true;
        for (DataField f : relation.getFieldList()) {
          if (!firstTime) {
            b.append(K_CS);
          }
          firstTime = false;
          b.append(K_BQ);
          b.append(f.getName());
          b.append(K_BQ);
          DataField.DataType type = f.getDataType();
          switch (type) {
          case BINARY:
            b.append(" BYTEA");
            break;
          case LONG_STRING:
            b.append(" TEXT");// b.append(" CHARACTER SET utf8");
            break;
          case STRING:
            b.append(" VARCHAR(");
            Long len = f.getMaxCharLen();
            if (len == null) {
              len = PersistConsts.DEFAULT_MAX_STRING_LENGTH;
            }
            b.append(len.toString());
            b.append(K_CLOSE_PAREN);
            // b.append(" CHARACTER SET utf8");
            break;
          case BOOLEAN:
            b.append(" BOOLEAN");
            break;
          case INTEGER:
            Integer int_digits = f.getNumericPrecision();
            if (int_digits == null) {
              int_digits = DEFAULT_INT_NUMERIC_PRECISION;
            }
            Integer isSerial = f.getNumericScale();
           // logger.warn("********************");
           // logger.warn(int_digits + " ---" + isSerial);
            if (int_digits.compareTo(9) > 0) {
              if (isSerial != null && isSerial == 1) {
                b.append(" SERIAL");
                 continue;
                //    logger.warn("^^^^^^^^^^^^^^^^");
              } else {
                b.append(" BIGINT");
              }
            }else {
              b.append(" INTEGER");
            }
            break;
          case DECIMAL:
            Integer dbl_digits = f.getNumericPrecision();
            Integer dbl_fract = f.getNumericScale();
            if (dbl_digits == null) {
              dbl_digits = DEFAULT_DBL_NUMERIC_PRECISION;
            }
            if (dbl_fract == null) {
              dbl_fract = DEFAULT_DBL_NUMERIC_SCALE;
            }
            b.append(" DECIMAL(");
            b.append(dbl_digits.toString());
            b.append(K_CS);
            b.append(dbl_fract.toString());
            b.append(K_CLOSE_PAREN);
            break;
          case DATETIME:
            b.append(" TIMESTAMP WITHOUT TIME ZONE");
            break;
          case URI:
            b.append(" VARCHAR(");
            len = f.getMaxCharLen();
            if (len == null) {
              len = PersistConsts.URI_STRING_LEN;
            }
            b.append(len.toString());
            b.append(")");// b.append(" CHARACTER SET utf8");
            break;
          }

          if (f == relation.primaryKey) {
            b.append(" UNIQUE ");
          }
          if (f.getNullable()) {
            b.append(" NULL ");
          } else {
            b.append(" NOT NULL ");
          }
        }
        b.append(K_CLOSE_PAREN);

        String createTableStmt = b.toString();
        LogFactory.getLog(DatastoreImpl.class).info("Attempting: " + createTableStmt);

        jc.execute(createTableStmt);
        LogFactory.getLog(DatastoreImpl.class).info(
            "create table success (before updateRelation): " + relation.getTableName());

        String idx;
        // create other indicies
        for (DataField f : relation.getFieldList()) {
          if ((f.getIndexable() != IndexType.NONE) && (f != relation.primaryKey)) {
            idx = relation.getTableName() + "_" + shortPrefix(f.getName());
            createIndex(jc, relation, idx, f);
          }
        }

        // and update the relation with actual dimensions...
        updateRelation(jc, relation, createTableStmt);
        tm.commit(status);
      }
    } catch (Exception e) {
      if (status != null) {
        tm.rollback(status);
      }
      throw new ODKDatastoreException(e);
    }
  }

  /**
   * Construct a 3-character or more prefix for use in the index name.
   *
   * @param name
   * @return
   */
  private String shortPrefix(String name) {
    StringBuilder b = new StringBuilder();
    String[] splits = name.split("_");
    for (int i = 0; i < splits.length; ++i) {
      if (splits[i].length() > 0) {
        b.append(splits[i].charAt(0));
      }
    }
    if (b.length() < 3) {
      b.append(Integer.toString(name.length() % 10));
    }
    return b.toString().toLowerCase();
  }

  private void createIndex(JdbcTemplate jc, CommonFieldsBase tbl, String idxName, DataField field) {
    StringBuilder b = new StringBuilder();

    b.append(K_CREATE_INDEX);
    b.append(K_BQ);
    b.append(idxName);
    b.append(K_BQ);
    b.append(K_ON);
    b.append(K_BQ);
    b.append(tbl.getSchemaName());
    b.append(K_BQ);
    b.append(".");
    b.append(K_BQ);
    b.append(tbl.getTableName());
    b.append(K_BQ);
    if (field.getIndexable() == IndexType.HASH) {
      b.append(K_USING_HASH);
    }
    b.append(" (");
    b.append(K_BQ);
    b.append(field.getName());
    b.append(K_BQ);
    b.append(" )");

    jc.execute(b.toString());
  }

  @Override
  public boolean hasRelation(String schema, String tableName, User user) {
    dam.recordQueryUsage(TableDefinition.INFORMATION_SCHEMA_COLUMNS, 1);
    String qs = TableDefinition.TABLE_EXISTS_QUERY;
    Integer columnCount = getJdbcConnection().queryForObject(qs, new Object[] { schema, tableName }, Integer.class);
    return (columnCount != null && columnCount != 0);
  }

  @Override
  public void dropRelation(CommonFieldsBase relation, User user) throws ODKDatastoreException {
    try {
      StringBuilder b = new StringBuilder();
      b.append(K_DROP_TABLE);
      b.append(K_BQ);
      b.append(relation.getSchemaName());
      b.append(K_BQ);
      b.append(".");
      b.append(K_BQ);
      b.append(relation.getTableName());
      b.append(K_BQ);

      LogFactory.getLog(DatastoreImpl.class).info(
          "Executing " + b.toString() + " by user " + user.getUriUser());
      getJdbcConnection().execute(b.toString());
    } catch (Exception e) {
      LogFactory.getLog(DatastoreImpl.class).warn(relation.getTableName() + " exception: " + e.toString());
      throw new ODKDatastoreException(e);
    }
  }

  /***************************************************************************
   * Entity manipulation APIs
   *
   */

  @SuppressWarnings("unchecked")
  @Override
  public <T extends CommonFieldsBase> T createEntityUsingRelation(T relation, User user) {

    // we are generating our own PK, so we don't need to interact with DB
    // yet...
    T row;
    try {
      row = (T) relation.getEmptyRow(user);
    } catch (Exception e) {
      throw new IllegalArgumentException("failed to create empty row", e);
    }
    return row;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends CommonFieldsBase> T getEntity(T relation, String uri, User user)
      throws ODKEntityNotFoundException {
    Query query = new QueryImpl(relation, "getEntity", this, user);
    query.addFilter(relation.primaryKey, FilterOperation.EQUAL, uri);
    dam.recordGetUsage(relation);
    try {
      List<? extends CommonFieldsBase> results = query.executeQuery();
      if (results == null || results.size() != 1) {
        throw new ODKEntityNotFoundException("Unable to retrieve " + relation.getSchemaName() + "."
            + relation.getTableName() + " key: " + uri);
      }
      return (T) results.get(0);
    } catch (ODKDatastoreException e) {
      throw new ODKEntityNotFoundException("Unable to retrieve " + relation.getSchemaName() + "."
          + relation.getTableName() + " key: " + uri, e);
    }
  }

  @Override
  public Query createQuery(CommonFieldsBase relation, String loggingContextTag, User user) {
    Query query = new QueryImpl(relation, loggingContextTag, this, user);
    return query;
  }

  @Override
  public void putEntity(CommonFieldsBase entity, User user) throws ODKEntityPersistException {
    dam.recordPutUsage(entity);
    try {
      boolean first;
      boolean containsACL = false;
      StringBuilder b = new StringBuilder();
      if (entity.isFromDatabase()) {
        // we need to do an update
        entity.setDateField(entity.lastUpdateDate, new Date());
        entity.setStringField(entity.lastUpdateUriUser, user.getUriUser());

        b.append(K_UPDATE);
        b.append(K_BQ);
        b.append(entity.getSchemaName());
        b.append(K_BQ);
        b.append(".");
        b.append(K_BQ);
        b.append(entity.getTableName());
        b.append(K_BQ);
        b.append(K_SET);

        int idx = 0;
        for (DataField f : entity.getFieldList()) {
          if (f.getName().contains("ACL_ID")) {
            containsACL = true;
            break;
          }
        }

        //size is reduced, because ACL ID is autogenerated
        Object[] ol = new Object[entity.getFieldList().size() - (containsACL ? 1 : 0)];
        int[] il = new int[entity.getFieldList().size() - (containsACL ? 1 : 0)];

        first = true;

        // fields...
        for (DataField f : entity.getFieldList()) {
          // primary key goes in the where clause...
          if (f == entity.primaryKey)
            continue;
          if (f.getName().contains("ACL_ID")) {
            continue;
          }
          if (!first) {
            b.append(K_CS);
          }
          first = false;
          b.append(K_BQ);
          b.append(f.getName());
          b.append(K_BQ);
          b.append(K_EQ);
          b.append(K_BIND_VALUE);

          buildArgumentList(ol, il, idx, entity, f);
          ++idx;
        }
        b.append(K_WHERE);
        b.append(K_BQ);
        b.append(entity.primaryKey.getName());
        b.append(K_BQ);
        b.append(K_EQ);
        b.append(K_BIND_VALUE);
        buildArgumentList(ol, il, idx, entity, entity.primaryKey);

        // update...
        getJdbcConnection().update(b.toString(), ol, il);
      } else {
        // not yet in database -- insert
        b.append(K_INSERT_INTO);
        b.append(K_BQ);
        b.append(entity.getSchemaName());
        b.append(K_BQ);
        b.append(".");
        b.append(K_BQ);
        b.append(entity.getTableName());
        b.append(K_BQ);
        first = true;
        b.append(K_OPEN_PAREN);
        // fields...
        for (DataField f : entity.getFieldList()) {
          if (f.getName().contains("ACL_ID")) {
            containsACL = true;
            continue;
          }
          if (!first) {
            b.append(K_CS);
          }

          first = false;
          b.append(K_BQ);
          b.append(f.getName());
          b.append(K_BQ);
        }
        b.append(K_CLOSE_PAREN);
        b.append(K_VALUES);

        int idx = 0;
        //size is reduced, because ACL ID is autogenerated
        Object[] ol = new Object[entity.getFieldList().size() - (containsACL ? 1 : 0) ];
        int[] il = new int[entity.getFieldList().size() - (containsACL ? 1 : 0)];

        first = true;
        b.append(K_OPEN_PAREN);
        // values
        for (DataField f : entity.getFieldList()) {
          if (f.getName().contains("ACL_ID"))
            continue;
          if (!first) {
            b.append(K_CS);
          }
          first = false;
          b.append(K_BIND_VALUE);

          buildArgumentList(ol, il, idx, entity, f);
          ++idx;
        }
        b.append(K_CLOSE_PAREN);

        // insert...
        getJdbcConnection().update(b.toString(), ol, il);
        entity.setFromDatabase(true); // now it is in the database...
      }
    } catch (Exception e) {
      throw new ODKEntityPersistException(e);
    }
  }

  @Override
  public void putEntities(Collection<? extends CommonFieldsBase> entityList, User user)
      throws ODKEntityPersistException {
    for (CommonFieldsBase d : entityList) {
      putEntity(d, user);
    }
  }

  @Override
  public void batchAlterData(List<? extends CommonFieldsBase> changes, User user)
      throws ODKEntityPersistException {
    if ( changes.isEmpty() ) {
      return;
    }

    boolean generateSQL = true;
    String sql = null;
    int[] argTypes = new int[changes.get(0).getFieldList().size()];
    List<Object[]> batchArgs = new ArrayList<Object[]>();
    StringBuilder b = new StringBuilder();

    for ( CommonFieldsBase entity : changes ) {
      dam.recordPutUsage(entity);

      boolean first;
      b.setLength(0);

      Object[] ol = new Object[entity.getFieldList().size()];

      if (entity.isFromDatabase()) {
        // we need to do an update
        entity.setDateField(entity.lastUpdateDate, new Date());
        entity.setStringField(entity.lastUpdateUriUser, user.getUriUser());

        if ( generateSQL ) {
          b.append(K_UPDATE);
          b.append(K_BQ);
          b.append(entity.getSchemaName());
          b.append(K_BQ);
          b.append(".");
          b.append(K_BQ);
          b.append(entity.getTableName());
          b.append(K_BQ);
          b.append(K_SET);
        }
        
        int idx = 0;
        first = true;
        // fields...
        for (DataField f : entity.getFieldList()) {
          // primary key goes in the where clause...
          if (f == entity.primaryKey)
            continue;
          
          if ( generateSQL ) {
            if (!first) {
              b.append(K_CS);
            }
            first = false;
            b.append(K_BQ);
            b.append(f.getName());
            b.append(K_BQ);
            b.append(K_EQ);
            b.append(K_BIND_VALUE);
          }
          
          buildArgumentList(ol, argTypes, idx, entity, f);
          ++idx;
        }
        if ( generateSQL ) {
          b.append(K_WHERE);
          b.append(K_BQ);
          b.append(entity.primaryKey.getName());
          b.append(K_BQ);
          b.append(K_EQ);
          b.append(K_BIND_VALUE);
        }
        buildArgumentList(ol, argTypes, idx, entity, entity.primaryKey);
        
      } else {
        if ( generateSQL ) {
        // not yet in database -- insert
          b.append(K_INSERT_INTO);
          b.append(K_BQ);
          b.append(entity.getSchemaName());
          b.append(K_BQ);
          b.append(".");
          b.append(K_BQ);
          b.append(entity.getTableName());
          b.append(K_BQ);
          first = true;
          b.append(K_OPEN_PAREN);
          // fields...
          for (DataField f : entity.getFieldList()) {
            if (!first) {
              b.append(K_CS);
            }
            first = false;
            b.append(K_BQ);
            b.append(f.getName());
            b.append(K_BQ);
          }
          b.append(K_CLOSE_PAREN);
          b.append(K_VALUES);
          b.append(K_OPEN_PAREN);
        }

        int idx = 0;
        first = true;
        // fields...
        for (DataField f : entity.getFieldList()) {
          if ( generateSQL ) {
            if (!first) {
              b.append(K_CS);
            }
            first = false;
            b.append(K_BIND_VALUE);
          }
          buildArgumentList(ol, argTypes, idx, entity, f);
          ++idx;
        }
        
        if ( generateSQL ) {
          b.append(K_CLOSE_PAREN);
        }
      }

      if ( generateSQL ) {
        b.append(K_COLON);
        sql = b.toString();
      }
      generateSQL = false;
      batchArgs.add(ol);
    }
    
    try {
      // update...
      getJdbcConnection().batchUpdate(sql, batchArgs, argTypes);
      // if this was an insert, set the fromDatabase flag in the entities
      if ( !changes.get(0).isFromDatabase() ) {
        for ( CommonFieldsBase entity : changes ) {
          entity.setFromDatabase(true);
        }
      }
    } catch (Exception e) {
      throw new ODKEntityPersistException(e);
    }
  }

  @Override
  public void deleteEntity(EntityKey key, User user) throws ODKDatastoreException {

    dam.recordDeleteUsage(key);
    try {
      CommonFieldsBase d = key.getRelation();

      StringBuilder b = new StringBuilder();
      b.append(K_DELETE_FROM);
      b.append(K_BQ);
      b.append(d.getSchemaName());
      b.append(K_BQ);
      b.append(".");
      b.append(K_BQ);
      b.append(d.getTableName());
      b.append(K_BQ);
      b.append(K_WHERE);
      b.append(K_BQ);
      b.append(d.primaryKey.getName());
      b.append(K_BQ);
      b.append(K_EQ);
      b.append(K_BIND_VALUE);

      LogFactory.getLog(DatastoreImpl.class).info(
          "Executing " + b.toString() + " with key " + key.getKey() + " by user "
              + user.getUriUser());
      getJdbcConnection().update(b.toString(), new Object[] { key.getKey() });
    } catch (Exception e) {
      throw new ODKDatastoreException("delete failed", e);
    }
  }

  @Override
  public void deleteEntities(Collection<EntityKey> keys, User user) throws ODKDatastoreException {
    ODKDatastoreException e = null;
    for (EntityKey k : keys) {
      try {
        deleteEntity(k, user);
      } catch (ODKDatastoreException ex) {
        ex.printStackTrace();
        if (e == null) {
          e = ex; // save the first exception...
        }
      }
    }
    if (e != null)
      throw e; // throw the first exception...
  }

  @Override
  public TaskLock createTaskLock(User user) {
    return new TaskLockImpl(this, dam, user);
  }
}
