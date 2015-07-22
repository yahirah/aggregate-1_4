/*
 * Copyright (C) 2012 University of Washington
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

package org.opendatakit.aggregate.client.widgets;

import org.opendatakit.aggregate.client.AggregateUI;
import org.opendatakit.aggregate.client.SecureGWT;
import org.opendatakit.aggregate.client.preferences.Preferences;
import org.opendatakit.aggregate.client.preferences.Preferences.PreferencesCompletionCallback;
import org.opendatakit.common.security.common.GrantedAuthorityName;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;

public final class EnableOdkTablesCheckbox extends AggregateCheckBox implements
    ValueChangeHandler<Boolean> {

  private static final String LABEL = "ODK Tables Syncronization Functionality";
  private static final String TOOLTIP_TXT = "Enable/Disable ODK Tables Sync Functionality";
  private static final String HELP_BALLOON_TXT = "Check this box if you want to manage ODK Tables.  "
      + "Otherwise leave unchecked.";

  private PreferencesCompletionCallback settingsChange;

  public EnableOdkTablesCheckbox(Boolean enabled, PreferencesCompletionCallback settingsChange) {
    super(LABEL, false, TOOLTIP_TXT, HELP_BALLOON_TXT);
    this.settingsChange = settingsChange;
    setValue(enabled);
    boolean accessible = AggregateUI.getUI().getUserInfo().getGrantedAuthorities()
        .contains(GrantedAuthorityName.ROLE_SITE_ACCESS_ADMIN);
    setEnabled(accessible);
  }

  public void updateValue(Boolean value) {
    Boolean currentValue = getValue();
    if (currentValue != value) {
      setValue(value);
    }
  }

  @Override
  public void onValueChange(ValueChangeEvent<Boolean> event) {
    super.onValueChange(event);

    final Boolean enabled = event.getValue();
    SecureGWT.getPreferenceService().setOdkTablesEnabled(enabled, new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
        // restore old value
        setValue(Preferences.getOdkTablesEnabled());
        AggregateUI.getUI().reportError(caught);
      }

      @Override
      public void onSuccess(Void result) {
        AggregateUI.getUI().clearError();
        Preferences.updatePreferences(settingsChange);
      }
    });
  }
}