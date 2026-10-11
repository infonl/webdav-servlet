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
        assertArrayEquals(new String[]{"alice"}, lo.getOwner());
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
        assertArrayEquals(new String[]{"bob"}, lo.getOwner());
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

    // --- checkLocks / checkChildren ---

    /** Value of AbstractMethod.INFINITY, the depth used for "Depth: infinity". */
    private static final int INFINITY = 3;

    @Test
    public void testCheckLocksDeepExclusiveBlockedByExclusiveChild() {
        resourceLocks.lock(null, "/a/b", "alice", true, 0, 100, false);
        LockedObject parent = resourceLocks.getLockedObjectByPath(null, "/a");
        assertFalse(parent.checkLocks(true, INFINITY));
    }

    @Test
    public void testCheckLocksDeepSharedBlockedByExclusiveChild() {
        resourceLocks.lock(null, "/a/b", "alice", true, 0, 100, false);
        LockedObject parent = resourceLocks.getLockedObjectByPath(null, "/a");
        assertFalse(parent.checkLocks(false, INFINITY));
    }

    @Test
    public void testCheckLocksDeepSharedAllowedWithSharedChild() {
        resourceLocks.lock(null, "/a/b", "alice", false, 0, 100, false);
        LockedObject parent = resourceLocks.getLockedObjectByPath(null, "/a");
        assertTrue(parent.checkLocks(false, INFINITY));
    }

    @Test
    public void testCheckLocksDeepExclusiveBlockedBySharedChild() {
        resourceLocks.lock(null, "/a/b", "alice", false, 0, 100, false);
        LockedObject parent = resourceLocks.getLockedObjectByPath(null, "/a");
        assertFalse(parent.checkLocks(true, INFINITY));
    }

    @Test
    public void testCheckLocksDeepExclusiveBlockedByExclusiveGrandchild() {
        resourceLocks.lock(null, "/a/b/c", "alice", true, 0, 100, false);
        LockedObject parent = resourceLocks.getLockedObjectByPath(null, "/a");
        assertNull(resourceLocks.getLockedObjectByPath(null, "/a/b").getOwner());
        assertFalse(parent.checkLocks(true, INFINITY));
    }

    @Test
    public void testCheckLocksDepthOneIgnoresGrandchild() {
        resourceLocks.lock(null, "/a/b/c", "alice", true, 0, 100, false);
        LockedObject parent = resourceLocks.getLockedObjectByPath(null, "/a");
        assertTrue(parent.checkLocks(true, 1));
    }

    @Test
    public void testCheckLocksDepthZeroIgnoresExclusiveChild() {
        resourceLocks.lock(null, "/a/b", "alice", true, 0, 100, false);
        LockedObject parent = resourceLocks.getLockedObjectByPath(null, "/a");
        assertTrue(parent.checkLocks(true, 0));
    }

    @Test
    public void testCheckLocksOnChildBlockedByExclusiveParent() {
        resourceLocks.lock(null, "/a", "alice", true, INFINITY, 100, false);
        LockedObject child = new LockedObject(resourceLocks, "/a/b", false);
        LockedObject parent = resourceLocks.getLockedObjectByPath(null, "/a");
        parent.addChild(child);
        child._parent = parent;
        assertFalse(child.checkLocks(false, 0));
    }

    // --- removeLockedObject / removeTempLockedObject ---

    @Test
    public void testRemoveOnlyChildSetsParentChildrenToNull() {
        resourceLocks.lock(null, "/a/b", "alice", true, 0, 100, false);
        LockedObject parent = resourceLocks.getLockedObjectByPath(null, "/a");
        LockedObject child = resourceLocks.getLockedObjectByPath(null, "/a/b");

        child.removeLockedObject();

        assertNull(parent._children);
        assertNull(resourceLocks.getLockedObjectByPath(null, "/a/b"));
        assertSame(parent, resourceLocks.getLockedObjectByPath(null, "/a"));
    }

    @Test
    public void testRemoveMiddleChildKeepsOtherChildrenInOrder() {
        resourceLocks.lock(null, "/p/x", "o1", false, 0, 100, false);
        resourceLocks.lock(null, "/p/y", "o2", false, 0, 100, false);
        resourceLocks.lock(null, "/p/z", "o3", false, 0, 100, false);
        LockedObject parent = resourceLocks.getLockedObjectByPath(null, "/p");
        LockedObject x = resourceLocks.getLockedObjectByPath(null, "/p/x");
        LockedObject y = resourceLocks.getLockedObjectByPath(null, "/p/y");
        LockedObject z = resourceLocks.getLockedObjectByPath(null, "/p/z");

        y.removeLockedObject();

        assertArrayEquals(new LockedObject[]{x, z}, parent._children);
        assertFalse(resourceLocks._locksByID.containsKey(y.getID()));
    }

    @Test
    public void testRemoveRootLockedObjectIsNoOp() {
        LockedObject root = resourceLocks.getLockedObjectByPath(null, "/");
        root.removeLockedObject();
        assertSame(root, resourceLocks.getLockedObjectByPath(null, "/"));
    }

    @Test
    public void testRemoveOnlyTemporaryChildSetsParentChildrenToNull() {
        resourceLocks.lock(null, "/a/b", "alice", false, 0, 100, true);
        LockedObject parent = resourceLocks.getTempLockedObjectByPath(null, "/a");
        LockedObject child = resourceLocks.getTempLockedObjectByPath(null, "/a/b");

        child.removeTempLockedObject();

        assertNull(parent._children);
        assertNull(resourceLocks.getTempLockedObjectByPath(null, "/a/b"));
        assertNull(resourceLocks.getTempLockedObjectByID(null, child.getID()));
    }

    @Test
    public void testRemoveMiddleTemporaryChildKeepsOtherChildrenInOrder() {
        resourceLocks.lock(null, "/p/x", "o1", false, 0, 100, true);
        resourceLocks.lock(null, "/p/y", "o2", false, 0, 100, true);
        resourceLocks.lock(null, "/p/z", "o3", false, 0, 100, true);
        LockedObject parent = resourceLocks.getTempLockedObjectByPath(null, "/p");
        LockedObject x = resourceLocks.getTempLockedObjectByPath(null, "/p/x");
        LockedObject y = resourceLocks.getTempLockedObjectByPath(null, "/p/y");
        LockedObject z = resourceLocks.getTempLockedObjectByPath(null, "/p/z");

        y.removeTempLockedObject();

        assertArrayEquals(new LockedObject[]{x, z}, parent._children);
        assertNull(resourceLocks.getTempLockedObjectByPath(null, "/p/y"));
    }

    @Test
    public void testRemoveTemporaryObjectWithoutParentIsNoOp() {
        LockedObject orphan = new LockedObject(resourceLocks, "/orphan", true);
        orphan.removeTempLockedObject();
        assertSame(orphan, resourceLocks.getTempLockedObjectByPath(null, "/orphan"));
    }

    // --- removeLockedObjectOwner edge cases ---

    @Test
    public void testRemoveAbsentOwnerLeavesOwnersUnchanged() {
        LockedObject lo = lockedObject("/foo");
        lo.addLockedObjectOwner("alice");
        lo.addLockedObjectOwner("bob");
        lo.removeLockedObjectOwner("mallory");
        assertArrayEquals(new String[]{"alice", "bob"}, lo.getOwner());
    }

    @Test
    public void testRemoveOwnerWhenNoOwnersIsNoOp() {
        LockedObject lo = lockedObject("/foo");
        lo.removeLockedObjectOwner("alice");
        assertNull(lo.getOwner());
    }

    @Test
    public void testRemoveLastOfTwoOwners() {
        LockedObject lo = lockedObject("/foo");
        lo.addLockedObjectOwner("alice");
        lo.addLockedObjectOwner("bob");
        lo.removeLockedObjectOwner("bob");
        assertArrayEquals(new String[]{"alice"}, lo.getOwner());
        lo.removeLockedObjectOwner("alice");
        assertNull(lo.getOwner());
    }
}
