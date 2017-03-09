package org.reflections;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.*;
import org.reflections.scanners.*;
import org.reflections.scanners.Scanner;
import org.reflections.serializers.Serializer;
import org.reflections.serializers.XmlSerializer;
import org.reflections.util.*;
import org.reflections.vfs.Vfs;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.regex.Pattern;

import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterables.concat;
import static java.lang.String.format;
import static org.reflections.ReflectionUtils.*;
import static org.reflections.util.Utils.*;

/**
 * Reflections one-stop-shop object
 * <p>Reflections scans your classpath, indexes the metadata, allows you to query it on runtime and may save and collect that information for many modules within your project.
 * <p>Using Reflections you can query your metadata such as:
 * <ul>
 *     <li>get all subtypes of some type
 *     <li>get all types/constructors/methods/fields annotated with some annotation, optionally with annotation parameters matching
 *     <li>get all resources matching matching a regular expression
 *     <li>get all methods with specific signature including parameters, parameter annotations and return type
 *     <li>get all methods parameter names
 *     <li>get all fields/methods/constructors usages in code
 * </ul>
 * <p>A typical use of Reflections would be:
 * <pre>
 *      Reflections reflections = new Reflections("my.project.prefix");
 *
 *      Set&#60Class&#60? extends SomeType>> subTypes = reflections.getSubTypesOf(SomeType.class);
 *
 *      Set&#60Class&#60?>> annotated = reflections.getTypesAnnotatedWith(SomeAnnotation.class);
 * </pre>
 * <p>Basically, to use Reflections first instantiate it with one of the constructors, then depending on the scanners, use the convenient query methods:
 * <pre>
 *      Reflections reflections = new Reflections("my.package.prefix");
 *      //or
 *      Reflections reflections = new Reflections(ClasspathHelper.forPackage("my.package.prefix"), 
 *            new SubTypesScanner(), new TypesAnnotationScanner(), new FilterBuilder().include(...), ...);
 *
 *       //or using the ConfigurationBuilder
 *       new Reflections(new ConfigurationBuilder()
 *            .filterInputsBy(new FilterBuilder().include(FilterBuilder.prefix("my.project.prefix")))
 *            .setUrls(ClasspathHelper.forPackage("my.project.prefix"))
 *            .setScanners(new SubTypesScanner(), new TypeAnnotationsScanner().filterResultsBy(optionalFilter), ...));
 * </pre>
 * And then query, for example:
 * <pre>
 *       Set&#60Class&#60? extends Module>> modules = reflections.getSubTypesOf(com.google.inject.Module.class);
 *       Set&#60Class&#60?>> singletons =             reflections.getTypesAnnotatedWith(javax.inject.Singleton.class);
 *
 *       Set&#60String> properties =       reflections.getResources(Pattern.compile(".*\\.properties"));
 *       Set&#60Constructor> injectables = reflections.getConstructorsAnnotatedWith(javax.inject.Inject.class);
 *       Set&#60Method> deprecateds =      reflections.getMethodsAnnotatedWith(javax.ws.rs.Path.class);
 *       Set&#60Field> ids =               reflections.getFieldsAnnotatedWith(javax.persistence.Id.class);
 *
 *       Set&#60Method> someMethods =      reflections.getMethodsMatchParams(long.class, int.class);
 *       Set&#60Method> voidMethods =      reflections.getMethodsReturn(void.class);
 *       Set&#60Method> pathParamMethods = reflections.getMethodsWithAnyParamAnnotated(PathParam.class);
 *       Set&#60Method> floatToString =    reflections.getConverters(Float.class, String.class);
 *       List&#60String> parameterNames =  reflections.getMethodsParamNames(Method.class);
 *
 *       Set&#60Member> fieldUsage =       reflections.getFieldUsage(Field.class);
 *       Set&#60Member> methodUsage =      reflections.getMethodUsage(Method.class);
 *       Set&#60Member> constructorUsage = reflections.getConstructorUsage(Constructor.class);
 * </pre>
 * <p>You can use other scanners defined in Reflections as well, such as: SubTypesScanner, TypeAnnotationsScanner (both default), 
 * ResourcesScanner, MethodAnnotationsScanner, ConstructorAnnotationsScanner, FieldAnnotationsScanner,
 * MethodParameterScanner, MethodParameterNamesScanner, MemberUsageScanner or any custom scanner.
 * <p>Use {@link #getStore()} to access and query the store directly
 * <p>In order to save the store metadata, use {@link #save(String)} or {@link #save(String, org.reflections.serializers.Serializer)}
 * for example with {@link org.reflections.serializers.XmlSerializer} or {@link org.reflections.serializers.JavaCodeSerializer}
 * <p>In order to collect pre saved metadata and avoid re-scanning, use {@link #collect(String, com.google.common.base.Predicate, org.reflections.serializers.Serializer...)}}
 * <p><i>Make sure to scan all the transitively relevant packages.
 * <br>for instance, given your class C extends B extends A, and both B and A are located in another package than C,
 * when only the package of C is scanned - then querying for sub types of A returns nothing (transitive), but querying for sub types of B returns C (direct).
 * In that case make sure to scan all relevant packages a priori.</i>
 * <p><p><p>For Javadoc, source code, and more information about Reflections Library, see http://github.com/ronmamo/reflections/
 */
