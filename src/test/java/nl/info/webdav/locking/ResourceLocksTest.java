// SPDX-FileCopyrightText: 2026 INFO.nl
// SPDX-License-Identifier: EUPL-1.2+
package nl.info.webdav.locking;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nl.info.webdav.testutil.MockTest;

public class ResourceLocksTest extends MockTest {

    private ResourceLocks resourceLocks;

    @BeforeEach
    public void setUp() {
        resourceLocks = new ResourceLocks();
    }

    @Test
    public void testExclusiveLockOnFreshPathSucceeds() {
        boolean result = resourceLocks.lock(null, "/foo", "alice", true, 0, 100, false);
        assertTrue(result);
        LockedObject lo = resourceLocks.getLockedObjectByPath(null, "/foo");
        assertNotNull(lo);
        assertArrayEquals(new String[]{"alice"}, lo.getOwner());
    }

    @Test
    public void testSecondExclusiveLockOnSamePathFails() {
        resourceLocks.lock(null, "/foo", "alice", true, 0, 100, false);
        boolean result = resourceLocks.lock(null, "/foo", "bob", true, 0, 100, false);
        assertFalse(result);
    }

    @Test
    public void testTwoSharedLocksOnSamePathBothSucceed() {
        boolean first = resourceLocks.lock(null, "/foo", "alice", false, 0, 100, false);
        boolean second = resourceLocks.lock(null, "/foo", "bob", false, 0, 100, false);
        assertTrue(first);
        assertTrue(second);
        LockedObject lo = resourceLocks.getLockedObjectByPath(null, "/foo");
        assertNotNull(lo);
        assertEquals(2, lo.getOwner().length);
    }

    @Test
    public void testExclusiveLockRejectedWhenSharedLockPresent() {
        resourceLocks.lock(null, "/foo", "alice", false, 0, 100, false);
        boolean result = resourceLocks.lock(null, "/foo", "bob", true, 0, 100, false);
        assertFalse(result);
    }

    @Test
    public void testUnlockReleasesExclusiveLock() {
        resourceLocks.lock(null, "/foo", "alice", true, 0, 100, false);
        LockedObject lo = resourceLocks.getLockedObjectByPath(null, "/foo");
        String id = lo.getID();
        boolean result = resourceLocks.unlock(null, id, "alice");
        assertTrue(result);
        assertFalse(resourceLocks._locks.containsKey("/foo"));
    }

    @Test
    public void testTemporaryLockLifecycle() {
        boolean result = resourceLocks.lock(null, "/foo", "alice", false, 0, 100, true);
        assertTrue(result);
        LockedObject lo = resourceLocks.getTempLockedObjectByPath(null, "/foo");
        assertNotNull(lo);
        resourceLocks.unlockTemporaryLockedObjects(null, "/foo", "alice");
        LockedObject loAfter = resourceLocks.getTempLockedObjectByPath(null, "/foo");
        assertTrue(loAfter == null || loAfter.getOwner() == null);
    }

    @Test
    public void testCheckTimeoutsRemovesExpiredLock() {
        resourceLocks.lock(null, "/foo", "alice", true, 0, 100, false);
        LockedObject lo = resourceLocks.getLockedObjectByPath(null, "/foo");
        lo._expiresAt = 1L;
        resourceLocks.checkTimeouts(null, false);
        assertNull(resourceLocks.getLockedObjectByPath(null, "/foo"));
    }

    @Test
    public void testGetLockedObjectByIdReturnsCorrectObject() {
        resourceLocks.lock(null, "/foo", "alice", true, 0, 100, false);
        LockedObject lo = resourceLocks.getLockedObjectByPath(null, "/foo");
        String id = lo.getID();
        LockedObject loById = resourceLocks.getLockedObjectByID(null, id);
        assertSame(lo, loById);
    }

    @Test
    public void testGetTempLockedObjectByIdReturnsCorrectObject() {
        resourceLocks.lock(null, "/foo", "alice", false, 0, 100, true);
        LockedObject lo = resourceLocks.getTempLockedObjectByPath(null, "/foo");
        String id = lo.getID();
        LockedObject loById = resourceLocks.getTempLockedObjectByID(null, id);
        assertSame(lo, loById);
    }

    // --- cleanLockedObjects (private) is only reached when _cleanupCounter exceeds the cleanup limit ---
    // NOTE: the ResourceLocks constructor registers _root as a temporary LockedObject and _tempRoot as a
    // real one, so real locks hang below _tempRoot and temporary locks below _root. As a consequence
    // unlockTemporaryLockedObjects() cleans the tree of real locks and unlock() cleans the tree of
    // temporary locks. The tests below characterize that current behavior.

    private static final int ABOVE_CLEANUP_LIMIT = 100001;

    @Test
    public void testRootsAreRegisteredInSwappedHashtables() {
        assertSame(resourceLocks._root, resourceLocks._tempLocks.get("/"));
        assertSame(resourceLocks._tempRoot, resourceLocks._locks.get("/"));
    }

    @Test
    public void testUnlockTemporaryLockedObjectsWithCleanupOnNestedTemporaryLocks() {
        resourceLocks.lock(null, "/a/b/c", "alice", false, 0, 100, true);
        resourceLocks._cleanupCounter = ABOVE_CLEANUP_LIMIT;

        resourceLocks.unlockTemporaryLockedObjects(null, "/a/b/c", "alice");

        assertEquals(0, resourceLocks._cleanupCounter);
        // "/a" never got an expiry time and is removed by the timeout check
        assertNull(resourceLocks.getTempLockedObjectByPath(null, "/a"));
        assertNull(resourceLocks._root._children);
        // "/a/b" and "/a/b/c" are not removed: cleanup walks the tree of real locks (see NOTE above)
        assertNotNull(resourceLocks.getTempLockedObjectByPath(null, "/a/b"));
        LockedObject leaf = resourceLocks.getTempLockedObjectByPath(null, "/a/b/c");
        assertNotNull(leaf);
        assertNull(leaf.getOwner());
    }

