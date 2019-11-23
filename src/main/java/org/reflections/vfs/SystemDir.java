package org.reflections.vfs;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
 * An implementation of {@link org.reflections.vfs.Vfs.Dir} for directory {@link java.io.File}.
 */
public class SystemDir implements Vfs.Dir {
    private final File file;

    public SystemDir(File file) {
        if (file != null && (!file.isDirectory() || !file.canRead())) {
            throw new RuntimeException("cannot use dir " + file);
        }

        this.file = file;
    }

    public String getPath() {
        if (file == null) {
            return "/NO-SUCH-DIRECTORY/";
        }
        return file.getPath().replace("\\", "/");
    }

    public Stream<Vfs.File> getFiles() {

        if (file == null || !file.exists()) {
            return Stream.empty();
        }

        final Stack<File> stack = new Stack<File>();
        {stack.addAll(listFiles(file));}

        return Stream.generate(() -> {
            while (!stack.isEmpty()) {
                final File file = stack.pop();
                if (file.isDirectory()) {
                    stack.addAll(listFiles(file));
                } else {
                    return new SystemFile(SystemDir.this, file);
                }
            }

            return null;
        });
    }

    private static List<File> listFiles(final File file) {
        final File[] files = file.listFiles();

        if (files != null)
            return Arrays.stream(files)
                    .collect(Collectors.toList());
        else
            return new ArrayList<>();
    }

    public void close() {
    }

    @Override
    public String toString() {
        return getPath();
    }
}
