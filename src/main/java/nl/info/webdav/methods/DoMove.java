/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.webdav.methods;

import java.io.IOException;
import java.util.Hashtable;
import java.util.logging.Logger;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import nl.info.webdav.ITransaction;
import nl.info.webdav.WebdavStatus;
import nl.info.webdav.exceptions.AccessDeniedException;
import nl.info.webdav.exceptions.LockFailedException;
import nl.info.webdav.exceptions.ObjectAlreadyExistsException;
import nl.info.webdav.exceptions.WebdavException;
import nl.info.webdav.locking.ResourceLocks;

public class DoMove extends AbstractMethod {
    private static final Logger LOG = Logger.getLogger(DoMove.class.getName());

    private final ResourceLocks _resourceLocks;
    private final DoDelete _doDelete;
    private final DoCopy _doCopy;
    private final boolean _readOnly;

    public DoMove(
            ResourceLocks resourceLocks,
            DoDelete doDelete,
            DoCopy doCopy,
            boolean readOnly
    ) {
        _resourceLocks = resourceLocks;
        _doDelete = doDelete;
        _doCopy = doCopy;
        _readOnly = readOnly;
    }

    @Override
    public void execute(
            ITransaction transaction,
            HttpServletRequest req,
            HttpServletResponse resp
    ) throws IOException, LockFailedException {

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

            String tempLockOwner = "doMove" + System.currentTimeMillis() + req;

            if (_resourceLocks.lock(transaction, sourcePath, tempLockOwner,
                    false, 0, TEMP_TIMEOUT, TEMPORARY)) {
                try {

                    if (_doCopy.copyResource(transaction, req, resp)) {

                        errorList = new Hashtable<>();
                        _doDelete.deleteResource(transaction, sourcePath, errorList, resp);
                        if (!errorList.isEmpty()) {
                            sendReport(resp, errorList);
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
                sendReport(resp, errorList);
            }
        } else {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);

        }
    }
}
