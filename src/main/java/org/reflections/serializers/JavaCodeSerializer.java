package org.reflections.serializers;

import com.google.common.base.Joiner;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
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
import java.util.*;
import java.util.function.Supplier;

import static org.reflections.Reflections.log;
import static org.reflections.util.Utils.prepareFile;
import static org.reflections.util.Utils.repeat;

public class JavaCodeSerializer implements Serializer {

  private static final String PATH_SEPARATOR = "_";
  private static final String DOUBLE_SEPARATOR = "__";
  private static final String DOT_SEPARATOR = ".";
  private static final String ARRAY_DESCRIPTOR = "$$";
  private static final String TOKEN_SEPARATOR = "_";

  public static Class<?> resolveClass(final Class aClass) {
    try {
      return resolveClassOf(aClass);
    } catch (Exception e) {
      throw new ReflectionsException("could not resolve to class " + aClass.getName(), e);
    }
  }

  //
  public static Class<?> resolveClassOf(final Class element) throws ClassNotFoundException {
    Class<?> cursor = element;
    LinkedList<String> ognl = new LinkedList<>();
    while (cursor != null) {
      ognl.addFirst(cursor.getSimpleName());
      cursor = cursor.getDeclaringClass();
    }

    String classOgnl = Joiner.on(".").join(ognl.subList(1, ognl.size())).replace(".$", "$");
    return Class.forName(classOgnl);
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
      String name = annotation.getSimpleName().replace(PATH_SEPARATOR, DOT_SEPARATOR);
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
      if (methodOgnl.contains(TOKEN_SEPARATOR)) {
        methodName = methodOgnl.substring(0, methodOgnl.indexOf(TOKEN_SEPARATOR));
        String[] params = methodOgnl.substring(methodOgnl.indexOf(TOKEN_SEPARATOR) + 1).split(DOUBLE_SEPARATOR);
        paramTypes = new Class<?>[params.length];
        for (int i = 0; i < params.length; i++) {
          String typeName = params[i].replace(ARRAY_DESCRIPTOR, "[]").replace(PATH_SEPARATOR, DOT_SEPARATOR);
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

    List<String> prevPaths = new ArrayList<>();
    int indent = 1;

    List<String> keys = new ArrayList<>(reflections.getStore().get(TypeElementsScanner.class.getSimpleName()).keySet());
    Collections.sort(keys);
    for (String fqn : keys) {
      List<String> typePaths = new ArrayList<>(Arrays.asList(fqn.split("\\.")));

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
      List<String> annotations = new ArrayList<>();
      List<String> fields = new ArrayList<>();


      final Map<String, Collection<String>> map = new HashMap<>();

      final Supplier supplier = (Supplier<Set<String>>) HashSet::new;

      final Multimap<String, String> methods = Multimaps.newSetMultimap(map, supplier);

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
              paramsDescriptor = TOKEN_SEPARATOR + params.replace(DOT_SEPARATOR, TOKEN_SEPARATOR).replace(", ", DOUBLE_SEPARATOR).replace("[]", ARRAY_DESCRIPTOR);
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
        return getNonDuplicateName(normalized + TOKEN_SEPARATOR, prev, offset);
      }
    }

    return normalized;
  }

  private String normalize(String candidate) {
    return candidate.replace(DOT_SEPARATOR, PATH_SEPARATOR);
  }

  private String getNonDuplicateName(String candidate, List<String> prev) {
    return getNonDuplicateName(candidate, prev, prev.size());
  }
}
