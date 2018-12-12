package com.tvd12.reflections.scanners;

import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.tvd12.reflections.Configuration;
import com.tvd12.reflections.util.Multimap;
import com.tvd12.reflections.vfs.Vfs;

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
