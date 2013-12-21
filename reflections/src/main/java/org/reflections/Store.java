package org.reflections;

import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.*;
import org.reflections.scanners.*;
import org.reflections.scanners.Scanner;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.Utils;

import javax.annotation.Nullable;
import java.lang.annotation.Inherited;
import java.util.*;
import java.util.regex.Pattern;

import static com.google.common.base.Predicates.*;
import static com.google.common.collect.Multimaps.*;

/**
 * stores metadata information in multimaps
 * <p>use the different query methods (getXXX) to query the metadata
 * <p>the query methods are string based, and does not cause the class loader to define the types
 * <p>use {@link org.reflections.Reflections#getStore()} to access this store
 */
public class Store {

	private final Map<String/*indexName*/, Multimap<String, String>> storeMap;
    private final transient boolean concurrent;
    private final transient Configuration configuration;

    //used via reflection
    @SuppressWarnings("UnusedDeclaration")
    protected Store() {
        this(new ConfigurationBuilder());
    }

    public Store(Configuration configuration) {
        this.configuration = configuration;
        concurrent = configuration.getExecutorService() != null;
        storeMap = new HashMap<String, Multimap<String, String>>();
    }

    protected ListMultimap<String, String> createMultimap() {
        ListMultimap<String, String> multimap = newListMultimap(new HashMap<String, Collection<String>>(), listSupplier);
        return concurrent ? synchronizedListMultimap(multimap) : multimap;
    }

    public Multimap<String, String> getOrCreate(String indexName) {
        if (indexName.contains(".")) {
            indexName = indexName.substring(indexName.lastIndexOf(".") + 1); //convert class name to simple name
        }
        Multimap<String, String> mmap = storeMap.get(indexName);
        if (mmap == null) {
            storeMap.put(indexName, mmap = createMultimap());
        }
        return mmap;
    }

    /** return the multimap store of the given scanner class. not immutable */
    @Nullable public Multimap<String, String> get(Class<? extends Scanner> scannerClass) {
        return storeMap.get(scannerClass.getSimpleName());
    }

    /** get the values of given keys stored for the given scanner class */
    public Set<String> get(Class<? extends Scanner> scannerClass, String... keys) {
        Set<String> result = Sets.newHashSet();

        Multimap<String, String> map = get(scannerClass);
        if (map != null) {
            for (String key : keys) {
                result.addAll(map.get(key));
            }
        }

        return result;
    }

    /** get the values of given keys stored for the given scanner class */
    public Set<String> get(Class<? extends Scanner> scannerClass, Iterable<String> keys) {
        Set<String> result = Sets.newHashSet();

        Multimap<String, String> map = get(scannerClass);
        if (map != null) {
            for (String key : keys) {
                result.addAll(map.get(key));
            }
        }

        return result;
    }

    /** return the store map. not immutable*/
    public Map<String, Multimap<String, String>> getStoreMap() {
        return storeMap;
    }

    /** merges given store into this */
    void merge(final Store outer) {
        if (outer != null) {
            for (String indexName : outer.storeMap.keySet()) {
                getOrCreate(indexName).putAll(outer.storeMap.get(indexName));
            }
        }
    }

    /** return the keys count */
    public Integer getKeysCount() {
        Integer keys = 0;
        for (Multimap<String, String> multimap : storeMap.values()) {
            keys += multimap.keySet().size();
        }
        return keys;
    }

    /** return the values count */
    public Integer getValuesCount() {
        Integer values = 0;
        for (Multimap<String, String> multimap : storeMap.values()) {
            values += multimap.size();
        }
        return values;
    }

    //query
    /** get sub types of a given type */
    public Set<String> getSubTypesOf(final String type) {
        Set<String> result = new HashSet<String>();

        Set<String> subTypes = get(SubTypesScanner.class, type);
        result.addAll(subTypes);

        for (String subType : subTypes) {
            result.addAll(getSubTypesOf(subType));
        }

        return result;
    }

    /**
     * get types directly annotated with a given annotation, both classes and annotations
     */
    public Set<String> getTypesAnnotatedWithDirectly(final String annotation) {
        return get(TypeAnnotationsScanner.class, annotation);
    }

    /**
     * get types annotated with a given annotation, both classes and annotations
     * <p>{@link java.lang.annotation.Inherited} is honored
     * <p><i>Note that this (@Inherited) meta-annotation type has no effect if the annotated type is used for anything other than a class.
     * Also, this meta-annotation causes annotations to be inherited only from superclasses; annotations on implemented interfaces have no effect.</i>
     */
    public Set<String> getTypesAnnotatedWith(final String annotation) {
        return getTypesAnnotatedWith(annotation, true);
    }

