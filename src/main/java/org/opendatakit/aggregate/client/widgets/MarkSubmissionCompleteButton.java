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

package org.opendatakit.aggregate.client.widgets;

import org.opendatakit.aggregate.client.AggregateUI;
import org.opendatakit.aggregate.client.SecureGWT;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class MarkSubmissionCompleteButton extends AggregateButton implements ClickHandler {

  private static final String BUTTON_TXT = "Mark Complete";
  private static final String TOOLTIP_TEXT = "Mark Submission as COMPLETE";
  private static final String HELP_BALLOON_TXT = "Mark Submission as complete so that it will show up in the UI. A submission may be incomplete because of transmission problems or another unknown reason.";

  private final String submissionKeyAsString;

  public MarkSubmissionCompleteButton(String submissionKeyAsString) {
    super(BUTTON_TXT, TOOLTIP_TEXT, HELP_BALLOON_TXT);
    this.submissionKeyAsString = submissionKeyAsString;
    addStyleDependentName("negative");
  }

  @Override
  public void onClick(ClickEvent event) {
    super.onClick(event);

    // Set up the callback object.
    AsyncCallback<Void> callback = new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
        AggregateUI.getUI().reportError(caught);
      }

      @Override
      public void onSuccess(Void result) {
        AggregateUI.getUI().clearError();
        AggregateUI.getUI().getTimer().refreshNow();
      }
    };
    // Make the call to the form service.
    SecureGWT.getFormAdminService().markSubmissionAsComplete(submissionKeyAsString, callback);
  }

}
