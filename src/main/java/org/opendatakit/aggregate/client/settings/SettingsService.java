package org.opendatakit.aggregate.client.settings;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import org.opendatakit.aggregate.client.exception.FormNotAvailableException;
import org.opendatakit.aggregate.client.exception.RequestFailureException;
import org.opendatakit.aggregate.client.form.MediaFileSummary;
import org.opendatakit.common.persistence.client.exception.DatastoreFailureException;
import org.opendatakit.common.security.client.exception.AccessDeniedException;

import java.util.ArrayList;

/**
 * @author Anna
 * These are the APIs available to users with the ROLE_DATA_OWNER privilege.
 * Delete & get settings functionality here. For internal use (client <-> server).
 *
 */
@RemoteServiceRelativePath("settingsservice")
public interface SettingsService extends RemoteService {

  ArrayList<AppSettingsSummary> getSettings() throws AccessDeniedException, DatastoreFailureException;

  ArrayList<MediaFileSummary> getSettingsFileList(String name) throws DatastoreFailureException;

  void deleteSettings(String name) throws AccessDeniedException, FormNotAvailableException, DatastoreFailureException, RequestFailureException;

}
