package org.reflections.vfs;

import org.reflections.Reflections;
import org.reflections.ReflectionsException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

public class JarInputDir implements Vfs.Dir {
    private final URL url;
    JarInputStream jarInputStream;
    long cursor = 0;
    long nextCursor = 0;

    public JarInputDir(URL url) {
        this.url = url;
    }

    public String getPath() {
        return url.getPath();
    }

    public Iterable<Vfs.File> getFiles() {
        return () -> new Iterator<Vfs.File>() {
            {
                try {
                    InputStream stream = url.openConnection().getInputStream();
                    if (stream instanceof JarInputStream) {
                        jarInputStream = (JarInputStream) stream;
                    } else {
                        jarInputStream = new JarInputStream(stream);
                    }
                } catch (Exception e) {
                    throw new ReflectionsException("Could not open url connection", e);
                }
            }

            Vfs.File entry = null;

            @Override
            public boolean hasNext() {
                return entry != null || (entry = computeNext()) != null;
            }

            @Override
            public Vfs.File next() {
                Vfs.File next = entry;
                entry = null;
                return next;
            }

            private Vfs.File computeNext() {
                while (true) {
                    try {
                        ZipEntry entry = jarInputStream.getNextJarEntry();
                        if (entry == null) {
                            return null;
                        }

                        long size = entry.getSize();
                        if (size < 0) size = 0xffffffffl + size; //JDK-6916399
                        nextCursor += size;
                        if (!entry.isDirectory()) {
                            return new JarInputFile(entry, JarInputDir.this, cursor, nextCursor);
                        }
                    } catch (IOException e) {
                        throw new ReflectionsException("could not get next zip entry", e);
                    }
                }
            }
        };
    }

    public void close() {
        try { if (jarInputStream != null) ((InputStream) jarInputStream).close(); }
        catch (IOException e) {
            if (Reflections.log != null) {
                Reflections.log.warn("Could not close InputStream", e);
            }
        }
    }
}
