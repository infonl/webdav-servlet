/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.webdav;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;

import org.jmock.Expectations;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import nl.info.webdav.exceptions.WebdavException;
import nl.info.webdav.methods.DoHead;
import nl.info.webdav.methods.DoPut;
import nl.info.webdav.testutil.MockPrincipal;
import nl.info.webdav.testutil.MockTest;
// TODO: create our own versions?
// import org.springframework.mock.web.MockHttpServletRequest;
// import org.springframework.mock.web.MockHttpServletResponse;
// import org.springframework.mock.web.MockHttpSession;
// import org.springframework.mock.web.MockServletConfig;
// import org.springframework.mock.web.MockServletContext;

public class WebdavServletTest extends MockTest {

    static ServletConfig servletConfig;
    static ServletContext servletContext;
    static IWebdavStore mockStore;

    // TODO:
    //    static MockServletConfig mockServletConfig;
    //    static MockServletContext mockServletContext;
    //    static MockHttpServletRequest mockReq;
    //    static MockHttpServletResponse mockRes;
    //    static MockHttpSession mockHttpSession;
    static MockPrincipal mockPrincipal;
    static ITransaction mockTransaction;

    static boolean readOnly = true;
    static byte[] resourceContent = new byte[]{'<', 'h', 'e', 'l', 'l', 'o',
                                               '/', '>'};
    static String dftIndexFile = "/index.html";
    static String insteadOf404 = "/insteadOf404";

    @BeforeAll
    public static void setUp() throws Exception {
        servletConfig = _mockery.mock(ServletConfig.class);
        servletContext = _mockery.mock(ServletContext.class);
        mockStore = _mockery.mock(IWebdavStore.class);

        // TODO:
        //        mockServletConfig = new MockServletConfig(mockServletContext);
        //        mockHttpSession = new MockHttpSession(mockServletContext);
        //        mockServletContext = new MockServletContext();
        //        mockReq = new MockHttpServletRequest(mockServletContext);
        //        mockRes = new MockHttpServletResponse();

        mockPrincipal = new MockPrincipal("Admin", new String[]{"Admin",
                                                                "Manager"});

        mockTransaction = _mockery.mock(ITransaction.class);
    }

    @Test
    public void testInit() throws Exception {

        _mockery.checking(new Expectations() {
        });

        WebDavServletBean servlet = new WebdavServlet();
        servlet.init(mockStore, dftIndexFile, insteadOf404, 1, true);

        _mockery.assertIsSatisfied();
    }

    // Test successes in eclipse, but fails in "mvn test"
    // first three expectations aren't successful with "mvn test"
    //    @Test
    //    public void testInitGenericServlet() throws Exception {
    //
    //        _mockery.checking(new Expectations() {
    //            {
    //                allowing(servletConfig).getServletContext();
    //                will(returnValue(mockServletContext));
    //
    //                allowing(servletConfig).getServletName();
    //                will(returnValue("webdav-servlet"));
    //
    //                allowing(servletContext).log("webdav-servlet: init");
    //
    //                one(servletConfig).getInitParameter(
    //                        "ResourceHandlerImplementation");
    //                will(returnValue(""));
    //
    //                one(servletConfig).getInitParameter("rootpath");
    //                will(returnValue("./target/tmpTestData/"));
    //
    //                exactly(2).of(servletConfig).getInitParameter(
    //                        "lazyFolderCreationOnPut");
    //                will(returnValue("1"));
    //
    //                one(servletConfig).getInitParameter("default-index-file");
    //                will(returnValue("index.html"));
    //
    //                one(servletConfig).getInitParameter("instead-of-404");
    //                will(returnValue(""));
    //
    //                exactly(2).of(servletConfig).getInitParameter(
    //                        "no-content-length-headers");
    //                will(returnValue("0"));
    //            }
    //        });
    //
    //        WebDavServletBean servlet = new WebdavServlet();
    //
    //        servlet.init(servletConfig);
    //
    //        _mockery.assertIsSatisfied();
    //    }

