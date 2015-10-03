package org.opendatakit.aggregate.client.permissions;

/**
 * @author: Anna
 * @created: 2015-09-28.
 */
public class PreUser {
  private String name;
  private String password;

  private String formId;

  public PreUser(String name, String pass, String form) {
    this.name = name;
    this.password = pass;
    this.formId = form;
  }
  public String getName() {
    return name;
  }

  public String getPassword() {
    return password;
  }

  public String getFormId() {
    return formId;
  }
}
