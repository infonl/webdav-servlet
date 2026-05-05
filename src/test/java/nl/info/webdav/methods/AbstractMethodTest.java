// SPDX-FileCopyrightText: 2026 INFO.nl
// SPDX-License-Identifier: EUPL-1.2+
package nl.info.webdav.methods;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
import java.util.Date;
import java.util.Hashtable;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXParseException;

import nl.info.webdav.ITransaction;
import nl.info.webdav.StoredObject;
import nl.info.webdav.exceptions.LockFailedException;
import nl.info.webdav.locking.IResourceLocks;
import nl.info.webdav.locking.LockedObject;
import nl.info.webdav.locking.ResourceLocks;
import nl.info.webdav.testutil.MockTest;

public class AbstractMethodTest extends MockTest {

    private static class TestableMethod extends AbstractMethod {
        @Override
        public void execute(ITransaction transaction, HttpServletRequest req, HttpServletResponse resp)
                                                                                                        throws LockFailedException {
        }
    }

    private final TestableMethod method = new TestableMethod();

    static ITransaction mockTransaction;
    static HttpServletRequest mockReq;
    static HttpServletResponse mockResp;
    static IResourceLocks mockResourceLocks;

    @BeforeAll
    public static void setUp() {
        mockTransaction = _mockery.mock(ITransaction.class);
        mockReq = _mockery.mock(HttpServletRequest.class);
        mockResp = _mockery.mock(HttpServletResponse.class);
        mockResourceLocks = _mockery.mock(IResourceLocks.class);
    }

    @Test
    public void testGetParentPathMultiSegment() {
        assertEquals("/foo/bar", method.getParentPath("/foo/bar/baz"));
    }

    @Test
    public void testGetParentPathSingleSegment() {
        assertEquals("", method.getParentPath("/foo"));
    }

    @Test
    public void testGetParentPathNoSlash() {
        assertNull(method.getParentPath("foo"));
    }

    @Test
    public void testGetCleanPathRemovesTrailingSlash() {
        assertEquals("/foo/bar", method.getCleanPath("/foo/bar/"));
    }

    @Test
    public void testGetCleanPathRootUnchanged() {
        assertEquals("/", method.getCleanPath("/"));
    }

