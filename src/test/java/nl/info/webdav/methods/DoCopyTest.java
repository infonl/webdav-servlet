/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.webdav.methods;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Supplier;
import java.util.stream.Stream;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.concurrent.Synchroniser;
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
import nl.info.webdav.exceptions.ObjectAlreadyExistsException;
import nl.info.webdav.exceptions.ObjectNotFoundException;
import nl.info.webdav.exceptions.WebdavException;
import nl.info.webdav.locking.LockedObject;
import nl.info.webdav.locking.ResourceLocks;
import nl.info.webdav.testutil.DelegatingServletInputStream;
import nl.info.webdav.testutil.MockTest;


public class DoCopyTest extends MockTest {

    static IWebdavStore mockStore;
    static HttpServletRequest mockReq;
    static HttpServletResponse mockRes;
    static ITransaction mockTransaction;
    static ByteArrayInputStream bais = new ByteArrayInputStream(resourceContent);
    static DelegatingServletInputStream dsis = new DelegatingServletInputStream(
            bais);

    @BeforeAll
    public static void setUp() throws Exception {
        mockStore = _mockery.mock(IWebdavStore.class);
        mockReq = _mockery.mock(HttpServletRequest.class);
        mockRes = _mockery.mock(HttpServletResponse.class);
        mockTransaction = _mockery.mock(ITransaction.class);
    }

