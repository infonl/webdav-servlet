package nl.info.webdav.locking;

import nl.info.webdav.ITransaction;
import nl.info.webdav.exceptions.LockFailedException;

public interface IResourceLocks {

    /**
     * Tries to lock the resource at "path".
     * 
     * @param transaction the transaction
     * @param path what resource to lock
     * @param owner the owner of the lock
     * @param exclusive if the lock should be exclusive (or shared)
     * @param depth depth
     * @param timeout lock duration in seconds.
     * @return true if the resource at path was successfully locked, false if an
     *  existing lock prevented this
     * @throws LockFailedException if the lock failed
     */
    boolean lock(
        ITransaction transaction,
        String path,
        String owner,
        boolean exclusive,
        int depth,
        int timeout,
        boolean temporary
    ) throws LockFailedException;

    /**
     * Unlocks all resources at "path" (and all sub-folders if existing) that
     * have the same owner.
     * 
     * @param transaction the transaction
     * @param id id to the resource to unlock
     * @param owner who wants to unlock
     */
    boolean unlock(ITransaction transaction, String id, String owner);

    /**
     * Unlocks all resources at "path" (and all sub-folders if existing) that
     * have the same owner.
     * 
     * @param transaction the transaction
     * @param path what resource to unlock
     * @param owner who wants to unlock
     */
    void unlockTemporaryLockedObjects(ITransaction transaction, String path,
            String owner);

    /**
     * Deletes LockedObjects, where timeout has reached.
     * 
     * @param transaction the transaction
     * @param temporary check timeout on temporary or real locks
     */
    void checkTimeouts(ITransaction transaction, boolean temporary);

    /**
     * Tries to lock the resource at "path" exclusively.
     * 
     * @param transaction the transaction
     * @param path what resource to lock
     * @param owner the owner of the lock
     * @param depth depth
     * @param timeout lock duration in seconds.
     * @return true if the resource at path was successfully locked, false if an
     *  existing lock prevented this
     * @throws LockFailedException if the lock failed
     */
    boolean exclusiveLock(
        ITransaction transaction,
        String path,
        String owner,
        int depth,
        int timeout
    ) throws LockFailedException;

    /**
     * Tries to lock the resource at "path" shared.
     * 
     * @param transaction the transaction
     * @param path what resource to lock
     * @param owner the owner of the lock
     * @param depth depth
     * @param timeout lock Duration in seconds.
     * @return true if the resource at path was successfully locked, false if an
     *  existing lock prevented this
     * @throws LockFailedException if the lock failed
     */
    boolean sharedLock(ITransaction transaction, String path, String owner,
            int depth, int timeout) throws LockFailedException;

    /**
     * Gets the LockedObject corresponding to specified id.
     * 
     * @param transaction the transaction
     * @param id lock token to requested resource
     * @return LockedObject or null if no LockedObject on specified path exists
     */
    LockedObject getLockedObjectByID(ITransaction transaction, String id);

    /**
     * Gets the locked object on specified path.
     * 
     * @param transaction the transaction
     * @param path the path to requested resource
     * @return LockedObject or null if no LockedObject on specified path exists
     */
    LockedObject getLockedObjectByPath(ITransaction transaction, String path);

    /**
     * Gets the LockedObject corresponding to specified id (lock token).
     * 
     * @param transaction the transaction
     * @param id the lock token to requested resource
     * @return LockedObject or null if no LockedObject on specified path exists
     */
    LockedObject getTempLockedObjectByID(ITransaction transaction, String id);

    /**
     * Gets the LockedObject on specified path.
     * 
     * @param transaction the transaction
     * @param path path to requested resource
     * @return LockedObject or null if no LockedObject on specified path exists
     */
    LockedObject getTempLockedObjectByPath(ITransaction transaction, String path);

}
