/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.webdav.methods;

import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import nl.info.webdav.IMimeTyper;
import nl.info.webdav.ITransaction;
import nl.info.webdav.IWebdavStore;
import nl.info.webdav.StoredObject;
import nl.info.webdav.WebdavStatus;
import nl.info.webdav.exceptions.AccessDeniedException;
import nl.info.webdav.exceptions.WebdavException;
import nl.info.webdav.locking.ResourceLocks;
import nl.info.webdav.testutil.MockTest;

public class DoHeadTest extends MockTest {

    static IWebdavStore mockStore;
    static IMimeTyper mockMimeTyper;
    static HttpServletRequest mockReq;
    static HttpServletResponse mockRes;
    static TestingOutputStream tos;
    static ITransaction mockTransaction;
    static byte[] resourceContent = new byte[]{'<', 'h', 'e', 'l', 'l', 'o',
                                               '/', '>'};

    @BeforeAll
    public static void setUp() throws Exception {
        mockStore = _mockery.mock(IWebdavStore.class);
        mockMimeTyper = _mockery.mock(IMimeTyper.class);
        mockReq = _mockery.mock(HttpServletRequest.class);
        mockRes = _mockery.mock(HttpServletResponse.class);
        tos = new TestingOutputStream();
        mockTransaction = _mockery.mock(ITransaction.class);
    }

    @Test
    public void testAccessOfaMissingPageResultsIn404() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/index.html"));

