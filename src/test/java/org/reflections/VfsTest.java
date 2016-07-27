package org.reflections;

import static java.text.MessageFormat.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.jar.JarFile;

import org.junit.Ignore;
import org.junit.Test;
import org.reflections.adapters.JavassistAdapter;
import org.reflections.util.ClasspathHelper;
import org.reflections.vfs.JarInputDir;
import org.reflections.vfs.SystemDir;
import org.reflections.vfs.Vfs;
import org.reflections.vfs.ZipDir;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import javassist.bytecode.ClassFile;

/** */
public class VfsTest {

    @Test
    public void allKindsOfShittyUrls() throws Exception {
        JavassistAdapter mdAdapter = new JavassistAdapter();

        {
            URL jar1 = getSomeJar();
            assertTrue(jar1.toString().startsWith("file:"));
            assertTrue(jar1.toString().contains(".jar"));

            assertTrue(Vfs.DefaultUrlTypes.jarFile.matches(jar1));
            assertFalse(Vfs.DefaultUrlTypes.jarUrl.matches(jar1));
            assertFalse(Vfs.DefaultUrlTypes.directory.matches(jar1));

            Vfs.Dir dir = Vfs.DefaultUrlTypes.jarFile.createDir(jar1);
            Vfs.File file = null;
            for (Vfs.File f : dir.getFiles()) {
                if (f.getRelativePath().endsWith(".class")) { file = f; break; }
            }

            ClassFile stringCF = mdAdapter.getOfCreateClassObject(file);
            //noinspection UnusedDeclaration
            String className = mdAdapter.getClassName(stringCF);
        }

        {
            URL rtJarUrl = ClasspathHelper.forClass(String.class);
            assertTrue(rtJarUrl.toString().startsWith("jar:file:"));
            assertTrue(rtJarUrl.toString().contains(".jar!"));

            assertFalse(Vfs.DefaultUrlTypes.jarFile.matches(rtJarUrl));
            assertTrue(Vfs.DefaultUrlTypes.jarUrl.matches(rtJarUrl));
            assertFalse(Vfs.DefaultUrlTypes.directory.matches(rtJarUrl));

            Vfs.Dir dir = Vfs.DefaultUrlTypes.jarUrl.createDir(rtJarUrl);
            Vfs.File file = null;
            for (Vfs.File f : dir.getFiles()) {
                if (f.getRelativePath().equals("java/lang/String.class")) { file = f; break; }
            }

            ClassFile stringCF = mdAdapter.getOfCreateClassObject(file);
            String className = mdAdapter.getClassName(stringCF);
            assertTrue(className.equals("java.lang.String"));
        }

        {
            URL thisUrl = ClasspathHelper.forClass(getClass());
            assertTrue(thisUrl.toString().startsWith("file:"));
            assertFalse(thisUrl.toString().contains(".jar"));

            assertFalse(Vfs.DefaultUrlTypes.jarFile.matches(thisUrl));
            assertFalse(Vfs.DefaultUrlTypes.jarUrl.matches(thisUrl));
            assertTrue(Vfs.DefaultUrlTypes.directory.matches(thisUrl));

            Vfs.Dir dir = Vfs.DefaultUrlTypes.directory.createDir(thisUrl);
            Vfs.File file = null;
            for (Vfs.File f : dir.getFiles()) {
                if (f.getRelativePath().equals("org/reflections/VfsTest.class")) { file = f; break; }
            }

            ClassFile stringCF = mdAdapter.getOfCreateClassObject(file);
            String className = mdAdapter.getClassName(stringCF);
            assertTrue(className.equals(getClass().getName()));
        }
        {
            // create a file, then delete it so we can treat as a non-existing directory
            File tempFile = File.createTempFile("nosuch", "dir");
            tempFile.delete();
            assertFalse(tempFile.exists());
            Vfs.Dir dir = Vfs.DefaultUrlTypes.directory.createDir(tempFile.toURL());
            assertNotNull(dir);
            assertFalse(dir.getFiles().iterator().hasNext());
            assertNotNull(dir.getPath());
            assertNotNull(dir.toString());
            dir.close();
        }

    }

    @Test public void dirWithSpaces() {
        Collection<URL> urls = ClasspathHelper.forPackage("dir+with spaces");
        assertFalse(urls.isEmpty());
        for (URL url : urls) {
            testVfsDir(url);
        }
    }

    @Test
    public void vfsFromJar() {
        testVfsDir(getSomeJar());
    }

