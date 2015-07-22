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

import java.util.Date;

import org.opendatakit.aggregate.client.externalserv.ExternServSummary;
import org.opendatakit.aggregate.client.popups.ConfirmPurgePopup;
import org.opendatakit.aggregate.constants.common.ExternalServicePublicationOption;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;

public final class PurgeButton extends AggregateButton implements ClickHandler {

  private static final String BUTTON_TXT = "Purge Published Data";
  private static final String TOOLTIP_TXT = "Clear the published data";
  private static final String HELP_BALLOON_TXT = "This will delete the published data.";

  private final String formId;
  private final ExternServSummary externServ;

  public PurgeButton(String formId, ExternServSummary externalService) {
    super(BUTTON_TXT, TOOLTIP_TXT, HELP_BALLOON_TXT);
    this.formId = formId;
    this.externServ = externalService;
  }

  @Override
  public void onClick(ClickEvent event) {
    super.onClick(event);
    
    Date earliest = null;
    switch (externServ.getPublicationOption()) {
    case UPLOAD_ONLY:
      if (externServ.getUploadCompleted()) {
        earliest = externServ.getTimeEstablished();
      } else {
        earliest = externServ.getTimeLastUploadCursor();
      }
      break;
    case UPLOAD_N_STREAM:
      if (externServ.getUploadCompleted()) {
        earliest = externServ.getTimeLastStreamingCursor();
        if (earliest == null) {
          earliest = externServ.getTimeEstablished();
        }
      } else {
        earliest = externServ.getTimeLastUploadCursor();
      }
      break;
    case STREAM_ONLY:
      earliest = externServ.getTimeLastStreamingCursor();
      if (earliest == null) {
        earliest = externServ.getTimeEstablished();
      }
      break;
    }

    StringBuilder b = new StringBuilder();
    if (earliest == null) {
      Window.alert("Data has not yet been published -- no data will be purged");
    } else {
      if (externServ.getPublicationOption() != ExternalServicePublicationOption.UPLOAD_ONLY) {
        b.append("<p><b>Note:</b> Even though the chosen publishing action involves an ongoing streaming"
            + " of data to the external service, this purge action is a one-time event and is "
            + "not automatically ongoing.  You will need to periodically repeat this process.</p>");
      }
      b.append("Click to confirm purge of <b>" + formId + "</b> submissions older than "
          + earliest.toString());

      // TODO: display pop-up with text from b...
      final ConfirmPurgePopup popup = new ConfirmPurgePopup(externServ, earliest, b.toString());
      popup.setPopupPositionAndShow(popup.getPositionCallBack());
    }
  }
}