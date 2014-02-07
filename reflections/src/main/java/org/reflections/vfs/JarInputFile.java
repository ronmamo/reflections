package org.reflections.vfs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;

/**
*
*/
public class JarInputFile implements Vfs.File {
    private final String name;
    private final String path;
    private final byte[] data;

    public JarInputFile(ZipEntry entry, byte[] data) {
        this.path = entry.getName();
        this.name = this.path.substring(path.lastIndexOf("/") + 1);
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public String getRelativePath() {
        return path;
    }

    public InputStream openInputStream() throws IOException {
        return new ByteArrayInputStream(data);
    }
}
