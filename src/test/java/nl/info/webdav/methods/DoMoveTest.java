/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.webdav.methods;

import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import nl.info.webdav.ITransaction;
import nl.info.webdav.IWebdavStore;
import nl.info.webdav.StoredObject;
import nl.info.webdav.WebdavStatus;
import nl.info.webdav.exceptions.AccessDeniedException;
import nl.info.webdav.exceptions.ObjectAlreadyExistsException;
import nl.info.webdav.exceptions.ObjectNotFoundException;
import nl.info.webdav.exceptions.PathTraversalException;
import nl.info.webdav.exceptions.WebdavException;
import nl.info.webdav.locking.ResourceLocks;
import nl.info.webdav.testutil.DelegatingServletInputStream;
import nl.info.webdav.testutil.MockTest;

public class DoMoveTest extends MockTest {
    static IWebdavStore mockStore;
    static HttpServletRequest mockReq;
    static HttpServletResponse mockRes;
    static ITransaction mockTransaction;
    static byte[] resourceContent = new byte[]{'<', 'h', 'e', 'l', 'l', 'o',
                                               '/', '>'};
    static ByteArrayInputStream bais = new ByteArrayInputStream(resourceContent);
    static DelegatingServletInputStream dsis = new DelegatingServletInputStream(
            bais);

    static final String tmpFolder = "/tmp/tests";
    static final String sourceCollectionPath = tmpFolder + "/sourceFolder";
    static final String destCollectionPath = tmpFolder + "/destFolder";
    static final String sourceFilePath = sourceCollectionPath + "/sourceFile";
    static final String destFilePath = destCollectionPath + "/destFile";
    static final String overwritePath = destCollectionPath + "/sourceFolder";

    @BeforeAll
    public static void setUp() throws Exception {
        mockStore = _mockery.mock(IWebdavStore.class);
        mockReq = _mockery.mock(HttpServletRequest.class);
        mockRes = _mockery.mock(HttpServletResponse.class);
        mockTransaction = _mockery.mock(ITransaction.class);
    }

    @Test
    public void testMovingOfFileOrFolderIfReadOnlyIsTrue() throws Exception {
        _mockery.checking(new Expectations() {
            {
                oneOf(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoDelete doDelete = new DoDelete(mockStore, resLocks, readOnly);
        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, readOnly);

        DoMove doMove = new DoMove(resLocks, doDelete, doCopy, readOnly);

        doMove.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testMovingOfaFileIfDestinationNotPresent() throws Exception {
        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                exactly(2).of(mockReq).getHeader("Destination");
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
                will(returnValue(8L));

                destFileSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, destFilePath);
                will(returnValue(destFileSo));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceFileSo));

                oneOf(mockStore).removeObject(mockTransaction, sourceFilePath);
            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);
        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);

        DoMove doMove = new DoMove(resLocks, doDelete, doCopy, !readOnly);

