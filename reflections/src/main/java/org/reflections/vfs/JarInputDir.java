package org.reflections.vfs;

import com.google.common.collect.AbstractIterator;
import org.reflections.ReflectionsException;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import static org.reflections.util.Utils.findLogger;

/**
 *
 */
public class JarInputDir implements Vfs.Dir {
    @Nullable
    public static Logger log = findLogger(JarInputDir.class);

    private static final int DEFAULT_BUFFER_SIZE = 8192;

    private final String path;
    private final JarInputStream jarInputStream;

    public JarInputDir(URL url) {
        try {
            jarInputStream = new JarInputStream(url.openConnection().getInputStream());
        } catch (Exception e) {
            throw new ReflectionsException("Could not open url connection", e);
        }
        this.path = url.getPath();
    }

    public String getPath() {
        return path;
    }

    public Iterable<Vfs.File> getFiles() {
        return new Iterable<Vfs.File>() {
            public Iterator<Vfs.File> iterator() {
                return new AbstractIterator<Vfs.File>() {
                    protected Vfs.File computeNext() {
                        while (true) {
                            try {
                                JarEntry entry = jarInputStream.getNextJarEntry();
                                if (entry == null) {
                                    return endOfData();
                                }

                                if (!entry.isDirectory()) {
                                    byte[] data = readCurrentEntry(jarInputStream, entry.getSize());
                                    return new JarInputFile(entry, data);
                                }
                            } catch (IOException e) {
                                throw new ReflectionsException("could not get next zip entry", e);
                            }
                        }
                    }
                };
            }
        };
    }

    private static byte[] readCurrentEntry(JarInputStream jarInputStream, long size) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream(size > 0 ? (int) size : DEFAULT_BUFFER_SIZE);
        byte[] buf = new byte[DEFAULT_BUFFER_SIZE];
        int readBytes;
        while ((readBytes = jarInputStream.read(buf, 0, buf.length)) > 0) {
            result.write(buf, 0, readBytes);
        }
        return result.toByteArray();
    }

    public void close() {
        try {
            jarInputStream.close();
        } catch (IOException e) {
            if (log != null) {
                log.warn("Can't close, ignoring", e);
            }
        }
    }

}