    //    @Test
    //    public void testService() throws Exception {
    //
    //        mockServletConfig.addInitParameter("ResourceHandlerImplementation", "");
    //        mockServletConfig.addInitParameter("rootpath", "./target/tmpTestData");
    //        mockServletConfig.addInitParameter("lazyFolderCreationOnPut", "1");
    //        mockServletConfig.addInitParameter("default-index-file", dftIndexFile);
    //        mockServletConfig.addInitParameter("instead-of-404", insteadOf404);
    //        mockServletConfig.addInitParameter("no-content-length-headers", "0");
    //
    //        mockReq.setMethod("PUT");
    //        mockReq.setAttribute("javax.servlet.include.request_uri", null);
    //        mockReq.setPathInfo("/aPath/toAFile");
    //        mockReq.setRequestURI("/aPath/toAFile");
    //        mockReq.addHeader("Host", "www.foo.bar");
    //        mockReq.addHeader("Depth", "0");
    //        mockReq.addHeader("Content-Type", "text/xml");
    //        mockReq.addHeader("Content-Length", "1234");
    //        mockReq.addHeader("User-Agent", "...some Client with WebDAVFS...");
    //
    //        mockReq.setSession(mockHttpSession);
    //        mockPrincipal = new MockPrincipal("Admin", new String[] { "Admin",
    //                "Manager" });
    //        mockReq.setUserPrincipal(mockPrincipal);
    //        mockReq.addUserRole("Admin");
    //        mockReq.addUserRole("Manager");
    //
    //        mockReq.setContent(resourceContent);
    //
    //        _mockery.checking(new Expectations() {
    //            {
    //
    //            }
    //        });
    //
    //        WebDavServletBean servlet = new WebdavServlet();
    //
    //        servlet.init(mockServletConfig);
    //
    //        servlet.service(mockReq, mockRes);
    //
    //        _mockery.assertIsSatisfied();
    //    }

    /**
     * Minimal {@link ServletConfig} stub backed by a map of init parameters.
     */
    private static class StubServletConfig implements ServletConfig {
        private final Map<String, String> params = new HashMap<>();

        StubServletConfig with(String name, String value) {
            params.put(name, value);
            return this;
        }

        @Override
        public String getServletName() {
            return "webdav-servlet";
        }

        @Override
        public ServletContext getServletContext() {
            return null;
        }

        @Override
        public String getInitParameter(String name) {
            return params.get(name);
        }

        @Override
        public Enumeration<String> getInitParameterNames() {
            return Collections.enumeration(params.keySet());
        }
    }

    private static Object getField(Object target, Class<?> declaringClass, String name) throws Exception {
        Field field = declaringClass.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    @SuppressWarnings("unchecked")
    private static IMethodExecutor getRegisteredMethod(WebDavServletBean servlet, String method) throws Exception {
        Map<String, IMethodExecutor> methodMap = (Map<String, IMethodExecutor>) getField(servlet, WebDavServletBean.class, "_methodMap");
        return methodMap.get(method);
    }

    @Test
    public void testInitWithoutResourceHandlerImplementationDefaultsToLocalFileSystemStore(@TempDir File root) throws Exception {
        WebdavServlet servlet = new WebdavServlet();

        servlet.init(new StubServletConfig().with("rootpath", root.getAbsolutePath()));

        assertInstanceOf(LocalFileSystemStore.class, servlet._store);
    }

    @Test
    public void testInitWithEmptyResourceHandlerImplementationDefaultsToLocalFileSystemStore(@TempDir File root) throws Exception {
        WebdavServlet servlet = new WebdavServlet();

        servlet.init(new StubServletConfig().with("ResourceHandlerImplementation", "").with("rootpath", root.getAbsolutePath()));

        assertInstanceOf(LocalFileSystemStore.class, servlet._store);
    }

    @Test
    public void testInitWithExplicitResourceHandlerImplementation(@TempDir File root) throws Exception {
        WebdavServlet servlet = new WebdavServlet();

        servlet.init(new StubServletConfig()
                .with("ResourceHandlerImplementation", LocalFileSystemStore.class.getName())
                .with("rootpath", root.getAbsolutePath()));

        assertInstanceOf(LocalFileSystemStore.class, servlet._store);
    }

    @Test
    public void testInitWithUnknownResourceHandlerImplementationThrowsRuntimeException(@TempDir File root) {
        WebdavServlet servlet = new WebdavServlet();
        StubServletConfig config = new StubServletConfig()
                .with("ResourceHandlerImplementation", "nl.info.webdav.DoesNotExistStore")
                .with("rootpath", root.getAbsolutePath());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> servlet.init(config));

        assertEquals("Failed to construct store", exception.getMessage());
    }

    @Test
    public void testConstructStoreWithValidClassReturnsInstance(@TempDir File root) {
        IWebdavStore store = new WebdavServlet().constructStore(LocalFileSystemStore.class.getName(), root);

        assertInstanceOf(LocalFileSystemStore.class, store);
    }

