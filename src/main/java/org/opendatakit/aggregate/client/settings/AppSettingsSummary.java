package org.opendatakit.aggregate.client.settings;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Anna
 * Helper class, used for storing information about app settings on client side - mostly for displaying purposes.
 */
public class AppSettingsSummary implements Serializable {

  private static final long serialVersionUID = 5320223139717436812L;
  private String name;
  // creation date is the date the form was uploaded to Aggregate...
  private Date creationDate;


  private String createdUser;
  private boolean download;
  private String viewURL;
  // number of media files associated with this form
  private int mediaFileCount = 1;

  public AppSettingsSummary() {

  }

  public AppSettingsSummary(String sName, Date sCreationDate, String sCreatedUser, boolean sDownload, String sUrl, int
      sCount) {
    this.name = sName;
    this.creationDate = sCreationDate;
    this.createdUser = sCreatedUser;
    this.download = sDownload;
    this.viewURL = sUrl;
    this.mediaFileCount = sCount;
  }


  public static long getSerialVersionUID() {
    return serialVersionUID;
  }

  public String getName() {
    return name;
  }

  public Date getCreationDate() {
    return creationDate;
  }

  public String getCreatedUser() {
    return createdUser;
  }

  public boolean isDownload() {
    return download;
  }

  public String getViewURL() {
    return viewURL;
  }

  public int getMediaFileCount() {
    return mediaFileCount;
  }


  /**
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof AppSettingsSummary)) {
      return false;
    }

    AppSettingsSummary other = (AppSettingsSummary) obj;
    return (name == null ? (other.name == null) : (name.equals(other.name)));
  }

  /**
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    int hashCode = 101;
    if (name != null)
      hashCode += name.hashCode();
    return hashCode;
  }
}
