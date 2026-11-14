package org.reflections.vfs;

import org.reflections.Reflections;
import org.reflections.ReflectionsException;
import org.reflections.util.ClasspathHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Predicate;
import java.util.jar.JarFile;

/**
 * a simple virtual file system bridge
 * <p>use the {@link org.reflections.vfs.Vfs#fromURL(java.net.URL)} to get a {@link org.reflections.vfs.Vfs.Dir},
 * then use {@link org.reflections.vfs.Vfs.Dir#getFiles()} to iterate over the {@link org.reflections.vfs.Vfs.File}
 * <p>for example:
 * <pre>
 *      Vfs.Dir dir = Vfs.fromURL(url);
 *      Iterable<Vfs.File> files = dir.getFiles();
 *      for (Vfs.File file : files) {
 *          InputStream is = file.openInputStream();
 *      }
 * </pre>
 * <p>{@link org.reflections.vfs.Vfs#fromURL(java.net.URL)} uses static {@link org.reflections.vfs.Vfs.DefaultUrlTypes} to resolve URLs.
 * It contains VfsTypes for handling for common resources such as local jar file, local directory, jar url, jar input stream and more.
 * <p>It can be plugged in with other {@link org.reflections.vfs.Vfs.UrlType} using {@link org.reflections.vfs.Vfs#addDefaultURLTypes(org.reflections.vfs.Vfs.UrlType)} or {@link org.reflections.vfs.Vfs#setDefaultURLTypes(java.util.List)}.
 * <p>for example:
 * <pre>
 *      Vfs.addDefaultURLTypes(new Vfs.UrlType() {
 *          public boolean matches(URL url)         {
 *              return url.getProtocol().equals("http");
 *          }
 *          public Vfs.Dir createDir(final URL url) {
 *              return new HttpDir(url); //implement this type... (check out a naive implementation on VfsTest)
 *          }
 *      });
 *
 *      Vfs.Dir dir = Vfs.fromURL(new URL("http://mirrors.ibiblio.org/pub/mirrors/maven2/org/slf4j/slf4j-api/1.5.6/slf4j-api-1.5.6.jar"));
 * </pre>
 * <p>use {@link org.reflections.vfs.Vfs#findFiles(java.util.Collection, java.util.function.Predicate)} to get an
 * iteration of files matching given name predicate over given list of urls
 */
public abstract class Vfs {
    private static List<UrlType> defaultUrlTypes = new ArrayList<>(Arrays.asList(DefaultUrlTypes.values()));

    /** an abstract vfs dir */
    public interface Dir extends AutoCloseable {
        String getPath();
        Iterable<File> getFiles();
        default void close() {}
    }

    /** an abstract vfs file */
    public interface File {
        String getName();
        String getRelativePath();
        InputStream openInputStream() throws IOException;
    }

    /** a matcher and factory for a url */
    public interface UrlType {
        boolean matches(URL url) throws Exception;
        Dir createDir(URL url) throws Exception;
    }

    /** the default url types that will be used when issuing {@link org.reflections.vfs.Vfs#fromURL(java.net.URL)} */
    public static List<UrlType> getDefaultUrlTypes() {
        return defaultUrlTypes;
    }

    /** sets the static default url types. can be used to statically plug in urlTypes */
    public static void setDefaultURLTypes(final List<UrlType> urlTypes) {
        defaultUrlTypes = urlTypes;
    }

    /** add a static default url types to the beginning of the default url types list. can be used to statically plug in urlTypes */
    public static void addDefaultURLTypes(UrlType urlType) {
        defaultUrlTypes.add(0, urlType);
    }

    /** tries to create a Dir from the given url, using the defaultUrlTypes */
    public static Dir fromURL(final URL url) {
        return fromURL(url, defaultUrlTypes);
    }

    /** tries to create a Dir from the given url, using the given urlTypes*/
    public static Dir fromURL(final URL url, final List<UrlType> urlTypes) {
        for (UrlType type : urlTypes) {
            try {
                if (type.matches(url)) {
                    Dir dir = type.createDir(url);
                    if (dir != null) return dir;
                }
            } catch (Throwable e) {
                if (Reflections.log != null) {
                    Reflections.log.warn("could not create Dir using " + type + " from url " + url.toExternalForm() + ". skipping.", e);
                }
            }
        }

        throw new ReflectionsException("could not create Vfs.Dir from url, no matching UrlType was found [" + url.toExternalForm() + "]\n" +
                "either use fromURL(final URL url, final List<UrlType> urlTypes) or " +
                "use the static setDefaultURLTypes(final List<UrlType> urlTypes) or addDefaultURLTypes(UrlType urlType) " +
                "with your specialized UrlType.");
    }