    @Test
    public void testGetDepthZero() {
        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getHeader("Depth");
                will(returnValue("0"));
            }
        });
        assertEquals(0, method.getDepth(mockReq));
        _mockery.assertIsSatisfied();
    }

    @Test
    public void testGetDepthOne() {
        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getHeader("Depth");
                will(returnValue("1"));
            }
        });
        assertEquals(1, method.getDepth(mockReq));
        _mockery.assertIsSatisfied();
    }

    @Test
    public void testGetDepthInfinity() {
        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getHeader("Depth");
                will(returnValue("infinity"));
            }
        });
        assertEquals(AbstractMethod.INFINITY, method.getDepth(mockReq));
        _mockery.assertIsSatisfied();
    }

    @Test
    public void testGetDepthAbsent() {
        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getHeader("Depth");
                will(returnValue(null));
            }
        });
        assertEquals(AbstractMethod.INFINITY, method.getDepth(mockReq));
        _mockery.assertIsSatisfied();
    }

    @Test
    public void testGetETagForResource() {
        StoredObject so = new StoredObject();
        so.setFolder(false);
        so.setResourceLength(100L);
        so.setLastModified(new Date(1000L));
        assertEquals("W/\"100-1000\"", method.getETag(so));
    }

    @Test
    public void testGetETagForNull() {
        assertEquals("W/\"-\"", method.getETag(null));
    }

    @Test
    public void testGetLockIdFromIfHeaderSingleToken() {
        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getHeader("If");
                will(returnValue("(<opaquelocktoken:abc-123>)"));
            }
        });
        String[] ids = method.getLockIdFromIfHeader(mockReq);
        assertNotNull(ids);
        assertEquals("abc-123", ids[0]);
        assertNull(ids[1]);
        _mockery.assertIsSatisfied();
    }

    @Test
    public void testGetLockIdFromIfHeaderAbsent() {
        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getHeader("If");
                will(returnValue(null));
            }
        });
        assertNull(method.getLockIdFromIfHeader(mockReq));
        _mockery.assertIsSatisfied();
    }

    @Test
    public void testGetLockIdFromLockTokenHeader() {
        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getHeader("Lock-Token");
                will(returnValue("<opaquelocktoken:abc-123>"));
            }
        });
        assertEquals("abc-123", method.getLockIdFromLockTokenHeader(mockReq));
        _mockery.assertIsSatisfied();
    }

    @Test
    public void testCheckLocksReturnsTrueWhenNoLockAtPath() {
        _mockery.checking(new Expectations() {
            {
                oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction, "/foo");
                will(returnValue(null));
            }
        });
        assertTrue(method.checkLocks(mockTransaction, mockReq, mockResourceLocks, "/foo"));
        _mockery.assertIsSatisfied();
    }

    @Test
    public void testCheckLocksReturnsTrueForSharedLock() {
        ResourceLocks realLocks = new ResourceLocks();
        LockedObject sharedLo = new LockedObject(realLocks, "/testshared", false);
        sharedLo.setExclusive(false);
        _mockery.checking(new Expectations() {
            {
                oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction, "/shared");
                will(returnValue(sharedLo));
            }
        });
        assertTrue(method.checkLocks(mockTransaction, mockReq, mockResourceLocks, "/shared"));
        _mockery.assertIsSatisfied();
    }

    @Test
    public void testGetDocumentBuilderRejectsDoctypeDeclaration() {
        String xmlWithDoctype = "<?xml version=\"1.0\"?><!DOCTYPE foo [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]><foo>&xxe;</foo>";
        ByteArrayInputStream input = new ByteArrayInputStream(xmlWithDoctype.getBytes(StandardCharsets.UTF_8));
        assertThrows(SAXParseException.class, () -> method.getDocumentBuilder().parse(input));
    }

    @Test
    public void testGetDocumentBuilderParsesWellFormedXml() {
        String xml = "<?xml version=\"1.0\"?><propfind xmlns=\"DAV:\"><allprop/></propfind>";
        ByteArrayInputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        assertDoesNotThrow(() -> assertNotNull(method.getDocumentBuilder().parse(input)));
    }

    @Test
    public void testSendReportEscapesAmpersandInMultiStatusPath() throws Exception {
        StringWriter sw = new StringWriter();
        _mockery.checking(new Expectations() {
            {
                oneOf(mockResp).setStatus(207);
                oneOf(mockResp).setContentType("text/xml; charset=UTF-8");
                oneOf(mockResp).getWriter();
                will(returnValue(new PrintWriter(sw)));
            }
        });
        Hashtable<String, Integer> errors = new Hashtable<>();
        errors.put("/path/with&ampersand", 423);
        errors.put("/other/path", 423);
        method.sendReport(mockResp, errors);
        String output = sw.toString();
        assertTrue(output.contains("&amp;"), "Expected &amp; in: " + output);
        assertDoesNotThrow(
                () -> method.getDocumentBuilder().parse(new ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8))),
                "Response must be well-formed XML"
        );
        _mockery.assertIsSatisfied();
    }

    @Test
    public void testSendReportEscapesLessThanInMultiStatusPath() throws Exception {
        StringWriter sw = new StringWriter();
        _mockery.checking(new Expectations() {
            {
                oneOf(mockResp).setStatus(207);
                oneOf(mockResp).setContentType("text/xml; charset=UTF-8");
                oneOf(mockResp).getWriter();
                will(returnValue(new PrintWriter(sw)));
            }
        });
        Hashtable<String, Integer> errors = new Hashtable<>();
        errors.put("/path/with<tag>", 423);
        errors.put("/other/path", 423);
        method.sendReport(mockResp, errors);
        String output = sw.toString();
        assertTrue(output.contains("&lt;"), "Expected &lt; in: " + output);
        assertDoesNotThrow(
                () -> method.getDocumentBuilder().parse(new ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8))),
                "Response must be well-formed XML"
        );
        _mockery.assertIsSatisfied();
    }

    @Test
    public void testCheckLocksReturnsFalseForExclusiveLockWithWrongToken() {
        ResourceLocks realLocks = new ResourceLocks();
        LockedObject exclusiveLo = new LockedObject(realLocks, "/testexcl", false);
        exclusiveLo.setExclusive(true);
        exclusiveLo.addLockedObjectOwner("owner");
        _mockery.checking(new Expectations() {
            {
                oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction, "/excl");
                will(returnValue(exclusiveLo));
                oneOf(mockReq).getHeader("If");
                will(returnValue("(<opaquelocktoken:wrong-token>)"));
                oneOf(mockResourceLocks).getLockedObjectByID(mockTransaction, "wrong-token");
                will(returnValue(null));
            }
        });
        assertFalse(method.checkLocks(mockTransaction, mockReq, mockResourceLocks, "/excl"));
        _mockery.assertIsSatisfied();
    }
}
