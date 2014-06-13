package org.reflections.vfs;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;

/** an implementation of {@link org.reflections.vfs.Vfs.File} for {@link java.util.zip.ZipEntry} */
public class ZipFile implements Vfs.File {
    private final ZipDir root;
    private final ZipEntry entry;

    public ZipFile(final ZipDir root, ZipEntry entry) {
        this.root = root;
        this.entry = entry;
    }

    public String getName() {
        String name = entry.getName();
        return name.substring(name.lastIndexOf("/") + 1);
    }

    public String getRelativePath() {
        return entry.getName();
    }

    public InputStream openInputStream() throws IOException {
        return root.jarFile.getInputStream(entry);
    }

    @Override
    public String toString() {
        return root.getPath() + "!" + java.io.File.separatorChar + entry.toString();
    }
}
