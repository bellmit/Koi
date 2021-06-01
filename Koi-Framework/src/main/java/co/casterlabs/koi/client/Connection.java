package co.casterlabs.koi.client;

import java.io.Closeable;
import java.io.IOException;

public interface Connection extends Closeable {

    public void open() throws IOException;

    public boolean isOpen();

}
