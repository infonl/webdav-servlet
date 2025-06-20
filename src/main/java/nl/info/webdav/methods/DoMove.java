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
import nl.info.webdav.WebdavStatus;
import nl.info.webdav.exceptions.AccessDeniedException;
import nl.info.webdav.exceptions.LockFailedException;
import nl.info.webdav.exceptions.ObjectAlreadyExistsException;
import nl.info.webdav.exceptions.WebdavException;
import nl.info.webdav.locking.ResourceLocks;

import java.io.IOException;
import java.util.Hashtable;
import java.util.logging.Logger;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class DoMove extends AbstractMethod {
    private static final Logger LOG = Logger.getLogger(DoMove.class.getName());

    private final ResourceLocks _resourceLocks;
    private final DoDelete _doDelete;
    private final DoCopy _doCopy;
    private final boolean _readOnly;

    public DoMove(ResourceLocks resourceLocks, DoDelete doDelete,
            DoCopy doCopy, boolean readOnly) {
        _resourceLocks = resourceLocks;
        _doDelete = doDelete;
        _doCopy = doCopy;
        _readOnly = readOnly;
    }

    public void execute(ITransaction transaction, HttpServletRequest req,
            HttpServletResponse resp) throws IOException, LockFailedException {

        if (!_readOnly) {
            LOG.fine("-- " + this.getClass().getName());

            String sourcePath = getRelativePath(req);
            Hashtable<String, Integer> errorList = new Hashtable<>();

            if (!checkLocks(transaction, req, _resourceLocks, sourcePath)) {
                resp.setStatus(WebdavStatus.SC_LOCKED);
                return;
            }

            String destinationPath = req.getHeader("Destination");
            if (destinationPath == null) {
                resp.sendError(WebdavStatus.SC_BAD_REQUEST);
                return;
            }

            if (!checkLocks(transaction, req, _resourceLocks,
                    destinationPath)) {
                resp.setStatus(WebdavStatus.SC_LOCKED);
                return;
            }

            String tempLockOwner = "doMove" + System.currentTimeMillis()
                    + req;

            if (_resourceLocks.lock(transaction, sourcePath, tempLockOwner,
                    false, 0, TEMP_TIMEOUT, TEMPORARY)) {
                try {

                    if (_doCopy.copyResource(transaction, req, resp)) {

                        errorList = new Hashtable<>();
                        _doDelete.deleteResource(transaction, sourcePath,
                                errorList, req, resp);
                        if (!errorList.isEmpty()) {
                            sendReport(req, resp, errorList);
                        }
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
                            sourcePath, tempLockOwner);
                }
            } else {
                errorList.put(req.getHeader("Destination"),
                        WebdavStatus.SC_LOCKED);
                sendReport(req, resp, errorList);
            }
        } else {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);

        }
    }
}