    /**
     * get types annotated with a given annotation, both classes and annotations
     * <p>{@link java.lang.annotation.Inherited} is honored according to given honorInherited
     * <p>when honoring @Inherited, meta-annotation should only effect annotated super classes and it's sub types
     * <p>when not honoring @Inherited, meta annotation effects all subtypes, including annotations interfaces and classes
     * <p><i>Note that this (@Inherited) meta-annotation type has no effect if the annotated type is used for anything other than a class.
     * Also, this meta-annotation causes annotations to be inherited only from superclasses; annotations on implemented interfaces have no effect.</i>
     */
    public Set<String> getTypesAnnotatedWith(final String annotation, boolean honorInherited) {
        final Set<String> result = new HashSet<String>();

        if (isAnnotation(annotation)) {
            final Set<String> types = getTypesAnnotatedWithDirectly(annotation);
            Set<String> inherited = getInheritedSubTypes(types, annotation, honorInherited);
            result.addAll(inherited);
        }
        return result;
    }

    /**
     *
     * <p>when honoring @Inherited, meta-annotation should only effect annotated super classes and it's sub types
     * <p>when not honoring @Inherited, meta annotation effects all subtypes, including annotations interfaces and classes
     * */
    public Set<String> getInheritedSubTypes(Iterable<String> types, String annotation, boolean honorInherited) {
        Set<String> result = Sets.newHashSet(types);

        if (honorInherited && isInheritedAnnotation(annotation)) {
            //when honoring @Inherited, meta-annotation should only effect annotated super classes and its sub types
            for (String type : types) {
                if (isClass(type)) {
                    result.addAll(getSubTypesOf(type));
                }
            }
        } else if (!honorInherited) {
            //when not honoring @Inherited, meta annotation effects all subtypes, including annotations interfaces and classes
            for (String type : types) {
                if (isAnnotation(type)) {
                    result.addAll(getTypesAnnotatedWith(type, false));
                } else {
                    result.addAll(getSubTypesOf(type));
                }
            }
        }

        return result;
    }

    /** get method names annotated with a given annotation */
    public Set<String> getMethodsAnnotatedWith(String annotation) {
        return Sets.filter(get(MethodAnnotationsScanner.class, annotation), not(isConstructor));
    }

    /** get fields annotated with a given annotation */
    public Set<String> getFieldsAnnotatedWith(String annotation) {
        return get(FieldAnnotationsScanner.class, annotation);
    }

    /** get constructor names annotated with a given annotation */
    public Set<String> getConstructorsAnnotatedWith(String annotation) {
        return Sets.filter(get(MethodAnnotationsScanner.class, annotation), isConstructor);
    }

    /** get resources relative paths where simple name (key) equals given name */
    public Set<String> getResources(final String key) {
        return get(ResourcesScanner.class, key);
    }

    /** get resources relative paths where simple name (key) matches given namePredicate */
    public Set<String> getResources(final Predicate<String> namePredicate) {
        Multimap<String, String> mmap = get(ResourcesScanner.class);
        if (mmap != null) {
            return get(ResourcesScanner.class, Collections2.filter(mmap.keySet(), namePredicate));
        } else {
            return Sets.newHashSet();
        }
    }

    /** get resources relative paths where simple name (key) matches given regular expression
     * <pre>Set&#60String> xmls = reflections.getResources(".*\\.xml");</pre>*/
    public Set<String> getResources(final Pattern pattern) {
        return getResources(new Predicate<String>() {
            public boolean apply(String input) {
                return pattern.matcher(input).matches();
            }
        });
    }

    //support
    /** is the given type name a class. <p>causes class loading */
    public boolean isClass(String type) {
        return !ReflectionUtils.forName(type, configuration.getClassLoaders()).isInterface();
    }

    /** is the given type is an annotation, based on the metadata stored by TypeAnnotationsScanner */
    public boolean isAnnotation(String typeAnnotatedWith) {
        Multimap<String, String> mmap = get(TypeAnnotationsScanner.class);
        return mmap != null && mmap.keySet().contains(typeAnnotatedWith);
    }

    /** is the given annotation an inherited annotation, based on the metadata stored by TypeAnnotationsScanner */
    public boolean isInheritedAnnotation(String typeAnnotatedWith) {
        Multimap<String, String> mmap = get(TypeAnnotationsScanner.class);
        return mmap != null && mmap.get(Inherited.class.getName()).contains(typeAnnotatedWith);
    }

    //
    private final static transient Supplier<List<String>> listSupplier = new Supplier<List<String>>() {
        public List<String> get() {
            return Lists.newArrayList();
        }
    };

    private static final Predicate<String> isConstructor = new Predicate<String>() {
        public boolean apply(@Nullable String input) {
            return Utils.isConstructor(input);
        }
    };
}
