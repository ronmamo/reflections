package org.reflections.util;

import org.reflections.Configuration;
import org.reflections.ReflectionsException;
import org.reflections.scanners.Scanner;
import org.reflections.scanners.Scanners;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * {@link Configuration} builder for instantiating Reflections
 * <pre>{@code
 * // add urls for package prefix, use default scanners
 * new Reflections(
 *   new ConfigurationBuilder()
 *     .forPackage("org.reflections"))
 *
 * new Reflections(
 *   new ConfigurationBuilder()
 *     .addUrls(ClasspathHelper.forPackage("org.reflections"))   // add urls for package prefix
 *     .addScanners(Scanners.values())                           // use all standard scanners
 *     .filterInputsBy(new FilterBuilder().includePackage(...))) // optionally filter inputs
 * }</pre>
 * <p>defaults scanners: {@link Scanners#SubTypes} and {@link Scanners#TypesAnnotated}
 * <p><i>(breaking changes) Inputs filter will NOT be set automatically, consider adding in case too many classes are scanned.</i>
 */
public class ConfigurationBuilder implements Configuration {
    public static final Set<Scanner> DEFAULT_SCANNERS = new HashSet<>(Arrays.asList(Scanners.TypesAnnotated, Scanners.SubTypes));
    public static final Predicate<String> DEFAULT_INPUTS_FILTER = t -> true;

    private Set<Scanner> scanners;
    private Set<URL> urls;
    private Predicate<String> inputsFilter;
    private boolean isParallel = true;
    private ClassLoader[] classLoaders;
    private boolean expandSuperTypes = true;

    public ConfigurationBuilder() {
        urls = new HashSet<>();
    }

    /** constructs a {@link ConfigurationBuilder}.
     * <p>each parameter in {@code params} is referred by its type:
     * <ul>
     *     <li>{@link String} - add urls using {@link ClasspathHelper#forPackage(String, ClassLoader...)} and an input filter
     *     <li>{@link Class} - add urls using {@link ClasspathHelper#forClass(Class, ClassLoader...)} and an input filter
     *     <li>{@link Scanner} - use scanner, overriding default scanners
     *     <li>{@link URL} - add url for scanning
     *     <li>{@link Predicate} - set/override inputs filter
     *     <li>{@link ClassLoader} - use these classloaders in order to find urls using ClasspathHelper and for resolving types
     *     <li>{@code Object[]} - flatten and use each element as above
     * </ul>
     * input filter will be set according to given packages
     * <p></p><i>prefer using the explicit accessor methods instead:</i>
     * <pre>{@code new ConfigurationBuilder().forPackage(...).setScanners(...)}</pre>
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static ConfigurationBuilder build(Object... params) {
        final ConfigurationBuilder builder = new ConfigurationBuilder();

        // flatten
        List<Object> parameters = new ArrayList<>();
        for (Object param : params) {
            if (param.getClass().isArray()) { for (Object p : (Object[]) param) parameters.add(p); }
            else if (param instanceof Iterable) { for (Object p : (Iterable) param) parameters.add(p); }
            else parameters.add(param);
        }

        ClassLoader[] loaders = Stream.of(params).filter(p -> p instanceof ClassLoader).distinct().toArray(ClassLoader[]::new);
        if (loaders.length != 0) { builder.addClassLoaders(loaders); }

        FilterBuilder inputsFilter = new FilterBuilder();
        builder.filterInputsBy(inputsFilter);

        for (Object param : parameters) {
            if (param instanceof String && !((String) param).isEmpty()) {
                builder.forPackage((String) param, loaders);
                inputsFilter.includePackage((String) param);
            } else if (param instanceof Class && !Scanner.class.isAssignableFrom((Class) param)) {
                builder.addUrls(ClasspathHelper.forClass((Class) param, loaders));
                inputsFilter.includePackage(((Class) param).getPackage().getName());
            } else if (param instanceof URL) {
                builder.addUrls((URL) param);
            } else if (param instanceof Scanner) {
                builder.addScanners((Scanner) param);
            } else if (param instanceof Class && Scanner.class.isAssignableFrom((Class) param)) {
                try { builder.addScanners(((Class<Scanner>) param).getDeclaredConstructor().newInstance()); }
                catch (Exception e) { throw new RuntimeException(e); }
            } else if (param instanceof Predicate) {
                builder.filterInputsBy((Predicate<String>) param);
            } else throw new ReflectionsException("could not use param '" + param + "'");
        }

        if (builder.getUrls().isEmpty()) {
            // scan all classpath if no urls provided todo avoid
            builder.addUrls(ClasspathHelper.forClassLoader(loaders));
        }

        return builder;
    }

    /** {@link #addUrls(URL...)} by applying {@link ClasspathHelper#forPackage(String, ClassLoader...)} for the given {@code pkg}*/
    public ConfigurationBuilder forPackage(String pkg, ClassLoader... classLoaders) {
        return addUrls(ClasspathHelper.forPackage(pkg, classLoaders));
    }

    /** {@link #addUrls(URL...)} by applying {@link ClasspathHelper#forPackage(String, ClassLoader...)} for the given {@code packages}*/
    public ConfigurationBuilder forPackages(String... packages) {
        for (String pkg : packages) forPackage(pkg);
        return this;
    }

    @Override
    /* @inherited */
    public Set<Scanner> getScanners() {
        return scanners != null ? scanners : DEFAULT_SCANNERS;
	}

    /** set the scanners instances for scanning different metadata */
    public ConfigurationBuilder setScanners(Scanner... scanners) {
        this.scanners = new HashSet<>(Arrays.asList(scanners));
        return this;
    }

    /** set the scanners instances for scanning different metadata */
    public ConfigurationBuilder addScanners(Scanner... scanners) {
        if (this.scanners == null) setScanners(scanners); else this.scanners.addAll(Arrays.asList(scanners));
        return this;
    }

    @Override
    /* @inherited */
    public Set<URL> getUrls() {
        return urls;
    }

    /** set the urls to be scanned
     * <p>use {@link ClasspathHelper} convenient methods to get the relevant urls
     * <p>see also {@link #forPackages(String...)} */
    public ConfigurationBuilder setUrls(Collection<URL> urls) {
		this.urls = new HashSet<>(urls);
        return this;
	}

    /** set the urls to be scanned
     * <p>use {@link ClasspathHelper} convenient methods to get the relevant urls
     * <p>see also {@link #forPackages(String...)} */
    public ConfigurationBuilder setUrls(URL... urls) {
        return setUrls(Arrays.asList(urls));
	}

    /** add urls to be scanned
     * <p>use {@link ClasspathHelper} convenient methods to get the relevant urls
     * <p>see also {@link #forPackages(String...)} */
    public ConfigurationBuilder addUrls(Collection<URL> urls) {
        this.urls.addAll(urls);
        return this;
    }

    /** add urls to be scanned
     * <p>use {@link ClasspathHelper} convenient methods to get the relevant urls
     * <p>see also {@link #forPackages(String...)} */
    public ConfigurationBuilder addUrls(URL... urls) {
        return addUrls(Arrays.asList(urls));
    }

    @Override
    /* @inherited */
    public Predicate<String> getInputsFilter() {
        return inputsFilter != null ? inputsFilter : DEFAULT_INPUTS_FILTER;
    }

    /** sets the input filter for all resources to be scanned.
     * <p>prefer using {@link FilterBuilder} */
    public ConfigurationBuilder setInputsFilter(Predicate<String> inputsFilter) {
        this.inputsFilter = inputsFilter;
        return this;
    }

    /** sets the input filter for all resources to be scanned.
     * <p>prefer using {@link FilterBuilder} */
    public ConfigurationBuilder filterInputsBy(Predicate<String> inputsFilter) {
        return setInputsFilter(inputsFilter);
    }

    @Override
    /* @inherited */
    public boolean isParallel() {
        return isParallel;
    }

    /** if true, scan urls in parallel. */
    public void setParallel(boolean parallel) {
        isParallel = parallel;
    }

    @Override
    /* @inherited */
    public ClassLoader[] getClassLoaders() {
        return classLoaders;
    }


    /** set optional class loaders used for resolving types. */
    public ConfigurationBuilder setClassLoaders(ClassLoader[] classLoaders) {
        this.classLoaders = classLoaders;
        return this;
    }

    /** add optional class loaders used for resolving types. */
    public ConfigurationBuilder addClassLoaders(ClassLoader... classLoaders) {
        this.classLoaders = this.classLoaders == null ? classLoaders :
            Stream.concat(Arrays.stream(this.classLoaders), Arrays.stream(classLoaders)).distinct().toArray(ClassLoader[]::new);
        return this;
    }

    @Override
    /* @inherited */
    public boolean shouldExpandSuperTypes() {
        return expandSuperTypes;
    }

    /** if set to true, Reflections will expand super types after scanning.
     * <p>see {@link org.reflections.Reflections#expandSuperTypes(Map, Map)} */
    public ConfigurationBuilder setExpandSuperTypes(boolean expandSuperTypes) {
        this.expandSuperTypes = expandSuperTypes;
        return this;
    }
}
