package org.reflections.scanners;

import javassist.bytecode.ClassFile;
import javassist.bytecode.FieldInfo;
import javassist.bytecode.MethodInfo;
import org.reflections.Store;
import org.reflections.util.FilterBuilder;
import org.reflections.util.JavassistHelper;
import org.reflections.util.NameHelper;
import org.reflections.util.QueryBuilder;
import org.reflections.util.QueryFunction;
import org.reflections.vfs.Vfs;

import java.lang.annotation.Inherited;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
     * Use {@link #filterResultsBy(Predicate)} to change, i.e. {@code filterResultsBy(c -> true)}</i>
     * */
    SubTypes {
        /* Object class is excluded by default from subtypes indexing */
        { filterResultsBy(new FilterBuilder().excludePattern("java\\.lang\\.Object")); }

        @Override
        public List<Map.Entry<String, String>> scan(ClassFile classFile) {
            List<Map.Entry<String, String>> entries = new ArrayList<>();
            entries.add(entry(classFile.getSuperclass(), classFile.getName()));
            entries.addAll(entries(Arrays.asList(classFile.getInterfaces()), classFile.getName()));
            return filtered(entries);
        }
    },

    /** scan type annotations */
    TypesAnnotated {
        @Override
        public boolean acceptResult(String annotation) {
            return super.acceptResult(annotation) || annotation.equals(Inherited.class.getName());
        }

        @Override
        public List<Map.Entry<String, String>> scan(ClassFile classFile) {
            return entries(JavassistHelper.getAnnotations(classFile::getAttribute), classFile.getName());
        }
    },

    /** scan method annotations */
    MethodsAnnotated {
        @Override
        public List<Map.Entry<String, String>> scan(ClassFile classFile) {
            List<Map.Entry<String, String>> entries = new ArrayList<>();
            for (MethodInfo method : classFile.getMethods()) {
                entries.addAll(entries(JavassistHelper.getAnnotations(method::getAttribute), JavassistHelper.toName(classFile, method)));
            }
            return filtered(entries);
        }

        @Override
        public QueryFunction<Store, String> with(Class<?>... keys) {
            return super.with(keys).filter(this::isMethod);
        }
    },

    /** scan constructor annotations */
    ConstructorsAnnotated {
        @Override
        public List<Map.Entry<String, String>> scan(ClassFile classFile) {
            return null;
        }

        @Override
        public String index() {
            return MethodsAnnotated.index();
        }

        @Override
        public QueryFunction<Store, String> with(Class<?>... keys) {
            return super.with(keys).filter(this::isConstructor);
        }
    },

    /** scan field annotations */
    FieldsAnnotated {
        @Override
        public List<Map.Entry<String, String>> scan(ClassFile classFile) {
            List<Map.Entry<String, String>> entries = new ArrayList<>();
            for (FieldInfo field : classFile.getFields()) {
                entries.addAll(entries(JavassistHelper.getAnnotations(field::getAttribute), JavassistHelper.toName(classFile, field)));
            }
            return filtered(entries);
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
        public List<Map.Entry<String, String>> scan(ClassFile classFile) {
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
        public List<Map.Entry<String, String>> scan(ClassFile classFile) {
            List<Map.Entry<String, String>> entries = new ArrayList<>();
            for (MethodInfo method : classFile.getMethods()) {
                entries.addAll(entries(JavassistHelper.getParameters(method), JavassistHelper.toName(classFile, method)));
                for (List<String> annotations : JavassistHelper.getParametersAnnotations(method)) {
                    entries.addAll(entries(annotations, JavassistHelper.toName(classFile, method)));
                }
            }
            return filtered(entries);
        }

        @Override
        public QueryFunction<Store, String> with(Class<?>... keys) {
            return super.with(keys).filter(this::isMethod);
        }
    },

    /** scan constructor parameters types and annotations
     * <p><i>requires {@link #MethodsParameter} configured</i> */
    ConstructorsParameter {
        @Override
        public List<Map.Entry<String, String>> scan(ClassFile classFile) {
            return null;
        }

        @Override
        public String index() {
            return MethodsParameter.index();
        }

        @Override
        public QueryFunction<Store, String> with(Class<?>... keys) {
            return super.with(keys).filter(this::isConstructor);
        }
    },

    /** scan method parameters types */
    MethodsSignature {
        @Override
        public List<Map.Entry<String, String>> scan(ClassFile classFile) {
            List<Map.Entry<String, String>> entries = new ArrayList<>();
            for (MethodInfo method : classFile.getMethods()) {
                entries.add(entry(JavassistHelper.getParameters(method).toString(), JavassistHelper.toName(classFile, method)));
            }
            return entries;
        }

        @Override
        public QueryFunction<Store, String> with(Class<?>... keys) {
            return QueryFunction.single(toNames(keys).toString()).getAll(this::get).filter(this::isMethod);
        }
    },

    /** scan constructor parameters types
     * <p><i>requires {@link #MethodsSignature} configured</i> */
    ConstructorsSignature {
        @Override
        public List<Map.Entry<String, String>> scan(ClassFile classFile) {
            return null;
        }

        @Override
        public String index() {
            return MethodsSignature.index();
        }

        @Override
        public QueryFunction<Store, String> with(Class<?>... keys) {
            return QueryFunction.single(toNames(keys).toString()).getAll(this::get).filter(this::isConstructor);
        }
    },

    /** scan method return type */
    MethodsReturn {
        @Override
        public List<Map.Entry<String, String>> scan(ClassFile classFile) {
            List<Map.Entry<String, String>> entries = new ArrayList<>();
            for (MethodInfo method : classFile.getMethods()) {
                if (method.isMethod()) {
                    entries.add(entry(JavassistHelper.getReturnType(method), JavassistHelper.toName(classFile, method)));
                }
            }
            return filtered(entries);
        }
    };

    @Override
    public String index() {
        return name();
    }

    private Predicate<String> resultFilter = s -> true; //accept all by default

    public Scanners filterResultsBy(Predicate<String> filter) {
        this.resultFilter = filter;
        return this;
    }

    protected List<Map.Entry<String, String>> filtered(List<Map.Entry<String, String>> entries) {
        return entries.stream().filter(a -> acceptResult(a.getValue())).collect(Collectors.toList());
    }

    protected boolean acceptResult(String fqn) {
        return fqn != null && resultFilter.test(fqn);
    }
}
