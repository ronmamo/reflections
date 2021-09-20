package org.reflections;

import org.reflections.scanners.Scanner;

import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Configuration is used to create a configured instance of {@link Reflections}
 * <p>it is preferred to use {@link org.reflections.util.ConfigurationBuilder}
 */
public interface Configuration {
    /** the scanner instances used for indexing metadata */
    Set<Scanner> getScanners();

    /** the urls to be scanned */
    Set<URL> getUrls();

    /** the fully qualified name filter used to filter types to be scanned */
    Predicate<String> getInputsFilter();

    /** scan urls in parallel, defaults to true */
    boolean isParallel();

    /** get class loaders, might be used for resolving methods/fields */
    ClassLoader[] getClassLoaders();

    /** if true (default), expand super types after scanning, for super types that were not scanned.
     * <p>see {@link org.reflections.Reflections#expandSuperTypes(Map)}*/
    boolean shouldExpandSuperTypes();
}
