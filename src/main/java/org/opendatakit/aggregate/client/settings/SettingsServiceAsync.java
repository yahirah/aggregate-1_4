package org.opendatakit.aggregate.client.settings;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.opendatakit.aggregate.client.form.MediaFileSummary;

import java.util.ArrayList;

/**
 * @author Anna
 * These are the APIs available to users with the ROLE_DATA_OWNER privilege.
 * Delete & get settings functionality here. For internal use (client <-> server).
 * Implementation is on the server side in "server" package.
 */
public interface SettingsServiceAsync {

  void getSettings(AsyncCallback<ArrayList<AppSettingsSummary>> callback);

  void getSettingsFileList(String name, AsyncCallback<ArrayList<MediaFileSummary>> mediaFileCallback);

  void deleteSettings(String name, AsyncCallback<Void> callback);
}
