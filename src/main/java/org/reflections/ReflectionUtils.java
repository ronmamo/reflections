package org.reflections;

import org.reflections.util.QueryFunction;
import org.reflections.util.ReflectionUtilsPredicates;
import org.reflections.util.UtilQueryBuilder;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * utils for querying java reflection meta types
 * <p>see {@link #SuperTypes}, {@link #Annotations}, {@link #AnnotationTypes}, {@link #Methods}, {@link #Constructors} and {@link #Fields}.
 * <pre>{@code
 * Set<Class<?>> supertypes = get(SuperTypes.of(type))
 * Set<Annotation> annotations = get(Annotations.of(type))
 * }</pre>
 * <p>generally, apply {@link #get(QueryFunction)} on {@link QueryFunction} created by {@link UtilQueryBuilder}, and optionally use the functional methods in QueryFunction.
 * <pre>{@code get(Methods.of(type)
 *   .filter(withPublic().and(withPrefix("get")).and(withParameterCount(0)))
 *   .as(Method.class)
 *   .map(m -> ...))
 * }</pre>
 * <p>or (previously), use {@code getAllXXX(type/s, withYYY)} methods:
 * <pre>{@code getAllSuperTypes(), getAllFields(), getAllMethods(), getAllConstructors() }
 * </pre>
 * <p>
 * some predicates included here:
 * <ul>
 * <li>{@link #withPublic()}
 * <li>{@link #withParametersCount(int)}}
 * <li>{@link #withAnnotation(java.lang.annotation.Annotation)}
 * <li>{@link #withParameters(Class[])}
 * <li>{@link #withModifier(int)}
 * <li>{@link #withReturnType(Class)}
 * </ul>
 * <pre>{@code
 * import static org.reflections.ReflectionUtils.*;
 *
 * Set<Method> getters =
 *     get(Methods(classes)
 *     .filter(withModifier(Modifier.PUBLIC).and(withPrefix("get")).and(withParametersCount(0)));
 *
 * get(Annotations.of(method)
 *   .filter(withAnnotation())
 *   .map(annotation -> Methods.of(annotation)
 *     .map(method -> )))))
 *   .stream()...
 * }</pre>
 * */
@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class ReflectionUtils extends ReflectionUtilsPredicates {

    /** get type elements {@code <T>} by applying {@link QueryFunction} <pre>{@code get(SuperTypes.of(type))}</pre> */
    public static <C, T> Set<T> get(QueryFunction<C, T> function) {
        return function.apply(null);
    }

    /** get type elements {@code <T>} by applying {@link QueryFunction} and {@code predicates} */
    public static <T> Set<T> get(QueryFunction<Store, T> queryFunction, Predicate<? super T>... predicates) {
        return get(queryFunction.filter(Arrays.stream((Predicate[]) predicates).reduce(t -> true, Predicate::and)));
    }

    private static final List<String> objectMethodNames =
        Arrays.asList("equals", "hashCode", "toString", "wait", "notify", "notifyAll");

    /** predicate to filter out {@code Object} methods */
    public static final Predicate<Method> notObjectMethod = m -> !objectMethodNames.contains(m.getName());

    /** query super class <pre>{@code get(SuperClass.of(element)) -> Set<Class<?>>}</pre>
     * <p>see also {@link ReflectionUtils#SuperTypes}, {@link ReflectionUtils#Interfaces} */
    public static final UtilQueryBuilder<Class<?>, Class<?>> SuperClass =
        element -> ctx -> {
            Class<?> superclass = element.getSuperclass();
            return superclass != null && !superclass.equals(Object.class) ? Collections.singleton(superclass) : Collections.emptySet();
        };

    /** query interfaces <pre>{@code get(Interfaces.of(element)) -> Set<Class<?>>}</pre> */
    public static final UtilQueryBuilder<Class<?>, Class<?>> Interfaces =
        element -> ctx -> Stream.of(element.getInterfaces()).collect(Collectors.toCollection(LinkedHashSet::new));

    /** query super classes and interfaces including element <pre>{@code get(SuperTypes.of(element)) -> Set<Class<?>> }</pre> */
    public static final UtilQueryBuilder<Class<?>, Class<?>> SuperTypes =
        new UtilQueryBuilder<Class<?>, Class<?>>() {
            @Override
            public QueryFunction<Store, Class<?>> get(Class<?> element) {
                return SuperClass.get(element).add(Interfaces.get(element));
            }

            @Override
            public QueryFunction<Store, Class<?>> of(Class<?> element) {
                return QueryFunction.<Store, Class<?>>single(element).getAll(SuperTypes::get);
            }
        };

    /** query annotations <pre>{@code get(Annotation.of(element)) -> Set<Annotation> }</pre> */
    public static final UtilQueryBuilder<AnnotatedElement, Annotation> Annotations =
        new UtilQueryBuilder<AnnotatedElement, Annotation>() {
            @Override
            public QueryFunction<Store, Annotation> get(AnnotatedElement element) {
                return ctx -> Arrays.stream(element.getAnnotations()).collect(Collectors.toCollection(LinkedHashSet::new));
            }

            @Override
            public QueryFunction<Store, Annotation> of(AnnotatedElement element) {
                return ReflectionUtils.extendType().get(element).getAll(Annotations::get, Annotation::annotationType);
            }
        };

    /** query annotation types <pre>{@code get(AnnotationTypes.of(element)) -> Set<Class<? extends Annotation>> }</pre> */
    public static final UtilQueryBuilder<AnnotatedElement, Class<? extends Annotation>> AnnotationTypes =
        new UtilQueryBuilder<AnnotatedElement, Class<? extends Annotation>>() {
            @Override
            public QueryFunction<Store, Class<? extends Annotation>> get(AnnotatedElement element) {
                return Annotations.get(element).map(Annotation::annotationType);
            }

            @Override
            public QueryFunction<Store, Class<? extends Annotation>> of(AnnotatedElement element) {
                return ReflectionUtils.extendType().get(element).getAll(AnnotationTypes::get, a -> a);
            }
        };

    /** query methods <pre>{@code get(Methods.of(type)) -> Set<Method>}</pre> */
    public static final UtilQueryBuilder<Class<?>, Method> Methods =
        element -> ctx -> Arrays.stream(element.getMethods()).filter(notObjectMethod).collect(Collectors.toCollection(LinkedHashSet::new));

    /** query constructors <pre>{@code get(Constructors.of(type)) -> Set<Constructor> }</pre> */
    public static final UtilQueryBuilder<Class<?>, Constructor> Constructors =
        element -> ctx -> Arrays.<Constructor>stream(element.getDeclaredConstructors()).collect(Collectors.toCollection(LinkedHashSet::new));

    /** query fields <pre>{@code get(Fields.of(type)) -> Set<Field> }</pre> */
    public static final UtilQueryBuilder<Class<?>, Field> Fields =
        element -> ctx -> Arrays.stream(element.getDeclaredFields()).collect(Collectors.toCollection(LinkedHashSet::new));

    public static <T extends AnnotatedElement> UtilQueryBuilder<AnnotatedElement, T> extendType() {
        return element -> {
            if (element instanceof Class && !((Class<?>) element).isAnnotation()) {
                QueryFunction<Store, Class<?>> single = QueryFunction.single((Class<?>) element);
                return (QueryFunction<Store, T>) single.add(single.getAll(SuperTypes::get));
            } else {
                return QueryFunction.single((T) element);
            }
        };
    }

    /** get all annotations of given {@code type}, up the super class hierarchy, optionally filtered by {@code predicates}
     * <p>marked for removal, use instead {@code get(Annotations.of())} */
    public static <T extends AnnotatedElement> Set<Annotation> getAllAnnotations(T type, Predicate<Annotation>... predicates) {
        return get(Annotations.of(type), predicates);
    }

    /** get all super types of given {@code type}, including, optionally filtered by {@code predicates} */
    public static Set<Class<?>> getAllSuperTypes(final Class<?> type, Predicate<? super Class<?>>... predicates) {
        Predicate<? super Class<?>>[] filter = predicates == null || predicates.length == 0 ? new Predicate[]{t -> !Object.class.equals(t)} : predicates;
        return get(SuperTypes.of(type), filter);
    }

    /** get the immediate supertype and interfaces of the given {@code type}
     * <p>marked for removal, use instead {@code get(SuperTypes.get())} */
    public static Set<Class<?>> getSuperTypes(Class<?> type) {
        return get(SuperTypes.get(type));
    }

    /** get all methods of given {@code type}, up the super class hierarchy, optionally filtered by {@code predicates}
     * <p>marked for removal, use instead {@code get(Methods.of())} */
    public static Set<Method> getAllMethods(final Class<?> type, Predicate<? super Method>... predicates) {
        return get(Methods.of(type), predicates);
    }

    /** get methods of given {@code type}, optionally filtered by {@code predicates}
     * <p>marked for removal, use instead {@code get(Methods.get())} */
    public static Set<Method> getMethods(Class<?> t, Predicate<? super Method>... predicates) {
        return get(Methods.get(t), predicates);
    }

    /** get all constructors of given {@code type}, up the super class hierarchy, optionally filtered by {@code predicates}
     * <p>marked for removal, use instead {@code get(Constructors.of())} */
    public static Set<Constructor> getAllConstructors(final Class<?> type, Predicate<? super Constructor>... predicates) {
        return get(Constructors.of(type), predicates);
    }

    /** get constructors of given {@code type}, optionally filtered by {@code predicates}
     * <p>marked for removal, use instead {@code get(Constructors.get())} */
    public static Set<Constructor> getConstructors(Class<?> t, Predicate<? super Constructor>... predicates) {
        return get(Constructors.get(t), predicates);
    }

    /** get all fields of given {@code type}, up the super class hierarchy, optionally filtered by {@code predicates}
     * <p>marked for removal, use instead {@code get(Fields.of())} */
    public static Set<Field> getAllFields(final Class<?> type, Predicate<? super Field>... predicates) {
        return get(Fields.of(type), predicates);
    }

    /** get fields of given {@code type}, optionally filtered by {@code predicates}
     * <p>marked for removal, use instead {@code get(Fields.get())} */
    public static Set<Field> getFields(Class<?> type, Predicate<? super Field>... predicates) {
        return get(Fields.get(type), predicates);
    }

    /** get annotations of given {@code type}, optionally honorInherited, optionally filtered by {@code predicates}
     * <p>marked for removal, use instead {@code get(Annotations.get())} */
    public static <T extends AnnotatedElement> Set<Annotation> getAnnotations(T type, Predicate<Annotation>... predicates) {
        return get(Annotations.get(type), predicates);
    }

    /** map {@code annotation} to hash map of member values recursively <pre>{@code Annotations.of(type).map(ReflectionUtils::toMap)} </pre>*/
    public static Map<String, Object> toMap(Annotation annotation) {
        return get(Methods.of(annotation.annotationType())
            .filter(notObjectMethod.and(withParametersCount(0))))
            .stream()
            .collect(Collectors.toMap(Method::getName, m -> {
                Object v1 = invoke(m, annotation);
                return v1.getClass().isArray() && v1.getClass().getComponentType().isAnnotation() ?
                    Stream.of((Annotation[]) v1).map(ReflectionUtils::toMap).collect(toList()) : v1;
            }));
    }

    /** map {@code annotation} and {@code annotatedElement} to hash map of member values
     * <pre>{@code Annotations.of(type).map(a -> toMap(type, a))} </pre>*/
    public static Map<String, Object> toMap(Annotation annotation, AnnotatedElement element) {
        Map<String, Object> map = toMap(annotation);
        if (element != null) map.put("annotatedElement", element);
        return map;
    }

    /** create new annotation proxy with member values from the given {@code map} <pre>{@code toAnnotation(Map.of("annotationType", annotationType, "value", ""))}</pre> */
    public static Annotation toAnnotation(Map<String, Object> map) {
        return toAnnotation(map, (Class<? extends Annotation>) map.get("annotationType"));
    }

    /** create new annotation proxy with member values from the given {@code map} and member values from the given {@code map}
     * <pre>{@code toAnnotation(Map.of("value", ""), annotationType)}</pre> */
    public static <T extends Annotation> T toAnnotation(Map<String, Object> map, Class<T> annotationType) {
        return (T) Proxy.newProxyInstance(annotationType.getClassLoader(), new Class<?>[]{annotationType},
            (proxy, method, args) -> notObjectMethod.test(method) ? map.get(method.getName()) : method.invoke(map));
    }

    /** invoke the given {@code method} with {@code args}, return either the result or an exception if occurred */
    public static Object invoke(Method method, Object obj, Object... args) {
        try {
            return method.invoke(obj, args);
        } catch (Exception e) {
            return e;
        }
    }
}
