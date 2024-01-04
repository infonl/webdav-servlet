package net.sf.webdav.methods;

import net.sf.webdav.IMimeTyper;
import net.sf.webdav.ITransaction;
import net.sf.webdav.IWebdavStore;
import net.sf.webdav.StoredObject;
import net.sf.webdav.exceptions.LockFailedException;
import net.sf.webdav.locking.ResourceLocks;
import net.sf.webdav.testutil.MockTest;
import org.jmock.Expectations;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class DoOptionsTest extends MockTest {

    static IWebdavStore mockStore;
    static HttpServletRequest mockReq;
    static HttpServletResponse mockRes;
    static IMimeTyper mockMimeTyper;
    static ITransaction mockTransaction;
    static byte[] resourceContent = new byte[] { '<', 'h', 'e', 'l', 'l', 'o',
            '/', '>' };

    @BeforeAll
    public static void setUp() throws Exception {
        mockStore = _mockery.mock(IWebdavStore.class);
        mockMimeTyper = _mockery.mock(IMimeTyper.class);
        mockReq = _mockery.mock(HttpServletRequest.class);
        mockRes = _mockery.mock(HttpServletResponse.class);
        mockTransaction = _mockery.mock(ITransaction.class);
    }

    @Test
    public void testOptionsOnExistingNode() throws IOException,
            LockFailedException {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/index.html"));

                oneOf(mockRes).addHeader("DAV", "1, 2");

                StoredObject indexSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, "/index.html");
                will(returnValue(indexSo));

                oneOf(mockRes).addHeader(
                        "Allow",
                        "OPTIONS, GET, HEAD, POST, DELETE, "
                                + "TRACE, PROPPATCH, COPY, "
                                + "MOVE, LOCK, UNLOCK, PROPFIND");

                oneOf(mockRes).addHeader("MS-Author-Via", "DAV");
            }
        });

        DoOptions doOptions = new DoOptions(mockStore, new ResourceLocks());
        doOptions.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testOptionsOnNonExistingNode() throws IOException,
            LockFailedException {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/index.html"));

                oneOf(mockRes).addHeader("DAV", "1, 2");

                StoredObject indexSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, "/index.html");
                will(returnValue(indexSo));

                oneOf(mockRes).addHeader("Allow", "OPTIONS, MKCOL, PUT");

                oneOf(mockRes).addHeader("MS-Author-Via", "DAV");
            }
        });

        DoOptions doOptions = new DoOptions(mockStore, new ResourceLocks());
        doOptions.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

}
