/*
 * Copyright (C) 2011 University of Washington
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

package org.opendatakit.aggregate.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.opendatakit.aggregate.ContextFactory;
import org.opendatakit.aggregate.client.exception.FormNotAvailableException;
import org.opendatakit.aggregate.client.exception.RequestFailureException;
import org.opendatakit.aggregate.client.filter.FilterGroup;
import org.opendatakit.aggregate.client.form.MediaFileSummary;
import org.opendatakit.aggregate.client.submission.SubmissionUI;
import org.opendatakit.aggregate.client.submission.SubmissionUISummary;
import org.opendatakit.aggregate.constants.BeanDefs;
import org.opendatakit.aggregate.constants.ErrorConsts;
import org.opendatakit.aggregate.datamodel.FormElementModel;
import org.opendatakit.aggregate.exception.ODKFormNotFoundException;
import org.opendatakit.aggregate.externalservice.FormServiceCursor;
import org.opendatakit.aggregate.form.FormFactory;
import org.opendatakit.aggregate.form.IForm;
import org.opendatakit.aggregate.form.MiscTasks;
import org.opendatakit.aggregate.form.MiscTasks.TaskType;
import org.opendatakit.aggregate.format.Row;
import org.opendatakit.aggregate.format.element.ElementFormatter;
import org.opendatakit.aggregate.format.element.UiElementFormatter;
import org.opendatakit.aggregate.process.DeleteSubmissions;
import org.opendatakit.aggregate.query.submission.QueryByUIFilterGroup;
import org.opendatakit.aggregate.query.submission.QueryByUIFilterGroup.CompletionFlag;
import org.opendatakit.aggregate.submission.Submission;
import org.opendatakit.aggregate.submission.SubmissionElement;
import org.opendatakit.aggregate.submission.SubmissionKey;
import org.opendatakit.aggregate.submission.SubmissionKeyPart;
import org.opendatakit.aggregate.submission.SubmissionSet;
import org.opendatakit.aggregate.submission.SubmissionValue;
import org.opendatakit.aggregate.submission.SubmissionVisitor;
import org.opendatakit.aggregate.submission.type.BlobSubmissionType;
import org.opendatakit.aggregate.task.FormDelete;
import org.opendatakit.aggregate.task.PurgeOlderSubmissions;
import org.opendatakit.common.datamodel.BinaryContentManipulator;
import org.opendatakit.common.persistence.client.exception.DatastoreFailureException;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.persistence.exception.ODKEntityNotFoundException;
import org.opendatakit.common.persistence.exception.ODKOverQuotaException;
import org.opendatakit.common.security.client.exception.AccessDeniedException;
import org.opendatakit.common.utils.WebUtils;
import org.opendatakit.common.web.CallingContext;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class FormAdminServiceImpl extends RemoteServiceServlet implements
    org.opendatakit.aggregate.client.form.FormAdminService {

  /**
	 * 
	 */
  private static final long serialVersionUID = -2513124088714784947L;

  @Override
  public void setFormDownloadable(String formId, Boolean downloadable) throws AccessDeniedException, FormNotAvailableException, RequestFailureException, DatastoreFailureException {
    HttpServletRequest req = this.getThreadLocalRequest();
    CallingContext cc = ContextFactory.getCallingContext(this, req);

    try {
      IForm form = FormFactory.retrieveFormByFormId(formId, cc);
      if (!form.hasValidFormDefinition()) {
        throw new RequestFailureException(ErrorConsts.FORM_DEFINITION_INVALID); // ill-formed definition
      }
      form.setDownloadEnabled(downloadable);
      form.persist(cc);
    } catch (ODKFormNotFoundException e) {
      e.printStackTrace();
      throw new FormNotAvailableException(e);
    } catch (ODKOverQuotaException e) {
      e.printStackTrace();
      throw new RequestFailureException(ErrorConsts.QUOTA_EXCEEDED);
    } catch (ODKDatastoreException e) {
      e.printStackTrace();
      throw new DatastoreFailureException(e);
    }
  }

  @Override
  public void setFormAcceptSubmissions(String formId, Boolean acceptSubmissions) throws AccessDeniedException, FormNotAvailableException, RequestFailureException, DatastoreFailureException {
    HttpServletRequest req = this.getThreadLocalRequest();
    CallingContext cc = ContextFactory.getCallingContext(this, req);

    try {
      IForm form = FormFactory.retrieveFormByFormId(formId, cc);
      if (!form.hasValidFormDefinition()) {
        throw new RequestFailureException(ErrorConsts.FORM_DEFINITION_INVALID); // ill-formed definition
      }
      form.setSubmissionEnabled(acceptSubmissions);
      form.persist(cc);
    } catch (ODKFormNotFoundException e) {
      e.printStackTrace();
      throw new FormNotAvailableException(e);
    } catch (ODKOverQuotaException e) {
      e.printStackTrace();
      throw new RequestFailureException(ErrorConsts.QUOTA_EXCEEDED);
    } catch (ODKDatastoreException e) {
      e.printStackTrace();
      throw new DatastoreFailureException(e);
    }
  }

  public Date purgePublishedData(String uriExternalService, Date earliest)
      throws AccessDeniedException, FormNotAvailableException, DatastoreFailureException, RequestFailureException {
    HttpServletRequest req = this.getThreadLocalRequest();
    CallingContext cc = ContextFactory.getCallingContext(this, req);

    FormServiceCursor fsc;
    try {
      fsc = FormServiceCursor.getFormServiceCursor(uriExternalService, cc);
    } catch (ODKEntityNotFoundException e) {
      e.printStackTrace();
      throw new RequestFailureException("Unable to retrieve publishing configuration");
    } catch (ODKOverQuotaException e) {
      e.printStackTrace();
      throw new RequestFailureException(ErrorConsts.QUOTA_EXCEEDED);
    } catch (ODKDatastoreException e) {
      e.printStackTrace();
      throw new DatastoreFailureException(e);
    }

    // any confirm parameter value means OK -- purge it!
    PurgeOlderSubmissions pos = (PurgeOlderSubmissions) cc
        .getBean(BeanDefs.PURGE_OLDER_SUBMISSIONS_BEAN);

    if (pos == null) {
      throw new RequestFailureException("Unable to configure task to purge submitted data for form " + fsc.getFormId());
    }
    // set up the purge request here...
    Map<String, String> parameters = new HashMap<String, String>();

    parameters.put(PurgeOlderSubmissions.PURGE_DATE, WebUtils.purgeDateString(earliest));
    IForm form;
    try {
      form = FormFactory.retrieveFormByFormId(fsc.getFormId(), cc);
      if (!form.hasValidFormDefinition()) {
        throw new RequestFailureException(ErrorConsts.FORM_DEFINITION_INVALID);
      }
    } catch (ODKFormNotFoundException e) {
      e.printStackTrace();
      throw new FormNotAvailableException(e);
    } catch (ODKOverQuotaException e) {
      e.printStackTrace();
      throw new RequestFailureException(ErrorConsts.QUOTA_EXCEEDED);
    } catch (ODKDatastoreException e) {
      e.printStackTrace();
      throw new DatastoreFailureException(e);
    }

    MiscTasks m;
    try {
      m = new MiscTasks(TaskType.PURGE_OLDER_SUBMISSIONS, form, parameters, cc);
      m.persist(cc);
    } catch (ODKOverQuotaException e) {
      e.printStackTrace();
      throw new RequestFailureException(ErrorConsts.QUOTA_EXCEEDED);
    } catch (ODKDatastoreException e) {
      e.printStackTrace();
      throw new RequestFailureException(
          "Unable to establish task to purge submitted data for form " + fsc.getFormId());
    }
    CallingContext ccDaemon = ContextFactory.getCallingContext(this, req);
    ccDaemon.setAsDaemon(true);
    try {
      pos.createPurgeOlderSubmissionsTask(form, m.getSubmissionKey(), 1L, ccDaemon);
    } catch (ODKOverQuotaException e) {
      e.printStackTrace();
      throw new RequestFailureException(ErrorConsts.QUOTA_EXCEEDED);
    } catch (ODKDatastoreException e) {
      e.printStackTrace();
      throw new RequestFailureException(
          "Unable to establish task to purge submitted data for form " + fsc.getFormId());
    } catch (ODKFormNotFoundException e) {
      e.printStackTrace();
      throw new RequestFailureException(
          "Unable to establish task to purge submitted data for form " + fsc.getFormId());
    }
    return earliest;
  }

  @Override
  public void deleteForm(String formId) throws AccessDeniedException, FormNotAvailableException, DatastoreFailureException, RequestFailureException {
    HttpServletRequest req = this.getThreadLocalRequest();
    CallingContext cc = ContextFactory.getCallingContext(this, req);

    try {
      FormDelete formDelete = (FormDelete) cc.getBean(BeanDefs.FORM_DELETE_BEAN);
      if (formDelete == null) {
        throw new RequestFailureException("Unable to configure task to delete form " + formId);
      }

      IForm formToDelete = FormFactory.retrieveFormByFormId(formId, cc);

      // If the FormInfo table is the target, log an error!
      if (formToDelete != null) {
        MiscTasks m = new MiscTasks(TaskType.DELETE_FORM, formToDelete, null, cc);
        m.persist(cc);
        CallingContext ccDaemon = ContextFactory.getCallingContext(this, req);
        ccDaemon.setAsDaemon(true);
        formDelete.createFormDeleteTask(formToDelete, m.getSubmissionKey(), 1L, ccDaemon);
      }
    } catch (ODKOverQuotaException e) {
      e.printStackTrace();
      throw new RequestFailureException(ErrorConsts.QUOTA_EXCEEDED);
    } catch (ODKFormNotFoundException e) {
      e.printStackTrace();
      throw new FormNotAvailableException(e);
    } catch (ODKDatastoreException e) {
      e.printStackTrace();
      throw new DatastoreFailureException(e);
    }
  }

  @Override
  public void deleteSubmission(String submissionKeyAsString) throws AccessDeniedException, DatastoreFailureException, FormNotAvailableException, RequestFailureException {
    HttpServletRequest req = this.getThreadLocalRequest();
    CallingContext cc = ContextFactory.getCallingContext(this, req);

    // create a list because the submission deleter require a list
    SubmissionKey subKey = new SubmissionKey(submissionKeyAsString);
    List<SubmissionKey> keyList = new ArrayList<SubmissionKey>();
    keyList.add(subKey);

    // delete the submission
    try {
      DeleteSubmissions deleter = new DeleteSubmissions(keyList);
      deleter.deleteSubmissions(cc);
    } catch (ODKOverQuotaException e) {
      e.printStackTrace();
      throw new RequestFailureException(ErrorConsts.QUOTA_EXCEEDED);
    } catch (ODKFormNotFoundException e) {
      e.printStackTrace();
      throw new FormNotAvailableException(e);
    } catch (ODKDatastoreException e) {
      e.printStackTrace();
      throw new DatastoreFailureException(e);
    }
  }

  @Override
  public SubmissionUISummary getIncompleteSubmissions(FilterGroup filterGroup)
      throws AccessDeniedException, FormNotAvailableException, DatastoreFailureException, RequestFailureException {

    HttpServletRequest req = this.getThreadLocalRequest();
    CallingContext cc = ContextFactory.getCallingContext(this, req);

    try {
      String formId = filterGroup.getFormId();
      IForm form = FormFactory.retrieveFormByFormId(formId, cc);
      if (!form.hasValidFormDefinition()) {
        throw new RequestFailureException(ErrorConsts.FORM_DEFINITION_INVALID);
      }
      SubmissionUISummary summary = new SubmissionUISummary(form.getViewableName());

      QueryByUIFilterGroup query = new QueryByUIFilterGroup(form, filterGroup,
          CompletionFlag.ONLY_INCOMPLETE_SUBMISSIONS, cc);
      List<Submission> submissions = query.getResultSubmissions(cc);

      getSubmissions(filterGroup, cc, summary, form, submissions);

      return summary;
    } catch (ODKFormNotFoundException e) {
      throw new FormNotAvailableException(e);
    } catch (ODKOverQuotaException e) {
      e.printStackTrace();
      throw new RequestFailureException(ErrorConsts.QUOTA_EXCEEDED);
    } catch (ODKDatastoreException e) {
      e.printStackTrace();
      throw new DatastoreFailureException(e);
    }

  }

  private void getSubmissions(FilterGroup filterGroup, CallingContext cc,
      SubmissionUISummary summary, IForm form, List<Submission> submissions)
      throws AccessDeniedException, DatastoreFailureException, RequestFailureException {
    GenerateHeaderInfo headerGenerator = new GenerateHeaderInfo(filterGroup, summary, form);
    headerGenerator.processForHeaderInfo(form.getTopLevelGroupElement());

    List<FormElementModel> filteredElements = headerGenerator.getIncludedElements();
    ElementFormatter elemFormatter = new UiElementFormatter(cc.getServerURL(),
        headerGenerator.getGeopointIncludes());

    // format row elements
    for (Submission sub : submissions) {
      try {
        Row row = sub.getFormattedValuesAsRow(headerGenerator.includedFormElementNamespaces(),
          filteredElements, elemFormatter, false, cc);

        SubmissionKey subKey = sub.constructSubmissionKey(form.getTopLevelGroupElement());
        summary.addSubmission(new SubmissionUI(row.getFormattedValues(), subKey.toString()));
      } catch (ODKOverQuotaException e) {
        e.printStackTrace();
        throw new RequestFailureException(ErrorConsts.QUOTA_EXCEEDED);
      } catch (ODKDatastoreException e) {
        e.printStackTrace();
        throw new DatastoreFailureException(e);
      }
    }
  }

  private interface VisitorOutcome extends SubmissionVisitor {
    public boolean getSuccess();
  }
  
  private static final class RemoveIncompleteAttachmentVisitor implements VisitorOutcome {
    final CallingContext cc;
    boolean success = true; // assume it completes successfully...

    RemoveIncompleteAttachmentVisitor(CallingContext cc) {
      this.cc = cc;
    }

    @Override
    public boolean getSuccess() {
      return success;
    }

    @Override
    public boolean traverse(SubmissionElement element) {
      if (element instanceof SubmissionSet) {
        SubmissionSet set = (SubmissionSet) element;
        List<FormElementModel> elements = set.getFormElements();
        for (FormElementModel e : elements) {
          SubmissionValue v = set.getElementValue(e);
          if (v instanceof BlobSubmissionType) {
            BlobSubmissionType blob = (BlobSubmissionType) v;
            try {
              if (blob.getAttachmentCount(cc) == 1 && blob.getContentHash(1, cc) == null) {
                // we have a missing attachment...
                blob.deleteAll(cc);
                set.removeElementValue(e);
              }
            } catch ( ODKDatastoreException ex ) {
              ex.printStackTrace();
              success = false;
            }
          }
        }
      }
      return false;
    }

  }

  private static final class ModifyIncompleteEncryptedAttachmentVisitor implements VisitorOutcome {
    
    private static final String ENC_EXTENSION = ".enc";
    private static final String MISSING_ENC_EXTENSION = ".missing.enc";
    private static final String MIME_OCTET = "application/octet-stream";
    
    final CallingContext cc;
    boolean success = true; // assume it completes successfully...

    ModifyIncompleteEncryptedAttachmentVisitor(CallingContext cc) {
      this.cc = cc;
    }

    @Override
    public boolean getSuccess() {
      return success;
    }

    @Override
    public boolean traverse(SubmissionElement element) {
      if (element instanceof SubmissionSet) {
        SubmissionSet set = (SubmissionSet) element;
        List<FormElementModel> elements = set.getFormElements();
        for (FormElementModel e : elements) {
          SubmissionValue v = set.getElementValue(e);
          if (v instanceof BlobSubmissionType) {
            BlobSubmissionType blob = (BlobSubmissionType) v;
            try {
              if (blob.getAttachmentCount(cc) == 1 && blob.getContentHash(1, cc) == null) {
                
                // we need to entirely delete the entry and re-create it
                // so that we can change the filename to the suffix ".missing.enc"
                String unrootedFilename = blob.getUnrootedFilename(1, cc);
                if ( unrootedFilename != null ) {
                  if ( unrootedFilename.endsWith(ENC_EXTENSION)) {
                    unrootedFilename = unrootedFilename.substring(0,unrootedFilename.lastIndexOf(".")) + MISSING_ENC_EXTENSION;
                  }
                }
                blob.deleteAll(cc);
                blob.setValueFromByteArray(new byte[]{}, MIME_OCTET, unrootedFilename, true, cc);
              }
            } catch ( ODKDatastoreException ex ) {
              ex.printStackTrace();
              success = false;
            }
          }
        }
      }
      return false;
    }

  }

  @Override
  public void markSubmissionAsComplete(String submissionKeyAsString)
      throws AccessDeniedException, FormNotAvailableException, DatastoreFailureException, RequestFailureException {
    HttpServletRequest req = this.getThreadLocalRequest();
    CallingContext cc = ContextFactory.getCallingContext(this, req);

    // create a list because the submission deleter require a list
    SubmissionKey submissionKey = new SubmissionKey(submissionKeyAsString);
    try {
      List<SubmissionKeyPart> parts = submissionKey.splitSubmissionKey();
      Submission sub = Submission.fetchSubmission(parts, cc);
      if ( sub == null ) {
        throw new RequestFailureException("Unable to revise submission (see logs)");
      }
      
      IForm form = FormFactory.retrieveFormByFormId(parts.get(0).getElementName(), cc);
      if ( !form.hasValidFormDefinition() ) {
        // should never happen here -- should happen in the fetchSubmission() call above.
        throw new IllegalArgumentException("Form definition is ill-formed"); // ill-formed definition
      }
      
      VisitorOutcome visitor = null;
      if ( !form.isEncryptedForm() ) {
        visitor = new RemoveIncompleteAttachmentVisitor(cc);
      } else {
        visitor = new ModifyIncompleteEncryptedAttachmentVisitor(cc);
      }

      // recursively examine all attachments and remove any that are
      // missing their files...
      sub.depthFirstTraversal(visitor);
      if (visitor.getSuccess()) {
        sub.setIsComplete(visitor.getSuccess());
        sub.setMarkedAsCompleteDate(new Date());
        sub.persist(cc);
      } else {
        throw new RequestFailureException("Unable to revise submission");
      }
    } catch (ODKFormNotFoundException e) {
      e.printStackTrace();
      throw new FormNotAvailableException(e);
    } catch (ODKEntityNotFoundException e) {
      e.printStackTrace();
      throw new RequestFailureException("Entity not found");
    } catch (ODKOverQuotaException e) {
      e.printStackTrace();
      throw new RequestFailureException(ErrorConsts.QUOTA_EXCEEDED);
    } catch (ODKDatastoreException e) {
      e.printStackTrace();
      throw new DatastoreFailureException(e);
    }
  }

  @Override
  public ArrayList<MediaFileSummary> getFormMediaFileList(String formId)
      throws AccessDeniedException, FormNotAvailableException, DatastoreFailureException, RequestFailureException {
    HttpServletRequest req = this.getThreadLocalRequest();
    CallingContext cc = ContextFactory.getCallingContext(this, req);

    ArrayList<MediaFileSummary> mediaSummaryList = new ArrayList<MediaFileSummary>();

    try {
      IForm form = FormFactory.retrieveFormByFormId(formId, cc);
      if (!form.hasValidFormDefinition())
        return mediaSummaryList; // ill-formed definition -- still show it...

      BinaryContentManipulator bcm = form.getManifestFileset();
      for (int i = 0; i < bcm.getAttachmentCount(cc); ++i) {
        MediaFileSummary mfs = new MediaFileSummary(bcm.getUnrootedFilename(i + 1, cc),
            bcm.getContentType(i + 1, cc), bcm.getContentLength(i + 1, cc));
        mediaSummaryList.add(mfs);
      }
      
      Collections.sort(mediaSummaryList, new Comparator<MediaFileSummary>() {

        @Override
        public int compare(MediaFileSummary o1, MediaFileSummary o2) {
          return o1.getFilename().compareToIgnoreCase(o2.getFilename());
        }});
      
    } catch (ODKFormNotFoundException e) {
      e.printStackTrace();
      throw new FormNotAvailableException(e);
    } catch (ODKOverQuotaException e) {
      e.printStackTrace();
      throw new RequestFailureException(ErrorConsts.QUOTA_EXCEEDED);
    } catch (ODKDatastoreException e) {
      e.printStackTrace();
      throw new DatastoreFailureException(e);
    }
    return mediaSummaryList;
  }

  @Override
  public Date purgeSubmissionsData(String formId, Date value) throws AccessDeniedException,
      FormNotAvailableException, DatastoreFailureException, RequestFailureException {
    HttpServletRequest req = this.getThreadLocalRequest();
    CallingContext cc = ContextFactory.getCallingContext(this, req);
    
    IForm form = null;
    try {
      form = FormFactory.retrieveFormByFormId(formId, cc);
      if (!form.hasValidFormDefinition()) {
        throw new RequestFailureException(ErrorConsts.FORM_DEFINITION_INVALID);
      }
    } catch (ODKFormNotFoundException e) {
      e.printStackTrace();
      throw new FormNotAvailableException(e);
    } catch (ODKOverQuotaException e) {
      e.printStackTrace();
      throw new RequestFailureException(ErrorConsts.QUOTA_EXCEEDED);
    } catch (ODKDatastoreException e) {
      e.printStackTrace();
      throw new DatastoreFailureException(e);
    }

    // any confirm parameter value means OK -- purge it!
    PurgeOlderSubmissions pos = (PurgeOlderSubmissions) cc
        .getBean(BeanDefs.PURGE_OLDER_SUBMISSIONS_BEAN);

    if (pos == null) {
      throw new RequestFailureException("Unable to configure task to purge submitted data for form " + formId);
    }
    // set up the purge request here...
    Map<String, String> parameters = new HashMap<String, String>();

    parameters.put(PurgeOlderSubmissions.PURGE_DATE, WebUtils.purgeDateString(value));

    MiscTasks m;
    try {
      m = new MiscTasks(TaskType.PURGE_OLDER_SUBMISSIONS, form, parameters, cc);
      m.persist(cc);
    } catch (ODKOverQuotaException e) {
      e.printStackTrace();
      throw new RequestFailureException(ErrorConsts.QUOTA_EXCEEDED);
    } catch (ODKDatastoreException e) {
      e.printStackTrace();
      throw new RequestFailureException(
          "Unable to establish task to purge submitted data for form " + formId);
    }
    CallingContext ccDaemon = ContextFactory.getCallingContext(this, req);
    ccDaemon.setAsDaemon(true);
    try {
      pos.createPurgeOlderSubmissionsTask(form, m.getSubmissionKey(), 1L, ccDaemon);
    } catch (ODKOverQuotaException e) {
      e.printStackTrace();
      throw new RequestFailureException(ErrorConsts.QUOTA_EXCEEDED);
    } catch (ODKDatastoreException e) {
      e.printStackTrace();
      throw new RequestFailureException(
          "Unable to establish task to purge submitted data for form " + formId);
    } catch (ODKFormNotFoundException e) {
      e.printStackTrace();
      throw new RequestFailureException(
          "Unable to establish task to purge submitted data for form " + formId);
    }
    return value;
  }

}
