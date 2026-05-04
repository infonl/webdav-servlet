/*
 * Copyright 1999,2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.info.webdav.methods;

import nl.info.webdav.IMimeTyper;
import nl.info.webdav.ITransaction;
import nl.info.webdav.IWebdavStore;
import nl.info.webdav.StoredObject;
import nl.info.webdav.WebdavStatus;
import nl.info.webdav.locking.ResourceLocks;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class DoGet extends DoHead {
    private static final Logger LOG = Logger.getLogger(DoGet.class.getName());

    public DoGet(IWebdavStore store, String dftIndexFile, String insteadOf404,
            ResourceLocks resourceLocks, IMimeTyper mimeTyper,
            int contentLengthHeader) {
        super(store, dftIndexFile, insteadOf404, resourceLocks, mimeTyper,
                contentLengthHeader);
    }

    protected void doBody(ITransaction transaction, HttpServletResponse resp, String path) {
        try {
            StoredObject so = _store.getStoredObject(transaction, path);
            if (so.isNullResource()) {
                String methodsAllowed = DeterminableMethod
                        .determineMethodsAllowed(so);
                resp.addHeader("Allow", methodsAllowed);
                resp.sendError(WebdavStatus.SC_METHOD_NOT_ALLOWED);
                return;
            }
            OutputStream out = resp.getOutputStream();
            InputStream in = _store.getResourceContent(transaction, path);
            try {
                int read;
                byte[] copyBuffer = new byte[BUF_SIZE];

                while ((read = in.read(copyBuffer, 0, copyBuffer.length)) != -1) {
                    out.write(copyBuffer, 0, read);
                }
            } finally {
                // flushing causes an IOException if a file is opened on the webserver
                // client disconnected before the server finished sending response
                try {
                    in.close();
                } catch (IOException ioException) {
                    LOG.log(Level.WARNING, "Failed to close InputStream", ioException);
                }
                try {
                    out.flush();
                    out.close();
                } catch (IOException ioException) {
                    LOG.log(Level.WARNING, "Failed to close OutputStream", ioException);
                }
            }
        } catch (Exception exception) {
            LOG.log(Level.WARNING, "Failed to copy buffer", exception);
        }
    }

    protected void folderBody(ITransaction transaction, String path,
            HttpServletResponse resp, HttpServletRequest req)
            throws IOException {

        StoredObject so = _store.getStoredObject(transaction, path);
        if (so == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, req
                    .getRequestURI());
        } else {

            if (so.isNullResource()) {
                String methodsAllowed = DeterminableMethod
                        .determineMethodsAllowed(so);
                resp.addHeader("Allow", methodsAllowed);
                resp.sendError(WebdavStatus.SC_METHOD_NOT_ALLOWED);
                return;
            }

            if (so.isFolder()) {
                // TODO some folder response (for browsers, DAV tools
                // use propfind) in html?
                DateFormat shortDF= getDateTimeFormat(req.getLocale());
                resp.setContentType("text/html");
                resp.setCharacterEncoding("UTF8");
                OutputStream out = resp.getOutputStream();
                String[] children = _store.getChildrenNames(transaction, path);
                // Make sure it's not null
                children = children == null ? new String[] {} : children;
                // Sort by name
                Arrays.sort(children);
                StringBuilder childrenTemp = new StringBuilder();
                childrenTemp.append("<html><head><title>Content of folder");
                childrenTemp.append(escapeHtml(path));
                childrenTemp.append("</title><style type=\"text/css\">");
                childrenTemp.append(getCSS());
                childrenTemp.append("</style></head>");
                childrenTemp.append("<body>");
                childrenTemp.append(getHeader(path));
                childrenTemp.append("<table>");
                childrenTemp.append("<tr><th>Name</th><th>Size</th><th>Created</th><th>Modified</th></tr>");
                childrenTemp.append("<tr>");
                childrenTemp.append("<td colspan=\"4\"><a href=\"../\">Parent</a></td></tr>");
                boolean isEven= false;
                for (String child : children)
                {
                    isEven= !isEven;
                    childrenTemp.append("<tr class=\"");
                    childrenTemp.append(isEven ? "even" : "odd");
                    childrenTemp.append("\">");
                    childrenTemp.append("<td>");
                    childrenTemp.append("<a href=\"");
                    childrenTemp.append(escapeHtml(child));
                    StoredObject obj= _store.getStoredObject(transaction, path + "/" +child);
                    if (obj == null)
                    {
                        LOG.severe(String.format("Should not return null for '%s/%s'", path, child));
                    }
                    if (obj != null && obj.isFolder())
                    {
                        childrenTemp.append("/");
                    }
                    childrenTemp.append("\">");
                    childrenTemp.append(escapeHtml(child));
                    childrenTemp.append("</a></td>");
                    if (obj != null && obj.isFolder())
                    {
                        childrenTemp.append("<td>Folder</td>");
                    }
                    else
                    {
                        childrenTemp.append("<td>");
                        if (obj != null )
                        {
                            childrenTemp.append(obj.getResourceLength());
                        }
                        else
                        {
                            childrenTemp.append("Unknown");
                        }
                        childrenTemp.append(" Bytes</td>");
                    }
                    if (obj != null && obj.getCreationDate() != null)
                    {
                        childrenTemp.append("<td>");
                        childrenTemp.append(shortDF.format(obj.getCreationDate()));
                        childrenTemp.append("</td>");
                    }
                    else
                    {
                        childrenTemp.append("<td></td>");
                    }
                    if (obj != null  && obj.getLastModified() != null)
                    {
                        childrenTemp.append("<td>");
                        childrenTemp.append(shortDF.format(obj.getLastModified()));
                        childrenTemp.append("</td>");
                    }
                    else
                    {
                        childrenTemp.append("<td></td>");
                    }
                    childrenTemp.append("</tr>");
                }
                childrenTemp.append("</table>");
                childrenTemp.append("</body></html>");
                out.write(childrenTemp.toString().getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    /**
     * Return the CSS styles used to display the HTML representation
     * of the webdav content.
     * 
     * @return the CSS styles
     */
    protected String getCSS()
    {
        // The default styles to use
       String retVal= """
               body {
               	font-family: Arial, Helvetica, sans-serif;
               }
               h1 {
               	font-size: 1.5em;
               }
               th {
               	background-color: #9DACBF;
               }
               table {
               	border-top-style: solid;
               	border-right-style: solid;
               	border-bottom-style: solid;
               	border-left-style: solid;
               }
               td {
               	margin: 0px;
               	padding-top: 2px;
               	padding-right: 5px;
               	padding-bottom: 2px;
               	padding-left: 5px;
               }
               tr.even {
               	background-color: #CCCCCC;
               }
               tr.odd {
               	background-color: #FFFFFF;
               }
               """;
        // Try loading one via class loader and use that one instead
        ClassLoader cl = getClass().getClassLoader();
        try (InputStream iStream = cl.getResourceAsStream("webdav.css")) {
            if (iStream != null) {
                // Found css via class loader, use that one
                StringBuilder out = new StringBuilder();
                byte[] b = new byte[4096];
                for (int n; (n = iStream.read(b)) != -1;) {
                    out.append(new String(b, 0, n));
                }
                retVal= out.toString();
            }
        }
        catch (Exception exception) {
            LOG.log(Level.SEVERE, "Error in reading webdav.css", exception);
        }

        return retVal;
    }

    /**
     * Return the header to be displayed in front of the folder content
     * 
     * @param path the path
     * @return the header
     */
    protected String getHeader(String path) {
        return "<h1>Content of folder " + escapeHtml(path) + "</h1>";
    }

    private static String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;");
    }

    /**
     * Return this as the Date/Time format for displaying Creation + Modification dates
     *
     * @param browserLocale the browser locale
     * @return DateFormat used to display creation and modification dates
     */
    protected DateFormat getDateTimeFormat(Locale browserLocale) {
        return SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.MEDIUM, browserLocale);
    }
 }
