package org.opendatakit.aggregate.client.permissions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author: Anna
 * @created: 2015-09-28.
 */
public class UserParser {
  public static List<PreUser> retrievePreUsers(String text, String form) {

    List<PreUser> result = new ArrayList<PreUser>();

    String[] entries = text.split("\n");
    for(String entry : entries) {
      String[] data = entry.split(",");
      if (data.length != 2) return null;
      PreUser user = new PreUser(data[0], data[1], form );
      result.add(user);
    }
    return  result;
  }

  public static List<String> retrieveNames(String text) {
    String[] entries = text.split("\n");
    List<String> result = Arrays.asList(entries);
    return  result;
  }
}
