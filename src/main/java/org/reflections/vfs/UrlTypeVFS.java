package org.reflections.vfs;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.reflections.Reflections;
import org.reflections.ReflectionsException;
import org.reflections.vfs.Vfs.Dir;
import org.reflections.vfs.Vfs.UrlType;

import com.google.common.base.Predicate;

/**
 * UrlType to be used by Reflections library.
 * This class handles the vfszip and vfsfile protocol of JBOSS files.
 * <p>
 * <p>to use it, register it in Vfs via {@link org.reflections.vfs.Vfs#addDefaultURLTypes(org.reflections.vfs.Vfs.UrlType)} or {@link org.reflections.vfs.Vfs#setDefaultURLTypes(java.util.List)}.
 * @author Sergio Pola
 *
 */
public class UrlTypeVFS implements UrlType {
    public final static String[] REPLACE_EXTENSION = new String[]{".ear/", ".jar/", ".war/", ".sar/", ".har/", ".par/"};

    final String VFSZIP = "vfszip";
    final String VFSFILE = "vfsfile";

    public boolean matches(URL url) {
        return VFSZIP.equals(url.getProtocol()) || VFSFILE.equals(url.getProtocol());
    }

    public Dir createDir(final URL url) {
        try {
            URL adaptedUrl = adaptURL(url);
            return new ZipDir(new JarFile(adaptedUrl.getFile()));
        } catch (Exception e) {
            try {
                return new ZipDir(new JarFile(url.getFile()));
            } catch (IOException e1) {
                if (Reflections.log != null) {
                    Reflections.log.warn("Could not get URL", e);
                    Reflections.log.warn("Could not get URL", e1);
                }
            }
        }
        return null;
    }

    public URL adaptURL(URL url) throws MalformedURLException {
        if (VFSZIP.equals(url.getProtocol())) {
            return replaceZipSeparators(url.getPath(), realFile);
        } else if (VFSFILE.equals(url.getProtocol())) {
            return new URL(url.toString().replace(VFSFILE, "file"));
        } else {
            return url;
        }
    }

    URL replaceZipSeparators(String path, Predicate<File> acceptFile)
            throws MalformedURLException {
        int pos = 0;
        while (pos != -1) {
            pos = findFirstMatchOfDeployableExtention(path, pos);

            if (pos > 0) {
                File file = new File(path.substring(0, pos - 1));
                if (acceptFile.apply(file)) { return replaceZipSeparatorStartingFrom(path, pos); }
            }
        }

        throw new ReflectionsException("Unable to identify the real zip file in path '" + path + "'.");
    }

    int findFirstMatchOfDeployableExtention(String path, int pos) {
        Pattern p = Pattern.compile("\\.[ejprw]ar/");
        Matcher m = p.matcher(path);
        if (m.find(pos)) {
            return m.end();
        } else {
            return -1;
        }
    }

    Predicate<File> realFile = new Predicate<File>() {
        public boolean apply(File file) {
            return file.exists() && file.isFile();
        }
    };

    URL replaceZipSeparatorStartingFrom(String path, int pos)
            throws MalformedURLException {
        String zipFile = path.substring(0, pos - 1);
        String zipPath = path.substring(pos);

        int numSubs = 1;
        for (String ext : REPLACE_EXTENSION) {
            while (zipPath.contains(ext)) {
                zipPath = zipPath.replace(ext, ext.substring(0, 4) + "!");
                numSubs++;
            }
        }

        String prefix = "";
        for (int i = 0; i < numSubs; i++) {
            prefix += "zip:";
        }

        if (zipPath.trim().length() == 0) {
            return new URL(prefix + "/" + zipFile);
        } else {
            return new URL(prefix + "/" + zipFile + "!" + zipPath);
        }
    }
}
