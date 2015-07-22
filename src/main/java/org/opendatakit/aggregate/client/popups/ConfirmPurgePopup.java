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

package org.opendatakit.aggregate.client.popups;

import java.util.Date;

import org.opendatakit.aggregate.client.AggregateUI;
import org.opendatakit.aggregate.client.SecureGWT;
import org.opendatakit.aggregate.client.externalserv.ExternServSummary;
import org.opendatakit.aggregate.client.widgets.AggregateButton;
import org.opendatakit.aggregate.client.widgets.ClosePopupButton;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;

public class ConfirmPurgePopup extends AbstractPopupBase {

  private static final String BUTTON_TXT = "<img src=\"images/green_right_arrow.png\" /> Purge Data";
  private static final String TOOLTIP_TXT = "Delete published data";
  private static final String HELP_BALLOON_TXT = "This confirms that you want to delete the published data.";

  private String uri;
  private Date earliest;

  public ConfirmPurgePopup(ExternServSummary e, Date earliest, String bodyText) {
    super();

    this.uri = e.getUri();
    this.earliest = earliest;

    AggregateButton confirm = new AggregateButton(BUTTON_TXT, TOOLTIP_TXT, HELP_BALLOON_TXT);
    confirm.addClickHandler(new PurgeHandler());

    FlexTable layout = new FlexTable();
    layout.setWidget(0, 0, new HTML(bodyText));
    layout.setWidget(0, 1, confirm);
    layout.setWidget(0, 2, new ClosePopupButton(this));
    setWidget(layout);
  }

  private class PurgeHandler implements ClickHandler {
    @Override
    public void onClick(ClickEvent event) {

      // OK -- we are to proceed.
      SecureGWT.getFormAdminService().purgePublishedData(uri, earliest, new AsyncCallback<Date>() {

        @Override
        public void onFailure(Throwable caught) {
          AggregateUI.getUI().reportError("Failed purge of published data: ", caught);
        }

        @Override
        public void onSuccess(Date result) {
          Window.alert("Successful commencement of the purge of " + "\nall data published as of "
              + result.toString());
        }
      });
      hide();
    }
  }
}