    @Test
    public void testConstructStoreWithUnknownClassThrowsRuntimeException(@TempDir File root) {
        WebdavServlet servlet = new WebdavServlet();

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> servlet.constructStore("nl.info.webdav.DoesNotExistStore", root)
        );

        assertEquals("Failed to construct store", exception.getMessage());
        assertInstanceOf(ClassNotFoundException.class, exception.getCause());
    }

    @Test
    public void testConstructStoreWithClassWithoutFileConstructorThrowsRuntimeException(@TempDir File root) {
        WebdavServlet servlet = new WebdavServlet();

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> servlet.constructStore(String.class.getName(), root)
        );

        assertEquals("Failed to construct store", exception.getMessage());
        assertInstanceOf(NoSuchMethodException.class, exception.getCause());
    }

    @Test
    public void testInitWithoutRootpathThrowsWebdavException() {
        WebdavServlet servlet = new WebdavServlet();
        StubServletConfig config = new StubServletConfig();

        WebdavException exception = assertThrows(WebdavException.class, () -> servlet.init(config));

        assertEquals("missing parameter: rootpath", exception.getMessage());
        assertNull(servlet._store);
    }

    @Test
    public void testInitWithWarFileRootOutsideWebInfThrowsWebdavException() {
        // in the test environment classes are loaded from the Gradle build directory,
        // so the code source location never contains '/WEB-INF/'
        WebdavServlet servlet = new WebdavServlet();
        StubServletConfig config = new StubServletConfig().with("rootpath", "*WAR-FILE-ROOT*");

        WebdavException exception = assertThrows(WebdavException.class, () -> servlet.init(config));

        assertTrue(exception.getMessage().startsWith("Could not determine root of war file"));
    }

    @Test
    public void testInitPassesOptionalParametersToMethodHandlers(@TempDir File root) throws Exception {
        WebdavServlet servlet = new WebdavServlet();

        servlet.init(new StubServletConfig()
                .with("rootpath", root.getAbsolutePath())
                .with("lazyFolderCreationOnPut", "1")
                .with("default-index-file", dftIndexFile)
                .with("instead-of-404", insteadOf404)
                .with("no-content-length-headers", "0"));

        IMethodExecutor put = getRegisteredMethod(servlet, "PUT");
        assertInstanceOf(DoPut.class, put);
        assertEquals(true, getField(put, DoPut.class, "_lazyFolderCreationOnPut"));

        IMethodExecutor get = getRegisteredMethod(servlet, "GET");
        assertInstanceOf(DoHead.class, get);
        assertEquals(0, getField(get, DoHead.class, "_contentLength"));
        assertEquals(dftIndexFile, getField(get, DoHead.class, "_dftIndexFile"));
        assertEquals(insteadOf404, getField(get, DoHead.class, "_insteadOf404"));
    }

    @Test
    public void testInitWithoutOptionalParametersUsesDefaults(@TempDir File root) throws Exception {
        WebdavServlet servlet = new WebdavServlet();

        servlet.init(new StubServletConfig().with("rootpath", root.getAbsolutePath()));

        assertEquals(false, getField(getRegisteredMethod(servlet, "PUT"), DoPut.class, "_lazyFolderCreationOnPut"));

        IMethodExecutor get = getRegisteredMethod(servlet, "GET");
        assertEquals(-1, getField(get, DoHead.class, "_contentLength"));
        assertNull(getField(get, DoHead.class, "_dftIndexFile"));
        assertNull(getField(get, DoHead.class, "_insteadOf404"));
    }

    @Test
    public void testInitWithLazyFolderCreationOtherThanOneIsFalse(@TempDir File root) throws Exception {
        WebdavServlet servlet = new WebdavServlet();

        servlet.init(new StubServletConfig().with("rootpath", root.getAbsolutePath()).with("lazyFolderCreationOnPut", "true"));

        assertFalse((Boolean) getField(getRegisteredMethod(servlet, "PUT"), DoPut.class, "_lazyFolderCreationOnPut"));
    }

    @Test
    public void testInitWithNonNumericContentLengthParameterThrowsNumberFormatException(@TempDir File root) {
        WebdavServlet servlet = new WebdavServlet();
        StubServletConfig config = new StubServletConfig()
                .with("rootpath", root.getAbsolutePath())
                .with("no-content-length-headers", "abc");

        assertThrows(NumberFormatException.class, () -> servlet.init(config));
    }
}
