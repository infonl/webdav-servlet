/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.webdav.methods;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Sequence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import nl.info.webdav.ITransaction;
import nl.info.webdav.IWebdavStore;
import nl.info.webdav.StoredObject;
import nl.info.webdav.WebdavStatus;
import nl.info.webdav.exceptions.PathTraversalException;
import nl.info.webdav.locking.IResourceLocks;
import nl.info.webdav.locking.LockedObject;
import nl.info.webdav.locking.ResourceLocks;
import nl.info.webdav.testutil.DelegatingServletInputStream;
import nl.info.webdav.testutil.MockTest;

public class DoLockTest extends MockTest {

    static IWebdavStore mockStore;
    static HttpServletRequest mockReq;
    static HttpServletResponse mockRes;
    static ITransaction mockTransaction;
    static IResourceLocks mockResourceLocks;

    static boolean exclusive = true;
    static String depthString = "-1";
    static int depth = -1;
    static String timeoutString = "10";

    private static final String DARWIN_USER_AGENT = "WebDAVFS/3.0.0 (03008000) Darwin/17.7.0 (x86_64)";

    private StringWriter responseBody;

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
    public void testDoLockIfReadOnly() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
            }
        });

        ResourceLocks resLocks = new ResourceLocks();

        DoLock doLock = new DoLock(mockStore, resLocks, readOnly);
        doLock.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoRefreshLockOnLockedResource() throws Exception {

        final String lockPath = "/aFileToLock";
        final String lockOwner = "owner";

        ResourceLocks resLocks = new ResourceLocks();
        resLocks.lock(mockTransaction, lockPath, lockOwner, exclusive, depth,
                TEMP_TIMEOUT, !TEMPORARY);

        LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction,
                lockPath);
        String lockTokenString = lo.getID();
        final String lockToken = "(<opaquelocktoken:" + lockTokenString + ">)";

        final PrintWriter pw = new PrintWriter("/tmp/XMLTestFile");

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(lockPath));

                oneOf(mockReq).getHeader("If");
                will(returnValue(lockToken));

                oneOf(mockReq).getHeader("User-Agent");
                will(returnValue("Goliath"));

                exactly(2).of(mockReq).getHeader("If");
                will(returnValue(lockToken));

                oneOf(mockReq).getHeader("Timeout");
                will(returnValue("Infinite"));

                oneOf(mockRes).setStatus(WebdavStatus.SC_OK);

                oneOf(mockRes).setContentType("text/xml; charset=UTF-8");

                oneOf(mockRes).getWriter();
                will(returnValue(pw));

                oneOf(mockRes).addHeader(
                        "Lock-Token",
                        lockToken.substring(lockToken.indexOf("(") + 1,
                                lockToken.indexOf(")")));
            }
        });

        DoLock doLock = new DoLock(mockStore, resLocks, !readOnly);
        doLock.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoExclusiveLockOnResource() throws Exception {

        final String lockPath = "/aFileToLock";

        ResourceLocks resLocks = new ResourceLocks();
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
                will(returnValue(lockPath));

                oneOf(mockReq).getHeader("User-Agent");
                will(returnValue("Goliath"));

                oneOf(mockReq).getHeader("If");
                will(returnValue(null));

                StoredObject so = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, lockPath);
                will(returnValue(so));

                oneOf(mockReq).getInputStream();
                will(returnValue(dsisExclusive));

                oneOf(mockReq).getHeader("Depth");
                will(returnValue(depthString));

                oneOf(mockReq).getHeader("Timeout");
                will(returnValue(timeoutString));

                oneOf(mockRes).setStatus(WebdavStatus.SC_OK);

                oneOf(mockRes).setContentType("text/xml; charset=UTF-8");

                oneOf(mockRes).getWriter();
                will(returnValue(pw));

                // addHeader("Lock-Token", "(<opaquelocktoken:xxx-xxx-xxx>)")
                oneOf(mockRes).addHeader(with(any(String.class)),
                        with(any(String.class)));
            }
        });

        DoLock doLock = new DoLock(mockStore, resLocks, !readOnly);
        doLock.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoSharedLockOnResource() throws Exception {

        final String lockPath = "/aFileToLock";

        ResourceLocks resLocks = new ResourceLocks();
        final PrintWriter pw = new PrintWriter("/tmp/XMLTestFile");

        final ByteArrayInputStream baisShared = new ByteArrayInputStream(
                sharedLockRequestByteArray);
        final DelegatingServletInputStream dsisShared = new DelegatingServletInputStream(
                baisShared);

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(lockPath));

                oneOf(mockReq).getHeader("User-Agent");
                will(returnValue("Goliath"));

                oneOf(mockReq).getHeader("If");
                will(returnValue(null));

                StoredObject so = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, lockPath);
                will(returnValue(so));

                oneOf(mockReq).getInputStream();
                will(returnValue(dsisShared));

                oneOf(mockReq).getHeader("Depth");
                will(returnValue(depthString));

                oneOf(mockReq).getHeader("Timeout");
                will(returnValue(timeoutString));

                oneOf(mockRes).setStatus(WebdavStatus.SC_OK);

                oneOf(mockRes).setContentType("text/xml; charset=UTF-8");

                oneOf(mockRes).getWriter();
                will(returnValue(pw));

                // addHeader("Lock-Token", "(<opaquelocktoken:xxx-xxx-xxx>)")
                oneOf(mockRes).addHeader(with(any(String.class)),
                        with(any(String.class)));
            }
        });

        DoLock doLock = new DoLock(mockStore, resLocks, !readOnly);
        doLock.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoExclusiveLockOnCollection() throws Exception {

        final String lockPath = "/aFolderToLock";

        ResourceLocks resLocks = new ResourceLocks();

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
                will(returnValue(lockPath));

                oneOf(mockReq).getHeader("User-Agent");
                will(returnValue("Goliath"));

                oneOf(mockReq).getHeader("If");
                will(returnValue(null));

                StoredObject so = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, lockPath);
                will(returnValue(so));

                oneOf(mockReq).getInputStream();
                will(returnValue(dsisExclusive));

                oneOf(mockReq).getHeader("Depth");
                will(returnValue(depthString));

                oneOf(mockReq).getHeader("Timeout");
                will(returnValue(timeoutString));

                oneOf(mockRes).setStatus(WebdavStatus.SC_OK);

                oneOf(mockRes).setContentType("text/xml; charset=UTF-8");

                oneOf(mockRes).getWriter();
                will(returnValue(pw));

                // addHeader("Lock-Token", "(<opaquelocktoken:xxx-xxx-xxx>)")
                oneOf(mockRes).addHeader(with(any(String.class)),
                        with(any(String.class)));
            }
        });

        DoLock doLock = new DoLock(mockStore, resLocks, !readOnly);
        doLock.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoSharedLockOnCollection() throws Exception {

        final String lockPath = "/aFolderToLock";

        ResourceLocks resLocks = new ResourceLocks();
        final PrintWriter pw = new PrintWriter("/tmp/XMLTestFile");

        final ByteArrayInputStream baisShared = new ByteArrayInputStream(
                sharedLockRequestByteArray);
        final DelegatingServletInputStream dsisShared = new DelegatingServletInputStream(
                baisShared);

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(lockPath));

                oneOf(mockReq).getHeader("User-Agent");
                will(returnValue("Goliath"));

                oneOf(mockReq).getHeader("If");
                will(returnValue(null));

                StoredObject so = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, lockPath);
                will(returnValue(so));

                oneOf(mockReq).getInputStream();
                will(returnValue(dsisShared));

                oneOf(mockReq).getHeader("Depth");
                will(returnValue(depthString));

                oneOf(mockReq).getHeader("Timeout");
                will(returnValue(timeoutString));

                oneOf(mockRes).setStatus(WebdavStatus.SC_OK);

                oneOf(mockRes).setContentType("text/xml; charset=UTF-8");

                oneOf(mockRes).getWriter();
                will(returnValue(pw));

                // addHeader("Lock-Token", "(<opaquelocktoken:xxx-xxx-xxx>)")
                oneOf(mockRes).addHeader(with(any(String.class)),
                        with(any(String.class)));
            }
        });

        DoLock doLock = new DoLock(mockStore, resLocks, !readOnly);
        doLock.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoLockNullResourceLock() throws Exception {

        final String parentPath = "/parentCollection";
        final String lockPath = parentPath.concat("/aNullResource");

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
                will(returnValue(lockPath));

                LockedObject lockNullResourceLo = null;

                oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction,
                        lockPath);
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

                oneOf(mockStore).getStoredObject(mockTransaction, lockPath);
                will(returnValue(lockNullResourceSo));

                StoredObject parentSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(parentSo));

                oneOf(mockStore).createFolder(mockTransaction, parentPath);

                oneOf(mockStore).getStoredObject(mockTransaction, lockPath);
                will(returnValue(lockNullResourceSo));

                oneOf(mockStore).createResource(mockTransaction, lockPath);

                oneOf(mockRes).setStatus(WebdavStatus.SC_CREATED);

                lockNullResourceSo = initLockNullStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, lockPath);
                will(returnValue(lockNullResourceSo));

                oneOf(mockReq).getInputStream();
                will(returnValue(dsisExclusive));

                oneOf(mockReq).getHeader("Depth");
                will(returnValue(("0")));

                oneOf(mockReq).getHeader("Timeout");
                will(returnValue("Infinite"));

                ResourceLocks resLocks = ResourceLocks.class.newInstance();

                oneOf(mockResourceLocks).exclusiveLock(mockTransaction, lockPath,
                        "I'am the Lock Owner", 0, 604800);
                will(returnValue(true));

                lockNullResourceLo = initLockNullLockedObject(resLocks,
                        lockPath);

                oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction,
                        lockPath);
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
            }
        });

        DoLock doLock = new DoLock(mockStore, mockResourceLocks, !readOnly);
        doLock.execute(mockTransaction, mockReq, mockRes);

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

        DoLock doLock = new DoLock(mockStore, mockResourceLocks, !readOnly);
        assertThrows(PathTraversalException.class, () -> doLock.execute(mockTransaction, mockReq, mockRes));
        _mockery.assertIsSatisfied();
    }

    // ---------------------------------------------------------------------
    // Timeout header parsing
    // ---------------------------------------------------------------------

    @ParameterizedTest(name = "Timeout ''{0}'' -> {1} seconds")
    @CsvSource(
            value = {
                     "NONE, 3600",
                     "Second-600, 600",
                     "Second-999999999, 604800",
                     "Second-0, 3600",
                     "Second--5, 3600",
                     "'Second-120, Second-900', 120",
                     "Infinity, 604800",
                     // 'Infinite' is not recognised; Integer.parseInt fails and the code falls back to MAX_TIMEOUT
                     "Infinite, 604800",
                     "300, 300",
                     "garbage, 604800"
            }, nullValues = "NONE"
    )
    public void testDoLockTimeoutParsing(String timeoutHeader, int expectedSeconds) throws Exception {
        final String lockPath = "/aFileToLock";
        ResourceLocks resLocks = new ResourceLocks();

        expectRequest(lockPath, "Goliath", null, timeoutHeader, "0", exclusiveLockRequestByteArray);
        expectExistingResource(lockPath);
        allowXmlResponse();
        _mockery.checking(new Expectations() {
            {
                oneOf(mockRes).setStatus(WebdavStatus.SC_OK);
            }
        });

        new DoLock(mockStore, resLocks, !readOnly).execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
        LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction, lockPath);
        assertNotNull(lo);
        assertTimeoutAbout(expectedSeconds, lo);
        assertTrue(responseBody.toString().contains("<D:timeout>Second-"));
    }

    @Test
    public void testDoLockWithNonNumericSecondTimeoutThrowsNumberFormatException() throws Exception {
        // characterization: 'Second-abc' is not caught by DoLock.getTimeout and propagates to the caller
        final String lockPath = "/aFileToLock";
        ResourceLocks resLocks = new ResourceLocks();

        expectRequest(lockPath, "Goliath", null, "Second-abc", "0", exclusiveLockRequestByteArray);
        expectExistingResource(lockPath);
        allowXmlResponse();

        DoLock doLock = new DoLock(mockStore, resLocks, !readOnly);
        assertThrows(NumberFormatException.class, () -> doLock.execute(mockTransaction, mockReq, mockRes));

        _mockery.assertIsSatisfied();
        assertNull(resLocks.getLockedObjectByPath(mockTransaction, lockPath));
    }

    // ---------------------------------------------------------------------
    // Null-resource lock
    // ---------------------------------------------------------------------

    @Test
    public void testDoNullResourceLockCreatesResourceAndGrantsLock() throws Exception {
        final String parentPath = "/parentCollection";
        final String lockPath = parentPath + "/aNullResource";
        final StoredObject nullSo = initFileStoredObject(new byte[0]);
        ResourceLocks resLocks = new ResourceLocks();

        expectRequest(lockPath, "Goliath", null, "Second-600", "0", exclusiveLockRequestByteArray);
        allowXmlResponse();
        final Sequence status = _mockery.sequence("status");
        _mockery.checking(new Expectations() {
            {
                exactly(2).of(mockStore).getStoredObject(mockTransaction, lockPath);
                will(returnValue(null));
                oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(initFolderStoredObject()));
                oneOf(mockStore).createResource(mockTransaction, lockPath);
                oneOf(mockStore).getStoredObject(mockTransaction, lockPath);
                will(returnValue(nullSo));

                // characterization: 201 is set first but then overwritten by 200 from generateXMLReport
                oneOf(mockRes).setStatus(WebdavStatus.SC_CREATED);
                inSequence(status);
                oneOf(mockRes).setStatus(WebdavStatus.SC_OK);
                inSequence(status);
            }
        });

        new DoLock(mockStore, resLocks, !readOnly).execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
        assertTrue(nullSo.isNullResource());
        LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction, lockPath);
        assertNotNull(lo);
        assertTrue(lo.isExclusive());
        String xml = responseBody.toString();
        assertTrue(xml.contains("<D:exclusive/>"));
        assertTrue(xml.contains("opaquelocktoken:" + lo.getID()));
    }

    @Test
    public void testDoNullResourceLockWithMissingParentCreatesParentFolder() throws Exception {
        // characterization: a missing parent is created instead of answering 409 Conflict
        final String parentPath = "/missingParent";
        final String lockPath = parentPath + "/aNullResource";
        final StoredObject nullSo = initFileStoredObject(new byte[0]);
        ResourceLocks resLocks = new ResourceLocks();

        expectRequest(lockPath, "Goliath", null, null, "0", exclusiveLockRequestByteArray);
        allowXmlResponse();
        _mockery.checking(new Expectations() {
            {
                exactly(2).of(mockStore).getStoredObject(mockTransaction, lockPath);
                will(returnValue(null));
                oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(null));
                oneOf(mockStore).createFolder(mockTransaction, parentPath);
                oneOf(mockStore).createResource(mockTransaction, lockPath);
                oneOf(mockStore).getStoredObject(mockTransaction, lockPath);
                will(returnValue(nullSo));

                oneOf(mockRes).setStatus(WebdavStatus.SC_CREATED);
                oneOf(mockRes).setStatus(WebdavStatus.SC_OK);
                never(mockRes).sendError(with(any(Integer.class)));
            }
        });

        new DoLock(mockStore, resLocks, !readOnly).execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
        assertNotNull(resLocks.getLockedObjectByPath(mockTransaction, lockPath));
    }

    @Test
    public void testDoNullResourceLockWithResourceAsParentSendsPreconditionFailed() throws Exception {
        final String parentPath = "/aFile";
        final String lockPath = parentPath + "/aNullResource";
        ResourceLocks resLocks = new ResourceLocks();

        // the request body is never read on this path
        expectRequest(lockPath, "Goliath", null, null, "0", null);
        _mockery.checking(new Expectations() {
            {
                oneOf(mockStore).getStoredObject(mockTransaction, lockPath);
                will(returnValue(null));
                oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(initFileStoredObject(resourceContent)));

                oneOf(mockRes).sendError(WebdavStatus.SC_PRECONDITION_FAILED);
            }
        });

        new DoLock(mockStore, resLocks, !readOnly).execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
        assertNull(resLocks.getLockedObjectByPath(mockTransaction, lockPath));
    }

    @Test
    public void testDoNullResourceLockWhenResourceAppearsSendsLocked() throws Exception {
        final String parentPath = "/parentCollection";
        final String lockPath = parentPath + "/aNullResource";
        ResourceLocks resLocks = new ResourceLocks();

        // the request body is never read on this path
        expectRequest(lockPath, "Goliath", null, null, "0", null);
        _mockery.checking(new Expectations() {
            {
                oneOf(mockStore).getStoredObject(mockTransaction, lockPath);
                will(returnValue(null));
                oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(initFolderStoredObject()));
                // resource was created concurrently between the two lookups
                oneOf(mockStore).getStoredObject(mockTransaction, lockPath);
                will(returnValue(initFileStoredObject(resourceContent)));

                oneOf(mockRes).sendError(WebdavStatus.SC_LOCKED, "Locked");
            }
        });

        new DoLock(mockStore, resLocks, !readOnly).execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
        assertNull(resLocks.getLockedObjectByPath(mockTransaction, lockPath));
    }

    @Test
    public void testDoNullResourceLockWithTransmitUserAgentSendsNoContent() throws Exception {
        final String parentPath = "/parentCollection";
        final String lockPath = parentPath + "/aNullResource";
        final StoredObject nullSo = initFileStoredObject(new byte[0]);
        ResourceLocks resLocks = new ResourceLocks();

        expectRequest(lockPath, "Transmit/4.4.10", null, null, "0", exclusiveLockRequestByteArray);
        allowXmlResponse();
        final Sequence status = _mockery.sequence("status");
        _mockery.checking(new Expectations() {
            {
                exactly(2).of(mockStore).getStoredObject(mockTransaction, lockPath);
                will(returnValue(null));
                oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(initFolderStoredObject()));
                oneOf(mockStore).createResource(mockTransaction, lockPath);
                oneOf(mockStore).getStoredObject(mockTransaction, lockPath);
                will(returnValue(nullSo));

                // Transmit workaround: 204 instead of 201 (later overwritten by 200 from generateXMLReport)
                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);
                inSequence(status);
                oneOf(mockRes).setStatus(WebdavStatus.SC_OK);
                inSequence(status);
                never(mockRes).setStatus(WebdavStatus.SC_CREATED);
            }
        });

        new DoLock(mockStore, resLocks, !readOnly).execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
        assertTrue(nullSo.isNullResource());
        assertNotNull(resLocks.getLockedObjectByPath(mockTransaction, lockPath));
    }

    // ---------------------------------------------------------------------
    // Lock refresh
    // ---------------------------------------------------------------------

    @Test
    public void testDoRefreshLockWithValidTokenRefreshesTimeout() throws Exception {
        final String lockPath = "/aFileToRefresh";
        ResourceLocks resLocks = new ResourceLocks();
        resLocks.lock(mockTransaction, lockPath, "theOwner", true, 0, 10, false);
        final LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction, lockPath);
        final String ifHeader = "(<opaquelocktoken:" + lo.getID() + ">)";

        expectRequest(lockPath, "Goliath", ifHeader, "Second-600", null, null);
        // declared before allowXmlResponse() so it takes precedence over the generic 'allowing'
        _mockery.checking(new Expectations() {
            {
                oneOf(mockRes).setStatus(WebdavStatus.SC_OK);
                oneOf(mockRes).addHeader("Lock-Token", "<opaquelocktoken:" + lo.getID() + ">");
            }
        });
        allowXmlResponse();

        new DoLock(mockStore, resLocks, !readOnly).execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
        assertTimeoutAbout(600, lo);
        String xml = responseBody.toString();
        assertTrue(xml.contains("opaquelocktoken:" + lo.getID()));
        assertTrue(xml.contains("<D:timeout>Second-"));
        // characterization: the refresh response does not reflect the existing lock's type, scope and owner
        assertTrue(xml.contains("<D:null/>"));
        assertTrue(xml.contains("<D:shared/>"));
        assertFalse(xml.contains("theOwner"));
    }

    @Test
    public void testDoRefreshLockWithUnknownTokenSendsPreconditionFailed() throws Exception {
        final String lockPath = "/aFileToRefresh";
        ResourceLocks resLocks = new ResourceLocks();

        expectRequest(lockPath, "Goliath", "(<opaquelocktoken:unknown-token>)", null, null, null);
        _mockery.checking(new Expectations() {
            {
                oneOf(mockRes).sendError(WebdavStatus.SC_PRECONDITION_FAILED);
            }
        });

        new DoLock(mockStore, resLocks, !readOnly).execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoRefreshLockWithEmptyIfHeaderSendsPreconditionFailed() throws Exception {
        final String lockPath = "/aFileToRefresh";
        ResourceLocks resLocks = new ResourceLocks();

        expectRequest(lockPath, "Goliath", "", null, null, null);
        _mockery.checking(new Expectations() {
            {
                oneOf(mockRes).sendError(WebdavStatus.SC_PRECONDITION_FAILED);
            }
        });

        new DoLock(mockStore, resLocks, !readOnly).execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoLockOnExclusivelyLockedResourceWithWrongTokenSetsLocked() throws Exception {
        final String lockPath = "/aLockedFile";
        ResourceLocks resLocks = new ResourceLocks();
        resLocks.lock(mockTransaction, lockPath, "theOwner", true, 0, 600, false);

        expectRequest(lockPath, "Goliath", "(<opaquelocktoken:unknown-token>)", null, null, null);
        _mockery.checking(new Expectations() {
            {
                oneOf(mockRes).setStatus(WebdavStatus.SC_LOCKED);
            }
        });

        new DoLock(mockStore, resLocks, !readOnly).execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoLockOnResourceInExclusivelyLockedParentSetsLocked() throws Exception {
        final String parentPath = "/aLockedFolder";
        final String lockPath = parentPath + "/aFile";
        ResourceLocks resLocks = new ResourceLocks();
        resLocks.lock(mockTransaction, parentPath, "theOwner", true, 0, 600, false);

        expectRequest(lockPath, "Goliath", null, null, null, null);
        _mockery.checking(new Expectations() {
            {
                oneOf(mockRes).setStatus(WebdavStatus.SC_LOCKED);
            }
        });

        new DoLock(mockStore, resLocks, !readOnly).execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    // ---------------------------------------------------------------------
    // macOS (Darwin) workaround and lock failure reporting
    // ---------------------------------------------------------------------

    @Test
    public void testDoLockWithDarwinUserAgentGrantsExclusiveLock() throws Exception {
        final String lockPath = "/aFileToLock";
        ResourceLocks resLocks = new ResourceLocks();

        // no request body is read for macOS clients
        expectRequest(lockPath, DARWIN_USER_AGENT, null, null, "0", null);
        expectExistingResource(lockPath);
        allowXmlResponse();
        _mockery.checking(new Expectations() {
            {
                oneOf(mockRes).setStatus(WebdavStatus.SC_OK);
                never(mockReq).getInputStream();
            }
        });

        new DoLock(mockStore, resLocks, !readOnly).execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
        LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction, lockPath);
        assertNotNull(lo);
        assertTrue(lo.isExclusive());
        assertEquals(1, lo.getOwner().length);
        assertTrue(lo.getOwner()[0].startsWith(DARWIN_USER_AGENT));
        assertTimeoutAbout(3600, lo);
        String xml = responseBody.toString();
        assertTrue(xml.contains("<D:href>" + DARWIN_USER_AGENT));
        // characterization: lock type and scope are never parsed for macOS clients, so the response reports
        // locktype 'null' and scope 'shared' although an exclusive lock was granted
        assertTrue(xml.contains("<D:null/>"));
        assertTrue(xml.contains("<D:shared/>"));
        assertFalse(xml.contains("<D:exclusive/>"));
    }

    @Test
    public void testDoLockWithDarwinUserAgentOnSharedLockedResourceSendsLocked() throws Exception {
        final String lockPath = "/aSharedLockedFile";
        ResourceLocks resLocks = new ResourceLocks();
        resLocks.sharedLock(mockTransaction, lockPath, "otherOwner", 0, 600);

        expectRequest(lockPath, DARWIN_USER_AGENT, null, null, "0", null);
        expectExistingResource(lockPath);
        _mockery.checking(new Expectations() {
            {
                // characterization: single-entry error list is sent as plain 423, not as a 207 multistatus
                oneOf(mockRes).sendError(WebdavStatus.SC_LOCKED, "Locked");
                never(mockRes).setStatus(WebdavStatus.SC_MULTI_STATUS);
            }
        });

        new DoLock(mockStore, resLocks, !readOnly).execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
        LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction, lockPath);
        assertTrue(lo.isShared());
        assertArrayEquals(new String[]{"otherOwner"}, lo.getOwner());
    }

    @Test
    public void testDoExclusiveLockOnSharedLockedResourceSendsLockedTwice() throws Exception {
        final String lockPath = "/aSharedLockedFile";
        ResourceLocks resLocks = new ResourceLocks();
        resLocks.sharedLock(mockTransaction, lockPath, "otherOwner", 0, 600);

        expectRequest(lockPath, "Goliath", null, null, "0", exclusiveLockRequestByteArray);
        expectExistingResource(lockPath);
        _mockery.checking(new Expectations() {
            {
                // characterization: executeLock sends the error and throws LockFailedException,
                // which doLocking catches and reports a second time
                exactly(2).of(mockRes).sendError(WebdavStatus.SC_LOCKED, "Locked");
            }
        });

        new DoLock(mockStore, resLocks, !readOnly).execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
        assertArrayEquals(new String[]{"otherOwner"},
                resLocks.getLockedObjectByPath(mockTransaction, lockPath).getOwner());
    }

    @Test
    public void testDoLockWhenTemporaryLockCannotBeAcquiredDoesNothing() throws Exception {
        final String lockPath = "/aFileToLock";

        expectRequest(lockPath, "Goliath", null, null, null, null);
        _mockery.checking(new Expectations() {
            {
                allowing(mockResourceLocks).getLockedObjectByPath(with(any(ITransaction.class)),
                        with(any(String.class)));
                will(returnValue(null));
                oneOf(mockResourceLocks).lock(with(any(ITransaction.class)), with(lockPath),
                        with(any(String.class)), with(false), with(0), with(any(int.class)), with(true));
                will(returnValue(false));
                // no store or response interaction expected
            }
        });

        new DoLock(mockStore, mockResourceLocks, !readOnly).execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    // ---------------------------------------------------------------------
    // Request body parsing
    // ---------------------------------------------------------------------

    @Test
    public void testDoSharedLockGrantsSharedLockWithHrefOwner() throws Exception {
        final String lockPath = "/aFileToLock";
        ResourceLocks resLocks = new ResourceLocks();

        expectRequest(lockPath, "Goliath", null, null, "0", sharedLockRequestByteArray);
        expectExistingResource(lockPath);
        allowXmlResponse();
        _mockery.checking(new Expectations() {
            {
                oneOf(mockRes).setStatus(WebdavStatus.SC_OK);
            }
        });

        new DoLock(mockStore, resLocks, !readOnly).execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
        LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction, lockPath);
        assertNotNull(lo);
        assertTrue(lo.isShared());
        assertArrayEquals(new String[]{"I'am the Lock Owner"}, lo.getOwner());
        String xml = responseBody.toString();
        assertTrue(xml.contains("<D:shared/>"));
        assertTrue(xml.contains("<D:write/>"));
        assertTrue(xml.contains("<D:owner>"));
        assertTrue(xml.contains("Lock Owner</D:href>"));
    }

    @Test
    public void testDoLockWithPlainTextOwnerThrowsNullPointerException() throws Exception {
        // characterization: an owner given as plain text (no <href>) causes a NullPointerException
        // because DoLock.getLockInformation calls getFirstChild() on the text node
        final String lockPath = "/aFileToLock";
        ResourceLocks resLocks = new ResourceLocks();

        expectRequest(lockPath, "Goliath", null, null, "0",
                lockInfo("<D:lockscope><D:exclusive/></D:lockscope><D:locktype><D:write/></D:locktype>" +
                         "<D:owner>plain owner</D:owner>"));
        expectExistingResource(lockPath);

        DoLock doLock = new DoLock(mockStore, resLocks, !readOnly);
        assertThrows(NullPointerException.class, () -> doLock.execute(mockTransaction, mockReq, mockRes));

        _mockery.assertIsSatisfied();
        assertNull(resLocks.getLockedObjectByPath(mockTransaction, lockPath));
    }

    @ParameterizedTest(name = "[{index}] incomplete lock info")
    @CsvSource(
            delimiter = '|', value = {
                                      "<D:lockscope><D:exclusive/></D:lockscope><D:owner><D:href>o</D:href></D:owner>",
                                      "<D:lockscope><D:exclusive/></D:lockscope><D:locktype/><D:owner><D:href>o</D:href></D:owner>",
                                      "<D:locktype><D:write/></D:locktype><D:owner><D:href>o</D:href></D:owner>",
                                      "<D:lockscope/><D:locktype><D:write/></D:locktype><D:owner><D:href>o</D:href></D:owner>",
                                      "<D:lockscope><D:exclusive/></D:lockscope><D:locktype><D:write/></D:locktype>",
                                      "<!-- c --><D:lockscope><D:exclusive/></D:lockscope><D:locktype><D:write/></D:locktype>"
            }
    )
    public void testDoLockWithIncompleteLockInfoSendsBadRequest(String lockInfoContent)
                                                                                        throws Exception {
        final String lockPath = "/aFileToLock";
        ResourceLocks resLocks = new ResourceLocks();

        expectRequest(lockPath, "Goliath", null, null, "0", lockInfo(lockInfoContent));
        expectExistingResource(lockPath);
        _mockery.checking(new Expectations() {
            {
                oneOf(mockRes).setContentType("text/xml; charset=UTF-8");
                oneOf(mockRes).sendError(WebdavStatus.SC_BAD_REQUEST);
            }
        });

        new DoLock(mockStore, resLocks, !readOnly).execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
        assertNull(resLocks.getLockedObjectByPath(mockTransaction, lockPath));
    }

    @Test
    public void testDoLockWithMalformedBodySendsErrorAndCreatesNoLock() throws Exception {
        final String lockPath = "/aFileToLock";
        ResourceLocks resLocks = new ResourceLocks();

        expectRequest(lockPath, "Goliath", null, null, "0",
                "<D:lockinfo xmlns:D='DAV:'><D:lockscope>".getBytes(StandardCharsets.UTF_8));
        expectExistingResource(lockPath);
        final Sequence errors = _mockery.sequence("errors");
        _mockery.checking(new Expectations() {
            {
                // characterization: the SAXException results in a 500, followed by a 400 for the same request
                oneOf(mockRes).sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
                inSequence(errors);
                oneOf(mockRes).setContentType("text/xml; charset=UTF-8");
                oneOf(mockRes).sendError(WebdavStatus.SC_BAD_REQUEST);
                inSequence(errors);
            }
        });

        new DoLock(mockStore, resLocks, !readOnly).execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
        assertNull(resLocks.getLockedObjectByPath(mockTransaction, lockPath));
    }

    @Test
    public void testDoLockWithDoctypeInBodyIsRejected() throws Exception {
        final String lockPath = "/aFileToLock";
        ResourceLocks resLocks = new ResourceLocks();

        expectRequest(lockPath, "Goliath", null, null, "0",
                ("<?xml version=\"1.0\"?><!DOCTYPE foo [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>" +
                 "<D:lockinfo xmlns:D='DAV:'><D:owner><D:href>&xxe;</D:href></D:owner></D:lockinfo>")
                         .getBytes(StandardCharsets.UTF_8));
        expectExistingResource(lockPath);
        _mockery.checking(new Expectations() {
            {
                oneOf(mockRes).sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
                oneOf(mockRes).setContentType("text/xml; charset=UTF-8");
                oneOf(mockRes).sendError(WebdavStatus.SC_BAD_REQUEST);
            }
        });

        new DoLock(mockStore, resLocks, !readOnly).execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
        assertNull(resLocks.getLockedObjectByPath(mockTransaction, lockPath));
    }

    // ---------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------

    private static byte[] lockInfo(String content) {
        return ("<?xml version=\"1.0\" encoding=\"utf-8\" ?><D:lockinfo xmlns:D='DAV:'>" + content + "</D:lockinfo>")
                .getBytes(StandardCharsets.UTF_8);
    }

    private static void assertTimeoutAbout(int expectedSeconds, LockedObject lo) {
        long millis = lo.getTimeoutMillis();
        assertTrue(millis <= expectedSeconds * 1000L && millis > expectedSeconds * 1000L - 10_000L,
                "expected timeout of about " + expectedSeconds + "s but was " + millis + "ms");
    }

    private void expectRequest(
            final String path,
            final String userAgent,
            final String ifHeader,
            final String timeout,
            final String depthHeader,
            final byte[] body
    ) throws Exception {
        _mockery.checking(new Expectations() {
            {
                allowing(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));
                allowing(mockReq).getPathInfo();
                will(returnValue(path));
                allowing(mockReq).getHeader("User-Agent");
                will(returnValue(userAgent));
                allowing(mockReq).getHeader("If");
                will(returnValue(ifHeader));
                allowing(mockReq).getHeader("Timeout");
                will(returnValue(timeout));
                allowing(mockReq).getHeader("Depth");
                will(returnValue(depthHeader));
                if (body != null) {
                    oneOf(mockReq).getInputStream();
                    will(returnValue(new DelegatingServletInputStream(new ByteArrayInputStream(body))));
                }
            }
        });
    }

    private void expectExistingResource(final String path) {
        _mockery.checking(new Expectations() {
            {
                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(initFileStoredObject(resourceContent)));
            }
        });
    }

    private void allowXmlResponse() throws Exception {
        responseBody = new StringWriter();
        final PrintWriter pw = new PrintWriter(responseBody);
        _mockery.checking(new Expectations() {
            {
                allowing(mockRes).setContentType("text/xml; charset=UTF-8");
                allowing(mockRes).getWriter();
                will(returnValue(pw));
                allowing(mockRes).addHeader(with("Lock-Token"), with(any(String.class)));
            }
        });
    }
}
