/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.webdav.methods;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.InputStream;
import java.io.PrintWriter;
import java.util.stream.Stream;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import nl.info.webdav.ITransaction;
import nl.info.webdav.IWebdavStore;
import nl.info.webdav.StoredObject;
import nl.info.webdav.WebdavStatus;
import nl.info.webdav.exceptions.AccessDeniedException;
import nl.info.webdav.exceptions.PathTraversalException;
import nl.info.webdav.exceptions.WebdavException;
import nl.info.webdav.locking.IResourceLocks;
import nl.info.webdav.locking.LockedObject;
import nl.info.webdav.locking.ResourceLocks;
import nl.info.webdav.testutil.MockTest;


public class DoPutTest extends MockTest {
    static IWebdavStore mockStore;
    static HttpServletRequest mockReq;
    static HttpServletResponse mockRes;
    static IResourceLocks mockResourceLocks;
    static ITransaction mockTransaction;

    static String parentPath = "/parentCollection";
    static String path = parentPath.concat("/fileToPut");

    static boolean lazyFolderCreationOnPut = true;

    @BeforeAll
    public static void setUp() throws Exception {
        mockStore = _mockery.mock(IWebdavStore.class);
        mockReq = _mockery.mock(HttpServletRequest.class);
        mockRes = _mockery.mock(HttpServletResponse.class);
        mockResourceLocks = _mockery.mock(IResourceLocks.class);
        mockTransaction = _mockery.mock(ITransaction.class);
    }

    @Test
    public void testDoPutIfReadOnlyTrue() throws Exception {
        _mockery.checking(new Expectations() {
            {
                oneOf(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
            }
        });

        DoPut doPut = new DoPut(mockStore, new ResourceLocks(), readOnly,
                lazyFolderCreationOnPut);
        doPut.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoPutIfReadOnlyFalse() throws Exception {
        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getHeader("User-Agent");
                will(returnValue("Goliath agent"));

                StoredObject parentSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(parentSo));

                StoredObject fileSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(fileSo));

                oneOf(mockStore).createResource(mockTransaction, path);

                oneOf(mockRes).setStatus(WebdavStatus.SC_CREATED);

                oneOf(mockReq).getInputStream();
                will(returnValue(dsis));

                oneOf(mockStore).setResourceContent(mockTransaction, path, dsis,
                        null, null);
                will(returnValue(8L));

                fileSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(fileSo));

