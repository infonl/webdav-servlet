/*
 * Copyright 1999,2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.info.webdav.fromcatalina;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

/**
 * XMLWriter helper class.
 * 
 * @author <a href="mailto:remm@apache.org">Remy Maucherat</a>
 */
public class XMLWriter {

    // -------------------------------------------------------------- Constants

    /**
     * Opening tag.
     */
    public static final int OPENING = 0;

    /**
     * Closing tag.
     */
    public static final int CLOSING = 1;

    /**
     * Element with no content.
     */
    public static final int NO_CONTENT = 2;

    // ----------------------------------------------------- Instance Variables

    /**
     * Buffer.
     */
    protected StringBuffer _buffer = new StringBuffer();

    /**
     * Writer.
     */
    protected Writer _writer = null;

    /**
     * Namespaces to be declared in the root element
     */
    protected Map<String, String> _namespaces;

    /**
     * Is true until the root element is written
     */
    protected boolean _isRootElement = true;

    // ----------------------------------------------------------- Constructors

    /**
     * Constructor.
     */
    public XMLWriter(Map<String, String> namespaces) {
        _namespaces = namespaces;
    }

    /**
     * Constructor.
     */
    public XMLWriter(Writer writer, Map<String, String> namespaces) {
        _writer = writer;
        _namespaces = namespaces;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Retrieve generated XML.
     * 
     * @return String containing the generated XML
     */
    public String toString() {
        return _buffer.toString();
    }

    /**
     * Write property to the XML.
     * 
     * @param name
     *      Property name
     * @param value
     *      Property value
     */
    public void writeProperty(String name, String value) {
        writeElement(name, OPENING);
        _buffer.append(escapeXml(value));
        writeElement(name, CLOSING);
    }

    /**
     * Write property to the XML.
     * 
     * @param name
     *      Property name
     */
    public void writeProperty(String name) {
        writeElement(name, NO_CONTENT);
    }

    /**
     * Write an element.
     * 
     * @param name
     *      Element name
     * @param type
     *      Element type
     */
    public void writeElement(String name, int type) {
        StringBuilder nsdecl = new StringBuilder();
        if (_isRootElement) {
            for (String fullName : _namespaces.keySet()) {
                String abbrev = _namespaces.get(fullName);
                nsdecl.append(" xmlns:").append(abbrev).append("=\"").append(fullName).append("\"");
            }
            _isRootElement = false;
        }

        int pos = name.lastIndexOf(':');
        if (pos >= 0) {
            // lookup prefix for namespace
            String fullns = name.substring(0, pos);
            String prefix = _namespaces.get(fullns);
            if (prefix == null) {
                // there is no prefix for this namespace
                name = name.substring(pos + 1);
                nsdecl.append(" xmlns=\"").append(fullns).append("\"");
            } else {
                // there is a prefix
                name = prefix + ":" + name.substring(pos + 1);
            }
        } else {
            throw new IllegalArgumentException(
                    "All XML elements must have a namespace");
        }

        switch (type) {
        case OPENING:
            _buffer.append("<").append(name).append(nsdecl).append(">");
            break;
        case CLOSING:
            _buffer.append("</").append(name).append(">\n");
            break;
        case NO_CONTENT:
        default:
            _buffer.append("<").append(name).append(nsdecl).append("/>");
            break;
        }
    }

    /**
     * Write text.
     *
     * @param text
     *      Text to append
     */
    public void writeText(String text) {
        _buffer.append(escapeXml(text));
    }

    /**
     * Write data.
     *
     * @param data
     *      Data to append
     */
    public void writeData(String data) {
        String safeData = data == null ? "" : data;
        // Prevent breaking out of CDATA by splitting any "]]>" into
        // multiple adjacent CDATA sections while preserving exact content.
        safeData = safeData.replace("]]>", "]]]]><![CDATA[>");
        _buffer.append("<![CDATA[").append(safeData).append("]]>");
    }

    /**
     * Write XML Header.
     */
    public void writeXMLHeader() {
        _buffer.append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n");
    }

    /**
     * Send data and re-initialize buffer.
     */
    public void sendData() throws IOException {
        if (_writer != null) {
            _writer.write(_buffer.toString());
            _writer.flush();
            _buffer = new StringBuffer();
        }
    }

    /**
     * Escape XML special characters.
     * By design — " and ' only need escaping inside XML attribute values, not in text node content.
     * escapeXml is only called to write text node content between tags, never inside attribute values.
     * Escaping them there would produce unnecessarily verbose output (&quot;, &#x27;) without any security benefit.
     *
     * @param text the XML text to escape
     * @return the escaped text
     */
    private static String escapeXml(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder result = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '&') {
                result.append("&amp;");
            } else if (c == '<') {
                result.append("&lt;");
            } else if (c == '>') {
                result.append("&gt;");
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
