/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.webdav;

public interface IMimeTyper {

    /**
     * Detect the mime type of this object
     * 
     * @param transaction the transaction
     * @param path        the path
     * @return the mime type as string
     */
    String getMimeType(ITransaction transaction, String path);
}
