package org.opendatakit.aggregate.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.aggregate.ContextFactory;
import org.opendatakit.aggregate.client.exception.FormNotAvailableException;
import org.opendatakit.aggregate.client.exception.RequestFailureException;
import org.opendatakit.aggregate.client.form.MediaFileSummary;
import org.opendatakit.aggregate.constants.BeanDefs;
import org.opendatakit.aggregate.constants.ErrorConsts;
import org.opendatakit.aggregate.exception.ODKFormNotFoundException;
import org.opendatakit.aggregate.form.FormFactory;
import org.opendatakit.aggregate.form.IForm;
import org.opendatakit.aggregate.form.MiscTasks;
import org.opendatakit.aggregate.settings.AppSettings;
import org.opendatakit.aggregate.client.settings.AppSettingsSummary;
import org.opendatakit.aggregate.settings.SettingsFactory;
import org.opendatakit.aggregate.task.FormDelete;
import org.opendatakit.common.datamodel.BinaryContentManipulator;
import org.opendatakit.common.persistence.client.exception.DatastoreFailureException;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.persistence.exception.ODKOverQuotaException;
import org.opendatakit.common.security.client.exception.AccessDeniedException;
import org.opendatakit.common.web.CallingContext;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * Created by Anna on 2015-08-23.
 */
public class SettingsServiceImpl extends RemoteServiceServlet implements
    org.opendatakit.aggregate.client.settings.SettingsService {

  private static final Log logger = LogFactory.getLog(SettingsServiceImpl.class.getName());

  public ArrayList<AppSettingsSummary> getSettings() throws AccessDeniedException, DatastoreFailureException {

    HttpServletRequest req = this.getThreadLocalRequest();
    CallingContext cc = ContextFactory.getCallingContext(this, req);
    ArrayList<AppSettingsSummary> settingsSummaries = new ArrayList<AppSettingsSummary>();

    try {
      // ensure that setting table exists...
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

  public ArrayList<MediaFileSummary> getSettingsFileList(String name) throws DatastoreFailureException {
    HttpServletRequest req = this.getThreadLocalRequest();
    CallingContext cc = ContextFactory.getCallingContext(this, req);

    ArrayList<MediaFileSummary> mediaSummaryList = new ArrayList<MediaFileSummary>();

    try {
      AppSettings setting = SettingsFactory.retrieveSettingsByName(name, cc);

      BinaryContentManipulator bcmS = setting.getSettingsFileset();
      for (int i = 0; i < bcmS.getAttachmentCount(cc); ++i) {
        MediaFileSummary mfs = new MediaFileSummary(bcmS.getUnrootedFilename(i + 1, cc),
            bcmS.getContentType(i + 1, cc), bcmS.getContentLength(i + 1, cc));
        mediaSummaryList.add(mfs);
      }

      Collections.sort(mediaSummaryList, new Comparator<MediaFileSummary>() {

        @Override
        public int compare(MediaFileSummary o1, MediaFileSummary o2) {
          return o1.getFilename().compareToIgnoreCase(o2.getFilename());
        }});

    } catch (ODKDatastoreException e) {
      e.printStackTrace();
      throw new DatastoreFailureException(e);
    } catch (ODKFormNotFoundException e) {
      e.printStackTrace();
      throw new DatastoreFailureException(e);
    }
    return mediaSummaryList;
  }

  public void deleteSettings(String name)  throws AccessDeniedException,
      DatastoreFailureException, RequestFailureException    {
      HttpServletRequest req = this.getThreadLocalRequest();
      CallingContext cc = ContextFactory.getCallingContext(this, req);
      logger.warn("***********DELETING****************" + name);
      try {
        AppSettings as = SettingsFactory.retrieveSettingsByName(name, cc);
        as.deleteSettings(cc);
      } catch (ODKDatastoreException e) {
        e.printStackTrace();
        throw new DatastoreFailureException(e);
      } catch (ODKFormNotFoundException e) {
        e.printStackTrace();
        throw new DatastoreFailureException(e);
      }
  }
}
