package nl.info.webdav.methods;

import nl.info.webdav.ITransaction;
import nl.info.webdav.IWebdavStore;
import nl.info.webdav.StoredObject;
import nl.info.webdav.WebdavStatus;
import nl.info.webdav.exceptions.LockFailedException;
import nl.info.webdav.locking.IResourceLocks;
import nl.info.webdav.locking.LockedObject;

import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class DoUnlock extends DeterminableMethod {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DoUnlock.class);

    private final IWebdavStore _store;
    private final IResourceLocks _resourceLocks;
    private final boolean _readOnly;

    public DoUnlock(IWebdavStore store, IResourceLocks resourceLocks,
            boolean readOnly) {
        _store = store;
        _resourceLocks = resourceLocks;
        _readOnly = readOnly;
    }

    public void execute(ITransaction transaction, HttpServletRequest req,
            HttpServletResponse resp) throws IOException, LockFailedException {
        LOG.trace("-- " + this.getClass().getName());

        if (_readOnly) {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
        } else {
            String path = getRelativePath(req);
            String tempLockOwner = "doUnlock" + System.currentTimeMillis() + req;
            try {
                if (_resourceLocks.lock(transaction, path, tempLockOwner,
                        false, 0, TEMP_TIMEOUT, TEMPORARY)) {

                    String lockId = getLockIdFromLockTokenHeader(req);
                    LockedObject lo;
                    if (lockId != null
                            && ((lo = _resourceLocks.getLockedObjectByID(
                                    transaction, lockId)) != null)) {

                        String[] owners = lo.getOwner();
                        String owner = null;
                        if (lo.isShared()) {
                            // more than one owner is possible
                            if (owners != null) {
                                for (String s : owners) {
                                    // remove owner from LockedObject
                                    lo.removeLockedObjectOwner(s);
                                }
                            }
                        } else {
                            // exclusive, only one lock owner
                            if (owners != null)
                                owner = owners[0];
                        }

                        if (_resourceLocks.unlock(transaction, lockId, owner)) {
                            StoredObject so = _store.getStoredObject(
                                    transaction, path);
                            if (so.isNullResource()) {
                                _store.removeObject(transaction, path);
                            }

                            resp.setStatus(WebdavStatus.SC_NO_CONTENT);
                        } else {
                            LOG.trace("DoUnlock failure at " + lo.getPath());
                            resp.sendError(WebdavStatus.SC_METHOD_FAILURE);
                        }

                    } else {
                        resp.sendError(WebdavStatus.SC_BAD_REQUEST);
                    }
                }
            } catch (LockFailedException e) {
                LOG.error("Failed to unlock", e);
            } finally {
                _resourceLocks.unlockTemporaryLockedObjects(transaction, path,
                        tempLockOwner);
            }
        }
    }
}
