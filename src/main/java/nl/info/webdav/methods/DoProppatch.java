package nl.info.webdav.methods;

import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import nl.info.webdav.ITransaction;
import nl.info.webdav.IWebdavStore;
import nl.info.webdav.StoredObject;
import nl.info.webdav.WebdavStatus;
import nl.info.webdav.exceptions.AccessDeniedException;
import nl.info.webdav.exceptions.LockFailedException;
import nl.info.webdav.exceptions.WebdavException;
import nl.info.webdav.fromcatalina.XMLHelper;
import nl.info.webdav.fromcatalina.XMLWriter;
import nl.info.webdav.locking.LockedObject;
import nl.info.webdav.locking.ResourceLocks;

public class DoProppatch extends AbstractMethod {
    private static final Logger LOG = Logger.getLogger(DoProppatch.class.getName());

    private boolean _readOnly;
    private IWebdavStore _store;
    private ResourceLocks _resourceLocks;

    public DoProppatch(
            IWebdavStore store,
            ResourceLocks resLocks,
            boolean readOnly
    ) {
        _readOnly = readOnly;
        _store = store;
        _resourceLocks = resLocks;
    }

    public void execute(
            ITransaction transaction,
            HttpServletRequest req,
            HttpServletResponse resp
    ) throws IOException, LockFailedException {
        LOG.fine("-- " + this.getClass().getName());

        if (_readOnly) {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
            return;
        }

        String path = getRelativePath(req);
        String parentPath = getParentPath(getCleanPath(path));

        Hashtable<String, Integer> errorList;

        if (!checkLocks(transaction, req, _resourceLocks, parentPath)) {
            resp.setStatus(WebdavStatus.SC_LOCKED);
            return; // parent is locked
        }

        if (!checkLocks(transaction, req, _resourceLocks, path)) {
            resp.setStatus(WebdavStatus.SC_LOCKED);
            return; // resource is locked
        }

        // TODO for now, PROPPATCH just sends a valid response, stating that
        // everything is fine, but doesn't do anything.

        // Retrieve the resources
        String tempLockOwner = "doProppatch" + System.currentTimeMillis() + req;

        if (_resourceLocks.lock(transaction, path, tempLockOwner, false, 0,
                TEMP_TIMEOUT, TEMPORARY)) {
            StoredObject so;
            LockedObject lo;
            try {
                so = _store.getStoredObject(transaction, path);
                lo = _resourceLocks.getLockedObjectByPath(transaction,
                        getCleanPath(path));

                if (so == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                    // we do not to continue since there is no root resource
                }

                if (so.isNullResource()) {
                    String methodsAllowed = DeterminableMethod
                            .determineMethodsAllowed(so);
                    resp.addHeader("Allow", methodsAllowed);
                    resp.sendError(WebdavStatus.SC_METHOD_NOT_ALLOWED);
                    return;
                }

                String[] lockTokens = getLockIdFromIfHeader(req);
                boolean lockTokenMatchesIfHeader = (lockTokens != null && lockTokens[0].equals(lo.getID()));
                if (lo != null && lo.isExclusive() && !lockTokenMatchesIfHeader) {
                    // Object on specified path is LOCKED
                    errorList = new Hashtable<>();
                    errorList.put(path, WebdavStatus.SC_LOCKED);
                    sendReport(req, resp, errorList);
                    return;
                }

                List<String> toset;
                List<String> toremove;
                List<String> tochange = new Vector<>();
                // contains all properties from toset and toremove

                path = getCleanPath(getRelativePath(req));

                Node tosetNode;
                Node toremoveNode;

                if (req.getContentLength() != 0) {
                    try {
                        DocumentBuilder documentBuilder = getDocumentBuilder();
                        Document document = documentBuilder
                                .parse(new InputSource(req.getInputStream()));
                        // Get the root element of the document
                        Element rootElement = document.getDocumentElement();

                        tosetNode = XMLHelper.findSubElement(XMLHelper
                                .findSubElement(rootElement, "set"), "prop");
                        toremoveNode = XMLHelper.findSubElement(XMLHelper
                                .findSubElement(rootElement, "remove"), "prop");
                    } catch (Exception e) {
                        resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
                        return;
                    }
                } else {
                    // no content: error
                    resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
                    return;
                }

                HashMap<String, String> namespaces = new HashMap<>();
                namespaces.put("DAV:", "D");

                if (tosetNode != null) {
                    toset = XMLHelper.getPropertiesFromXML(tosetNode);
                    tochange.addAll(toset);
                }

                if (toremoveNode != null) {
                    toremove = XMLHelper.getPropertiesFromXML(toremoveNode);
                    tochange.addAll(toremove);
                }

                resp.setStatus(WebdavStatus.SC_MULTI_STATUS);
                resp.setContentType("text/xml; charset=UTF-8");

                // Create multi status object
                XMLWriter generatedXML = new XMLWriter(resp.getWriter(),
                        namespaces);
                generatedXML.writeXMLHeader();
                generatedXML
                        .writeElement("DAV::multistatus", XMLWriter.OPENING);

                generatedXML.writeElement("DAV::response", XMLWriter.OPENING);
                String status = "HTTP/1.1 " + WebdavStatus.SC_OK + " " + WebdavStatus.getStatusText(WebdavStatus.SC_OK);

                // Generating href element
                generatedXML.writeElement("DAV::href", XMLWriter.OPENING);

                String href = req.getContextPath();
                if ((href.endsWith("/")) && (path.startsWith("/")))
                    href += path.substring(1);
                else
                    href += path;
                if ((so.isFolder()) && (!href.endsWith("/")))
                    href += "/";

                generatedXML.writeText(rewriteUrl(href));

                generatedXML.writeElement("DAV::href", XMLWriter.CLOSING);

                for (Iterator<String> iter = tochange.iterator(); iter
                        .hasNext();) {
                    String property = iter.next();

                    generatedXML.writeElement("DAV::propstat", XMLWriter.OPENING);
                    generatedXML.writeElement("DAV::prop", XMLWriter.OPENING);
                    generatedXML.writeElement(property, XMLWriter.NO_CONTENT);
                    generatedXML.writeElement("DAV::prop", XMLWriter.CLOSING);
                    generatedXML.writeElement("DAV::status", XMLWriter.OPENING);
                    generatedXML.writeText(status);
                    generatedXML.writeElement("DAV::status", XMLWriter.CLOSING);
                    generatedXML.writeElement("DAV::propstat", XMLWriter.CLOSING);
                }

                generatedXML.writeElement("DAV::response", XMLWriter.CLOSING);
                generatedXML.writeElement("DAV::multistatus", XMLWriter.CLOSING);

                generatedXML.sendData();
            } catch (AccessDeniedException e) {
                resp.sendError(WebdavStatus.SC_FORBIDDEN);
            } catch (WebdavException e) {
                resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
            } finally {
                _resourceLocks.unlockTemporaryLockedObjects(transaction, path,
                        tempLockOwner);
            }
        } else {
            resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
