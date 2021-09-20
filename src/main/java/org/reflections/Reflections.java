package org.reflections;

import javassist.bytecode.ClassFile;
import org.reflections.scanners.MemberUsageScanner;
import org.reflections.scanners.MethodParameterNamesScanner;
import org.reflections.scanners.Scanner;
import org.reflections.scanners.Scanners;
import org.reflections.serializers.Serializer;
import org.reflections.serializers.XmlSerializer;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.reflections.util.NameHelper;
import org.reflections.util.QueryFunction;
import org.reflections.vfs.Vfs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.lang.String.format;
import static org.reflections.ReflectionUtils.withAnnotation;
import static org.reflections.ReflectionUtils.withAnyParameterAnnotation;
import static org.reflections.scanners.Scanners.*;

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
 * <pre>{@code
 *      Reflections reflections = new Reflections("my.project.prefix");
 *
 *      Set<Class<? extends SomeType>> subTypes = reflections.getSubTypesOf(SomeType.class);
 *
 *      Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(SomeAnnotation.class);
 * }</pre>
 * <p>Basically, to use Reflections first instantiate it with one of the constructors, then depending on the scanners, use the convenient query methods:
 * <pre>{@code
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
 * }</pre>
 * And then query, for example:
 * <pre>{@code 
 *       Set<Class<? extends Module>> modules = reflections.getSubTypesOf(com.google.inject.Module.class);
 *       Set<Class<?>> singletons =             reflections.getTypesAnnotatedWith(javax.inject.Singleton.class);
 *
 *       Set<String> properties =       reflections.getResources(Pattern.compile(".*\\.properties"));
 *       Set<Constructor> injectables = reflections.getConstructorsAnnotatedWith(javax.inject.Inject.class);
 *       Set<Method> deprecateds =      reflections.getMethodsAnnotatedWith(javax.ws.rs.Path.class);
 *       Set<Field> ids =               reflections.getFieldsAnnotatedWith(javax.persistence.Id.class);
 *
 *       Set<Method> someMethods =      reflections.getMethodsMatchParams(long.class, int.class);
 *       Set<Method> voidMethods =      reflections.getMethodsReturn(void.class);
 *       Set<Method> pathParamMethods = reflections.getMethodsWithAnyParamAnnotated(PathParam.class);
 *       List<String> parameterNames =  reflections.getMethodsParamNames(Method.class);
 *
 *       Set<Member> fieldUsage =       reflections.getFieldUsage(Field.class);
 *       Set<Member> methodUsage =      reflections.getMethodUsage(Method.class);
 *       Set<Member> constructorUsage = reflections.getConstructorUsage(Constructor.class);
 * }</pre>
 * <p>You can use other scanners defined in Reflections as well, such as: SubTypesScanner, TypeAnnotationsScanner (both default),
 * ResourcesScanner, MethodsAnnotated, ConstructorAnnotated, FieldAnnotated,
 * MethodParameters, MethodParameterNamesScanner, MemberUsageScanner or any custom scanner.
 * <p>Use {@link #store} to access and query the store directly
 * <p>In order to save the store metadata, use {@link #save(String)} or {@link #save(String, org.reflections.serializers.Serializer)}
 * for example with {@link org.reflections.serializers.XmlSerializer} or {@link org.reflections.serializers.JavaCodeSerializer}
 * <p>In order to collect pre saved metadata and avoid re-scanning, use {@link #collect(String, java.util.function.Predicate, org.reflections.serializers.Serializer)}
 * <p><i>Make sure to scan all the transitively relevant packages.
 * <br>for instance, given your class C extends B extends A, and both B and A are located in another package than C,
 * when only the package of C is scanned - then querying for sub types of A returns nothing (transitive), but querying for sub types of B returns C (direct).
 * In that case make sure to scan all relevant packages a priori.</i>
 * <p><p><p>For Javadoc, source code, and more information about Reflections Library, see http://github.com/ronmamo/reflections/
 */
public class Reflections implements NameHelper {
    public final static Logger log = LoggerFactory.getLogger(Reflections.class);

    protected final transient Configuration configuration;
    protected final Store store;

    /**
     * constructs a Reflections instance and scan according to given {@link org.reflections.Configuration}
     * <p>it is preferred to use {@link org.reflections.util.ConfigurationBuilder}
     */
    public Reflections(Configuration configuration) {
        this.configuration = configuration;
        Map<String, Map<String, Set<String>>> storeMap = scan();
        if (configuration.shouldExpandSuperTypes()) {
            expandSuperTypes(storeMap.get(SubTypes.index()));
        }
        store = new Store(storeMap);
    }

    public Reflections(Store store) {
        this.configuration = new ConfigurationBuilder();
        this.store = store;
    }

    /**
     * a convenient constructor for scanning within a package prefix.
     * <p>this actually create a {@link org.reflections.Configuration} with:
     * <br> - urls that contain resources with name {@code prefix}
     * <br> - filterInputsBy where name starts with the given {@code prefix}
     * <br> - scanners set to the given {@code scanners}, otherwise defaults to {@link Scanners#TypesAnnotated} and {@link Scanners#SubTypes}.
     * @param prefix package prefix, to be used with {@link org.reflections.util.ClasspathHelper#forPackage(String, ClassLoader...)} )}
     * @param scanners optionally supply scanners
     */
    public Reflections(String prefix, Scanner... scanners) {
        this((Object) prefix, scanners);
    }

    /**
     * Convenient constructor for Reflections, where given {@code Object...} parameter types can be either:
     * <ul>
     *     <li>{@link String} - would add urls using {@link org.reflections.util.ClasspathHelper#forPackage(String, ClassLoader...)} ()}</li>
     *     <li>{@link Class} - would add urls using {@link org.reflections.util.ClasspathHelper#forClass(Class, ClassLoader...)} </li>
     *     <li>{@link ClassLoader} - would use this classloaders in order to find urls in {@link org.reflections.util.ClasspathHelper#forPackage(String, ClassLoader...)} and {@link org.reflections.util.ClasspathHelper#forClass(Class, ClassLoader...)}</li>
     *     <li>{@link org.reflections.scanners.Scanner} - would use given scanner, overriding the default scanners</li>
     *     <li>{@link java.net.URL} - would add the given url for scanning</li>
     *     <li>{@link Object[]} - would use each element as above</li>
     * </ul>
     *
     * <br><br>for example:
     * <pre>
     *     new Reflections("some.path", classLoader);
     *     //or
     *     new Reflections("some.path", someScanner, anotherScanner, classLoader);
     * </pre>
     */
    public Reflections(Object... params) {
        this(ConfigurationBuilder.build(params));
    }

    protected Reflections() {
        configuration = new ConfigurationBuilder();
        store = new Store(new HashMap<>());
    }

    protected Map<String, Map<String, Set<String>>> scan() {
        long start = System.currentTimeMillis();
        Map<String, Set<Map.Entry<String, String>>> collect = configuration.getScanners().stream().map(Scanner::index).distinct()
            .collect(Collectors.toConcurrentMap(s -> s, s -> new HashSet<>()));
        Set<URL> urls = configuration.getUrls();

        (configuration.isParallel() ? urls.stream().parallel() : urls.stream()).forEach(url -> {
            Vfs.Dir dir = null;
            try {
                dir = Vfs.fromURL(url);
                for (Vfs.File file : dir.getFiles()) {
                    if (doFilter(file, configuration.getInputsFilter())) {
                        ClassFile classFile = null;
                        for (Scanner scanner : configuration.getScanners()) {
                            try {
                                if (doFilter(file, scanner::acceptsInput)) {
                                    List<Map.Entry<String, String>> entries = scanner.scan(file);
                                    if (entries == null) {
                                        if (classFile == null) classFile = getClassFile(file);
                                        entries = scanner.scan(classFile);
                                    }
                                    collect.get(scanner.index()).addAll(entries);
                                }
                            } catch (Exception e) {
                                if (log != null) log.trace("could not scan file {} with scanner {}", file.getRelativePath(), scanner.getClass().getSimpleName(), e);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                if (log != null) log.warn("could not create Vfs.Dir from url. ignoring the exception and continuing", e);
            } finally {
                if (dir != null) dir.close();
            }
        });

        // merge
        Map<String, Map<String, Set<String>>> storeMap =
            collect.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().stream().filter(e -> e.getKey() != null)
                        .collect(Collectors.groupingBy(
                            Map.Entry::getKey,
                            HashMap::new,
                            Collectors.mapping(Map.Entry::getValue, Collectors.toSet())))));

        if (log != null) {
            int keys = 0, values = 0;
            for (Map<String, Set<String>> map : storeMap.values()) {
                keys += map.size();
                values += map.values().stream().mapToLong(Set::size).sum();
            }
            log.info(format("Reflections took %d ms to scan %d urls, producing %d keys and %d values", System.currentTimeMillis() - start, urls.size(), keys, values));
        }

        return storeMap;
    }

    private boolean doFilter(Vfs.File file, Predicate<String> predicate) {
        String path = file.getRelativePath();
        String fqn = path.replace('/', '.');
        return predicate == null || predicate.test(path) || predicate.test(fqn);
    }

    private ClassFile getClassFile(Vfs.File file) {
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(file.openInputStream()))) {
            return new ClassFile(dis);
        } catch (Exception e) {
            throw new ReflectionsException("could not create class object from file " + file.getRelativePath(), e);
        }
    }

    /** collect saved Reflection xml resources and merge it into a Reflections instance
     * <p>by default, resources are collected from all urls that contains the package META-INF/reflections
     * and includes files matching the pattern .*-reflections.xml
     * */
    public static Reflections collect() {
        return collect("META-INF/reflections/", new FilterBuilder().includePattern(".*-reflections\\.xml"));
    }

    /**
     * collect saved Reflections metadata from all urls that contains the given {@code packagePrefix} and matches the given {@code resourceNameFilter},
     * and deserialize using the default serializer {@link org.reflections.serializers.XmlSerializer}
     * <pre>{@code Reflections.collect("META-INF/reflections/",
     *   new FilterBuilder().includePattern(".*-reflections\\.xml")}</pre>
     * <i>it is preferred to use a designated resource prefix (for example META-INF/reflections but not just META-INF), so that relevant urls could be found much faster</i>
     */
    public static Reflections collect(String packagePrefix, Predicate<String> resourceNameFilter) {
        return collect(packagePrefix, resourceNameFilter, new XmlSerializer());
    }

    /**
     * collect saved Reflections metadata from all urls that contains the given {@code packagePrefix} and matches the given {@code resourceNameFilter},
     * and deserializes using the given {@code serializer}
     * <pre>{@code Reflections reflections = Reflections.collect(
     *   "META-INF/reflections/",
     *   new FilterBuilder().includePattern(".*-reflections\\.xml"),
     *   new XmlSerializer())}</pre>
     * <i>it is preferred to use a designated resource prefix (for example META-INF/reflections but not just META-INF), so that relevant urls could be found much faster</i> */
    public static Reflections collect(String packagePrefix, Predicate<String> resourceNameFilter, Serializer serializer) {
        Collection<URL> urls = ClasspathHelper.forPackage(packagePrefix);
        Iterable<Vfs.File> files = Vfs.findFiles(urls, packagePrefix, resourceNameFilter);
        Reflections reflections = new Reflections();
        StreamSupport.stream(files.spliterator(), false)
            .forEach(file -> {
                try (InputStream inputStream = file.openInputStream()) {
                    reflections.collect(inputStream, serializer);
                } catch (IOException e) {
                    throw new ReflectionsException("could not merge " + file, e);
                }
            });
        return reflections;
    }

    /** deserialize and merge saved Reflections metadata from the given input stream, using the serializer configured in this instance's Configuration
     * <p><i>useful if you know the serialized resource location and prefer not to look it up the classpath</i> */
    public Reflections collect(InputStream inputStream, Serializer serializer) {
        return merge(serializer.read(inputStream));
    }

    /** deserialize and merge saved Reflections metadata from the given {@code file} using the given {@code serializer}
     * <p><i>useful if you know the serialized resource location and prefer not to look it up the classpath</i> */
    public Reflections collect(File file, Serializer serializer) {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            return collect(inputStream, serializer);
        } catch (IOException e) {
            throw new ReflectionsException("could not obtain input stream from file " + file, e);
        }
    }

    /** merges a Reflections instance metadata into this instance */
    public Reflections merge(Reflections reflections) {
        reflections.store.forEach((index, map) -> this.store.merge(index, map, (m1, m2) -> {
            m2.forEach((k, v) -> m1.merge(k, v, (s1, s2) -> { s1.addAll(s2); return s1;}));
            return m1;
        }));
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
    public void expandSuperTypes(Map<String, Set<String>> map) {
        if (map == null || map.isEmpty()) return;
        Set<String> keys = new LinkedHashSet<>(map.keySet());
        keys.removeAll(map.values().stream().flatMap(Collection::stream).collect(Collectors.toSet()));
        for (String key : keys) {
            Class<?> type = forClass(key, loaders());
            if (type != null) {
                expandSupertypes(map, key, type);
            }
        }
    }

    private void expandSupertypes(Map<String, Set<String>> map, String key, Class<?> type) {
        for (Class<?> supertype : ReflectionUtils.getSuperTypes(type)) {
            String supertypeName = supertype.getName();
            Map.Entry<String, String> entry = new AbstractMap.SimpleEntry<>(supertypeName, key);
            if (!map.containsKey(supertypeName)) {
                map.computeIfAbsent(entry.getKey(), s -> new HashSet<>()).add(entry.getValue());
                expandSupertypes(map, supertypeName, supertype);
            }
        }
    }

    /**
     * apply {@code QueryFunction} on {@code Store}
     * <pre>{@code Set<T> ts = get(query)}</pre>
     * <p>use builders such as {@link Scanners} and {@link ReflectionUtils}:
     * <pre>{@code
     * Set<Class<?>> as = get(Scanners.TypesAnnotated.with(A.class))
     * Set<Class<? extends B>> bs = get(Scanners.SubTypes.of(B.class))
     * Set<Method> ms = get(ReflectionUtils.Methods.of(B.class))
     * }</pre>
     * <p>supports function map, filter, as
     * <pre>{@code
     * get(SuperTypes.of(C.class)
     *   .filter(c -> !c.isInterface())     // .filter
     *   .as(Class.class)                   // .as
     *   .map(c -> Methods.of(c)            // .map
     *     .filter(withPublic())))
     * }</pre>
     * */
    public <T> Set<T> get(QueryFunction<Store, T> query) {
        return query.apply(store);
    }

    /**
     * gets all sub types in hierarchy of a given type
     * <p/>depends on SubTypesScanner configured
     */
    public <T> Collection<Class<? extends T>> getSubTypesOf(Class<T> type) {
        //noinspection unchecked
        return (Collection<Class<? extends T>>) get(SubTypes.of(type)
            .as((Class<? extends T>) Class.class, loaders()));
    }

    /**
     * get types annotated with a given annotation, both classes and annotations
     * <p>{@link java.lang.annotation.Inherited} is not honored by default.
     * <p>when honoring @Inherited, meta-annotation should only effect annotated super classes and its sub types
     * <p><i>Note that this (@Inherited) meta-annotation type has no effect if the annotated type is used for anything other then a class.
     * Also, this meta-annotation causes annotations to be inherited only from superclasses; annotations on implemented interfaces have no effect.</i>
     * <p/>depends on TypeAnnotationsScanner and SubTypesScanner configured
     */
    public Collection<Class<?>> getTypesAnnotatedWith(Class<? extends Annotation> annotation) {
        return get(SubTypes.of(TypesAnnotated.with(annotation)).asClass(loaders()));
    }

    /**
     * get types annotated with a given annotation, both classes and annotations
     * <p>{@link java.lang.annotation.Inherited} is honored according to given honorInherited.
     * <p>when honoring @Inherited, meta-annotation should only effect annotated super classes and subtypes
     * <p>when not honoring @Inherited, meta annotation effects all subtypes, including annotations interfaces and classes
     * <p><i>Note that this (@Inherited) meta-annotation type has no effect if the annotated type is used for anything other then a class.
     * Also, this meta-annotation causes annotations to be inherited only from superclasses; annotations on implemented interfaces have no effect.</i>
     * <p/>depends on TypeAnnotationsScanner and SubTypesScanner configured
     */
    public Collection<Class<?>> getTypesAnnotatedWith(Class<? extends Annotation> annotation, boolean honorInherited) {
        if (!honorInherited) {
            return getTypesAnnotatedWith(annotation);
        } else {
            if (annotation.isAnnotationPresent(Inherited.class)) {
                return get(TypesAnnotated.get(annotation)
                    .add(SubTypes.of(TypesAnnotated.get(annotation)
                        .filter(c -> !forClass(c, loaders()).isInterface())))
                    .asClass(loaders()));
            } else {
                return get(TypesAnnotated.get(annotation).asClass(loaders()));
            }
        }
    }

    /**
     * get types annotated with a given annotation, both classes and annotations, including annotation member values matching
     * <p>{@link java.lang.annotation.Inherited} is not honored by default
     * <p/>depends on TypeAnnotationsScanner configured
     */
    public Collection<Class<?>> getTypesAnnotatedWith(Annotation annotation) {
        return get(SubTypes.of(
            TypesAnnotated.of(TypesAnnotated.get(annotation.annotationType())
                .filter(c -> withAnnotation(annotation).test(forClass(c, loaders())))))
            .asClass(loaders()));
    }

    /**
     * get types annotated with a given annotation, both classes and annotations, including annotation member values matching
     * <p>{@link java.lang.annotation.Inherited} is honored according to given honorInherited
     * <p/>depends on TypeAnnotationsScanner configured
     */
    public Collection<Class<?>> getTypesAnnotatedWith(Annotation annotation, boolean honorInherited) {
        if (!honorInherited) {
            return getTypesAnnotatedWith(annotation);
        } else {
            Class<? extends Annotation> type = annotation.annotationType();
            if (type.isAnnotationPresent(Inherited.class)) {
                return get(TypesAnnotated.with(type)
                    .asClass(loaders())
                    .filter(withAnnotation(annotation))
                    .add(SubTypes.of(
                        TypesAnnotated.with(type)
                            .asClass(loaders())
                            .filter(c -> !c.isInterface()))
                        .asClass(loaders())));
            } else {
                return get(TypesAnnotated.with(type)
                    .asClass(loaders())
                    .filter(withAnnotation(annotation)));
            }
        }
    }

    /**
     * get all methods annotated with a given annotation
     * <p/>depends on METHODS_ANNOTATED configured
     */
    public Collection<Method> getMethodsAnnotatedWith(Class<? extends Annotation> annotation) {
        return get(MethodsAnnotated.with(annotation).as(Method.class, loaders()));
    }

    /**
     * get all methods annotated with a given annotation, including annotation member values matching
     * <p/>depends on METHODS_ANNOTATED configured
     */
    public Collection<Method> getMethodsAnnotatedWith(Annotation annotation) {
        return get(MethodsAnnotated.with(annotation.annotationType()).as(Method.class, loaders())
            .filter(withAnnotation(annotation)));
    }

    /** get methods with signature matching given {@code types} */
    public Collection<Method> getMethodsWithSignature(Class<?>... types) {
        return get(MethodsSignature.with(types).as(Method.class, loaders()));
    }

    /** get methods with any parameter matching {@code type} or annotated with {@code annotation} */
    public Collection<Method> getMethodsWithParameter(AnnotatedElement type) {
        return get(MethodsParameter.of(type).as(Method.class, loaders()));
    }

    /** get methods with any parameter matching {@code type} or annotated with {@code annotation} */
    public Collection<Method> getMethodsWithParameter(Annotation annotation) {
        return get(MethodsParameter.of(annotation.annotationType()).as(Method.class, loaders())
            .filter(withAnyParameterAnnotation(annotation)));
    }

    /** get methods with return type match given type */
    public Collection<Method> getMethodsReturn(Class<?> returnType) {
        return get(MethodsReturn.with(returnType).as(Method.class, loaders()));
    }

    /**
     * get all constructors annotated with a given annotation
     * <p/>depends on METHODS_ANNOTATED configured
     */
    public Collection<Constructor> getConstructorsAnnotatedWith(Class<? extends Annotation> annotation) {
        return get(ConstructorsAnnotated.with(annotation).as(Constructor.class, loaders()));
    }

    /**
     * get all constructors annotated with a given annotation, including annotation member values matching
     * <p/>depends on METHODS_ANNOTATED configured
     */
    public Collection<Constructor> getConstructorsAnnotatedWith(Annotation annotation) {
        return get(ConstructorsAnnotated.with(annotation.annotationType()).as(Constructor.class, loaders())
            .filter(withAnyParameterAnnotation(annotation)));
    }

    /** get constructors with signature matching given {@code types} */
    public Collection<Constructor> getConstructorsWithSignature(Class<?>... types) {
        return get(ConstructorsSignature.with(types).as(Constructor.class, loaders()));
    }

    /** get constructors with any parameter matching {@code type}*/
    public Collection<Constructor> getConstructorsWithParameter(AnnotatedElement type) {
        return get(ConstructorsParameter.of(type).as(Constructor.class, loaders()));
    }

    /** get constructors with any parameter matching {@code annotation}*/
    public Collection<Constructor> getConstructorsWithParameter(Annotation annotation) {
        return get(ConstructorsParameter.of(annotation.annotationType()).as(Constructor.class, loaders())
            .filter(withAnnotation(annotation)));
    }

    /** get constructors with any parameter annotated with given annotation */
    public Collection<Constructor> getConstructorsWithParameterAnnotated(Class<? extends Annotation> annotation) {
        return get(ConstructorsParameter.of(annotation).as(Constructor.class, loaders()));
    }

    /** get constructors with any parameter annotated with given annotation, including annotation member values matching */
    public Collection<Constructor> getConstructorsWithParameterAnnotated(Annotation annotation) {
        return get(ConstructorsParameter.of(annotation.annotationType()).as(Constructor.class, loaders())
            .filter(withAnyParameterAnnotation(annotation)));
    }

    /**
     * get all fields annotated with a given annotation
     * <p/>depends on FieldAnnotationsScanner configured
     */
    public Collection<Field> getFieldsAnnotatedWith(Class<? extends Annotation> annotation) {
        return get(FieldsAnnotated.with(annotation).as(Field.class, loaders()));
    }

    /**
     * get all methods annotated with a given annotation, including annotation member values matching
     * <p/>depends on FieldAnnotationsScanner configured
     */
    public Collection<Field> getFieldsAnnotatedWith(Annotation annotation) {
        return get(FieldsAnnotated.with(annotation.annotationType()).as(Field.class, loaders())
            .filter(withAnnotation(annotation)));
    }

    /** get resources matching regular expression <pre>{@code Collection<String> xmls = reflections.getResources(".*\\.xml")}</pre>
     * <p>depends on {@link Scanners#Resources} configured */
    public Collection<String> getResources(String pattern) {
        return get(Resources.with(pattern));
    }

    /** get resources matching regular expression <pre>{@code Collection<String> xmls = reflections.getResources(Pattern.compile(".*\\.xml"))}</pre>
     * <p>depends on ResourcesScanner configured */
    public Collection<String> getResources(Pattern pattern) {
        return getResources(pattern.pattern());
    }

    /**
     * get parameter names of given method or constructor
     * <p>depends on MethodParameterNamesScanner configured
     */
    public List<String> getMemberParameterNames(Member member) {
        return get(MethodParameterNamesScanner.class, (AnnotatedElement) member)
            .stream().flatMap(s -> Stream.of(s.split(", "))).collect(Collectors.toList());
    }

    /** get all code usages for the given {@code member} field/method/constructor
     * <p>depends on MemberUsageScanner configured
     */
    public Collection<Member> getMemberUsage(Member member) {
        return forNames(get(MemberUsageScanner.class, (AnnotatedElement) member), Member.class, loaders());
    }

    /** returns all keys and values scanned by {@link Scanners#SubTypes} scanner
     * <p><i>using this api is discouraged, it is better to get elements by specific criteria such as {@code SubTypes.of(Class)} or {@code TypesAnnotated.with(Class)} </i>
     * <p></p><i>deprecated, use {@link #getAll(Scanner)} instead</i> */
    @Deprecated
    public Collection<String> getAllTypes() {
        return getAll(SubTypes);
    }

    /** returns all key and values scanned by {@code scanner} <pre>{@code Collection<String> all = reflections.getAll(SubTypes)}</pre>
     * <p><i>using this is discouraged, it is better to get elements by specific criteria such as {@code SubTypes.of(Class)} or {@code TypesAnnotated.with(Class)} </i> */
    public Collection<String> getAll(Scanner scanner) {
        Map<String, Set<String>> map = store.getOrDefault(scanner.index(), Collections.emptyMap());
        return Stream.concat(map.keySet().stream(), map.values().stream().flatMap(Collection::stream)).collect(Collectors.toSet());
    }

    private Set<String> get(Class<? extends Scanner> scannerClass, AnnotatedElement member) {
        return new LinkedHashSet<>(store.getOrDefault(scannerClass.getSimpleName(), Collections.emptyMap()).getOrDefault(toName(member), Collections.emptySet()));
    }

    /** returns the {@link org.reflections.Store} object used for storing and querying the metadata
     * <p>{@code Store} is basically {@code Map<String, Map<String, Set<String>>>} */
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
    public File save(String filename) {
        return save(filename, new XmlSerializer());
    }

    /**
     * serialize metadata to the given {@code filename} and {@link Serializer}
     * <p>
     * directory and filename using given serializer
     * <p>* it is preferred to specify a designated directory (for example META-INF/reflections),
     * so that it could be found later much faster using the load method
     */
    public File save(String filename, Serializer serializer) {
        return serializer.save(this, filename);
    }

    ClassLoader[] loaders() { return configuration.getClassLoaders(); }
}
