// SPDX-FileCopyrightText: 2026 INFO.nl
// SPDX-License-Identifier: EUPL-1.2+
package nl.info.webdav.fromcatalina;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertTrue(writer.toString().contains("foo&amp;bar"));
    }

    @Test
    public void testWriteTextEscapesLessThan() {
        writer.writeText("foo<bar");
        assertTrue(writer.toString().contains("foo&lt;bar"));
    }

    @Test
    public void testWriteTextEscapesGreaterThan() {
        writer.writeText("foo>bar");
        assertTrue(writer.toString().contains("foo&gt;bar"));
    }

    @Test
    public void testWriteTextNoSpecialCharactersUnchanged() {
        writer.writeText("normal text 123");
        assertTrue(writer.toString().contains("normal text 123"));
    }

    @Test
    public void testWritePropertyEscapesAmpersandInValue() {
        writer.writeProperty("DAV::displayname", "foo&bar");
        String output = writer.toString();
        assertTrue(output.contains("foo&amp;bar"), "Expected &amp; in: " + output);
    }

    @Test
    public void testWritePropertyEscapesLessThanInValue() {
        writer.writeProperty("DAV::displayname", "foo<bar");
        String output = writer.toString();
        assertTrue(output.contains("foo&lt;bar"), "Expected &lt; in: " + output);
    }

    @Test
    public void testWriteTextNullProducesEmptyOutput() {
        writer.writeText(null);
        assertEquals("", writer.toString());
    }

    @Test
    public void testWritePropertyRendersElementWithEscapedValue() {
        writer.writeProperty("DAV::displayname", "a&b<c>d");
        String output = writer.toString();
        assertEquals("<D:displayname xmlns:D=\"DAV:\">a&amp;b&lt;c&gt;d</D:displayname>\n", output);
    }
}
