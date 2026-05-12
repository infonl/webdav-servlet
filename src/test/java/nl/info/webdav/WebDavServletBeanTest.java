/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.webdav;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.security.Principal;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import nl.info.webdav.exceptions.PathTraversalException;
import nl.info.webdav.exceptions.UnauthenticatedException;
import nl.info.webdav.testutil.MockTest;

public class WebDavServletBeanTest extends MockTest {

    static IWebdavStore mockStore;
    static HttpServletRequest mockReq;
    static HttpServletResponse mockRes;
    static ITransaction mockTransaction;
    static IMethodExecutor mockExecutor;

    @BeforeAll
    public static void setUp() {
        mockStore = _mockery.mock(IWebdavStore.class);
        mockReq = _mockery.mock(HttpServletRequest.class);
        mockRes = _mockery.mock(HttpServletResponse.class);
        mockTransaction = _mockery.mock(ITransaction.class);
        mockExecutor = _mockery.mock(IMethodExecutor.class);
    }

    private static class TestBean extends WebDavServletBean {
        TestBean(IWebdavStore store, IMethodExecutor executor) {
            _store = store;
            register("GET", executor);
        }

        TestBean(IWebdavStore store, String method, IMethodExecutor executor) {
            _store = store;
            register(method, executor);
        }

        void callService(HttpServletRequest req, HttpServletResponse resp)
                                                                           throws IOException, ServletException {
            service(req, resp);
        }
    }

