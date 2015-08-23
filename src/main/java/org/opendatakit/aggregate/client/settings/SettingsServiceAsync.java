package org.opendatakit.aggregate.client.settings;

import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.ArrayList;

/**
 * Created by Anna on 2015-08-23.
 */
public interface SettingsServiceAsync {

  void getSettings(AsyncCallback<ArrayList<AppSettingsSummary>> callback);

}
