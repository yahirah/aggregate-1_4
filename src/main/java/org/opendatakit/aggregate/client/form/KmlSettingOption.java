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

package org.opendatakit.aggregate.client.form;

import java.io.Serializable;

public final class KmlSettingOption implements Serializable {

  /**
   * Serialization Version Identifier
   */
  private static final long serialVersionUID = -6824246262410791227L;

  private String displayName;
  private String elementKey;

  public KmlSettingOption() {
  }

  public KmlSettingOption(String displayName, String elementKey) {
    this.setDisplayName(displayName);
    this.setElementKey(elementKey);
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setElementKey(String elementKey) {
    this.elementKey = elementKey;
  }

  public String getElementKey() {
    return elementKey;
  }

}