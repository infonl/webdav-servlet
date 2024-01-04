package nl.info.webdav.methods;

import nl.info.webdav.ITransaction;
import nl.info.webdav.IWebdavStore;
import nl.info.webdav.StoredObject;
import nl.info.webdav.WebdavStatus;
import nl.info.webdav.locking.LockedObject;
import nl.info.webdav.locking.ResourceLocks;
import nl.info.webdav.testutil.MockTest;
import org.jmock.Expectations;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class DoDeleteTest extends MockTest {

    static IWebdavStore mockStore;
    static HttpServletRequest mockReq;
    static HttpServletResponse mockRes;
    static ITransaction mockTransaction;
    static byte[] resourceContent = new byte[] { '<', 'h', 'e', 'l', 'l', 'o',
            '/', '>' };

    @BeforeAll
    public static void setUp() throws Exception {
        mockStore = _mockery.mock(IWebdavStore.class);
        mockReq = _mockery.mock(HttpServletRequest.class);
        mockRes = _mockery.mock(HttpServletResponse.class);
        mockTransaction = _mockery.mock(ITransaction.class);
    }

    @Test
    public void testDeleteIfReadOnlyIsTrue() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoDelete doDelete = new DoDelete(mockStore, resLocks, readOnly);
        doDelete.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDeleteFileIfObjectExists() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                StoredObject fileSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(fileSo));

                oneOf(mockStore).removeObject(mockTransaction, sourceFilePath);
            }
        });

        DoDelete doDelete = new DoDelete(mockStore, new ResourceLocks(),
                !readOnly);

        doDelete.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDeleteFileIfObjectNotExists() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                StoredObject fileSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(fileSo));

                oneOf(mockRes).sendError(WebdavStatus.SC_NOT_FOUND);
            }
        });

        DoDelete doDelete = new DoDelete(mockStore, new ResourceLocks(),
                !readOnly);

        doDelete.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDeleteFolderIfObjectExists() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceCollectionPath));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                StoredObject folderSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction,
                        sourceCollectionPath);
                will(returnValue(folderSo));

                oneOf(mockStore).getChildrenNames(mockTransaction,
                        sourceCollectionPath);
                will(returnValue(new String[] { "subFolder", "sourceFile" }));

                StoredObject fileSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(fileSo));

                oneOf(mockStore).removeObject(mockTransaction, sourceFilePath);

                StoredObject subFolderSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction,
                        sourceCollectionPath + "/subFolder");
                will(returnValue(subFolderSo));

                oneOf(mockStore).getChildrenNames(mockTransaction,
                        sourceCollectionPath + "/subFolder");
                will(returnValue(new String[] { "fileInSubFolder" }));

                StoredObject fileInSubFolderSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction,
                        sourceCollectionPath + "/subFolder/fileInSubFolder");
                will(returnValue(fileInSubFolderSo));

                oneOf(mockStore).removeObject(mockTransaction,
                        sourceCollectionPath + "/subFolder/fileInSubFolder");

                oneOf(mockStore).removeObject(mockTransaction,
                        sourceCollectionPath + "/subFolder");

                oneOf(mockStore).removeObject(mockTransaction,
                        sourceCollectionPath);
            }
        });

        DoDelete doDelete = new DoDelete(mockStore, new ResourceLocks(),
                !readOnly);

        doDelete.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDeleteFolderIfObjectNotExists() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceCollectionPath));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                StoredObject folderSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction,
                        sourceCollectionPath);
                will(returnValue(folderSo));

                oneOf(mockRes).sendError(WebdavStatus.SC_NOT_FOUND);
            }
        });

        DoDelete doDelete = new DoDelete(mockStore, new ResourceLocks(),
                !readOnly);

        doDelete.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDeleteFileInFolder() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                StoredObject fileSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(fileSo));

                oneOf(mockStore).removeObject(mockTransaction, sourceFilePath);
            }
        });

        DoDelete doDelete = new DoDelete(mockStore, new ResourceLocks(),
                !readOnly);

        doDelete.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDeleteFileInLockedFolderWithWrongLockToken()
            throws Exception {

        final String lockedFolderPath = "/lockedFolder";
        final String fileInLockedFolderPath = lockedFolderPath
                .concat("/fileInLockedFolder");

        String owner = new String("owner");
        ResourceLocks resLocks = new ResourceLocks();

        resLocks.lock(mockTransaction, lockedFolderPath, owner, true, -1,
                TEMP_TIMEOUT, !TEMPORARY);
        LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction,
                lockedFolderPath);
        final String wrongLockToken = "(<opaquelocktoken:" + lo.getID()
                + "WRONG>)";

        final PrintWriter pw = new PrintWriter("/tmp/XMLTestFile");

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(fileInLockedFolderPath));

                oneOf(mockReq).getHeader("If");
                will(returnValue(wrongLockToken));

                oneOf(mockRes).setStatus(WebdavStatus.SC_LOCKED);
            }
        });

        DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);

        doDelete.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDeleteFileInLockedFolderWithRightLockToken()
            throws Exception {

        final String path = "/lockedFolder/fileInLockedFolder";
        final String parentPath = "/lockedFolder";
        final String owner = new String("owner");
        ResourceLocks resLocks = new ResourceLocks();

        resLocks.lock(mockTransaction, parentPath, owner, true, -1,
                TEMP_TIMEOUT, !TEMPORARY);
        LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction,
                "/lockedFolder");
        final String rightLockToken = "(<opaquelocktoken:" + lo.getID() + ">)";

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getHeader("If");
                will(returnValue(rightLockToken));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                StoredObject so = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(so));

                oneOf(mockStore).removeObject(mockTransaction, path);

            }
        });

        DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);

        doDelete.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDeleteFileInFolderIfObjectNotExists() throws Exception {

        boolean readOnly = false;

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/folder/file"));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                StoredObject nonExistingSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, "/folder/file");
                will(returnValue(nonExistingSo));

                oneOf(mockRes).sendError(WebdavStatus.SC_NOT_FOUND);
            }
        });

        DoDelete doDelete = new DoDelete(mockStore, new ResourceLocks(),
                readOnly);

        doDelete.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

}
