/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.webdav.fromcatalina;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class XMLHelperTest {

    private static Element parse(String xml, boolean namespaceAware) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(namespaceAware);
        return factory.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))
                .getDocumentElement();
    }

    private static Element parse(String xml) throws Exception {
        return parse(xml, true);
    }

    @Test
    public void testFindSubElementByLocalName() throws Exception {
        Element root = parse("<D:propfind xmlns:D=\"DAV:\">text<D:allprop/><D:prop><D:a/></D:prop></D:propfind>");

        Node prop = XMLHelper.findSubElement(root, "prop");

        assertNotNull(prop);
        assertEquals("prop", prop.getLocalName());
        assertEquals("DAV:", prop.getNamespaceURI());
    }

    @Test
    public void testFindSubElementReturnsFirstMatch() throws Exception {
        Element root = parse("<D:x xmlns:D=\"DAV:\" xmlns:O=\"urn:other\"><O:prop id=\"1\"/><D:prop id=\"2\"/></D:x>");

        Node prop = XMLHelper.findSubElement(root, "prop");

        // characterization: matching is on local name only, the namespace is ignored
        assertEquals("1", ((Element) prop).getAttribute("id"));
    }

    @Test
    public void testFindSubElementOnlySearchesDirectChildren() throws Exception {
        Element root = parse("<D:propfind xmlns:D=\"DAV:\"><D:prop><D:a/></D:prop></D:propfind>");

        assertNull(XMLHelper.findSubElement(root, "a"));
    }

    @Test
    public void testFindSubElementAbsent() throws Exception {
        Element root = parse("<D:propfind xmlns:D=\"DAV:\"><D:allprop/></D:propfind>");

        assertNull(XMLHelper.findSubElement(root, "prop"));
    }

    @Test
    public void testFindSubElementWithOnlyTextChildren() throws Exception {
        Element root = parse("<D:propfind xmlns:D=\"DAV:\">just text</D:propfind>");

        assertNull(XMLHelper.findSubElement(root, "prop"));
    }

    @Test
    public void testFindSubElementWithNullParent() {
        assertNull(XMLHelper.findSubElement(null, "prop"));
    }

    @Test
    public void testFindSubElementOnNonNamespaceAwareDomThrowsNullPointerException() throws Exception {
        // characterization: without namespace awareness getLocalName() returns null for element nodes
        Element root = parse("<propfind><prop/></propfind>", false);

        assertThrows(NullPointerException.class, () -> XMLHelper.findSubElement(root, "prop"));
    }

    @Test
    public void testGetPropertiesFromXMLReturnsNamespaceQualifiedNames() throws Exception {
        Element prop = parse("<D:prop xmlns:D=\"DAV:\">\n  <D:a/>text<!-- comment --><D:b>value</D:b>\n</D:prop>");

        Vector<String> properties = XMLHelper.getPropertiesFromXML(prop);

        // characterization: each entry is "<namespaceURI>:<localName>"
        assertEquals(List.of("DAV::a", "DAV::b"), properties);
    }

    @Test
    public void testGetPropertiesFromXMLWithMixedNamespaces() throws Exception {
        Element prop = parse("<D:prop xmlns:D=\"DAV:\" xmlns:Z=\"urn:custom\"><Z:color/><D:getetag/></D:prop>");

        assertEquals(List.of("urn:custom:color", "DAV::getetag"), XMLHelper.getPropertiesFromXML(prop));
    }

    @Test
    public void testGetPropertiesFromXMLWithoutElementChildren() throws Exception {
        Element prop = parse("<D:prop xmlns:D=\"DAV:\">only text</D:prop>");

        assertTrue(XMLHelper.getPropertiesFromXML(prop).isEmpty());
    }
}
