/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.webdav.methods;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nl.info.webdav.ITransaction;
import nl.info.webdav.IWebdavStore;
import nl.info.webdav.StoredObject;
import nl.info.webdav.WebdavStatus;
import nl.info.webdav.locking.IResourceLocks;
import nl.info.webdav.locking.LockedObject;
import nl.info.webdav.locking.ResourceLocks;
import nl.info.webdav.testutil.DelegatingServletInputStream;
import nl.info.webdav.testutil.MockTest;

public class DoUnlockTest extends MockTest {

    static IWebdavStore mockStore;
    static HttpServletRequest mockReq;
    static HttpServletResponse mockRes;
    static ITransaction mockTransaction;
    static IResourceLocks mockResourceLocks;

    static boolean exclusive = true;

    @BeforeEach
    public void setUp() {
        // fresh Mockery and mocks per test, so 'allowing' expectations do not leak between tests
        setUpBeforeClass();
        mockStore = _mockery.mock(IWebdavStore.class);
        mockReq = _mockery.mock(HttpServletRequest.class);
        mockRes = _mockery.mock(HttpServletResponse.class);
        mockTransaction = _mockery.mock(ITransaction.class);
        mockResourceLocks = _mockery.mock(IResourceLocks.class);
    }

    @Test
    public void testDoUnlockIfReadOnly() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
            }
        });

        DoUnlock doUnlock = new DoUnlock(mockStore, new ResourceLocks(),
                readOnly);

        doUnlock.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoUnlockaLockedResourceWithRightLockToken()
                                                                throws Exception {

        final String lockPath = "/lockedResource";
        final String lockOwner = "theOwner";

        ResourceLocks resLocks = new ResourceLocks();
        resLocks.lock(mockTransaction, lockPath, lockOwner, exclusive, 0,
                TEMP_TIMEOUT, !TEMPORARY);

        LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction,
                lockPath);
        final String loID = lo.getID();
        final String lockToken = "<opaquelocktoken:".concat(loID).concat(">");

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(lockPath));

                oneOf(mockReq).getHeader("Lock-Token");
                will(returnValue(lockToken));

                StoredObject lockedSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, lockPath);
                will(returnValue(lockedSo));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);
            }
        });

        DoUnlock doUnlock = new DoUnlock(mockStore, resLocks, !readOnly);

        doUnlock.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoUnlockaLockedResourceWithWrongLockToken()
                                                                throws Exception {

        final String lockPath = "/lockedResource";
        final String lockOwner = "theOwner";

        ResourceLocks resLocks = new ResourceLocks();
        resLocks.lock(mockTransaction, lockPath, lockOwner, exclusive, 0,
                TEMP_TIMEOUT, !TEMPORARY);

        LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction,
                lockPath);
        final String loID = lo.getID();
        final String lockToken = "<opaquelocktoken:".concat(loID).concat(
                "WRONG>");

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(lockPath));

                oneOf(mockReq).getHeader("Lock-Token");
                will(returnValue(lockToken));

                oneOf(mockRes).sendError(WebdavStatus.SC_BAD_REQUEST);
            }
        });

        DoUnlock doUnlock = new DoUnlock(mockStore, resLocks, !readOnly);
        doUnlock.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoUnlockaNotLockedResource() throws Exception {

        ResourceLocks resLocks = new ResourceLocks();
        final String lockPath = "/notLockedResource";
        final String lockToken = "<opaquelocktoken:xxxx-xxxx-xxxxWRONG>";

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(lockPath));

                oneOf(mockReq).getHeader("Lock-Token");
                will(returnValue(lockToken));

                oneOf(mockRes).sendError(WebdavStatus.SC_BAD_REQUEST);
            }
        });

        DoUnlock doUnlock = new DoUnlock(mockStore, resLocks, !readOnly);

        doUnlock.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoUnlockaLockNullResource() throws Exception {
        final String parentPath = "/parentCollection";
        final String nullLoPath = parentPath.concat("/aNullResource");

        try (final PrintWriter pw = new PrintWriter("/tmp/XMLTestFile");
             final ByteArrayInputStream baisExclusive = new ByteArrayInputStream(
                     exclusiveLockRequestByteArray);
             final DelegatingServletInputStream dsisExclusive = new DelegatingServletInputStream(
                     baisExclusive)
        ) {
            _mockery.checking(new Expectations() {
                {
                    oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                    will(returnValue(null));

                    oneOf(mockReq).getPathInfo();
                    will(returnValue(nullLoPath));

                    LockedObject lockNullResourceLo = null;

                    oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction,
                            nullLoPath);
                    will(returnValue(lockNullResourceLo));

                    LockedObject parentLo = null;

                    oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction,
                            parentPath);
                    will(returnValue(parentLo));

                    oneOf(mockReq).getHeader("User-Agent");
                    will(returnValue("Goliath"));

                    oneOf(mockResourceLocks).lock(with(any(ITransaction.class)),
                            with(any(String.class)), with(any(String.class)),
                            with(any(boolean.class)), with(any(int.class)),
                            with(any(int.class)), with(any(boolean.class)));
                    will(returnValue(true));

                    oneOf(mockReq).getHeader("If");
                    will(returnValue(null));

                    StoredObject lockNullResourceSo = null;

                    oneOf(mockStore).getStoredObject(mockTransaction, nullLoPath);
                    will(returnValue(lockNullResourceSo));

                    StoredObject parentSo = null;

                    oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
                    will(returnValue(parentSo));

                    oneOf(mockStore).createFolder(mockTransaction, parentPath);

                    oneOf(mockStore).getStoredObject(mockTransaction, nullLoPath);
                    will(returnValue(lockNullResourceSo));

                    oneOf(mockStore).createResource(mockTransaction, nullLoPath);

                    oneOf(mockRes).setStatus(WebdavStatus.SC_CREATED);

                    lockNullResourceSo = initLockNullStoredObject();

                    oneOf(mockStore).getStoredObject(mockTransaction, nullLoPath);
                    will(returnValue(lockNullResourceSo));

                    oneOf(mockReq).getInputStream();
                    will(returnValue(dsisExclusive));

                    oneOf(mockReq).getHeader("Depth");
                    will(returnValue(("0")));

                    oneOf(mockReq).getHeader("Timeout");
                    will(returnValue("Infinite"));

                    ResourceLocks resLocks = ResourceLocks.class.newInstance();

                    oneOf(mockResourceLocks).exclusiveLock(mockTransaction,
                            nullLoPath, "I'am the Lock Owner", 0, 604800);
                    will(returnValue(true));

                    lockNullResourceLo = initLockNullLockedObject(resLocks,
                            nullLoPath);

                    oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction,
                            nullLoPath);
                    will(returnValue(lockNullResourceLo));

                    oneOf(mockRes).setStatus(WebdavStatus.SC_OK);

                    oneOf(mockRes).setContentType("text/xml; charset=UTF-8");

                    oneOf(mockRes).getWriter();
                    will(returnValue(pw));

                    String loId = null;
                    if (lockNullResourceLo != null) {
                        loId = lockNullResourceLo.getID();
                    }
                    final String lockToken = "<opaquelocktoken:" + loId + ">";

                    oneOf(mockRes).addHeader("Lock-Token", lockToken);

                    oneOf(mockResourceLocks).unlockTemporaryLockedObjects(
                            with(any(ITransaction.class)), with(any(String.class)),
                            with(any(String.class)));

                    // -----LOCK on a non-existing resource successful------
                    // ----------------now try to unlock it-----------------

                    oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                    will(returnValue(null));

                    oneOf(mockReq).getPathInfo();
                    will(returnValue(nullLoPath));

                    oneOf(mockResourceLocks).lock(with(any(ITransaction.class)),
                            with(any(String.class)), with(any(String.class)),
                            with(any(boolean.class)), with(any(int.class)),
                            with(any(int.class)), with(any(boolean.class)));
                    will(returnValue(true));

                    oneOf(mockReq).getHeader("Lock-Token");
                    will(returnValue(lockToken));

                    oneOf(mockResourceLocks).getLockedObjectByID(mockTransaction,
                            loId);
                    will(returnValue(lockNullResourceLo));

                    String[] owners = lockNullResourceLo.getOwner();
                    String owner = null;
                    if (owners != null)
                        owner = owners[0];

                    oneOf(mockResourceLocks).unlock(mockTransaction, loId, owner);
                    will(returnValue(true));

                    oneOf(mockStore).getStoredObject(mockTransaction, nullLoPath);
                    will(returnValue(lockNullResourceSo));

                    oneOf(mockStore).removeObject(mockTransaction, nullLoPath);

                    oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                    oneOf(mockResourceLocks).unlockTemporaryLockedObjects(
                            with(any(ITransaction.class)), with(any(String.class)),
                            with(any(String.class)));

                }
            });

            DoLock doLock = new DoLock(mockStore, mockResourceLocks, !readOnly);
            doLock.execute(mockTransaction, mockReq, mockRes);

            DoUnlock doUnlock = new DoUnlock(mockStore, mockResourceLocks,
                    !readOnly);
            doUnlock.execute(mockTransaction, mockReq, mockRes);

            _mockery.assertIsSatisfied();
        }
    }

    @Test
    public void testDoUnlockWithoutLockTokenHeaderSendsBadRequest() throws Exception {
        final String lockPath = "/lockedResource";
        ResourceLocks resLocks = new ResourceLocks();
        resLocks.lock(mockTransaction, lockPath, "theOwner", exclusive, 0, 600, !TEMPORARY);

        expectRequest(lockPath, null);
        _mockery.checking(new Expectations() {
            {
                oneOf(mockRes).sendError(WebdavStatus.SC_BAD_REQUEST);
            }
        });

        new DoUnlock(mockStore, resLocks, !readOnly).execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
        assertNotNull(resLocks.getLockedObjectByPath(mockTransaction, lockPath));
    }

    @Test
    public void testDoUnlockWithUnknownLockTokenSendsBadRequestAndKeepsLock() throws Exception {
        final String lockPath = "/lockedResource";
        ResourceLocks resLocks = new ResourceLocks();
        resLocks.lock(mockTransaction, lockPath, "theOwner", exclusive, 0, 600, !TEMPORARY);

        expectRequest(lockPath, "<opaquelocktoken:unknown-token>");
        _mockery.checking(new Expectations() {
            {
                oneOf(mockRes).sendError(WebdavStatus.SC_BAD_REQUEST);
            }
        });

        new DoUnlock(mockStore, resLocks, !readOnly).execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
        assertNotNull(resLocks.getLockedObjectByPath(mockTransaction, lockPath));
    }

    @Test
    public void testDoUnlockIgnoresRequestOwnerAndRemovesLockOfOtherOwner() throws Exception {
        // characterization: DoUnlock never compares a requesting owner with the lock owner; it unlocks with the
        // lock's own owner, so a valid token always removes the lock (no 423 Locked for an owner mismatch)
        final String lockPath = "/lockedResource";
        ResourceLocks resLocks = new ResourceLocks();
        resLocks.lock(mockTransaction, lockPath, "someoneElse", exclusive, 0, 600, !TEMPORARY);
        final String loId = resLocks.getLockedObjectByPath(mockTransaction, lockPath).getID();

        expectRequest(lockPath, "<opaquelocktoken:" + loId + ">");
        _mockery.checking(new Expectations() {
            {
                oneOf(mockStore).getStoredObject(mockTransaction, lockPath);
                will(returnValue(initFileStoredObject(resourceContent)));
                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);
            }
        });

        new DoUnlock(mockStore, resLocks, !readOnly).execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
        assertNull(resLocks.getLockedObjectByPath(mockTransaction, lockPath));
        assertNull(resLocks.getLockedObjectByID(mockTransaction, loId));
    }

    @Test
    public void testDoUnlockSharedLockRemovesAllOwners() throws Exception {
        final String lockPath = "/sharedLockedResource";
        ResourceLocks resLocks = new ResourceLocks();
        resLocks.sharedLock(mockTransaction, lockPath, "ownerA", 0, 600);
        resLocks.sharedLock(mockTransaction, lockPath, "ownerB", 0, 600);
        final LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction, lockPath);
        assertArrayEquals(new String[]{"ownerA", "ownerB"}, lo.getOwner());

        expectRequest(lockPath, "<opaquelocktoken:" + lo.getID() + ">");
        _mockery.checking(new Expectations() {
            {
                oneOf(mockStore).getStoredObject(mockTransaction, lockPath);
                will(returnValue(initFileStoredObject(resourceContent)));
                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);
            }
        });

        new DoUnlock(mockStore, resLocks, !readOnly).execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
        assertNull(lo.getOwner());
        assertNull(resLocks.getLockedObjectByPath(mockTransaction, lockPath));
    }

    @Test
    public void testDoUnlockOfNullResourceRemovesItFromStore() throws Exception {
        final String lockPath = "/parentCollection/aNullResource";
        ResourceLocks resLocks = new ResourceLocks();
        resLocks.exclusiveLock(mockTransaction, lockPath, "theOwner", 0, 600);
        final String loId = resLocks.getLockedObjectByPath(mockTransaction, lockPath).getID();

        expectRequest(lockPath, "<opaquelocktoken:" + loId + ">");
        _mockery.checking(new Expectations() {
            {
                oneOf(mockStore).getStoredObject(mockTransaction, lockPath);
                will(returnValue(initLockNullStoredObject()));
                oneOf(mockStore).removeObject(mockTransaction, lockPath);
                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);
            }
        });

        new DoUnlock(mockStore, resLocks, !readOnly).execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
        assertNull(resLocks.getLockedObjectByPath(mockTransaction, lockPath));
    }

    @Test
    public void testDoUnlockWhenUnlockFailsSendsMethodFailure() throws Exception {
        final String lockPath = "/lockedResource";
        ResourceLocks resLocks = new ResourceLocks();
        resLocks.lock(mockTransaction, lockPath, "theOwner", exclusive, 0, 600, !TEMPORARY);
        final LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction, lockPath);

        expectRequest(lockPath, "<opaquelocktoken:" + lo.getID() + ">");
        _mockery.checking(new Expectations() {
            {
                oneOf(mockResourceLocks).lock(with(any(ITransaction.class)), with(lockPath),
                        with(any(String.class)), with(false), with(0), with(any(int.class)), with(true));
                will(returnValue(true));
                oneOf(mockResourceLocks).getLockedObjectByID(mockTransaction, lo.getID());
                will(returnValue(lo));
                oneOf(mockResourceLocks).unlock(mockTransaction, lo.getID(), "theOwner");
                will(returnValue(false));
                oneOf(mockResourceLocks).unlockTemporaryLockedObjects(with(any(ITransaction.class)),
                        with(lockPath), with(any(String.class)));

                oneOf(mockRes).sendError(WebdavStatus.SC_METHOD_FAILURE);
            }
        });

        new DoUnlock(mockStore, mockResourceLocks, !readOnly).execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoUnlockWhenTemporaryLockCannotBeAcquiredDoesNothing() throws Exception {
        final String lockPath = "/lockedResource";

        expectRequest(lockPath, "<opaquelocktoken:some-token>");
        _mockery.checking(new Expectations() {
            {
                oneOf(mockResourceLocks).lock(with(any(ITransaction.class)), with(lockPath),
                        with(any(String.class)), with(false), with(0), with(any(int.class)), with(true));
                will(returnValue(false));
                oneOf(mockResourceLocks).unlockTemporaryLockedObjects(with(any(ITransaction.class)),
                        with(lockPath), with(any(String.class)));
                // no store or response interaction expected
            }
        });

        new DoUnlock(mockStore, mockResourceLocks, !readOnly).execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoUnlockWhenStoredObjectIsMissingThrowsNullPointerException() throws Exception {
        // characterization: DoUnlock dereferences the stored object without a null check
        final String lockPath = "/lockedButDeletedResource";
        ResourceLocks resLocks = new ResourceLocks();
        resLocks.lock(mockTransaction, lockPath, "theOwner", exclusive, 0, 600, !TEMPORARY);
        final String loId = resLocks.getLockedObjectByPath(mockTransaction, lockPath).getID();

        expectRequest(lockPath, "<opaquelocktoken:" + loId + ">");
        _mockery.checking(new Expectations() {
            {
                oneOf(mockStore).getStoredObject(mockTransaction, lockPath);
                will(returnValue(null));
            }
        });

        DoUnlock doUnlock = new DoUnlock(mockStore, resLocks, !readOnly);
        assertThrows(NullPointerException.class, () -> doUnlock.execute(mockTransaction, mockReq, mockRes));

        _mockery.assertIsSatisfied();
        // the lock itself was already removed before the failure
        assertNull(resLocks.getLockedObjectByPath(mockTransaction, lockPath));
    }

    private void expectRequest(final String path, final String lockTokenHeader) {
        _mockery.checking(new Expectations() {
            {
                allowing(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));
                allowing(mockReq).getPathInfo();
                will(returnValue(path));
                allowing(mockReq).getHeader("Lock-Token");
                will(returnValue(lockTokenHeader));
            }
        });
    }
}
