/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.webdav.exceptions;

public class WebdavException extends RuntimeException {

    public WebdavException() {
        super();
    }

    public WebdavException(String message) {
        super(message);
    }

    public WebdavException(String message, Throwable cause) {
        super(message, cause);
    }

    public WebdavException(Throwable cause) {
        super(cause);
    }
}
