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
}