public class Reflections {
    @Nullable public static Logger log = findLogger(Reflections.class);

    protected final transient Configuration configuration;
    protected Store store;

    /**
     * constructs a Reflections instance and scan according to given {@link org.reflections.Configuration}
     * <p>it is preferred to use {@link org.reflections.util.ConfigurationBuilder}
     */
    public Reflections(final Configuration configuration) {
        this.configuration = configuration;
        store = new Store(configuration);

        if (configuration.getScanners() != null && !configuration.getScanners().isEmpty()) {
            //inject to scanners
            for (Scanner scanner : configuration.getScanners()) {
                scanner.setConfiguration(configuration);
                scanner.setStore(store.getOrCreate(scanner.getClass().getSimpleName()));
            }

            scan();

            if (configuration.shouldExpandSuperTypes()) {
                expandSuperTypes();
            }
        }
    }

    /**
     * a convenient constructor for scanning within a package prefix.
     * <p>this actually create a {@link org.reflections.Configuration} with:
     * <br> - urls that contain resources with name {@code prefix}
     * <br> - filterInputsBy where name starts with the given {@code prefix}
     * <br> - scanners set to the given {@code scanners}, otherwise defaults to {@link org.reflections.scanners.TypeAnnotationsScanner} and {@link org.reflections.scanners.SubTypesScanner}.
     * @param prefix package prefix, to be used with {@link org.reflections.util.ClasspathHelper#forPackage(String, ClassLoader...)} )}
     * @param scanners optionally supply scanners, otherwise defaults to {@link org.reflections.scanners.TypeAnnotationsScanner} and {@link org.reflections.scanners.SubTypesScanner}
     */
    public Reflections(final String prefix, @Nullable final Scanner... scanners) {
        this((Object) prefix, scanners);
    }

    /**
     * a convenient constructor for Reflections, where given {@code Object...} parameter types can be either:
     * <ul>
     *     <li>{@link String} - would add urls using {@link org.reflections.util.ClasspathHelper#forPackage(String, ClassLoader...)} ()}</li>
     *     <li>{@link Class} - would add urls using {@link org.reflections.util.ClasspathHelper#forClass(Class, ClassLoader...)} </li>
     *     <li>{@link ClassLoader} - would use this classloaders in order to find urls in {@link org.reflections.util.ClasspathHelper#forPackage(String, ClassLoader...)} and {@link org.reflections.util.ClasspathHelper#forClass(Class, ClassLoader...)}</li>
     *     <li>{@link org.reflections.scanners.Scanner} - would use given scanner, overriding the default scanners</li>
     *     <li>{@link java.net.URL} - would add the given url for scanning</li>
     *     <li>{@link Object[]} - would use each element as above</li>
     * </ul>
     *
     * use any parameter type in any order. this constructor uses instanceof on each param and instantiate a {@link org.reflections.util.ConfigurationBuilder} appropriately.
     * if you prefer the usual statically typed constructor, don't use this, although it can be very useful.
     *
     * <br><br>for example:
     * <pre>
     *     new Reflections("my.package", classLoader);
     *     //or
     *     new Reflections("my.package", someScanner, anotherScanner, classLoader);
     *     //or
     *     new Reflections(myUrl, myOtherUrl);
     * </pre>
     */
    public Reflections(final Object... params) {
        this(ConfigurationBuilder.build(params));
    }

