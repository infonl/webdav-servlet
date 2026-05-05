/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.webdav;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.servlet.ServletException;

import nl.info.webdav.exceptions.WebdavException;

/**
 * Servlet which provides support for WebDAV level 2.
 * <p>
 * the original class is org.apache.catalina.servlets.WebdavServlet by Remy
 * Maucherat, which was heavily changed
 * 
 * @author Remy Maucherat
 */
public class WebdavServlet extends WebDavServletBean {
    private static final Logger LOG = Logger.getLogger(WebdavServlet.class.getName());
    private static final String ROOT_PATH_PARAMETER = "rootpath";

    public void init() throws ServletException {

        // Parameters from web.xml
        String clazzName = getServletConfig().getInitParameter("ResourceHandlerImplementation");
        if (clazzName == null || clazzName.isEmpty()) {
            clazzName = LocalFileSystemStore.class.getName();
        }

        File root = getFileRoot();

        IWebdavStore webdavStore = constructStore(clazzName, root);

        boolean lazyFolderCreationOnPut = getInitParameter("lazyFolderCreationOnPut") != null && getInitParameter("lazyFolderCreationOnPut")
                .equals("1");

        String dftIndexFile = getInitParameter("default-index-file");
        String insteadOf404 = getInitParameter("instead-of-404");

        int noContentLengthHeader = getIntInitParameter("no-content-length-headers");

        super.init(webdavStore, dftIndexFile, insteadOf404,
                noContentLengthHeader, lazyFolderCreationOnPut);
    }

    private int getIntInitParameter(String key) {
        return getInitParameter(key) == null ? -1 : Integer
                .parseInt(getInitParameter(key));
    }

    protected IWebdavStore constructStore(String clazzName, File root) {
        IWebdavStore webdavStore;
        try {
            Class<?> clazz = WebdavServlet.class.getClassLoader().loadClass(
                    clazzName);

            Constructor<?> ctor = clazz
                    .getConstructor(File.class);

            webdavStore = (IWebdavStore) ctor
                    .newInstance(new Object[]{root});
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to construct store", e);
            throw new RuntimeException("Failed to construct store", e);
        }
        return webdavStore;
    }

    private File getFileRoot() {
        String rootPath = getInitParameter(ROOT_PATH_PARAMETER);
        if (rootPath == null) {
            throw new WebdavException("missing parameter: " + ROOT_PATH_PARAMETER);
        }
        if (rootPath.equals("*WAR-FILE-ROOT*")) {
            String file = LocalFileSystemStore.class.getProtectionDomain()
                    .getCodeSource().getLocation().getFile().replace('\\', '/');
            if (file.charAt(0) == '/' && System.getProperty("os.name").contains("Windows")) {
                file = file.substring(1);
            }

            int ix = file.indexOf("/WEB-INF/");
            if (ix != -1) {
                rootPath = file.substring(0, ix).replace('/',
                        File.separatorChar);
            } else {
                throw new WebdavException(
                        "Could not determine root of war file. Can't extract from path '" + file + "' for this web container");
            }
        }
        return new File(rootPath);
    }
}
