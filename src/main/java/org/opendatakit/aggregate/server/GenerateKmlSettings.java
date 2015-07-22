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

package org.opendatakit.aggregate.server;

import java.util.List;

import org.opendatakit.aggregate.client.form.KmlSettings;
import org.opendatakit.aggregate.datamodel.FormElementKey;
import org.opendatakit.aggregate.datamodel.FormElementModel;
import org.opendatakit.aggregate.form.IForm;

public class GenerateKmlSettings {

  private KmlSettings settings;
  private IForm form;
  boolean ignoreRepeats;

  public GenerateKmlSettings(IForm form, boolean ignoreRepeats) {
    this.form = form;
    this.settings = new KmlSettings();
    this.ignoreRepeats = ignoreRepeats;
  }

  public KmlSettings generate() {
    FormElementModel root = form.getTopLevelGroupElement();
    processElementForColumnHead(form, root, root);
    return settings;
  }

  /**
   * Helper function to recursively go through the element tree and create the
   * FormElementKeys
   *
   */
  private void processElementForColumnHead(IForm form, FormElementModel node, FormElementModel root) {
    if (node == null)
      return;

    FormElementKey key = node.constructFormElementKey(form);
    String nodeName = key.userFriendlyString(form);
    switch (node.getElementType()) {
    case GEOPOINT:
      settings.addGeopointNode(nodeName, key.toString());
      break;
    case BINARY:
      settings.addBinaryNode(nodeName, key.toString());
      break;
    case REPEAT:
      if (ignoreRepeats) {
        return;
      }
    case GROUP:
      break; // should not be in any list
    default:
      settings.addTitleNode(nodeName, key.toString());
    }

    List<FormElementModel> childDataElements = node.getChildren();
    if (childDataElements == null) {
      return;
    }
    for (FormElementModel child : childDataElements) {
      processElementForColumnHead(form, child, root);
    }
  }

}