    @Test
    public void testDoCopyIfReadOnly() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoDelete doDelete = new DoDelete(mockStore, resLocks, readOnly);

        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, readOnly);
        doCopy.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoCopyOfLockNullResource() throws Exception {

        final String parentPath = "/lockedFolder";
        final String path = parentPath.concat("/nullFile");

        String owner = "owner";
        ResourceLocks resLocks = new ResourceLocks();
        resLocks.lock(mockTransaction, parentPath, owner, true, 1,
                TEMP_TIMEOUT, !TEMPORARY);

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue("/destination"));

                oneOf(mockReq).getServerName();
                will(returnValue("myServer"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/destination"));

                oneOf(mockReq).getServletPath();
                will(returnValue("/servletPath"));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("T"));

                StoredObject so = initLockNullStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(so));

                oneOf(mockRes).addHeader("Allow",
                        "OPTIONS, MKCOL, PUT, PROPFIND, LOCK, UNLOCK");

                oneOf(mockRes).sendError(WebdavStatus.SC_METHOD_NOT_ALLOWED);
            }
        });

        DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);

        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);
        doCopy.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoCopyIfParentIsLockedWithWrongLockToken() throws Exception {

        String owner = "owner";
        ResourceLocks resLocks = new ResourceLocks();
        resLocks.lock(mockTransaction, destCollectionPath, owner, true, 1,
                TEMP_TIMEOUT, !TEMPORARY);

        final LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction,
                destCollectionPath);
        final String wrongLockToken = "(<opaquelocktoken:" + lo.getID() + "WRONG>)";

        _mockery.checking(new Expectations() {
            {
                exactly(2).of(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                exactly(3).of(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue(destFilePath));

                oneOf(mockReq).getServerName();
                will(returnValue("myServer"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getServletPath();
                will(returnValue("/servletPath"));

                oneOf(mockReq).getHeader("If");
                will(returnValue(wrongLockToken));

                oneOf(mockRes).setStatus(WebdavStatus.SC_LOCKED);
            }
        });

        DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);

        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);
        doCopy.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoCopyIfParentIsLockedWithRightLockToken() throws Exception {

        String owner = "owner";
        ResourceLocks resLocks = new ResourceLocks();

        resLocks.lock(mockTransaction, destCollectionPath, owner, true, 1,
                TEMP_TIMEOUT, !TEMPORARY);

        final LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction,
                destCollectionPath);
        final String rightLockToken = "(<opaquelocktoken:" + lo.getID() + ">)";

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue(destFilePath));

                oneOf(mockReq).getServerName();
                will(returnValue("myServer"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue(destFilePath));

                oneOf(mockReq).getServletPath();
                will(returnValue("/servletPath"));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getHeader("If");
                will(returnValue(rightLockToken));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("F"));

                StoredObject sourceFileSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceFileSo));

                StoredObject destFileSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, destFilePath);
                will(returnValue(destFileSo));

                oneOf(mockRes).setStatus(WebdavStatus.SC_CREATED);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceFileSo));

                oneOf(mockStore).createResource(mockTransaction, destFilePath);

                oneOf(mockStore).getResourceContent(mockTransaction,
                        sourceFilePath);
                will(returnValue(dsis));

                oneOf(mockStore).setResourceContent(mockTransaction,
                        destFilePath, dsis, null, null);
                will(returnValue(resourceLength));

                destFileSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, destFilePath);
                will(returnValue(destFileSo));

            }
        });

        DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);

        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);
        doCopy.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoCopyIfDestinationPathInvalid() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue(null));

                oneOf(mockRes).sendError(WebdavStatus.SC_BAD_REQUEST);
            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);

        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);
        doCopy.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();

    }

    @Test
    public void testDoCopyIfSourceEqualsDestination() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getServerName();
                will(returnValue("serverName"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getServletPath();
                will(returnValue("/servletPath"));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);

            }
        });

        ResourceLocks resLocks = new ResourceLocks();

        DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);

        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);
        doCopy.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoCopyFolderIfNoLocks() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceCollectionPath));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue(destCollectionPath));

                oneOf(mockReq).getServerName();
                will(returnValue("serverName"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue(destCollectionPath));

                oneOf(mockReq).getServletPath();
                will(returnValue("/servletPath"));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceCollectionPath));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("F"));

                StoredObject sourceCollectionSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction,
                        sourceCollectionPath);
                will(returnValue(sourceCollectionSo));

                StoredObject destCollectionSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction,
                        destCollectionPath);
                will(returnValue(destCollectionSo));

                oneOf(mockRes).setStatus(WebdavStatus.SC_CREATED);

                oneOf(mockStore).getStoredObject(mockTransaction,
                        sourceCollectionPath);
                will(returnValue(sourceCollectionSo));

                oneOf(mockStore)
                        .createFolder(mockTransaction, destCollectionPath);

                oneOf(mockReq).getHeader("Depth");
                will(returnValue("-1"));

                sourceChildren = new String[]{"sourceFile"};

                oneOf(mockStore).getChildrenNames(mockTransaction,
                        sourceCollectionPath);
                will(returnValue(sourceChildren));

                StoredObject sourceFileSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceFileSo));

                oneOf(mockStore).createResource(mockTransaction,
                        destCollectionPath + "/sourceFile");

                oneOf(mockStore).getResourceContent(mockTransaction,
                        sourceFilePath);
                will(returnValue(dsis));

                oneOf(mockStore).setResourceContent(mockTransaction,
                        destCollectionPath + "/sourceFile", dsis, null, null);

                StoredObject destFileSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction,
                        destCollectionPath + "/sourceFile");
                will(returnValue(destFileSo));

            }
        });

        ResourceLocks resLocks = new ResourceLocks();

        DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);

        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);
        doCopy.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoCopyIfSourceDoesntExist() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue(destFilePath));

                oneOf(mockReq).getServerName();
                will(returnValue("serverName"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue(destFilePath));

                oneOf(mockReq).getServletPath();
                will(returnValue("/servletPath"));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("F"));

                StoredObject notExistSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(notExistSo));

                oneOf(mockRes).sendError(WebdavStatus.SC_NOT_FOUND);

            }
        });

        ResourceLocks resLocks = new ResourceLocks();

        DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);

        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);
        doCopy.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoCopyIfDestinationAlreadyExistsAndOverwriteTrue()
                                                                       throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                StoredObject sourceSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceSo));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue(destFilePath));

                oneOf(mockReq).getServerName();
                will(returnValue("serverName"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/folder/destFolder"));

                oneOf(mockReq).getServletPath();
                will(returnValue("/servletPath"));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                StoredObject existingDestSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, destFilePath);
                will(returnValue(existingDestSo));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("T"));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, destFilePath);
                will(returnValue(existingDestSo));

                oneOf(mockStore).removeObject(mockTransaction, destFilePath);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceSo));

                oneOf(mockStore).createResource(mockTransaction, destFilePath);

                oneOf(mockStore).getResourceContent(mockTransaction,
                        sourceFilePath);
                will(returnValue(dsis));

                oneOf(mockStore).setResourceContent(mockTransaction,
                        destFilePath, dsis, null, null);

                oneOf(mockStore).getStoredObject(mockTransaction, destFilePath);
                will(returnValue(existingDestSo));

            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);

        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);
        doCopy.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();

    }

    @Test
    public void testDoCopyIfDestinationAlreadyExistsAndOverwriteFalse()
                                                                        throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                StoredObject sourceSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceSo));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue("serverName".concat(destFilePath)));

                oneOf(mockReq).getServerName();
                will(returnValue("serverName"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue(destFilePath));

                oneOf(mockReq).getServletPath();
                will(returnValue("/servletPath"));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                StoredObject existingDestSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, destFilePath);
                will(returnValue(existingDestSo));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("F"));

                oneOf(mockRes).sendError(WebdavStatus.SC_PRECONDITION_FAILED);

            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);

        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);
        doCopy.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();

    }

    @Test
    public void testDoCopyIfOverwriteTrue() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                StoredObject sourceSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceSo));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue("http://destination:80".concat(destFilePath)));

                oneOf(mockReq).getContextPath();
                will(returnValue("http://destination:80"));

                oneOf(mockReq).getPathInfo();
                will(returnValue(destCollectionPath));

                oneOf(mockReq).getServletPath();
                will(returnValue("http://destination:80"));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getAttribute("javax.servlet.include.path_info");
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("T"));

                StoredObject destFileSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, destFilePath);
                will(returnValue(destFileSo));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, destFilePath);
                will(returnValue(destFileSo));

                oneOf(mockStore).removeObject(mockTransaction, destFilePath);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceSo));

                oneOf(mockStore).createResource(mockTransaction, destFilePath);

                oneOf(mockStore).getResourceContent(mockTransaction,
                        sourceFilePath);
                will(returnValue(dsis));

                oneOf(mockStore).setResourceContent(mockTransaction,
                        destFilePath, dsis, null, null);

                oneOf(mockStore).getStoredObject(mockTransaction, destFilePath);
                will(returnValue(destFileSo));
            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);

        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);
        doCopy.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();

    }

    @Test
    public void testDoCopyIfOverwriteFalse() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                StoredObject sourceSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceSo));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue("http://destination:80"
                        .concat(destCollectionPath)));

                oneOf(mockReq).getContextPath();
                will(returnValue("http://destination:80"));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getServletPath();
                will(returnValue("http://destination:80"));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getAttribute("javax.servlet.include.path_info");
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("F"));

                StoredObject existingDestSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction,
                        destCollectionPath);
                will(returnValue(existingDestSo));

                oneOf(mockRes).sendError(WebdavStatus.SC_PRECONDITION_FAILED);
            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);

        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);
        doCopy.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();

    }

    @Test
    public void testDestinationHeaderTraversalIsRejected() throws Exception {
        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));
                oneOf(mockReq).getPathInfo();
                will(returnValue("/source"));

                // "/../" segment is caught by assertSafePath before normalize() runs
                oneOf(mockReq).getHeader("Destination");
                will(returnValue("http://localhost/webdav/safe/../../../etc/passwd"));

                oneOf(mockRes).sendError(WebdavStatus.SC_BAD_REQUEST);
            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);
        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);
        doCopy.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDestinationHeaderSingleParentSegmentIsRejected() throws Exception {
        // normalize() would resolve /safe/../secret to /secret (not null),
        // so this test proves assertSafePath runs before normalize()
        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));
                oneOf(mockReq).getPathInfo();
                will(returnValue("/source"));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue("http://localhost/safe/../secret"));

                oneOf(mockRes).sendError(WebdavStatus.SC_BAD_REQUEST);
            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);
        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);
        doCopy.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDestinationHeaderDotSegmentIsRejected() throws Exception {
        // normalize() would resolve /foo/./bar to /foo/bar (not null),
        // so this test proves assertSafePath runs before normalize()
        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));
                oneOf(mockReq).getPathInfo();
                will(returnValue("/source"));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue("http://localhost/foo/./bar"));

                oneOf(mockRes).sendError(WebdavStatus.SC_BAD_REQUEST);
            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);
        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);
        doCopy.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    // ---------------------------------------------------------------------
    // The tests below use a fresh Mockery per test (see Fixture) so that
    // 'allowing' expectations on request getters do not leak into other tests.
    // ---------------------------------------------------------------------

    private static final String SOURCE = "/source";

    private static final class Fixture {
        final Mockery context = new Mockery();
        final IWebdavStore store;
        final HttpServletRequest req;
        final HttpServletResponse resp;
        final ITransaction tx;
        final ResourceLocks locks = new ResourceLocks();

        Fixture() {
            context.setThreadingPolicy(new Synchroniser());
            store = context.mock(IWebdavStore.class);
            req = context.mock(HttpServletRequest.class);
            resp = context.mock(HttpServletResponse.class);
            tx = context.mock(ITransaction.class);
        }

        DoCopy doCopy(boolean readOnly) {
            return new DoCopy(store, locks, new DoDelete(store, locks, readOnly), readOnly);
        }

        /**
         * Allows all request getters used by DoCopy.
         */
        void allowRequest(
                String sourcePath,
                String destinationHeader,
                String contextPath,
                String servletPath,
                String overwrite,
                String depth
        ) {
            context.checking(new Expectations() {
                {
                    allowing(req).getAttribute("javax.servlet.include.request_uri");
                    will(returnValue(null));
                    allowing(req).getPathInfo();
                    will(returnValue(sourcePath));
                    allowing(req).getHeader("Destination");
                    will(returnValue(destinationHeader));
                    allowing(req).getServerName();
                    will(returnValue("myServer"));
                    allowing(req).getContextPath();
                    will(returnValue(contextPath));
                    allowing(req).getServletPath();
                    will(returnValue(servletPath));
                    allowing(req).getHeader("If");
                    will(returnValue(null));
                    allowing(req).getHeader("Overwrite");
                    will(returnValue(overwrite));
                    allowing(req).getHeader("Depth");
                    will(returnValue(depth));
                    allowing(req).getRequestURI();
                    will(returnValue("/requestUri"));
                }
            });
        }

        void allowRequest(String sourcePath, String destinationHeader) {
            allowRequest(sourcePath, destinationHeader, "", "", null, null);
        }

        /**
         * Expects a file at sourcePath to be copied to a not yet existing expectedDestination.
         */
        void expectFileCopyToNewDestination(String sourcePath, String expectedDestination) {
            StoredObject sourceSo = initFileStoredObject(resourceContent);
            context.checking(new Expectations() {
                {
                    allowing(store).getStoredObject(tx, sourcePath);
                    will(returnValue(sourceSo));
                    oneOf(store).getStoredObject(tx, expectedDestination);
                    will(returnValue(null));
                    oneOf(resp).setStatus(WebdavStatus.SC_CREATED);
                    oneOf(store).createResource(tx, expectedDestination);
                    allowing(store).getResourceContent(tx, sourcePath);
                    will(returnValue(dsis));
                    oneOf(store).setResourceContent(tx, expectedDestination, dsis, null, null);
                    will(returnValue(-1L));
                }
            });
        }
    }

    @Test
    public void testAbsoluteUrlDestinationIsStrippedToRelativePath() throws Exception {
        Fixture f = new Fixture();
        f.allowRequest(SOURCE, "http://host:8080/context/servlet/target", "/context", "/servlet", null, null);
        f.expectFileCopyToNewDestination(SOURCE, "/target");

        f.doCopy(false).execute(f.tx, f.req, f.resp);

        f.context.assertIsSatisfied();
    }

    @Test
    public void testAbsoluteUrlDestinationWithoutPathResolvesToRoot() throws Exception {
        Fixture f = new Fixture();
        f.allowRequest(SOURCE, "http://host:8080");
        f.expectFileCopyToNewDestination(SOURCE, "/");

        f.doCopy(false).execute(f.tx, f.req, f.resp);

        f.context.assertIsSatisfied();
    }

    @Test
    public void testHostNameAndPortWithoutSchemeAreStripped() throws Exception {
        Fixture f = new Fixture();
        f.allowRequest(SOURCE, "myServer:8080/target");
        f.expectFileCopyToNewDestination(SOURCE, "/target");

        f.doCopy(false).execute(f.tx, f.req, f.resp);

        f.context.assertIsSatisfied();
    }

    @Test
    public void testPortWithoutPathResolvesToRoot() throws Exception {
        Fixture f = new Fixture();
        f.allowRequest(SOURCE, "myServer:8080");
        f.expectFileCopyToNewDestination(SOURCE, "/");

        f.doCopy(false).execute(f.tx, f.req, f.resp);

        f.context.assertIsSatisfied();
    }

    @ParameterizedTest
    @CsvSource({
                "/dest%20file, /dest file",
                "/caf%C3%A9, /café",
                "http://host/a%2Fb, /a/b"
    })
    public void testPercentEncodedDestinationIsDecoded(String header, String expected) throws Exception {
        Fixture f = new Fixture();
        f.allowRequest(SOURCE, header);
        f.expectFileCopyToNewDestination(SOURCE, expected);

        f.doCopy(false).execute(f.tx, f.req, f.resp);

        f.context.assertIsSatisfied();
    }

    @Test
    public void testDestinationWithDoubleSlashesIsNormalized() throws Exception {
        Fixture f = new Fixture();
        f.allowRequest(SOURCE, "http://host//a//b");
        f.expectFileCopyToNewDestination(SOURCE, "/a/b");

        f.doCopy(false).execute(f.tx, f.req, f.resp);

        f.context.assertIsSatisfied();
    }

    @ParameterizedTest
    @CsvSource({
                "/., /",
                "a/b, /a/b",
                "/a//b, /a/b",
                "/a/./b, /a/b",
                "/a/b/../c, /a/c",
                "/a/b/./../c, /a/c",
                "\\a\\b, /a/b"
    })
    public void testNormalizeResolvesDotSegmentsInsideRoot(String path, String expected) {
        DoCopy doCopy = new DoCopy(null, new ResourceLocks(), null, false);
        assertEquals(expected, doCopy.normalize(path));
    }

    @ParameterizedTest
    @CsvSource({
                "/../a",
                "/a/../../b"
    })
    public void testNormalizeReturnsNullWhenLeavingRoot(String path) {
        DoCopy doCopy = new DoCopy(null, new ResourceLocks(), null, false);
        assertNull(doCopy.normalize(path));
    }

    @Test
    public void testNormalizeOfNullIsNull() {
        DoCopy doCopy = new DoCopy(null, new ResourceLocks(), null, false);
        assertNull(doCopy.normalize(null));
    }

    @Test
    public void testNormalizeDoesNotResolveTrailingParentSegment() {
        // Characterization (candidate bug): normalize() only resolves "/../" when followed by a slash,
        // so a trailing "/.." is kept. Via the Destination header this is reachable with backslashes
        // (e.g. "/a%5C..%5C.."), because assertSafePath() only splits on '/'.
        DoCopy doCopy = new DoCopy(null, new ResourceLocks(), null, false);
        assertEquals("/a/..", doCopy.normalize("/a\\.."));
        assertEquals("/..", doCopy.normalize("/a\\..\\.."));
    }

    @ParameterizedTest
    @CsvSource({
                "http://host/source",
                "//source",
                "myServer/source"
    })
    public void testNormalizedDestinationEqualToSourceIsForbidden(String header) throws Exception {
        Fixture f = new Fixture();
        f.allowRequest(SOURCE, header);
        f.context.checking(new Expectations() {
            {
                oneOf(f.resp).sendError(WebdavStatus.SC_FORBIDDEN);
            }
        });

        f.doCopy(false).execute(f.tx, f.req, f.resp);

        f.context.assertIsSatisfied();
    }

    @Test
    public void testMissingDestinationHeaderIsBadRequest() throws Exception {
        Fixture f = new Fixture();
        f.allowRequest(SOURCE, null);
        f.context.checking(new Expectations() {
            {
                oneOf(f.resp).sendError(WebdavStatus.SC_BAD_REQUEST);
            }
        });

        f.doCopy(false).execute(f.tx, f.req, f.resp);

        f.context.assertIsSatisfied();
    }

    @Test
    public void testOverwriteFalseWithExistingDestinationLeavesDestinationUntouched() throws Exception {
        Fixture f = new Fixture();
        f.allowRequest(SOURCE, "/target", "", "", "F", null);
        f.context.checking(new Expectations() {
            {
                allowing(f.store).getStoredObject(f.tx, SOURCE);
                will(returnValue(initFileStoredObject(resourceContent)));
                allowing(f.store).getStoredObject(f.tx, "/target");
                will(returnValue(initFileStoredObject(resourceContent)));
                oneOf(f.resp).sendError(WebdavStatus.SC_PRECONDITION_FAILED);
                never(f.store).removeObject(f.tx, "/target");
                never(f.store).createResource(f.tx, "/target");
            }
        });

        f.doCopy(false).execute(f.tx, f.req, f.resp);

        f.context.assertIsSatisfied();
    }

    @ParameterizedTest
    @CsvSource(value = {"T", "t", "NULL"}, nullValues = "NULL")
    public void testOverwriteTrueOrAbsentWithExistingDestinationReplacesIt(String overwrite) throws Exception {
        Fixture f = new Fixture();
        f.allowRequest(SOURCE, "/target", "", "", overwrite, null);
        StoredObject sourceSo = initFileStoredObject(resourceContent);
        StoredObject destSo = initFileStoredObject(new byte[0]);
        f.context.checking(new Expectations() {
            {
                allowing(f.store).getStoredObject(f.tx, SOURCE);
                will(returnValue(sourceSo));
                allowing(f.store).getStoredObject(f.tx, "/target");
                will(returnValue(destSo));
                oneOf(f.resp).setStatus(WebdavStatus.SC_NO_CONTENT);
                oneOf(f.store).removeObject(f.tx, "/target");
                oneOf(f.store).createResource(f.tx, "/target");
                allowing(f.store).getResourceContent(f.tx, SOURCE);
                will(returnValue(dsis));
                oneOf(f.store).setResourceContent(f.tx, "/target", dsis, null, null);
                will(returnValue(resourceLength));
            }
        });

        f.doCopy(false).execute(f.tx, f.req, f.resp);

        f.context.assertIsSatisfied();
        assertEquals(resourceLength, destSo.getResourceLength());
    }

    @Test
    public void testOverwriteOtherValueIsTreatedAsFalse() throws Exception {
        Fixture f = new Fixture();
        f.allowRequest(SOURCE, "/target", "", "", "X", null);
        f.context.checking(new Expectations() {
            {
                allowing(f.store).getStoredObject(f.tx, SOURCE);
                will(returnValue(initFileStoredObject(resourceContent)));
                allowing(f.store).getStoredObject(f.tx, "/target");
                will(returnValue(initFileStoredObject(resourceContent)));
                oneOf(f.resp).sendError(WebdavStatus.SC_PRECONDITION_FAILED);
            }
        });

        f.doCopy(false).execute(f.tx, f.req, f.resp);

        f.context.assertIsSatisfied();
    }

    @Test
    public void testFolderCopyWithDepthZeroDoesNotCopyChildren() throws Exception {
        Fixture f = new Fixture();
        f.allowRequest("/folder", "/copy", "", "", null, "0");
        f.context.checking(new Expectations() {
            {
                allowing(f.store).getStoredObject(f.tx, "/folder");
                will(returnValue(initFolderStoredObject()));
                oneOf(f.store).getStoredObject(f.tx, "/copy");
                will(returnValue(null));
                oneOf(f.resp).setStatus(WebdavStatus.SC_CREATED);
                oneOf(f.store).createFolder(f.tx, "/copy");
                never(f.store).getChildrenNames(f.tx, "/folder");
            }
        });

        f.doCopy(false).execute(f.tx, f.req, f.resp);

        f.context.assertIsSatisfied();
    }

    @Test
    public void testFolderCopyRecursesIntoSubFolders() throws Exception {
        Fixture f = new Fixture();
        f.allowRequest("/folder", "/copy");
        f.context.checking(new Expectations() {
            {
                allowing(f.store).getStoredObject(f.tx, "/folder");
                will(returnValue(initFolderStoredObject()));
                oneOf(f.store).getStoredObject(f.tx, "/copy");
                will(returnValue(null));
                oneOf(f.resp).setStatus(WebdavStatus.SC_CREATED);
                oneOf(f.store).createFolder(f.tx, "/copy");
                oneOf(f.store).getChildrenNames(f.tx, "/folder");
                will(returnValue(new String[]{"sub"}));
                allowing(f.store).getStoredObject(f.tx, "/folder/sub");
                will(returnValue(initFolderStoredObject()));
                oneOf(f.store).createFolder(f.tx, "/copy/sub");
                oneOf(f.store).getChildrenNames(f.tx, "/folder/sub");
                will(returnValue(null));
            }
        });

        f.doCopy(false).execute(f.tx, f.req, f.resp);

        f.context.assertIsSatisfied();
    }

    static Stream<Arguments> childCopyExceptions() {
        return Stream.of(
                Arguments.of((Supplier<WebdavException>) () -> new AccessDeniedException(null), WebdavStatus.SC_FORBIDDEN),
                Arguments.of((Supplier<WebdavException>) () -> new ObjectNotFoundException(null), WebdavStatus.SC_NOT_FOUND),
                Arguments.of((Supplier<WebdavException>) () -> new ObjectAlreadyExistsException(null), WebdavStatus.SC_CONFLICT),
                Arguments.of((Supplier<WebdavException>) () -> new WebdavException("boom"),
                        WebdavStatus.SC_INTERNAL_SERVER_ERROR)
        );
    }

    @ParameterizedTest
    @MethodSource("childCopyExceptions")
    public void testChildCopyFailuresAreReportedInMultiStatus(
            Supplier<WebdavException> exception,
            int expectedStatus
    ) throws Exception {
        Fixture f = new Fixture();
        f.allowRequest("/folder", "/copy");
        StringWriter body = new StringWriter();
        f.context.checking(new Expectations() {
            {
                allowing(f.store).getStoredObject(f.tx, "/folder");
                will(returnValue(initFolderStoredObject()));
                oneOf(f.store).getStoredObject(f.tx, "/copy");
                will(returnValue(null));
                oneOf(f.resp).setStatus(WebdavStatus.SC_CREATED);
                oneOf(f.store).createFolder(f.tx, "/copy");
                oneOf(f.store).getChildrenNames(f.tx, "/folder");
                will(returnValue(new String[]{"a", "b"}));
                oneOf(f.store).getStoredObject(f.tx, "/folder/a");
                will(throwException(exception.get()));
                oneOf(f.store).getStoredObject(f.tx, "/folder/b");
                will(throwException(exception.get()));
                oneOf(f.resp).setStatus(WebdavStatus.SC_MULTI_STATUS);
                oneOf(f.resp).setContentType("text/xml; charset=UTF-8");
                oneOf(f.resp).getWriter();
                will(returnValue(new PrintWriter(body)));
            }
        });

        f.doCopy(false).execute(f.tx, f.req, f.resp);

        f.context.assertIsSatisfied();
        String xml = body.toString();
        assertTrue(xml.contains("multistatus"), xml);
        assertTrue(xml.contains("/copy/a"), xml);
        assertTrue(xml.contains("/copy/b"), xml);
        assertTrue(xml.contains("HTTP/1.1 " + expectedStatus + " " + WebdavStatus.getStatusText(expectedStatus)), xml);
    }

    @Test
    public void testSingleChildCopyFailureIsSentAsPlainError() throws Exception {
        Fixture f = new Fixture();
        f.allowRequest("/folder", "/copy");
        f.context.checking(new Expectations() {
            {
                allowing(f.store).getStoredObject(f.tx, "/folder");
                will(returnValue(initFolderStoredObject()));
                oneOf(f.store).getStoredObject(f.tx, "/copy");
                will(returnValue(null));
                oneOf(f.resp).setStatus(WebdavStatus.SC_CREATED);
                oneOf(f.store).createFolder(f.tx, "/copy");
                oneOf(f.store).getChildrenNames(f.tx, "/folder");
                will(returnValue(new String[]{"a"}));
                oneOf(f.store).getStoredObject(f.tx, "/folder/a");
                will(throwException(new AccessDeniedException(null)));
                oneOf(f.resp).sendError(WebdavStatus.SC_FORBIDDEN, "Forbidden");
            }
        });

        f.doCopy(false).execute(f.tx, f.req, f.resp);

        f.context.assertIsSatisfied();
    }

    @Test
    public void testCopyIfReadOnlyIsForbidden() throws Exception {
        Fixture f = new Fixture();
        f.allowRequest(SOURCE, "/target");
        f.context.checking(new Expectations() {
            {
                oneOf(f.resp).sendError(WebdavStatus.SC_FORBIDDEN);
            }
        });

        f.doCopy(true).execute(f.tx, f.req, f.resp);

        f.context.assertIsSatisfied();
    }

    @Test
    public void testCopyIfSourceNotFound() throws Exception {
        Fixture f = new Fixture();
        f.allowRequest(SOURCE, "/target");
        f.context.checking(new Expectations() {
            {
                oneOf(f.store).getStoredObject(f.tx, SOURCE);
                will(returnValue(null));
                oneOf(f.resp).sendError(WebdavStatus.SC_NOT_FOUND);
            }
        });

        f.doCopy(false).execute(f.tx, f.req, f.resp);

        f.context.assertIsSatisfied();
    }

    @Test
    public void testCopyIfDestinationIsLockedWithoutToken() throws Exception {
        Fixture f = new Fixture();
        f.allowRequest(SOURCE, "/target");
        f.locks.lock(f.tx, "/target", "owner", true, 0, TEMP_TIMEOUT, !TEMPORARY);
        f.context.checking(new Expectations() {
            {
                oneOf(f.resp).setStatus(WebdavStatus.SC_LOCKED);
            }
        });

        f.doCopy(false).execute(f.tx, f.req, f.resp);

        f.context.assertIsSatisfied();
    }

    @Test
    public void testCopyIfDestinationParentIsLockedWithoutToken() throws Exception {
        Fixture f = new Fixture();
        f.allowRequest(SOURCE, "/parent/target");
        f.locks.lock(f.tx, "/parent", "owner", true, 0, TEMP_TIMEOUT, !TEMPORARY);
        f.context.checking(new Expectations() {
            {
                oneOf(f.resp).setStatus(WebdavStatus.SC_LOCKED);
            }
        });

        f.doCopy(false).execute(f.tx, f.req, f.resp);

        f.context.assertIsSatisfied();
    }

    @Test
    public void testCopyIfDestinationIsSharedLockedIsAllowed() throws Exception {
        Fixture f = new Fixture();
        f.allowRequest(SOURCE, "/target");
        f.locks.lock(f.tx, "/target", "owner", false, 0, TEMP_TIMEOUT, !TEMPORARY);
        f.expectFileCopyToNewDestination(SOURCE, "/target");

        f.doCopy(false).execute(f.tx, f.req, f.resp);

        f.context.assertIsSatisfied();
    }

    @Test
    public void testCopyIfSourceIsTemporarilyLockedExclusively() throws Exception {
        Fixture f = new Fixture();
        f.allowRequest(SOURCE, "/target");
        f.locks.lock(f.tx, SOURCE, "other", true, 0, TEMP_TIMEOUT, TEMPORARY);
        f.context.checking(new Expectations() {
            {
                oneOf(f.resp).sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
            }
        });

        f.doCopy(false).execute(f.tx, f.req, f.resp);

        f.context.assertIsSatisfied();
    }

    @Test
    public void testCopyIfDestinationIsTemporarilyLockedExclusively() throws Exception {
        Fixture f = new Fixture();
        f.allowRequest(SOURCE, "/target");
        f.locks.lock(f.tx, "/target", "other", true, 0, TEMP_TIMEOUT, TEMPORARY);
        f.context.checking(new Expectations() {
            {
                oneOf(f.resp).sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
            }
        });

        f.doCopy(false).execute(f.tx, f.req, f.resp);

        f.context.assertIsSatisfied();
    }

    static Stream<Arguments> storeExceptions() {
        return Stream.of(
                Arguments.of((Supplier<WebdavException>) () -> new AccessDeniedException(null), WebdavStatus.SC_FORBIDDEN, false),
                Arguments.of((Supplier<WebdavException>) () -> new ObjectAlreadyExistsException(null), WebdavStatus.SC_CONFLICT, true),
                Arguments.of((Supplier<WebdavException>) () -> new ObjectNotFoundException(null), WebdavStatus.SC_NOT_FOUND, true),
                Arguments.of((Supplier<WebdavException>) () -> new WebdavException("boom"),
                        WebdavStatus.SC_INTERNAL_SERVER_ERROR, false)
        );
    }

    @ParameterizedTest
    @MethodSource("storeExceptions")
    public void testStoreExceptionIsMappedToStatus(
            Supplier<WebdavException> exception,
            int expectedStatus,
            boolean withRequestUri
    ) throws Exception {
        Fixture f = new Fixture();
        f.allowRequest(SOURCE, "/target");
        f.context.checking(new Expectations() {
            {
                oneOf(f.store).getStoredObject(f.tx, SOURCE);
                will(throwException(exception.get()));
                if (withRequestUri) {
                    oneOf(f.resp).sendError(expectedStatus, "/requestUri");
                } else {
                    oneOf(f.resp).sendError(expectedStatus);
                }
            }
        });

        f.doCopy(false).execute(f.tx, f.req, f.resp);

        f.context.assertIsSatisfied();
    }
}
