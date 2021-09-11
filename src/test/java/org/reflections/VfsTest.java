package org.reflections;

import javassist.bytecode.ClassFile;
import org.junit.Test;
import org.reflections.adapters.JavassistAdapter;
import org.reflections.util.ClasspathHelper;
import org.reflections.vfs.SystemDir;
import org.reflections.vfs.Vfs;
import org.slf4j.Logger;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Comparator;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static java.text.MessageFormat.format;
import static org.junit.Assert.*;

/**
 *
 */
public class VfsTest {

    @Test
    public void testJarFile() throws Exception {
        URL url = new URL(ClasspathHelper.forClass(Logger.class).toExternalForm().replace("jar:", ""));
        assertTrue(url.toString().startsWith("file:"));
        assertTrue(url.toString().contains(".jar"));

        assertTrue(Vfs.DefaultUrlTypes.jarFile.matches(url));
        assertFalse(Vfs.DefaultUrlTypes.jarUrl.matches(url));
        assertFalse(Vfs.DefaultUrlTypes.directory.matches(url));

        Vfs.Dir dir = Vfs.DefaultUrlTypes.jarFile.createDir(url);
        testVfsDir(dir);
    }

    @Test
    public void testJarUrl() throws Exception {
        URL url = ClasspathHelper.forClass(Logger.class);
        assertTrue(url.toString().startsWith("jar:file:"));
        assertTrue(url.toString().contains(".jar!"));

        assertFalse(Vfs.DefaultUrlTypes.jarFile.matches(url));
        assertTrue(Vfs.DefaultUrlTypes.jarUrl.matches(url));
        assertFalse(Vfs.DefaultUrlTypes.directory.matches(url));

        Vfs.Dir dir = Vfs.DefaultUrlTypes.jarUrl.createDir(url);
        testVfsDir(dir);
    }

    @Test
    public void testDirectory() throws Exception {
        URL url = ClasspathHelper.forClass(getClass());
        assertTrue(url.toString().startsWith("file:"));
        assertFalse(url.toString().contains(".jar"));

        assertFalse(Vfs.DefaultUrlTypes.jarFile.matches(url));
        assertFalse(Vfs.DefaultUrlTypes.jarUrl.matches(url));
        assertTrue(Vfs.DefaultUrlTypes.directory.matches(url));

        Vfs.Dir dir = Vfs.DefaultUrlTypes.directory.createDir(url);
        testVfsDir(dir);
    }

    @Test
    public void testJarInputStream() throws Exception {
        URL url = ClasspathHelper.forClass(Logger.class);
        assertTrue(Vfs.DefaultUrlTypes.jarInputStream.matches(url));
        try {
            testVfsDir(Vfs.DefaultUrlTypes.jarInputStream.createDir(url));
            fail();
        } catch (ReflectionsException e) {
            // expected
        }

        url = new URL(ClasspathHelper.forClass(Logger.class).toExternalForm().replace("jar:", "").replace(".jar!", ".jar"));
        assertTrue(Vfs.DefaultUrlTypes.jarInputStream.matches(url));
        testVfsDir(Vfs.DefaultUrlTypes.jarInputStream.createDir(url));

        url = ClasspathHelper.forClass(getClass());
        assertFalse(Vfs.DefaultUrlTypes.jarInputStream.matches(url));
        try {
            testVfsDir(Vfs.DefaultUrlTypes.jarInputStream.createDir(url));
            fail();
        } catch (NullPointerException e) {
            // expected
        }
    }

    @Test
    public void dirWithSpaces() {
        Collection<URL> urls = ClasspathHelper.forPackage("dir+with spaces");
        assertFalse(urls.isEmpty());
        for (URL url : urls) {
            Vfs.Dir dir = Vfs.fromURL(url);
            assertNotNull(dir);
            assertNotNull(dir.getFiles().iterator().next());
        }
    }

    @Test
    public void vfsFromDirWithJarInName() throws MalformedURLException {
        String tmpFolder = System.getProperty("java.io.tmpdir");
        tmpFolder = tmpFolder.endsWith(File.separator) ? tmpFolder : tmpFolder + File.separator;
        String dirWithJarInName = tmpFolder + "tony.jarvis";
        File newDir = new File(dirWithJarInName);
        newDir.mkdir();

        try {
            Vfs.Dir dir = Vfs.fromURL(new URL(format("file:{0}", dirWithJarInName)));

            assertEquals(dirWithJarInName, dir.getPath());
            assertEquals(SystemDir.class, dir.getClass());
        } finally {
            newDir.delete();
        }
    }

