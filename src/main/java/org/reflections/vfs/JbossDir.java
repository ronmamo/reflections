package org.reflections.vfs;

import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualJarInputStream;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.Iterator;
import java.util.Stack;
import java.util.jar.JarFile;

public class JbossDir implements Vfs.Dir {

    private final VirtualFile virtualFile;

    private JbossDir(VirtualFile virtualFile) {
        this.virtualFile = virtualFile;
    }

    public static Vfs.Dir createDir(URL url) throws Exception {
        Object content = url.openConnection().getContent();
        if (content instanceof VirtualJarInputStream) {
            Field root = content.getClass().getDeclaredField("root");
            root.setAccessible(true);
            content = root.get(content);
        }
        VirtualFile virtualFile = (VirtualFile) content;
        if (virtualFile.isFile()) {
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

            @Override
            public Vfs.File next() {
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
