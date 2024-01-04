package nl.info.webdav;

import java.security.Principal;

/**
 * Definition of a basic webdav transaction.
 */
public interface ITransaction {

    /**
     * @return the security principal associated with this transaction
     */
    Principal getPrincipal();
}
