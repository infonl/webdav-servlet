package net.sf.webdav.methods;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import org.junit.Ignore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Ignore
public class TestingOutputStream extends ServletOutputStream {

    private ByteArrayOutputStream baos = new ByteArrayOutputStream();

    public void write(int i) throws IOException {
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
