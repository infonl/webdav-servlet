/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.webdav.fromcatalina;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class URLEncoderTest {

    private URLEncoder encoder;

    @BeforeEach
    public void setUp() {
        encoder = new URLEncoder();
    }

    @Test
    public void testDefaultSafeCharactersAreUnchanged() {
        String safe = "abcxyzABCXYZ0189$-_.+!*'(),";

        assertEquals(safe, encoder.encode(safe));
    }

    @Test
    public void testEmptyStringIsUnchanged() {
        assertEquals("", encoder.encode(""));
    }

    @Test
    public void testSlashIsEncodedByDefault() {
        // characterization: '/' is not part of the default safe set
        assertEquals("%2Fa%2Fb", encoder.encode("/a/b"));
    }

    @Test
    public void testAddedSafeCharacterIsUnchanged() {
        encoder.addSafeCharacter('/');

        assertEquals("/a/b/file.txt", encoder.encode("/a/b/file.txt"));
    }

    @Test
    public void testUnsafeAsciiCharactersArePercentEncoded() {
        assertEquals("a%20b%23c%25d%3Fe%26f", encoder.encode("a b#c%d?e&f"));
    }

    @Test
    public void testNonAsciiCharactersArePercentEncodedAsUtf8() {
        assertEquals("caf%C3%A9", encoder.encode("café"));
        assertEquals("%E2%82%AC", encoder.encode("€"));
    }

    @Test
    public void testSupplementaryCharacterIsPercentEncodedAsUtf8() {
        assertEquals("%F0%9F%98%80", encoder.encode("😀"));
    }

    @Test
    public void testMixedPathWithSlashSafe() {
        encoder.addSafeCharacter('/');

        assertEquals("/my%20folder/r%C3%A9sum%C3%A9%231.txt", encoder.encode("/my folder/résumé#1.txt"));
    }
}