    @Test
    public void testUnlockTemporaryLockedObjectsWithCleanupRemovesOwnerlessRealLocksButKeepsOwnedParent() {
        resourceLocks.lock(null, "/a/b", "alice", false, 0, 100, false);
        resourceLocks.lock(null, "/a/b/c", "bob", false, 0, 100, false);
        LockedObject loB = resourceLocks.getLockedObjectByPath(null, "/a/b");
        LockedObject loC = resourceLocks.getLockedObjectByPath(null, "/a/b/c");
        loC.removeLockedObjectOwner("bob");
        resourceLocks._cleanupCounter = ABOVE_CLEANUP_LIMIT;

        resourceLocks.unlockTemporaryLockedObjects(null, "/unrelated", "nobody");

        LockedObject loA = resourceLocks.getLockedObjectByPath(null, "/a");
        assertSame(loA, resourceLocks._tempRoot._children[0]);
        assertSame(loB, loA._children[0]);
        assertArrayEquals(new String[]{"alice"}, loB.getOwner());
        // "/a/b/c" is detached from the tree ...
        assertNull(loB._children);
        // ... but stays in the real-lock hashtables, because it is removed as a temporary lock
        assertSame(loC, resourceLocks.getLockedObjectByPath(null, "/a/b/c"));
    }

    @Test
    public void testUnlockTemporaryLockedObjectsWithCleanupRemovesAllOwnerlessSiblings() {
        String[] siblings = {"/p/x", "/p/y", "/p/z"};
        for (String sibling : siblings) {
            resourceLocks.lock(null, sibling, "owner" + sibling, false, 0, 100, false);
        }
        for (String sibling : siblings) {
            resourceLocks.getLockedObjectByPath(null, sibling).removeLockedObjectOwner("owner" + sibling);
        }
        LockedObject loP = resourceLocks.getLockedObjectByPath(null, "/p");
        assertEquals(3, loP._children.length);
        resourceLocks._cleanupCounter = ABOVE_CLEANUP_LIMIT;

        resourceLocks.unlockTemporaryLockedObjects(null, "/unrelated", "nobody");

        // no sibling is skipped due to array shifting, and the now empty "/p" is removed too
        assertNull(loP._children);
        assertNull(resourceLocks._tempRoot._children);
    }

    @Test
    public void testUnlockWithCleanupRemovesTemporaryLockTreeButLeavesTemporaryHashtableEntries() {
        resourceLocks.lock(null, "/a/b", "alice", false, 0, 100, false);
        resourceLocks.lock(null, "/a/b/c", "bob", false, 0, 100, false);
        resourceLocks.lock(null, "/t/u", "tmp", false, 0, 100, true);
        resourceLocks.unlockTemporaryLockedObjects(null, "/t/u", "tmp");
        String idC = resourceLocks.getLockedObjectByPath(null, "/a/b/c").getID();
        resourceLocks._cleanupCounter = ABOVE_CLEANUP_LIMIT;

        boolean result = resourceLocks.unlock(null, idC, "bob");

        assertTrue(result);
        assertEquals(0, resourceLocks._cleanupCounter);
        // the released real lock is removed, the owned parent stays locked
        assertNull(resourceLocks.getLockedObjectByPath(null, "/a/b/c"));
        LockedObject loB = resourceLocks.getLockedObjectByPath(null, "/a/b");
        assertArrayEquals(new String[]{"alice"}, loB.getOwner());
        assertNull(loB._children);
        // cleanup walked the temporary tree (see NOTE above): it is detached from _root ...
        assertNull(resourceLocks._root._children);
        // ... but the entries remain in the temporary hashtables
        assertNotNull(resourceLocks.getTempLockedObjectByPath(null, "/t"));
        assertNotNull(resourceLocks.getTempLockedObjectByPath(null, "/t/u"));
    }

    // --- unlock edge cases ---

    @Test
    public void testUnlockUnknownIdLeavesExistingLocksUnaffected() {
        resourceLocks.lock(null, "/foo", "alice", true, 0, 100, false);
        LockedObject lo = resourceLocks.getLockedObjectByPath(null, "/foo");

        boolean result = resourceLocks.unlock(null, "unknown-id", "alice");

        // current behavior: an unknown id is reported as a successful unlock
        assertTrue(result);
        assertSame(lo, resourceLocks.getLockedObjectByPath(null, "/foo"));
        assertArrayEquals(new String[]{"alice"}, lo.getOwner());
    }

    @Test
    public void testUnlockByNonOwnerLeavesLockInPlace() {
        resourceLocks.lock(null, "/foo", "alice", true, 0, 100, false);
        LockedObject lo = resourceLocks.getLockedObjectByPath(null, "/foo");

        boolean result = resourceLocks.unlock(null, lo.getID(), "mallory");

        // current behavior: unlock by a non-owner also returns true, but the lock is not released
        assertTrue(result);
        assertSame(lo, resourceLocks.getLockedObjectByPath(null, "/foo"));
        assertSame(lo, resourceLocks.getLockedObjectByID(null, lo.getID()));
        assertArrayEquals(new String[]{"alice"}, lo.getOwner());
    }
}
