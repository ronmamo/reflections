package com.tvd12.reflections;

import java.net.URL;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.tvd12.reflections.adapters.MetadataAdapter;
import com.tvd12.reflections.scanners.Scanner;
import com.tvd12.reflections.serializers.Serializer;

/**
 * Configuration is used to create a configured instance of {@link Reflections}
 * <p>it is preferred to use {@link com.tvd12.reflections.util.ConfigurationBuilder}
 */
public interface Configuration {
    /** the scanner instances used for scanning different metadata */
    Set<Scanner> getScanners();

    /** the urls to be scanned */
    Set<URL> getUrls();

    /** the metadata adapter used to fetch metadata from classes */
    @SuppressWarnings({"rawtypes"})
    MetadataAdapter getMetadataAdapter();

    /** get the fully qualified name filter used to filter types to be scanned */
    @Nullable
    Predicate<String> getInputsFilter();

    /** executor service used to scan files. if null, scanning is done in a simple for loop */
    ExecutorService getExecutorService();

    /** the default serializer to use when saving Reflection */
    Serializer getSerializer();

    /** get class loaders, might be used for resolving methods/fields */
    @Nullable
    ClassLoader[] getClassLoaders();

    /** if true (default), expand super types after scanning, for super types that were not scanned.
     * <p>see {@link com.tvd12.reflections.Reflections#expandSuperTypes()}*/
    boolean shouldExpandSuperTypes();
}
