/*
 * Copyright 1999,2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.info.webdav.fromcatalina;

import static java.text.MessageFormat.format;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Map;

import jakarta.servlet.http.Cookie;

/**
 * General purpose request parsing and encoding utility methods.
 * 
 * @author Craig R. McClanahan
 * @author Tim Tye
 * @version $Revision: 1.2 $ $Date: 2008-08-05 07:38:45 $
 */
public final class RequestUtil {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(RequestUtil.class);

    /**
     * Encode a cookie as per RFC 2109. The resulting string can be used as the
     * value for a <code>Set-Cookie</code> header.
     * 
     * @param cookie the cookie to encode.
     * @return A string following RFC 2109.
     */
    public static String encodeCookie(Cookie cookie) {
        StringBuilder stringBuilder = new StringBuilder(cookie.getName());
        stringBuilder.append("=");
        stringBuilder.append(cookie.getValue());

        String domain = cookie.getDomain();
        if (domain != null) {
            stringBuilder.append("; Domain=\"");
            stringBuilder.append(domain);
            stringBuilder.append("\"");
        }

        int age = cookie.getMaxAge();
        if (age >= 0) {
            stringBuilder.append("; Max-Age=\"");
            stringBuilder.append(age);
            stringBuilder.append("\"");
        }

        String path = cookie.getPath();
        if (path != null) {
            stringBuilder.append("; Path=\"");
            stringBuilder.append(path);
            stringBuilder.append("\"");
        }

        if (cookie.getSecure()) {
            stringBuilder.append("; Secure");
        }

        return (stringBuilder.toString());
    }

    /**
     * Filter the specified message string for characters that are sensitive in
     * HTML. This avoids potential attacks caused by including JavaScript codes
     * in the request URL that is often reported in error messages.
     * 
     * @param message the message string to be filtered
     */
    public static String filter(String message) {
        if (message == null)
            return (null);

        char[] content = new char[message.length()];
        message.getChars(0, message.length(), content, 0);
        StringBuilder result = new StringBuilder(content.length + 50);
        for (int i = 0; i < content.length; i++) {
            switch (content[i]) {
                case '<':
                    result.append("&lt;");
                    break;
                case '>':
                    result.append("&gt;");
                    break;
                case '&':
                    result.append("&amp;");
                    break;
                case '"':
                    result.append("&quot;");
                    break;
                default:
                    result.append(content[i]);
            }
        }
        return result.toString();
    }

    /**
     * Normalize a relative URI path that may have relative values ("/./",
     * "/../", and so on ) in it. <strong>WARNING</strong> - This method is
     * useful only for normalizing application-generated paths. It does not try
     * to perform security checks for malicious input.
     * 
     * @param path relative path to be normalized
     */
    public static String normalize(String path) {

        if (path == null)
            return null;

        // Create a place for the normalized path
        String normalized = path;

        if (normalized.equals("/."))
            return "/";

        // Add a leading "/" if necessary
        if (!normalized.startsWith("/"))
            normalized = "/" + normalized;

        // Resolve occurrences of "//" in the normalized path
        while (true) {
            int index = normalized.indexOf("//");
            if (index < 0)
                break;
            normalized = normalized.substring(0, index) + normalized.substring(index + 1);
        }

        // Resolve occurrences of "/./" in the normalized path
        while (true) {
            int index = normalized.indexOf("/./");
            if (index < 0)
                break;
            normalized = normalized.substring(0, index) + normalized.substring(index + 2);
        }

        // Resolve occurrences of "/../" in the normalized path
        while (true) {
            int index = normalized.indexOf("/../");
            if (index < 0)
                break;
            if (index == 0)
                return (null); // Trying to go outside our context
            int index2 = normalized.lastIndexOf('/', index - 1);
            normalized = normalized.substring(0, index2) + normalized.substring(index + 3);
        }

        // Return the normalized path that we have completed
        return (normalized);

    }

    /**
     * Parse the character encoding from the specified content type header. If
     * the content type is null, or there is no explicit character encoding,
     * <code>null</code> is returned.
     * 
     * @param contentType a content type header
     */
    public static String parseCharacterEncoding(String contentType) {
        if (contentType == null)
            return (null);
        int start = contentType.indexOf("charset=");
        if (start < 0)
            return (null);
        String encoding = contentType.substring(start + 8);
        int end = encoding.indexOf(';');
        if (end >= 0)
            encoding = encoding.substring(0, end);
        encoding = encoding.trim();
        if ((encoding.length() > 2) && (encoding.startsWith("\"")) && (encoding.endsWith("\"")))
            encoding = encoding.substring(1, encoding.length() - 1);
        return encoding.trim();
    }

