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
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.regex.Pattern;

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
import nl.info.webdav.exceptions.PathTraversalException;
import nl.info.webdav.locking.ResourceLocks;
import nl.info.webdav.testutil.DelegatingServletInputStream;
import nl.info.webdav.testutil.MockTest;

public class DoPropfindTest extends MockTest {
    static IWebdavStore mockStore;
    static IMimeTyper mockMimeTyper;
    static HttpServletRequest mockReq;
    static HttpServletResponse mockRes;
    static ITransaction mockTransaction;
    static PrintWriter printWriter;
    static byte[] resourceContent = new byte[]{'<', 'h', 'e', 'l', 'l', 'o',
                                               '/', '>'};

    @BeforeAll
    public static void setUp() throws Exception {
        mockStore = _mockery.mock(IWebdavStore.class);
        mockMimeTyper = _mockery.mock(IMimeTyper.class);
        mockReq = _mockery.mock(HttpServletRequest.class);
        mockRes = _mockery.mock(HttpServletResponse.class);
        mockTransaction = _mockery.mock(ITransaction.class);
    }

    @Test
    public void doPropFindOnDirectory() throws Exception {
        final String path = "/";

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getHeader("Depth");
                will(returnValue("infinity"));

                StoredObject rootSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(rootSo));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getContentLength();
                will(returnValue(0));
                // no content, which means it is a allprop request

                oneOf(mockRes).setStatus(WebdavStatus.SC_MULTI_STATUS);

                oneOf(mockRes).setContentType("text/xml; charset=UTF-8");

                oneOf(mockRes).getWriter();
                will(returnValue(printWriter));