    @Test
    public void vfsFromDir() {
        testVfsDir(getSomeDirectory());
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
    public void vfsFromDirWithinAJarUrl() throws MalformedURLException {
    	URL directoryInJarUrl = ClasspathHelper.forClass(String.class);
        assertTrue(directoryInJarUrl.toString().startsWith("jar:file:"));
        assertTrue(directoryInJarUrl.toString().contains(".jar!"));
        
        String directoryInJarPath = directoryInJarUrl.toExternalForm().replaceFirst("jar:", "");
        int start = directoryInJarPath.indexOf(":") + 1;
		int end = directoryInJarPath.indexOf(".jar!") + 4;
		String expectedJarFile = directoryInJarPath.substring(start, end);
        
        Vfs.Dir dir = Vfs.fromURL(new URL(directoryInJarPath));

        assertEquals(ZipDir.class, dir.getClass());
        assertEquals(expectedJarFile, dir.getPath());
    }

    @Test
    public void vfsFromJarFileUrl() throws MalformedURLException {
        testVfsDir(new URL("jar:file:" + getSomeJar().getPath() + "!/"));
    }

    @Test
    public void findFilesFromEmptyMatch() throws MalformedURLException {
        final URL jar = getSomeJar();
        final Iterable<Vfs.File> files = Vfs.findFiles(java.util.Arrays.asList(jar), Predicates.<Vfs.File>alwaysTrue());
        assertNotNull(files);
        assertTrue(files.iterator().hasNext());
    }

    private void testVfsDir(URL url) {
        System.out.println("testVfsDir(" + url + ")");
        assertNotNull(url);

        Vfs.Dir dir = Vfs.fromURL(url);
        assertNotNull(dir);

        Iterable<Vfs.File> files = dir.getFiles();
        Vfs.File first = files.iterator().next();
        assertNotNull(first);

        first.getName();
        try {
            first.openInputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        dir.close();
    }

    @Test @Ignore
    public void vfsFromHttpUrl() throws MalformedURLException {
        Vfs.addDefaultURLTypes(new Vfs.UrlType() {
            public boolean matches(URL url)         {return url.getProtocol().equals("http");}
            public Vfs.Dir createDir(final URL url) {return new HttpDir(url);}
        });

        testVfsDir(new URL("http://mirrors.ibiblio.org/pub/mirrors/maven2/org/slf4j/slf4j-api/1.5.6/slf4j-api-1.5.6.jar"));
    }

    //this is just for the test...
    static class HttpDir implements Vfs.Dir {
        private final File file;
        private final ZipDir zipDir;
        private final String path;

        HttpDir(URL url) {
            this.path = url.toExternalForm();
            try {file = downloadTempLocally(url);}
            catch (IOException e) {throw new RuntimeException(e);}
            try { zipDir = new ZipDir(new JarFile(file)); } catch (Exception e) { throw new RuntimeException(e); }
        }

        public String getPath() {return path;}
        public Iterable<Vfs.File> getFiles() {return zipDir.getFiles();}
        public void close() {file.delete();}

        private static java.io.File downloadTempLocally(URL url) throws IOException {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            if (connection.getResponseCode() == 200) {
                java.io.File temp = java.io.File.createTempFile("urlToVfs", "tmp");
                FileOutputStream out = new FileOutputStream(temp);
                DataInputStream in = new DataInputStream(connection.getInputStream());

                int len; byte ch[] = new byte[1024];
                while ((len = in.read(ch)) != -1) {out.write(ch, 0, len);}

                connection.disconnect();
                return temp;
            }

            return null;
        }
    }

    @Test
    public void vfsFromJarWithInnerJars() {
        //todo?
    }

    @Test
    public void jarInputStream() {
        JavassistAdapter javassistAdapter = new JavassistAdapter();

        for (URL jar : ClasspathHelper.forClassLoader()) {
            try {
                for (Vfs.File file : Iterables.limit(new JarInputDir(jar).getFiles(), 5)) {
                    if (file.getName().endsWith(".class")) {
                        String className = javassistAdapter.getClassName(javassistAdapter.getOfCreateClassObject(file));
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    //
    private URL getSomeJar() {
        Collection<URL> urls = ClasspathHelper.forClassLoader();
        for (URL url : urls) {
            if (!url.toExternalForm().contains("surefire") && url.toExternalForm().endsWith(".jar")) return url; //damn
        }
        throw new RuntimeException();
    }

    private URL getSomeDirectory() {
        try {
            return new File(ReflectionsTest.getUserDir()).toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}