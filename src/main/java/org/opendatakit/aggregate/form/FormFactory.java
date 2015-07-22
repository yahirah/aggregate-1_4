/*
 * Copyright (C) 2011 University of Washington.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opendatakit.aggregate.form;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.aggregate.exception.ODKConversionException;
import org.opendatakit.aggregate.exception.ODKFormNotFoundException;
import org.opendatakit.aggregate.parser.FormParserForJavaRosa;
import org.opendatakit.aggregate.submission.SubmissionKeyPart;
import org.opendatakit.aggregate.util.BackendActionsTable;
import org.opendatakit.common.persistence.CommonFieldsBase;
import org.opendatakit.common.persistence.Datastore;
import org.opendatakit.common.persistence.PersistConsts;
import org.opendatakit.common.persistence.Query;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.persistence.exception.ODKEntityNotFoundException;
import org.opendatakit.common.persistence.exception.ODKOverQuotaException;
import org.opendatakit.common.security.User;
import org.opendatakit.common.web.CallingContext;

/**
 * Factory class for managing Form objects.
 * Does caching of the forms so as to minimize the number of database accesses.
 *
 * @author mitchellsundt@gmail.com
 *
 */
public class FormFactory {

  private static final Log logger = LogFactory.getLog(FormFactory.class);

  private static long cacheTimestamp = 0L;
  private static final List<IForm> cache = new LinkedList<IForm>();

  private FormFactory() {};

  /**
   * Return the list of forms in the database.
   * If topLevelAuri is null, return all forms. Otherwise, return the form with the matching URI.
   * This is the main interface to the cache of form objects.  The cache is refreshed as a whole
   * every PersistConsts.MAX_SETTLE_MILLISECONDS.
   *
   * @param topLevelAuri
   * @param cc
   * @return
   * @throws ODKOverQuotaException
   * @throws ODKDatastoreException
   */
  private static synchronized final List<IForm> internalGetForms(String topLevelAuri, CallingContext cc)
      throws ODKOverQuotaException, ODKDatastoreException {

    List<IForm> forms = new ArrayList<IForm>();
    if ( cacheTimestamp + PersistConsts.MAX_SETTLE_MILLISECONDS > System.currentTimeMillis() ) {
      // TODO: This cache should reside in MemCache.  Right now, different running
      // servers might see different Form definitions for up to the settle time.
      //
      // Since the datastore is treated as having a settle time of MAX_SETTLE_MILLISECONDS,
      // we should rely on the cache for that time interval.  Without MemCache-style
      // support, this is somewhat problematic since different server instances might
      // see different versions of the same Form.
      //
      logger.info("FormCache: using cached list of Forms");
    } else {
      // we have a fairly stale list of forms -- interrogate the database
      // for what is really there and update the cache.
      Map<String,IForm> oldForms = new HashMap<String,IForm>();
      for ( IForm f : cache ) {
        oldForms.put(f.getUri(), f);
      }
      cache.clear();
      logger.info("FormCache: fetching new list of Forms");

      Datastore ds = cc.getDatastore();
      User user = cc.getCurrentUser();

      FormInfoTable relation = FormInfoTable.assertRelation(cc);
      // ensure that Form table exists...
      Query formQuery = ds.createQuery(relation, "Form.getForms", user);
      List<? extends CommonFieldsBase> infoRows = formQuery.executeQuery();

      for (CommonFieldsBase cb : infoRows) {
        FormInfoTable infoRow = (FormInfoTable) cb;
        IForm f = oldForms.get(infoRow.getUri());
        // rely on the fact that a persist updates the last-update-date of the
        // top-level FormInfoTable even if only subordinate values are updated.
        Date infoDate = infoRow.getLastUpdateDate();
        Date oldDate = (f == null) ? null : f.getLastUpdateDate();
        if ( f != null &&  f.hasValidFormDefinition() &&
            (infoRow.getCreationDate().equals(f.getCreationDate())) &&
            ((infoDate == null && oldDate == null) ||
             (infoDate != null && oldDate != null && infoDate.equals(oldDate))) ) {
          cache.add(f);
        } else {
          logger.info("FormCache: refreshing form definition from database: " + infoRow.getStringField(FormInfoTable.FORM_ID));
          // pull and update from the datastore
          f = new Form(infoRow, cc);
          cache.add(f);
        }
      }

      // sort by form title then by form id
      Collections.sort(forms, new Comparator<IForm>() {

        @Override
        public int compare(IForm o1, IForm o2) {
          int ref = o1.getViewableName().compareToIgnoreCase(o2.getViewableName());
          if ( ref != 0 ) return ref;
          return o1.getFormId().compareToIgnoreCase(o2.getFormId());
        }});

      // update cacheTimestamp -- note that if the datastore is very slow, this will
      // space out the updates because the cacheTimestamp is established after all
      // the datastore accesses.
      cacheTimestamp = System.currentTimeMillis();

      // test to see if we need to trigger the watchdog
      BackendActionsTable.triggerWatchdog(cc);
    }

    for (IForm v : cache) {
      if ( topLevelAuri == null || v.getUri().equals(topLevelAuri) ) {
        forms.add(v);
      }
    }
    return forms;
  }

  public static synchronized void clearForm(IForm match) {
    // NOTE: delays refresh of the forms list by the settle time.
    cache.remove(match);
    cacheTimestamp = System.currentTimeMillis();
  }