    /** tries to create a Dir from the given url, using the given urlTypes*/
    public static Dir fromURL(final URL url, final UrlType... urlTypes) {
        return fromURL(url, Arrays.asList(urlTypes));
    }

    /** return an iterable of all {@link org.reflections.vfs.Vfs.File} in given urls, starting with given packagePrefix and matching nameFilter */
    public static Iterable<File> findFiles(final Collection<URL> inUrls, final String packagePrefix, final Predicate<String> nameFilter) {
        String normalizedPrefix = packagePrefix.replace('\\', '/');
        String prefix = normalizedPrefix.startsWith("/") ? normalizedPrefix.substring(1) : normalizedPrefix;
        Predicate<File> fileNamePredicate = file -> {
            String relativePath = file.getRelativePath();
            String path = (relativePath != null ? relativePath : file.toString()).replace('\\', '/');
            if (path.contains(prefix)) {
                String filename = path.substring(path.indexOf(prefix) + prefix.length());
                if (filename.startsWith("/")) {
                    filename = filename.substring(1);
                }
                return (!filename.isEmpty() && nameFilter.test(filename)) || (filename.isEmpty() && nameFilter.test(file.getName()));
            } else {
                return false;
            }
        };
        return findFiles(inUrls, fileNamePredicate);
    }

    /** return an iterable of all {@link org.reflections.vfs.Vfs.File} in given urls, matching filePredicate */
    public static Iterable<File> findFiles(final Collection<URL> urls, final Predicate<File> filePredicate) {
        return () -> new FileIterator(urls.iterator(), filePredicate);
    }

    /** Iterates matching files across URLs and closes each {@link Dir} once exhausted or closed. */
    private static final class FileIterator implements Iterator<File>, AutoCloseable {
        private final Iterator<URL> urlIterator;
        private final Predicate<File> filePredicate;
        private Dir currentDir;
        private Iterator<File> fileIterator;
        private File nextFile;

        private FileIterator(Iterator<URL> urlIterator, Predicate<File> filePredicate) {
            this.urlIterator = urlIterator;
            this.filePredicate = filePredicate;
        }

        @Override
        public boolean hasNext() {
            if (nextFile != null) {
                return true;
            }
            while (true) {
                while (fileIterator != null && fileIterator.hasNext()) {
                    File candidate = fileIterator.next();
                    if (filePredicate.test(candidate)) {
                        nextFile = candidate;
                        return true;
                    }
                }
                close();
                if (!urlIterator.hasNext()) {
                    return false;
                }
                URL url = urlIterator.next();
                try {
                    currentDir = fromURL(url);
                    fileIterator = currentDir.getFiles().iterator();
                } catch (Throwable e) {
                    close();
                    if (Reflections.log != null) {
                        Reflections.log.error("could not findFiles for url. continuing. [" + url + "]", e);
                    }
                }
            }
        }

        @Override
        public File next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            File result = nextFile;
            nextFile = null;
            return result;
        }

