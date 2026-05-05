// SPDX-FileCopyrightText: 2026 INFO.nl
// SPDX-License-Identifier: EUPL-1.2+
package nl.info.webdav.fromcatalina;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class XMLWriterTest {

    private XMLWriter writer;

    @BeforeEach
    public void setUp() {
        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("DAV:", "D");
        writer = new XMLWriter(namespaces);
    }

    @Test
    public void testWriteTextEscapesAmpersand() {
        writer.writeText("foo&bar");
        assertEquals("foo&amp;bar", writer.toString());
    }

    @Test
    public void testWriteTextEscapesLessThan() {
        writer.writeText("foo<bar");
        assertEquals("foo&lt;bar", writer.toString());
    }

    @Test
    public void testWriteTextEscapesGreaterThan() {
        writer.writeText("foo>bar");
        assertEquals("foo&gt;bar", writer.toString());
    }

    @Test
    public void testWriteTextNoSpecialCharactersUnchanged() {
        writer.writeText("normal text 123");
        assertEquals("normal text 123", writer.toString());
    }

    @Test
    public void testWriteTextNullProducesEmptyOutput() {
        writer.writeText(null);
        assertEquals("", writer.toString());
    }

    @Test
    public void testWritePropertyEscapesAmpersandInValue() {
        writer.writeProperty("DAV::displayname", "foo&bar");
        assertEquals("<D:displayname xmlns:D=\"DAV:\">foo&amp;bar</D:displayname>\n", writer.toString());
    }

    @Test
    public void testWritePropertyEscapesLessThanInValue() {
        writer.writeProperty("DAV::displayname", "foo<bar");
        assertEquals("<D:displayname xmlns:D=\"DAV:\">foo&lt;bar</D:displayname>\n", writer.toString());
    }

    @Test
    public void testWritePropertyRendersElementWithEscapedValue() {
        writer.writeProperty("DAV::displayname", "a&b<c>d");
        assertEquals("<D:displayname xmlns:D=\"DAV:\">a&amp;b&lt;c&gt;d</D:displayname>\n", writer.toString());
    }
}