                StoredObject indexSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, "/index.html");
                will(returnValue(indexSo));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NOT_FOUND);
            }
        });

        DoHead doHead = new DoHead(mockStore, null, null, new ResourceLocks(),
                mockMimeTyper, 0);
        doHead.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testAccessOfaPageResultsInPage() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/index.html"));

                StoredObject indexSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, "/index.html");
                will(returnValue(indexSo));

                oneOf(mockReq).getHeader("If-None-Match");
                will(returnValue(null));

                oneOf(mockRes).setDateHeader("last-modified",
                        indexSo.getLastModified().getTime());

                oneOf(mockRes).addHeader(with(any(String.class)),
                        with(any(String.class)));

                oneOf(mockMimeTyper).getMimeType(mockTransaction, "/index.html");
                will(returnValue("text/foo"));

                oneOf(mockRes).setContentType("text/foo");
            }
        });

        DoHead doHead = new DoHead(mockStore, null, null, new ResourceLocks(),
                mockMimeTyper, 0);

        doHead.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testAccessOfaDirectoryResultsInRedirectIfDefaultIndexFilePresent()
                                                                                   throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/foo/"));

                StoredObject fooSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, "/foo/");
                will(returnValue(fooSo));

                oneOf(mockReq).getRequestURI();
                will(returnValue("/foo/"));

                oneOf(mockRes).encodeRedirectURL("/foo//indexFile");

                oneOf(mockRes).sendRedirect("");
            }
        });

        DoHead doHead = new DoHead(mockStore, "/indexFile", null,
                new ResourceLocks(), mockMimeTyper, 0);

        doHead.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testIfNoneMatchMatchingETagResultsIn304() throws Exception {

        final StoredObject indexSo = initFileStoredObject(resourceContent);
        final String eTag = "W/\"" + indexSo.getResourceLength() + "-" + indexSo.getLastModified().getTime() + "\"";

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/index.html"));

                oneOf(mockStore).getStoredObject(mockTransaction, "/index.html");
                will(returnValue(indexSo));

                oneOf(mockReq).getHeader("If-None-Match");
                will(returnValue(eTag));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NOT_MODIFIED);
            }
        });

        DoHead doHead = new DoHead(mockStore, null, null, new ResourceLocks(),
                mockMimeTyper, 0);
        doHead.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testContentLengthHeaderIsSetWhenEnabled() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/index.html"));

                StoredObject indexSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, "/index.html");
                will(returnValue(indexSo));

                oneOf(mockReq).getHeader("If-None-Match");
                will(returnValue(null));

                oneOf(mockRes).setDateHeader("last-modified",
                        indexSo.getLastModified().getTime());

                oneOf(mockRes).addHeader(with("ETag"), with(any(String.class)));

                oneOf(mockRes).setContentLength(resourceContent.length);

                oneOf(mockMimeTyper).getMimeType(mockTransaction, "/index.html");
                will(returnValue("text/foo"));

                oneOf(mockRes).setContentType("text/foo");
            }
        });

        // contentLengthHeader == 1 enables the Content-Length header
        DoHead doHead = new DoHead(mockStore, null, null, new ResourceLocks(),
                mockMimeTyper, 1);
        doHead.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testContentLengthHeaderIsSuppressedWhenDisabled() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/index.html"));

                StoredObject indexSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, "/index.html");
                will(returnValue(indexSo));

                oneOf(mockReq).getHeader("If-None-Match");
                will(returnValue(null));

                oneOf(mockRes).setDateHeader("last-modified",
                        indexSo.getLastModified().getTime());

                oneOf(mockRes).addHeader(with("ETag"), with(any(String.class)));

                never(mockRes).setContentLength(with(any(int.class)));
                never(mockRes).setHeader(with("content-length"), with(any(String.class)));

                oneOf(mockMimeTyper).getMimeType(mockTransaction, "/index.html");
                will(returnValue("text/foo"));

                oneOf(mockRes).setContentType("text/foo");
            }
        });

        DoHead doHead = new DoHead(mockStore, null, null, new ResourceLocks(),
                mockMimeTyper, 0);
        doHead.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testContentLengthHeaderForResourceLargerThanIntegerMaxValue() throws Exception {

        final long hugeLength = Integer.MAX_VALUE + 1L;

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/huge.bin"));

                StoredObject hugeSo = initFileStoredObject(resourceContent);
                hugeSo.setResourceLength(hugeLength);

                oneOf(mockStore).getStoredObject(mockTransaction, "/huge.bin");
                will(returnValue(hugeSo));

                oneOf(mockReq).getHeader("If-None-Match");
                will(returnValue(null));

                oneOf(mockRes).setDateHeader("last-modified",
                        hugeSo.getLastModified().getTime());

                oneOf(mockRes).addHeader(with("ETag"), with(any(String.class)));

                oneOf(mockRes).setHeader("content-length", Long.toString(hugeLength));

                oneOf(mockMimeTyper).getMimeType(mockTransaction, "/huge.bin");
                will(returnValue("application/octet-stream"));

                oneOf(mockRes).setContentType("application/octet-stream");
            }
        });

        DoHead doHead = new DoHead(mockStore, null, null, new ResourceLocks(),
                mockMimeTyper, 1);
        doHead.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testUnknownMimeTypeWithoutExtensionDefaultsToTextHtml() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/folder.d/readme"));

                StoredObject so = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, "/folder.d/readme");
                will(returnValue(so));

                oneOf(mockReq).getHeader("If-None-Match");
                will(returnValue(null));

                oneOf(mockRes).setDateHeader("last-modified",
                        so.getLastModified().getTime());

                oneOf(mockRes).addHeader(with("ETag"), with(any(String.class)));

                oneOf(mockMimeTyper).getMimeType(mockTransaction, "/folder.d/readme");
                will(returnValue(null));

                oneOf(mockRes).setContentType("text/html");
            }
        });

        DoHead doHead = new DoHead(mockStore, null, null, new ResourceLocks(),
                mockMimeTyper, 0);
        doHead.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testUnknownMimeTypeWithExtensionSetsNoContentType() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/file.unknown"));

                StoredObject so = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, "/file.unknown");
                will(returnValue(so));

                oneOf(mockReq).getHeader("If-None-Match");
                will(returnValue(null));

                oneOf(mockRes).setDateHeader("last-modified",
                        so.getLastModified().getTime());

                oneOf(mockRes).addHeader(with("ETag"), with(any(String.class)));

                oneOf(mockMimeTyper).getMimeType(mockTransaction, "/file.unknown");
                will(returnValue(null));

                never(mockRes).setContentType(with(any(String.class)));
            }
        });

        DoHead doHead = new DoHead(mockStore, null, null, new ResourceLocks(),
                mockMimeTyper, 0);
        doHead.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testAccessDeniedResultsIn403() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/secret.html"));

                oneOf(mockStore).getStoredObject(mockTransaction, "/secret.html");
                will(throwException(new AccessDeniedException(null)));

                oneOf(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
            }
        });

        DoHead doHead = new DoHead(mockStore, null, null, new ResourceLocks(),
                mockMimeTyper, 0);
        doHead.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testWebdavExceptionDuringLookupIsNotHandled() {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/broken.html"));

                oneOf(mockStore).getStoredObject(mockTransaction, "/broken.html");
                will(throwException(new WebdavException("store failure")));
            }
        });

        DoHead doHead = new DoHead(mockStore, null, null, new ResourceLocks(),
                mockMimeTyper, 0);
        // only AccessDeniedException is mapped during the initial lookup; other store errors propagate
        assertThrows(WebdavException.class, () -> doHead.execute(mockTransaction, mockReq, mockRes));

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testHeadOnFolderWithoutIndexFileProducesNoBody() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/foo/"));

                StoredObject fooSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, "/foo/");
                will(returnValue(fooSo));

                oneOf(mockReq).getHeader("If-None-Match");
                will(returnValue(null));

                never(mockRes).getOutputStream();
                never(mockRes).setStatus(with(any(int.class)));
                never(mockRes).sendError(with(any(int.class)));
            }
        });

        DoHead doHead = new DoHead(mockStore, null, null, new ResourceLocks(),
                mockMimeTyper, 0);
        doHead.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testHeadOnLockNullResourceResultsIn405() throws Exception {

        final StoredObject lockNullSo = initLockNullStoredObject();

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/locknull"));

                oneOf(mockStore).getStoredObject(mockTransaction, "/locknull");
                will(returnValue(lockNullSo));

                oneOf(mockRes).addHeader("Allow", DeterminableMethod.determineMethodsAllowed(lockNullSo));

                oneOf(mockRes).sendError(WebdavStatus.SC_METHOD_NOT_ALLOWED);
            }
        });

        DoHead doHead = new DoHead(mockStore, null, null, new ResourceLocks(),
                mockMimeTyper, 0);
        doHead.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testResourcePathWithTrailingSlashResultsIn404() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/index.html/"));

                StoredObject so = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, "/index.html/");
                will(returnValue(so));

                oneOf(mockReq).getHeader("If-None-Match");
                will(returnValue(null));

                oneOf(mockReq).getRequestURI();
                will(returnValue("/index.html/"));

                oneOf(mockRes).sendError(WebdavStatus.SC_NOT_FOUND, "/index.html/");
            }
        });

        DoHead doHead = new DoHead(mockStore, null, null, new ResourceLocks(),
                mockMimeTyper, 0);
        doHead.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testTemporaryLockFailureResultsIn500() throws Exception {

        final ResourceLocks resourceLocks = new ResourceLocks();
        // an exclusive temporary lock held by someone else prevents the temporary read lock
        resourceLocks.lock(mockTransaction, "/index.html", "someone else", true, 0, 100, true);

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/index.html"));

                StoredObject so = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, "/index.html");
                will(returnValue(so));

                oneOf(mockRes).sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
            }
        });

        DoHead doHead = new DoHead(mockStore, null, null, resourceLocks,
                mockMimeTyper, 0);
        doHead.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

}
