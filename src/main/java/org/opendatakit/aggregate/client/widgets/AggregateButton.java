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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;

public class AggregateButton extends Button implements ClickHandler{
  
  private final AggregateBaseHandlers handlers;
  
  public AggregateButton(String buttonText, String tooltipText) {
    this(buttonText, tooltipText, null);    
  }

  public AggregateButton(String buttonText, String tooltipText, String helpBalloonText) {
    super(buttonText);

    addClickHandler(this);

    // setup help system
    handlers = new AggregateBaseHandlers(this, tooltipText, helpBalloonText);  
    addMouseOverHandler(handlers);
    addMouseOutHandler(handlers);  
  }
  
  @Override
  public void onClick(ClickEvent event) {
    handlers.userAction();
  }

}
