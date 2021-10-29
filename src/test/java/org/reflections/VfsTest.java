package org.reflections;

import org.junit.jupiter.api.Test;
import org.reflections.util.ClasspathHelper;
import org.reflections.vfs.SystemDir;
import org.reflections.vfs.Vfs;
import org.slf4j.Logger;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.text.MessageFormat.format;
import static org.junit.jupiter.api.Assertions.*;

public class VfsTest {

    @Test
    public void testJarFile() throws Exception {
        URL url = new URL(ClasspathHelper.forClass(Logger.class).toExternalForm().replace("jar:", ""));
        assertTrue(url.toString().startsWith("file:"));
        assertTrue(url.toString().contains(".jar"));

        assertTrue(Vfs.DefaultUrlTypes.JAR_FILE.matches(url));
        assertFalse(Vfs.DefaultUrlTypes.JAR_URL.matches(url));
        assertFalse(Vfs.DefaultUrlTypes.DIRECTORY.matches(url));

        Vfs.Dir dir = Vfs.DefaultUrlTypes.JAR_FILE.createDir(url);
        testVfsDir(dir);
    }

    @Test
    public void testJarUrl() throws Exception {
        URL url = ClasspathHelper.forClass(Logger.class);
        assertTrue(url.toString().startsWith("jar:file:"));
        assertTrue(url.toString().contains(".jar!"));

        assertFalse(Vfs.DefaultUrlTypes.JAR_FILE.matches(url));
        assertTrue(Vfs.DefaultUrlTypes.JAR_URL.matches(url));
        assertFalse(Vfs.DefaultUrlTypes.DIRECTORY.matches(url));

        Vfs.Dir dir = Vfs.DefaultUrlTypes.JAR_URL.createDir(url);
        testVfsDir(dir);
    }

    @Test
    public void testDirectory() throws Exception {
        URL url = ClasspathHelper.forClass(getClass());
        assertTrue(url.toString().startsWith("file:"));
        assertFalse(url.toString().contains(".jar"));

        assertFalse(Vfs.DefaultUrlTypes.JAR_FILE.matches(url));
        assertFalse(Vfs.DefaultUrlTypes.JAR_URL.matches(url));
        assertTrue(Vfs.DefaultUrlTypes.DIRECTORY.matches(url));

        Vfs.Dir dir = Vfs.DefaultUrlTypes.DIRECTORY.createDir(url);
        testVfsDir(dir);
    }

    @Test
    public void testJarInputStream() throws Exception {
        URL url = ClasspathHelper.forClass(Logger.class);
        assertTrue(Vfs.DefaultUrlTypes.JAR_INPUT_STREAM.matches(url));
        try {
            testVfsDir(Vfs.DefaultUrlTypes.JAR_INPUT_STREAM.createDir(url));
            fail();
        } catch (ReflectionsException e) {
            // expected
        }

        url = new URL(ClasspathHelper.forClass(Logger.class).toExternalForm().replace("jar:", "").replace(".jar!", ".jar"));
        assertTrue(Vfs.DefaultUrlTypes.JAR_INPUT_STREAM.matches(url));
        testVfsDir(Vfs.DefaultUrlTypes.JAR_INPUT_STREAM.createDir(url));

        url = ClasspathHelper.forClass(getClass());
        assertFalse(Vfs.DefaultUrlTypes.JAR_INPUT_STREAM.matches(url));
        try {
            testVfsDir(Vfs.DefaultUrlTypes.JAR_INPUT_STREAM.createDir(url));
            fail();
        } catch (AssertionError e) {
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

            assertEquals(dirWithJarInName.replace("\\", "/"), dir.getPath());
            assertEquals(SystemDir.class, dir.getClass());
        } finally {
            newDir.delete();
        }
    }

    private void testVfsDir(Vfs.Dir dir) {
        List<Vfs.File> files = new ArrayList<>();
        for (Vfs.File file : dir.getFiles()) {
            files.add(file);
        }
        assertFalse(files.isEmpty());
    }
}
