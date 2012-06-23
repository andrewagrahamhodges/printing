package net.sourceforge.guacamole.printing;

/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is guacamole-common.
 *
 * The Initial Developer of the Original Code is
 * Jocelyn Delalande <j.delalande@ulteo.com>
 * Portions created by the Initial Developer are Copyright (C) 2012
 * Ulteo SAS <http://www.ulteo.com>
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */


import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import net.sourceforge.guacamole.*;
import net.sourceforge.guacamole.io.GuacamoleReader;
import net.sourceforge.guacamole.io.GuacamoleWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sourceforge.guacamole.protocol.GuacamoleConfiguration;  
 import net.sourceforge.guacamole.servlet.GuacamoleHTTPTunnelServlet;
/**
 * A HttpServlet to fetch printed jobs in PDF format and handle printing in general
 *
 * @author Jocelyn Delalande
 */
public class GuacamolePrinterServlet extends HttpServlet {

    private Logger logger = LoggerFactory.getLogger(GuacamoleHTTPTunnelServlet.class);

	/**
	 * Get the PDF.
	 * Issue a 404 error if the document does not exists and a n
	 */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		String path = request.getPathInfo();
		logger.info("PATHINFO {}", request.getPathInfo());
		String jobName = "";
		if (path.startsWith("/job/") && path.endsWith("/get") ) {
			try {
				int getPos = path.lastIndexOf("/get");
				jobName = path.substring(5, getPos);
				
				this.sendJobToBrowser(request, response, jobName);
			} catch (Exception e) {
				logger.info("Failed serving {}", jobName);
				throw new ServletException("Failed serving "+jobName);
			}
		} else {
			throw new ServletException("Invalid printer operation: " + request.getPathInfo());
		}
	}

	/** 
	 * Sends a PDF file (created by guacd) to the client.
	 */
    protected void sendJobToBrowser(HttpServletRequest request, HttpServletResponse response, String jobName) throws Exception {
		HttpSession httpSession = request.getSession(true);
		Map<String, GuacamoleConfiguration> configs = (Map<String, GuacamoleConfiguration>)(httpSession.getAttribute("GUAC_CONFIGS"));

		//FIXME: configure inside a properties file
		String pdfSpoolPath = new String("/var/spool/ulteo/pdf-printer/" + configs.get("DEFAULT").getParameter("username"));
		String pdfFileName = jobName + ".pdf";
		
		// Checks
		if (!new File(pdfSpoolPath).exists()) {
			throw new GuacamoleServerException("Spool directory "+pdfSpoolPath+" does not exists");
		}
		File jobPdfFile = new File(pdfSpoolPath, pdfFileName);
		if  (! jobPdfFile.exists()) {
			response.sendError(response.SC_NOT_FOUND,
							   "Unexistent printjob : "+ pdfFileName);
		}
		
		logger.info("serving {} print job over HTTP", jobPdfFile.toString());
		// Send the file over HTTP
		response.setContentType("application/pdf");
		response.setContentLength((int)jobPdfFile.length());
		
		FileInputStream in = new FileInputStream(jobPdfFile);
		OutputStream out = response.getOutputStream();
		
		byte[] buf = new byte[1024];
		int count = 0;
		while ((count = in.read(buf)) >= 0) {
			out.write(buf, 0, count);
		}
		in.close();
		out.close();
	}
}