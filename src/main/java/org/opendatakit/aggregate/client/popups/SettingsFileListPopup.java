package org.opendatakit.aggregate.client.popups;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlexTable;
import org.opendatakit.aggregate.client.AggregateUI;
import org.opendatakit.aggregate.client.SecureGWT;
import org.opendatakit.aggregate.client.form.MediaFileSummary;
import org.opendatakit.aggregate.client.widgets.ClosePopupButton;

import java.util.ArrayList;

/**
 * Created by Anna on 2015-08-23.
 */
public class SettingsFileListPopup extends AbstractPopupBase{

  private FlexTable fileList;

  public SettingsFileListPopup(String name) {

    SecureGWT.getSettingsService().getSettingsFileList(name,
        new MediaFileCallback());

    fileList = new FlexTable();
    fileList.setWidget(0, 0, new ClosePopupButton(this));
    fileList.getCellFormatter().getElement(0, 0).setAttribute("align",
        "right");
    fileList.setText(1, 0, "Media Filename");
    fileList.setText(1, 1, "Content Type");
    fileList.setText(1, 2, "Length");
    fileList.getRowFormatter().addStyleName(1, "titleBar");

    setWidget(fileList);
  }


  private class MediaFileCallback implements
      AsyncCallback<ArrayList<MediaFileSummary>> {

    @Override
    public void onFailure(Throwable caught) {
      AggregateUI.getUI().reportError(caught);
    }

    @Override
    public void onSuccess(ArrayList<MediaFileSummary> result) {
      if(result == null)
        return;

      int index = 2;
      for(MediaFileSummary file : result) {
        fileList.setText(index, 0, file.getFilename());
        fileList.setText(index, 1, file.getContentType());
        fileList.setText(index, 2, Long.toString(file.getContentLength()));
        index++;
      }
    }

  }
}