    /**
     * Parse a cookie header into an array of cookies according to RFC 2109.
     * 
     * @param header value of an HTTP "Cookie" header
     */
    public static Cookie[] parseCookieHeader(String header) {
        if ((header == null) || (header.isEmpty()))
            return (new Cookie[0]);

        ArrayList<Cookie> cookies = new ArrayList<>();
        while (!header.isEmpty()) {
            int semicolon = header.indexOf(';');
            if (semicolon < 0)
                semicolon = header.length();
            if (semicolon == 0)
                break;
            String token = header.substring(0, semicolon);
            if (semicolon < header.length())
                header = header.substring(semicolon + 1);
            else
                header = "";
            try {
                int equals = token.indexOf('=');
                if (equals > 0) {
                    String name = token.substring(0, equals).trim();
                    String value = token.substring(equals + 1).trim();
                    cookies.add(new Cookie(name, value));
                }
            } catch (Throwable ignored) {
                // ignore
            }
        }

        return cookies.toArray(new Cookie[0]);
    }

    /**
     * Append request parameters from the specified String to the specified Map.
     * It is presumed that the specified Map is not accessed from any other
     * thread, so no synchronization is performed.
     * <p>
     * <strong>IMPLEMENTATION NOTE</strong>: URL decoding is performed
     * individually on the parsed name and value elements, rather than on the
     * entire query string ahead of time, to properly deal with the case where
     * the name or value includes an encoded "=" or "{@literal &}" character that would
     * otherwise be interpreted as a delimiter.
     * 
     * @param map  map that accumulates the resulting parameters
     * @param data input string containing request parameters
     * 
     * @exception IllegalArgumentException if the data is malformed
     */
    public static void parseParameters(
            Map<String, String[]> map,
            String data,
            String encoding
    ) throws UnsupportedEncodingException {
        if ((data != null) && (!data.isEmpty())) {

            // use the specified encoding to extract bytes out of the
            // given string so that the encoding is not lost. If an
            // encoding is not specified, let it use platform default
            byte[] bytes = null;
            try {
                if (encoding == null) {
                    bytes = data.getBytes();
                } else {
                    bytes = data.getBytes(encoding);
                }
            } catch (UnsupportedEncodingException ignored) {
                // ignore
            }

            parseParameters(map, bytes, encoding);
        }

    }

    /**
     * Decode and return the specified URL-encoded String. When the byte array
     * is converted to a string, the system default character encoding is
     * used... This may be different than some other servers.
     * 
     * @param str
     *            The url-encoded string
     * 
     * @exception IllegalArgumentException
     *                                     if a '%' character is not followed by a valid 2-digit hexadecimal
     *                                     number
     */
    public static String URLDecode(String str) {
        return URLDecode(str, null);
    }

    /**
     * Decode and return the specified URL-encoded String.
     * 
     * @param str
     *            The url-encoded string
     * @param enc
     *            The encoding to use; if null, the default encoding is used
     * @exception IllegalArgumentException
     *                                     if a '%' character is not followed by a valid 2-digit hexadecimal
     *                                     number
     */
    public static String URLDecode(String str, String enc) {
        if (str == null)
            return null;

        // use the specified encoding to extract bytes out of the
        // given string so that the encoding is not lost. If an
        // encoding is not specified, let it use platform default
        byte[] bytes = null;
        try {
            if (enc == null) {
                bytes = str.getBytes();
            } else {
                bytes = str.getBytes(enc);
            }
        } catch (UnsupportedEncodingException ignored) {
            // ignore
        }

        return URLDecode(bytes, enc);

    }

    /**
     * Decode and return the specified URL-encoded byte array.
     * 
     * @param bytes
     *              The url-encoded byte array
     * @exception IllegalArgumentException
     *                                     if a '%' character is not followed by a valid 2-digit hexadecimal
     *                                     number
     */
    public static String URLDecode(byte[] bytes) {
        return URLDecode(bytes, null);
    }