  /**
   * Common private static method through which all Form objects are obtained.
   * This provides a cache of the form data.  If known, the top-level object's
   * row object is passed in.  This is a database access optimization (minimize
   * GAE billing).
   *
   * @param topLevelAuri
   * @param cc
   * @return
   * @throws ODKOverQuotaException
   * @throws ODKEntityNotFoundException
   * @throws ODKDatastoreException
   */
  private static IForm getForm(String topLevelAuri, CallingContext cc)
      throws ODKOverQuotaException, ODKEntityNotFoundException, ODKDatastoreException {
    List<IForm> forms = internalGetForms(topLevelAuri, cc);

    if ( forms.isEmpty() ) throw new ODKEntityNotFoundException("Could not retrieve form uri: " + topLevelAuri);
    IForm f = forms.get(0);
    // TODO: check authorization?
    return f;
  }

  public static final List<IForm> getForms(boolean checkAuthorization, CallingContext cc)
      throws ODKOverQuotaException, ODKDatastoreException {
    List<IForm> forms = internalGetForms(null, cc);
    // TODO: check authorization
    return forms;
  }

  /**
   * Called during the startup action to load the Form table and eventually
   * handle migrations of forms from older table formats to newer ones.
   *
   * @param cc
   * @throws ODKDatastoreException
   */
  public static final void initialize(CallingContext cc) throws ODKDatastoreException {
    internalGetForms(null, cc);
  }

  /**
   * Clean up the incoming string to extract just the formId from it.
   *
   * @param submissionKey
   * @return
   */
  public static final String extractWellFormedFormId(String submissionKey) {
    int firstSlash = submissionKey.indexOf('/');
    String formId = submissionKey;
    if (firstSlash != -1) {
      // strip off the group path of the key
      formId = submissionKey.substring(0, firstSlash);
    }
    return formId;
  }

  /**
   * Static function to retrieve a form with the specified ODK id from the
   * datastore
   *
   * @param formId
   *          The ODK identifier that identifies the form
   *
   * @return The ODK aggregate form definition/conversion object
   *
   * @throws ODKOverQuotaException
   * @throws ODKDatastoreException
   * @throws ODKFormNotFoundException
   *           Thrown when a form was not able to be found with the
   *           corresponding ODK ID
   */
  public static IForm retrieveFormByFormId(String formId, CallingContext cc)
      throws ODKFormNotFoundException, ODKOverQuotaException, ODKDatastoreException {

    if (formId == null) {
      return null;
    }
    try {
      String formUri = CommonFieldsBase.newMD5HashUri(formId);
      IForm form = getForm(formUri, cc);
      if (!formId.equals(form.getFormId())) {
        throw new IllegalStateException("more than one FormInfo entry for the given form id: "
            + formId);
      }
      return form;
    } catch (ODKOverQuotaException e) { // datastore exception
      throw e;
    } catch (ODKEntityNotFoundException e) { // datastore exception
      throw new ODKFormNotFoundException(e);
    } catch (ODKDatastoreException e) {
      throw e;
    } catch (Exception e) {
      throw new ODKFormNotFoundException(e);
    }
  }

  /**
   * Static function to retrieve a form with the specified ODK id from the
   * datastore
   *
   * @param formId
   *          The ODK identifier that identifies the form
   *
   * @return The ODK aggregate form definition/conversion object
   *
   * @throws ODKOverQuotaException
   * @throws ODKDatastoreException
   * @throws ODKFormNotFoundException
   *           Thrown when a form was not able to be found with the
   *           corresponding ODK ID
   */
  public static IForm retrieveForm(List<SubmissionKeyPart> parts, CallingContext cc)
      throws ODKOverQuotaException, ODKDatastoreException, ODKFormNotFoundException {

    if (!FormInfo.validFormKey(parts)) {
      return null;
    }

    try {
      String formUri = parts.get(1).getAuri();
      IForm form = getForm(formUri, cc);
      return form;
    } catch ( ODKOverQuotaException e) { // datastore exception
      throw e;
    } catch ( ODKEntityNotFoundException e) { // datastore exception
      throw new ODKFormNotFoundException(e);
    } catch ( ODKDatastoreException e) {
      throw e;
    } catch (Exception e) {
      throw new ODKFormNotFoundException(e);
    }
  }

  /**
   * Called only from FormParserForJavaRosa.  The form should not already exist.
   * Returns the new form.
   *
   * @param incomingFormXml
   * @param rootElementDefn
   * @param isEncryptedForm
   * @param isDownloadEnabled
   * @param title
   * @param cc
   * @return
   * @throws ODKDatastoreException
   * @throws ODKConversionException
   */
  public static IForm createFormId(String incomingFormXml, XFormParameters rootElementDefn,
        boolean isEncryptedForm, boolean isDownloadEnabled, String title, CallingContext cc)
            throws ODKDatastoreException {
    IForm thisForm = null;

    String formUri = CommonFieldsBase.newMD5HashUri(rootElementDefn.formId);
    try {
      thisForm = getForm(formUri, cc); // this SHOULD throw an exception!!!
    } catch (ODKEntityNotFoundException e) {
      thisForm = new Form(rootElementDefn, isEncryptedForm, isDownloadEnabled, title, cc);
      FormParserForJavaRosa.updateFormXmlVersion(thisForm, incomingFormXml, rootElementDefn.modelVersion, cc);
    }
    return thisForm;
  }
}

