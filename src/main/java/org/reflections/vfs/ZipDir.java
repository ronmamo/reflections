package org.reflections.vfs;

import org.reflections.Reflections;

import java.io.IOException;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

/** an implementation of {@link org.reflections.vfs.Vfs.Dir} for {@link java.util.zip.ZipFile} */
public class ZipDir implements Vfs.Dir {
    final java.util.zip.ZipFile jarFile;

    public ZipDir(final JarFile jarFile) {
        this.jarFile = jarFile;
    }

    public String getPath() {
        return jarFile.getName();
    }

    public Stream<Vfs.File> getFiles() {
        return jarFile.stream()
                .filter(e->!e.isDirectory())
                .map(e -> new ZipFile(ZipDir.this, e));
    }

    public void close() {
        try { jarFile.close(); } catch (IOException e) {
            if (Reflections.log != null) {
                Reflections.log.warn("Could not close JarFile", e);
            }
        }
    }

    @Override
    public String toString() {
        return jarFile.getName();
    }
}
