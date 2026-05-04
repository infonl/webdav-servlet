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

import nl.info.webdav.testutil.MockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LockedObjectTest extends MockTest {

    private ResourceLocks resourceLocks;

    @BeforeEach
    public void setUp() {
        resourceLocks = new ResourceLocks();
    }

    private LockedObject lockedObject(String path) {
        return new LockedObject(resourceLocks, path, false);
    }

    @Test
    public void testAddFirstOwnerSucceeds() {
        LockedObject lo = lockedObject("/foo");
        boolean result = lo.addLockedObjectOwner("alice");
        assertTrue(result);
        assertArrayEquals(new String[] { "alice" }, lo.getOwner());
    }

    @Test
    public void testAddSecondDistinctOwnerSucceeds() {
        LockedObject lo = lockedObject("/foo");
        lo.addLockedObjectOwner("alice");
        boolean result = lo.addLockedObjectOwner("bob");
        assertTrue(result);
        assertEquals(2, lo.getOwner().length);
    }

    @Test
    public void testDuplicateOwnerRejected() {
        LockedObject lo = lockedObject("/foo");
        lo.addLockedObjectOwner("alice");
        boolean result = lo.addLockedObjectOwner("alice");
        assertFalse(result);
        assertEquals(1, lo.getOwner().length);
    }

    @Test
    public void testRemoveSingleOwnerClearsArray() {
        LockedObject lo = lockedObject("/foo");
        lo.addLockedObjectOwner("alice");
        lo.removeLockedObjectOwner("alice");
        assertNull(lo.getOwner());
    }

    @Test
    public void testRemoveOneOfTwoOwners() {
        LockedObject lo = lockedObject("/foo");
        lo.addLockedObjectOwner("alice");
        lo.addLockedObjectOwner("bob");
        lo.removeLockedObjectOwner("alice");
        assertArrayEquals(new String[] { "bob" }, lo.getOwner());
    }

    @Test
    public void testAddChildToChildlessObject() {
        LockedObject parent = lockedObject("/parent");
        LockedObject child = lockedObject("/parent/child");
        parent.addChild(child);
        assertNotNull(parent._children);
        assertEquals(1, parent._children.length);
        assertSame(child, parent._children[0]);
    }

    @Test
    public void testCheckLocksReturnsTrueForUnownedChildlessObject() {
        LockedObject lo = lockedObject("/foo");
        lo._parent = resourceLocks._root;
        assertTrue(lo.checkLocks(false, 0));
    }

    @Test
    public void testCheckLocksReturnsFalseWhenExclusiveOwnerPresent() {
        LockedObject lo = lockedObject("/foo");
        lo.addLockedObjectOwner("alice");
        lo._exclusive = true;
        assertFalse(lo.checkLocks(true, 0));
    }

    @Test
    public void testHasExpiredReturnsFalseAfterRefresh() {
        LockedObject lo = lockedObject("/foo");
        lo.refreshTimeout(3600);
        assertFalse(lo.hasExpired());
    }

    @Test
    public void testHasExpiredReturnsTrueForPastExpiry() {
        LockedObject lo = lockedObject("/foo");
        lo._expiresAt = 1L;
        assertTrue(lo.hasExpired());
    }

    @Test
    public void testRemoveLockedObjectClearsFromHashtables() {
        resourceLocks.lock(null, "/foo", "alice", true, 0, 100, false);
        LockedObject lo = resourceLocks.getLockedObjectByPath(null, "/foo");
        String id = lo.getID();
        lo.removeLockedObject();
        assertFalse(resourceLocks._locks.containsKey("/foo"));
        assertFalse(resourceLocks._locksByID.containsKey(id));
    }
}
