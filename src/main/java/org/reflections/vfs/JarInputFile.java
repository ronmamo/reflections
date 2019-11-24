package org.reflections.vfs;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;

/**
*
*/
public class JarInputFile implements Vfs.File {
    private final JarInputDir root;
    private final Path entry;

    public JarInputFile(final JarInputDir root, Path entry) {
        this.root = root;
        this.entry = entry;
    }

    public String getName() {
        final String name = entry.toString();
        return name.substring(name.lastIndexOf("/") + 1);
    }

    public String getRelativePath() {
        return entry.toString();
    }

    public InputStream openInputStream() throws IOException {
        return Files.newInputStream(entry);
    }

    @Override
    public String toString() {
        return root.getPath() + "!" + java.io.File.separatorChar + entry.toString();
    }
}
