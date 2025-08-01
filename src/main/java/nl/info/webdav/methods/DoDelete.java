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

import nl.info.webdav.ITransaction;
import nl.info.webdav.IWebdavStore;
import nl.info.webdav.StoredObject;
import nl.info.webdav.WebdavStatus;
import nl.info.webdav.exceptions.AccessDeniedException;
import nl.info.webdav.exceptions.LockFailedException;
import nl.info.webdav.exceptions.ObjectAlreadyExistsException;
import nl.info.webdav.exceptions.ObjectNotFoundException;
import nl.info.webdav.exceptions.WebdavException;
import nl.info.webdav.locking.ResourceLocks;

import java.io.IOException;
import java.util.Hashtable;
import java.util.logging.Logger;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class DoDelete extends AbstractMethod {
    private static Logger LOG = Logger.getLogger(DoDelete.class.getName());

    private final IWebdavStore _store;
    private final ResourceLocks _resourceLocks;
    private final boolean _readOnly;

    public DoDelete(IWebdavStore store, ResourceLocks resourceLocks,
            boolean readOnly) {
        _store = store;
        _resourceLocks = resourceLocks;
        _readOnly = readOnly;
    }

    public void execute(
        ITransaction transaction,
        HttpServletRequest req,
        HttpServletResponse resp
    ) throws IOException, LockFailedException {
        LOG.fine("-- " + this.getClass().getName());

        if (!_readOnly) {
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

            String tempLockOwner = "doDelete" + System.currentTimeMillis() + req;
            if (_resourceLocks.lock(transaction, path, tempLockOwner, false, 0,
                    TEMP_TIMEOUT, TEMPORARY)) {
                try {
                    errorList = new Hashtable<>();
                    deleteResource(transaction, path, errorList, req, resp);
                    if (!errorList.isEmpty()) {
                        sendReport(req, resp, errorList);
                    }
                } catch (AccessDeniedException e) {
                    resp.sendError(WebdavStatus.SC_FORBIDDEN);
                } catch (ObjectAlreadyExistsException e) {
                    resp.sendError(WebdavStatus.SC_NOT_FOUND, req
                            .getRequestURI());
                } catch (WebdavException e) {
                    resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
                } finally {
                    _resourceLocks.unlockTemporaryLockedObjects(transaction,
                            path, tempLockOwner);
                }
            } else {
                resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
            }
        } else {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
        }
    }

    /**
     * deletes the recources at "path"
     * 
     * @param transaction
     *      indicates that the method is within the scope of a WebDAV
     *      transaction
     * @param path
     *      the folder to be deleted
     * @param errorList
     *      all errors that ocurred
     * @param req
     *      HttpServletRequest
     * @param resp
     *      HttpServletResponse
     * @throws WebdavException
     *      if an error in the underlying store occurs
     * @throws IOException
     *      when an error occurs while sending the response
     */
    public void deleteResource(ITransaction transaction, String path,
            Hashtable<String, Integer> errorList, HttpServletRequest req,
            HttpServletResponse resp) throws IOException, WebdavException {

        resp.setStatus(WebdavStatus.SC_NO_CONTENT);

        if (!_readOnly) {
            StoredObject so = _store.getStoredObject(transaction, path);
            if (so != null) {

                if (so.isResource()) {
                    _store.removeObject(transaction, path);
                } else {
                    if (so.isFolder()) {
                        deleteFolder(transaction, path, errorList, req);
                        _store.removeObject(transaction, path);
                    } else {
                        resp.sendError(WebdavStatus.SC_NOT_FOUND);
                    }
                }
            } else {
                resp.sendError(WebdavStatus.SC_NOT_FOUND);
            }
        } else {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
        }
    }

    /**
     * 
     * helper method of deleteResource() deletes the folder and all of its
     * contents
     * 
     * @param transaction
     *      indicates that the method is within the scope of a WebDAV
     *      transaction
     * @param path
     *      the folder to be deleted
     * @param errorList
     *      all errors that ocurred
     * @param req
     *      HttpServletRequest
     * @throws WebdavException
     *      if an error in the underlying store occurs
     */
    private void deleteFolder(
        ITransaction transaction, String path,
        Hashtable<String, Integer> errorList,
        HttpServletRequest req
    ) throws WebdavException {
        String[] children = _store.getChildrenNames(transaction, path);
        children = children == null ? new String[] {} : children;
        for (int i = children.length - 1; i >= 0; i--) {
            children[i] = "/" + children[i];
            try {
                StoredObject so = _store.getStoredObject(transaction, path + children[i]);
                if (so.isResource()) {
                    _store.removeObject(transaction, path + children[i]);
                } else {
                    deleteFolder(transaction, path + children[i], errorList,
                            req);

                    _store.removeObject(transaction, path + children[i]);
                }
            } catch (AccessDeniedException e) {
                errorList.put(path + children[i], WebdavStatus.SC_FORBIDDEN);
            } catch (ObjectNotFoundException e) {
                errorList.put(path + children[i], WebdavStatus.SC_NOT_FOUND);
            } catch (WebdavException e) {
                errorList.put(path + children[i], WebdavStatus.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }
}
