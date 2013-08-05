package org.reflections.util;

import com.google.common.collect.Sets;
import org.reflections.Reflections;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Helper methods for working with the classpath.
 */
public abstract class ClasspathHelper {

    /**
     * Gets the current thread context class loader.
     * {@code Thread.currentThread().getContextClassLoader()}.
     * 
     * @return the context class loader, may be null
     */
    public static ClassLoader contextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    /**
     * Gets the class loader of this library.
     * {@code Reflections.class.getClassLoader()}.
     * 
     * @return the static library class loader, may be null
     */
    public static ClassLoader staticClassLoader() {
        return Reflections.class.getClassLoader();
    }

    /**
     * Returns an array of class Loaders initialized from the specified array.
     * <p>
     * If the input is null or empty, it defaults to both {@link #contextClassLoader()} and {@link #staticClassLoader()}
     * 
     * @return the array of class loaders, not null
     */
    public static ClassLoader[] classLoaders(ClassLoader... classLoaders) {
        if (classLoaders != null && classLoaders.length != 0) {
            return classLoaders;
        } else {
            ClassLoader contextClassLoader = contextClassLoader(), staticClassLoader = staticClassLoader();
            return contextClassLoader != null ?
                    staticClassLoader != null && contextClassLoader != staticClassLoader ?
                            new ClassLoader[]{contextClassLoader, staticClassLoader} :
                            new ClassLoader[]{contextClassLoader} :
                    new ClassLoader[] {};

        }
    }

    /**
     * Returns a set of URLs based on a package name.
     * <p>
     * This searches for the package name as a resource, using {@link ClassLoader#getResources(String)}.
     * For example, {@code forPackage("org.reflections")} effectively returns URLs from the
     * classpath containing packages starting with {@code org.reflections}.
     * <p>
     * If the optional {@link ClassLoader}s are not specified, then both {@link #contextClassLoader()}
     * and {@link #staticClassLoader()} are used for {@link ClassLoader#getResources(String)}.
     * <p>
     * The returned set retains order using {@code LinkedHashSet}, however that order
     * may not be the same as the classpath order.
     * 
     * @return the set of URLs, not null
     */
    public static Set<URL> forPackage(String name, ClassLoader... classLoaders) {
        return forResource(resourceName(name), classLoaders);
    }

    /**
     * Returns a set of URLs based on a resource.
     * <p>
     * This searches for the resource name, using {@link ClassLoader#getResources(String)}.
     * For example, {@code forResource("test.properties")} effectively returns URLs from the
     * classpath containing files of that name.
     * <p>
     * If the optional {@link ClassLoader}s are not specified, then both {@link #contextClassLoader()}
     * and {@link #staticClassLoader()} are used for {@link ClassLoader#getResources(String)}.
     * <p>
     * The returned set retains order using {@code LinkedHashSet}, however that order
     * may not be the same as the classpath order.
     * 
     * @return the set of URLs, not null
     */
    public static Set<URL> forResource(String resourceName, ClassLoader... classLoaders) {
        final Set<URL> result = Sets.newLinkedHashSet();
        final ClassLoader[] loaders = classLoaders(classLoaders);
        for (ClassLoader classLoader : loaders) {
            try {
                final Enumeration<URL> urls = classLoader.getResources(resourceName);
                while (urls.hasMoreElements()) {
                    final URL url = urls.nextElement();
                    int index = url.toExternalForm().lastIndexOf(resourceName);
                    if (index != -1) {
                        result.add(new URL(url.toExternalForm().substring(0, index)));
                    } else {
                        result.add(url); //whatever
                    }
                }
            } catch (IOException e) {
                if (Reflections.log != null) {
                    Reflections.log.error("error getting resources for " + resourceName, e);
                }
            }
        }
        return result;
    }