    @Test
    public void testServicePathTraversalExceptionSends400() throws Exception {
        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getMethod();
                will(returnValue("GET"));

                oneOf(mockReq).getUserPrincipal();
                will(returnValue(null));

                oneOf(mockStore).begin(null);
                will(returnValue(mockTransaction));

                oneOf(mockStore).checkAuthentication(mockTransaction);

                oneOf(mockRes).setStatus(WebdavStatus.SC_OK);

                oneOf(mockExecutor).execute(mockTransaction, mockReq, mockRes);
                will(throwException(new PathTraversalException("/safe/../../etc/passwd")));

                oneOf(mockRes).isCommitted();
                will(returnValue(false));

                oneOf(mockRes).sendError(WebdavStatus.SC_BAD_REQUEST);

                oneOf(mockStore).rollback(mockTransaction);
            }
        });

        new TestBean(mockStore, mockExecutor).callService(mockReq, mockRes);
        _mockery.assertIsSatisfied();
    }

    @Test
    public void testServiceUnauthenticatedExceptionSends403() throws Exception {
        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getMethod();
                will(returnValue("GET"));

                oneOf(mockReq).getUserPrincipal();
                will(returnValue(null));

                oneOf(mockStore).begin(null);
                will(returnValue(mockTransaction));

                oneOf(mockStore).checkAuthentication(mockTransaction);
                will(throwException(new UnauthenticatedException()));

                oneOf(mockRes).isCommitted();
                will(returnValue(false));

                oneOf(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);

                oneOf(mockStore).rollback(mockTransaction);
            }
        });

        new TestBean(mockStore, mockExecutor).callService(mockReq, mockRes);
        _mockery.assertIsSatisfied();
    }

    @Test
    public void testServiceSuccessfulExecutionCommitsAndDoesNotRollback() throws Exception {
        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getMethod();
                will(returnValue("GET"));

                oneOf(mockReq).getUserPrincipal();
                will(returnValue(null));

                oneOf(mockStore).begin(null);
                will(returnValue(mockTransaction));

                oneOf(mockStore).checkAuthentication(mockTransaction);

                oneOf(mockRes).setStatus(WebdavStatus.SC_OK);

                oneOf(mockExecutor).execute(mockTransaction, mockReq, mockRes);

                oneOf(mockStore).commit(mockTransaction);

                oneOf(mockReq).getContentLength();
                will(returnValue(0));

                never(mockStore).rollback(mockTransaction);
            }
        });

        new TestBean(mockStore, mockExecutor).callService(mockReq, mockRes);
        _mockery.assertIsSatisfied();
    }

    @Test
    public void testServiceRuntimeExceptionThrowsServletExceptionAndRollsBack() throws Exception {
        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getMethod();
                will(returnValue("GET"));

                oneOf(mockReq).getUserPrincipal();
                will(returnValue(null));

                oneOf(mockStore).begin(null);
                will(returnValue(mockTransaction));

                oneOf(mockStore).checkAuthentication(mockTransaction);

                oneOf(mockRes).setStatus(WebdavStatus.SC_OK);

                oneOf(mockExecutor).execute(mockTransaction, mockReq, mockRes);
                will(throwException(new RuntimeException("unexpected")));

                oneOf(mockStore).rollback(mockTransaction);
            }
        });

        assertThrows(
                ServletException.class,
                () -> new TestBean(mockStore, mockExecutor).callService(mockReq, mockRes)
        );
        _mockery.assertIsSatisfied();
    }

    @Test
    public void testServiceIOExceptionRollsBackAndThrowsServletException() throws Exception {
        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getMethod();
                will(returnValue("GET"));

                oneOf(mockReq).getUserPrincipal();
                will(returnValue(null));

                oneOf(mockStore).begin(null);
                will(returnValue(mockTransaction));

                oneOf(mockStore).checkAuthentication(mockTransaction);

                oneOf(mockRes).setStatus(WebdavStatus.SC_OK);

                oneOf(mockExecutor).execute(mockTransaction, mockReq, mockRes);
                will(throwException(new IOException("disk error")));

                oneOf(mockRes).isCommitted();
                will(returnValue(false));

                oneOf(mockRes).sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);

                // rollback called once explicitly in the IOException catch block,
                // then again via the finally block (needRollback remains true)
                exactly(2).of(mockStore).rollback(mockTransaction);
            }
        });

        assertThrows(
                ServletException.class,
                () -> new TestBean(mockStore, mockExecutor).callService(mockReq, mockRes)
        );
        _mockery.assertIsSatisfied();
    }

    @Test
    public void testServicePathTraversalAlreadyCommittedSkipsSendError() throws Exception {
        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getMethod();
                will(returnValue("GET"));

                oneOf(mockReq).getUserPrincipal();
                will(returnValue(null));

                oneOf(mockStore).begin(null);
                will(returnValue(mockTransaction));

                oneOf(mockStore).checkAuthentication(mockTransaction);

                oneOf(mockRes).setStatus(WebdavStatus.SC_OK);

                oneOf(mockExecutor).execute(mockTransaction, mockReq, mockRes);
                will(throwException(new PathTraversalException("/bad/path")));

                oneOf(mockRes).isCommitted();
                will(returnValue(true));

                never(mockRes).sendError(with(any(Integer.class)));

                oneOf(mockStore).rollback(mockTransaction);
            }
        });

        new TestBean(mockStore, mockExecutor).callService(mockReq, mockRes);
        _mockery.assertIsSatisfied();
    }

    @Test
    public void testServiceWithUnconsumedInputStreamDrainsBytes() throws Exception {
        AtomicInteger availableCallCount = new AtomicInteger(0);
        ServletInputStream stream = new ServletInputStream() {
            @Override
            public boolean isFinished() { return false; }
            @Override
            public boolean isReady() { return true; }
            @Override
            public void setReadListener(ReadListener readListener) {}
            @Override
            public int read() { return -1; }
            @Override
            public int available() { return availableCallCount.getAndIncrement() == 0 ? 5 : 0; }
            @Override
            public long skip(long n) { return n; }
        };

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getMethod();
                will(returnValue("GET"));

                oneOf(mockReq).getUserPrincipal();
                will(returnValue(null));

                oneOf(mockStore).begin(null);
                will(returnValue(mockTransaction));

                oneOf(mockStore).checkAuthentication(mockTransaction);

                oneOf(mockRes).setStatus(WebdavStatus.SC_OK);

                oneOf(mockExecutor).execute(mockTransaction, mockReq, mockRes);

                oneOf(mockStore).commit(mockTransaction);

                oneOf(mockReq).getContentLength();
                will(returnValue(100));

                exactly(4).of(mockReq).getInputStream();
                will(returnValue(stream));

                never(mockStore).rollback(mockTransaction);
            }
        });

        new TestBean(mockStore, mockExecutor).callService(mockReq, mockRes);
        _mockery.assertIsSatisfied();
    }

    @Test
    public void testServiceContentLengthNonZeroButStreamFinishedSkipsDrain() throws Exception {
        ServletInputStream finishedStream = new ServletInputStream() {
            @Override
            public boolean isFinished() { return true; }
            @Override
            public boolean isReady() { return true; }
            @Override
            public void setReadListener(ReadListener readListener) {}
            @Override
            public int read() { return -1; }
        };

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getMethod();
                will(returnValue("GET"));

                oneOf(mockReq).getUserPrincipal();
                will(returnValue(null));

                oneOf(mockStore).begin(null);
                will(returnValue(mockTransaction));

                oneOf(mockStore).checkAuthentication(mockTransaction);

                oneOf(mockRes).setStatus(WebdavStatus.SC_OK);

                oneOf(mockExecutor).execute(mockTransaction, mockReq, mockRes);

                oneOf(mockStore).commit(mockTransaction);

                oneOf(mockReq).getContentLength();
                will(returnValue(100));

                oneOf(mockReq).getInputStream();
                will(returnValue(finishedStream));

                never(mockStore).rollback(mockTransaction);
            }
        });

        new TestBean(mockStore, mockExecutor).callService(mockReq, mockRes);
        _mockery.assertIsSatisfied();
    }

    @Test
    public void testServiceIOExceptionAlreadyCommittedSkipsSendError() throws Exception {
        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getMethod();
                will(returnValue("GET"));

                oneOf(mockReq).getUserPrincipal();
                will(returnValue(null));

                oneOf(mockStore).begin(null);
                will(returnValue(mockTransaction));

                oneOf(mockStore).checkAuthentication(mockTransaction);

                oneOf(mockRes).setStatus(WebdavStatus.SC_OK);

                oneOf(mockExecutor).execute(mockTransaction, mockReq, mockRes);
                will(throwException(new IOException("disk error")));

                oneOf(mockRes).isCommitted();
                will(returnValue(true));

                never(mockRes).sendError(with(any(Integer.class)));

                // IOException handler calls rollback, then finally block calls it again
                // because needRollback is still true (the ServletException re-throw
                // is caught by the outer catch(Exception) block)
                exactly(2).of(mockStore).rollback(mockTransaction);
            }
        });

        assertThrows(
                ServletException.class,
                () -> new TestBean(mockStore, mockExecutor).callService(mockReq, mockRes)
        );
        _mockery.assertIsSatisfied();
    }

    @Test
    public void testServiceUnauthenticatedAlreadyCommittedSkipsSendError() throws Exception {
        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getMethod();
                will(returnValue("GET"));

                oneOf(mockReq).getUserPrincipal();
                will(returnValue(null));

                oneOf(mockStore).begin(null);
                will(returnValue(mockTransaction));

                oneOf(mockStore).checkAuthentication(mockTransaction);
                will(throwException(new UnauthenticatedException()));

                oneOf(mockRes).isCommitted();
                will(returnValue(true));

                never(mockRes).sendError(with(any(Integer.class)));

                oneOf(mockStore).rollback(mockTransaction);
            }
        });

        new TestBean(mockStore, mockExecutor).callService(mockReq, mockRes);
        _mockery.assertIsSatisfied();
    }

    @Test
    public void testServicePassesPrincipalToStoreBegin() throws Exception {
        Principal principal = _mockery.mock(Principal.class);

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getMethod();
                will(returnValue("GET"));

                oneOf(mockReq).getUserPrincipal();
                will(returnValue(principal));

                oneOf(mockStore).begin(principal);
                will(returnValue(mockTransaction));

                oneOf(mockStore).checkAuthentication(mockTransaction);

                oneOf(mockRes).setStatus(WebdavStatus.SC_OK);

                oneOf(mockExecutor).execute(mockTransaction, mockReq, mockRes);

                oneOf(mockStore).commit(mockTransaction);

                oneOf(mockReq).getContentLength();
                will(returnValue(0));

                never(mockStore).rollback(mockTransaction);
            }
        });

        new TestBean(mockStore, mockExecutor).callService(mockReq, mockRes);
        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDestroyCallsStoreDestroy() {
        _mockery.checking(new Expectations() {
            {
                oneOf(mockStore).destroy();
            }
        });

        new TestBean(mockStore, mockExecutor).destroy();
        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDestroyWithNullStoreDoesNotThrow() {
        assertDoesNotThrow(() -> new WebDavServletBean().destroy());
    }

    @Test
    public void testServiceUnknownMethodFallsBackToNoImpl() throws Exception {
        IMethodExecutor noImplExecutor = _mockery.mock(IMethodExecutor.class, "noImplExecutor");

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getMethod();
                will(returnValue("SEARCH"));

                oneOf(mockReq).getUserPrincipal();
                will(returnValue(null));

                oneOf(mockStore).begin(null);
                will(returnValue(mockTransaction));

                oneOf(mockStore).checkAuthentication(mockTransaction);

                oneOf(mockRes).setStatus(WebdavStatus.SC_OK);

                oneOf(noImplExecutor).execute(mockTransaction, mockReq, mockRes);

                oneOf(mockStore).commit(mockTransaction);

                oneOf(mockReq).getContentLength();
                will(returnValue(0));
            }
        });

        new TestBean(mockStore, "*NO*IMPL*", noImplExecutor).callService(mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }
}
