package org.opendatakit.aggregate.client.table;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import org.opendatakit.aggregate.client.popups.MediaFileListPopup;
import org.opendatakit.aggregate.client.popups.SettingsFileListPopup;
import org.opendatakit.aggregate.client.widgets.*;
import org.opendatakit.aggregate.client.settings.AppSettingsSummary;
import org.opendatakit.common.security.client.UserSecurityInfo;

import java.util.ArrayList;

/**
 * Created by Anna on 2015-08-23.
 */
public class SettingsTable extends FlexTable {


  private static int TITLE_COLUMN = 0;
  private static String TITLE_HEADING = "Title";
  private static int MEDIA_COUNT_COLUMN = 1;
  private static String MEDIA_COUNT_HEADING = "File name";
  private static int USER_COLUMN = 2;
  private static String USER_HEADING = "User";
  private static int DOWNLOADABLE_COLUMN = 3;
  private static String DOWNLOADABLE_HEADING = "Downloadable";
  private static int DELETE_COLUMN = 4;
  private static String DELETE_HEADING = "Delete";

  public SettingsTable() {

    // create table headers
    setText(0, TITLE_COLUMN, TITLE_HEADING);
    setText(0, MEDIA_COUNT_COLUMN, MEDIA_COUNT_HEADING);
    setText(0, USER_COLUMN, USER_HEADING);
    setText(0, DOWNLOADABLE_COLUMN, DOWNLOADABLE_HEADING);
    setText(0, DELETE_COLUMN, DELETE_HEADING);
    // add styling
    getRowFormatter().addStyleName(0, "titleBar");
    addStyleName("dataTable");
  }

  /**
   * Update the list of settings
   *
   * @param AppSettingsSumary
   */
  public void updateSettingsTable(ArrayList<AppSettingsSummary> settings) {
    int i = 0;
    for (int j = 0; j < settings.size(); j++) {
      AppSettingsSummary setting = settings.get(j);
      ++i;
      setWidget(i, TITLE_COLUMN, new HTML(setting.getViewURL()));

      Widget mediaCount;
      if (setting.getMediaFileCount() > 0) {
        Anchor mediaCountLink = new Anchor(Integer.toString(setting.getMediaFileCount()), true);
        mediaCountLink.addClickHandler(new MediaFileListClickHandler(setting.getName()));
        mediaCount = mediaCountLink;
      } else {
        mediaCount = new HTML(Integer.toString(setting.getMediaFileCount()));
      }
      setWidget(i, MEDIA_COUNT_COLUMN, mediaCount);

      setWidget(i, DOWNLOADABLE_COLUMN,
          new HTML(setting.isDownload() ? "yes" : "no"));

      String user = setting.getCreatedUser();
      String displayName = UserSecurityInfo.getDisplayName(user);
      setText(i, USER_COLUMN, displayName);

      //setWidget(i, DOWNLOADABLE_COLUMN,  new DownloadableCheckBox(setting.getId(), setting.isDownloadable()));
      setWidget(i, DELETE_COLUMN, new DeleteSettingsButton(setting.getName()));

      if (i % 2 == 0)
        getRowFormatter().addStyleName(i, "evenTableRow");
    }

    // remove any trailing rows...
    ++i; // to get number or rows in actual table...
    while (getRowCount() > i) {
      removeRow(getRowCount() - 1);
    }
  }

  private class MediaFileListClickHandler implements ClickHandler {

    private String name;

    public MediaFileListClickHandler(String name) {
      this.name = name;
    }

    @Override
    public void onClick(ClickEvent event) {
      SettingsFileListPopup mediaListpopup = new SettingsFileListPopup(name);
      mediaListpopup.setPopupPositionAndShow(mediaListpopup.getPositionCallBack());
    }

  }

}
