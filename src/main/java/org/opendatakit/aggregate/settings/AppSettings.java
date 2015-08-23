package org.opendatakit.aggregate.settings;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.aggregate.client.settings.AppSettingsSummary;
import org.opendatakit.aggregate.constants.HtmlUtil;
import org.opendatakit.aggregate.constants.ServletConsts;
import org.opendatakit.aggregate.parser.MultiPartFormItem;
import org.opendatakit.common.datamodel.BinaryContentManipulator;
import org.opendatakit.common.persistence.CommonFieldsBase;
import org.opendatakit.common.persistence.Datastore;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.security.User;
import org.opendatakit.common.web.CallingContext;
import org.opendatakit.common.web.constants.BasicConsts;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Anna on 2015-08-22.
 */
public class AppSettings {
  private static final Log logger = LogFactory.getLog(AppSettings.class.getName());

  private final AppSettingsFilesetTable settingsRow;

  private final BinaryContentManipulator settings;

  public AppSettings(AppSettingsFilesetTable tSettingsRow, CallingContext cc) throws ODKDatastoreException {
    this.settingsRow = tSettingsRow;
    String topLevelAuri = tSettingsRow.getUri();
    this.settings = AppSettingsFilesetTable.assertSettingsManipulator(topLevelAuri, settingsRow.getUri(), cc);
  }

  public AppSettings(boolean isDownloadEnabled,String settingsType, CallingContext cc) throws ODKDatastoreException {
    Datastore ds = cc.getDatastore();
    User user = cc.getCurrentUser();

    {
      Date now = new Date();
      // get fileset (for now, zero or one record)
      AppSettingsFilesetTable settingsRelation = AppSettingsFilesetTable.assertRelation(cc);
      settingsRow = ds.createEntityUsingRelation(settingsRelation, user);
      settingsRow.setStringField(settingsRow.primaryKey, CommonFieldsBase.newMD5HashUri(settingsRow.getUri()));
      settingsRow.setSubmissionDate(now);
      settingsRow.setMarkedAsCompleteDate(now);
      settingsRow.setIsComplete(true);
      settingsRow.setModelVersion(1L); // rollback (v1.0.x) compatibility
      settingsRow.setUiVersion(0L);    // rollback (v1.0.x) compatibility
      settingsRow.setBooleanField(AppSettingsFilesetTable.IS_DOWNLOAD_ALLOWED, isDownloadEnabled);
      settingsRow.setStringField(AppSettingsFilesetTable.SETTINGS_NAME, settingsType);
      logger.warn("*********************");
      logger.warn("imma creating record");
    }

    this.settings = AppSettingsFilesetTable.assertSettingsManipulator(settingsRow.getUri(),
        settingsRow.getUri(), cc);
  }



  public synchronized void persist(CallingContext cc) throws ODKDatastoreException {
    Datastore ds = cc.getDatastore();
    User user = cc.getCurrentUser();

    ds.putEntity(settingsRow, user);
    settings.persist(cc);

  }


  public boolean hasSettingsFileset(CallingContext cc) throws ODKDatastoreException {
    return settings.getAttachmentCount(cc) != 0;
  }

  public BinaryContentManipulator getSettingsFileset() {
    return settings;
  }


  /**
   * Media files are assumed to be in a directory one level deeper than the xml
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
  private boolean getDownloadEnabled() {
    return settingsRow.getBooleanField(AppSettingsFilesetTable.IS_DOWNLOAD_ALLOWED);
  }

  public String getUri() { return settingsRow.getUri();  }

  public Date getLastUpdateDate() { return settingsRow.getLastUpdateDate();}

  private String getCreationUser() { return settingsRow.getCreatorUriUser(); }
  public Date getCreationDate() { return settingsRow.getCreationDate(); }

  public String getFileName() { return settingsRow.getStringField
      (AppSettingsFilesetTable.SETTINGS_NAME); }

  public AppSettingsSummary generateSettingsSummary(CallingContext cc) throws ODKDatastoreException {
    boolean downloadable = getDownloadEnabled();
    Map<String, String> xmlProperties = new HashMap<String, String>();
    xmlProperties.put(ServletConsts.SETTINGS_NAME, getFileName());
    xmlProperties.put(ServletConsts.HUMAN_READABLE, BasicConsts.TRUE);

    String viewableURL = HtmlUtil.createHrefWithProperties(
        cc.getWebApplicationURL("www/settingsXML"), xmlProperties, getFileName(), false);
     int mediaFileCount = getSettingsFileset().getAttachmentCount(cc);
    return new AppSettingsSummary(getFileName(), getCreationDate(), getCreationUser(),
        downloadable, viewableURL, mediaFileCount);
  }



}
