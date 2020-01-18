package org.reflections.vfs;

import org.jboss.vfs.VirtualFile;

import java.io.IOException;
import java.io.InputStream;

public class JbossFile implements Vfs.File {

    private final JbossDir root;
    private final VirtualFile virtualFile;

    public JbossFile(final JbossDir root, VirtualFile virtualFile) {
        this.root = root;
        this.virtualFile = virtualFile;
    }

    @Override
    public String getName() {
        return virtualFile.getName();
    }

    @Override
    public String getRelativePath() {
        String filepath  = virtualFile.getPathName();
        if(filepath.startsWith(root.getPath())) {
            return filepath.substring(root.getPath().length() + 1);
        }

        return null;
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return virtualFile.openStream();
    }
}
