package org.opendatakit.aggregate.servlet;

import org.apache.commons.lang3.StringEscapeUtils;
import org.opendatakit.aggregate.ContextFactory;
import org.opendatakit.aggregate.constants.HtmlUtil;
import org.opendatakit.aggregate.constants.ServletConsts;
import org.opendatakit.aggregate.exception.ODKFormNotFoundException;
import org.opendatakit.aggregate.settings.AppSettings;
import org.opendatakit.aggregate.settings.SettingsFactory;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.persistence.exception.ODKOverQuotaException;
import org.opendatakit.common.web.CallingContext;
import org.opendatakit.common.web.constants.BasicConsts;
import org.opendatakit.common.web.constants.HtmlConsts;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Anna on 2015-08-25.
 */
public class SettingsXmlServlet extends ServletUtilBase {

  /**
   * Serial number for serialization
   */
  private static final long serialVersionUID = -5861333658170389989L;

  /**
   * URI from base
   */
  public static final String ADDR = "settingsXml";
  public static final String WWW_ADDR = "www/settingsXml";

  /**
   * Title for generated webpage
   */
  private static final String TITLE_INFO = "Settings Xml Viewer";

  /**
   * Handler for HTTP Get request that responds with the XML in plain
   *
   * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse)
   */
  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

    // get parameters
    String name = getParameter(req, ServletConsts.SETTINGS_NAME);
    if (name == null) {
      errorMissingKeyParam(resp);
      return;
    }

    String readable = getParameter(req, ServletConsts.HUMAN_READABLE);
    boolean humanReadable = false;
    if (readable != null) {
      humanReadable = Boolean.parseBoolean(readable);
    }

    CallingContext cc = ContextFactory.getCallingContext(this, req);

    AppSettings settings;
    try {
      settings = SettingsFactory.retrieveSettingsByName(name, cc);
    } catch (ODKFormNotFoundException e) {
      e.printStackTrace();
      odkIdNotFoundError(resp);
      return;
    } catch (ODKDatastoreException e) {
      e.printStackTrace();
      datastoreError(resp);
      return;
    }
    String xmlString = null;

    try {
      if (settings != null) {
        xmlString = settings.getSettingsXml(cc);
      } else {
        odkIdNotFoundError(resp);
        return;
      }

      // Debug: String debugDisplay = WebUtils.escapeUTF8String(xmlString);

      if (humanReadable) {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(ServletConsts.SETTINGS_NAME, name);
        String downloadXmlButton = HtmlUtil.createHtmlButtonToGetServlet(
            cc.getWebApplicationURL(ADDR), ServletConsts.DOWNLOAD_XML_BUTTON_TXT, properties);

        beginBasicHtmlResponse(TITLE_INFO, resp, cc); // header info
        PrintWriter out = resp.getWriter();
        out.println("<h3>Settings Name: <FONT COLOR=0000FF>" + settings.getFileName() + "</FONT></h3>");
        out.println(downloadXmlButton); // download button
        out.println("<PRE>");
        out.print(StringEscapeUtils.escapeHtml4(xmlString));// settings xml
        out.println("</PRE>");
        finishBasicHtmlResponse(resp); // footer info
      } else {
        resp.setCharacterEncoding(HtmlConsts.UTF8_ENCODE);
        resp.setContentType(HtmlConsts.RESP_TYPE_XML);
        PrintWriter out = resp.getWriter();
        resp.setHeader(HtmlConsts.CONTENT_DISPOSITION,
        HtmlConsts.ATTACHMENT_FILENAME_TXT + settings.getFileName() + BasicConsts.QUOTE
            + BasicConsts.SEMI_COLON);
        out.print(xmlString);
      }
    } catch (ODKOverQuotaException e) {
      e.printStackTrace();
      quotaExceededError(resp);
    } catch (ODKDatastoreException e) {
      e.printStackTrace();
      odkIdNotFoundError(resp);
    }
  }
}