    /**
     * Decode and return the specified URL-encoded byte array.
     * 
     * @param bytes
     *              The url-encoded byte array
     * @param enc
     *              The encoding to use; if null, the default encoding is used
     * @exception IllegalArgumentException
     *                                     if a '%' character is not followed by a valid 2-digit hexadecimal
     *                                     number
     */
    public static String URLDecode(byte[] bytes, String enc) {
        if (bytes == null)
            return null;

        int len = bytes.length;
        int ix = 0;
        int ox = 0;
        while (ix < len) {
            byte b = bytes[ix++]; // Get byte to test
            if (b == '+') {
                b = (byte) ' ';
            } else if (b == '%') {
                b = (byte) ((convertHexDigit(bytes[ix++]) << 4) + convertHexDigit(bytes[ix++]));
            }
            bytes[ox++] = b;
        }
        if (enc != null) {
            try {
                return new String(bytes, 0, ox, enc);
            } catch (Exception e) {
                LOG.error(format("Failed to decode URL byte array using encoding: {0}", enc), e);
            }
        }
        return new String(bytes, 0, ox);
    }

    /**
     * Convert a byte character value to hexidecimal digit value.
     * 
     * @param b
     *          the character value byte
     */
    private static byte convertHexDigit(byte b) {
        if ((b >= '0') && (b <= '9'))
            return (byte) (b - '0');
        if ((b >= 'a') && (b <= 'f'))
            return (byte) (b - 'a' + 10);
        if ((b >= 'A') && (b <= 'F'))
            return (byte) (b - 'A' + 10);
        return 0;
    }

    /**
     * Put name and value pair in map. When name already exist, add value to
     * array of values.
     * 
     * @param map
     *              The map to populate
     * @param name
     *              The parameter name
     * @param value
     *              The parameter value
     */
    private static void putMapEntry(
            Map<String, String[]> map,
            String name,
            String value
    ) {
        String[] newValues;
        String[] oldValues = map.get(name);
        if (oldValues == null) {
            newValues = new String[1];
            newValues[0] = value;
        } else {
            newValues = new String[oldValues.length + 1];
            System.arraycopy(oldValues, 0, newValues, 0, oldValues.length);
            newValues[oldValues.length] = value;
        }
        map.put(name, newValues);
    }

    /**
     * Append request parameters from the specified String to the specified Map.
     * It is presumed that the specified Map is not accessed from any other
     * thread, so no synchronization is performed.
     * <p>
     * <strong>IMPLEMENTATION NOTE</strong>: URL decoding is performed
     * individually on the parsed name and value elements, rather than on the
     * entire query string ahead of time, to properly deal with the case where
     * the name or value includes an encoded "=" or "{@literal &}" character that would
     * otherwise be interpreted as a delimiter. NOTE: byte array data is
     * modified by this method. Caller beware.
     * 
     * @param map      map that accumulates the resulting parameters
     * @param data     input string containing request parameters
     * @param encoding
     *                 Encoding to use for converting hex
     * 
     * @exception UnsupportedEncodingException
     *                                         if the data is malformed
     */
    public static void parseParameters(
            Map<String, String[]> map,
            byte[] data,
            String encoding
    ) throws UnsupportedEncodingException {
        if (data != null && data.length > 0) {
            int ix = 0;
            int ox = 0;
            String key = null;
            String value;
            while (ix < data.length) {
                byte c = data[ix++];
                switch ((char) c) {
                    case '&':
                        value = new String(data, 0, ox, encoding);
                        if (key != null) {
                            putMapEntry(map, key, value);
                            key = null;
                        }
                        ox = 0;
                        break;
                    case '=':
                        if (key == null) {
                            key = new String(data, 0, ox, encoding);
                            ox = 0;
                        } else {
                            data[ox++] = c;
                        }
                        break;
                    case '+':
                        data[ox++] = (byte) ' ';
                        break;
                    case '%':
                        data[ox++] = (byte) ((convertHexDigit(data[ix++]) << 4) + convertHexDigit(data[ix++]));
                        break;
                    default:
                        data[ox++] = c;
                }
            }
            // The last value does not end in '&'. So save it now.
            if (key != null) {
                value = new String(data, 0, ox, encoding);
                putMapEntry(map, key, value);
            }
        }
    }
}
