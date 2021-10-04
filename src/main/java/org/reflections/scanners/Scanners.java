package org.reflections.scanners;

import javassist.bytecode.ClassFile;
import org.reflections.Store;
import org.reflections.util.FilterBuilder;
import org.reflections.util.NameHelper;
import org.reflections.util.QueryBuilder;
import org.reflections.util.QueryFunction;
import org.reflections.vfs.Vfs;

import java.lang.annotation.Inherited;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.reflections.util.JavassistHelper.*;

/**
 * base Reflections {@link Scanner}s such as:
 * <ul>
 *   <li>{@link #SubTypes}</li>
 *   <li>{@link #TypesAnnotated}</li>
 *   <li>{@link #MethodsAnnotated}</li>
 *   <li>{@link #FieldsAnnotated}</li>
 *   <li>{@link #Resources}</li>
 *   <li>{@link #MethodsParameter}</li>
 *   <li>{@link #MethodsSignature}</li>
 *   <li>{@link #MethodsReturn}</li>
 * </ul>
 * <i>note that scanners must be configured in {@link org.reflections.Configuration} in order to be queried</i>
 * */
public enum Scanners implements Scanner, QueryBuilder, NameHelper {

    /** scan type superclasses and interfaces
     * <p></p>
     * <i>Note that {@code Object} class is excluded by default, in order to reduce store size.
     * <br>Use {@link #filterResultsBy(Predicate)} to change, for example {@code SubTypes.filterResultsBy(c -> true)}</i>
     * */
    SubTypes {
        /* Object class is excluded by default from subtypes indexing */
        { filterResultsBy(new FilterBuilder().excludePattern("java\\.lang\\.Object")); }

        @Override
        public void scan(ClassFile classFile, List<Map.Entry<String, String>> entries) {
            entries.add(entry(classFile.getSuperclass(), classFile.getName()));
            entries.addAll(entries(Arrays.asList(classFile.getInterfaces()), classFile.getName()));
        }
    },

    /** scan type annotations */
    TypesAnnotated {
        @Override
        public boolean acceptResult(String annotation) {
            return super.acceptResult(annotation) || annotation.equals(Inherited.class.getName());
        }

        @Override
        public void scan(ClassFile classFile, List<Map.Entry<String, String>> entries) {
            entries.addAll(entries(getAnnotations(classFile::getAttribute), classFile.getName()));
        }
    },

    /** scan method annotations */
    MethodsAnnotated {
        @Override
        public void scan(ClassFile classFile, List<Map.Entry<String, String>> entries) {
            getMethods(classFile).forEach(method ->
                entries.addAll(entries(getAnnotations(method::getAttribute), methodName(classFile, method))));
        }
    },

    /** scan constructor annotations */
    ConstructorsAnnotated {
        @Override
        public void scan(ClassFile classFile, List<Map.Entry<String, String>> entries) {
            getConstructors(classFile).forEach(constructor ->
                entries.addAll(entries(getAnnotations(constructor::getAttribute), methodName(classFile, constructor))));
        }
    },

    /** scan field annotations */
    FieldsAnnotated {
        @Override
        public void scan(ClassFile classFile, List<Map.Entry<String, String>> entries) {
            classFile.getFields().forEach(field ->
                entries.addAll(entries(getAnnotations(field::getAttribute), fieldName(classFile, field))));
        }
    },

    /** scan non .class files such as xml or properties files */
    Resources {
        @Override
        public boolean acceptsInput(String file) {
            return !file.endsWith(".class");
        }

        @Override
        public List<Map.Entry<String, String>> scan(Vfs.File file) {
            return Collections.singletonList(entry(file.getName(), file.getRelativePath()));
        }

        @Override
        public void scan(ClassFile classFile, List<Map.Entry<String, String>> entries) {
            throw new IllegalStateException();
        }

        @Override
        public QueryFunction<Store, String> with(String pattern) {
            return store -> store.get(index()).entrySet().stream().filter(entry -> entry.getKey().matches(pattern))
                .flatMap(entry -> entry.getValue().stream()).collect(Collectors.toCollection(LinkedHashSet::new));
        }
    },

    /** scan method parameters types and annotations */
    MethodsParameter {
        @Override
        public void scan(ClassFile classFile, List<Map.Entry<String, String>> entries) {
            getMethods(classFile).forEach(method -> {
                String value = methodName(classFile, method);
                entries.addAll(entries(getParameters(method), value));
                getParametersAnnotations(method).forEach(annotations -> entries.addAll(entries(annotations, value)));
            });
        }
    },

    /** scan constructor parameters types and annotations */
    ConstructorsParameter {
        @Override
        public void scan(ClassFile classFile, List<Map.Entry<String, String>> entries) {
            getConstructors(classFile).forEach(constructor -> {
                String value = methodName(classFile, constructor);
                entries.addAll(entries(getParameters(constructor), value));
                getParametersAnnotations(constructor).forEach(annotations -> entries.addAll(entries(annotations, value)));
            });
        }
    },

    /** scan methods signature */
    MethodsSignature {
        @Override
        public void scan(ClassFile classFile, List<Map.Entry<String, String>> entries) {
            getMethods(classFile).forEach(method ->
                entries.add(entry(getParameters(method).toString(), methodName(classFile, method))));
        }

        @Override
        public QueryFunction<Store, String> with(AnnotatedElement... keys) {
            return QueryFunction.single(toNames(keys).toString()).getAll(this::get);
        }
    },

    /** scan constructors signature */
    ConstructorsSignature {
        @Override
        public void scan(ClassFile classFile, List<Map.Entry<String, String>> entries) {
            getConstructors(classFile).forEach(constructor ->
                entries.add(entry(getParameters(constructor).toString(), methodName(classFile, constructor))));
        }

        @Override
        public QueryFunction<Store, String> with(AnnotatedElement... keys) {
            return QueryFunction.single(toNames(keys).toString()).getAll(this::get);
        }
    },

    /** scan method return type */
    MethodsReturn {
        @Override
        public void scan(ClassFile classFile, List<Map.Entry<String, String>> entries) {
            getMethods(classFile).forEach(method ->
                entries.add(entry(getReturnType(method), methodName(classFile, method))));
        }
    };

    private Predicate<String> resultFilter = s -> true; //accept all by default

    @Override
    public String index() {
        return name();
    }

    public Scanners filterResultsBy(Predicate<String> filter) {
        this.resultFilter = filter;
        return this;
    }

    @Override
    public final List<Map.Entry<String, String>> scan(ClassFile classFile) {
        List<Map.Entry<String, String>> entries = new ArrayList<>();
        scan(classFile, entries);
        return entries.stream().filter(a -> acceptResult(a.getKey())).collect(Collectors.toList());
    }

    abstract void scan(ClassFile classFile, List<Map.Entry<String, String>> entries);

    protected boolean acceptResult(String fqn) {
        return fqn != null && resultFilter.test(fqn);
    }
}