    protected Reflections() {
        configuration = new ConfigurationBuilder();
        store = new Store(configuration);
    }

    //
    protected void scan() {
        if (configuration.getUrls() == null || configuration.getUrls().isEmpty()) {
            if (log != null) log.warn("given scan urls are empty. set urls in the configuration");
            return;
        }

        if (log != null && log.isDebugEnabled()) {
            log.debug("going to scan these urls:\n" + Joiner.on("\n").join(configuration.getUrls()));
        }

        long time = System.currentTimeMillis();
        int scannedUrls = 0;
        ExecutorService executorService = configuration.getExecutorService();
        List<Future<?>> futures = Lists.newArrayList();

        for (final URL url : configuration.getUrls()) {
            try {
                if (executorService != null) {
                    futures.add(executorService.submit(new Runnable() {
                        public void run() {
                            if (log != null && log.isDebugEnabled()) log.debug("[" + Thread.currentThread().toString() + "] scanning " + url);
                            scan(url);
                        }
                    }));
                } else {
                    scan(url);
                }
                scannedUrls++;
            } catch (ReflectionsException e) {
                if (log != null && log.isWarnEnabled()) log.warn("could not create Vfs.Dir from url. ignoring the exception and continuing", e);
            }
        }

        //todo use CompletionService
        if (executorService != null) {
            for (Future future : futures) {
                try { future.get(); } catch (Exception e) { throw new RuntimeException(e); }
            }
        }

        time = System.currentTimeMillis() - time;

        //gracefully shutdown the parallel scanner executor service.
        if (executorService != null) {
            executorService.shutdown();
        }

        if (log != null) {
            int keys = 0;
            int values = 0;
            for (String index : store.keySet()) {
                keys += store.get(index).keySet().size();
                values += store.get(index).size();
            }

            log.info(format("Reflections took %d ms to scan %d urls, producing %d keys and %d values %s",
                    time, scannedUrls, keys, values,
                    executorService != null && executorService instanceof ThreadPoolExecutor ?
                            format("[using %d cores]", ((ThreadPoolExecutor) executorService).getMaximumPoolSize()) : ""));
        }
    }

    protected void scan(URL url) {
        Vfs.Dir dir = Vfs.fromURL(url);

        try {
            for (final Vfs.File file : dir.getFiles()) {
                // scan if inputs filter accepts file relative path or fqn
                Predicate<String> inputsFilter = configuration.getInputsFilter();
                String path = file.getRelativePath();
                String fqn = path.replace('/', '.');
                if (inputsFilter == null || inputsFilter.apply(path) || inputsFilter.apply(fqn)) {
                    Object classObject = null;
                    for (Scanner scanner : configuration.getScanners()) {
                        try {
                            if (scanner.acceptsInput(path) || scanner.acceptResult(fqn)) {
                                classObject = scanner.scan(file, classObject);
                            }
                        } catch (Exception e) {
                            if (log != null && log.isDebugEnabled())
                                log.debug("could not scan file " + file.getRelativePath() + " in url " + url.toExternalForm() + " with scanner " + scanner.getClass().getSimpleName(), e);
                        }
                    }
                }
            }
        } finally {
            dir.close();
        }
    }

    /** collect saved Reflection xml resources and merge it into a Reflections instance
     * <p>by default, resources are collected from all urls that contains the package META-INF/reflections
     * and includes files matching the pattern .*-reflections.xml
     * */
    public static Reflections collect() {
        return collect("META-INF/reflections/", new FilterBuilder().include(".*-reflections.xml"));
    }