    /**
     * Returns the URL that contains a {@code Class}.
     * <p>
     * This searches for the class using {@link ClassLoader#getResource(String)}.
     * <p>
     * If the optional {@link ClassLoader}s are not specified, then both {@link #contextClassLoader()}
     * and {@link #staticClassLoader()} are used for {@link ClassLoader#getResources(String)}.
     * 
     * @return the URL containing the class, null if not found
     */
    public static URL forClass(Class<?> aClass, ClassLoader... classLoaders) {
        final ClassLoader[] loaders = classLoaders(classLoaders);
        final String resourceName = aClass.getName().replace(".", "/") + ".class";
        for (ClassLoader classLoader : loaders) {
            try {
                final URL url = classLoader.getResource(resourceName);
                if (url != null) {
                    final String normalizedUrl = url.toExternalForm().substring(0, url.toExternalForm().lastIndexOf(aClass.getPackage().getName().replace(".", "/")));
                    return new URL(normalizedUrl);
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    
    /**
     * Returns a set of URLs based on URLs derived from class loaders.
     * <p>
     * This finds the URLs using {@link URLClassLoader#getURLs()} using both
     * {@link #contextClassLoader()} and {@link #staticClassLoader()}.
     * <p>
     * The returned set retains order using {@code LinkedHashSet}, however that order
     * may not be the same as the classpath order.
     * 
     * @return the set of URLs, not null
     */
    public static Set<URL> forClassLoader() {
        return forClassLoader(classLoaders());
    }

    /**
     * Returns a set of URLs based on URLs derived from class loaders.
     * <p>
     * This finds the URLs using {@link URLClassLoader#getURLs()} using the specified
     * class loader, searching up the parent hierarchy.
     * <p>
     * If the optional {@link ClassLoader}s are not specified, then both {@link #contextClassLoader()}
     * and {@link #staticClassLoader()} are used for {@link ClassLoader#getResources(String)}.
     * <p>
     * The returned set retains order using {@code LinkedHashSet}, however that order
     * may not be the same as the classpath order.
     * 
     * @return the set of URLs, not null
     */
    public static Set<URL> forClassLoader(ClassLoader... classLoaders) {
        final Set<URL> result = Sets.newLinkedHashSet();
        final ClassLoader[] loaders = classLoaders(classLoaders);
        for (ClassLoader classLoader : loaders) {
            while (classLoader != null) {
                if (classLoader instanceof URLClassLoader) {
                    URL[] urls = ((URLClassLoader) classLoader).getURLs();
                    if (urls != null) {
                        result.addAll(Sets.<URL>newHashSet(urls));
                    }
                }
                classLoader = classLoader.getParent();
            }
        }
        return result;
    }

    /**
     * Returns a set of URLs based on the {@code java.class.path} system property.
     * <p>
     * This finds the URLs using the {@code java.class.path} system property.
     * <p>
     * The returned set retains the classpath order using {@code LinkedHashSet}.
     * 
     * @return the set of URLs, not null
     */
    public static Set<URL> forJavaClassPath() {
        Set<URL> urls = Sets.newLinkedHashSet();
        String javaClassPath = System.getProperty("java.class.path");
        if (javaClassPath != null) {
            for (String path : javaClassPath.split(File.pathSeparator)) {
                try {
                    urls.add(new File(path).toURI().toURL());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return urls;
    }

    /**
     * Returns a set of URLs based on the {@code WEB-INF/lib} folder.
     * <p>
     * This finds the URLs using the {@link ServletContext}.
     * <p>
     * The returned set retains order using {@code LinkedHashSet}, however that order
     * may not be the same as the classpath order.
     * 
     * @return the set of URLs, not null
     */
    public static Set<URL> forWebInfLib(final ServletContext servletContext) {
        final Set<URL> urls = Sets.newLinkedHashSet();
        for (Object urlString : servletContext.getResourcePaths("/WEB-INF/lib")) {
            try {
                urls.add(servletContext.getResource((String) urlString));
            } catch (MalformedURLException e) { /*fuck off*/ }
        }
        return urls;
    }

    /**
     * Returns the URL of the {@code WEB-INF/classes} folder.
     * <p>
     * This finds the URLs using the {@link ServletContext}.
     * 
     * @return the set of URLs, not null
     */
    public static URL forWebInfClasses(final ServletContext servletContext) {
        try {
            final String path = servletContext.getRealPath("/WEB-INF/classes");
            if (path != null) {
                final File file = new File(path);
                if (file.exists())
                    return file.toURL();
            } else {
                return servletContext.getResource("/WEB-INF/classes");
            }
        } catch (MalformedURLException e) { /*fuck off*/ }
        return null;
    }

    /**
     * Returns a set of URLs based on URLs derived from class loaders expanded with Manifest information.
     * <p>
     * The {@code MANIFEST.MF} file can contain a {@code Class-Path} entry that defines
     * additional jar files to be included on the classpath. This method finds the jar files
     * using the {@link #contextClassLoader()} and {@link #staticClassLoader()}, before
     * searching for any additional manifest classpaths.
     * 
     * @return the set of URLs, not null
     */
    public static Set<URL> forManifest() {
        return forManifest(forClassLoader());
    }

    /**
     * Returns a set of URLs from a single URL based on the Manifest information.
     * <p>
     * The {@code MANIFEST.MF} file can contain a {@code Class-Path} entry that defines additional
     * jar files to be included on the classpath. This method takes a single URL, tries to
     * resolve it as a jar file, and if so, adds any additional manifest classpaths.
     * The returned set will always contain the input URL.
     * 
     * @return the set of URLs, not null
     */
    public static Set<URL> forManifest(final URL url) {
        final Set<URL> result = Sets.newLinkedHashSet();
        result.add(url);
        try {
            final String part = cleanPath(url);
            File jarFile = new File(part);
            JarFile myJar = new JarFile(part);
            URL validUrl = tryToGetValidUrl(jarFile.getPath(), new File(part).getParent(), part);
            if (validUrl != null) { result.add(validUrl); }
            final Manifest manifest = myJar.getManifest();
            if (manifest != null) {
                final String classPath = manifest.getMainAttributes().getValue(new Attributes.Name("Class-Path"));
                if (classPath != null) {
                    for (String jar : classPath.split(" ")) {
                        validUrl = tryToGetValidUrl(jarFile.getPath(), new File(part).getParent(), jar);
                        if (validUrl != null) { result.add(validUrl); }
                    }
                }
            }
        } catch (IOException e) {
            // don't do anything, we're going on the assumption it is a jar, which could be wrong
        }
        return result;
    }

    /**
     * Returns a set of URLs by expanding the specified URLs with Manifest information.
     * <p>
     * The {@code MANIFEST.MF} file can contain a {@code Class-Path} entry that defines additional
     * jar files to be included on the classpath. This method takes each URL in turn, tries to
     * resolve it as a jar file, and if so, adds any additional manifest classpaths.
     * The returned set will always contain all the input URLs.
     * <p>
     * The returned set retains the input order using {@code LinkedHashSet}.
     * 
     * @return the set of URLs, not null
     */
    public static Set<URL> forManifest(final Iterable<URL> urls) {
        Set<URL> result = Sets.newLinkedHashSet();
        // determine if any of the URLs are JARs, and get any dependencies
        for (URL url : urls) {
            result.addAll(forManifest(url));
        }
        return result;
    }

    //a little bit cryptic...
    static URL tryToGetValidUrl(String workingDir, String path, String filename) {
        try {
            if (new File(filename).exists())
                return new File(filename).toURI().toURL();
            if (new File(path + File.separator + filename).exists())
                return new File(path + File.separator + filename).toURI().toURL();
            if (new File(workingDir + File.separator + filename).exists())
                return new File(workingDir + File.separator + filename).toURI().toURL();
            if (new File(new URL(filename).getFile()).exists())
                return new File(new URL(filename).getFile()).toURI().toURL();
        } catch (MalformedURLException e) {
            // don't do anything, we're going on the assumption it is a jar, which could be wrong
        }
        return null;
    }

    /**
     * Cleans the URL.
     * 
     * @param url  the URL to clean, not null
     * @return the path, not null
     */
    public static String cleanPath(final URL url) {
        String path = url.getPath();
        try {
            path = URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException e) { /**/ }
        if (path.startsWith("jar:")) {
            path = path.substring("jar:".length());
        }
        if (path.startsWith("file:")) {
            path = path.substring("file:".length());
        }
        if (path.endsWith("!/")) {
            path = path.substring(0, path.lastIndexOf("!/")) + "/";
        }
        return path;
    }

    private static String resourceName(String name) {
        if (name != null) {
            String resourceName = name.replace(".", "/");
            resourceName = resourceName.replace("\\", "/");
            if (resourceName.startsWith("/")) {
                resourceName = resourceName.substring(1);
            }
            return resourceName;
        } else {
            return name;
        }
    }
}

