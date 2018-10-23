package org.reflections.vfs;

import java.util.Collections;

import org.reflections.vfs.SystemDir;
import org.reflections.vfs.SystemFile;
import org.reflections.vfs.Vfs.Dir;
import org.reflections.vfs.Vfs.File;

/**
 * Implementation of Dir representing a class file.
 */
public class ClassFileDir implements Dir
{

    private final java.io.File file;

    public ClassFileDir(java.io.File file)
    {
        this.file = file;
    }

    @Override
    public String getPath()
    {
        return this.file.getName();
    }

    @Override
    public Iterable<File> getFiles()
    {
        return Collections.singleton((Vfs.File) new SystemFile(new SystemDir(this.file.getParentFile()), this.file));
    }

    @Override
    public void close()
    {
    }

}
