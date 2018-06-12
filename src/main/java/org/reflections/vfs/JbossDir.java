package org.reflections.vfs;

import com.google.common.collect.AbstractIterator;
import org.jboss.vfs.VirtualFile;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.Stack;
import java.util.jar.JarFile;

public class JbossDir implements Vfs.Dir {

    private final VirtualFile virtualFile;

    private JbossDir(VirtualFile virtualFile) throws IOException {
        this.virtualFile = virtualFile;
    }

    public static Vfs.Dir createDir(URL url) throws Exception {
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
        return new Iterable<Vfs.File>() {
            public Iterator<Vfs.File> iterator() {
                return new AbstractIterator<Vfs.File>() {
                    final Stack<VirtualFile> stack = new Stack<>();
                    {
                        stack.addAll(virtualFile.getChildren());
                    }

                    protected Vfs.File computeNext() {
                        while (!stack.isEmpty()) {
                            final VirtualFile file = stack.pop();
                            if (file.isDirectory()) {
                                stack.addAll(file.getChildren());
                            } else {
                                return new JbossFile(JbossDir.this, file);
                            }
                        }

                        return endOfData();
                    }
                };
            }
        };
    }

    @Override
    public void close() {

    }

}