        @Override
        public void close() {
            fileIterator = null;
            if (currentDir != null) {
                try {
                    currentDir.close();
                } catch (Exception ignored) {
                }
                currentDir = null;
            }
        }
    }

    /**try to get {@link java.io.File} from url*/
    public static java.io.File getFile(URL url) {
        java.io.File file;
        String path;

        try {
            path = url.toURI().getSchemeSpecificPart();
            if ((file = new java.io.File(path)).exists()) return file;
        } catch (URISyntaxException ignored) {
        }

        try {
            path = URLDecoder.decode(url.getPath(), "UTF-8");
            if (path.contains(".jar!")) path = path.substring(0, path.lastIndexOf(".jar!") + ".jar".length());
            else if (path.contains("!") && !hasInnerJarFileInPath(url)) path = path.substring(0, path.lastIndexOf("!"));
            if ((file = new java.io.File(path)).exists()) return file;

        } catch (UnsupportedEncodingException ignored) {
        }

        try {
            path = url.toExternalForm();
            if (path.startsWith("jar:")) path = path.substring("jar:".length());
            if (path.startsWith("wsjar:")) path = path.substring("wsjar:".length());
            if (path.startsWith("file:")) path = path.substring("file:".length());
            if (path.contains(".jar!")) path = path.substring(0, path.indexOf(".jar!") + ".jar".length());
            else if (path.contains(".war!")) path = path.substring(0, path.indexOf(".war!") + ".war".length());
            else if (path.contains("!/")) path = path.substring(0, path.indexOf("!/"));
            else if (path.endsWith("!")) path = path.substring(0, path.length() - 1);
            if ((file = new java.io.File(path)).exists()) return file;

            path = path.replace("%20", " ");
            if ((file = new java.io.File(path)).exists()) return file;

        } catch (Exception ignored) {
        }

        return null;
    }

    private static boolean hasJarFileInPath(URL url) {
        return url.toExternalForm().matches(".*\\.(jar|apk|zip|war|ear|aar)(!.*|$)");
    }

    private static boolean hasInnerJarFileInPath(URL url) {
        return url.toExternalForm().matches(".+\\.(jar|apk|zip|war|ear|aar)!/.+");
    }

    /** default url types used by {@link org.reflections.vfs.Vfs#fromURL(java.net.URL)}
     * <p>
     * <p>jarFile - creates a {@link org.reflections.vfs.ZipDir} over jar file
     * <p>jarUrl - creates a {@link org.reflections.vfs.ZipDir} over a jar url, using Java's {@link JarURLConnection}
     * <p>directory - creates a {@link org.reflections.vfs.SystemDir} over a file system directory
     * <p>jboss vfs - for protocols vfs, using jboss vfs (should be provided in classpath)
     * <p>jboss vfsfile - creates a {@link UrlTypeVFS} for protocols vfszip and vfsfile.
     * <p>bundle - for bundle protocol, using eclipse FileLocator (should be provided in classpath)
     * <p>jarInputStream - creates a {@link JarInputDir} over jar files (contains ".jar!/" in it's name), using Java's JarInputStream
     * */
    public enum DefaultUrlTypes implements UrlType {
        jarFile {
            public boolean matches(URL url) {
                return url.getProtocol().equals("file") && hasJarFileInPath(url);
            }

            public Dir createDir(final URL url) throws Exception {
                return new ZipDir(new JarFile(getFile(url)));
            }
        },

        jarUrl {
            public boolean matches(URL url) {
                return ("jar".equals(url.getProtocol()) || "zip".equals(url.getProtocol()) || "wsjar".equals(url.getProtocol())) && !hasInnerJarFileInPath(url);
            }

            public Dir createDir(URL url) throws Exception {
                try {
                    URLConnection urlConnection = url.openConnection();
                    if (urlConnection instanceof JarURLConnection) {
                        urlConnection.setUseCaches(false);
                        return new ZipDir(((JarURLConnection) urlConnection).getJarFile());
                    }
                } catch (Throwable e) { /*fallback*/ }
                java.io.File file = getFile(url);
                if (file != null) {
                    return new ZipDir(new JarFile(file));
                }
                return null;
            }
        },

        directory {
            public boolean matches(URL url) {
                if (url.getProtocol().equals("file") && !hasJarFileInPath(url)) {
                    java.io.File file = getFile(url);
                    return file != null && file.isDirectory();
                } else return false;
            }

            public Dir createDir(final URL url) throws Exception {
                return new SystemDir(getFile(url));
            }
        },

        jboss_vfs {
            public boolean matches(URL url) {
                return url.getProtocol().equals("vfs");
            }

            public Vfs.Dir createDir(URL url) throws Exception {
                return JbossDir.createDir(url);
            }
        },

        jboss_vfsfile {
            public boolean matches(URL url) throws Exception {
                return "vfszip".equals(url.getProtocol()) || "vfsfile".equals(url.getProtocol());
            }

            public Dir createDir(URL url) throws Exception {
                return new UrlTypeVFS().createDir(url);
            }
        },

        bundle {
            public boolean matches(URL url) throws Exception {
                return url.getProtocol().startsWith("bundle");
            }

            public Dir createDir(URL url) throws Exception {
                return fromURL((URL) ClasspathHelper.contextClassLoader().
                        loadClass("org.eclipse.core.runtime.FileLocator").getMethod("resolve", URL.class).invoke(null, url));
            }
        },

        jarInputStream {
            public boolean matches(URL url) throws Exception {
                return hasJarFileInPath(url) || url.toExternalForm().contains(".jar");
            }

            public Dir createDir(final URL url) throws Exception {
                return new JarInputDir(url);
            }
        }
    }
}
