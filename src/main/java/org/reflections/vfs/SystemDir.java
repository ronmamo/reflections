package org.reflections.vfs;

import com.google.common.collect.AbstractIterator;

import java.io.File;
import java.util.*;

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

  @Override
  public String toString() {
    return getPath();
  }

  public String getPath() {
    if (file == null) {
      return "/NO-SUCH-DIRECTORY/";
    }
    return file.getPath().replace("\\", "/");
  }

  public Iterable<Vfs.File> getFiles() {
    if (file == null || !file.exists()) {
      return Collections.emptyList();
    }
    return () -> new AbstractIterator<Vfs.File>() {
      final Stack<File> stack = new Stack<File>();

      {
        stack.addAll(listFiles(file));
      }

      protected Vfs.File computeNext() {
        while (!stack.isEmpty()) {
          final File file = stack.pop();
          if (file.isDirectory()) {
            stack.addAll(listFiles(file));
          } else {
            return new SystemFile(SystemDir.this, file);
          }
        }

        return endOfData();
      }
    };
  }

  private static List<File> listFiles(final File file) {
    File[] files = file.listFiles();

    if (files != null)
      return new ArrayList<>(Arrays.asList(files));
    else
      return new ArrayList<>();
  }

  public void close() {
  }
}
