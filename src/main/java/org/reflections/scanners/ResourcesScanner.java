package org.reflections.scanners;

import org.reflections.vfs.Vfs;

/** collects all resources that are not classes in a collection
 * <p>key: value - {web.xml: WEB-INF/web.xml} */
public class ResourcesScanner extends AbstractScanner {
    public boolean acceptsInput(String file) {
        return !file.endsWith(".class"); //not a class
    }

    @Override public Object scan(Vfs.File file, Object classObject) {
        getStore().put(file.getName(), file.getRelativePath());
        return classObject;
    }

    public void scan(Object cls) {
        throw new UnsupportedOperationException(); //shouldn't get here
    }
}
