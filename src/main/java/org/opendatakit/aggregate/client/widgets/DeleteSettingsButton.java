package org.opendatakit.aggregate.client.widgets;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import org.opendatakit.aggregate.client.AggregateUI;
import org.opendatakit.aggregate.client.popups.ConfirmSettingsDeletePopup;
import org.opendatakit.common.security.common.GrantedAuthorityName;

/**
 * @author Anna
 * Small button for UI for deleting the app settings.
 */
public final class DeleteSettingsButton extends AggregateButton implements ClickHandler {

  private static final String BUTTON_TXT = "<img src=\"images/red_x.png\" /> Delete";
  private static final String TOOLTIP_TXT = "Remove the form";
  private static final String HELP_BALLOON_TXT = "Delete this form from Aggregate.";

  private final String name;

  public DeleteSettingsButton(String name) {
      super(BUTTON_TXT, TOOLTIP_TXT, HELP_BALLOON_TXT);
      this.name = name;
      addStyleDependentName("negative");
      boolean enabled = AggregateUI.getUI().getUserInfo().getGrantedAuthorities()
          .contains(GrantedAuthorityName.ROLE_DATA_OWNER);
      setEnabled(enabled);
    }

    @Override
    public void onClick(ClickEvent event) {
      super.onClick(event);

      ConfirmSettingsDeletePopup popup = new ConfirmSettingsDeletePopup(name);
      popup.setPopupPositionAndShow(popup.getPositionCallBack());
    }
}
