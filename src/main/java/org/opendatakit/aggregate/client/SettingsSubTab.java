package org.opendatakit.aggregate.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.opendatakit.aggregate.client.preferences.Preferences;
import org.opendatakit.aggregate.client.settings.AppSettingsSummary;
import org.opendatakit.aggregate.client.table.SettingsTable;
import org.opendatakit.aggregate.client.widgets.ServletPopupButton;
import org.opendatakit.aggregate.constants.common.UIConsts;

import java.util.ArrayList;

/**
 * Created by Anna on 2015-08-22.
 */
public class SettingsSubTab extends  AggregateSubTabBase {

  private static final String NEW_SETTINGS_TXT = "Add New Settings";
  private static final String NEW_SETTINGS_TOOLTIP_TXT = "Upload NEW Settings";
  private static final String NEW_SETTINGS_BALLOON_TXT = "Upload a NEW settings to Aggregate.";
  private static final String NEW_SETTINGS_BUTTON_TEXT = "<img src=\"images/yellow_plus.png\" /> "
      + NEW_SETTINGS_TXT;

  private boolean showEnketoIntegration;
  private SettingsTable listOfSettings;

  public SettingsSubTab(AggregateUI baseUI) {
    // vertical
    setStylePrimaryName(UIConsts.VERTICAL_FLOW_PANEL_STYLENAME);

    // create navigation buttons to servlet

    ServletPopupButton newSettings = new ServletPopupButton(NEW_SETTINGS_BUTTON_TEXT, NEW_SETTINGS_TXT,
        UIConsts.SETTINGS_UPLOAD_SERVLET_ADDR, this, NEW_SETTINGS_TOOLTIP_TXT, NEW_SETTINGS_BALLOON_TXT);

    // save the webform setting
    showEnketoIntegration = Preferences.showEnketoIntegration();
    // create settings list

    listOfSettings = new SettingsTable();
    listOfSettings.getElement().setId("settings_management_table");

    // add tables to panels
    add(newSettings);
    add(listOfSettings);

  }

  @Override
  public boolean canLeave() {
    return true;
  }

  @Override
  public void update() {
    // Set up the callback object.
    AsyncCallback<ArrayList<AppSettingsSummary>> callback = new AsyncCallback<ArrayList<AppSettingsSummary>>() {
      public void onFailure(Throwable caught) {
        AggregateUI.getUI().reportError(caught);
      }
      public void onSuccess(ArrayList<AppSettingsSummary> settings) {
        AggregateUI.getUI().clearError();
        boolean resizeFormTable = true;
        boolean newShowEnketoIntegration = Preferences.showEnketoIntegration();
        if ( newShowEnketoIntegration != showEnketoIntegration ) {
          resizeFormTable = true;
          SettingsTable t = listOfSettings;
          @SuppressWarnings("unused")
          boolean success = true;
          if ( t != null ) {
            listOfSettings = null;
            success = remove(t);
          }
          showEnketoIntegration = newShowEnketoIntegration;
          listOfSettings = new SettingsTable();
          listOfSettings.getElement().setId("form_management_table");
          add(listOfSettings);
        }
        listOfSettings.updateSettingsTable(settings);
        if ( resizeFormTable ) {
          // we need to force FormTable to be full width
          AggregateUI.resize();
        }
      }
    };

    // Make the call to the form service.
    SecureGWT.getSettingsService().getSettings(callback);


  }
}
