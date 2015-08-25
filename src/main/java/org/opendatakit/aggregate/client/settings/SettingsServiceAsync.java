package org.opendatakit.aggregate.client.settings;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.opendatakit.aggregate.client.form.MediaFileSummary;

import java.util.ArrayList;

/**
 * Created by Anna on 2015-08-23.
 */
public interface SettingsServiceAsync {

  void getSettings(AsyncCallback<ArrayList<AppSettingsSummary>> callback);

  void getSettingsFileList(String name, AsyncCallback<ArrayList<MediaFileSummary>> mediaFileCallback);

  void deleteSettings(String name, AsyncCallback<Void> callback);
}
