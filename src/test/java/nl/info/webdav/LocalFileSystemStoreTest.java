/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.webdav;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import nl.info.webdav.exceptions.WebdavException;

class LocalFileSystemStoreTest {

    @TempDir
    File tempDir;

    LocalFileSystemStore store;

    @BeforeEach
    void setUp() throws IOException {
        store = new LocalFileSystemStore(tempDir);
    }

    // --- Happy path: normal URIs within root ---

    @Test
    void createFolder_withinRoot_succeeds() {
        assertDoesNotThrow(() -> store.createFolder(null, "/subdir"));
    }

    @Test
    void createResource_withinRoot_succeeds() {
        assertDoesNotThrow(() -> store.createResource(null, "/file.txt"));
    }

    @Test
    void setResourceContent_withinRoot_succeeds() throws WebdavException {
        store.createResource(null, "/file.txt");
        InputStream data = new ByteArrayInputStream("hello".getBytes());
        long length = store.setResourceContent(null, "/file.txt", data, null, null);
        assertEquals(5L, length);
    }

    @Test
    void getChildrenNames_rootUri_accepted() throws WebdavException {
        store.createResource(null, "/a.txt");
        String[] names = store.getChildrenNames(null, "/");
        assertNotNull(names);
        assertEquals(1, names.length);
        assertEquals("a.txt", names[0]);
    }

    @Test
    void removeObject_withinRoot_succeeds() throws WebdavException {
        store.createResource(null, "/del.txt");
        assertDoesNotThrow(() -> store.removeObject(null, "/del.txt"));
    }

    @Test
    void getResourceContent_withinRoot_succeeds() throws WebdavException {
        store.createResource(null, "/r.txt");
        store.setResourceContent(null, "/r.txt", new ByteArrayInputStream("data".getBytes()), null, null);
        InputStream in = store.getResourceContent(null, "/r.txt");
        assertNotNull(in);
    }

    @Test
    void getResourceLength_withinRoot_succeeds() throws WebdavException {
        store.createResource(null, "/len.txt");
        store.setResourceContent(null, "/len.txt", new ByteArrayInputStream("abc".getBytes()), null, null);
        long len = store.getResourceLength(null, "/len.txt");
        assertEquals(3L, len);
    }

    @Test
    void getStoredObject_withinRoot_succeeds() throws WebdavException {
        store.createResource(null, "/obj.txt");
        StoredObject so = store.getStoredObject(null, "/obj.txt");
        assertNotNull(so);
    }

    // --- Root URI itself ---

    @Test
    void getStoredObject_rootUri_accepted() {
        StoredObject so = store.getStoredObject(null, "/");
        assertNotNull(so);
    }

    @Test
    void getChildrenNames_rootUri_noTraversal() {
        assertDoesNotThrow(() -> store.getChildrenNames(null, "/"));
    }

    // --- Path traversal rejection ---

    @Test
    void createFolder_traversal_throwsWebdavException() {
        assertThrows(WebdavException.class, () -> store.createFolder(null, "/../escaped"));
    }

    @Test
    void createResource_traversal_throwsWebdavException() {
        assertThrows(WebdavException.class, () -> store.createResource(null, "/../escaped.txt"));
    }

    @Test
    void setResourceContent_traversal_throwsWebdavException() {
        assertThrows(WebdavException.class, () -> store.setResourceContent(null, "/../escaped.txt",
                new ByteArrayInputStream(new byte[0]), null, null));
    }

    @Test
    void getChildrenNames_traversal_throwsWebdavException() {
        assertThrows(WebdavException.class, () -> store.getChildrenNames(null, "/../escaped"));
    }

    @Test
    void removeObject_traversal_throwsWebdavException() {
        assertThrows(WebdavException.class, () -> store.removeObject(null, "/../escaped.txt"));
    }

    @Test
    void getResourceContent_traversal_throwsWebdavException() {
        assertThrows(WebdavException.class, () -> store.getResourceContent(null, "/../escaped.txt"));
    }

    @Test
    void getResourceLength_traversal_throwsWebdavException() {
        assertThrows(WebdavException.class, () -> store.getResourceLength(null, "/../escaped.txt"));
    }

    @Test
    void getStoredObject_traversal_returnsNull() {
        StoredObject so = store.getStoredObject(null, "/../escaped.txt");
        assertNull(so);
    }

    // --- Symlink pointing outside root ---

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void getStoredObject_symlinkEscapingRoot_returnsNull(@TempDir File outside) throws IOException {
        Path link = tempDir.toPath().resolve("escape");
        Files.createSymbolicLink(link, outside.toPath());
        StoredObject so = store.getStoredObject(null, "/escape");
        assertNull(so);
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void createResource_symlinkEscapingRoot_throwsWebdavException(@TempDir File outside) throws IOException {
        Path link = tempDir.toPath().resolve("escape");
        Files.createSymbolicLink(link, outside.toPath());
        assertThrows(WebdavException.class, () -> store.createResource(null, "/escape/file.txt"));
    }
}
