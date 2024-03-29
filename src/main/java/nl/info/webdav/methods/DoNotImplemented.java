package nl.info.webdav.methods;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import nl.info.webdav.IMethodExecutor;
import nl.info.webdav.ITransaction;
import nl.info.webdav.WebdavStatus;

public class DoNotImplemented implements IMethodExecutor {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DoNotImplemented.class);
    private final boolean _readOnly;

    public DoNotImplemented(boolean readOnly) {
        _readOnly = readOnly;
    }

    public void execute(
            ITransaction transaction,
            HttpServletRequest req,
            HttpServletResponse resp
    ) throws IOException {
        LOG.trace("-- " + req.getMethod());

        if (_readOnly) {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
        } else
            resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
    }
}
