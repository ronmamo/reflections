package org.reflections.vfs;

import org.reflections.Reflections;

import java.io.IOException;
import java.util.jar.JarFile;

/** an implementation of {@link org.reflections.vfs.Vfs.Dir} for {@link java.util.zip.ZipFile} */
public class ZipDir implements Vfs.Dir {
    final java.util.zip.ZipFile jarFile;

    public ZipDir(JarFile jarFile) {
        this.jarFile = jarFile;
    }

    public String getPath() {
        if (jarFile == null) {
            return "/NO-SUCH-DIRECTORY/";
        }
        return jarFile.getName().replace("\\", "/");
    }

    public Iterable<Vfs.File> getFiles() {
        return () -> jarFile.stream()
                .filter(entry -> !entry.isDirectory())
                .map(entry -> (Vfs.File) new ZipFile(ZipDir.this, entry))
                .iterator();
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
