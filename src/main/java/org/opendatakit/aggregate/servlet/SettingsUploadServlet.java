package org.opendatakit.aggregate.servlet;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.aggregate.ContextFactory;
import org.opendatakit.aggregate.constants.ErrorConsts;
import org.opendatakit.aggregate.constants.HtmlUtil;
import org.opendatakit.aggregate.constants.ServletConsts;
import org.opendatakit.aggregate.constants.common.UIConsts;
import org.opendatakit.aggregate.parser.MultiPartFormData;
import org.opendatakit.aggregate.parser.MultiPartFormItem;
import org.opendatakit.aggregate.settings.AppSettings;
import org.opendatakit.aggregate.settings.SettingsFactory;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.web.CallingContext;
import org.opendatakit.common.web.constants.BasicConsts;
import org.opendatakit.common.web.constants.HtmlConsts;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Servlet for uploading App Settings. Mostly for internal use.
 * @author Anna
 */
public class SettingsUploadServlet extends ServletUtilBase {

  /**
   * Serial number for serialization
   */
  private static final long serialVersionUID = -3784460105551008112L;

  /**
   * URI from base
   */
  public static final String ADDR = UIConsts.SETTINGS_UPLOAD_SERVLET_ADDR;

  /**
   * Title for generated webpage
   */
  private static final String TITLE_INFO = "Settigs Upload";

  private static final String UPLOAD_PAGE_BODY_START =

      "<div style=\"overflow: auto;\"><p id=\"subHeading\"><h2>Upload one form into ODK Aggregate</h2></p>"
          + "<!--[if true]><p style=\"color: red;\">For a better user experience, use Chrome, Firefox or Safari</p>"
          + "<![endif] -->"
          + "<form id=\"ie_backward_compatible_form\""
          + " accept-charset=\"UTF-8\" method=\"POST\" encoding=\"multipart/form-data\" enctype=\"multipart/form-data\""
          + " action=\"";// emit the ADDR

  private static final String UPLOAD_PAGE_BODY_MIDDLE = "\">"
      + "	  <table id=\"uploadTable\">"
      + "	  	<tr>"
      + "	  		<td><label for=\"gSettings\">Global settings:</label></td>"
      + "	  		<td><input id=\"gSettings\" class=\"gwt-Button\" type=\"file\" size=\"80,20\" name=\"datafile\" multiple />"
      + "           <input id=\"clear_gSettings_files\" type=\"button\" class=\"gwt-Button\" value=\"Clear\" "
      + "                  onClick=\"clearMediaInputField('gSettings')\" /></td>"
      + "	  	</tr>"
      + "     <tr>"
      + "	       <td><label for=\"aSettings\">Admin settings::</label></td>"
      + "	       <td><input id=\"aSettings\" class=\"gwt-Button\" type=\"file\" size=\"80\" name=\"datafile\" />"
      + "            <input id=\"clear_aSettings_files\" type=\"button\" class=\"gwt-Button\" value=\"Clear\" "
      + "                   onClick=\"clearMediaInputField('aSettings')\" /></td>"
      + "	    </tr>\n"
      + "	  	<tr>"
      + "	  		<td><input id=\"upload_form\" type=\"submit\" name=\"button\" class=\"gwt-Button\" value=\"Upload Form\" /></td>"
      + "	  		<td />"
      + "	  	</tr>"
      + "	  </table>\n"
      + "	  </form>"
      + "<p>Media files for the form's logo, images, audio clips and video clips "
      + "(if any) should be in a single directory without subdirectories.</p>"
      + "<br><br>"
      + "<p id=\"note\"><b><font color=\"red\">NOTE:</font> "
      + "These settings files will overwrite old settings files! </p> "
      + "</div>\n";

  /**
   * Title for generated webpage to obtain title
   */
  private static final String OBTAIN_TITLE_INFO = "Settings Title Entry";

  /**
   * Text to display to user to obtain title
   */
  //private static final String TITLE_OF_THE_XFORM = "Title of the Xform:";

