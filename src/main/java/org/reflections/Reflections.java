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

import javax.annotation.Nullable;
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
 * <p></p>
 * Reflections scans and indexes your project's classpath, allowing reverse query of the type system metadata on runtime.
 * <p>Using Reflections you can query for example:
 * <ul>
 *   <li> Subtypes of a type
 *   <li> Types annotated with an annotation
 *   <li> Methods with annotation, parameters, return type
 *   <li> Resources found in classpath
 * <br>And more...
 * </ul>
 *
 * <p>Create Reflections instance, preferably using {@link ConfigurationBuilder}:
 * <pre>{@code
 * Reflections reflections = new Reflections(
 *   new ConfigurationBuilder()
 *     .forPackage("com.my.project"));
 *
 * // or similarly
 * Reflections reflections = new Reflections("com.my.project");
 *
 * // another example
 * Reflections reflections = new Reflections(
 *   new ConfigurationBuilder()
 *     .forPackage("com.my.project")
 *     .setScanners(Scanners.values())     // all standard scanners
 *     .filterInputsBy(new FilterBuilder().includePackage("com.my.project").excludePackage("com.my.project.exclude")));
 * }</pre>
 *
 * <p>All relevant URLs should be configured.
 * <br>If required, Reflections will {@link #expandSuperTypes(Map, Map)} in order to get the transitive closure metadata without scanning large 3rd party urls.
 * <p>{@link Scanners} must be configured in order to be queried, otherwise an empty result is returned.
 * <br>Default scanners are {@code SubTypes} and {@code TypesAnnotated}.
 * For all standard scanners use {@code Scanners.values()}.
 * <p>Classloader can optionally be used for resolving runtime classes from names.
 *
 * <p></p>Query using {@link Reflections#get(QueryFunction)}, such as:
 * <pre>{@code
 * Set<Class<?>> modules = reflections.get(SubTypes.of(Module.class).asClass());
 * Set<Class<?>> singletons = reflections.get(TypesAnnotated.with(Singleton.class).asClass());
 * Set<String> properties   = reflections.get(Resources.with(".*\\.properties"));
 * Set<Method> requests     = reflections.get(MethodsAnnotated.with(RequestMapping.class).as(Method.class));
 * Set<Method> voidMethods  = reflections.get(MethodsReturn.with(void.class).as(Method.class));
 * Set<Method> someMethods  = reflections.get(MethodsSignature.of(long.class, int.class).as(Method.class));
 * }</pre>
 *
 * If not using {@code asClass()} or {@code as()} query results are strings, such that:
 * <pre>{@code Set<String> modules    = reflections.get(SubTypes.of(Module.class));
 * Set<String> singletons = reflections.get(TypesAnnotated.with(Singleton.class));
 * }</pre>
 * <p><i>Note that previous 0.9.x API is still supported, for example:</i>
 * <pre>{@code Set<Class<? extends Module>> modules = reflections.getSubTypesOf(Module.class);
 * Set<Class<?>> singletons = reflections.getTypesAnnotatedWith(Singleton.class);
 * }</pre>
 * <p>Queries can combine {@link Scanners} and {@link ReflectionUtils} functions, and compose fluent functional methods from {@link QueryFunction}.
 * <pre>{@code }</pre>
 * <p>Scanned metadata can be saved using {@link #save(String)}, and collected using {@link #collect(String, java.util.function.Predicate, org.reflections.serializers.Serializer)}
 * <p></p>
 * <i>For Javadoc, source code, and more information about Reflections Library, see http://github.com/ronmamo/reflections/</i>
 */
public class Reflections implements NameHelper {
    public final static Logger log = LoggerFactory.getLogger(Reflections.class);

    protected final transient Configuration configuration;
    protected final Store store;

    /**
     * constructs Reflections instance and scan according to the given {@link org.reflections.Configuration}
     * <p>it is preferred to use {@link org.reflections.util.ConfigurationBuilder} <pre>{@code new Reflections(new ConfigurationBuilder()...)}</pre>
     */
    public Reflections(Configuration configuration) {
        this.configuration = configuration;
        Map<String, Map<String, Set<String>>> storeMap = scan();
        if (configuration.shouldExpandSuperTypes()) {
            expandSuperTypes(storeMap.get(SubTypes.index()), storeMap.get(TypesAnnotated.index()));
        }
        store = new Store(storeMap);
    }

    public Reflections(Store store) {
        this.configuration = new ConfigurationBuilder();
        this.store = store;
    }

    /**
     * constructs Reflections instance and scan according to the given package {@code prefix} and optional {@code scanners}
     * <pre>{@code new Reflections("org.reflections")}</pre>
     * <p>it is preferred to use {@link org.reflections.util.ConfigurationBuilder} instead, this is actually similar to:
     * <pre>{@code new Reflections(
     *   new ConfigurationBuilder()
     *     .forPackage(prefix)
     *     .setScanners(scanners))
     * }</pre>
     * <p>uses {@link org.reflections.util.ClasspathHelper#forPackage(String, ClassLoader...)} to resolve urls from given {@code prefix}
     * <p>optional {@code scanners} defaults to {@link Scanners#TypesAnnotated} and {@link Scanners#SubTypes}
     */
    public Reflections(String prefix, Scanner... scanners) {
        this((Object) prefix, scanners);
    }

    /**
     * Convenient constructor for Reflections.
     * <p></p>see the javadoc of {@link ConfigurationBuilder#build(Object...)} for details.
     * <p></p><i>it is preferred to use {@link org.reflections.util.ConfigurationBuilder} instead.</i> */
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
            .collect(Collectors.toMap(s -> s, s -> Collections.synchronizedSet(new HashSet<>())));
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
                                    if (entries != null) collect.get(scanner.index()).addAll(entries);
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

    private boolean doFilter(Vfs.File file, @Nullable Predicate<String> predicate) {
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
     * <i>prefer using a designated directory (for example META-INF/reflections but not just META-INF), so that collect can work much faster</i>
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
     * <i>prefer using a designated directory (for example META-INF/reflections but not just META-INF), so that collect can work much faster</i>
     */
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

    /**
     * deserialize and merge saved Reflections metadata from the given {@code inputStream} and {@code serializer}
     * <p><i>useful if you know the serialized resource location and prefer not to look it up the classpath</i>
     */
    public Reflections collect(InputStream inputStream, Serializer serializer) {
        return merge(serializer.read(inputStream));
    }

    /**
     * deserialize and merge saved Reflections metadata from the given {@code file} and {@code serializer}
     * <p><i>useful if you know the serialized resource location and prefer not to look it up the classpath</i>
     */
    public Reflections collect(File file, Serializer serializer) {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            return collect(inputStream, serializer);
        } catch (IOException e) {
            throw new ReflectionsException("could not obtain input stream from file " + file, e);
        }
    }

    /** merges the given {@code reflections} instance metadata into this instance */
    public Reflections merge(Reflections reflections) {
        reflections.store.forEach((index, map) -> this.store.merge(index, map, (m1, m2) -> {
            m2.forEach((k, v) -> m1.merge(k, v, (s1, s2) -> { s1.addAll(s2); return s1;}));
            return m1;
        }));
        return this;
    }

    /**
     * expand super types after scanning, for super types that were not scanned.
     * <br>this is helpful in finding the transitive closure without scanning all 3rd party dependencies.
     * <p></p>
     * for example, for classes A,B,C where A supertype of B, B supertype of C (A -> B -> C):
     * <ul>
     *     <li>if scanning C resulted in B (B->C in store), but A was not scanned (although A is a supertype of B) - then getSubTypes(A) will not return C</li>
     *     <li>if expanding supertypes, B will be expanded with A (A->B in store) - then getSubTypes(A) will return C</li>
     * </ul>
     */
    public void expandSuperTypes(Map<String, Set<String>> subTypesStore, Map<String, Set<String>> typesAnnotatedStore) {
        if (subTypesStore == null || subTypesStore.isEmpty()) return;
        Set<String> keys = new LinkedHashSet<>(subTypesStore.keySet());
        keys.removeAll(subTypesStore.values().stream().flatMap(Collection::stream).collect(Collectors.toSet()));
        keys.remove("java.lang.Object");
        for (String key : keys) {
            Class<?> type = forClass(key, loaders());
            if (type != null) {
                expandSupertypes(subTypesStore, typesAnnotatedStore, key, type);
            }
        }
    }

    private void expandSupertypes(Map<String, Set<String>> subTypesStore,
              Map<String, Set<String>> typesAnnotatedStore, String key, Class<?> type) {
        Set<Annotation> typeAnnotations = ReflectionUtils.getAnnotations(type);
        if (typesAnnotatedStore != null && !typeAnnotations.isEmpty()) {
            String typeName = type.getName();
            for (Annotation typeAnnotation : typeAnnotations) {
                String annotationName = typeAnnotation.annotationType().getName();
                typesAnnotatedStore.computeIfAbsent(annotationName, s -> new HashSet<>()).add(typeName);
            }
        }
        for (Class<?> supertype : ReflectionUtils.getSuperTypes(type)) {
            String supertypeName = supertype.getName();
            if (subTypesStore.containsKey(supertypeName)) {
                subTypesStore.get(supertypeName).add(key);
            } else {
                subTypesStore.computeIfAbsent(supertypeName, s -> new HashSet<>()).add(key);
                expandSupertypes(subTypesStore, typesAnnotatedStore, supertypeName, supertype);
            }
        }
    }

    /**
     * apply {@link QueryFunction} on {@link Store}
     * <pre>{@code Set<T> ts = get(query)}</pre>
     * <p>use {@link Scanners} and {@link ReflectionUtils} query functions, such as:
     * <pre>{@code
     * Set<String> annotated = get(Scanners.TypesAnnotated.with(A.class))
     * Set<Class<?>> subtypes = get(Scanners.SubTypes.of(B.class).asClass())
     * Set<Method> methods = get(ReflectionUtils.Methods.of(B.class))
     * }</pre>
     */
    public <T> Set<T> get(QueryFunction<Store, T> query) {
        return query.apply(store);
    }

    /**
     * gets all subtypes in hierarchy of a given {@code type}.
     * <p>similar to {@code get(SubTypes.of(type))}
     * <p></p><i>depends on {@link Scanners#SubTypes} configured</i>
     */
    public <T> Set<Class<? extends T>> getSubTypesOf(Class<T> type) {
        //noinspection unchecked
        return (Set<Class<? extends T>>) get(SubTypes.of(type)
            .as((Class<? extends T>) Class.class, loaders()));
    }

    /**
     * get types annotated with the given {@code annotation}, both classes and annotations
     * <p>{@link java.lang.annotation.Inherited} is not honored by default, see {@link #getTypesAnnotatedWith(Class, boolean)}.
     * <p>similar to {@code get(SubTypes.of(TypesAnnotated.with(annotation)))}
     * <p></p><i>depends on {@link Scanners#TypesAnnotated} and {@link Scanners#SubTypes} configured</i>
     */
    public Set<Class<?>> getTypesAnnotatedWith(Class<? extends Annotation> annotation) {
        return get(SubTypes.of(TypesAnnotated.with(annotation)).asClass(loaders()));
    }

    /**
     * get types annotated with the given {@code annotation}, both classes and annotations
     * <p>{@link java.lang.annotation.Inherited} is honored according to the given {@code honorInherited}.
     * <p>when honoring @Inherited, meta-annotation should only effect annotated super classes and subtypes
     * <p>when not honoring @Inherited, meta annotation effects all subtypes, including annotations interfaces and classes
     * <p><i>Note that this (@Inherited) meta-annotation type has no effect if the annotated type is used for anything other then a class.
     * Also, this meta-annotation causes annotations to be inherited only from superclasses; annotations on implemented interfaces have no effect.</i>
     * <p></p><i>depends on {@link Scanners#TypesAnnotated} and {@link Scanners#SubTypes} configured</i>
     */
    public Set<Class<?>> getTypesAnnotatedWith(Class<? extends Annotation> annotation, boolean honorInherited) {
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
     * get types annotated with the given {@code annotation}, both classes and annotations, including annotation member values matching
     * <p>{@link java.lang.annotation.Inherited} is not honored by default, see {@link #getTypesAnnotatedWith(Annotation, boolean)}.
     * <p></p><i>depends on {@link Scanners#TypesAnnotated} and {@link Scanners#SubTypes} configured</i>
     */
    public Set<Class<?>> getTypesAnnotatedWith(Annotation annotation) {
        return get(SubTypes.of(
                TypesAnnotated.of(TypesAnnotated.get(annotation.annotationType())
                    .filter(c -> withAnnotation(annotation).test(forClass(c, loaders())))))
            .asClass(loaders()));
    }

    /**
     * get types annotated with the given {@code annotation}, both classes and annotations, including annotation member values matching
     * <p>{@link java.lang.annotation.Inherited} is honored according to given honorInherited
     * <p></p><i>depends on {@link Scanners#TypesAnnotated} and {@link Scanners#SubTypes} configured</i>
     */
    public Set<Class<?>> getTypesAnnotatedWith(Annotation annotation, boolean honorInherited) {
        if (!honorInherited) {
            return getTypesAnnotatedWith(annotation);
        } else {
            Class<? extends Annotation> type = annotation.annotationType();
            if (type.isAnnotationPresent(Inherited.class)) {
                return get(TypesAnnotated.with(type).asClass(loaders()).filter(withAnnotation(annotation))
                    .add(SubTypes.of(TypesAnnotated.with(type).asClass(loaders()).filter(c -> !c.isInterface()))));
            } else {
                return get(TypesAnnotated.with(type).asClass(loaders()).filter(withAnnotation(annotation)));
            }
        }
    }

    /**
     * get methods annotated with the given {@code annotation}
     * <p>similar to {@code get(MethodsAnnotated.with(annotation))}
     * <p></p><i>depends on {@link Scanners#MethodsAnnotated} configured</i>
     */
    public Set<Method> getMethodsAnnotatedWith(Class<? extends Annotation> annotation) {
        return get(MethodsAnnotated.with(annotation).as(Method.class, loaders()));
    }

    /**
     * get methods annotated with the given {@code annotation}, including annotation member values matching
     * <p>similar to {@code get(MethodsAnnotated.with(annotation))}
     * <p></p><i>depends on {@link Scanners#MethodsAnnotated} configured</i>
     */
    public Set<Method> getMethodsAnnotatedWith(Annotation annotation) {
        return get(MethodsAnnotated.with(annotation.annotationType()).as(Method.class, loaders())
            .filter(withAnnotation(annotation)));
    }

    /**
     * get methods with signature matching the given {@code types}
     * <p>similar to {@code get(MethodsSignature.of(types))}
     * <p></p><i>depends on {@link Scanners#MethodsSignature} configured</i>
     */
    public Set<Method> getMethodsWithSignature(Class<?>... types) {
        return get(MethodsSignature.with(types).as(Method.class, loaders()));
    }

    /**
     * get methods with any parameter matching the given {@code type}, either class or annotation
     * <p>similar to {@code get(MethodsParameter.with(type))}
     * <p></p><i>depends on {@link Scanners#MethodsParameter} configured</i>
     */
    public Set<Method> getMethodsWithParameter(AnnotatedElement type) {
        return get(MethodsParameter.with(type).as(Method.class, loaders()));
    }

    /**
     * get methods with return type matching the given {@code returnType}
     * <p>similar to {@code get(MethodsReturn.of(type))}
     * <p></p><i>depends on {@link Scanners#MethodsParameter} configured</i>
     */
    public Set<Method> getMethodsReturn(Class<?> type) {
        return get(MethodsReturn.of(type).as(Method.class, loaders()));
    }

    /**
     * get constructors annotated with the given {@code annotation}
     * <p>similar to {@code get(ConstructorsAnnotated.with(annotation))}
     * <p></p><i>depends on {@link Scanners#ConstructorsAnnotated} configured</i>
     */
    public Set<Constructor> getConstructorsAnnotatedWith(Class<? extends Annotation> annotation) {
        return get(ConstructorsAnnotated.with(annotation).as(Constructor.class, loaders()));
    }

    /**
     * get constructors annotated with the given {@code annotation}, including annotation member values matching
     * <p>similar to {@code get(ConstructorsAnnotated.with(annotation))}
     * <p></p><i>depends on {@link Scanners#ConstructorsAnnotated} configured</i>
     */
    public Set<Constructor> getConstructorsAnnotatedWith(Annotation annotation) {
        return get(ConstructorsAnnotated.with(annotation.annotationType()).as(Constructor.class, loaders())
            .filter(withAnyParameterAnnotation(annotation)));
    }

    /**
     * get constructors with signature matching the given {@code types}
     * <p>similar to {@code get(ConstructorsSignature.with(types))}
     * <p></p><i>depends on {@link Scanners#ConstructorsSignature} configured</i>
     */
    public Set<Constructor> getConstructorsWithSignature(Class<?>... types) {
        return get(ConstructorsSignature.with(types).as(Constructor.class, loaders()));
    }

    /**
     * get constructors with any parameter matching the given {@code type}, either class or annotation
     * <p>similar to {@code get(ConstructorsParameter.with(types))}
     * <p></p><i>depends on {@link Scanners#ConstructorsParameter} configured</i>
     */
    public Set<Constructor> getConstructorsWithParameter(AnnotatedElement type) {
        return get(ConstructorsParameter.of(type).as(Constructor.class, loaders()));
    }

    /**
     * get fields annotated with the given {@code annotation}
     * <p>similar to {@code get(FieldsAnnotated.with(annotation))}
     * <p></p><i>depends on {@link Scanners#FieldsAnnotated} configured</i>
     */
    public Set<Field> getFieldsAnnotatedWith(Class<? extends Annotation> annotation) {
        return get(FieldsAnnotated.with(annotation).as(Field.class, loaders()));
    }

    /**
     * get fields annotated with the given {@code annotation}, including annotation member values matching
     * <p>similar to {@code get(FieldsAnnotated.with(annotation))}
     * <p></p><i>depends on {@link Scanners#FieldsAnnotated} configured</i>
     */
    public Set<Field> getFieldsAnnotatedWith(Annotation annotation) {
        return get(FieldsAnnotated.with(annotation.annotationType()).as(Field.class, loaders())
            .filter(withAnnotation(annotation)));
    }

    /**
     * get resources matching the given {@code pattern} regex <pre>{@code Set<String> xmls = reflections.getResources(".*\\.xml")}</pre>
     * <p>similar to {@code get(Resources.with(pattern))}
     * <p></p><i>depends on {@link Scanners#Resources} configured</i>
     */
    public Set<String> getResources(String pattern) {
        return get(Resources.with(pattern));
    }

    /**
     * get resources matching the given {@code pattern} regex <pre>{@code Set<String> xmls = reflections.getResources(Pattern.compile(".*\\.xml"))}</pre>
     * <p>similar to {@code get(Resources.with(pattern))}
     * <p></p><i>depends on {@link Scanners#Resources} configured</i>
     */
    public Set<String> getResources(Pattern pattern) {
        return getResources(pattern.pattern());
    }

    /**
     * get parameter names of the given {@code member}, either method or constructor
     * <p>depends on {@link MethodParameterNamesScanner} configured
     */
    public List<String> getMemberParameterNames(Member member) {
        return store.getOrDefault(MethodParameterNamesScanner.class.getSimpleName(), Collections.emptyMap()).getOrDefault(toName((AnnotatedElement) member), Collections.emptySet())
            .stream().flatMap(s -> Stream.of(s.split(", "))).collect(Collectors.toList());
    }

    /**
     * get code usages for the given {@code member}, either field, method or constructor
     * <p>depends on {@link MemberUsageScanner} configured
     */
    public Collection<Member> getMemberUsage(Member member) {
        Set<String> usages = store.getOrDefault(MemberUsageScanner.class.getSimpleName(), Collections.emptyMap()).getOrDefault(toName((AnnotatedElement) member), Collections.emptySet());
        return forNames(usages, Member.class, loaders());
    }

    /**
     * returns all keys and values scanned by {@link Scanners#SubTypes} scanner
     * <p><i>using this api is discouraged, it is better to get elements by specific criteria such as {@code SubTypes.of(Class)} or {@code TypesAnnotated.with(Class)} </i>
     * <p></p><i>deprecated, use {@link #getAll(Scanner)} instead</i>
     */
    @Deprecated
    public Set<String> getAllTypes() {
        return getAll(SubTypes);
    }

    /**
     * returns all key and values scanned by the given {@code scanner} <pre>{@code Set<String> all = reflections.getAll(SubTypes)}</pre>
     * <p><i>using this is discouraged, it is better to get elements by specific criteria such as {@code SubTypes.of(Class)} or {@code TypesAnnotated.with(Class)} </i>
     */
    public Set<String> getAll(Scanner scanner) {
        Map<String, Set<String>> map = store.getOrDefault(scanner.index(), Collections.emptyMap());
        return Stream.concat(map.keySet().stream(), map.values().stream().flatMap(Collection::stream)).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * returns the {@link org.reflections.Store} object used for storing and querying the metadata
     * <p>{@code Store} is basically {@code Map<String, Map<String, Set<String>>>}
     */
    public Store getStore() {
        return store;
    }

    /** returns the {@link org.reflections.Configuration} object of this instance */
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * serialize metadata to the given {@code filename}
     * <p></p><i>prefer using a designated directory (for example META-INF/reflections but not just META-INF), so that {@link Reflections#collect(String, Predicate)} can work much faster</i>
     */
    public File save(String filename) {
        return save(filename, new XmlSerializer());
    }

    /**
     * serialize metadata to the given {@code filename} and {@code serializer}
     * <p></p><i>prefer using a designated directory (for example META-INF/reflections but not just META-INF), so that {@link Reflections#collect(String, Predicate, Serializer)} can work much faster</i>
     */
    public File save(String filename, Serializer serializer) {
        return serializer.save(this, filename);
    }

    ClassLoader[] loaders() { return configuration.getClassLoaders(); }
}
