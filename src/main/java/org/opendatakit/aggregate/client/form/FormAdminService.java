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

package org.opendatakit.aggregate.client.form;

import java.util.ArrayList;
import java.util.Date;

import org.opendatakit.aggregate.client.exception.FormNotAvailableException;
import org.opendatakit.aggregate.client.exception.RequestFailureException;
import org.opendatakit.aggregate.client.filter.FilterGroup;
import org.opendatakit.aggregate.client.submission.SubmissionUISummary;
import org.opendatakit.common.persistence.client.exception.DatastoreFailureException;
import org.opendatakit.common.security.client.exception.AccessDeniedException;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * These are the APIs available to users with the ROLE_DATA_OWNER privilege.
 * Adding forms, deleting forms, and other forms management are here.
 * 
 * See FormService for actions that require the lesser ROLE_DATA_VIEWER privilege.
 * 
 * @author wbrunette@gmail.com
 *
 */@RemoteServiceRelativePath("formadminservice")
public interface FormAdminService extends RemoteService {
  
  void setFormDownloadable(String formId, Boolean downloadable) throws AccessDeniedException, FormNotAvailableException, RequestFailureException, DatastoreFailureException;
  
  void setFormAcceptSubmissions(String formId, Boolean acceptSubmissions) throws AccessDeniedException, FormNotAvailableException, RequestFailureException, DatastoreFailureException;
  
  Date purgePublishedData(String uriExternalService, Date earliest) throws  AccessDeniedException, FormNotAvailableException, DatastoreFailureException, RequestFailureException;
  
  void deleteForm(String formId) throws  AccessDeniedException, FormNotAvailableException, DatastoreFailureException, RequestFailureException;
  
  void deleteSubmission(String submissionKeyAsString) throws AccessDeniedException, DatastoreFailureException, FormNotAvailableException, RequestFailureException;
  
  SubmissionUISummary getIncompleteSubmissions(FilterGroup filter) throws AccessDeniedException, FormNotAvailableException, DatastoreFailureException, RequestFailureException;
  
  void markSubmissionAsComplete(String submissionKeyAsString) throws AccessDeniedException, FormNotAvailableException, DatastoreFailureException, RequestFailureException;
  
  ArrayList<MediaFileSummary> getFormMediaFileList(String formId) throws AccessDeniedException, FormNotAvailableException, DatastoreFailureException, RequestFailureException;

  Date purgeSubmissionsData(String formId, Date value) throws AccessDeniedException, FormNotAvailableException, DatastoreFailureException, RequestFailureException;
}