    @Test
    public void vfsFromDirWithJarInJar() {
        String tmpFolder = System.getProperty("java.io.tmpdir");
        tmpFolder = tmpFolder.endsWith(File.separator) ? tmpFolder : tmpFolder + File.separator;
        String dirWithJarInJar = tmpFolder + "jarinjar";
        try {
            Path lib = Paths.get(dirWithJarInJar, "BOOT-INF", "lib");
            if (Files.exists(Paths.get(dirWithJarInJar)))
                Files.walk(Paths.get(dirWithJarInJar)).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);

            Files.createDirectories(lib);
            Path jar = Paths.get(Logger.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Files.copy(jar, lib.resolve(jar.getFileName()));

            File tempjar = new File(dirWithJarInJar + File.separator + "output.jar");
            Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
            JarOutputStream target = new JarOutputStream(new FileOutputStream(tempjar), manifest);
            createJar(new File(dirWithJarInJar + File.separator + "BOOT-INF"), new File(dirWithJarInJar), target);
            target.close();

            URL innerjarurl = new URL(format("jar:{0}!/BOOT-INF/lib/{1}", tempjar.toURI().toString(), jar.getFileName()));

            assertFalse(Vfs.DefaultUrlTypes.jarUrl.matches(innerjarurl));
            assertTrue(Vfs.DefaultUrlTypes.jarInputStream.matches(innerjarurl));

            Vfs.Dir jarUrlDir = Vfs.DefaultUrlTypes.jarUrl.createDir(innerjarurl);
            assertNotEquals(innerjarurl.getPath(), jarUrlDir.getPath());

            Vfs.Dir jarInputStreamDir = Vfs.DefaultUrlTypes.jarInputStream.createDir(innerjarurl);
            assertEquals(innerjarurl.getPath(), jarInputStreamDir.getPath());
        }  catch (Exception e) {
        } finally {
            try {
                Files.walk(Paths.get(dirWithJarInJar)).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            } catch (IOException e) {
            }
        }
    }

    private void testVfsDir(Vfs.Dir dir) {
        JavassistAdapter mdAdapter = new JavassistAdapter();
        Vfs.File file = null;
        for (Vfs.File f : dir.getFiles()) {
            if (f.getRelativePath().endsWith(".class")) {
                file = f;
                break;
            }
        }

        ClassFile stringCF = mdAdapter.getOrCreateClassObject(file);
        String className = mdAdapter.getClassName(stringCF);
        assertFalse(className.isEmpty());
    }

    private void createJar(File source, File baseDir, JarOutputStream target) {
        BufferedInputStream in = null;

        try {
            if (!source.exists()){
                throw new IOException("Source directory is empty");
            }
            if (source.isDirectory()) {
                // For Jar entries, all path separates should be '/'(OS independent)
                String name = baseDir.toPath().relativize(source.toPath()).toFile().getPath().replace("\\", "/");
                if (!name.isEmpty()) {
                    if (!name.endsWith("/")) {
                        name += "/";
                    }
                    JarEntry entry = new JarEntry(name);
                    entry.setTime(source.lastModified());
                    target.putNextEntry(entry);
                    target.closeEntry();
                }
                for (File nestedFile : source.listFiles()) {
                    createJar(nestedFile, baseDir, target);
                }
                return;
            }

            String entryName = baseDir.toPath().relativize(source.toPath()).toFile().getPath().replace("\\", "/");
            JarEntry entry = new JarEntry(entryName);
            entry.setTime(source.lastModified());
            target.putNextEntry(entry);
            in = new BufferedInputStream(new FileInputStream(source));

            byte[] buffer = new byte[1024];
            while (true) {
                int count = in.read(buffer);
                if (count == -1)
                    break;
                target.write(buffer, 0, count);
            }
            target.closeEntry();
        } catch (Exception ignored) {
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ignored) {
                    throw new RuntimeException(ignored);
                }
            }
        }
    }
}