/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.webdav.methods;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

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
import nl.info.webdav.locking.ResourceLocks;
import nl.info.webdav.testutil.DelegatingServletInputStream;
import nl.info.webdav.testutil.MockTest;

public class DoProppatchTest extends MockTest {
    static IWebdavStore mockStore;
    static IMimeTyper mockMimeTyper;
    static HttpServletRequest mockReq;
    static HttpServletResponse mockRes;
    static ITransaction mockTransaction;
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
    public void doProppatchIfReadOnly() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
            }
        });

        DoProppatch doProppatch = new DoProppatch(mockStore,
                new ResourceLocks(), readOnly);

        doProppatch.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void doProppatchOnNonExistingResource() throws Exception {

        final String path = "/notExists";

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                StoredObject notExistingSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(notExistingSo));

                oneOf(mockRes).sendError(WebdavStatus.SC_NOT_FOUND);
            }
        });

        DoProppatch doProppatch = new DoProppatch(mockStore,
                new ResourceLocks(), !readOnly);

        doProppatch.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void doProppatchOnRequestWithNoContent() throws Exception {

        final String path = "/testFile";

        _mockery.checking(new Expectations() {
            {
                exactly(2).of(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                exactly(2).of(mockReq).getPathInfo();
                will(returnValue(path));

                StoredObject testFileSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(testFileSo));

                oneOf(mockReq).getContentLength();
                will(returnValue(0));

                oneOf(mockReq).getHeader("If");
                will(returnValue(null));

                oneOf(mockRes).sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
            }
        });

        DoProppatch doProppatch = new DoProppatch(mockStore,
                new ResourceLocks(), !readOnly);

        doProppatch.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void doProppatchWithXxeContentReturnsInternalServerError() throws Exception {

        final String path = "/testFile";
        final byte[] xxeContent = ("<?xml version=\"1.0\"?>" + "<!DOCTYPE foo [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>" +
                                   "<propertyupdate><set><prop><evil>&xxe;</evil></prop></set></propertyupdate>")
                                           .getBytes(StandardCharsets.UTF_8);
        final DelegatingServletInputStream xxeDsis = new DelegatingServletInputStream(
                new ByteArrayInputStream(xxeContent));

        _mockery.checking(new Expectations() {
            {
                exactly(2).of(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                exactly(2).of(mockReq).getPathInfo();
                will(returnValue(path));

                StoredObject testFileSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(testFileSo));

                oneOf(mockReq).getHeader("If");
                will(returnValue(null));

                oneOf(mockReq).getContentLength();
                will(returnValue(xxeContent.length));

                oneOf(mockReq).getInputStream();
                will(returnValue(xxeDsis));

                oneOf(mockRes).sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
            }
        });

        DoProppatch doProppatch = new DoProppatch(mockStore,
                new ResourceLocks(), !readOnly);

        doProppatch.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void doProppatchWithMalformedXmlReturnsInternalServerError() throws Exception {

        final String path = "/testFile";
        final byte[] malformedContent = "not valid xml <<>>".getBytes(StandardCharsets.UTF_8);
        final DelegatingServletInputStream malformedDsis = new DelegatingServletInputStream(
                new ByteArrayInputStream(malformedContent));

        _mockery.checking(new Expectations() {
            {
                exactly(2).of(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                exactly(2).of(mockReq).getPathInfo();
                will(returnValue(path));

                StoredObject testFileSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(testFileSo));

                oneOf(mockReq).getHeader("If");
                will(returnValue(null));

                oneOf(mockReq).getContentLength();
                will(returnValue(malformedContent.length));

                oneOf(mockReq).getInputStream();
                will(returnValue(malformedDsis));

                oneOf(mockRes).sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
            }
        });

        DoProppatch doProppatch = new DoProppatch(mockStore,
                new ResourceLocks(), !readOnly);

        doProppatch.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void doProppatchOnResource() throws Exception {

        final String path = "/testFile";
        final PrintWriter pw = new PrintWriter("/tmp/XMLTestFile");

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                StoredObject testFileSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(testFileSo));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getContentLength();
                will(returnValue(8));

                oneOf(mockReq).getInputStream();
                will(returnValue(dsis));

                oneOf(mockRes).setStatus(WebdavStatus.SC_MULTI_STATUS);

                oneOf(mockRes).setContentType("text/xml; charset=UTF-8");

                oneOf(mockRes).getWriter();
                will(returnValue(pw));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getHeader("If");
                will(returnValue(null));
            }
        });

        DoProppatch doProppatch = new DoProppatch(mockStore,
                new ResourceLocks(), !readOnly);

        doProppatch.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    private static final String OK_STATUS = "HTTP/1.1 " + WebdavStatus.SC_OK + " " + WebdavStatus.getStatusText(WebdavStatus.SC_OK);

    private static final String SET_AND_REMOVE_REQUEST = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>" +
                                                         "<D:propertyupdate xmlns:D=\"DAV:\" xmlns:Z=\"http://example.com/ns\">" +
                                                         "<D:set><D:prop><Z:author>Jane</Z:author><Z:title>Doc</Z:title></D:prop></D:set>" +
                                                         "<D:remove><D:prop><Z:obsolete/></D:prop></D:remove>" + "</D:propertyupdate>";

    private static int mockCounter = 0;

    /**
     * Creates a fresh request mock (unique name) with lenient expectations for the incidental calls made by DoProppatch.
     */
    private static HttpServletRequest newRequest(final String path, final String ifHeader, final String body) throws IOException {
        final HttpServletRequest req = _mockery.mock(HttpServletRequest.class, "proppatchReq" + (++mockCounter));
        final byte[] bytes = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
        _mockery.checking(new Expectations() {
            {
                allowing(req).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));
                allowing(req).getPathInfo();
                will(returnValue(path));
                allowing(req).getHeader("If");
                will(returnValue(ifHeader));
                allowing(req).getContentLength();
                will(returnValue(bytes.length));
                allowing(req).getInputStream();
                will(returnValue(new DelegatingServletInputStream(new ByteArrayInputStream(bytes))));
                allowing(req).getContextPath();
                will(returnValue(""));
            }
        });
        return req;
    }

    private static HttpServletResponse newResponse() {
        return _mockery.mock(HttpServletResponse.class, "proppatchRes" + (++mockCounter));
    }

    private static HttpServletResponse newMultiStatusResponse(final StringWriter out) throws IOException {
        final HttpServletResponse res = newResponse();
        _mockery.checking(new Expectations() {
            {
                oneOf(res).setStatus(WebdavStatus.SC_MULTI_STATUS);
                oneOf(res).setContentType("text/xml; charset=UTF-8");
                oneOf(res).getWriter();
                will(returnValue(new PrintWriter(out)));
            }
        });
        return res;
    }

    /**
     * Creates a store mock that only allows reading the stored object; any other (mutating) call fails the test.
     */
    private static IWebdavStore newStore(final String path, final StoredObject so) {
        final IWebdavStore store = _mockery.mock(IWebdavStore.class, "proppatchStore" + (++mockCounter));
        _mockery.checking(new Expectations() {
            {
                allowing(store).getStoredObject(mockTransaction, path);
                will(returnValue(so));
            }
        });
        return store;
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        int idx = haystack.indexOf(needle);
        while (idx != -1) {
            count++;
            idx = haystack.indexOf(needle, idx + needle.length());
        }
        return count;
    }

    /**
     * Characterization: DoProppatch does not persist anything (IWebdavStore has no custom-property API); it only
     * reports every property from the set and remove sections with status 200 in a multistatus response.
     */
    @Test
    public void proppatchWithSetAndRemoveReturnsMultiStatusListingEachProperty() throws Exception {
        final String path = "/test.txt";
        final IWebdavStore store = newStore(path, initFileStoredObject(resourceContent));
        final HttpServletRequest req = newRequest(path, null, SET_AND_REMOVE_REQUEST);
        final StringWriter out = new StringWriter();
        final HttpServletResponse res = newMultiStatusResponse(out);

        new DoProppatch(store, new ResourceLocks(), !readOnly).execute(mockTransaction, req, res);

        _mockery.assertIsSatisfied();
        final String xml = out.toString();
        assertTrue(xml.contains("<D:multistatus xmlns:D=\"DAV:\">"), xml);
        assertEquals(1, countOccurrences(xml, "<D:response>"), xml);
        assertTrue(xml.contains("<D:href>/test.txt</D:href>"), xml);
        assertTrue(xml.contains("<author xmlns=\"http://example.com/ns\"/>"), xml);
        assertTrue(xml.contains("<title xmlns=\"http://example.com/ns\"/>"), xml);
        assertTrue(xml.contains("<obsolete xmlns=\"http://example.com/ns\"/>"), xml);
        // property values are not echoed
        assertTrue(!xml.contains("Jane"), xml);
        assertEquals(3, countOccurrences(xml, "<D:propstat>"), xml);
        assertEquals(3, countOccurrences(xml, "<D:status>" + OK_STATUS + "</D:status>"), xml);
    }

    @Test
    public void proppatchOnFolderAppendsTrailingSlashToHref() throws Exception {
        final String path = "/folder";
        final IWebdavStore store = newStore(path, initFolderStoredObject());
        final HttpServletRequest req = newRequest(
                path,
                null,
                "<D:propertyupdate xmlns:D=\"DAV:\"><D:remove><D:prop><D:foo/></D:prop></D:remove></D:propertyupdate>"
        );
        final StringWriter out = new StringWriter();
        final HttpServletResponse res = newMultiStatusResponse(out);

        new DoProppatch(store, new ResourceLocks(), !readOnly).execute(mockTransaction, req, res);

        _mockery.assertIsSatisfied();
        final String xml = out.toString();
        assertTrue(xml.contains("<D:href>/folder/</D:href>"), xml);
        assertTrue(xml.contains("<D:foo/>"), xml);
        assertEquals(1, countOccurrences(xml, "<D:propstat>"), xml);
    }

    @Test
    public void proppatchOnResourceLockedByOtherOwnerReturnsLocked() throws Exception {
        final String path = "/locked.txt";
        final ResourceLocks resourceLocks = new ResourceLocks();
        assertTrue(resourceLocks.exclusiveLock(mockTransaction, path, "other owner", 0, 3600));
        final IWebdavStore store = _mockery.mock(IWebdavStore.class, "proppatchStore" + (++mockCounter));
        final HttpServletRequest req = newRequest(path, null, SET_AND_REMOVE_REQUEST);
        final HttpServletResponse res = newResponse();
        _mockery.checking(new Expectations() {
            {
                oneOf(res).setStatus(WebdavStatus.SC_LOCKED);
                never(store).getStoredObject(mockTransaction, path);
            }
        });

        new DoProppatch(store, resourceLocks, !readOnly).execute(mockTransaction, req, res);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void proppatchOnLockedResourceWithNonMatchingLockTokenReturnsLocked() throws Exception {
        final String path = "/locked.txt";
        final ResourceLocks resourceLocks = new ResourceLocks();
        assertTrue(resourceLocks.exclusiveLock(mockTransaction, path, "other owner", 0, 3600));
        final IWebdavStore store = _mockery.mock(IWebdavStore.class, "proppatchStore" + (++mockCounter));
        final HttpServletRequest req = newRequest(path, "(<opaquelocktoken:not-the-right-token>)", SET_AND_REMOVE_REQUEST);
        final HttpServletResponse res = newResponse();
        _mockery.checking(new Expectations() {
            {
                oneOf(res).setStatus(WebdavStatus.SC_LOCKED);
            }
        });

        new DoProppatch(store, resourceLocks, !readOnly).execute(mockTransaction, req, res);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void proppatchOnResourceWithLockedParentReturnsLocked() throws Exception {
        final String path = "/lockedFolder/test.txt";
        final ResourceLocks resourceLocks = new ResourceLocks();
        assertTrue(resourceLocks.exclusiveLock(mockTransaction, "/lockedFolder", "other owner", 0, 3600));
        final IWebdavStore store = _mockery.mock(IWebdavStore.class, "proppatchStore" + (++mockCounter));
        final HttpServletRequest req = newRequest(path, null, SET_AND_REMOVE_REQUEST);
        final HttpServletResponse res = newResponse();
        _mockery.checking(new Expectations() {
            {
                oneOf(res).setStatus(WebdavStatus.SC_LOCKED);
            }
        });

        new DoProppatch(store, resourceLocks, !readOnly).execute(mockTransaction, req, res);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void proppatchOnLockedResourceWithMatchingLockTokenSucceeds() throws Exception {
        final String path = "/locked.txt";
        final ResourceLocks resourceLocks = new ResourceLocks();
        assertTrue(resourceLocks.exclusiveLock(mockTransaction, path, "owner", 0, 3600));
        final String lockToken = resourceLocks.getLockedObjectByPath(mockTransaction, path).getID();
        final IWebdavStore store = newStore(path, initFileStoredObject(resourceContent));
        final HttpServletRequest req = newRequest(path, "(<opaquelocktoken:" + lockToken + ">)", SET_AND_REMOVE_REQUEST);
        final StringWriter out = new StringWriter();
        final HttpServletResponse res = newMultiStatusResponse(out);

        new DoProppatch(store, resourceLocks, !readOnly).execute(mockTransaction, req, res);

        _mockery.assertIsSatisfied();
        assertEquals(3, countOccurrences(out.toString(), "<D:propstat>"), out.toString());
    }

    /**
     * Characterization (possible bug): an If header on a resource that is not locked dereferences the null
     * LockedObject in DoProppatch.execute and throws a NullPointerException.
     */
    @Test
    public void proppatchWithIfHeaderOnUnlockedResourceThrowsNullPointerException() throws Exception {
        final String path = "/test.txt";
        final IWebdavStore store = newStore(path, initFileStoredObject(resourceContent));
        final HttpServletRequest req = newRequest(path, "(<opaquelocktoken:some-token>)", SET_AND_REMOVE_REQUEST);
        final HttpServletResponse res = newResponse();
        final DoProppatch doProppatch = new DoProppatch(store, new ResourceLocks(), !readOnly);

        assertThrows(NullPointerException.class, () -> doProppatch.execute(mockTransaction, req, res));
        _mockery.assertIsSatisfied();
    }

    @Test
    public void proppatchOnLockNullResourceReturnsMethodNotAllowed() throws Exception {
        final String path = "/lockNull";
        final StoredObject lockNullSo = initLockNullStoredObject();
        final IWebdavStore store = newStore(path, lockNullSo);
        final HttpServletRequest req = newRequest(path, null, SET_AND_REMOVE_REQUEST);
        final HttpServletResponse res = newResponse();
        _mockery.checking(new Expectations() {
            {
                oneOf(res).addHeader("Allow", DeterminableMethod.determineMethodsAllowed(lockNullSo));
                oneOf(res).sendError(WebdavStatus.SC_METHOD_NOT_ALLOWED);
            }
        });

        new DoProppatch(store, new ResourceLocks(), !readOnly).execute(mockTransaction, req, res);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void proppatchWhenStoreDeniesAccessSendsForbidden() throws Exception {
        final String path = "/secret.txt";
        final IWebdavStore store = _mockery.mock(IWebdavStore.class, "proppatchStore" + (++mockCounter));
        final HttpServletRequest req = newRequest(path, null, SET_AND_REMOVE_REQUEST);
        final HttpServletResponse res = newResponse();
        _mockery.checking(new Expectations() {
            {
                oneOf(store).getStoredObject(mockTransaction, path);
                will(throwException(new AccessDeniedException(new RuntimeException("denied"))));
                oneOf(res).sendError(WebdavStatus.SC_FORBIDDEN);
            }
        });

        new DoProppatch(store, new ResourceLocks(), !readOnly).execute(mockTransaction, req, res);

        _mockery.assertIsSatisfied();
    }

}
