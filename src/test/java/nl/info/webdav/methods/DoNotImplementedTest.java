package nl.info.webdav.methods;

import nl.info.webdav.ITransaction;
import nl.info.webdav.IWebdavStore;
import nl.info.webdav.WebdavStatus;
import nl.info.webdav.testutil.MockTest;
import org.jmock.Expectations;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class DoNotImplementedTest extends MockTest {

    static IWebdavStore mockStore;
    static HttpServletRequest mockReq;
    static HttpServletResponse mockRes;
    static ITransaction mockTransaction;

    @BeforeAll
    public static void setUp() throws Exception {
        mockStore = _mockery.mock(IWebdavStore.class);
        mockReq = _mockery.mock(HttpServletRequest.class);
        mockRes = _mockery.mock(HttpServletResponse.class);
        mockTransaction = _mockery.mock(ITransaction.class);
    }

    @Test
    public void testDoNotImplementedIfReadOnlyTrue() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getMethod();
                will(returnValue("notImplementedMethod"));
                oneOf(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
            }
        });

        DoNotImplemented doNotImplemented = new DoNotImplemented(readOnly);
        doNotImplemented.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoNotImplementedIfReadOnlyFalse() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getMethod();
                will(returnValue("notImplementedMethod"));
                oneOf(mockRes).sendError(WebdavStatus.SC_NOT_IMPLEMENTED);
            }
        });

        DoNotImplemented doNotImplemented = new DoNotImplemented(!readOnly);
        doNotImplemented.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }
}
