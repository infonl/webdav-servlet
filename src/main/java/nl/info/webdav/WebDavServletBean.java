package nl.info.webdav;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.HashMap;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import nl.info.webdav.exceptions.UnauthenticatedException;
import nl.info.webdav.exceptions.WebdavException;
import nl.info.webdav.locking.ResourceLocks;
import nl.info.webdav.methods.DoCopy;
import nl.info.webdav.methods.DoDelete;
import nl.info.webdav.methods.DoGet;
import nl.info.webdav.methods.DoHead;
import nl.info.webdav.methods.DoLock;
import nl.info.webdav.methods.DoMkcol;
import nl.info.webdav.methods.DoMove;
import nl.info.webdav.methods.DoNotImplemented;
import nl.info.webdav.methods.DoOptions;
import nl.info.webdav.methods.DoPropfind;
import nl.info.webdav.methods.DoProppatch;
import nl.info.webdav.methods.DoPut;
import nl.info.webdav.methods.DoUnlock;

public class WebDavServletBean extends HttpServlet {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(WebDavServletBean.class);

    /**
     * MD5 message digest provider.
     */
    protected static MessageDigest MD5_HELPER;

    private static final boolean READ_ONLY = false;
    private final HashMap<String, IMethodExecutor> _methodMap = new HashMap<>();
    protected ResourceLocks _resLocks;
    protected IWebdavStore _store;

    public WebDavServletBean() {
        _resLocks = new ResourceLocks();

        try {
            MD5_HELPER = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException();
        }
    }

    public void init(
            IWebdavStore store,
            String dftIndexFile,
            String insteadOf404,
            int nocontentLenghHeaders,
            boolean lazyFolderCreationOnPut
    ) throws ServletException {

        _store = store;

        IMimeTyper mimeTyper = new IMimeTyper() {
            public String getMimeType(ITransaction transaction, String path) {
                String retVal = _store.getStoredObject(transaction, path).getMimeType();
                if (retVal == null) {
                    retVal = getServletContext().getMimeType(path);
                }
                return retVal;
            }
        };

        register("GET", new DoGet(store, dftIndexFile, insteadOf404, _resLocks,
                mimeTyper, nocontentLenghHeaders));
        register("HEAD", new DoHead(store, dftIndexFile, insteadOf404,
                _resLocks, mimeTyper, nocontentLenghHeaders));
        DoDelete doDelete = (DoDelete) register("DELETE", new DoDelete(store,
                _resLocks, READ_ONLY));
        DoCopy doCopy = (DoCopy) register("COPY", new DoCopy(store, _resLocks,
                doDelete, READ_ONLY));
        register("LOCK", new DoLock(store, _resLocks, READ_ONLY));
        register("UNLOCK", new DoUnlock(store, _resLocks, READ_ONLY));
        register("MOVE", new DoMove(_resLocks, doDelete, doCopy, READ_ONLY));
        register("MKCOL", new DoMkcol(store, _resLocks, READ_ONLY));
        register("OPTIONS", new DoOptions(store, _resLocks));
        register("PUT", new DoPut(store, _resLocks, READ_ONLY,
                lazyFolderCreationOnPut));
        register("PROPFIND", new DoPropfind(store, _resLocks, mimeTyper));
        register("PROPPATCH", new DoProppatch(store, _resLocks, READ_ONLY));
        register("*NO*IMPL*", new DoNotImplemented(READ_ONLY));
    }

    @Override
    public void destroy() {
        if (_store != null)
            _store.destroy();
        super.destroy();
    }

    protected IMethodExecutor register(String methodName, IMethodExecutor method) {
        _methodMap.put(methodName, method);
        return method;
    }

    /**
     * Handles the special WebDAV methods.
     */
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
                                                                             throws ServletException, IOException {

        String methodName = req.getMethod();
        ITransaction transaction = null;
        boolean needRollback = false;

        if (LOG.isTraceEnabled())
            debugRequest(methodName, req);

        try {
            Principal userPrincipal = getUserPrincipal(req);
            transaction = _store.begin(userPrincipal);
            needRollback = true;
            _store.checkAuthentication(transaction);
            resp.setStatus(WebdavStatus.SC_OK);

            try {
                IMethodExecutor methodExecutor = (IMethodExecutor) _methodMap
                        .get(methodName);
                if (methodExecutor == null) {
                    methodExecutor = (IMethodExecutor) _methodMap
                            .get("*NO*IMPL*");
                }

                methodExecutor.execute(transaction, req, resp);

                _store.commit(transaction);

                // Clear input stream if available otherwise later access
                // include current input. This occurs if the client
                // sends a request with body to an not existing resource.
                if (req.getContentLength() != 0 && req.getInputStream().available() > 0) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Clear not consumed data!");
                    }
                    while (req.getInputStream().available() > 0) {
                        req.getInputStream().read();
                    }
                }
                needRollback = false;
            } catch (IOException e) {
                java.io.StringWriter sw = new java.io.StringWriter();
                java.io.PrintWriter pw = new java.io.PrintWriter(sw);
                e.printStackTrace(pw);
                LOG.error("IOException: " + sw);
                resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
                _store.rollback(transaction);
                throw new ServletException(e);
            }

        } catch (UnauthenticatedException e) {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
        } catch (WebdavException e) {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            e.printStackTrace(pw);
            LOG.error("WebdavException: " + sw);
            throw new ServletException(e);
        } catch (Exception e) {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            e.printStackTrace(pw);
            LOG.error("Exception: " + sw);
        } finally {
            if (needRollback)
                _store.rollback(transaction);
        }
    }

    /**
     * Method that permit to customize the way
     * user information are extracted from the request, default use JAAS
     * 
     * @param req the request
     * @return the principal
     */
    protected Principal getUserPrincipal(HttpServletRequest req) {
        return req.getUserPrincipal();
    }

    private void debugRequest(String methodName, HttpServletRequest req) {
        LOG.trace("-----------");
        LOG.trace("WebdavServlet\n request: methodName = " + methodName);
        LOG.trace("time: " + System.currentTimeMillis());
        LOG.trace("path: " + req.getRequestURI());
        LOG.trace("-----------");
        Enumeration<?> e = req.getHeaderNames();
        while (e.hasMoreElements()) {
            String s = (String) e.nextElement();
            LOG.trace("header: " + s + " " + req.getHeader(s));
        }
        e = req.getAttributeNames();
        while (e.hasMoreElements()) {
            String s = (String) e.nextElement();
            LOG.trace("attribute: " + s + " " + req.getAttribute(s));
        }
        e = req.getParameterNames();
        while (e.hasMoreElements()) {
            String s = (String) e.nextElement();
            LOG.trace("parameter: " + s + " " + req.getParameter(s));
        }
    }
}
