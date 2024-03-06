package nl.info.webdav.methods;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;

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

}
