package org.reflections.vfs;

import org.reflections.ReflectionsException;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 *
 */
public class JarInputDir implements Vfs.Dir {
    private final URL url;

    public JarInputDir(URL url) {
        this.url = url;
    }

    public String getPath() {
        return url.getPath();
    }

    public Stream<Vfs.File> getFiles() {
        try {
            return Files.walk(Paths.get(url.toURI()))
                    .filter(Files::isRegularFile)
                    .map(path -> new JarInputFile(JarInputDir.this, path));
        }
        catch (Exception e) { throw new ReflectionsException("Could not open url connection", e); }
    }

    public void close() { }
}
