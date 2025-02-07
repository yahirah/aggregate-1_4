/*
 * Copyright (C) 2013 University of Washington
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

package org.opendatakit.aggregate.client.preferences;

import java.util.ArrayList;

import org.opendatakit.common.security.client.UserSecurityInfo;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface OdkTablesAdminServiceAsync {

  void deleteAdmin(String uriUser, AsyncCallback<Boolean> callback);

  void listAdmin(AsyncCallback<OdkTablesAdmin[]> callback);

  void updateAdmin(OdkTablesAdmin admin, AsyncCallback<Boolean> callback);

  void setAdmins(ArrayList<UserSecurityInfo> admins, AsyncCallback<Boolean> callback);

}
