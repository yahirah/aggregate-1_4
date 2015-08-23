package org.opendatakit.aggregate.settings;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.aggregate.util.BackendActionsTable;
import org.opendatakit.common.persistence.CommonFieldsBase;
import org.opendatakit.common.persistence.Datastore;
import org.opendatakit.common.persistence.PersistConsts;
import org.opendatakit.common.persistence.Query;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.persistence.exception.ODKOverQuotaException;
import org.opendatakit.common.security.User;
import org.opendatakit.common.web.CallingContext;

import java.util.*;

/**
 * Created by Anna on 2015-08-23.
 */
public class SettingsFactory {

  private static final Log logger = LogFactory.getLog(SettingsFactory.class);
  
  private static long cacheTimestamp = 0L;
  private static final List<AppSettings> cache = new LinkedList<AppSettings>();

  private SettingsFactory() {};

  /**
   * Return the list of settings in the database.
   * If topLevelAuri is null, return all settings. Otherwise, return the setting with the matching URI.
   * This is the main interface to the cache of setting objects.  The cache is refreshed as a whole
   * every PersistConsts.MAX_SETTLE_MILLISECONDS.
   *
   * @param topLevelAuri
   * @param cc
   * @return
   * @throws ODKOverQuotaException
   * @throws ODKDatastoreException
   */
  private static synchronized final List<AppSettings> internalGetSettings(String topLevelAuri, CallingContext cc)
      throws ODKDatastoreException {

    List<AppSettings> settings = new ArrayList<AppSettings>();
    if ( cacheTimestamp + PersistConsts.MAX_SETTLE_MILLISECONDS > System.currentTimeMillis() ) {
      // TODO: This cache should reside in MemCache.  Right now, different running
      // servers might see different Setting definitions for up to the settle time.
      //
      // Since the datastore is treated as having a settle time of MAX_SETTLE_MILLISECONDS,
      // we should rely on the cache for that time interval.  Without MemCache-style
      // support, this is somewhat problematic since different server instances might
      // see different versions of the same Setting.
      //
      logger.info("SettingCache: using cached list of Settings");
    } else {
      // we have a fairly stale list of settings -- interrogate the database
      // for what is really there and update the cache.
      Map<String,AppSettings> oldSettings = new HashMap<String,AppSettings>();
      for ( AppSettings as : cache ) {
        oldSettings.put(as.getUri(), as);
      }
      cache.clear();
      logger.info("SettingCache: fetching new list of Settings");

      Datastore ds = cc.getDatastore();
      User user = cc.getCurrentUser();

      AppSettingsFilesetTable relation = AppSettingsFilesetTable.assertRelation(cc);
      // ensure that Settings table exists...
      Query settingQuery = ds.createQuery(relation, "AppSettings.getSettings", user);
      List<? extends CommonFieldsBase> settingsRows = settingQuery.executeQuery();

      for (CommonFieldsBase cb : settingsRows) {
        AppSettingsFilesetTable settingsRow = (AppSettingsFilesetTable) cb;
        AppSettings as = oldSettings.get(settingsRow.getUri());
        // rely on the fact that a persist updates the last-update-date of the
        // top-level SettingInfoTable even if only subordinate values are updated.
        Date infoDate = settingsRow.getLastUpdateDate();
        Date oldDate = (as == null) ? null : as.getLastUpdateDate();
        if ( as != null && (settingsRow.getCreationDate().equals(as.getCreationDate()))
            && ((infoDate == null && oldDate == null) ||
                (infoDate != null && oldDate != null && infoDate.equals(oldDate))) ) {
          cache.add(as);
        } else {
          logger.info("SettingCache: refreshing setting definition from database: " + settingsRow.getStringField
              (AppSettingsFilesetTable.SETTINGS_NAME));
          // pull and update from the datastore
          as = new AppSettings(settingsRow, cc);
          cache.add(as);
        }
      }

      // sort by setting title then by setting id
      Collections.sort(settings, new Comparator<AppSettings>() {

        @Override
        public int compare(AppSettings o1, AppSettings o2) {
          return o1.getFileName().compareToIgnoreCase(o2.getFileName());
        }});

      // update cacheTimestamp -- note that if the datastore is very slow, this will
      // space out the updates because the cacheTimestamp is established after all
      // the datastore accesses.
      cacheTimestamp = System.currentTimeMillis();

      // test to see if we need to trigger the watchdog
      BackendActionsTable.triggerWatchdog(cc);
    }

    for (AppSettings v : cache) {
      if ( topLevelAuri == null || v.getUri().equals(topLevelAuri) ) {
        settings.add(v);
      }
    }
    return settings;
  }

  public static final List<AppSettings> getSettings(boolean checkAuthorization, CallingContext cc)
      throws ODKDatastoreException {
    List<AppSettings> settings = internalGetSettings(null, cc);
    // TODO: check authorization
    return settings;
  }
}