        doMove.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testMovingOfaFileIfDestinationIsPresentAndOverwriteFalse() throws Exception {
        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                exactly(2).of(mockReq).getHeader("Destination");
                will(returnValue(destFilePath));

                oneOf(mockReq).getServerName();
                will(returnValue("server_name"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue(destFilePath));

                oneOf(mockReq).getServletPath();
                will(returnValue("servlet_path"));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("F"));

                StoredObject sourceFileSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceFileSo));

                StoredObject destFileSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, destFilePath);
                will(returnValue(destFileSo));

                oneOf(mockRes).sendError(WebdavStatus.SC_PRECONDITION_FAILED);

            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);
        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);

        DoMove doMove = new DoMove(resLocks, doDelete, doCopy, !readOnly);

        doMove.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testMovingOfaFileIfDestinationIsPresentAndOverwriteTrue() throws Exception {
        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                exactly(2).of(mockReq).getHeader("Destination");
                will(returnValue(destFilePath));

                oneOf(mockReq).getServerName();
                will(returnValue("server_name"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue(destFilePath));

                oneOf(mockReq).getServletPath();
                will(returnValue("servlet_path"));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("T"));

                StoredObject sourceFileSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceFileSo));

                StoredObject destFileSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, destFilePath);
                will(returnValue(destFileSo));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, destFilePath);
                will(returnValue(destFileSo));

                oneOf(mockStore).removeObject(mockTransaction, destFilePath);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceFileSo));

                oneOf(mockStore).createResource(mockTransaction, destFilePath);

                oneOf(mockStore).getResourceContent(mockTransaction,
                        sourceFilePath);
                will(returnValue(dsis));

                oneOf(mockStore).setResourceContent(mockTransaction,
                        destFilePath, dsis, null, null);
                will(returnValue(8L));

                oneOf(mockStore).getStoredObject(mockTransaction, destFilePath);
                will(returnValue(destFileSo));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceFileSo));

                oneOf(mockStore).removeObject(mockTransaction, sourceFilePath);
            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);
        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);
        DoMove doMove = new DoMove(resLocks, doDelete, doCopy, !readOnly);

        doMove.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testMovingOfaFileIfSourceNotPresent() throws Exception {
        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                exactly(2).of(mockReq).getHeader("Destination");
                will(returnValue(destFilePath));

                oneOf(mockReq).getServerName();
                will(returnValue("server_name"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue(destFilePath));

                oneOf(mockReq).getServletPath();
                will(returnValue("servlet_path"));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("F"));

                StoredObject sourceFileSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceFileSo));

                oneOf(mockRes).sendError(WebdavStatus.SC_NOT_FOUND);
            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);
        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);

        DoMove doMove = new DoMove(resLocks, doDelete, doCopy, !readOnly);

        doMove.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testMovingIfSourcePathEqualsDestinationPath() throws Exception {
        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(destFilePath));

                exactly(2).of(mockReq).getHeader("Destination");
                will(returnValue(destFilePath));

                oneOf(mockReq).getServerName();
                will(returnValue("server_name"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue(destFilePath));

                oneOf(mockReq).getServletPath();
                will(returnValue("servlet_path"));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(destFilePath));

                oneOf(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);
        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);

        DoMove doMove = new DoMove(resLocks, doDelete, doCopy, !readOnly);

        doMove.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testMovingOfaCollectionIfDestinationIsNotPresent() throws Exception {
        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceCollectionPath));

                exactly(2).of(mockReq).getHeader("Destination");
                will(returnValue(destCollectionPath));

                oneOf(mockReq).getServerName();
                will(returnValue("server_name"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue(destCollectionPath));

                oneOf(mockReq).getServletPath();
                will(returnValue("servlet_path"));

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
                will(returnValue(null));

                String[] sourceChildren = new String[]{"sourceFile"};

                oneOf(mockStore).getChildrenNames(mockTransaction,
                        sourceCollectionPath);
                will(returnValue(sourceChildren));

                StoredObject sourceFileSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction,
                        sourceCollectionPath + "/sourceFile");
                will(returnValue(sourceFileSo));

                oneOf(mockStore).createResource(mockTransaction,
                        destCollectionPath + "/sourceFile");

                oneOf(mockStore).getResourceContent(mockTransaction,
                        sourceCollectionPath + "/sourceFile");
                will(returnValue(dsis));

                oneOf(mockStore).setResourceContent(mockTransaction,
                        destCollectionPath + "/sourceFile", dsis, null, null);
                will(returnValue(8L));

                StoredObject movedSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction,
                        destCollectionPath + "/sourceFile");
                will(returnValue(movedSo));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction,
                        sourceCollectionPath);
                will(returnValue(sourceCollectionSo));

                sourceChildren = new String[]{"sourceFile"};

                oneOf(mockStore).getChildrenNames(mockTransaction,
                        sourceCollectionPath);
                will(returnValue(sourceChildren));

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceFileSo));

                oneOf(mockStore).removeObject(mockTransaction, sourceFilePath);

                oneOf(mockStore).removeObject(mockTransaction,
                        sourceCollectionPath);
            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);
        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);

        DoMove doMove = new DoMove(resLocks, doDelete, doCopy, !readOnly);

        doMove.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testMovingOfaCollectionIfDestinationIsPresentAndOverwriteFalse() throws Exception {
        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceCollectionPath));

                exactly(2).of(mockReq).getHeader("Destination");
                will(returnValue(destCollectionPath));

                oneOf(mockReq).getServerName();
                will(returnValue("server_name"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue(destCollectionPath));

                oneOf(mockReq).getServletPath();
                will(returnValue("servlet_path"));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceCollectionPath));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("F"));

                StoredObject sourceCollectionSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, sourceCollectionPath);
                will(returnValue(sourceCollectionSo));

                StoredObject destCollectionSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, destCollectionPath);
                will(returnValue(destCollectionSo));

                oneOf(mockRes).sendError(WebdavStatus.SC_PRECONDITION_FAILED);
            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);
        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);

        DoMove doMove = new DoMove(resLocks, doDelete, doCopy, !readOnly);

        doMove.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    @Disabled("Fails currently")
    public void testMovingOfaCollectionIfDestinationIsPresentAndOverwriteTrue() throws Exception {
        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceCollectionPath));

                exactly(2).of(mockReq).getHeader("Destination");
                will(returnValue(overwritePath));

                oneOf(mockReq).getServerName();
                will(returnValue("server_name"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue(overwritePath));

                oneOf(mockReq).getServletPath();
                will(returnValue("servlet_path"));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceCollectionPath));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("T"));

                StoredObject sourceCollectionSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction,
                        sourceCollectionPath);
                will(returnValue(sourceCollectionSo));

                StoredObject destCollectionSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, overwritePath);
                will(returnValue(destCollectionSo));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, overwritePath);
                will(returnValue(destCollectionSo));

                oneOf(mockStore).getChildrenNames(mockTransaction, overwritePath);
                will(returnValue(destChildren));

                StoredObject destFileSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction,
                        overwritePath + "/destFile");
                will(returnValue(destFileSo));

                oneOf(mockStore).removeObject(mockTransaction,
                        overwritePath + "/destFile");

                oneOf(mockStore).removeObject(mockTransaction, overwritePath);

                oneOf(mockStore).getStoredObject(mockTransaction,
                        sourceCollectionPath);
                will(returnValue(sourceCollectionSo));

                oneOf(mockStore).createFolder(mockTransaction, overwritePath);

                oneOf(mockReq).getHeader("Depth");
                will(returnValue(null));

                oneOf(mockStore).getChildrenNames(mockTransaction,
                        sourceCollectionPath);
                will(returnValue(sourceChildren));

                StoredObject sourceFileSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceFileSo));

                oneOf(mockStore).createResource(mockTransaction,
                        overwritePath + "/sourceFile");

                oneOf(mockStore).getResourceContent(mockTransaction,
                        sourceFilePath);
                will(returnValue(dsis));

                oneOf(mockStore).setResourceContent(mockTransaction,
                        overwritePath + "/sourceFile", dsis, null, null);

                StoredObject movedSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction,
                        overwritePath + "/sourceFile");
                will(returnValue(movedSo));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceCollectionPath);
                will(returnValue(sourceCollectionSo));

                sourceChildren = new String[]{"sourceFile"};

                oneOf(mockStore).getChildrenNames(mockTransaction, sourceCollectionPath);
                will(returnValue(sourceChildren));

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceFileSo));

                oneOf(mockStore).removeObject(mockTransaction, sourceFilePath);

                oneOf(mockStore).removeObject(mockTransaction,
                        sourceCollectionPath);
            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);
        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);
        DoMove doMove = new DoMove(resLocks, doDelete, doCopy, !readOnly);

        doMove.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testPathTraversalIsRejected() {
        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));
                oneOf(mockReq).getPathInfo();
                will(returnValue("/safe/../../etc/passwd"));
            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);
        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);
        DoMove doMove = new DoMove(resLocks, doDelete, doCopy, !readOnly);
        assertThrows(PathTraversalException.class, () -> doMove.execute(mockTransaction, mockReq, mockRes));
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

        DoMove doMove(boolean readOnly) {
            DoDelete doDelete = new DoDelete(store, locks, readOnly);
            DoCopy doCopy = new DoCopy(store, locks, doDelete, readOnly);
            return new DoMove(locks, doDelete, doCopy, readOnly);
        }

        void allowRequest(String sourcePath, String destinationHeader, String depth) {
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
                    will(returnValue(""));
                    allowing(req).getServletPath();
                    will(returnValue(""));
                    allowing(req).getHeader("If");
                    will(returnValue(null));
                    allowing(req).getHeader("Overwrite");
                    will(returnValue(null));
                    allowing(req).getHeader("Depth");
                    will(returnValue(depth));
                    allowing(req).getRequestURI();
                    will(returnValue("/requestUri"));
                }
            });
        }

        void allowRequest(String sourcePath, String destinationHeader) {
            allowRequest(sourcePath, destinationHeader, null);
        }
    }

    @Test
    public void testMoveIfReadOnlyIsForbidden() throws Exception {
        Fixture f = new Fixture();
        f.allowRequest(SOURCE, "/target");
        f.context.checking(new Expectations() {
            {
                oneOf(f.resp).sendError(WebdavStatus.SC_FORBIDDEN);
            }
        });

        f.doMove(true).execute(f.tx, f.req, f.resp);

        f.context.assertIsSatisfied();
    }

    @Test
    public void testMoveWithoutDestinationHeaderIsBadRequest() throws Exception {
        Fixture f = new Fixture();
        f.allowRequest(SOURCE, null);
        f.context.checking(new Expectations() {
            {
                oneOf(f.resp).sendError(WebdavStatus.SC_BAD_REQUEST);
            }
        });

        f.doMove(false).execute(f.tx, f.req, f.resp);

        f.context.assertIsSatisfied();
    }

    @Test
    public void testMoveIfSourceIsLockedWithoutToken() throws Exception {
        Fixture f = new Fixture();
        f.allowRequest(SOURCE, "/target");
        f.locks.lock(f.tx, SOURCE, "owner", true, 0, TEMP_TIMEOUT, !TEMPORARY);
        f.context.checking(new Expectations() {
            {
                oneOf(f.resp).setStatus(WebdavStatus.SC_LOCKED);
            }
        });

        f.doMove(false).execute(f.tx, f.req, f.resp);

        f.context.assertIsSatisfied();
    }

    @Test
    public void testMoveIfDestinationIsLockedWithoutToken() throws Exception {
        Fixture f = new Fixture();
        f.allowRequest(SOURCE, "/target");
        f.locks.lock(f.tx, "/target", "owner", true, 0, TEMP_TIMEOUT, !TEMPORARY);
        f.context.checking(new Expectations() {
            {
                oneOf(f.resp).setStatus(WebdavStatus.SC_LOCKED);
            }
        });

        f.doMove(false).execute(f.tx, f.req, f.resp);

        f.context.assertIsSatisfied();
    }

    @Test
    public void testMoveIfDestinationGivenAsAbsoluteUrlIsLockedWithoutToken() throws Exception {
        // DoMove checks the lock on the raw Destination header value, which does not match the lock path
        // for an absolute URL; the parsed destination is then rejected by DoCopy.copyResource instead.
        Fixture f = new Fixture();
        f.allowRequest(SOURCE, "http://host/target");
        f.locks.lock(f.tx, "/target", "owner", true, 0, TEMP_TIMEOUT, !TEMPORARY);
        f.context.checking(new Expectations() {
            {
                oneOf(f.resp).setStatus(WebdavStatus.SC_LOCKED);
                never(f.store).removeObject(f.tx, SOURCE);
            }
        });

        f.doMove(false).execute(f.tx, f.req, f.resp);

        f.context.assertIsSatisfied();
    }

    @Test
    public void testMoveIfSourceIsTemporarilyLockedExclusively() throws Exception {
        Fixture f = new Fixture();
        f.allowRequest(SOURCE, "/target");
        f.locks.lock(f.tx, SOURCE, "other", true, 0, TEMP_TIMEOUT, TEMPORARY);
        f.context.checking(new Expectations() {
            {
                // single-entry error report keyed on the Destination header
                oneOf(f.resp).sendError(WebdavStatus.SC_LOCKED, "Locked");
            }
        });

        f.doMove(false).execute(f.tx, f.req, f.resp);

        f.context.assertIsSatisfied();
    }

    static Stream<Arguments> storeExceptions() {
        return Stream.of(
                Arguments.of((Supplier<WebdavException>) () -> new AccessDeniedException(null), WebdavStatus.SC_FORBIDDEN, false),
                // ObjectAlreadyExistsException is mapped to 404 (not 409/412) by DoMove
                Arguments.of((Supplier<WebdavException>) () -> new ObjectAlreadyExistsException(null), WebdavStatus.SC_NOT_FOUND, true),
                // ObjectNotFoundException has no dedicated catch in DoMove and falls through to 500
                Arguments.of((Supplier<WebdavException>) () -> new ObjectNotFoundException(null),
                        WebdavStatus.SC_INTERNAL_SERVER_ERROR, false),
                Arguments.of((Supplier<WebdavException>) () -> new WebdavException("boom"),
                        WebdavStatus.SC_INTERNAL_SERVER_ERROR, false)
        );
    }

    @ParameterizedTest
    @MethodSource("storeExceptions")
    public void testStoreExceptionDuringMoveIsMappedToStatus(
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

        f.doMove(false).execute(f.tx, f.req, f.resp);

        f.context.assertIsSatisfied();
    }

    @Test
    public void testMoveDoesNotDeleteSourceWhenCopyFails() throws Exception {
        Fixture f = new Fixture();
        f.allowRequest(SOURCE, "/target");
        f.context.checking(new Expectations() {
            {
                oneOf(f.store).getStoredObject(f.tx, SOURCE);
                will(returnValue(null));
                oneOf(f.resp).sendError(WebdavStatus.SC_NOT_FOUND);
                never(f.store).removeObject(f.tx, SOURCE);
            }
        });

        f.doMove(false).execute(f.tx, f.req, f.resp);

        f.context.assertIsSatisfied();
    }

    @Test
    public void testMoveOfFolderWithDepthZeroDeletesUncopiedChildren() throws Exception {
        // Characterization (candidate bug): with "Depth: 0" DoCopy only creates the destination folder,
        // but DoDelete ignores Depth and removes the whole source tree, so children are lost.
        Fixture f = new Fixture();
        f.allowRequest("/folder", "/target", "0");
        f.context.checking(new Expectations() {
            {
                allowing(f.store).getStoredObject(f.tx, "/folder");
                will(returnValue(initFolderStoredObject()));
                oneOf(f.store).getStoredObject(f.tx, "/target");
                will(returnValue(null));
                oneOf(f.resp).setStatus(WebdavStatus.SC_CREATED);
                oneOf(f.store).createFolder(f.tx, "/target");
                never(f.store).createResource(f.tx, "/target/child");

                oneOf(f.resp).setStatus(WebdavStatus.SC_NO_CONTENT);
                oneOf(f.store).getChildrenNames(f.tx, "/folder");
                will(returnValue(new String[]{"child"}));
                oneOf(f.store).getStoredObject(f.tx, "/folder/child");
                will(returnValue(initFileStoredObject(resourceContent)));
                oneOf(f.store).removeObject(f.tx, "/folder/child");
                oneOf(f.store).removeObject(f.tx, "/folder");
            }
        });

        f.doMove(false).execute(f.tx, f.req, f.resp);

        f.context.assertIsSatisfied();
    }

    @Test
    public void testMoveReportsSourceChildDeleteFailuresInMultiStatus() throws Exception {
        Fixture f = new Fixture();
        // Depth 0 keeps the copy step trivial so that only the delete step touches the children
        f.allowRequest("/folder", "/target", "0");
        StringWriter body = new StringWriter();
        f.context.checking(new Expectations() {
            {
                allowing(f.store).getStoredObject(f.tx, "/folder");
                will(returnValue(initFolderStoredObject()));
                oneOf(f.store).getStoredObject(f.tx, "/target");
                will(returnValue(null));
                oneOf(f.resp).setStatus(WebdavStatus.SC_CREATED);
                oneOf(f.store).createFolder(f.tx, "/target");

                oneOf(f.resp).setStatus(WebdavStatus.SC_NO_CONTENT);
                oneOf(f.store).getChildrenNames(f.tx, "/folder");
                will(returnValue(new String[]{"a", "b"}));
                oneOf(f.store).getStoredObject(f.tx, "/folder/a");
                will(throwException(new AccessDeniedException(null)));
                oneOf(f.store).getStoredObject(f.tx, "/folder/b");
                will(throwException(new WebdavException("boom")));
                oneOf(f.store).removeObject(f.tx, "/folder");

                oneOf(f.resp).setStatus(WebdavStatus.SC_MULTI_STATUS);
                oneOf(f.resp).setContentType("text/xml; charset=UTF-8");
                oneOf(f.resp).getWriter();
                will(returnValue(new PrintWriter(body)));
            }
        });

        f.doMove(false).execute(f.tx, f.req, f.resp);

        f.context.assertIsSatisfied();
        String xml = body.toString();
        assertTrue(xml.contains("/folder/a"), xml);
        assertTrue(xml.contains("HTTP/1.1 403 Forbidden"), xml);
        assertTrue(xml.contains("/folder/b"), xml);
        assertTrue(xml.contains("HTTP/1.1 500 Internal Server Error"), xml);
    }
}
