package org.reflections.vfs;

import com.google.common.collect.AbstractIterator;
import org.apache.commons.vfs2.*;
import org.reflections.Reflections;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

/**
 * A {@link org.reflections.vfs.Vfs.UrlType} using the commons vfs 2 library, for creating {@link org.reflections.vfs.Vfs.Dir}.
 * <p>It can be plugged into using {@link Vfs#addDefaultURLTypes(Vfs.UrlType)}, and requires the library provided in classpath
 */
public class CommonsVfs2UrlType implements Vfs.UrlType {

    @Override
    public boolean matches(URL url) throws Exception {
        try {
            final FileSystemManager manager = VFS.getManager();
            final FileObject fileObject = manager.resolveFile(url.toExternalForm());
            return fileObject.exists() && fileObject.getType() == FileType.FOLDER;
        } catch (FileSystemException e) {
            Reflections.log.warn("Could not create CommonsVfs2UrlType from url " + url.toExternalForm(), e);
            return false;
        }
    }

    @Override
    public Vfs.Dir createDir(URL url) throws Exception {
        final FileSystemManager manager = VFS.getManager();
        final FileObject fileObject = manager.resolveFile(url.toExternalForm());
        return new CommonsVfs2UrlType.Dir(fileObject);
    }

    public static class Dir implements Vfs.Dir {
        private final FileObject file;

        public Dir(FileObject file) {
            this.file = file;
        }

        public String getPath() {
            try {
                return file.getURL().getPath();
            } catch (FileSystemException e) {
                throw new RuntimeException(e);
            }
        }

        public Iterable<Vfs.File> getFiles() {
            return new Iterable<Vfs.File>() {
                public Iterator<Vfs.File> iterator() {
                    return new FileAbstractIterator();
                }
            };
        }

        public void close() {
            try {
                file.close();
            } catch (FileSystemException e) {
                //todo log
            }
        }

        private class FileAbstractIterator extends AbstractIterator<Vfs.File> {
            final Stack<FileObject> stack = new Stack<FileObject>();

            {
                listDir(file);}

            protected Vfs.File computeNext() {
                while (!stack.isEmpty()) {
                    final FileObject file = stack.pop();
                    try {
                        if (isDir(file)) listDir(file); else return getFile(file);
                    } catch (FileSystemException e) {
                        throw new RuntimeException(e);
                    }
                }

                return endOfData();
            }

            private File getFile(FileObject file) {
                return new File(Dir.this.file, file);
            }

            private boolean listDir(FileObject file) {
                return stack.addAll(listFiles(file));
            }

            private boolean isDir(FileObject file) throws FileSystemException {
                return file.getType() == FileType.FOLDER;
            }

            protected List<FileObject> listFiles(final FileObject file) {
                try {
                    FileObject[] files = file.getType().hasChildren() ? file.getChildren() : null;
                    return files != null ? Arrays.asList(files) : new ArrayList<FileObject>();
                } catch (FileSystemException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static class File implements Vfs.File {
        private final FileObject root;
        private final FileObject file;

        public File(FileObject root, FileObject file) {
            this.root = root;
            this.file = file;
        }

        public String getName() {
            return file.getName().getBaseName();
        }

        public String getRelativePath() {
            String filepath = file.getName().getPath().replace("\\", "/");
            if (filepath.startsWith(root.getName().getPath())) {
                return filepath.substring(root.getName().getPath().length() + 1);
            }

            return null; //should not get here
        }

        public InputStream openInputStream() throws IOException {
            return file.getContent().getInputStream();
        }
    }
}
