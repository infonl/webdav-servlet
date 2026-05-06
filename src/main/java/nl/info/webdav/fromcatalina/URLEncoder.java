/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.webdav.fromcatalina;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * This class is very similar to the java.net.URLEncoder class.
 * <p>
 * Unfortunately, with java.net.URLEncoder there is no way to specify to the
 * java.net.URLEncoder which characters should NOT be encoded.
 * <p>
 * This code was moved from DefaultServlet.java
 * 
 * @author Craig R. McClanahan
 * @author Remy Maucherat
 */
public class URLEncoder {
    private static final Logger LOG = Logger.getLogger(URLEncoder.class.getName());

    protected static final char[] HEXADECIMAL = {'0', '1', '2', '3', '4', '5',
                                                 '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    // Array containing the safe characters set.
    protected BitSet _safeCharacters = new BitSet(256);

    public URLEncoder() {
        for (char i = 'a'; i <= 'z'; i++) {
            addSafeCharacter(i);
        }
        for (char i = 'A'; i <= 'Z'; i++) {
            addSafeCharacter(i);
        }
        for (char i = '0'; i <= '9'; i++) {
            addSafeCharacter(i);
        }
        for (char c : "$-_.+!*'(),".toCharArray()) {
            addSafeCharacter(c);
        }
    }

    public void addSafeCharacter(char c) {
        _safeCharacters.set(c);
    }

    public String encode(String path) {
        int maxBytesPerChar = 10;
        StringBuilder rewrittenPath = new StringBuilder(path.length());
        ByteArrayOutputStream buf = new ByteArrayOutputStream(maxBytesPerChar);
        OutputStreamWriter writer;
        try {
            writer = new OutputStreamWriter(buf, StandardCharsets.UTF_8);
        } catch (Exception exception) {
            LOG.log(Level.SEVERE, "Error in encode <" + path + ">", exception);
            writer = new OutputStreamWriter(buf);
        }

        for (int i = 0; i < path.length(); i++) {
            int c = path.charAt(i);
            if (_safeCharacters.get(c)) {
                rewrittenPath.append((char) c);
            } else {
                // convert to external encoding before hex conversion
                try {
                    writer.write((char) c);
                    writer.flush();
                } catch (IOException e) {
                    buf.reset();
                    continue;
                }
                byte[] ba = buf.toByteArray();
                for (byte toEncode : ba) {
                    // Converting each byte in the buffer
                    rewrittenPath.append('%');
                    int low = (int) (toEncode & 0x0f);
                    int high = (int) ((toEncode & 0xf0) >> 4);
                    rewrittenPath.append(HEXADECIMAL[high]);
                    rewrittenPath.append(HEXADECIMAL[low]);
                }
                buf.reset();
            }
        }
        return rewrittenPath.toString();
    }
}
