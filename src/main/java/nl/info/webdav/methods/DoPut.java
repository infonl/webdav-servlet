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
import nl.info.webdav.exceptions.WebdavException;
import nl.info.webdav.locking.IResourceLocks;
import nl.info.webdav.locking.LockedObject;

import java.io.IOException;
import java.util.Hashtable;
import java.util.logging.Logger;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class DoPut extends AbstractMethod {
    private static final Logger LOG = Logger.getLogger(DoPut.class.getName());

    private final IWebdavStore _store;
    private final IResourceLocks _resourceLocks;
    private final boolean _readOnly;
    private final boolean _lazyFolderCreationOnPut;

    private String _userAgent;

    public DoPut(IWebdavStore store, IResourceLocks resLocks, boolean readOnly, boolean lazyFolderCreationOnPut) {
        _store = store;
        _resourceLocks = resLocks;
        _readOnly = readOnly;
        _lazyFolderCreationOnPut = lazyFolderCreationOnPut;
    }

    public void execute(
        ITransaction transaction,
        HttpServletRequest req,
        HttpServletResponse resp
    ) throws IOException, LockFailedException {
        LOG.fine("-- " + this.getClass().getName());

        if (!_readOnly) {
            String path = getRelativePath(req);
            String parentPath = getParentPath(path);

            _userAgent = req.getHeader("User-Agent");

            Hashtable<String, Integer> errorList = new Hashtable<>();

            if (!checkLocks(transaction, req, _resourceLocks, parentPath)) {
                resp.setStatus(WebdavStatus.SC_LOCKED);
                return; // parent is locked
            }

            if (!checkLocks(transaction, req, _resourceLocks, path)) {
                resp.setStatus(WebdavStatus.SC_LOCKED);
                return; // resource is locked
            }

            String tempLockOwner = "doPut" + System.currentTimeMillis() + req;
            if (_resourceLocks.lock(transaction, path, tempLockOwner, false, 0,
                    TEMP_TIMEOUT, TEMPORARY)) {
                StoredObject parentSo, so = null;
                try {
                    parentSo = _store.getStoredObject(transaction, parentPath);
                    if (parentPath != null && parentSo != null
                            && parentSo.isResource()) {
                        resp.sendError(WebdavStatus.SC_FORBIDDEN);
                        return;

                    } else if (parentPath != null && parentSo == null && _lazyFolderCreationOnPut) {
                        _store.createFolder(transaction, parentPath);

                    } else if (parentPath != null && parentSo == null && !_lazyFolderCreationOnPut) {
                        errorList.put(parentPath, WebdavStatus.SC_NOT_FOUND);
                        sendReport(req, resp, errorList);
                        return;
                    }

                    so = _store.getStoredObject(transaction, path);

                    if (so == null) {
                        _store.createResource(transaction, path);
                        // resp.setStatus(WebdavStatus.SC_CREATED);
                    } else {
                        // This has already been created, just update the data
                        if (so.isNullResource()) {

                            LockedObject nullResourceLo = _resourceLocks
                                    .getLockedObjectByPath(transaction, path);
                            if (nullResourceLo == null) {
                                resp
                                        .sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
                                return;
                            }
                            String nullResourceLockToken = nullResourceLo
                                    .getID();
                            String[] lockTokens = getLockIdFromIfHeader(req);
                            String lockToken = null;
                            if (lockTokens != null) {
                                lockToken = lockTokens[0];
                            } else {
                                resp.sendError(WebdavStatus.SC_BAD_REQUEST);
                                return;
                            }
                            if (lockToken.equals(nullResourceLockToken)) {
                                so.setNullResource(false);
                                so.setFolder(false);

                                String[] nullResourceLockOwners = nullResourceLo
                                        .getOwner();
                                String owner = null;
                                if (nullResourceLockOwners != null)
                                    owner = nullResourceLockOwners[0];

                                if (!_resourceLocks.unlock(transaction,
                                        lockToken, owner)) {
                                    resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
                                }
                            } else {
                                errorList.put(path, WebdavStatus.SC_LOCKED);
                                sendReport(req, resp, errorList);
                            }
                        }
                    }
                    // User-Agent workarounds
                    doUserAgentWorkaround(resp);

                    // setting resourceContent
                    long resourceLength = _store
                            .setResourceContent(transaction, path, req
                                    .getInputStream(), null, null);

                    so = _store.getStoredObject(transaction, path);
                    if (resourceLength != -1)
                        so.setResourceLength(resourceLength);
                    // Now lets report back what was actually saved

                } catch (AccessDeniedException e) {
                    resp.sendError(WebdavStatus.SC_FORBIDDEN);
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

    private void doUserAgentWorkaround(HttpServletResponse resp) {
        if (_userAgent != null && _userAgent.contains("WebDAVFS")
                && !_userAgent.contains("Transmit")) {
            LOG.fine("DoPut.execute() : do workaround for user agent '" + _userAgent + "'");
            resp.setStatus(WebdavStatus.SC_CREATED);
        } else if (_userAgent != null && _userAgent.contains("Transmit")) {
            // Transmit also uses WEBDAVFS 1.x.x but crashes
            // with SC_CREATED response
            LOG.fine("DoPut.execute() : do workaround for user agent '" + _userAgent + "'");
            resp.setStatus(WebdavStatus.SC_NO_CONTENT);
        } else {
            resp.setStatus(WebdavStatus.SC_CREATED);
        }
    }
}
