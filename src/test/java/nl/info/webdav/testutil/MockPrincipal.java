/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.webdav.testutil;

import java.security.Principal;

/**
 * <p>
 * Mock <strong>Principal</strong> object for low-level unit tests of Struts
 * controller components. Coarser grained tests should be implemented in terms
 * of the Cactus framework, instead of the mock object classes.
 * </p>
 * 
 * <p>
 * <strong>WARNING</strong> - Only the minimal set of methods needed to create
 * unit tests is provided, plus additional methods to configure this object as
 * necessary. Methods for unsupported operations will throw
 * <code>UnsupportedOperationException</code>.
 * </p>
 * 
 * <p>
 * <strong>WARNING</strong> - Because unit tests operate in a single threaded
 * environment, no synchronization is performed.
 * </p>
 * 
 * @version $Rev: 54929 $ $Date: 2008-08-05 07:38:44 $
 */

public class MockPrincipal implements Principal {

    protected String _name = null;
    protected String[] _roles = null;

    public MockPrincipal() {
        super();
        _name = "";
        _roles = new String[0];
    }

    public MockPrincipal(String name) {
        super();
        _name = name;
        _roles = new String[0];
    }

    public MockPrincipal(String name, String[] roles) {
        super();
        _name = name;
        _roles = roles;
    }

    public String getName() {
        return _name;
    }

    public boolean isUserInRole(String role) {
        for (int i = 0; i < _roles.length; i++) {
            if (role.equals(_roles[i])) {
                return true;
            }
        }
        return false;
    }

    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (!(o instanceof Principal)) {
            return false;
        }

        Principal p = (Principal) o;
        if (_name == null) {
            return (p.getName() == null);
        } else {
            return (_name.equals(p.getName()));
        }
    }

    public int hashCode() {
        if (_name == null) {
            return "".hashCode();
        } else {
            return _name.hashCode();
        }
    }

}
