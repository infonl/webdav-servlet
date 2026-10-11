/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.webdav.methods;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.util.stream.Stream;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import nl.info.webdav.ITransaction;
import nl.info.webdav.IWebdavStore;
import nl.info.webdav.StoredObject;
import nl.info.webdav.WebdavStatus;
import nl.info.webdav.exceptions.AccessDeniedException;
import nl.info.webdav.exceptions.WebdavException;
import nl.info.webdav.locking.IResourceLocks;
import nl.info.webdav.locking.LockedObject;
import nl.info.webdav.locking.ResourceLocks;
import nl.info.webdav.testutil.DelegatingServletInputStream;
import nl.info.webdav.testutil.MockTest;

public class DoMkcolTest extends MockTest {

    static IWebdavStore mockStore;
    static HttpServletRequest mockReq;
    static HttpServletResponse mockRes;
    static ITransaction mockTransaction;
    static IResourceLocks mockResourceLocks;

    static String parentPath = "/parentCollection";
    static String mkcolPath = parentPath.concat("/makeCollection");
    static String owner = "a lock owner";

    @BeforeAll
    public static void setUp() throws Exception {
        mockStore = _mockery.mock(IWebdavStore.class);
        mockReq = _mockery.mock(HttpServletRequest.class);
        mockRes = _mockery.mock(HttpServletResponse.class);
        mockTransaction = _mockery.mock(ITransaction.class);
        mockResourceLocks = _mockery.mock(IResourceLocks.class);
    }