    /**
     * collect saved Reflections resources from all urls that contains the given packagePrefix and matches the given resourceNameFilter
     * and de-serializes them using the default serializer {@link org.reflections.serializers.XmlSerializer} or using the optionally supplied optionalSerializer
     * <p>
     * it is preferred to use a designated resource prefix (for example META-INF/reflections but not just META-INF),
     * so that relevant urls could be found much faster
     * @param optionalSerializer - optionally supply one serializer instance. if not specified or null, {@link org.reflections.serializers.XmlSerializer} will be used
     */
    public static Reflections collect(final String packagePrefix, final Predicate<String> resourceNameFilter, @Nullable Serializer... optionalSerializer) {
        Serializer serializer = optionalSerializer != null && optionalSerializer.length == 1 ? optionalSerializer[0] : new XmlSerializer();

        Collection<URL> urls = ClasspathHelper.forPackage(packagePrefix);
        if (urls.isEmpty()) return null;
        long start = System.currentTimeMillis();
        final Reflections reflections = new Reflections();
        Iterable<Vfs.File> files = Vfs.findFiles(urls, packagePrefix, resourceNameFilter);
        for (final Vfs.File file : files) {
            InputStream inputStream = null;
            try {
                inputStream = file.openInputStream();
                reflections.merge(serializer.read(inputStream));
            } catch (IOException e) {
                throw new ReflectionsException("could not merge " + file, e);
            } finally {
                close(inputStream);
            }
        }

        if (log != null) {
            Store store = reflections.getStore();
            int keys = 0;
            int values = 0;
            for (String index : store.keySet()) {
                keys += store.get(index).keySet().size();
                values += store.get(index).size();
            }

            log.info(format("Reflections took %d ms to collect %d url%s, producing %d keys and %d values [%s]",
                    System.currentTimeMillis() - start, urls.size(), urls.size() > 1 ? "s" : "", keys, values, Joiner.on(", ").join(urls)));
        }
        return reflections;
    }

    /** merges saved Reflections resources from the given input stream, using the serializer configured in this instance's Configuration
     * <br> useful if you know the serialized resource location and prefer not to look it up the classpath
     * */
    public Reflections collect(final InputStream inputStream) {
        try {
            merge(configuration.getSerializer().read(inputStream));
            if (log != null) log.info("Reflections collected metadata from input stream using serializer " + configuration.getSerializer().getClass().getName());
        } catch (Exception ex) {
            throw new ReflectionsException("could not merge input stream", ex);
        }

        return this;
    }

