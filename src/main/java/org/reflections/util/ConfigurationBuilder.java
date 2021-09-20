package org.reflections.util;

import org.reflections.Configuration;
import org.reflections.Reflections;
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
 * {@link org.reflections.Configuration} builder for instantiating Reflections using {@link Reflections#Reflections(org.reflections.Configuration)}
 * <pre>{@code
 * new Reflections(
 *   new ConfigurationBuilder()
 *     .setUrls(ClasspathHelper.forPackage("org.reflections"))
 *     .addScanners(Scanners.SubTypes, Scanners.TypesAnnotated.filterResultsBy(filter))
 *     .filterInputsBy(new FilterBuilder().includePackage("org.reflections")))
 * }</pre>
 * <p>defaults scanners: {@link Scanners#SubTypes} and {@link Scanners#TypesAnnotated}
 */
public class ConfigurationBuilder implements Configuration {
    private final Set<Scanner> scanners;
    private Set<URL> urls;
    private Predicate<String> inputsFilter;
    private boolean isParallel = true;
    private ClassLoader[] classLoaders;
    private boolean expandSuperTypes = true;

    public ConfigurationBuilder() {
        scanners = new HashSet<>(Arrays.asList(Scanners.TypesAnnotated, Scanners.SubTypes));
        urls = new HashSet<>();
    }

    /** constructs a {@link ConfigurationBuilder}
     * <p>each parameter in {@code params} is referred by its type:
     * <ul>
     *     <li>{@link String} - add urls using {@link ClasspathHelper#forPackage(String, ClassLoader...)} and an input filter
     *     <li>{@link Class} - add urls using {@link ClasspathHelper#forClass(Class, ClassLoader...)} and an input filter
     *     <li>{@link Scanner} - use scanner, overriding default scanners
     *     <li>{@link URL} - add url for scanning
     *     <li>{@link Predicate} - override inputs filter
     *     <li>{@link ClassLoader} - use these classloaders in order to find urls using ClasspathHelper and for resolving types
     *     <li>{@code Object[]} - flatten and use each element as above
     * </ul>
     *
     * an input {@link FilterBuilder} will be set according to given packages.
     * */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static ConfigurationBuilder build(final Object... params) {
        ConfigurationBuilder builder = new ConfigurationBuilder();

        //flatten
        List<Object> parameters = new ArrayList<>();
        if (params != null) {
            for (Object param : params) {
                if (param != null) {
                    if (param.getClass().isArray()) { for (Object p : (Object[]) param) if (p != null) parameters.add(p); }
                    else if (param instanceof Iterable) { for (Object p : (Iterable) param) if (p != null) parameters.add(p); }
                    else parameters.add(param);
                }
            }
        }

        List<ClassLoader> loaders = new ArrayList<>();
        for (Object param : parameters) if (param instanceof ClassLoader) loaders.add((ClassLoader) param);

        ClassLoader[] classLoaders = loaders.isEmpty() ? null : loaders.toArray(new ClassLoader[0]);
        FilterBuilder filter = new FilterBuilder();
        List<Scanner> scanners = new ArrayList<>();

        for (Object param : parameters) {
            if (param instanceof String) {
                builder.addUrls(ClasspathHelper.forPackage((String) param, classLoaders));
                filter.includePackage((String) param);
            }
            else if (param instanceof Class) {
                if (Scanner.class.isAssignableFrom((Class) param)) {
                    try { builder.addScanners(((Scanner) ((Class) param).getDeclaredConstructor().newInstance())); }
                    catch (Exception e) { /*fallback*/ }
                }
                builder.addUrls(ClasspathHelper.forClass((Class) param, classLoaders));
                filter.includePackage(((Class) param).getPackage().getName());
            }
            else if (param instanceof Scanner) { scanners.add((Scanner) param); }
            else if (param instanceof URL) { builder.addUrls((URL) param); }
            // predicate - set override inputFilter
            else if (param instanceof Predicate) { filter = new FilterBuilder().add((Predicate<String>) param); }
            else throw new ReflectionsException("could not use param " + param);
        }

        if (builder.getUrls().isEmpty()) {
            if (classLoaders != null) {
                builder.addUrls(ClasspathHelper.forClassLoader(classLoaders)); //default urls getResources("")
            } else {
                builder.addUrls(ClasspathHelper.forClassLoader()); //default urls getResources("")
            }
            if (builder.urls.isEmpty()) {
                builder.addUrls(ClasspathHelper.forJavaClassPath());
            }
        }

        builder.filterInputsBy(filter);
        if (!scanners.isEmpty()) { builder.setScanners(scanners.toArray(new Scanner[0])); }
        if (!loaders.isEmpty()) { builder.addClassLoaders(loaders); }

        return builder;
    }

    public ConfigurationBuilder forPackages(String... packages) {
        for (String pkg : packages) {
            addUrls(ClasspathHelper.forPackage(pkg));
        }
        return this;
    }

    @Override
    /* @inherited */
    public Set<Scanner> getScanners() {
		return scanners;
	}

    /** set the scanners instances for scanning different metadata */
    public ConfigurationBuilder setScanners(final Scanner... scanners) {
        this.scanners.clear();
        return addScanners(scanners);
    }

    /** set the scanners instances for scanning different metadata */
    public ConfigurationBuilder addScanners(final Scanner... scanners) {
        this.scanners.addAll(Arrays.asList(scanners));
        return this;
    }

    @Override
    /* @inherited */
    public Set<URL> getUrls() {
        return urls;
    }

    /** set the urls to be scanned
     * <p>use {@link org.reflections.util.ClasspathHelper} convenient methods to get the relevant urls
     * */
    public ConfigurationBuilder setUrls(final Collection<URL> urls) {
		this.urls = new HashSet<>(urls);
        return this;
	}

    /** set the urls to be scanned
     * <p>use {@link org.reflections.util.ClasspathHelper} convenient methods to get the relevant urls
     * */
    public ConfigurationBuilder setUrls(final URL... urls) {
        this.urls = new HashSet<>(Arrays.asList(urls));
        return this;
	}

    /** add urls to be scanned
     * <p>use {@link org.reflections.util.ClasspathHelper} convenient methods to get the relevant urls
     * */
    public ConfigurationBuilder addUrls(final Collection<URL> urls) {
        this.urls.addAll(urls);
        return this;
    }

    /** add urls to be scanned
     * <p>use {@link org.reflections.util.ClasspathHelper} convenient methods to get the relevant urls
     * */
    public ConfigurationBuilder addUrls(final URL... urls) {
        this.urls.addAll(new HashSet<>(Arrays.asList(urls)));
        return this;
    }

    @Override
    /* @inherited */
    public Predicate<String> getInputsFilter() {
        return inputsFilter;
    }

    /** sets the input filter for all resources to be scanned.
     * <p> supply a {@link Predicate} or use the {@link FilterBuilder}*/
    public void setInputsFilter(Predicate<String> inputsFilter) {
        this.inputsFilter = inputsFilter;
    }

    /** sets the input filter for all resources to be scanned.
     * <p> supply a {@link Predicate} or use the {@link FilterBuilder}*/
    public ConfigurationBuilder filterInputsBy(Predicate<String> inputsFilter) {
        this.inputsFilter = inputsFilter;
        return this;
    }

    @Override
    /* @inherited */
    public boolean isParallel() {
        return isParallel;
    }

    /** */
    public void setParallel(boolean parallel) {
        isParallel = parallel;
    }

    @Override
    /* @inherited */
    public ClassLoader[] getClassLoaders() {
        return classLoaders;
    }

    @Override
    /* @inherited */
    public boolean shouldExpandSuperTypes() {
        return expandSuperTypes;
    }

    /**
     * if set to true, Reflections will expand super types after scanning.
     * <p>see {@link org.reflections.Reflections#expandSuperTypes(Map)}
     */
    public ConfigurationBuilder setExpandSuperTypes(boolean expandSuperTypes) {
        this.expandSuperTypes = expandSuperTypes;
        return this;
    }

    /** set class loader, might be used for resolving methods/fields */
    public ConfigurationBuilder setClassLoaders(ClassLoader[] classLoaders) {
        this.classLoaders = classLoaders;
        return this;
    }

    /** add class loader, might be used for resolving methods/fields */
    public ConfigurationBuilder addClassLoader(ClassLoader classLoader) {
        return addClassLoaders(classLoader);
    }

    /** add class loader, might be used for resolving methods/fields */
    public ConfigurationBuilder addClassLoaders(ClassLoader... classLoaders) {
        this.classLoaders = this.classLoaders == null
                            ? classLoaders
                            : Stream.concat(Arrays.stream(this.classLoaders), Arrays.stream(classLoaders)).toArray(ClassLoader[]::new);
        return this;
    }

    /** add class loader, might be used for resolving methods/fields */
    public ConfigurationBuilder addClassLoaders(Collection<ClassLoader> classLoaders) {
        return addClassLoaders(classLoaders.toArray(new ClassLoader[0]));
    }
}
