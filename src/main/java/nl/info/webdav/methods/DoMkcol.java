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

public class DoMkcol extends AbstractMethod {
    private static final Logger LOG = Logger.getLogger(DoMkcol.class.getName());

    private final IWebdavStore _store;
    private final IResourceLocks _resourceLocks;
    private final boolean _readOnly;

    public DoMkcol(IWebdavStore store, IResourceLocks resourceLocks,
            boolean readOnly) {
        _store = store;
        _resourceLocks = resourceLocks;
        _readOnly = readOnly;
    }

    public void execute(ITransaction transaction, HttpServletRequest req,
            HttpServletResponse resp) throws IOException, LockFailedException {
        LOG.fine("-- " + this.getClass().getName());

        if (!_readOnly) {
            String path = getRelativePath(req);
            String parentPath = getParentPath(getCleanPath(path));
            Hashtable<String, Integer> errorList = new Hashtable<>();

            if (!checkLocks(transaction, req, _resourceLocks, parentPath)) {
                resp.sendError(WebdavStatus.SC_FORBIDDEN);
                return;
            }

            String tempLockOwner = "doMkcol" + System.currentTimeMillis()
                    + req.toString();

            if (_resourceLocks.lock(transaction, path, tempLockOwner, false, 0,
                    TEMP_TIMEOUT, TEMPORARY)) {
                StoredObject parentSo, so;
                try {
                    parentSo = _store.getStoredObject(transaction, parentPath);
					if (parentSo == null) {
						// parent not exists
						resp.sendError(WebdavStatus.SC_CONFLICT);
						return;
					}
					if (parentPath != null && parentSo.isFolder()) {
                        so = _store.getStoredObject(transaction, path);
                        if (so == null) {
                            _store.createFolder(transaction, path);
                            resp.setStatus(WebdavStatus.SC_CREATED);
                        } else {
                            // object already exists
                            if (so.isNullResource()) {

                                LockedObject nullResourceLo = _resourceLocks
                                        .getLockedObjectByPath(transaction,
                                                path);
                                if (nullResourceLo == null) {
                                    resp
                                            .sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
                                    return;
                                }
                                String nullResourceLockToken = nullResourceLo
                                        .getID();
                                String[] lockTokens = getLockIdFromIfHeader(req);
                                String lockToken;
                                if (lockTokens != null)
                                    lockToken = lockTokens[0];
                                else {
                                    resp.sendError(WebdavStatus.SC_BAD_REQUEST);
                                    return;
                                }
                                if (lockToken.equals(nullResourceLockToken)) {
                                    so.setNullResource(false);
                                    so.setFolder(true);

                                    String[] nullResourceLockOwners = nullResourceLo
                                            .getOwner();
                                    String owner = null;
                                    if (nullResourceLockOwners != null)
                                        owner = nullResourceLockOwners[0];

                                    if (_resourceLocks.unlock(transaction,
                                            lockToken, owner)) {
                                        resp.setStatus(WebdavStatus.SC_CREATED);
                                    } else {
                                        resp
                                                .sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
                                    }
                                } else {
                                    errorList.put(path, WebdavStatus.SC_LOCKED);
                                    sendReport(req, resp, errorList);
                                }
                            } else {
                                String methodsAllowed = DeterminableMethod
                                        .determineMethodsAllowed(so);
                                resp.addHeader("Allow", methodsAllowed);
                                resp.sendError(WebdavStatus.SC_METHOD_NOT_ALLOWED);
                            }
                        }

					} else if (parentPath != null && parentSo.isResource()) {
                        String methodsAllowed = DeterminableMethod
                                .determineMethodsAllowed(parentSo);
                        resp.addHeader("Allow", methodsAllowed);
                        resp.sendError(WebdavStatus.SC_METHOD_NOT_ALLOWED);
                    } else {
                        resp.sendError(WebdavStatus.SC_FORBIDDEN);
                    }
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
}