    /** merges saved Reflections resources from the given file, using the serializer configured in this instance's Configuration
     * <p> useful if you know the serialized resource location and prefer not to look it up the classpath
     * */
    public Reflections collect(final File file) {
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            return collect(inputStream);
        } catch (FileNotFoundException e) {
            throw new ReflectionsException("could not obtain input stream from file " + file, e);
        } finally {
            Utils.close(inputStream);
        }
    }

    /**
     * merges a Reflections instance metadata into this instance
     */
    public Reflections merge(final Reflections reflections) {
        if (reflections.store != null) {
            for (String indexName : reflections.store.keySet()) {
                Multimap<String, String> index = reflections.store.get(indexName);
                for (String key : index.keySet()) {
                    for (String string : index.get(key)) {
                        store.getOrCreate(indexName).put(key, string);
                    }
                }
            }
        }
        return this;
    }

    /**
     * expand super types after scanning, for super types that were not scanned.
     * this is helpful in finding the transitive closure without scanning all 3rd party dependencies.
     * it uses {@link ReflectionUtils#getSuperTypes(Class)}.
     * <p>
     * for example, for classes A,B,C where A supertype of B, B supertype of C:
     * <ul>
     *     <li>if scanning C resulted in B (B->C in store), but A was not scanned (although A supertype of B) - then getSubTypes(A) will not return C</li>
     *     <li>if expanding supertypes, B will be expanded with A (A->B in store) - then getSubTypes(A) will return C</li>
     * </ul>
     */
    public void expandSuperTypes() {
        if (store.keySet().contains(index(SubTypesScanner.class))) {
            Multimap<String, String> mmap = store.get(index(SubTypesScanner.class));
            Sets.SetView<String> keys = Sets.difference(mmap.keySet(), Sets.newHashSet(mmap.values()));
            Multimap<String, String> expand = HashMultimap.create();
            for (String key : keys) {
                final Class<?> type = forName(key);
                if (type != null) {
                    expandSupertypes(expand, key, type);
                }
            }
            mmap.putAll(expand);
        }
    }

    private void expandSupertypes(Multimap<String, String> mmap, String key, Class<?> type) {
        for (Class<?> supertype : ReflectionUtils.getSuperTypes(type)) {
            if (mmap.put(supertype.getName(), key)) {
                if (log != null) log.debug("expanded subtype {} -> {}", supertype.getName(), key);
                expandSupertypes(mmap, supertype.getName(), supertype);
            }
        }
    }

    //query
    /**
     * gets all sub types in hierarchy of a given type
     * <p/>depends on SubTypesScanner configured
     */
    public <T> Set<Class<? extends T>> getSubTypesOf(final Class<T> type) {
        return Sets.newHashSet(ReflectionUtils.<T>forNames(
                store.getAll(index(SubTypesScanner.class), Arrays.asList(type.getName())), loaders()));
    }

    /**
     * get types annotated with a given annotation, both classes and annotations
     * <p>{@link java.lang.annotation.Inherited} is not honored by default.
     * <p>when honoring @Inherited, meta-annotation should only effect annotated super classes and its sub types
     * <p><i>Note that this (@Inherited) meta-annotation type has no effect if the annotated type is used for anything other then a class.
     * Also, this meta-annotation causes annotations to be inherited only from superclasses; annotations on implemented interfaces have no effect.</i>
     * <p/>depends on TypeAnnotationsScanner and SubTypesScanner configured
     */
    public Set<Class<?>> getTypesAnnotatedWith(final Class<? extends Annotation> annotation) {
        return getTypesAnnotatedWith(annotation, false);
    }

    /**
     * get types annotated with a given annotation, both classes and annotations
     * <p>{@link java.lang.annotation.Inherited} is honored according to given honorInherited.
     * <p>when honoring @Inherited, meta-annotation should only effect annotated super classes and it's sub types
     * <p>when not honoring @Inherited, meta annotation effects all subtypes, including annotations interfaces and classes
     * <p><i>Note that this (@Inherited) meta-annotation type has no effect if the annotated type is used for anything other then a class.
     * Also, this meta-annotation causes annotations to be inherited only from superclasses; annotations on implemented interfaces have no effect.</i>
     * <p/>depends on TypeAnnotationsScanner and SubTypesScanner configured
     */
    public Set<Class<?>> getTypesAnnotatedWith(final Class<? extends Annotation> annotation, boolean honorInherited) {
        Iterable<String> annotated = store.get(index(TypeAnnotationsScanner.class), annotation.getName());
        Iterable<String> classes = getAllAnnotated(annotated, annotation.isAnnotationPresent(Inherited.class), honorInherited);
        return Sets.newHashSet(concat(forNames(annotated, loaders()), forNames(classes, loaders())));
    }

    /**
     * get types annotated with a given annotation, both classes and annotations, including annotation member values matching
     * <p>{@link java.lang.annotation.Inherited} is not honored by default
     * <p/>depends on TypeAnnotationsScanner configured
     */
    public Set<Class<?>> getTypesAnnotatedWith(final Annotation annotation) {
        return getTypesAnnotatedWith(annotation, false);
    }

    /**
     * get types annotated with a given annotation, both classes and annotations, including annotation member values matching
     * <p>{@link java.lang.annotation.Inherited} is honored according to given honorInherited
     * <p/>depends on TypeAnnotationsScanner configured
     */
    public Set<Class<?>> getTypesAnnotatedWith(final Annotation annotation, boolean honorInherited) {
        Iterable<String> annotated = store.get(index(TypeAnnotationsScanner.class), annotation.annotationType().getName());
        Iterable<Class<?>> filter = filter(forNames(annotated, loaders()), withAnnotation(annotation));
        Iterable<String> classes = getAllAnnotated(names(filter), annotation.annotationType().isAnnotationPresent(Inherited.class), honorInherited);
        return Sets.newHashSet(concat(filter, forNames(filter(classes, not(in(Sets.newHashSet(annotated)))), loaders())));
    }

    protected Iterable<String> getAllAnnotated(Iterable<String> annotated, boolean inherited, boolean honorInherited) {
        if (honorInherited) {
            if (inherited) {
                Iterable<String> subTypes = store.get(index(SubTypesScanner.class), filter(annotated, new Predicate<String>() {
                    public boolean apply(@Nullable String input) {
                        final Class<?> type = forName(input, loaders());
                        return type != null && !type.isInterface();
                    }
                }));
                return concat(subTypes, store.getAll(index(SubTypesScanner.class), subTypes));
            } else {
                return annotated;
            }
        } else {
            Iterable<String> subTypes = concat(annotated, store.getAll(index(TypeAnnotationsScanner.class), annotated));
            return concat(subTypes, store.getAll(index(SubTypesScanner.class), subTypes));
        }
    }

    /**
     * get all methods annotated with a given annotation
     * <p/>depends on MethodAnnotationsScanner configured
     */
    public Set<Method> getMethodsAnnotatedWith(final Class<? extends Annotation> annotation) {
        Iterable<String> methods = store.get(index(MethodAnnotationsScanner.class), annotation.getName());
        return getMethodsFromDescriptors(methods, loaders());
    }

    /**
     * get all methods annotated with a given annotation, including annotation member values matching
     * <p/>depends on MethodAnnotationsScanner configured
     */
    public Set<Method> getMethodsAnnotatedWith(final Annotation annotation) {
        return filter(getMethodsAnnotatedWith(annotation.annotationType()), withAnnotation(annotation));
    }

    /** get methods with parameter types matching given {@code types}*/
    public Set<Method> getMethodsMatchParams(Class<?>... types) {
        return getMethodsFromDescriptors(store.get(index(MethodParameterScanner.class), names(types).toString()), loaders());
    }

    /** get methods with return type match given type */
    public Set<Method> getMethodsReturn(Class returnType) {
        return getMethodsFromDescriptors(store.get(index(MethodParameterScanner.class), names(returnType)), loaders());
    }

    /** get methods with any parameter annotated with given annotation */
    public Set<Method> getMethodsWithAnyParamAnnotated(Class<? extends Annotation> annotation) {
        return getMethodsFromDescriptors(store.get(index(MethodParameterScanner.class), annotation.getName()), loaders());

    }

    /** get methods with any parameter annotated with given annotation, including annotation member values matching */
    public Set<Method> getMethodsWithAnyParamAnnotated(Annotation annotation) {
        return filter(getMethodsWithAnyParamAnnotated(annotation.annotationType()), withAnyParameterAnnotation(annotation));
    }

    /**
     * get all constructors annotated with a given annotation
     * <p/>depends on MethodAnnotationsScanner configured
     */
    public Set<Constructor> getConstructorsAnnotatedWith(final Class<? extends Annotation> annotation) {
        Iterable<String> methods = store.get(index(MethodAnnotationsScanner.class), annotation.getName());
        return getConstructorsFromDescriptors(methods, loaders());
    }

    /**
     * get all constructors annotated with a given annotation, including annotation member values matching
     * <p/>depends on MethodAnnotationsScanner configured
     */
    public Set<Constructor> getConstructorsAnnotatedWith(final Annotation annotation) {
        return filter(getConstructorsAnnotatedWith(annotation.annotationType()), withAnnotation(annotation));
    }

    /** get constructors with parameter types matching given {@code types}*/
    public Set<Constructor> getConstructorsMatchParams(Class<?>... types) {
        return getConstructorsFromDescriptors(store.get(index(MethodParameterScanner.class), names(types).toString()), loaders());
    }

    /** get constructors with any parameter annotated with given annotation */
    public Set<Constructor> getConstructorsWithAnyParamAnnotated(Class<? extends Annotation> annotation) {
        return getConstructorsFromDescriptors(store.get(index(MethodParameterScanner.class), annotation.getName()), loaders());
    }

    /** get constructors with any parameter annotated with given annotation, including annotation member values matching */
    public Set<Constructor> getConstructorsWithAnyParamAnnotated(Annotation annotation) {
        return filter(getConstructorsWithAnyParamAnnotated(annotation.annotationType()), withAnyParameterAnnotation(annotation));
    }

    /**
     * get all fields annotated with a given annotation
     * <p/>depends on FieldAnnotationsScanner configured
     */
    public Set<Field> getFieldsAnnotatedWith(final Class<? extends Annotation> annotation) {
        final Set<Field> result = Sets.newHashSet();
        for (String annotated : store.get(index(FieldAnnotationsScanner.class), annotation.getName())) {
            result.add(getFieldFromString(annotated, loaders()));
        }
        return result;
    }

    /**
     * get all methods annotated with a given annotation, including annotation member values matching
     * <p/>depends on FieldAnnotationsScanner configured
     */
    public Set<Field> getFieldsAnnotatedWith(final Annotation annotation) {
        return filter(getFieldsAnnotatedWith(annotation.annotationType()), withAnnotation(annotation));
    }

    /** get resources relative paths where simple name (key) matches given namePredicate
     * <p>depends on ResourcesScanner configured
     * */
    public Set<String> getResources(final Predicate<String> namePredicate) {
        Iterable<String> resources = Iterables.filter(store.get(index(ResourcesScanner.class)).keySet(), namePredicate);
        return Sets.newHashSet(store.get(index(ResourcesScanner.class), resources));
    }

    /** get resources relative paths where simple name (key) matches given regular expression
     * <p>depends on ResourcesScanner configured
     * <pre>Set<String> xmls = reflections.getResources(".*\\.xml");</pre>
     */
    public Set<String> getResources(final Pattern pattern) {
        return getResources(new Predicate<String>() {
            public boolean apply(String input) {
                return pattern.matcher(input).matches();
            }
        });
    }

    /** get parameter names of given {@code method}
     * <p>depends on MethodParameterNamesScanner configured
     */
    public List<String> getMethodParamNames(Method method) {
        Iterable<String> names = store.get(index(MethodParameterNamesScanner.class), name(method));
        return !Iterables.isEmpty(names) ? Arrays.asList(Iterables.getOnlyElement(names).split(", ")) : Arrays.<String>asList();
    }

    /** get parameter names of given {@code constructor}
     * <p>depends on MethodParameterNamesScanner configured
     */
    public List<String> getConstructorParamNames(Constructor constructor) {
        Iterable<String> names = store.get(index(MethodParameterNamesScanner.class), Utils.name(constructor));
        return !Iterables.isEmpty(names) ? Arrays.asList(Iterables.getOnlyElement(names).split(", ")) : Arrays.<String>asList();
    }

    /** get all given {@code field} usages in methods and constructors
     * <p>depends on MemberUsageScanner configured
     */
    public Set<Member> getFieldUsage(Field field) {
        return getMembersFromDescriptors(store.get(index(MemberUsageScanner.class), name(field)));
    }

    /** get all given {@code method} usages in methods and constructors
     * <p>depends on MemberUsageScanner configured
     */
    public Set<Member> getMethodUsage(Method method) {
        return getMembersFromDescriptors(store.get(index(MemberUsageScanner.class), name(method)));
    }

    /** get all given {@code constructors} usages in methods and constructors
     * <p>depends on MemberUsageScanner configured
     */
    public Set<Member> getConstructorUsage(Constructor constructor) {
        return getMembersFromDescriptors(store.get(index(MemberUsageScanner.class), name(constructor)));
    }

    /** get all types scanned. this is effectively similar to getting all subtypes of Object.
     * <p>depends on SubTypesScanner configured with {@code SubTypesScanner(false)}, otherwise {@code ReflectionsException} is thrown
     * <p><i>note using this might be a bad practice. it is better to get types matching some criteria,
     * such as {@link #getSubTypesOf(Class)} or {@link #getTypesAnnotatedWith(Class)}</i>
     * @return Set of String, and not of Class, in order to avoid definition of all types in PermGen
     */
    public Set<String> getAllTypes() {
        Set<String> allTypes = Sets.newHashSet(store.getAll(index(SubTypesScanner.class), Object.class.getName()));
        if (allTypes.isEmpty()) {
            throw new ReflectionsException("Couldn't find subtypes of Object. " +
                    "Make sure SubTypesScanner initialized to include Object class - new SubTypesScanner(false)");
        }
        return allTypes;
    }

    /** returns the {@link org.reflections.Store} used for storing and querying the metadata */
    public Store getStore() {
        return store;
    }

    /** returns the {@link org.reflections.Configuration} object of this instance */
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * serialize to a given directory and filename
     * <p>* it is preferred to specify a designated directory (for example META-INF/reflections),
     * so that it could be found later much faster using the load method
     * <p>see the documentation for the save method on the configured {@link org.reflections.serializers.Serializer}
     */
    public File save(final String filename) {
        return save(filename, configuration.getSerializer());
    }

    /**
     * serialize to a given directory and filename using given serializer
     * <p>* it is preferred to specify a designated directory (for example META-INF/reflections),
     * so that it could be found later much faster using the load method
     */
    public File save(final String filename, final Serializer serializer) {
        File file = serializer.save(this, filename);
        if (log != null) //noinspection ConstantConditions
            log.info("Reflections successfully saved in " + file.getAbsolutePath() + " using " + serializer.getClass().getSimpleName());
        return file;
    }

    private static String index(Class<? extends Scanner> scannerClass) { return scannerClass.getSimpleName(); }

    private ClassLoader[] loaders() { return configuration.getClassLoaders(); }
}
