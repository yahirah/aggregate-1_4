package org.opendatakit.aggregate.client.popups;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import org.opendatakit.aggregate.client.AggregateUI;
import org.opendatakit.aggregate.client.SecureGWT;
import org.opendatakit.aggregate.client.widgets.AggregateButton;
import org.opendatakit.aggregate.client.widgets.ClosePopupButton;
import org.opendatakit.aggregate.constants.common.SubTabs;

/**
 * Created by Anna on 2015-08-24.
 */
public final class ConfirmSettingsDeletePopup extends AbstractPopupBase  {
  
  private static final String BUTTON_TXT = "<img src=\"images/green_right_arrow.png\" /> Delete Data and Form";
  private static final String TOOLTIP_TXT = "Delete data and form";
  private static final String HELP_BALLOON_TXT = "This will delete the form and all of the contained " +
      "data.";

  private final String name;

  public ConfirmSettingsDeletePopup(String name) {
    super();

    this.name = name;

    AggregateButton deleteButton = new AggregateButton(BUTTON_TXT, TOOLTIP_TXT, HELP_BALLOON_TXT);
    deleteButton.addClickHandler(new DeleteHandler());

    FlexTable layout = new FlexTable();

    HTML message = new HTML(
        "Delete settings for <b>"
            + name
            + "</b>?<br/>");
    layout.setWidget(0, 0, message);
    layout.setWidget(0, 1, deleteButton);
    layout.setWidget(0, 2, new ClosePopupButton(this));

    setWidget(layout);
  }

  private class DeleteHandler implements ClickHandler {

    @Override
    public void onClick(ClickEvent event) {
      // OK -- we are to proceed.
      // Set up the callback object.
      AsyncCallback<Void> callback = new AsyncCallback<Void>() {
        @Override
        public void onFailure(Throwable caught) {
          AggregateUI.getUI().reportError(caught);
        }

        @Override
        public void onSuccess(Void result) {
          AggregateUI.getUI().clearError();
          Window.alert("Deleted!");
          AggregateUI.getUI().getTimer().refreshNow();
          AggregateUI.getUI().getSubTab(SubTabs.SETTINGS).update();
        }
      };
      // Make the call to the form service.
      SecureGWT.getSettingsService().deleteSettings(name, callback);
      hide();
    }
  }
}