                // User-Agent: Goliath --> don't add ContentLength
                // oneOf(mockRes).setContentLength(8);
            }
        });

        DoPut doPut = new DoPut(mockStore, new ResourceLocks(), !readOnly,
                lazyFolderCreationOnPut);
        doPut.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoPutIfLazyFolderCreationOnPutIsFalse() throws Exception {
        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getHeader("User-Agent");
                will(returnValue("Transmit agent"));

                oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(null));

                oneOf(mockRes).sendError(404, "Not Found");
            }
        });

        DoPut doPut = new DoPut(mockStore, new ResourceLocks(), !readOnly, !lazyFolderCreationOnPut);
        doPut.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoPutIfLazyFolderCreationOnPutIsTrue() throws Exception {
        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getHeader("User-Agent");
                will(returnValue("WebDAVFS/1.5.0 (01500000) ....."));

                StoredObject parentSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(parentSo));

                oneOf(mockStore).createFolder(mockTransaction, parentPath);

                StoredObject fileSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(fileSo));

                oneOf(mockStore).createResource(mockTransaction, path);

                oneOf(mockRes).setStatus(WebdavStatus.SC_CREATED);

                oneOf(mockReq).getInputStream();
                will(returnValue(dsis));

                oneOf(mockStore).setResourceContent(mockTransaction, path, dsis,
                        null, null);
                will(returnValue(8L));

                fileSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(fileSo));

            }
        });

        DoPut doPut = new DoPut(mockStore, new ResourceLocks(), !readOnly,
                lazyFolderCreationOnPut);
        doPut.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoPutIfParentPathIsResource() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getHeader("User-Agent");
                will(returnValue("WebDAVFS/1.5.0 (01500000) ....."));

                StoredObject parentSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(parentSo));

                oneOf(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
            }
        });

        DoPut doPut = new DoPut(mockStore, new ResourceLocks(), !readOnly,
                lazyFolderCreationOnPut);
        doPut.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoPutOnALockNullResource() throws Exception {

        final PrintWriter pw = new PrintWriter("/tmp/XMLTestFile");

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                LockedObject lockNullResourceLo = null;

                oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction,
                        path);
                will(returnValue(lockNullResourceLo));

                LockedObject parentLo = null;

                oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction,
                        parentPath);
                will(returnValue(parentLo));

                oneOf(mockReq).getHeader("User-Agent");
                will(returnValue("Transmit agent"));

                oneOf(mockResourceLocks).lock(with(any(ITransaction.class)),
                        with(any(String.class)), with(any(String.class)),
                        with(any(boolean.class)), with(any(int.class)),
                        with(any(int.class)), with(any(boolean.class)));
                will(returnValue(true));

                oneOf(mockReq).getHeader("If");
                will(returnValue(null));

                StoredObject lockNullResourceSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(lockNullResourceSo));

                StoredObject parentSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(parentSo));

                oneOf(mockStore).createFolder(mockTransaction, parentPath);

                parentSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(lockNullResourceSo));

                oneOf(mockStore).createResource(mockTransaction, path);

                lockNullResourceSo = initLockNullStoredObject();

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(lockNullResourceSo));

                oneOf(mockReq).getInputStream();
                will(returnValue(dsisExclusive));

                oneOf(mockReq).getHeader("Depth");
                will(returnValue(("0")));

                oneOf(mockReq).getHeader("Timeout");
                will(returnValue("Infinite"));

                ResourceLocks resLocks = ResourceLocks.class.newInstance();

                oneOf(mockResourceLocks).exclusiveLock(mockTransaction, path,
                        "I'am the Lock Owner", 0, 604800);
                will(returnValue(true));

                lockNullResourceLo = initLockNullLockedObject(resLocks, path);

                oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction,
                        path);
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

                // // -----LOCK on a non-existing resource successful------
                // // --------now doPUT on the lock-null resource----------

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getHeader("User-Agent");
                will(returnValue("Transmit agent"));

                oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction,
                        parentPath);
                will(returnValue(parentLo));

                oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction,
                        path);
                will(returnValue(lockNullResourceLo));

                final String ifHeaderLockToken = "(<locktoken:" + loId + ">)";

                oneOf(mockReq).getHeader("If");
                will(returnValue(ifHeaderLockToken));

                oneOf(mockResourceLocks).getLockedObjectByID(mockTransaction,
                        loId);
                will(returnValue(lockNullResourceLo));

                oneOf(mockResourceLocks).lock(with(any(ITransaction.class)),
                        with(any(String.class)), with(any(String.class)),
                        with(any(boolean.class)), with(any(int.class)),
                        with(any(int.class)), with(any(boolean.class)));
                will(returnValue(true));

                parentSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(parentSo));

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(lockNullResourceSo));

                oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction,
                        path);
                will(returnValue(lockNullResourceLo));

                oneOf(mockReq).getHeader("If");
                will(returnValue(ifHeaderLockToken));

                String[] owners = lockNullResourceLo.getOwner();
                String owner = null;
                if (owners != null) {
                    owner = owners[0];
                }

                oneOf(mockResourceLocks).unlock(mockTransaction, loId, owner);
                will(returnValue(true));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                oneOf(mockReq).getInputStream();
                will(returnValue(dsis));

                oneOf(mockStore).setResourceContent(mockTransaction, path, dsis,
                        null, null);
                will(returnValue(8L));

                StoredObject newResourceSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(newResourceSo));

                oneOf(mockResourceLocks).unlockTemporaryLockedObjects(
                        with(any(ITransaction.class)), with(any(String.class)),
                        with(any(String.class)));
            }
        });

        DoLock doLock = new DoLock(mockStore, mockResourceLocks, !readOnly);
        doLock.execute(mockTransaction, mockReq, mockRes);

        DoPut doPut = new DoPut(mockStore, mockResourceLocks, !readOnly,
                lazyFolderCreationOnPut);
        doPut.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testPathTraversalIsRejected() {
        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));
                oneOf(mockReq).getPathInfo();
                will(returnValue("/safe/../../etc/cron.d/evil"));
            }
        });

        DoPut doPut = new DoPut(mockStore, new ResourceLocks(), !readOnly, lazyFolderCreationOnPut);
        assertThrows(PathTraversalException.class, () -> doPut.execute(mockTransaction, mockReq, mockRes));
        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoPutOnLockedResourceWithoutLockTokenResultsIn423() throws Exception {

        ResourceLocks resLocks = new ResourceLocks();
        resLocks.lock(mockTransaction, path, "another owner", true, 0, 200, false);

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getHeader("User-Agent");
                will(returnValue("Goliath agent"));

                oneOf(mockReq).getHeader("If");
                will(returnValue(null));

                oneOf(mockRes).setStatus(WebdavStatus.SC_LOCKED);

                never(mockStore).setResourceContent(with(any(ITransaction.class)), with(any(String.class)),
                        with(any(InputStream.class)), with(any(String.class)), with(any(String.class)));
            }
        });

        DoPut doPut = new DoPut(mockStore, resLocks, !readOnly, lazyFolderCreationOnPut);
        doPut.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoPutWithLockedParentWithoutLockTokenResultsIn423() throws Exception {

        ResourceLocks resLocks = new ResourceLocks();
        resLocks.lock(mockTransaction, parentPath, "another owner", true, 0, 200, false);

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getHeader("User-Agent");
                will(returnValue("Goliath agent"));

                oneOf(mockReq).getHeader("If");
                will(returnValue(null));

                oneOf(mockRes).setStatus(WebdavStatus.SC_LOCKED);
            }
        });

        DoPut doPut = new DoPut(mockStore, resLocks, !readOnly, lazyFolderCreationOnPut);
        doPut.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoPutOnLockedResourceWithRightLockTokenSucceeds() throws Exception {

        ResourceLocks resLocks = new ResourceLocks();
        resLocks.lock(mockTransaction, path, "lock owner", true, 0, 200, false);
        LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction, path);
        final String rightLockToken = "(<opaquelocktoken:" + lo.getID() + ">)";

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getHeader("User-Agent");
                will(returnValue("Goliath agent"));

                oneOf(mockReq).getHeader("If");
                will(returnValue(rightLockToken));

                StoredObject parentSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(parentSo));

                StoredObject existingSo = initFileStoredObject(resourceContent);

                exactly(2).of(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(existingSo));

                oneOf(mockRes).setStatus(WebdavStatus.SC_CREATED);

                oneOf(mockReq).getInputStream();
                will(returnValue(dsis));

                oneOf(mockStore).setResourceContent(mockTransaction, path, dsis,
                        null, null);
                will(returnValue(8L));
            }
        });

        DoPut doPut = new DoPut(mockStore, resLocks, !readOnly, lazyFolderCreationOnPut);
        doPut.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @ParameterizedTest
    @CsvSource({
                "Goliath agent, 201",
                "WebDAVFS/1.5.0 (01500000), 201",
                "Transmit/4.4.1 WebDAVFS/1.0, 204"
    })
    public void testDoPutOverwritesExistingResource(String userAgent, int expectedStatus) throws Exception {

        final StoredObject existingSo = initFileStoredObject(resourceContent);

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getHeader("User-Agent");
                will(returnValue(userAgent));

                StoredObject parentSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(parentSo));

                exactly(2).of(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(existingSo));

                never(mockStore).createResource(mockTransaction, path);

                // the status depends only on the User-Agent, not on whether the resource existed
                oneOf(mockRes).setStatus(expectedStatus);

                oneOf(mockReq).getInputStream();
                will(returnValue(dsis));

                oneOf(mockStore).setResourceContent(mockTransaction, path, dsis,
                        null, null);
                will(returnValue(42L));
            }
        });

        DoPut doPut = new DoPut(mockStore, new ResourceLocks(), !readOnly,
                lazyFolderCreationOnPut);
        doPut.execute(mockTransaction, mockReq, mockRes);

        assertEquals(42L, existingSo.getResourceLength());

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoPutKeepsResourceLengthWhenStoreReportsUnknownLength() throws Exception {

        final StoredObject existingSo = initFileStoredObject(resourceContent);

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getHeader("User-Agent");
                will(returnValue(null));

                StoredObject parentSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(parentSo));

                exactly(2).of(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(existingSo));

                oneOf(mockRes).setStatus(WebdavStatus.SC_CREATED);

                oneOf(mockReq).getInputStream();
                will(returnValue(dsis));

                oneOf(mockStore).setResourceContent(mockTransaction, path, dsis,
                        null, null);
                will(returnValue(-1L));
            }
        });

        DoPut doPut = new DoPut(mockStore, new ResourceLocks(), !readOnly,
                lazyFolderCreationOnPut);
        doPut.execute(mockTransaction, mockReq, mockRes);

        assertEquals(resourceContent.length, existingSo.getResourceLength());

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoPutOnExistingFolderWritesContentToFolderPath() throws Exception {

        final String folderPath = parentPath.concat("/existingFolder");

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(folderPath));

                oneOf(mockReq).getHeader("User-Agent");
                will(returnValue("Goliath agent"));

                StoredObject parentSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(parentSo));

                StoredObject folderSo = initFolderStoredObject();

                exactly(2).of(mockStore).getStoredObject(mockTransaction, folderPath);
                will(returnValue(folderSo));

                // current behavior: no 405/409, the folder is treated like an existing resource
                oneOf(mockRes).setStatus(WebdavStatus.SC_CREATED);

                oneOf(mockReq).getInputStream();
                will(returnValue(dsis));

                oneOf(mockStore).setResourceContent(mockTransaction, folderPath, dsis,
                        null, null);
                will(returnValue(8L));
            }
        });

        DoPut doPut = new DoPut(mockStore, new ResourceLocks(), !readOnly,
                lazyFolderCreationOnPut);
        doPut.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoPutOnNullResourceWithoutLockResultsIn500() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getHeader("User-Agent");
                will(returnValue("Goliath agent"));

                StoredObject parentSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(parentSo));

                StoredObject nullSo = initLockNullStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(nullSo));

                oneOf(mockRes).sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
            }
        });

        DoPut doPut = new DoPut(mockStore, new ResourceLocks(), !readOnly,
                lazyFolderCreationOnPut);
        doPut.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoPutOnSharedLockedNullResourceWithoutIfHeaderResultsIn400() throws Exception {

        ResourceLocks resLocks = new ResourceLocks();
        resLocks.lock(mockTransaction, path, "lock owner", false, 0, 200, false);

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getHeader("User-Agent");
                will(returnValue("Goliath agent"));

                StoredObject parentSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(parentSo));

                StoredObject nullSo = initLockNullStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(nullSo));

                oneOf(mockReq).getHeader("If");
                will(returnValue(null));

                oneOf(mockRes).sendError(WebdavStatus.SC_BAD_REQUEST);
            }
        });

        DoPut doPut = new DoPut(mockStore, resLocks, !readOnly, lazyFolderCreationOnPut);
        doPut.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    static Stream<Arguments> storeExceptions() {
        return Stream.of(
                Arguments.of(new AccessDeniedException(null), WebdavStatus.SC_FORBIDDEN),
                Arguments.of(new WebdavException("store failure"), WebdavStatus.SC_INTERNAL_SERVER_ERROR));
    }

    @ParameterizedTest
    @MethodSource("storeExceptions")
    public void testDoPutStoreExceptionIsMappedToStatus(WebdavException exception, int expectedStatus)
                                                                                                       throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getHeader("User-Agent");
                will(returnValue("Goliath agent"));

                StoredObject parentSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(parentSo));

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(null));

                oneOf(mockStore).createResource(mockTransaction, path);
                will(throwException(exception));

                oneOf(mockRes).sendError(expectedStatus);
            }
        });

        DoPut doPut = new DoPut(mockStore, new ResourceLocks(), !readOnly,
                lazyFolderCreationOnPut);
        doPut.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoPutTemporaryLockFailureResultsIn500() throws Exception {

        ResourceLocks resLocks = new ResourceLocks();
        resLocks.lock(mockTransaction, path, "someone else", true, 0, 200, true);

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getHeader("User-Agent");
                will(returnValue("Goliath agent"));

                oneOf(mockRes).sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
            }
        });

        DoPut doPut = new DoPut(mockStore, resLocks, !readOnly, lazyFolderCreationOnPut);
        doPut.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }
}
