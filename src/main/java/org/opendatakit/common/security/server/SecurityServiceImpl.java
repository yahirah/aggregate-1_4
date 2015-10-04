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

package org.opendatakit.common.security.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.aggregate.ContextFactory;
import org.opendatakit.aggregate.constants.ErrorConsts;
import org.opendatakit.aggregate.server.SettingsServiceImpl;
import org.opendatakit.aggregate.servlet.UserManagePasswordsServlet;
import org.opendatakit.common.persistence.CommonFieldsBase;
import org.opendatakit.common.persistence.Datastore;
import org.opendatakit.common.persistence.client.exception.DatastoreFailureException;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.security.SecurityBeanDefs;
import org.opendatakit.common.security.User;
import org.opendatakit.common.security.client.CredentialsInfo;
import org.opendatakit.common.security.client.RealmSecurityInfo;
import org.opendatakit.common.security.client.UserSecurityInfo;
import org.opendatakit.common.security.client.exception.AccessDeniedException;
import org.opendatakit.common.security.common.GrantedAuthorityName;
import org.opendatakit.common.security.spring.AclTable;
import org.opendatakit.common.security.spring.RegisteredUsersTable;
import org.opendatakit.common.web.CallingContext;
import org.opendatakit.common.web.constants.BasicConsts;
import org.springframework.security.authentication.encoding.MessageDigestPasswordEncoder;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * GWT Server implementation for the SecurityService interface. This provides
 * privileges context to the client and is therefore accessible to anyone with a
 * ROLE_USER privilege.
 * 
 * @author mitchellsundt@gmail.com
 * 
 */
