package org.opendatakit.aggregate.settings;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.aggregate.client.settings.AppSettingsSummary;
import org.opendatakit.aggregate.constants.HtmlUtil;
import org.opendatakit.aggregate.constants.ServletConsts;
import org.opendatakit.aggregate.parser.MultiPartFormItem;
import org.opendatakit.aggregate.servlet.SettingsXmlServlet;
import org.opendatakit.common.datamodel.BinaryContentManipulator;
import org.opendatakit.common.persistence.CommonFieldsBase;
import org.opendatakit.common.persistence.Datastore;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.security.User;
import org.opendatakit.common.web.CallingContext;
import org.opendatakit.common.web.constants.BasicConsts;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class that wraps the concept of the app settings, storing both entity with settings description (@see
 * #settingsRow) and file handler (@see #settings). Works as Data Access Object with CRUD functionalities.
 * @author Anna
 */
public class AppSettings {
  private static final Log logger = LogFactory.getLog(AppSettings.class.getName());

  private final AppSettingsFilesetTable settingsRow;
  private final BinaryContentManipulator settings;

  /**
   * Constructor used for internal purposes of fetching the data & wrapping it in this helper.
   * @param tSettingsRow
   * @param cc
   * @throws ODKDatastoreException
   */
  public AppSettings(AppSettingsFilesetTable tSettingsRow, CallingContext cc) throws ODKDatastoreException {
    this.settingsRow = tSettingsRow;
    String topLevelAuri = tSettingsRow.getUri();
    this.settings = AppSettingsFilesetTable.assertSettingsManipulator(topLevelAuri, settingsRow.getUri(), cc);
  }

  /**
   * Constructor used when creating new app settings from uploaded xml file.
   * @param isDownloadEnabled
   * @param settingsType
   * @param cc
   * @throws ODKDatastoreException
   */
  public AppSettings(boolean isDownloadEnabled,String settingsType, CallingContext cc) throws ODKDatastoreException {
    Datastore ds = cc.getDatastore();
    User user = cc.getCurrentUser();

    {
      Date now = new Date();
      // get fileset (for now, zero or one record)
      AppSettingsFilesetTable settingsRelation = AppSettingsFilesetTable.assertRelation(cc);
      settingsRow = ds.createEntityUsingRelation(settingsRelation, user);
      String primaryKey = CommonFieldsBase.newMD5HashUri(settingsType);
      logger.warn("Creating settings: " + settingsType + " with key: " + primaryKey);
      settingsRow.setStringField(settingsRow.primaryKey, primaryKey);
      settingsRow.setSubmissionDate(now);
      settingsRow.setMarkedAsCompleteDate(now);
      settingsRow.setIsComplete(true);
      settingsRow.setModelVersion(1L); // rollback (v1.0.x) compatibility
      settingsRow.setUiVersion(0L);    // rollback (v1.0.x) compatibility
      settingsRow.setBooleanField(AppSettingsFilesetTable.IS_DOWNLOAD_ALLOWED, isDownloadEnabled);
      settingsRow.setStringField(AppSettingsFilesetTable.SETTINGS_NAME, settingsType);
    }

    this.settings = AppSettingsFilesetTable.assertSettingsManipulator(settingsRow.getUri(),
        settingsRow.getUri(), cc);
  }

  private boolean getDownloadEnabled() {
    return settingsRow.getBooleanField(AppSettingsFilesetTable.IS_DOWNLOAD_ALLOWED);
  }

  public String getUri() { return settingsRow.getUri();  }

  public Date getLastUpdateDate() { return settingsRow.getLastUpdateDate();}

  private String getCreationUser() { return settingsRow.getCreatorUriUser(); }
  public Date getCreationDate() { return settingsRow.getCreationDate(); }

  public String getFileName() { return settingsRow.getStringField
      (AppSettingsFilesetTable.SETTINGS_NAME); }


  /**
   * Operation of saving changes (create/update) in database.
   * @param cc
   * @throws ODKDatastoreException
   */
  public synchronized void persist(CallingContext cc) throws ODKDatastoreException {
    Datastore ds = cc.getDatastore();
    User user = cc.getCurrentUser();
    ds.putEntity(settingsRow, user);
    settings.persist(cc);

  }

  /**
   * Deleting from database, both description and file.
   * @param cc
   * @throws ODKDatastoreException
   */
  public synchronized void deleteSettings(CallingContext cc) throws ODKDatastoreException {
    SettingsFactory.clearSettings(this);

    Datastore ds = cc.getDatastore();
    User user = cc.getCurrentUser();
    // delete everything in formInfo

    settings.deleteAll(cc);
    ds.deleteEntity(settingsRow.getEntityKey(), user);
  }

  /**
   * Checks, if there is any file connected to this settings.
   * @param cc
   * @return
   * @throws ODKDatastoreException
   */
  public boolean hasSettingsFileset(CallingContext cc) throws ODKDatastoreException {
    return settings.getAttachmentCount(cc) != 0;
  }

  public BinaryContentManipulator getSettingsFileset() {
    return settings;
  }


  /**
   * Files are assumed to be in a directory one level deeper than the xml
   * definition. So the filename reported on the mime item has an extra leading
   * directory. Strip that off.
   *
   * @param item
   * @param overwriteOK
   * @param cc
   * @return true if a file should be overwritten (updated); false if the file is completely new or unchanged.
   * @throws ODKDatastoreException
   */
  public boolean setSettingsFile(MultiPartFormItem item, boolean overwriteOK, CallingContext cc) throws
      ODKDatastoreException {
    String filePath = item.getFilename();
    if (filePath.indexOf("/") != -1) {
      filePath = filePath.substring(filePath.indexOf("/") + 1);
    }
    byte[] byteArray = item.getStream().toByteArray();
    BinaryContentManipulator.BlobSubmissionOutcome outcome =
        settings.setValueFromByteArray(byteArray, item.getContentType(), filePath, overwriteOK, cc);
    return (outcome == BinaryContentManipulator.BlobSubmissionOutcome.NEW_FILE_VERSION);
  }

  /**
   *
   * @param cc
   * @return summarison of this App Settings in form of @see org.opendatakit.aggregate.client.settings
   * .AppSettingsSummary .
   * @throws ODKDatastoreException
   */
  public AppSettingsSummary generateSettingsSummary(CallingContext cc) throws ODKDatastoreException {
    boolean downloadable = getDownloadEnabled();
    Map<String, String> xmlProperties = new HashMap<String, String>();
    xmlProperties.put(ServletConsts.SETTINGS_NAME, getFileName());
    xmlProperties.put(ServletConsts.HUMAN_READABLE, BasicConsts.TRUE);

    String viewableURL = HtmlUtil.createHrefWithProperties(
        cc.getWebApplicationURL(SettingsXmlServlet.WWW_ADDR), xmlProperties, getFileName(), false);
     int mediaFileCount = getSettingsFileset().getAttachmentCount(cc);
    return new AppSettingsSummary(getFileName(), getCreationDate(), getCreationUser(),
        downloadable, viewableURL, mediaFileCount);
  }

  /**
   *
   * @param cc
   * @return xml of the settings file as String
   * @throws ODKDatastoreException
   */
  public String getSettingsXml(CallingContext cc) throws ODKDatastoreException {
    if (settings.getAttachmentCount(cc) == 1) {
      if (settings.getContentHash(1, cc) == null) {
        return null;
      }
      byte[] byteArray = settings.getBlob(1, cc);
      try {
        return new String(byteArray, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
        throw new IllegalStateException("UTF-8 charset not supported!");
      }
    } else if (settings.getAttachmentCount(cc) > 1) {
      throw new IllegalStateException("Expecting only one fileset record at this time!");
    }
    return null;
  }

  /**
   * Settings flags & data to prepare the record to be updated
   * @param value - new settings file
   * @param cc
   * @throws ODKDatastoreException
   */
  public void updateSettings(MultiPartFormItem value, CallingContext cc) throws ODKDatastoreException {
    Date now = new Date();
    settingsRow.setFromDatabase(true);
    settingsRow.setSubmissionDate(now);
    setSettingsFile(value, true, cc);

  }
}
