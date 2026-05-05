// SPDX-FileCopyrightText: 2026 INFO.nl
// SPDX-License-Identifier: EUPL-1.2+
package nl.info.webdav.exceptions;

public class PathTraversalException extends WebdavException {

    public PathTraversalException(String message) {
        super(message);
    }
}
