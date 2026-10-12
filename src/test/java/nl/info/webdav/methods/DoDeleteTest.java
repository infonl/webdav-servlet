/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.webdav.methods;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Hashtable;
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
import nl.info.webdav.locking.LockedObject;
import nl.info.webdav.locking.ResourceLocks;
import nl.info.webdav.testutil.MockTest;

public class DoDeleteTest extends MockTest {

    static IWebdavStore mockStore;
    static HttpServletRequest mockReq;
    static HttpServletResponse mockRes;
    static ITransaction mockTransaction;
    static byte[] resourceContent = new byte[]{'<', 'h', 'e', 'l', 'l', 'o',
                                               '/', '>'};

    @BeforeAll
    public static void setUp() throws Exception {
        mockStore = _mockery.mock(IWebdavStore.class);
        mockReq = _mockery.mock(HttpServletRequest.class);
        mockRes = _mockery.mock(HttpServletResponse.class);
        mockTransaction = _mockery.mock(ITransaction.class);
    }

    @Test
    public void testDeleteIfReadOnlyIsTrue() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoDelete doDelete = new DoDelete(mockStore, resLocks, readOnly);
        doDelete.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDeleteFileIfObjectExists() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                StoredObject fileSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(fileSo));

                oneOf(mockStore).removeObject(mockTransaction, sourceFilePath);
            }
        });

        DoDelete doDelete = new DoDelete(mockStore, new ResourceLocks(),
                !readOnly);

        doDelete.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDeleteFileIfObjectNotExists() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                StoredObject fileSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(fileSo));

                oneOf(mockRes).sendError(WebdavStatus.SC_NOT_FOUND);
            }
        });

        DoDelete doDelete = new DoDelete(mockStore, new ResourceLocks(),
                !readOnly);

        doDelete.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDeleteFolderIfObjectExists() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceCollectionPath));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                StoredObject folderSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction,
                        sourceCollectionPath);
                will(returnValue(folderSo));

                oneOf(mockStore).getChildrenNames(mockTransaction,
                        sourceCollectionPath);
                will(returnValue(new String[]{"subFolder", "sourceFile"}));

                StoredObject fileSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(fileSo));

                oneOf(mockStore).removeObject(mockTransaction, sourceFilePath);

                StoredObject subFolderSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction,
                        sourceCollectionPath + "/subFolder");
                will(returnValue(subFolderSo));

                oneOf(mockStore).getChildrenNames(mockTransaction,
                        sourceCollectionPath + "/subFolder");
                will(returnValue(new String[]{"fileInSubFolder"}));

                StoredObject fileInSubFolderSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction,
                        sourceCollectionPath + "/subFolder/fileInSubFolder");
                will(returnValue(fileInSubFolderSo));

                oneOf(mockStore).removeObject(mockTransaction,
                        sourceCollectionPath + "/subFolder/fileInSubFolder");

                oneOf(mockStore).removeObject(mockTransaction,
                        sourceCollectionPath + "/subFolder");

                oneOf(mockStore).removeObject(mockTransaction,
                        sourceCollectionPath);
            }
        });

        DoDelete doDelete = new DoDelete(mockStore, new ResourceLocks(),
                !readOnly);

        doDelete.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDeleteFolderIfObjectNotExists() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceCollectionPath));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                StoredObject folderSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction,
                        sourceCollectionPath);
                will(returnValue(folderSo));

                oneOf(mockRes).sendError(WebdavStatus.SC_NOT_FOUND);
            }
        });

        DoDelete doDelete = new DoDelete(mockStore, new ResourceLocks(),
                !readOnly);

        doDelete.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDeleteFileInFolder() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                StoredObject fileSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(fileSo));

                oneOf(mockStore).removeObject(mockTransaction, sourceFilePath);
            }
        });

        DoDelete doDelete = new DoDelete(mockStore, new ResourceLocks(),
                !readOnly);

        doDelete.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDeleteFileInLockedFolderWithWrongLockToken()
                                                                 throws Exception {

        final String lockedFolderPath = "/lockedFolder";
        final String fileInLockedFolderPath = lockedFolderPath
                .concat("/fileInLockedFolder");

        String owner = new String("owner");
        ResourceLocks resLocks = new ResourceLocks();

        resLocks.lock(mockTransaction, lockedFolderPath, owner, true, -1,
                TEMP_TIMEOUT, !TEMPORARY);
        LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction,
                lockedFolderPath);
        final String wrongLockToken = "(<opaquelocktoken:" + lo.getID() + "WRONG>)";

        final PrintWriter pw = new PrintWriter("/tmp/XMLTestFile");

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(fileInLockedFolderPath));

                oneOf(mockReq).getHeader("If");
                will(returnValue(wrongLockToken));

                oneOf(mockRes).setStatus(WebdavStatus.SC_LOCKED);
            }
        });

        DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);

        doDelete.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDeleteFileInLockedFolderWithRightLockToken()
                                                                 throws Exception {

        final String path = "/lockedFolder/fileInLockedFolder";
        final String parentPath = "/lockedFolder";
        final String owner = new String("owner");
        ResourceLocks resLocks = new ResourceLocks();

        resLocks.lock(mockTransaction, parentPath, owner, true, -1,
                TEMP_TIMEOUT, !TEMPORARY);
        LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction,
                "/lockedFolder");
        final String rightLockToken = "(<opaquelocktoken:" + lo.getID() + ">)";

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getHeader("If");
                will(returnValue(rightLockToken));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                StoredObject so = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(so));

                oneOf(mockStore).removeObject(mockTransaction, path);

            }
        });

        DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);

        doDelete.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDeleteFileInFolderIfObjectNotExists() throws Exception {

        boolean readOnly = false;

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/folder/file"));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                StoredObject nonExistingSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, "/folder/file");
                will(returnValue(nonExistingSo));

                oneOf(mockRes).sendError(WebdavStatus.SC_NOT_FOUND);
            }
        });

        DoDelete doDelete = new DoDelete(mockStore, new ResourceLocks(),
                readOnly);

        doDelete.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testPathTraversalIsRejected() {
        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));
                oneOf(mockReq).getPathInfo();
                will(returnValue("/../secret"));
            }
        });

        DoDelete doDelete = new DoDelete(mockStore, new ResourceLocks(), !readOnly);
        assertThrows(PathTraversalException.class, () -> doDelete.execute(mockTransaction, mockReq, mockRes));
        _mockery.assertIsSatisfied();
    }

    // ---------------------------------------------------------------------
    // The tests below use a fresh Mockery per test (see Fixture) so that
    // 'allowing' expectations on request getters do not leak into other tests.
    // ---------------------------------------------------------------------

    private static final String FOLDER = "/folder";

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

        DoDelete doDelete(boolean readOnly) {
            return new DoDelete(store, locks, readOnly);
        }

        void allowRequest(String path) {
            context.checking(new Expectations() {
                {
                    allowing(req).getAttribute("javax.servlet.include.request_uri");
                    will(returnValue(null));
                    allowing(req).getPathInfo();
                    will(returnValue(path));
                    allowing(req).getHeader("If");
                    will(returnValue(null));
                    allowing(req).getRequestURI();
                    will(returnValue("/requestUri"));
                }
            });
        }
    }

    @Test
    public void testDeleteResourceIfReadOnlyIsForbidden() throws Exception {
        Fixture f = new Fixture();
        f.context.checking(new Expectations() {
            {
                oneOf(f.resp).setStatus(WebdavStatus.SC_NO_CONTENT);
                oneOf(f.resp).sendError(WebdavStatus.SC_FORBIDDEN);
            }
        });

        f.doDelete(true).deleteResource(f.tx, FOLDER, new Hashtable<>(), f.resp);

        f.context.assertIsSatisfied();
    }

    @Test
    public void testDeleteIfResourceIsLockedWithoutToken() throws Exception {
        Fixture f = new Fixture();
        f.allowRequest(FOLDER);
        f.locks.lock(f.tx, FOLDER, "owner", true, 0, TEMP_TIMEOUT, !TEMPORARY);
        f.context.checking(new Expectations() {
            {
                oneOf(f.resp).setStatus(WebdavStatus.SC_LOCKED);
            }
        });

        f.doDelete(false).execute(f.tx, f.req, f.resp);

        f.context.assertIsSatisfied();
    }

    @Test
    public void testDeleteIfParentIsLockedWithoutToken() throws Exception {
        Fixture f = new Fixture();
        f.allowRequest(FOLDER + "/file");
        f.locks.lock(f.tx, FOLDER, "owner", true, 0, TEMP_TIMEOUT, !TEMPORARY);
        f.context.checking(new Expectations() {
            {
                oneOf(f.resp).setStatus(WebdavStatus.SC_LOCKED);
            }
        });

        f.doDelete(false).execute(f.tx, f.req, f.resp);

        f.context.assertIsSatisfied();
    }

    @Test
    public void testDeleteIfResourceIsSharedLockedIsAllowed() throws Exception {
        Fixture f = new Fixture();
        f.allowRequest(FOLDER + "/file");
        f.locks.lock(f.tx, FOLDER + "/file", "owner", false, 0, TEMP_TIMEOUT, !TEMPORARY);
        f.context.checking(new Expectations() {
            {
                oneOf(f.resp).setStatus(WebdavStatus.SC_NO_CONTENT);
                oneOf(f.store).getStoredObject(f.tx, FOLDER + "/file");
                will(returnValue(initFileStoredObject(resourceContent)));
                oneOf(f.store).removeObject(f.tx, FOLDER + "/file");
            }
        });

        f.doDelete(false).execute(f.tx, f.req, f.resp);

        f.context.assertIsSatisfied();
    }

    @Test
    public void testDeleteIfResourceIsTemporarilyLockedExclusively() throws Exception {
        Fixture f = new Fixture();
        f.allowRequest(FOLDER);
        f.locks.lock(f.tx, FOLDER, "other", true, 0, TEMP_TIMEOUT, TEMPORARY);
        f.context.checking(new Expectations() {
            {
                oneOf(f.resp).sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
            }
        });

        f.doDelete(false).execute(f.tx, f.req, f.resp);

        f.context.assertIsSatisfied();
    }

    @Test
    public void testDeleteOfLockNullResourceRemovesIt() throws Exception {
        // StoredObject.isResource() is !isFolder(), so a lock-null resource is treated as a resource
        // and the "neither resource nor folder" 404 branch in deleteResource is unreachable.
        Fixture f = new Fixture();
        f.allowRequest(FOLDER + "/null");
        f.context.checking(new Expectations() {
            {
                oneOf(f.resp).setStatus(WebdavStatus.SC_NO_CONTENT);
                oneOf(f.store).getStoredObject(f.tx, FOLDER + "/null");
                will(returnValue(initLockNullStoredObject()));
                oneOf(f.store).removeObject(f.tx, FOLDER + "/null");
            }
        });

        f.doDelete(false).execute(f.tx, f.req, f.resp);

        f.context.assertIsSatisfied();
    }

    @Test
    public void testDeleteOfEmptyFolderWithNullChildren() throws Exception {
        Fixture f = new Fixture();
        f.allowRequest(FOLDER);
        f.context.checking(new Expectations() {
            {
                oneOf(f.resp).setStatus(WebdavStatus.SC_NO_CONTENT);
                oneOf(f.store).getStoredObject(f.tx, FOLDER);
                will(returnValue(initFolderStoredObject()));
                oneOf(f.store).getChildrenNames(f.tx, FOLDER);
                will(returnValue(null));
                oneOf(f.store).removeObject(f.tx, FOLDER);
            }
        });

        f.doDelete(false).execute(f.tx, f.req, f.resp);

        f.context.assertIsSatisfied();
    }

    static Stream<Arguments> childDeleteExceptions() {
        return Stream.of(
                Arguments.of((Supplier<WebdavException>) () -> new AccessDeniedException(null), WebdavStatus.SC_FORBIDDEN),
                Arguments.of((Supplier<WebdavException>) () -> new ObjectNotFoundException(null), WebdavStatus.SC_NOT_FOUND),
                Arguments.of((Supplier<WebdavException>) () -> new WebdavException("boom"),
                        WebdavStatus.SC_INTERNAL_SERVER_ERROR)
        );
    }

    @ParameterizedTest
    @MethodSource("childDeleteExceptions")
    public void testChildDeleteFailuresAreReportedInMultiStatus(
            Supplier<WebdavException> exception,
            int expectedStatus
    ) throws Exception {
        Fixture f = new Fixture();
        f.allowRequest(FOLDER);
        StringWriter body = new StringWriter();
        f.context.checking(new Expectations() {
            {
                oneOf(f.resp).setStatus(WebdavStatus.SC_NO_CONTENT);
                oneOf(f.store).getStoredObject(f.tx, FOLDER);
                will(returnValue(initFolderStoredObject()));
                oneOf(f.store).getChildrenNames(f.tx, FOLDER);
                will(returnValue(new String[]{"a", "b"}));
                oneOf(f.store).getStoredObject(f.tx, FOLDER + "/a");
                will(returnValue(initFileStoredObject(resourceContent)));
                oneOf(f.store).removeObject(f.tx, FOLDER + "/a");
                will(throwException(exception.get()));
                oneOf(f.store).getStoredObject(f.tx, FOLDER + "/b");
                will(returnValue(initFolderStoredObject()));
                oneOf(f.store).getChildrenNames(f.tx, FOLDER + "/b");
                will(returnValue(new String[0]));
                oneOf(f.store).removeObject(f.tx, FOLDER + "/b");
                will(throwException(exception.get()));
                // the folder itself is still removed, despite the child failures
                oneOf(f.store).removeObject(f.tx, FOLDER);

                oneOf(f.resp).setStatus(WebdavStatus.SC_MULTI_STATUS);
                oneOf(f.resp).setContentType("text/xml; charset=UTF-8");
                oneOf(f.resp).getWriter();
                will(returnValue(new PrintWriter(body)));
            }
        });

        f.doDelete(false).execute(f.tx, f.req, f.resp);

        f.context.assertIsSatisfied();
        String xml = body.toString();
        assertTrue(xml.contains("multistatus"), xml);
        assertTrue(xml.contains(FOLDER + "/a"), xml);
        assertTrue(xml.contains(FOLDER + "/b"), xml);
        assertTrue(xml.contains("HTTP/1.1 " + expectedStatus + " " + WebdavStatus.getStatusText(expectedStatus)), xml);
    }

    @Test
    public void testSingleChildDeleteFailureIsSentAsPlainError() throws Exception {
        Fixture f = new Fixture();
        f.allowRequest(FOLDER);
        f.context.checking(new Expectations() {
            {
                oneOf(f.resp).setStatus(WebdavStatus.SC_NO_CONTENT);
                oneOf(f.store).getStoredObject(f.tx, FOLDER);
                will(returnValue(initFolderStoredObject()));
                oneOf(f.store).getChildrenNames(f.tx, FOLDER);
                will(returnValue(new String[]{"a"}));
                oneOf(f.store).getStoredObject(f.tx, FOLDER + "/a");
                will(throwException(new AccessDeniedException(null)));
                oneOf(f.store).removeObject(f.tx, FOLDER);
                oneOf(f.resp).sendError(WebdavStatus.SC_FORBIDDEN, "Forbidden");
            }
        });

        f.doDelete(false).execute(f.tx, f.req, f.resp);

        f.context.assertIsSatisfied();
    }

    static Stream<Arguments> rootDeleteExceptions() {
        return Stream.of(
                Arguments.of((Supplier<WebdavException>) () -> new AccessDeniedException(null), WebdavStatus.SC_FORBIDDEN, false),
                // ObjectAlreadyExistsException is mapped to 404 by DoDelete
                Arguments.of((Supplier<WebdavException>) () -> new ObjectAlreadyExistsException(null), WebdavStatus.SC_NOT_FOUND, true),
                // ObjectNotFoundException has no dedicated catch in DoDelete.execute and falls through to 500
                Arguments.of((Supplier<WebdavException>) () -> new ObjectNotFoundException(null),
                        WebdavStatus.SC_INTERNAL_SERVER_ERROR, false),
                Arguments.of((Supplier<WebdavException>) () -> new WebdavException("boom"),
                        WebdavStatus.SC_INTERNAL_SERVER_ERROR, false)
        );
    }

    @ParameterizedTest
    @MethodSource("rootDeleteExceptions")
    public void testStoreFailureOnRootDeleteIsMappedToStatus(
            Supplier<WebdavException> exception,
            int expectedStatus,
            boolean withRequestUri
    ) throws Exception {
        Fixture f = new Fixture();
        f.allowRequest(FOLDER + "/file");
        f.context.checking(new Expectations() {
            {
                oneOf(f.resp).setStatus(WebdavStatus.SC_NO_CONTENT);
                oneOf(f.store).getStoredObject(f.tx, FOLDER + "/file");
                will(returnValue(initFileStoredObject(resourceContent)));
                oneOf(f.store).removeObject(f.tx, FOLDER + "/file");
                will(throwException(exception.get()));
                if (withRequestUri) {
                    oneOf(f.resp).sendError(expectedStatus, "/requestUri");
                } else {
                    oneOf(f.resp).sendError(expectedStatus);
                }
            }
        });

        f.doDelete(false).execute(f.tx, f.req, f.resp);

        f.context.assertIsSatisfied();
    }
}
