package net.sf.webdav.methods;

import org.junit.Ignore;

import java.io.ByteArrayOutputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;

@Ignore
public class TestingOutputStream extends ServletOutputStream {

    private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

    public void write(int i) {
        baos.write(i);
    }

    public String toString() {
        return baos.toString();
    }

    @Override
    public boolean isReady() {
        return false;
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
        // no op
    }
}
