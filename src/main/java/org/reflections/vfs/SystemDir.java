package org.reflections.vfs;

import org.reflections.ReflectionsException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.util.Collections;

/*
 * An implementation of {@link org.reflections.vfs.Vfs.Dir} for directory {@link java.io.File}.
 */
public class SystemDir implements Vfs.Dir {
    private final File file;

    public SystemDir(File file) throws NotDirectoryException {
        if (file != null && (!file.isDirectory() || !file.canRead())) {
            throw new NotDirectoryException("cannot use dir " + file);
        }
        this.file = file;
    }

    public String getPath() {
        return file != null ? file.getPath().replace("\\", "/") : "/NO-SUCH-DIRECTORY/";
    }

    public Iterable<Vfs.File> getFiles() {
        if (file == null || !file.exists()){
            return Collections.emptyList();
        }
        return () -> {
            try {
                return Files.walk(file.toPath())
                        .filter(Files::isRegularFile)
                        .map(path -> (Vfs.File) new SystemFile(SystemDir.this, path.toFile()))
                        .iterator();
            } catch (IOException e) {
                throw new ReflectionsException("could not get files for " + file, e);
            }
        };
    }
}