  private static final Log logger = LogFactory.getLog(SettingsUploadServlet.class);

  /**
   * Handler for HTTP Get request to create xform upload page
   *
   * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse)
   */
  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    CallingContext cc = ContextFactory.getCallingContext(this, req);

    Double openRosaVersion = getOpenRosaVersion(req);
    if (openRosaVersion != null) {
      /*
       * If we have an OpenRosa version header, assume that this is due to a
       * channel redirect (http: => https:) and that the request was originally
       * a HEAD request. Reply with a response appropriate for a HEAD request.
       *
       * It is unclear whether this is a GAE issue or a Spring Frameworks issue.
       */
      logger.warn("Inside doGet -- replying as doHead");
      doHead(req, resp);
      return;
    }

    StringBuilder headerString = new StringBuilder();
    headerString.append("<script type=\"application/javascript\" src=\"");
    headerString.append(cc.getWebApplicationURL(ServletConsts.UPLOAD_SCRIPT_RESOURCE));
    headerString.append("\"></script>");
    headerString.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"");
    headerString.append(cc.getWebApplicationURL(ServletConsts.UPLOAD_STYLE_RESOURCE));
    headerString.append("\" />");
    headerString.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"");
    headerString.append(cc.getWebApplicationURL(ServletConsts.UPLOAD_BUTTON_STYLE_RESOURCE));
    headerString.append("\" />");
    headerString.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"");
    headerString.append(cc.getWebApplicationURL(ServletConsts.AGGREGATE_STYLE));
    headerString.append("\" />");

    // header info
    beginBasicHtmlResponse(TITLE_INFO, headerString.toString(), resp, cc);
    PrintWriter out = resp.getWriter();
    out.write(UPLOAD_PAGE_BODY_START);
    out.write(cc.getWebApplicationURL(ADDR));
    out.write(UPLOAD_PAGE_BODY_MIDDLE);
    finishBasicHtmlResponse(resp);
  }

  /**
   * Handler for HTTP head request. This is used to verify that channel security
   * and authentication have been properly established when uploading form
   * definitions via a program (e.g., Briefcase).
   */
  @Override
  protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    CallingContext cc = ContextFactory.getCallingContext(this, req);
    logger.info("Inside doHead");

    addOpenRosaHeaders(resp);
    String serverUrl = cc.getServerURL();
    String url = serverUrl + BasicConsts.FORWARDSLASH + ADDR;
    resp.setHeader("Location", url);
    resp.setStatus(204); // no content...
  }

  /**
   * Handler for HTTP Post request that takes settings, parses, and saves a
   * parsed versions in the datastore
   *
   * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse)
   */
  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    CallingContext cc = ContextFactory.getCallingContext(this, req);

    Double openRosaVersion = getOpenRosaVersion(req);

    // verify request is multipart
    if (!ServletFileUpload.isMultipartContent(req)) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, ErrorConsts.NO_MULTI_PART_CONTENT);
      return;
    }

    boolean allowUpdates = true;
    List<AppSettings> settings = new ArrayList<AppSettings>();

    try {

      // process settings files

      MultiPartFormData uploadedFormItems = new MultiPartFormData(req);
      try {
        Set<Map.Entry<String, MultiPartFormItem>> fileSet = uploadedFormItems.getFileNameEntrySet();
        for (Map.Entry<String, MultiPartFormItem> itm : fileSet) {
          logger.info("*********************");
          logger.info("Current item: " + itm.getValue().getFilename());
          if (itm.getValue().getFilename().contains("admin")) {
            AppSettings existing = SettingsFactory.retrieveSettingsByName("admin", cc);
            logger.info(existing);
            logger.info(existing == null);
            logger.info("*********");
            if (existing == null) {
              logger.warn("************ im not null");
              AppSettings admin = new AppSettings(true, "admin", cc);
              admin.setSettingsFile(itm.getValue(), allowUpdates, cc);
              admin.persist(cc);
            } else {
              logger.info("im nul ***********");
              existing.updateSettings(itm.getValue(), cc);
              existing.persist(cc);
            }
          }
          if (itm.getValue().getFilename().contains("global")) {
              String name = itm.getValue().getFilename();
              AppSettings existing = SettingsFactory.retrieveSettingsByName("global", cc);
              logger.info(existing);
              if (existing == null) {
                AppSettings global = new AppSettings(true, "global", cc);
                global.setSettingsFile(itm.getValue(), allowUpdates, cc);
                global.persist(cc);
              } else {
                existing.updateSettings(itm.getValue(), cc);
                existing.persist(cc);
              }
          }
        }
      } catch (ODKDatastoreException e){
        logger.error("Settings upload persistence error: " + e.toString());
        e.printStackTrace(resp.getWriter());
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ErrorConsts.UPLOAD_PROBLEM);
      } catch (Exception e) {
        logger.error("Settings upload persistence error: " + e.toString());
        e.printStackTrace(resp.getWriter());
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ErrorConsts.UPLOAD_PROBLEM);
      }

    } catch (FileUploadException e) {
      logger.error("Settings upload persistence error: " + e.toString());
      e.printStackTrace(resp.getWriter());
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ErrorConsts.UPLOAD_PROBLEM);
    }
        // GAE requires some settle time before these entries will be
        // accurately retrieved. Do not re-fetch the form after it has been
        // uploaded.
    resp.setStatus(HttpServletResponse.SC_CREATED);
    resp.setHeader("Location", cc.getServerURL() + BasicConsts.FORWARDSLASH + ADDR);
    if (openRosaVersion == null) {
      // web page -- show HTML response
      resp.setContentType(HtmlConsts.RESP_TYPE_HTML);
      resp.setCharacterEncoding(HtmlConsts.UTF8_ENCODE);
      PrintWriter out = resp.getWriter();
      out.write(HtmlConsts.HTML_OPEN);
      out.write(HtmlConsts.BODY_OPEN);
      out.write("<p>Successful form upload.</p>");
    } else {
      addOpenRosaHeaders(resp);
      resp.setContentType(HtmlConsts.RESP_TYPE_XML);
      resp.setCharacterEncoding(HtmlConsts.UTF8_ENCODE);
      PrintWriter out = resp.getWriter();
      out.write("<OpenRosaResponse xmlns=\"http://openrosa.org/http/response\">");
      out.write("<message>Successful upload.</message>");
      out.write("</OpenRosaResponse>");
    }
  }

  private void createTitleQuestionWebpage(HttpServletResponse resp, String formXml,
                                          String xmlFileName, CallingContext cc) throws IOException {
   // beginBasicHtmlResponse(OBTAIN_TITLE_INFO, resp, cc); // header info

    PrintWriter out = resp.getWriter();
    out.write(HtmlUtil.createFormBeginTag(cc.getWebApplicationURL(FormUploadServlet.ADDR),
        HtmlConsts.MULTIPART_FORM_DATA, HtmlConsts.POST));
    //out.write(TITLE_OF_THE_XFORM + HtmlConsts.LINE_BREAK);
    out.write(HtmlUtil.createInput(HtmlConsts.INPUT_TYPE_TEXT, ServletConsts.FORM_NAME_PRAM, null));
    out.write(HtmlConsts.LINE_BREAK + HtmlConsts.LINE_BREAK);
    out.write(HtmlUtil.encodeFormInHiddenInput(formXml, xmlFileName));
    out.write(HtmlConsts.LINE_BREAK + HtmlConsts.LINE_BREAK);
    out.write(HtmlUtil.createInput(HtmlConsts.INPUT_TYPE_SUBMIT, null, "Submit"));
    out.write(HtmlConsts.FORM_CLOSE);
    finishBasicHtmlResponse(resp);
    System.out.println("\n\n\nsettings upload servlet\n\n\n");
  }

}
