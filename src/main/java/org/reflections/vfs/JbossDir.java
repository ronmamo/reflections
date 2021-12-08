package org.reflections.vfs;

import org.jboss.vfs.VirtualFile;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.jar.JarFile;

public class JbossDir implements Vfs.Dir {

    private final VirtualFile virtualFile;

    private JbossDir(VirtualFile virtualFile) {
        this.virtualFile = virtualFile;
    }

    public static Vfs.Dir createDir(URL url) throws IOException {
        VirtualFile virtualFile = (VirtualFile) url.openConnection().getContent();
        if(virtualFile.isFile()) {
            return new ZipDir(new JarFile(virtualFile.getPhysicalFile()));
        }
        return new JbossDir(virtualFile);
    }


    @Override
    public String getPath() {
        return virtualFile.getPathName();
    }

    @Override
    public Iterable<Vfs.File> getFiles() {
        return () -> new Iterator<Vfs.File>() {
            final Stack<VirtualFile> stack = new Stack<>();
            Vfs.File entry = null;

            {
                stack.addAll(virtualFile.getChildren());
            }

            @Override
            public boolean hasNext() {
                return entry != null || (entry = computeNext()) != null;
            }

            /** CS427 Issue link: https://github.com/ronmamo/reflections/issues/338 */
            @Override
            public Vfs.File next() {
                if(!hasNext()){
                    throw new NoSuchElementException();
                }
                Vfs.File next = entry;
                entry = null;
                return next;
            }

            private Vfs.File computeNext() {
                while (!stack.isEmpty()) {
                    final VirtualFile file = stack.pop();
                    if (file.isDirectory()) {
                        stack.addAll(file.getChildren());
                    } else {
                        return new JbossFile(JbossDir.this, file);
                    }
                }
                return null;
            }
        };
    }
}
