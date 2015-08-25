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
 * Created by Anna on 2015-08-23.
 */
/**
 * These are the APIs available to users with the ROLE_DATA_OWNER privilege.
 * Adding settings, settings forms, and other forms settings are here.
 *
 *
 */
@RemoteServiceRelativePath("settingsservice")
public interface SettingsService extends RemoteService {

  ArrayList<AppSettingsSummary> getSettings() throws AccessDeniedException, DatastoreFailureException;

  ArrayList<MediaFileSummary> getSettingsFileList(String name) throws DatastoreFailureException;

  void deleteSettings(String name) throws AccessDeniedException, FormNotAvailableException, DatastoreFailureException, RequestFailureException;

}