public class SecurityServiceImpl extends RemoteServiceServlet implements
    org.opendatakit.common.security.client.security.SecurityService {

  /**
	 * 
	 */
  private static final long serialVersionUID = -7360632450727200941L;
  private static final Log logger = LogFactory.getLog(SecurityServiceImpl.class.getName());

  @Override
  public UserSecurityInfo getUserInfo() throws AccessDeniedException, DatastoreFailureException {

    HttpServletRequest req = this.getThreadLocalRequest();
    CallingContext cc = ContextFactory.getCallingContext(this, req);

    Datastore ds = cc.getDatastore();
    User user = cc.getCurrentUser();

    String uriUser = user.getUriUser();
    UserSecurityInfo info;
    try {
      if (user.isRegistered()) {
        RegisteredUsersTable t;
        t = RegisteredUsersTable.getUserByUri(uriUser, ds, user);
        if (t != null) {
          info = new UserSecurityInfo(t.getUsername(), t.getFullName(), t.getEmail(),
              UserSecurityInfo.UserType.REGISTERED);
          SecurityServiceUtil.setAuthenticationLists(info, t.getUri(), cc);
        } else {
          throw new DatastoreFailureException("Unable to retrieve user record");
        }
      } else if (user.isAnonymous()) {
        info = new UserSecurityInfo(User.ANONYMOUS_USER, User.ANONYMOUS_USER_NICKNAME, null,
            UserSecurityInfo.UserType.ANONYMOUS);
        SecurityServiceUtil.setAuthenticationListsForSpecialUser(info,
            GrantedAuthorityName.USER_IS_ANONYMOUS, cc);
      } else {
        // should never get to this case via interactive actions...
        throw new DatastoreFailureException("Internal error: 45443");
      }
    } catch (ODKDatastoreException e) {
      e.printStackTrace();
      throw new DatastoreFailureException(e);
    }
    return info;
  }

  @Override
  public RealmSecurityInfo getRealmInfo(String xsrfString) throws AccessDeniedException, DatastoreFailureException {

    HttpServletRequest req = this.getThreadLocalRequest();
    CallingContext cc = ContextFactory.getCallingContext(this, req);

    if (!req.getSession().getId().equals(xsrfString)) {
      throw new AccessDeniedException("Invalid request");
    }

    RealmSecurityInfo r = new RealmSecurityInfo();
    r.setRealmString(cc.getUserService().getCurrentRealm().getRealmString());
    MessageDigestPasswordEncoder mde = (MessageDigestPasswordEncoder) cc
        .getBean(SecurityBeanDefs.BASIC_AUTH_PASSWORD_ENCODER);
    r.setBasicAuthHashEncoding(mde.getAlgorithm());
    r.setSuperUserEmail(cc.getUserService().getSuperUserEmail());
    r.setSuperUsername(cc.getUserService().getSuperUserUsername());
    try {
      r.setSuperUsernamePasswordSet(cc.getUserService().isSuperUsernamePasswordSet(cc));
    } catch (ODKDatastoreException e) {
      e.printStackTrace();
      throw new DatastoreFailureException("Unable to access datastore");
    }
    // User interface layer uses this URL to submit password changes securely
    r.setChangeUserPasswordURL(cc.getSecureServerURL() + BasicConsts.FORWARDSLASH
        + UserManagePasswordsServlet.ADDR);
    return r;
  }

  @Override
  public Integer changePasswords(List<CredentialsInfo> users) {
    HttpServletRequest req = this.getThreadLocalRequest();
    CallingContext cc = ContextFactory.getCallingContext(this, req);
    Integer outcome = 0;
    logger.debug("So many users to change: " + users.size());
    for(CredentialsInfo credential : users) {
      try {
      SecurityServiceUtil.setUserCredentials(credential, cc);

      String superUsername = cc.getUserService().getSuperUserUsername();
      if ( superUsername.equals(credential.getUsername()) ) {
        cc.getUserService().reloadPermissions();
      }
      outcome++;
      } catch (AccessDeniedException e1) {
        logger.warn("Bad username: " + credential.getUsername() + "with problem" + e1.getMessage());
      } catch (DatastoreFailureException e1) {
        logger.warn(ErrorConsts.PERSISTENCE_LAYER_PROBLEM);
      }
    }
    return outcome;
  }

  public void assignUsersToForm(List<String> usernames, String formId) {
    logger.warn("Assigng users to form");
    HttpServletRequest req = this.getThreadLocalRequest();
    CallingContext cc = ContextFactory.getCallingContext(this, req);
    Datastore ds = cc.getDatastore();
    logger.warn("Assigning " + usernames.size() + " users to " + formId);
    List<Long> ids = new ArrayList<Long>();
    try {
      for(String username : usernames) {
        RegisteredUsersTable userDefinition = RegisteredUsersTable.getUserByUsername(username,
            cc.getUserService(), ds);
        if (userDefinition == null) {
          throw new AccessDeniedException("User is not a registered user.");
        }
        ids.add(userDefinition.getId());

      }
      SecurityServiceUtil.assignUserToForm(ids, formId, cc);
    } catch (ODKDatastoreException e) {
      logger.warn("Error with database while adding access to " + formId);
      e.printStackTrace();
    } catch (AccessDeniedException e) {
      logger.warn("Access denied while adding access to " + formId);
      e.printStackTrace();
    }
    logger.info("Generating access rights to form " + formId.toString() + " for " + usernames.size() + " users");
  }

  public ArrayList<UserSecurityInfo> getUserAssignedToForm(String formId) {
    logger.info("Getting user assigned to form");

    HttpServletRequest req = this.getThreadLocalRequest();
    CallingContext cc = ContextFactory.getCallingContext(this, req);

    ArrayList<UserSecurityInfo> users = null;
    try {
      users = SecurityServiceUtil.getUsersForForm(formId, cc);
    } catch (ODKDatastoreException e) {
      logger.warn("Error with database while retrieving access list to " + formId);
      e.printStackTrace();
    }

    return users;
  }

  public Integer removeUsersFromForm(List<String> usernames, String formId) {
    logger.warn("Removing users from form");
    HttpServletRequest req = this.getThreadLocalRequest();
    CallingContext cc = ContextFactory.getCallingContext(this, req);
    Datastore ds = cc.getDatastore();
    logger.warn("Removing " + usernames.size() + " users to " + formId);
    List<Long> ids = new ArrayList<Long>();
    try {
      Integer outcome = 0;
      for(String username : usernames) {
        RegisteredUsersTable userDefinition = RegisteredUsersTable.getUserByUsername(username,
            cc.getUserService(), ds);
        if (userDefinition == null) {
          throw new AccessDeniedException("User is not a registered user.");
        }
        ids.add(userDefinition.getId());
        outcome++;
      }
      SecurityServiceUtil.removeUsersFromForm(ids, formId, cc);
      return outcome;
    } catch (ODKDatastoreException e) {
      logger.warn("Error with database while Removing access from " + formId);
      e.printStackTrace();
    } catch (AccessDeniedException e) {
      logger.warn("Access denied while Removing access from " + formId);
      e.printStackTrace();
    }
    logger.info("Removing access rights to form " + formId.toString() + " for " + usernames.size() + " users");
    return 0;
  }
}
