package org.reflections.serializers;

import com.google.common.base.Joiner;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import org.reflections.ReflectionUtils;
import org.reflections.Reflections;
import org.reflections.ReflectionsException;
import org.reflections.scanners.TypeElementsScanner;
import org.reflections.util.Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.reflections.Reflections.log;
import static org.reflections.util.Utils.prepareFile;
import static org.reflections.util.Utils.repeat;

/** serialization of Reflections to java code
 * <p> serializes types and types elements into interfaces respectively to fully qualified name,
 * <p> for example:
 * <pre>
 * public interface MyTestModelStore {
 *	public interface <b>org</b> extends IPackage {
 *	    public interface <b>reflections</b> extends IPackage {
 *			public interface <b>TestModel$AC1</b> extends IClass {}
 *			public interface <b>TestModel$C4</b> extends IClass {
 *				public interface <b>f1</b> extends IField {}
 *				public interface <b>m1</b> extends IMethod {}
 *				public interface <b>m1_int_java$lang$String$$$$</b> extends IMethod {}
 *	...
 * }
 * </pre>
 * <p> use the different resolve methods to resolve the serialized element into Class, Field or Method. for example:
 * <pre>
 *  Class&#60? extends IMethod> imethod = MyTestModelStore.org.reflections.TestModel$C4.m1.class;
 *  Method method = JavaCodeSerializer.resolve(imethod);
 * </pre>
 * <p>depends on Reflections configured with {@link org.reflections.scanners.TypeElementsScanner}
 * <p><p>the {@link #save(org.reflections.Reflections, String)} method filename should be in the pattern: path/path/path/package.package.classname
 * */
public class JavaCodeSerializer implements Serializer {

    private static final String pathSeparator = "_";
    private static final String doubleSeparator = "__";
    private static final String dotSeparator = ".";
    private static final String arrayDescriptor = "$$";
    private static final String tokenSeparator = "_";

    public Reflections read(InputStream inputStream) {
        throw new UnsupportedOperationException("read is not implemented on JavaCodeSerializer");
    }

    /**
     * name should be in the pattern: path/path/path/package.package.classname,
     * for example <pre>/data/projects/my/src/main/java/org.my.project.MyStore</pre>
     * would create class MyStore in package org.my.project in the path /data/projects/my/src/main/java
     */
    public File save(Reflections reflections, String name) {
        if (name.endsWith("/")) {
            name = name.substring(0, name.length() - 1); //trim / at the end
        }

        //prepare file
        String filename = name.replace('.', '/').concat(".java");
        File file = prepareFile(filename);

        //get package and class names
        String packageName;
        String className;
        int lastDot = name.lastIndexOf('.');
        if (lastDot == -1) {
            packageName = "";
            className = name.substring(name.lastIndexOf('/') + 1);
        } else {
            packageName = name.substring(name.lastIndexOf('/') + 1, lastDot);
            className = name.substring(lastDot + 1);
        }

        //generate
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("//generated using Reflections JavaCodeSerializer")
                    .append(" [").append(new Date()).append("]")
                    .append("\n");
            if (packageName.length() != 0) {
                sb.append("package ").append(packageName).append(";\n");
                sb.append("\n");
            }
            sb.append("public interface ").append(className).append(" {\n\n");
            sb.append(toString(reflections));
            sb.append("}\n");

            Files.write(sb.toString(), new File(filename), Charset.defaultCharset());

        } catch (IOException e) {
            throw new RuntimeException();
        }