    @Test
    public void testMkcolIfReadOnlyIsTrue() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoMkcol doMkcol = new DoMkcol(mockStore, resLocks, readOnly);
        doMkcol.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testMkcolSuccess() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(mkcolPath));

                StoredObject parentSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(parentSo));

                StoredObject mkcolSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, mkcolPath);
                will(returnValue(mkcolSo));

                oneOf(mockStore).createFolder(mockTransaction, mkcolPath);

                oneOf(mockRes).setStatus(WebdavStatus.SC_CREATED);

            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoMkcol doMkcol = new DoMkcol(mockStore, resLocks, !readOnly);
        doMkcol.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testMkcolIfParentPathIsNoFolder() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(mkcolPath));

                StoredObject parentSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(parentSo));

                String methodsAllowed = "OPTIONS, GET, HEAD, POST, DELETE, TRACE, " + "PROPPATCH, COPY, MOVE, LOCK, UNLOCK, PROPFIND";

                oneOf(mockRes).addHeader("Allow", methodsAllowed);

                oneOf(mockRes).sendError(WebdavStatus.SC_METHOD_NOT_ALLOWED);
            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoMkcol doMkcol = new DoMkcol(mockStore, resLocks, !readOnly);
        doMkcol.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testMkcolIfParentPathIsAFolderButObjectAlreadyExists()
                                                                       throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(mkcolPath));

                StoredObject parentSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(parentSo));

                StoredObject mkcolSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, mkcolPath);
                will(returnValue(mkcolSo));

                oneOf(mockRes)
                        .addHeader(
                                "Allow",
                                "OPTIONS, GET, HEAD, POST, DELETE, TRACE, PROPPATCH, COPY, MOVE, LOCK, UNLOCK, PROPFIND, PUT");

                oneOf(mockRes).sendError(WebdavStatus.SC_METHOD_NOT_ALLOWED);

            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoMkcol doMkcol = new DoMkcol(mockStore, resLocks, !readOnly);
        doMkcol.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testMkcolIfParentFolderIsLockedWithRightLockToken()
                                                                    throws Exception {

        ResourceLocks resLocks = new ResourceLocks();
        resLocks.lock(mockTransaction, parentPath, owner, true, -1, 200, false);
        LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction,
                parentPath);
        final String rightLockToken = "(<opaquelocktoken:" + lo.getID() + ">)";

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(mkcolPath));

                oneOf(mockReq).getHeader("If");
                will(returnValue(rightLockToken));

                StoredObject parentSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(parentSo));

                StoredObject mkcolSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, mkcolPath);
                will(returnValue(mkcolSo));

                oneOf(mockStore).createFolder(mockTransaction, mkcolPath);

                oneOf(mockRes).setStatus(WebdavStatus.SC_CREATED);

            }
        });

        DoMkcol doMkcol = new DoMkcol(mockStore, resLocks, !readOnly);
        doMkcol.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testMkcolIfParentFolderIsLockedWithWrongLockToken()
                                                                    throws Exception {

        ResourceLocks resLocks = new ResourceLocks();
        resLocks.lock(mockTransaction, parentPath, owner, true, -1, 200, false);
        final String wrongLockToken = "(<opaquelocktoken:" + "aWrongLockToken" + ">)";

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(mkcolPath));

                oneOf(mockReq).getHeader("If");
                will(returnValue(wrongLockToken));

                oneOf(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
            }
        });

        DoMkcol doMkcol = new DoMkcol(mockStore, resLocks, !readOnly);
        doMkcol.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testMkcolOnALockNullResource() throws Exception {

        final PrintWriter pw = new PrintWriter("/tmp/XMLTestFile");

        final ByteArrayInputStream baisExclusive = new ByteArrayInputStream(
                exclusiveLockRequestByteArray);
        final DelegatingServletInputStream dsisExclusive = new DelegatingServletInputStream(
                baisExclusive);

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(mkcolPath));

                LockedObject lockNullResourceLo = null;

                oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction,
                        mkcolPath);
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

                oneOf(mockStore).getStoredObject(mockTransaction, mkcolPath);
                will(returnValue(lockNullResourceSo));

                StoredObject parentSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(parentSo));

                oneOf(mockStore).createFolder(mockTransaction, parentPath);

                parentSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, mkcolPath);
                will(returnValue(lockNullResourceSo));

                oneOf(mockStore).createResource(mockTransaction, mkcolPath);

                lockNullResourceSo = initLockNullStoredObject();

                oneOf(mockRes).setStatus(WebdavStatus.SC_CREATED);

                oneOf(mockStore).getStoredObject(mockTransaction, mkcolPath);
                will(returnValue(lockNullResourceSo));

                oneOf(mockReq).getInputStream();
                will(returnValue(dsisExclusive));

                oneOf(mockReq).getHeader("Depth");
                will(returnValue(("0")));

                oneOf(mockReq).getHeader("Timeout");
                will(returnValue("Infinite"));

                ResourceLocks resLocks = ResourceLocks.class.newInstance();

                oneOf(mockResourceLocks).exclusiveLock(mockTransaction,
                        mkcolPath, "I'am the Lock Owner", 0, 604800);
                will(returnValue(true));

                lockNullResourceLo = initLockNullLockedObject(resLocks,
                        mkcolPath);

                oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction,
                        mkcolPath);
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
                // --------now MKCOL on the lock-null resource----------

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(mkcolPath));

                oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction,
                        parentPath);
                will(returnValue(parentLo));

                oneOf(mockResourceLocks).lock(with(any(ITransaction.class)),
                        with(any(String.class)), with(any(String.class)),
                        with(any(boolean.class)), with(any(int.class)),
                        with(any(int.class)), with(any(boolean.class)));
                will(returnValue(true));

                oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(parentSo));

                oneOf(mockStore).getStoredObject(mockTransaction, mkcolPath);
                will(returnValue(lockNullResourceSo));

                oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction,
                        mkcolPath);
                will(returnValue(lockNullResourceLo));

                final String ifHeaderLockToken = "(<locktoken:" + loId + ">)";

                oneOf(mockReq).getHeader("If");
                will(returnValue(ifHeaderLockToken));

                String[] owners = lockNullResourceLo.getOwner();
                String owner = null;
                if (owners != null)
                    owner = owners[0];

                oneOf(mockResourceLocks).unlock(mockTransaction, loId, owner);
                will(returnValue(true));

                oneOf(mockRes).setStatus(WebdavStatus.SC_CREATED);

                oneOf(mockResourceLocks).unlockTemporaryLockedObjects(
                        with(any(ITransaction.class)), with(any(String.class)),
                        with(any(String.class)));

            }
        });

        DoLock doLock = new DoLock(mockStore, mockResourceLocks, !readOnly);
        doLock.execute(mockTransaction, mockReq, mockRes);

        DoMkcol doMkcol = new DoMkcol(mockStore, mockResourceLocks, !readOnly);
        doMkcol.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testMkcolIfParentDoesNotExistResultsIn409() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(mkcolPath));

                oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(null));

                oneOf(mockRes).sendError(WebdavStatus.SC_CONFLICT);

                never(mockStore).createFolder(mockTransaction, mkcolPath);
            }
        });

        DoMkcol doMkcol = new DoMkcol(mockStore, new ResourceLocks(), !readOnly);
        doMkcol.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testMkcolIfResourceAlreadyExistsResultsIn405WithAllowHeader() throws Exception {

        final StoredObject existingSo = initFileStoredObject(resourceContent);

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(mkcolPath));

                StoredObject parentSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(parentSo));

                oneOf(mockStore).getStoredObject(mockTransaction, mkcolPath);
                will(returnValue(existingSo));

                oneOf(mockRes).addHeader("Allow", DeterminableMethod.determineMethodsAllowed(existingSo));

                oneOf(mockRes).sendError(WebdavStatus.SC_METHOD_NOT_ALLOWED);
            }
        });

        DoMkcol doMkcol = new DoMkcol(mockStore, new ResourceLocks(), !readOnly);
        doMkcol.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testMkcolReplacesOwnedLockNullResourceWithFolder() throws Exception {

        final ResourceLocks resLocks = new ResourceLocks();
        resLocks.lock(mockTransaction, mkcolPath, owner, true, 0, 200, false);
        final LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction, mkcolPath);
        final String lockToken = "(<opaquelocktoken:" + lo.getID() + ">)";
        final StoredObject lockNullSo = initLockNullStoredObject();

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(mkcolPath));

                StoredObject parentSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(parentSo));

                oneOf(mockStore).getStoredObject(mockTransaction, mkcolPath);
                will(returnValue(lockNullSo));

                oneOf(mockReq).getHeader("If");
                will(returnValue(lockToken));

                oneOf(mockRes).setStatus(WebdavStatus.SC_CREATED);

                never(mockStore).createFolder(mockTransaction, mkcolPath);
            }
        });

        DoMkcol doMkcol = new DoMkcol(mockStore, resLocks, !readOnly);
        doMkcol.execute(mockTransaction, mockReq, mockRes);

        assertTrue(lockNullSo.isFolder());
        assertFalse(lockNullSo.isNullResource());
        assertNull(resLocks.getLockedObjectByPath(mockTransaction, mkcolPath));

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testMkcolOnLockNullResourceWithWrongLockTokenResultsIn423() throws Exception {

        final ResourceLocks resLocks = new ResourceLocks();
        resLocks.lock(mockTransaction, mkcolPath, owner, true, 0, 200, false);
        final StoredObject lockNullSo = initLockNullStoredObject();

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(mkcolPath));

                StoredObject parentSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(parentSo));

                oneOf(mockStore).getStoredObject(mockTransaction, mkcolPath);
                will(returnValue(lockNullSo));

                oneOf(mockReq).getHeader("If");
                will(returnValue("(<opaquelocktoken:aWrongLockToken>)"));

                oneOf(mockRes).sendError(WebdavStatus.SC_LOCKED, WebdavStatus.getStatusText(WebdavStatus.SC_LOCKED));
            }
        });

        DoMkcol doMkcol = new DoMkcol(mockStore, resLocks, !readOnly);
        doMkcol.execute(mockTransaction, mockReq, mockRes);

        assertTrue(lockNullSo.isNullResource());
        assertNotNull(resLocks.getLockedObjectByPath(mockTransaction, mkcolPath));

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testMkcolOnLockNullResourceWithoutIfHeaderResultsIn400() throws Exception {

        final ResourceLocks resLocks = new ResourceLocks();
        resLocks.lock(mockTransaction, mkcolPath, owner, true, 0, 200, false);

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(mkcolPath));

                StoredObject parentSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(parentSo));

                StoredObject lockNullSo = initLockNullStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, mkcolPath);
                will(returnValue(lockNullSo));

                oneOf(mockReq).getHeader("If");
                will(returnValue(null));

                oneOf(mockRes).sendError(WebdavStatus.SC_BAD_REQUEST);
            }
        });

        DoMkcol doMkcol = new DoMkcol(mockStore, resLocks, !readOnly);
        doMkcol.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testMkcolOnLockNullResourceWithoutLockResultsIn500() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(mkcolPath));

                StoredObject parentSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(parentSo));

                StoredObject lockNullSo = initLockNullStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, mkcolPath);
                will(returnValue(lockNullSo));

                oneOf(mockRes).sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
            }
        });

        DoMkcol doMkcol = new DoMkcol(mockStore, new ResourceLocks(), !readOnly);
        doMkcol.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    static Stream<Arguments> storeExceptions() {
        return Stream.of(
                Arguments.of(new AccessDeniedException(null), WebdavStatus.SC_FORBIDDEN),
                Arguments.of(new WebdavException("store failure"), WebdavStatus.SC_INTERNAL_SERVER_ERROR));
    }

    @ParameterizedTest
    @MethodSource("storeExceptions")
    public void testMkcolStoreExceptionIsMappedToStatus(WebdavException exception, int expectedStatus)
                                                                                                       throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(mkcolPath));

                StoredObject parentSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(parentSo));

                oneOf(mockStore).getStoredObject(mockTransaction, mkcolPath);
                will(returnValue(null));

                oneOf(mockStore).createFolder(mockTransaction, mkcolPath);
                will(throwException(exception));

                oneOf(mockRes).sendError(expectedStatus);
            }
        });

        DoMkcol doMkcol = new DoMkcol(mockStore, new ResourceLocks(), !readOnly);
        doMkcol.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testMkcolTemporaryLockFailureResultsIn500() throws Exception {

        final ResourceLocks resLocks = new ResourceLocks();
        resLocks.lock(mockTransaction, mkcolPath, "someone else", true, 0, 200, true);

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(mkcolPath));

                oneOf(mockRes).sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
            }
        });

        DoMkcol doMkcol = new DoMkcol(mockStore, resLocks, !readOnly);
        doMkcol.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }
}
