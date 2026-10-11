/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.webdav.fromcatalina;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * Tests for the {@code URLDecode} overloads of {@link RequestUtil} (the only methods used by production code).
 */
public class RequestUtilTest {

    @Test
    public void testURLDecodePercentEncodedUtf8() {
        assertEquals("/a b/é", RequestUtil.URLDecode("/a%20b/%C3%A9", "UTF8"));
    }

    @Test
    public void testURLDecodeLowercaseHexDigits() {
        assertEquals("/é", RequestUtil.URLDecode("/%c3%a9", "UTF-8"));
    }

    @Test
    public void testURLDecodeWithoutEncodingDecodesAscii() {
        assertEquals("/a b", RequestUtil.URLDecode("/a%20b"));
    }

    @Test
    public void testURLDecodePlusIsConvertedToSpace() {
        assertEquals("a b c", RequestUtil.URLDecode("a+b+c", "UTF-8"));
        assertEquals("a b", RequestUtil.URLDecode("a+b"));
    }

    @Test
    public void testURLDecodeStringWithoutEscapesIsUnchanged() {
        assertEquals("/plain/path.txt", RequestUtil.URLDecode("/plain/path.txt", "UTF-8"));
    }

    @Test
    public void testURLDecodeInvalidHexDigitIsTreatedAsZero() {
        // characterization: non-hex digits are silently converted to 0
        assertEquals("a\u0001b", RequestUtil.URLDecode("a%z1b", "UTF-8"));
    }

    @Test
    public void testURLDecodeNullString() {
        assertNull(RequestUtil.URLDecode((String) null));
        assertNull(RequestUtil.URLDecode((String) null, "UTF-8"));
    }

    @Test
    public void testURLDecodeNullBytes() {
        assertNull(RequestUtil.URLDecode((byte[]) null));
        assertNull(RequestUtil.URLDecode((byte[]) null, "UTF-8"));
    }

    @Test
    public void testURLDecodeBytes() {
        byte[] bytes = "/a%20b+c/%C3%A9".getBytes(StandardCharsets.US_ASCII);

        assertEquals("/a b c/é", RequestUtil.URLDecode(bytes, "UTF-8"));
    }

    @Test
    public void testURLDecodeBytesWithoutEncoding() {
        assertEquals("a b", RequestUtil.URLDecode("a%20b".getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    public void testURLDecodeBytesDecodesInPlace() {
        // characterization: the input array is overwritten with the decoded bytes
        byte[] bytes = "a%20b".getBytes(StandardCharsets.US_ASCII);

        RequestUtil.URLDecode(bytes, "UTF-8");

        assertArrayEquals("a b0b".getBytes(StandardCharsets.US_ASCII), bytes);
    }

    @Test
    public void testURLDecodeBytesWithUnsupportedEncodingFallsBackToPlatformDefault() {
        byte[] bytes = "a%20b".getBytes(StandardCharsets.US_ASCII);

        assertEquals("a b", RequestUtil.URLDecode(bytes, "NO-SUCH-ENCODING"));
    }

    @Test
    public void testURLDecodeStringWithUnsupportedEncodingReturnsNull() {
        // characterization: String.getBytes(enc) throws UnsupportedEncodingException, which is ignored,
        // leaving the byte array null, so the result is null instead of a platform-default fallback
        assertNull(RequestUtil.URLDecode("a%20b", "NO-SUCH-ENCODING"));
    }
}
