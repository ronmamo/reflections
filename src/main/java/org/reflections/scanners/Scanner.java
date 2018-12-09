package org.reflections.scanners;

import org.reflections.Configuration;
import org.reflections.util.Multimap;
import org.reflections.vfs.Vfs;

import java.util.function.Predicate;

import javax.annotation.Nullable;

/**
 *
 */
public interface Scanner {

    void setConfiguration(Configuration configuration);

    Multimap<String, String> getStore();

    void setStore(Multimap<String, String> store);

    Scanner filterResultsBy(Predicate<String> filter);

    boolean acceptsInput(String file);

    Object scan(Vfs.File file, @Nullable Object classObject);

    boolean acceptResult(String fqn);
}