        return file;
    }

    public String toString(Reflections reflections) {
        if (reflections.getStore().get(TypeElementsScanner.class.getSimpleName()).isEmpty()) {
            if (log != null) log.warn("JavaCodeSerializer needs TypeElementsScanner configured");
        }

        StringBuilder sb = new StringBuilder();

        List<String> prevPaths = Lists.newArrayList();
        int indent = 1;

        List<String> keys = Lists.newArrayList(reflections.getStore().get(TypeElementsScanner.class.getSimpleName()).keySet());
        Collections.sort(keys);
        for (String fqn : keys) {
            List<String> typePaths = Lists.newArrayList(fqn.split("\\."));

            //skip indention
            int i = 0;
            while (i < Math.min(typePaths.size(), prevPaths.size()) && typePaths.get(i).equals(prevPaths.get(i))) {
                i++;
            }

            //indent left
            for (int j = prevPaths.size(); j > i; j--) {
                sb.append(repeat("\t", --indent)).append("}\n");
            }

            //indent right - add packages
            for (int j = i; j < typePaths.size() - 1; j++) {
                sb.append(repeat("\t", indent++)).append("public interface ").append(getNonDuplicateName(typePaths.get(j), typePaths, j)).append(" {\n");
            }

            //indent right - add class
            String className = typePaths.get(typePaths.size() - 1);

            //get fields and methods
            List<String> annotations = Lists.newArrayList();
            List<String> fields = Lists.newArrayList();
            final Multimap<String,String> methods = Multimaps.newSetMultimap(new HashMap<String, Collection<String>>(), new Supplier<Set<String>>() {
                public Set<String> get() {
                    return Sets.newHashSet();
                }
            });

            for (String element : reflections.getStore().get(TypeElementsScanner.class.getSimpleName(), fqn)) {
                if (element.startsWith("@")) {
                    annotations.add(element.substring(1));
                } else if (element.contains("(")) {
                    //method
                    if (!element.startsWith("<")) {
                        int i1 = element.indexOf('(');
                        String name = element.substring(0, i1);
                        String params = element.substring(i1 + 1, element.indexOf(")"));

                        String paramsDescriptor = "";
                        if (params.length() != 0) {
                            paramsDescriptor = tokenSeparator + params.replace(dotSeparator, tokenSeparator).replace(", ", doubleSeparator).replace("[]", arrayDescriptor);
                        }
                        String normalized = name + paramsDescriptor;
                        methods.put(name, normalized);
                    }
                } else if (!Utils.isEmpty(element)) {
                    //field
                    fields.add(element);
                }
            }

            //add class and it's fields and methods
            sb.append(repeat("\t", indent++)).append("public interface ").append(getNonDuplicateName(className, typePaths, typePaths.size() - 1)).append(" {\n");

            //add fields
            if (!fields.isEmpty()) {
                sb.append(repeat("\t", indent++)).append("public interface fields {\n");
                for (String field : fields) {
                    sb.append(repeat("\t", indent)).append("public interface ").append(getNonDuplicateName(field, typePaths)).append(" {}\n");
                }
                sb.append(repeat("\t", --indent)).append("}\n");
            }

            //add methods
            if (!methods.isEmpty()) {
                sb.append(repeat("\t", indent++)).append("public interface methods {\n");
                for (Map.Entry<String, String> entry : methods.entries()) {
                    String simpleName = entry.getKey();
                    String normalized = entry.getValue();

                    String methodName = methods.get(simpleName).size() == 1 ? simpleName : normalized;

                    methodName = getNonDuplicateName(methodName, fields);

                    sb.append(repeat("\t", indent)).append("public interface ").append(getNonDuplicateName(methodName, typePaths)).append(" {}\n");
                }
                sb.append(repeat("\t", --indent)).append("}\n");
            }

            //add annotations
            if (!annotations.isEmpty()) {
                sb.append(repeat("\t", indent++)).append("public interface annotations {\n");
                for (String annotation : annotations) {
                    String nonDuplicateName = annotation;
                    nonDuplicateName = getNonDuplicateName(nonDuplicateName, typePaths);
                    sb.append(repeat("\t", indent)).append("public interface ").append(nonDuplicateName).append(" {}\n");
                }
                sb.append(repeat("\t", --indent)).append("}\n");
            }

            prevPaths = typePaths;
        }


        //close indention
        for (int j = prevPaths.size(); j >= 1; j--) {
            sb.append(repeat("\t", j)).append("}\n");
        }

        return sb.toString();
    }

    private String getNonDuplicateName(String candidate, List<String> prev, int offset) {
        String normalized = normalize(candidate);
        for (int i = 0; i < offset; i++) {
            if (normalized.equals(prev.get(i))) {
                return getNonDuplicateName(normalized + tokenSeparator, prev, offset);
            }
        }

        return normalized;
    }

    private String normalize(String candidate) {
        return candidate.replace(dotSeparator, pathSeparator);
    }

    private String getNonDuplicateName(String candidate, List<String> prev) {
        return getNonDuplicateName(candidate, prev, prev.size());
    }

    //
    public static Class<?> resolveClassOf(final Class element) throws ClassNotFoundException {
        Class<?> cursor = element;
        LinkedList<String> ognl = Lists.newLinkedList();

        while (cursor != null) {
            ognl.addFirst(cursor.getSimpleName());
            cursor = cursor.getDeclaringClass();
        }

        String classOgnl = Joiner.on(".").join(ognl.subList(1, ognl.size())).replace(".$", "$");
        return Class.forName(classOgnl);
    }

    public static Class<?> resolveClass(final Class aClass) {
        try {
            return resolveClassOf(aClass);
        } catch (Exception e) {
            throw new ReflectionsException("could not resolve to class " + aClass.getName(), e);
        }
    }

    public static Field resolveField(final Class aField) {
        try {
            String name = aField.getSimpleName();
            Class<?> declaringClass = aField.getDeclaringClass().getDeclaringClass();
            return resolveClassOf(declaringClass).getDeclaredField(name);
        } catch (Exception e) {
            throw new ReflectionsException("could not resolve to field " + aField.getName(), e);
        }
    }

    public static Annotation resolveAnnotation(Class annotation) {
        try {
            String name = annotation.getSimpleName().replace(pathSeparator, dotSeparator);
            Class<?> declaringClass = annotation.getDeclaringClass().getDeclaringClass();
            Class<?> aClass = resolveClassOf(declaringClass);
            Class<? extends Annotation> aClass1 = (Class<? extends Annotation>) ReflectionUtils.forName(name);
            Annotation annotation1 = aClass.getAnnotation(aClass1);
            return annotation1;
        } catch (Exception e) {
            throw new ReflectionsException("could not resolve to annotation " + annotation.getName(), e);
        }
    }

    public static Method resolveMethod(final Class aMethod) {
        String methodOgnl = aMethod.getSimpleName();

        try {
            String methodName;
            Class<?>[] paramTypes;
            if (methodOgnl.contains(tokenSeparator)) {
                methodName = methodOgnl.substring(0, methodOgnl.indexOf(tokenSeparator));
                String[] params = methodOgnl.substring(methodOgnl.indexOf(tokenSeparator) + 1).split(doubleSeparator);
                paramTypes = new Class<?>[params.length];
                for (int i = 0; i < params.length; i++) {
                    String typeName = params[i].replace(arrayDescriptor, "[]").replace(pathSeparator, dotSeparator);
                    paramTypes[i] = ReflectionUtils.forName(typeName);
                }
            } else {
                methodName = methodOgnl;
                paramTypes = null;
            }

            Class<?> declaringClass = aMethod.getDeclaringClass().getDeclaringClass();
            return resolveClassOf(declaringClass).getDeclaredMethod(methodName, paramTypes);
        } catch (Exception e) {
            throw new ReflectionsException("could not resolve to method " + aMethod.getName(), e);
        }
    }
}
