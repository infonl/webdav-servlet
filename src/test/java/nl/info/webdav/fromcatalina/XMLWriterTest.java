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

    @Test
    public void testWriteDataEscapesCdataTerminatorAndProducesWellFormedXml() throws Exception {
        writer.writeElement("DAV::displayname", XMLWriter.OPENING);
        writer.writeData("foo]]>bar");
        writer.writeElement("DAV::displayname", XMLWriter.CLOSING);
        String xml = "<?xml version=\"1.0\"?>" + writer.toString();
        javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        org.w3c.dom.Document doc = dbf.newDocumentBuilder().parse(
                new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertEquals("foo]]>bar", doc.getElementsByTagNameNS("DAV:", "displayname").item(0).getTextContent());
    }

    @Test
    public void testWriteDataWithoutCdataTerminatorIsUnchanged() {
        writer.writeData("plain content");
        assertEquals("<![CDATA[plain content]]>", writer.toString());
    }

    @Test
    public void testWriteDataNullProducesEmptyCdataSection() {
        writer.writeData(null);
        assertEquals("<![CDATA[]]>", writer.toString());
    }
}
