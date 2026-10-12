/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.webdav.methods;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.Locale;
import java.util.stream.Stream;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import nl.info.webdav.IMimeTyper;
import nl.info.webdav.ITransaction;
import nl.info.webdav.IWebdavStore;
import nl.info.webdav.StoredObject;
import nl.info.webdav.WebdavStatus;
import nl.info.webdav.exceptions.AccessDeniedException;
import nl.info.webdav.exceptions.ObjectAlreadyExistsException;
import nl.info.webdav.exceptions.PathTraversalException;
import nl.info.webdav.exceptions.WebdavException;
import nl.info.webdav.locking.ResourceLocks;
import nl.info.webdav.testutil.DelegatingServletInputStream;
import nl.info.webdav.testutil.MockTest;


public class DoGetTest extends MockTest {

    static IWebdavStore mockStore;
    static IMimeTyper mockMimeTyper;
    static HttpServletRequest mockReq;
    static HttpServletResponse mockRes;
    static ITransaction mockTransaction;
    static TestingOutputStream tos = new TestingOutputStream();
    static byte[] resourceContent = new byte[]{'<', 'h', 'e', 'l', 'l', 'o',
                                               '/', '>'};
    static ByteArrayInputStream bais = new ByteArrayInputStream(resourceContent);
    static DelegatingServletInputStream dsis = new DelegatingServletInputStream(
            bais);

