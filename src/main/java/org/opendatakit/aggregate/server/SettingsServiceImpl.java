package org.opendatakit.aggregate.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import org.opendatakit.aggregate.ContextFactory;
import org.opendatakit.aggregate.settings.AppSettings;
import org.opendatakit.aggregate.client.settings.AppSettingsSummary;
import org.opendatakit.aggregate.settings.SettingsFactory;
import org.opendatakit.common.persistence.client.exception.DatastoreFailureException;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.security.client.exception.AccessDeniedException;
import org.opendatakit.common.web.CallingContext;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * Created by Anna on 2015-08-23.
 */
public class SettingsServiceImpl extends RemoteServiceServlet implements
    org.opendatakit.aggregate.client.settings.SettingsService {

  public ArrayList<AppSettingsSummary> getSettings() throws AccessDeniedException, DatastoreFailureException {

    HttpServletRequest req = this.getThreadLocalRequest();
    CallingContext cc = ContextFactory.getCallingContext(this, req);
    ArrayList<AppSettingsSummary> settingsSummaries = new ArrayList<AppSettingsSummary>();

    try {
      // ensure that Form table exists...
      List<AppSettings> settings = SettingsFactory.getSettings(false, cc);
      if (settings.size() == 0)
        return settingsSummaries;

      for (AppSettings setting : settings) {
        AppSettingsSummary summary = setting.generateSettingsSummary(cc);
        settingsSummaries.add(summary);
      }
      Collections.sort(settingsSummaries, new Comparator<AppSettingsSummary>() {

        @Override
        public int compare(AppSettingsSummary arg0, AppSettingsSummary arg1) {
          int cmp;
          cmp = arg0.getName().compareTo(arg1.getName());
          return cmp;
        }
      });

      return settingsSummaries;

    } catch (ODKDatastoreException e) {
      e.printStackTrace();
      throw new DatastoreFailureException();
    }
  }
}
