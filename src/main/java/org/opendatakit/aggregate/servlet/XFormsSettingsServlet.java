package org.opendatakit.aggregate.servlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.aggregate.ContextFactory;
import org.opendatakit.aggregate.constants.ServletConsts;
import org.opendatakit.aggregate.exception.ODKFormNotFoundException;
import org.opendatakit.aggregate.form.FormFactory;
import org.opendatakit.aggregate.form.IForm;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.persistence.exception.ODKOverQuotaException;
import org.opendatakit.common.web.CallingContext;
import org.opendatakit.common.web.constants.HtmlConsts;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Servlet that allows user to download the settings for particular form.
 * @author Anna
 */
public class XFormsSettingsServlet extends ServletUtilBase {
  /**
   * Serial number for serialization
   */
  private static final long serialVersionUID = 23886844567070038L;

  private static final Log logger = LogFactory.getLog(XFormsSettingsServlet.class.getName());
  /**
   * URI from base
   */
  public static final String ADDR = "xformsSettings";

  /**
   * Handler for HTTP Get request that responds with an XML list of forms to
   * download
   *
   * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse)
   */
  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    CallingContext cc = ContextFactory.getCallingContext(this, req);
    addOpenRosaHeaders(resp);

    // get parameters
    String formId = getParameter(req, ServletConsts.FORM_ID);
    if (formId == null) {
      errorMissingKeyParam(resp);
      return;
    }

    IForm form;
    String xml;
    try {
      form = FormFactory.retrieveFormByFormId(formId, cc);
      xml = form.getSettingsXml(cc);
    } catch (ODKFormNotFoundException e) {
      e.printStackTrace();
      odkIdNotFoundError(resp);
      return;
    } catch (ODKOverQuotaException e) {
      e.printStackTrace();
      quotaExceededError(resp);
      return;
    } catch (ODKDatastoreException e) {
      e.printStackTrace();
      datastoreError(resp);
      return;
    }
    logger.info(xml.substring(0,50));
    resp.setContentType(HtmlConsts.RESP_TYPE_XML);
    PrintWriter out = resp.getWriter();
    out.write(xml);
    out.flush();
  }
}

