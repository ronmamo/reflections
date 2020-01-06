package org.reflections.scanners;

import org.reflections.Configuration;
import org.reflections.Store;
import org.reflections.vfs.Vfs;

import java.util.function.Predicate;

/**
 *
 */
public interface Scanner {

    void setConfiguration(Configuration configuration);

    Scanner filterResultsBy(Predicate<String> filter);

    boolean acceptsInput(String file);

    Object scan(Vfs.File file, Object classObject, Store store);

    boolean acceptResult(String fqn);
}
