package org.opendatakit.aggregate.client;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextArea;
import org.opendatakit.aggregate.client.form.FormSummary;
import org.opendatakit.aggregate.client.permissions.UserParser;
import org.opendatakit.aggregate.client.widgets.AggregateButton;
import org.opendatakit.aggregate.constants.common.UIConsts;
import org.opendatakit.common.security.client.UserSecurityInfo;
import org.opendatakit.common.security.common.GrantedAuthorityName;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author: Anna
 * @created: 2015-10-04.
 */
public class AssignmentSubTab extends AggregateSubTabBase {

  private static final Logger logger = Logger.getLogger("AssignmentSubTab");
  private ListBox forms = new ListBox();
  private final CellList<String> cellList;
  private final AggregateButton confirm;
  private TextArea usersToDelete = new TextArea();
  private final AggregateButton delete;
  private TextArea usersToAdd = new TextArea();
  private final AggregateButton add;


  public AssignmentSubTab() {
    // vertical
    setStylePrimaryName(UIConsts.VERTICAL_FLOW_PANEL_STYLENAME);
    HTML info = new HTML("Pick form the check what users are assigned to it:");

    fillFormsList();

    // Create a cell to render each value.
    TextCell textCell = new TextCell();

    // Create a CellList that uses the cell.
    cellList = new CellList<String>(textCell);
    cellList.setKeyboardSelectionPolicy(HasKeyboardSelectionPolicy.KeyboardSelectionPolicy.DISABLED);


    confirm = createGetButton();

    cellList.setStyleName("dataTable");
    cellList.setVisible(false);

    delete = createDeleteButton();

    add = createAddButton();

    add(info);
    add(forms);
    add(confirm);
    add(cellList);
    add(new HTML("<br />"));
    add(new HTML("Write down users you want to delete from this form, each one in new line."));
    add(new HTML("<br />"));
    add(usersToDelete);
    add(new HTML("<br />"));
    add(delete);
    add(new HTML("<br />"));
    add(new HTML("Write down users you want to add to this form, each one in new line."));
    add(new HTML("<br />"));
    add(usersToAdd);
    add(new HTML("<br />"));
    add(add);


  }

  private AggregateButton createAddButton() {
    AggregateButton newAdd = new AggregateButton("Add users to form", "Click to add users");
    newAdd.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent clickEvent) {
        final String formId = forms.getSelectedValue();
        logger.log(Level.INFO, formId);
        if(formId == null || formId == "") {
          Window
              .alert("You have not picked from which form remove the users!");
          return;
        }
        String text = usersToAdd.getValue();
        final List<String> usernames = UserParser.retrieveNames(text);
        SecureGWT.getSecurityService().assignUsersToForm(usernames, formId, new
            AsyncCallback<Void>() {

              public void onFailure(Throwable throwable) {
                System.out.println(throwable.getMessage());
              }

              public void onSuccess(Void success) {
                Window.alert("Users added successfully!");
                usersToAdd.setText("");
                confirm.click();

              }
            });
      }
    });

    newAdd.setStyleName("gwt-Button");
    return newAdd;
  }

  private AggregateButton createGetButton() {
    AggregateButton confirm = new AggregateButton("See assigned users", "Click to get users list");
    confirm.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent clickEvent) {
        String formId = forms.getSelectedValue();
        logger.log(Level.INFO, formId);
        if(formId == null || formId == "") {
          Window
              .alert("You have not picked form!");
          return;
        }
        SecureGWT.getSecurityService().getUserAssignedToForm(formId, new
            AsyncCallback<ArrayList<UserSecurityInfo>>() {

              public void onFailure(Throwable throwable) {
                System.out.println(throwable.getMessage());
              }

              public void onSuccess(ArrayList<UserSecurityInfo> users) {
                cellList.setRowCount(users.size());
                List<String> names = new ArrayList<String>();
                for (UserSecurityInfo user : users) {
                  String name = user.getUsername();
                  names.add(name);
                }
                cellList.setRowData(0, names);
                cellList.setVisible(true);
              }
            });
      }
    });

    confirm.setStyleName("gwt-Button");
    return confirm;
  }


  private AggregateButton createDeleteButton() {
    final AggregateButton delete = new AggregateButton("Delete users", "Click to delete users");
    delete.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent clickEvent) {
        final String formId = forms.getSelectedValue();
        logger.log(Level.INFO, formId);
        if(formId == null || formId == "") {
          Window
              .alert("You have not picked from which form remove the users!");
          return;
        }
        String text = usersToDelete.getValue();
        final List<String> usernames = UserParser.retrieveNames(text);
        SecureGWT.getSecurityService().removeUsersFromForm(usernames, formId, new
            AsyncCallback<Integer>() {

              public void onFailure(Throwable throwable) {
                System.out.println(throwable.getMessage());
              }

              public void onSuccess(Integer users) {
                if(users == 0) {
                  Window.alert("Something went wrong with request. Inspect the usernames.");
                } else if (users < usernames.size()) {
                  Window.alert("Not all the users has been removed from form");
                } else {
                  Window.alert("Users removed successfully!");
                }
                usersToDelete.setText("");
                confirm.click();

              }
            });
      }
    });

    delete.setStyleName("gwt-Button");
    return delete;
  }
  private void fillFormsList() {
    forms.addItem("--Pick form---", "");
    SecureGWT.getFormService().getForms(new AsyncCallback<ArrayList<FormSummary>>() {

      public void onFailure(Throwable throwable) {
        System.out.println(throwable.getMessage());
      }

      public void onSuccess(ArrayList<FormSummary> formSummaries) {
        for(FormSummary form : formSummaries) {
          forms.addItem(form.getTitle(), String.valueOf(form.getAclId()));
        }
      }
    });
  }


  @Override
  public boolean canLeave() {
    return true;
  }

  @Override
  public void update() {

    if ( AggregateUI.getUI().getUserInfo().getGrantedAuthorities().contains(GrantedAuthorityName.ROLE_SITE_ACCESS_ADMIN)) {
      setVisible(true);
    } else {
     setVisible(false);
    }
  }
}