                oneOf(mockMimeTyper).getMimeType(mockTransaction, path);
                will(returnValue("text/xml; charset=UTF-8"));

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(rootSo));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getServletPath();
                will(returnValue(path));

                oneOf(mockStore).getChildrenNames(mockTransaction, path);
                will(returnValue(new String[]{"file1", "file2"}));

                StoredObject file1So = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, path + "file1");
                will(returnValue(file1So));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getServletPath();
                will(returnValue(path));

                oneOf(mockStore)
                        .getChildrenNames(mockTransaction, path + "file1");
                will(returnValue(new String[]{}));

                StoredObject file2So = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, path + "file2");
                will(returnValue(file2So));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getServletPath();
                will(returnValue(path));

                oneOf(mockStore)
                        .getChildrenNames(mockTransaction, path + "file2");
                will(returnValue(new String[]{}));
            }
        });

        DoPropfind doPropfind = new DoPropfind(mockStore, new ResourceLocks(),
                mockMimeTyper);
        doPropfind.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void doPropFindOnFile() throws Exception {
        final String path = "/testFile";

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getHeader("Depth");
                will(returnValue("0"));

                StoredObject fileSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(fileSo));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getContentLength();
                will(returnValue(0));
                // no content, which means it is a allprop request

                oneOf(mockRes).setStatus(WebdavStatus.SC_MULTI_STATUS);

                oneOf(mockRes).setContentType("text/xml; charset=UTF-8");

                oneOf(mockRes).getWriter();
                will(returnValue(printWriter));

                oneOf(mockMimeTyper).getMimeType(mockTransaction, path);
                will(returnValue("text/xml; charset=UTF-8"));

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(fileSo));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getServletPath();
                will(returnValue("/"));
            }
        });

        DoPropfind doPropfind = new DoPropfind(mockStore, new ResourceLocks(),
                mockMimeTyper);

        doPropfind.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void doPropFindOnNonExistingResource() throws Exception {
        final String path = "/notExists";

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getHeader("Depth");
                will(returnValue("0"));

                StoredObject notExistingSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(notExistingSo));

                oneOf(mockRes).setContentType("text/xml; charset=UTF-8");

                oneOf(mockReq).getRequestURI();
                will(returnValue(path));

                oneOf(mockRes).sendError(WebdavStatus.SC_NOT_FOUND, path);
            }
        });

        DoPropfind doPropfind = new DoPropfind(mockStore, new ResourceLocks(),
                mockMimeTyper);

        doPropfind.execute(mockTransaction, mockReq, mockRes);

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

        DoPropfind doPropfind = new DoPropfind(mockStore, new ResourceLocks(), mockMimeTyper);
        assertThrows(PathTraversalException.class, () -> doPropfind.execute(mockTransaction, mockReq, mockRes));
        _mockery.assertIsSatisfied();
    }

    private static final String OK_STATUS = "HTTP/1.1 " + WebdavStatus.SC_OK + " " + WebdavStatus.getStatusText(WebdavStatus.SC_OK);
    private static final String NOT_FOUND_STATUS = "HTTP/1.1 " + WebdavStatus.SC_NOT_FOUND + " " + WebdavStatus.getStatusText(
            WebdavStatus.SC_NOT_FOUND
    );

    private static final String ALL_LIVE_PROPS_REQUEST = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>" +
                                                         "<D:propfind xmlns:D=\"DAV:\"><D:prop>" +
                                                         "<D:creationdate/><D:displayname/><D:getcontentlanguage/>" +
                                                         "<D:getcontentlength/><D:getcontenttype/><D:getetag/>" +
                                                         "<D:getlastmodified/><D:resourcetype/><D:source/>" +
                                                         "<D:supportedlock/><D:lockdiscovery/>" + "</D:prop></D:propfind>";

    private static int mockCounter = 0;

    /**
     * Creates a fresh request mock (unique name) with lenient expectations for the incidental calls made by DoPropfind.
     */
    private static HttpServletRequest newRequest(final String path, final String depth, final String body) throws IOException {
        final HttpServletRequest req = _mockery.mock(HttpServletRequest.class, "propfindReq" + (++mockCounter));
        final byte[] bytes = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
        _mockery.checking(new Expectations() {
            {
                allowing(req).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));
                allowing(req).getPathInfo();
                will(returnValue(path));
                allowing(req).getHeader("Depth");
                will(returnValue(depth));
                allowing(req).getContentLength();
                will(returnValue(bytes.length));
                allowing(req).getInputStream();
                will(returnValue(new DelegatingServletInputStream(new ByteArrayInputStream(bytes))));
                allowing(req).getContextPath();
                will(returnValue(""));
                allowing(req).getServletPath();
                will(returnValue(""));
                allowing(req).getRequestURI();
                will(returnValue(path));
            }
        });
        return req;
    }

    /**
     * Creates a fresh response mock that accepts a 207 multistatus and captures the written XML.
     */
    private static HttpServletResponse newMultiStatusResponse(final StringWriter out) throws IOException {
        final HttpServletResponse res = _mockery.mock(HttpServletResponse.class, "propfindRes" + (++mockCounter));
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

    private static HttpServletResponse newResponse() {
        return _mockery.mock(HttpServletResponse.class, "propfindRes" + (++mockCounter));
    }

    private static IWebdavStore newStore() {
        return _mockery.mock(IWebdavStore.class, "propfindStore" + (++mockCounter));
    }

    private static IMimeTyper newMimeTyper(final String path, final String mimeType) {
        final IMimeTyper mimeTyper = _mockery.mock(IMimeTyper.class, "propfindMimeTyper" + (++mockCounter));
        _mockery.checking(new Expectations() {
            {
                allowing(mimeTyper).getMimeType(mockTransaction, path);
                will(returnValue(mimeType));
            }
        });
        return mimeTyper;
    }

    private static void storeReturns(final IWebdavStore store, final String path, final StoredObject so) {
        _mockery.checking(new Expectations() {
            {
                allowing(store).getStoredObject(mockTransaction, path);
                will(returnValue(so));
            }
        });
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

    @Test
    public void propfindRequestingEachLivePropertyOnFileReturnsValuesInOkPropstat() throws Exception {
        final String path = "/folder/test.txt";
        final StoredObject fileSo = initFileStoredObject(resourceContent);
        final IWebdavStore store = newStore();
        storeReturns(store, path, fileSo);
        final HttpServletRequest req = newRequest(path, "0", ALL_LIVE_PROPS_REQUEST);
        final StringWriter out = new StringWriter();
        final HttpServletResponse res = newMultiStatusResponse(out);

        new DoPropfind(store, new ResourceLocks(), newMimeTyper(path, "text/plain")).execute(mockTransaction, req, res);

        _mockery.assertIsSatisfied();
        final String xml = out.toString();
        assertTrue(xml.contains("<D:multistatus xmlns:D=\"DAV:\">"), xml);
        assertTrue(xml.contains("<D:href>/folder/test.txt</D:href>"), xml);
        assertTrue(xml.contains("<D:creationdate>" + AbstractMethod.creationDateFormat(fileSo.getCreationDate()) + "</D:creationdate>"),
                xml);
        assertTrue(xml.contains("<D:displayname><![CDATA[test.txt]]></D:displayname>"), xml);
        assertTrue(xml.contains("<D:getcontentlanguage/>"), xml);
        assertTrue(xml.contains("<D:getcontentlength>" + resourceContent.length + "</D:getcontentlength>"), xml);
        assertTrue(xml.contains("<D:getcontenttype>text/plain</D:getcontenttype>"), xml);
        assertTrue(
                xml.contains(
                        "<D:getetag>W/&quot;" + resourceContent.length + "-" + fileSo.getLastModified().getTime() + "&quot;</D:getetag>"
                ),
                xml
        );
        assertTrue(
                xml.contains("<D:getlastmodified>" + AbstractMethod.lastModifiedDateFormat(fileSo.getLastModified()) +
                             "</D:getlastmodified>"),
                xml
        );
        assertTrue(xml.contains("<D:resourcetype/>"), xml);
        assertTrue(xml.contains("<D:source></D:source>"), xml);
        // no lock on the resource: both exclusive and shared lock entries are supported
        assertTrue(xml.contains("<D:supportedlock><D:lockentry><D:lockscope><D:exclusive/>"), xml);
        assertTrue(xml.contains("<D:lockscope><D:shared/></D:lockscope>"), xml);
        assertTrue(xml.contains("<D:lockdiscovery/>"), xml);
        // all properties are known for a file: a single 200 propstat, no 404 propstat
        assertEquals(1, countOccurrences(xml, "<D:propstat>"), xml);
        assertTrue(xml.contains("<D:status>" + OK_STATUS + "</D:status>"), xml);
        assertFalse(xml.contains(NOT_FOUND_STATUS), xml);
    }

    @Test
    public void propfindRequestingEachLivePropertyOnFolderReportsFileOnlyPropertiesAsNotFound() throws Exception {
        final String path = "/folder";
        final StoredObject folderSo = initFolderStoredObject();
        final IWebdavStore store = newStore();
        storeReturns(store, path, folderSo);
        final HttpServletRequest req = newRequest(path, "0", ALL_LIVE_PROPS_REQUEST);
        final StringWriter out = new StringWriter();
        final HttpServletResponse res = newMultiStatusResponse(out);

        new DoPropfind(store, new ResourceLocks(), newMimeTyper(path, null)).execute(mockTransaction, req, res);

        _mockery.assertIsSatisfied();
        final String xml = out.toString();
        final int okStatusIdx = xml.indexOf(OK_STATUS);
        assertTrue(okStatusIdx > 0, xml);
        final String okPropstat = xml.substring(0, okStatusIdx);
        final String notFoundPropstat = xml.substring(okStatusIdx);

        assertTrue(xml.contains("<D:href>/folder/</D:href>"), xml);
        assertTrue(okPropstat.contains("<D:resourcetype><D:collection/></D:resourcetype>"), xml);
        assertTrue(okPropstat.contains("<D:creationdate>"), xml);
        assertTrue(okPropstat.contains("<D:displayname><![CDATA[folder]]></D:displayname>"), xml);
        assertTrue(okPropstat.contains("<D:supportedlock>"), xml);
        assertTrue(okPropstat.contains("<D:lockdiscovery/>"), xml);
        assertFalse(okPropstat.contains("getcontentlength"), xml);
        assertFalse(okPropstat.contains("getcontenttype"), xml);
        assertFalse(okPropstat.contains("getetag"), xml);

        assertTrue(notFoundPropstat.contains(NOT_FOUND_STATUS), xml);
        assertTrue(notFoundPropstat.contains("<D:getcontentlanguage/>"), xml);
        assertTrue(notFoundPropstat.contains("<D:getcontentlength/>"), xml);
        assertTrue(notFoundPropstat.contains("<D:getcontenttype/>"), xml);
        assertTrue(notFoundPropstat.contains("<D:getetag/>"), xml);
        assertTrue(notFoundPropstat.contains("<D:getlastmodified/>"), xml);
        assertEquals(2, countOccurrences(xml, "<D:propstat>"), xml);
    }

    @Test
    public void propfindOnLockNullResourceReportsEtagAsNotFound() throws Exception {
        final String path = "/lockNull";
        final StoredObject lockNullSo = initLockNullStoredObject();
        // dates must be set, see propfindOnLockNullResourceWithoutDatesThrowsNullPointerException
        lockNullSo.setCreationDate(new Date());
        lockNullSo.setLastModified(new Date());
        final IWebdavStore store = newStore();
        storeReturns(store, path, lockNullSo);
        final HttpServletRequest req = newRequest(
                path,
                "0",
                "<D:propfind xmlns:D=\"DAV:\"><D:prop><D:getetag/></D:prop></D:propfind>"
        );
        final StringWriter out = new StringWriter();
        final HttpServletResponse res = newMultiStatusResponse(out);

        new DoPropfind(store, new ResourceLocks(), newMimeTyper(path, null)).execute(mockTransaction, req, res);

        _mockery.assertIsSatisfied();
        final String xml = out.toString();
        final String notFoundPropstat = xml.substring(xml.indexOf(OK_STATUS));
        assertTrue(notFoundPropstat.contains("<D:getetag/>"), xml);
        assertTrue(notFoundPropstat.contains(NOT_FOUND_STATUS), xml);
    }

    /**
     * Characterization: DoPropfind formats creation and last-modified dates unconditionally, so a stored object
     * without dates (such as a lock-null resource) causes a NullPointerException that escapes execute().
     */
    @Test
    public void propfindOnLockNullResourceWithoutDatesThrowsNullPointerException() throws Exception {
        final String path = "/lockNullNoDates";
        final IWebdavStore store = newStore();
        storeReturns(store, path, initLockNullStoredObject());
        final HttpServletRequest req = newRequest(path, "0", null);
        final HttpServletResponse res = newMultiStatusResponse(new StringWriter());
        final DoPropfind doPropfind = new DoPropfind(store, new ResourceLocks(), newMimeTyper(path, null));

        assertThrows(NullPointerException.class, () -> doPropfind.execute(mockTransaction, req, res));
        _mockery.assertIsSatisfied();
    }

    @Test
    public void propfindRequestingUnknownPropertyListsItInNotFoundPropstat() throws Exception {
        final String path = "/test.txt";
        final IWebdavStore store = newStore();
        storeReturns(store, path, initFileStoredObject(resourceContent));
        final HttpServletRequest req = newRequest(
                path,
                "0",
                "<D:propfind xmlns:D=\"DAV:\" xmlns:Z=\"http://example.com/ns\"><D:prop>" +
                     "<D:displayname/><Z:unknownprop/><D:nosuchprop/></D:prop></D:propfind>"
        );
        final StringWriter out = new StringWriter();
        final HttpServletResponse res = newMultiStatusResponse(out);

        new DoPropfind(store, new ResourceLocks(), newMimeTyper(path, "text/plain")).execute(mockTransaction, req, res);

        _mockery.assertIsSatisfied();
        final String xml = out.toString();
        final int okStatusIdx = xml.indexOf(OK_STATUS);
        final String okPropstat = xml.substring(0, okStatusIdx);
        final String notFoundPropstat = xml.substring(okStatusIdx);
        assertTrue(okPropstat.contains("<D:displayname><![CDATA[test.txt]]></D:displayname>"), xml);
        assertFalse(okPropstat.contains("unknownprop"), xml);
        assertTrue(notFoundPropstat.contains("<unknownprop xmlns=\"http://example.com/ns\"/>"), xml);
        assertTrue(notFoundPropstat.contains("<D:nosuchprop/>"), xml);
        assertTrue(notFoundPropstat.contains(NOT_FOUND_STATUS), xml);
    }

    @Test
    public void propfindPropnameOnFileListsPropertyNamesWithoutValues() throws Exception {
        final String path = "/test.txt";
        final IWebdavStore store = newStore();
        storeReturns(store, path, initFileStoredObject(resourceContent));
        final HttpServletRequest req = newRequest(path, "0", "<D:propfind xmlns:D=\"DAV:\"><D:propname/></D:propfind>");
        final StringWriter out = new StringWriter();
        final HttpServletResponse res = newMultiStatusResponse(out);

        new DoPropfind(store, new ResourceLocks(), newMimeTyper(path, "text/plain")).execute(mockTransaction, req, res);

        _mockery.assertIsSatisfied();
        final String xml = out.toString();
        for (String name : new String[]{"creationdate", "displayname", "getcontentlanguage", "getcontentlength",
                                        "getcontenttype", "getetag", "getlastmodified", "resourcetype", "supportedlock",
                                        "source"}) {
            assertTrue(xml.contains("<D:" + name + "/>"), name + " missing in " + xml);
            assertFalse(xml.contains("<D:" + name + ">"), name + " should have no value in " + xml);
        }
        assertFalse(xml.contains("text/plain"), xml);
        assertFalse(xml.contains("CDATA"), xml);
        assertTrue(xml.contains(OK_STATUS), xml);
    }

    @Test
    public void propfindPropnameOnFolderOmitsFileOnlyPropertyNames() throws Exception {
        final String path = "/folder";
        final IWebdavStore store = newStore();
        storeReturns(store, path, initFolderStoredObject());
        final HttpServletRequest req = newRequest(path, "0", "<D:propfind xmlns:D=\"DAV:\"><D:propname/></D:propfind>");
        final StringWriter out = new StringWriter();
        final HttpServletResponse res = newMultiStatusResponse(out);

        new DoPropfind(store, new ResourceLocks(), newMimeTyper(path, null)).execute(mockTransaction, req, res);

        _mockery.assertIsSatisfied();
        final String xml = out.toString();
        assertTrue(xml.contains("<D:creationdate/>"), xml);
        assertTrue(xml.contains("<D:displayname/>"), xml);
        assertTrue(xml.contains("<D:resourcetype/>"), xml);
        assertFalse(xml.contains("getcontentlength"), xml);
        assertFalse(xml.contains("getetag"), xml);
    }

    @Test
    public void propfindAllpropWithDepthOneOnFolderReturnsOneResponsePerResource() throws Exception {
        final String path = "/folder";
        final IWebdavStore store = newStore();
        storeReturns(store, path, initFolderStoredObject());
        storeReturns(store, path + "/a.txt", initFileStoredObject(resourceContent));
        storeReturns(store, path + "/sub", initFolderStoredObject());
        _mockery.checking(new Expectations() {
            {
                oneOf(store).getChildrenNames(mockTransaction, path);
                will(returnValue(new String[]{"a.txt", "sub"}));
                // depth is exhausted for the children: their children are not listed
                never(store).getChildrenNames(mockTransaction, path + "/sub");
            }
        });
        final HttpServletRequest req = newRequest(path, "1", "<D:propfind xmlns:D=\"DAV:\"><D:allprop/></D:propfind>");
        final StringWriter out = new StringWriter();
        final HttpServletResponse res = newMultiStatusResponse(out);

        new DoPropfind(store, new ResourceLocks(), newMimeTyper(path, "httpd/unix-directory")).execute(
                mockTransaction,
                req,
                res
        );

        _mockery.assertIsSatisfied();
        final String xml = out.toString();
        assertEquals(3, countOccurrences(xml, "<D:response>"), xml);
        assertTrue(xml.contains("<D:href>/folder/</D:href>"), xml);
        assertTrue(xml.contains("<D:href>/folder/a.txt</D:href>"), xml);
        assertTrue(xml.contains("<D:href>/folder/sub/</D:href>"), xml);
        assertEquals(2, countOccurrences(xml, "<D:resourcetype><D:collection/></D:resourcetype>"), xml);
        assertEquals(3, countOccurrences(xml, "<D:creationdate>"), xml);
        assertEquals(3, countOccurrences(xml, "<D:displayname>"), xml);
        assertEquals(3, countOccurrences(xml, "<D:supportedlock>"), xml);
        assertEquals(3, countOccurrences(xml, "<D:lockdiscovery/>"), xml);
        assertEquals(3, countOccurrences(xml, "<D:source></D:source>"), xml);
        // file-only properties are rendered only for the file
        assertEquals(1, countOccurrences(xml, "<D:getcontentlength>" + resourceContent.length + "</D:getcontentlength>"), xml);
        assertEquals(1, countOccurrences(xml, "<D:getlastmodified>"), xml);
        assertEquals(1, countOccurrences(xml, "<D:getetag>"), xml);
        assertEquals(1, countOccurrences(xml, "<D:resourcetype/>"), xml);
        // Characterization: the mime type of the requested (root) path is reused for all children,
        // so the child file gets the folder's content type (DoPropfind.recursiveParseProperties).
        assertTrue(xml.contains("<D:getcontenttype>httpd/unix-directory</D:getcontenttype>"), xml);
        assertEquals(3, countOccurrences(xml, OK_STATUS), xml);
    }

    @Test
    public void propfindAllpropOnFileWithoutMimeTypeOmitsContentType() throws Exception {
        final String path = "/test.bin";
        final IWebdavStore store = newStore();
        storeReturns(store, path, initFileStoredObject(resourceContent));
        final HttpServletRequest req = newRequest(path, "0", null);
        final StringWriter out = new StringWriter();
        final HttpServletResponse res = newMultiStatusResponse(out);

        new DoPropfind(store, new ResourceLocks(), newMimeTyper(path, null)).execute(mockTransaction, req, res);

        _mockery.assertIsSatisfied();
        final String xml = out.toString();
        assertTrue(xml.contains("<D:getcontentlength>"), xml);
        assertFalse(xml.contains("getcontenttype"), xml);
    }

    @Test
    public void propfindLockdiscoveryOnExclusivelyLockedResource() throws Exception {
        final String path = "/locked.txt";
        final ResourceLocks resourceLocks = new ResourceLocks();
        assertTrue(resourceLocks.exclusiveLock(mockTransaction, path, "Lock Owner", 0, 3600));
        final String lockToken = resourceLocks.getLockedObjectByPath(mockTransaction, path).getID();
        final IWebdavStore store = newStore();
        storeReturns(store, path, initFileStoredObject(resourceContent));
        final HttpServletRequest req = newRequest(
                path,
                "0",
                "<D:propfind xmlns:D=\"DAV:\"><D:prop><D:lockdiscovery/><D:supportedlock/></D:prop></D:propfind>"
        );
        final StringWriter out = new StringWriter();
        final HttpServletResponse res = newMultiStatusResponse(out);

        new DoPropfind(store, resourceLocks, newMimeTyper(path, "text/plain")).execute(mockTransaction, req, res);

        _mockery.assertIsSatisfied();
        final String xml = out.toString();
        assertTrue(xml.contains("<D:lockdiscovery><D:activelock>"), xml);
        assertTrue(xml.contains("<D:locktype><D:write/></D:locktype>"), xml);
        assertTrue(xml.contains("<D:lockscope><D:exclusive/></D:lockscope>"), xml);
        // Characterization: depth is taken from the request Depth header, not from the lock itself
        assertTrue(xml.contains("<D:depth>0</D:depth>"), xml);
        assertTrue(xml.contains("<D:owner><D:href>Lock Owner</D:href>"), xml);
        assertTrue(Pattern.compile("<D:timeout>Second-(359\\d|3600)</D:timeout>").matcher(xml).find(), xml);
        assertTrue(xml.contains("<D:locktoken><D:href>opaquelocktoken:" + lockToken + "</D:href>"), xml);
        // an exclusive lock leaves no supported lock entries
        assertTrue(xml.contains("<D:supportedlock></D:supportedlock>"), xml);
    }

    @Test
    public void propfindLockdiscoveryOnSharedLockedResourceWithInfiniteDepth() throws Exception {
        final String path = "/shared.txt";
        final ResourceLocks resourceLocks = new ResourceLocks();
        assertTrue(resourceLocks.sharedLock(mockTransaction, path, "Owner A", 0, 3600));
        final IWebdavStore store = newStore();
        storeReturns(store, path, initFileStoredObject(resourceContent));
        _mockery.checking(new Expectations() {
            {
                allowing(store).getChildrenNames(mockTransaction, path);
                will(returnValue(null));
            }
        });
        // no Depth header means infinity
        final HttpServletRequest req = newRequest(
                path,
                null,
                "<D:propfind xmlns:D=\"DAV:\"><D:prop><D:lockdiscovery/><D:supportedlock/></D:prop></D:propfind>"
        );
        final StringWriter out = new StringWriter();
        final HttpServletResponse res = newMultiStatusResponse(out);

        new DoPropfind(store, resourceLocks, newMimeTyper(path, "text/plain")).execute(mockTransaction, req, res);

        _mockery.assertIsSatisfied();
        final String xml = out.toString();
        assertTrue(xml.contains("<D:lockdiscovery><D:activelock>"), xml);
        assertTrue(xml.contains("<D:lockscope><D:shared/></D:lockscope>"), xml);
        assertFalse(xml.contains("<D:exclusive/>"), xml);
        assertTrue(xml.contains("<D:depth>Infinity</D:depth>"), xml);
        assertTrue(xml.contains("<D:owner><D:href>Owner A</D:href>"), xml);
        // a shared lock still allows further shared locks
        assertTrue(xml.contains("<D:supportedlock><D:lockentry><D:lockscope><D:shared/></D:lockscope>"), xml);
    }

    @Test
    public void propfindOnMissingResourceSendsNotFound() throws Exception {
        final String path = "/missing.txt";
        final IWebdavStore store = newStore();
        storeReturns(store, path, null);
        final HttpServletRequest req = newRequest(path, "0", null);
        final HttpServletResponse res = newResponse();
        _mockery.checking(new Expectations() {
            {
                oneOf(res).setContentType("text/xml; charset=UTF-8");
                oneOf(res).sendError(WebdavStatus.SC_NOT_FOUND, path);
                never(res).getWriter();
            }
        });

        new DoPropfind(store, new ResourceLocks(), newMimeTyper(path, null)).execute(mockTransaction, req, res);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void propfindWhenStoreDeniesAccessSendsForbidden() throws Exception {
        final String path = "/secret.txt";
        final IWebdavStore store = newStore();
        _mockery.checking(new Expectations() {
            {
                oneOf(store).getStoredObject(mockTransaction, path);
                will(throwException(new AccessDeniedException(new RuntimeException("denied"))));
            }
        });
        final HttpServletRequest req = newRequest(path, "0", null);
        final HttpServletResponse res = newResponse();
        _mockery.checking(new Expectations() {
            {
                oneOf(res).sendError(WebdavStatus.SC_FORBIDDEN);
                never(res).getWriter();
            }
        });

        new DoPropfind(store, new ResourceLocks(), newMimeTyper(path, null)).execute(mockTransaction, req, res);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void propfindWithMalformedBodySendsInternalServerErrorWithoutBody() throws Exception {
        final String path = "/test.txt";
        final IWebdavStore store = newStore();
        storeReturns(store, path, initFileStoredObject(resourceContent));
        final HttpServletRequest req = newRequest(path, "0", "not valid xml <<>>");
        final HttpServletResponse res = newResponse();
        _mockery.checking(new Expectations() {
            {
                oneOf(res).sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
                never(res).getWriter();
                never(res).setStatus(WebdavStatus.SC_MULTI_STATUS);
            }
        });

        new DoPropfind(store, new ResourceLocks(), newMimeTyper(path, null)).execute(mockTransaction, req, res);

        _mockery.assertIsSatisfied();
    }

}