    @BeforeAll
    public static void setUp() throws Exception {
        mockStore = _mockery.mock(IWebdavStore.class);
        mockMimeTyper = _mockery.mock(IMimeTyper.class);
        mockReq = _mockery.mock(HttpServletRequest.class);
        mockRes = _mockery.mock(HttpServletResponse.class);
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

                exactly(2).of(mockStore).getStoredObject(mockTransaction,
                        "/index.html");
                will(returnValue(indexSo));

                oneOf(mockReq).getRequestURI();
                will(returnValue("/index.html"));

                oneOf(mockRes)
                        .sendError(WebdavStatus.SC_NOT_FOUND, "/index.html");

                oneOf(mockRes).setStatus(WebdavStatus.SC_NOT_FOUND);
            }
        });

        DoGet doGet = new DoGet(mockStore, null, null, new ResourceLocks(),
                mockMimeTyper, 0);

        doGet.execute(mockTransaction, mockReq, mockRes);

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

                StoredObject so = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, "/index.html");
                will(returnValue(so));

                oneOf(mockRes).getOutputStream();
                will(returnValue(tos));

                oneOf(mockStore).getResourceContent(mockTransaction,
                        "/index.html");
                will(returnValue(dsis));
            }
        });

        DoGet doGet = new DoGet(mockStore, null, null, new ResourceLocks(),
                mockMimeTyper, 0);

        doGet.execute(mockTransaction, mockReq, mockRes);

        assertEquals("<hello/>", tos.toString());

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testAccessOfaDirectoryResultsInRudimentaryChildList()
                                                                      throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/foo/"));

                StoredObject fooSo = initFolderStoredObject();
                StoredObject aaa = initFolderStoredObject();
                StoredObject bbb = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, "/foo/");
                will(returnValue(fooSo));

                oneOf(mockReq).getHeader("If-None-Match");
                will(returnValue(null));

                oneOf(mockStore).getStoredObject(mockTransaction, "/foo/");
                will(returnValue(fooSo));

                oneOf(mockReq).getLocale();
                will(returnValue(Locale.GERMAN));

                oneOf(mockRes).setContentType("text/html");
                oneOf(mockRes).setCharacterEncoding("UTF8");

                tos = new TestingOutputStream();

                oneOf(mockRes).getOutputStream();
                will(returnValue(tos));

                oneOf(mockStore).getChildrenNames(mockTransaction, "/foo/");
                will(returnValue(new String[]{"AAA", "BBB"}));

                oneOf(mockStore).getStoredObject(mockTransaction, "/foo//AAA");
                will(returnValue(aaa));

                oneOf(mockStore).getStoredObject(mockTransaction, "/foo//BBB");
                will(returnValue(bbb));

            }
        });

        DoGet doGet = new DoGet(mockStore, null, null, new ResourceLocks(),
                mockMimeTyper, 0);

        doGet.execute(mockTransaction, mockReq, mockRes);

        assertFalse(tos.toString().isEmpty());

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

        DoGet doGet = new DoGet(mockStore, "/indexFile", null,
                new ResourceLocks(), mockMimeTyper, 0);

        doGet.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testFolderBodyWithXssInPathIsEncoded() throws Exception {

        final TestingOutputStream xssPathTos = new TestingOutputStream();

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/<script>alert(1)</script>/"));

                StoredObject folderSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction,
                        "/<script>alert(1)</script>/");
                will(returnValue(folderSo));

                oneOf(mockReq).getHeader("If-None-Match");
                will(returnValue(null));

                oneOf(mockStore).getStoredObject(mockTransaction,
                        "/<script>alert(1)</script>/");
                will(returnValue(folderSo));

                oneOf(mockReq).getLocale();
                will(returnValue(Locale.ENGLISH));

                oneOf(mockRes).setContentType("text/html");
                oneOf(mockRes).setCharacterEncoding("UTF8");

                oneOf(mockRes).getOutputStream();
                will(returnValue(xssPathTos));

                oneOf(mockStore).getChildrenNames(mockTransaction,
                        "/<script>alert(1)</script>/");
                will(returnValue(new String[]{}));
            }
        });

        DoGet doGet = new DoGet(mockStore, null, null, new ResourceLocks(),
                mockMimeTyper, 0);

        doGet.execute(mockTransaction, mockReq, mockRes);

        String output = xssPathTos.toString();
        assertTrue(output.contains("&lt;script&gt;"), "Path in title must be HTML-encoded");
        assertFalse(output.contains("<script>"), "Raw <script> tag must not appear in output");

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testFolderBodyWithXssInChildNameIsEncoded() throws Exception {

        final TestingOutputStream xssChildTos = new TestingOutputStream();

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/safe/"));

                StoredObject folderSo = initFolderStoredObject();
                StoredObject evilChild = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, "/safe/");
                will(returnValue(folderSo));

                oneOf(mockReq).getHeader("If-None-Match");
                will(returnValue(null));

                oneOf(mockStore).getStoredObject(mockTransaction, "/safe/");
                will(returnValue(folderSo));

                oneOf(mockReq).getLocale();
                will(returnValue(Locale.ENGLISH));

                oneOf(mockRes).setContentType("text/html");
                oneOf(mockRes).setCharacterEncoding("UTF8");

                oneOf(mockRes).getOutputStream();
                will(returnValue(xssChildTos));

                oneOf(mockStore).getChildrenNames(mockTransaction, "/safe/");
                will(returnValue(new String[]{"<script>evil</script>"}));

                oneOf(mockStore).getStoredObject(mockTransaction,
                        "/safe//<script>evil</script>");
                will(returnValue(evilChild));
            }
        });

        DoGet doGet = new DoGet(mockStore, null, null, new ResourceLocks(),
                mockMimeTyper, 0);

        doGet.execute(mockTransaction, mockReq, mockRes);

        String output = xssChildTos.toString();
        assertTrue(output.contains("&lt;script&gt;"),
                "Child name in href and link text must be HTML-encoded");
        assertFalse(output.contains("<script>"), "Raw <script> tag must not appear in output");

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testAccessOfaMissingPageResultsInPossibleAlternatveTo404()
                                                                           throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/index.html"));

                StoredObject indexSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, "/index.html");
                will(returnValue(indexSo));

                StoredObject alternativeSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, "/alternative");
                will(returnValue(alternativeSo));

                oneOf(mockReq).getHeader("If-None-Match");
                will(returnValue(null));

                oneOf(mockRes).setDateHeader("last-modified",
                        alternativeSo.getLastModified().getTime());

                oneOf(mockRes).addHeader(with(any(String.class)),
                        with(any(String.class)));

                oneOf(mockMimeTyper).getMimeType(mockTransaction, "/alternative");
                will(returnValue("text/foo"));

                oneOf(mockRes).setContentType("text/foo");

                oneOf(mockStore).getStoredObject(mockTransaction, "/alternative");
                will(returnValue(alternativeSo));

                tos = new TestingOutputStream();
                tos.write(resourceContent);

                oneOf(mockRes).getOutputStream();
                will(returnValue(tos));

                oneOf(mockStore).getResourceContent(mockTransaction,
                        "/alternative");
                will(returnValue(dsis));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NOT_FOUND);
            }
        });

        DoGet doGet = new DoGet(mockStore, null, "/alternative",
                new ResourceLocks(), mockMimeTyper, 0);

        doGet.execute(mockTransaction, mockReq, mockRes);

        assertEquals("<hello/>", tos.toString());

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

        DoGet doGet = new DoGet(mockStore, null, null, new ResourceLocks(), mockMimeTyper, 0);
        assertThrows(PathTraversalException.class, () -> doGet.execute(mockTransaction, mockReq, mockRes));
        _mockery.assertIsSatisfied();
    }

    @Test
    public void testMissingPageWithMissingInsteadOf404ResultsIn404() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/index.html"));

                oneOf(mockStore).getStoredObject(mockTransaction, "/index.html");
                will(returnValue(null));

                exactly(2).of(mockStore).getStoredObject(mockTransaction, "/alternative");
                will(returnValue(null));

                oneOf(mockReq).getRequestURI();
                will(returnValue("/index.html"));

                oneOf(mockRes).sendError(WebdavStatus.SC_NOT_FOUND, "/index.html");

                oneOf(mockRes).setStatus(WebdavStatus.SC_NOT_FOUND);
            }
        });

        DoGet doGet = new DoGet(mockStore, null, "/alternative",
                new ResourceLocks(), mockMimeTyper, 0);

        doGet.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testIfNoneMatchMatchingETagResultsIn304WithoutBody() throws Exception {

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

                never(mockRes).getOutputStream();
                never(mockStore).getResourceContent(mockTransaction, "/index.html");
            }
        });

        DoGet doGet = new DoGet(mockStore, null, null, new ResourceLocks(),
                mockMimeTyper, 0);

        doGet.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testIfNoneMatchNotMatchingETagServesPage() throws Exception {

        final TestingOutputStream pageTos = new TestingOutputStream();
        final DelegatingServletInputStream pageContent = new DelegatingServletInputStream(
                new ByteArrayInputStream(resourceContent));

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/index.html"));

                StoredObject indexSo = initFileStoredObject(resourceContent);

                exactly(2).of(mockStore).getStoredObject(mockTransaction, "/index.html");
                will(returnValue(indexSo));

                oneOf(mockReq).getHeader("If-None-Match");
                will(returnValue("W/\"other-etag\""));

                oneOf(mockRes).setDateHeader("last-modified",
                        indexSo.getLastModified().getTime());

                oneOf(mockRes).addHeader(with("ETag"), with(any(String.class)));

                oneOf(mockRes).setContentLength(resourceContent.length);

                oneOf(mockMimeTyper).getMimeType(mockTransaction, "/index.html");
                will(returnValue("text/foo"));

                oneOf(mockRes).setContentType("text/foo");

                oneOf(mockRes).getOutputStream();
                will(returnValue(pageTos));

                oneOf(mockStore).getResourceContent(mockTransaction, "/index.html");
                will(returnValue(pageContent));
            }
        });

        // contentLengthHeader == 1 enables the Content-Length header
        DoGet doGet = new DoGet(mockStore, null, null, new ResourceLocks(),
                mockMimeTyper, 1);

        doGet.execute(mockTransaction, mockReq, mockRes);

        assertEquals("<hello/>", pageTos.toString());

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

        DoGet doGet = new DoGet(mockStore, null, null, new ResourceLocks(),
                mockMimeTyper, 0);

        doGet.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    static Stream<Arguments> folderListingStoreExceptions() {
        return Stream.of(
                Arguments.of(new AccessDeniedException(null), WebdavStatus.SC_FORBIDDEN),
                Arguments.of(new WebdavException("store failure"), WebdavStatus.SC_INTERNAL_SERVER_ERROR));
    }

    @ParameterizedTest
    @MethodSource("folderListingStoreExceptions")
    public void testStoreExceptionWhileListingFolderIsMappedToStatus(
            WebdavException exception,
            int expectedStatus
    ) throws Exception {

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

                oneOf(mockStore).getStoredObject(mockTransaction, "/foo/");
                will(throwException(exception));

                oneOf(mockRes).sendError(expectedStatus);
            }
        });

        DoGet doGet = new DoGet(mockStore, null, null, new ResourceLocks(),
                mockMimeTyper, 0);

        doGet.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testObjectAlreadyExistsWhileListingFolderResultsIn404() throws Exception {

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

                oneOf(mockStore).getStoredObject(mockTransaction, "/foo/");
                will(throwException(new ObjectAlreadyExistsException(null)));

                oneOf(mockReq).getRequestURI();
                will(returnValue("/foo/"));

                oneOf(mockRes).sendError(WebdavStatus.SC_NOT_FOUND, "/foo/");
            }
        });

        DoGet doGet = new DoGet(mockStore, null, null, new ResourceLocks(),
                mockMimeTyper, 0);

        doGet.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDirectoryListingEmbedsDefaultCss() throws Exception {

        final TestingOutputStream listingTos = new TestingOutputStream();

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/foo"));

                StoredObject fooSo = initFolderStoredObject();

                exactly(2).of(mockStore).getStoredObject(mockTransaction, "/foo");
                will(returnValue(fooSo));

                oneOf(mockReq).getHeader("If-None-Match");
                will(returnValue(null));

                oneOf(mockReq).getLocale();
                will(returnValue(Locale.ENGLISH));

                oneOf(mockRes).setContentType("text/html");
                oneOf(mockRes).setCharacterEncoding("UTF8");

                oneOf(mockRes).getOutputStream();
                will(returnValue(listingTos));

                oneOf(mockStore).getChildrenNames(mockTransaction, "/foo");
                will(returnValue(null));
            }
        });

        DoGet doGet = new DoGet(mockStore, null, null, new ResourceLocks(),
                mockMimeTyper, 0);

        doGet.execute(mockTransaction, mockReq, mockRes);

        String output = listingTos.toString();
        assertTrue(output.contains("<style type=\"text/css\">"), "Listing must contain a style element");
        assertTrue(output.contains("background-color: #9DACBF;"), "Default CSS must be embedded");
        assertTrue(output.contains("tr.even"), "Default CSS must be embedded");
        assertTrue(output.contains("<h1>Content of folder /foo</h1>"), "Listing must contain the header");
        assertTrue(output.endsWith("</table></body></html>"));

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDirectoryListingShowsFileSizeAndUnknownForVanishedChild() throws Exception {

        final TestingOutputStream listingTos = new TestingOutputStream();

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/foo"));

                StoredObject fooSo = initFolderStoredObject();
                StoredObject fileSo = initFileStoredObject(resourceContent);

                exactly(2).of(mockStore).getStoredObject(mockTransaction, "/foo");
                will(returnValue(fooSo));

                oneOf(mockReq).getHeader("If-None-Match");
                will(returnValue(null));

                oneOf(mockReq).getLocale();
                will(returnValue(Locale.ENGLISH));

                oneOf(mockRes).setContentType("text/html");
                oneOf(mockRes).setCharacterEncoding("UTF8");

                oneOf(mockRes).getOutputStream();
                will(returnValue(listingTos));

                oneOf(mockStore).getChildrenNames(mockTransaction, "/foo");
                will(returnValue(new String[]{"gone", "file.txt"}));

                oneOf(mockStore).getStoredObject(mockTransaction, "/foo/file.txt");
                will(returnValue(fileSo));

                oneOf(mockStore).getStoredObject(mockTransaction, "/foo/gone");
                will(returnValue(null));
            }
        });

        DoGet doGet = new DoGet(mockStore, null, null, new ResourceLocks(),
                mockMimeTyper, 0);

        doGet.execute(mockTransaction, mockReq, mockRes);

        String output = listingTos.toString();
        // children are sorted by name: "file.txt" (even row) before "gone" (odd row)
        assertTrue(output.indexOf("file.txt") < output.indexOf("gone"));
        assertTrue(output.contains("<tr class=\"even\"><td><a href=\"file.txt\">file.txt</a></td><td>" + resourceContent.length +
                                   " Bytes</td>"));
        assertTrue(output.contains("<tr class=\"odd\"><td><a href=\"gone\">gone</a></td><td>Unknown Bytes</td><td></td><td></td></tr>"));

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testGetOnLockNullResourceResultsIn405() throws Exception {

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

        DoGet doGet = new DoGet(mockStore, null, null, new ResourceLocks(),
                mockMimeTyper, 0);

        doGet.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

}
