package org.reflections;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import org.reflections.util.ClasspathHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * convenient java reflection helper methods
 * <p>
 * 1. some helper methods to get type by name: {@link #forName(String, ClassLoader...)} and {@link #forNames(Iterable, ClassLoader...)}
 * <p>
 * 2. some helper methods to get all types/methods/fields/constructors/properties matching some predicates, generally:
 * <pre> Set&#60?> result = getAllXXX(type/s, withYYY) </pre>
 * <p>where get methods are:
 * <ul>
 * <li>{@link #getAllSuperTypes(Class, com.google.common.base.Predicate<T>...)}
 * <li>{@link #getAllFields(Class, com.google.common.base.Predicate<T>...)}
 * <li>{@link #getAllMethods(Class, com.google.common.base.Predicate<T>...)}
 * <li>{@link #getAllConstructors(Class, com.google.common.base.Predicate<T>...)}
 * </ul>
 * <p>and predicates included here all starts with "with", such as
 * <ul>
 * <li>{@link #withAnnotation(java.lang.annotation.Annotation)}
 * <li>{@link #withModifier(int)}
 * <li>{@link #withName(String)}
 * <li>{@link #withParameters(Class[])}
 * <li>{@link #withAnyParameterAnnotation(Class)}
 * <li>{@link #withParametersAssignableTo(Class[])}
 * <li>{@link #withPrefix(String)}
 * <li>{@link #withReturnType(Class)}
 * <li>{@link #withType(Class)}
 * <li>{@link #withTypeAssignableTo}
 * </ul>
 *
 * <p><br>
 * for example, getting all getters would be:
 * <pre>
 *      Set&#60Method> getters = getAllMethods(someClasses,
 *              Predicates.and(
 *                      withModifier(Modifier.PUBLIC),
 *                      withPrefix("get"),
 *                      withParametersCount(0)));
 *     </pre>
 */
@SuppressWarnings("unchecked")
public abstract class ReflectionUtils {

  /**
   * would include {@code Object.class} when {@link #getAllSuperTypes(Class, com.google.common.base.Predicate<T>[])}. default is false.
   */
  public static boolean includeObject = false;
  //
  private static List<String> primitiveNames;
  private static List<Class> primitiveTypes;
  private static List<String> primitiveDescriptors;

  /**
   * get all super types of given {@code type}, including, optionally filtered by {@code predicates}
   * <p> include {@code Object.class} if {@link #includeObject} is true
   */
  public static Set<Class<?>> getAllSuperTypes(final Class<?> type, Predicate<? super Class<?>>... predicates) {
    Set<Class<?>> result = new LinkedHashSet();
    if (type != null && (includeObject || !type.equals(Object.class))) {
      result.add(type);
      for (Class<?> supertype : getSuperTypes(type)) {
        result.addAll(getAllSuperTypes(supertype));
      }
    }
    return filter(result, predicates);
  }

  /**
   * get the immediate supertype and interfaces of the given {@code type}
   */
  public static Set<Class<?>> getSuperTypes(Class<?> type) {
    Set<Class<?>> result = new LinkedHashSet<>();
    Class<?> superclass = type.getSuperclass();
    Class<?>[] interfaces = type.getInterfaces();
    if (superclass != null && (includeObject || !superclass.equals(Object.class))) result.add(superclass);
    if (interfaces != null && interfaces.length > 0) result.addAll(Arrays.asList(interfaces));
    return result;
  }

  /**
   * get all methods of given {@code type}, up the super class hierarchy, optionally filtered by {@code predicates}
   */
  public static Set<Method> getAllMethods(final Class<?> type, Predicate<? super Method>... predicates) {
    Set<Method> result = new HashSet();
    for (Class<?> t : getAllSuperTypes(type)) {
      result.addAll(getMethods(t, predicates));
    }
    return result;
  }

  /**
   * get methods of given {@code type}, optionally filtered by {@code predicates}
   */
  public static Set<Method> getMethods(Class<?> t, Predicate<? super Method>... predicates) {
    return filter(t.isInterface() ? t.getMethods() : t.getDeclaredMethods(), predicates);
  }

  /**
   * get all constructors of given {@code type}, up the super class hierarchy, optionally filtered by {@code predicates}
   */
  public static Set<Constructor> getAllConstructors(final Class<?> type, Predicate<? super Constructor>... predicates) {
    Set<Constructor> result = new HashSet();
    for (Class<?> t : getAllSuperTypes(type)) {
      result.addAll(getConstructors(t, predicates));
    }
    return result;
  }

  /**
   * get constructors of given {@code type}, optionally filtered by {@code predicates}
   */
  public static Set<Constructor> getConstructors(Class<?> t, Predicate<? super Constructor>... predicates) {
    return ReflectionUtils.<Constructor>filter(t.getDeclaredConstructors(), predicates); //explicit needed only for jdk1.5
  }

  /**
   * get all fields of given {@code type}, up the super class hierarchy, optionally filtered by {@code predicates}
   */
  public static Set<Field> getAllFields(final Class<?> type, Predicate<? super Field>... predicates) {
    Set<Field> result = new HashSet();
    for (Class<?> t : getAllSuperTypes(type)) result.addAll(getFields(t, predicates));
    return result;
  }

  /**
   * get fields of given {@code type}, optionally filtered by {@code predicates}
   */
  public static Set<Field> getFields(Class<?> type, Predicate<? super Field>... predicates) {
    return filter(type.getDeclaredFields(), predicates);
  }

  //predicates

  /**
   * get all annotations of given {@code type}, up the super class hierarchy, optionally filtered by {@code predicates}
   */
  public static <T extends AnnotatedElement> Set<Annotation> getAllAnnotations(T type, Predicate<Annotation>... predicates) {
    Set<Annotation> result = new HashSet();
    if (type instanceof Class) {
      for (Class<?> t : getAllSuperTypes((Class<?>) type)) {
        result.addAll(getAnnotations(t, predicates));
      }
    } else {
      result.addAll(getAnnotations(type, predicates));
    }
    return result;
  }

  /**
   * get annotations of given {@code type}, optionally honorInherited, optionally filtered by {@code predicates}
   */
  public static <T extends AnnotatedElement> Set<Annotation> getAnnotations(T type, Predicate<Annotation>... predicates) {
    return filter(type.getDeclaredAnnotations(), predicates);
  }

  /**
   * filter all given {@code elements} with {@code predicates}, if given
   */
  public static <T extends AnnotatedElement> Set<T> getAll(final Set<T> elements, Predicate<? super T>... predicates) {
    final Predicate<T> and = Predicates.and(predicates);
    final Iterable<T> filter = elements.stream().filter(and::apply).collect(Collectors.toList());
    return Predicates.isEmpty(predicates) ?
        elements :
        StreamSupport.stream(filter.spliterator(), false).collect(Collectors.toSet());
  }


  /**
   * where member name equals given {@code name}
   */
  public static <T extends Member> Predicate<T> withName(final String name) {
    return input -> input != null && input.getName().equals(name);
  }

  /**
   * where member name startsWith given {@code prefix}
   */
  public static <T extends Member> Predicate<T> withPrefix(final String prefix) {
    return input -> input != null && input.getName().startsWith(prefix);
  }

  /**
   * where member's {@code toString} matches given {@code regex}
   * <p>for example:
   * <pre>
   *  getAllMethods(someClass, withPattern("public void .*"))
   * </pre>
   */
  public static <T extends AnnotatedElement> Predicate<T> withPattern(final String regex) {
    return input -> Pattern.matches(regex, input.toString());
  }

  /**
   * where element is annotated with given {@code annotation}
   */
  public static <T extends AnnotatedElement> Predicate<T> withAnnotation(final Class<? extends Annotation> annotation) {
    return input -> input != null && input.isAnnotationPresent(annotation);
  }

  /**
   * where element is annotated with given {@code annotations}
   */
  public static <T extends AnnotatedElement> Predicate<T> withAnnotations(final Class<? extends Annotation>... annotations) {
    return input -> input != null && Arrays.equals(annotations, annotationTypes(input.getAnnotations()));
  }

  private static Class<? extends Annotation>[] annotationTypes(Annotation[] annotations) {
    Class<? extends Annotation>[] result = new Class[annotations.length];
    for (int i = 0; i < annotations.length; i++) result[i] = annotations[i].annotationType();
    return result;
  }

  /**
   * where element is annotated with given {@code annotation}, including member matching
   */
  public static <T extends AnnotatedElement> Predicate<T> withAnnotation(final Annotation annotation) {
    return input -> input != null && input.isAnnotationPresent(annotation.annotationType()) &&
        areAnnotationMembersMatching(input.getAnnotation(annotation.annotationType()), annotation);
  }

  private static boolean areAnnotationMembersMatching(Annotation annotation1, Annotation annotation2) {
    if (annotation2 != null && annotation1.annotationType() == annotation2.annotationType()) {
      for (Method method : annotation1.annotationType().getDeclaredMethods()) {
        try {
          if (!method.invoke(annotation1).equals(method.invoke(annotation2))) return false;
        } catch (Exception e) {
          throw new ReflectionsException(String.format("could not invoke method %s on annotation %s", method.getName(), annotation1.annotationType()), e);
        }
      }
      return true;
    }
    return false;
  }

  /**
   * where element is annotated with given {@code annotations}, including member matching
   */
  public static <T extends AnnotatedElement> Predicate<T> withAnnotations(final Annotation... annotations) {
    return input -> {
      if (input != null) {
        Annotation[] inputAnnotations = input.getAnnotations();
        if (inputAnnotations.length == annotations.length) {
          for (int i = 0; i < inputAnnotations.length; i++) {
            if (!areAnnotationMembersMatching(inputAnnotations[i], annotations[i])) return false;
          }
        }
      }
      return true;
    };
  }

  /**
   * when method/constructor parameter types equals given {@code types}
   */
  public static Predicate<Member> withParameters(final Class<?>... types) {
    return input -> Arrays.equals(parameterTypes(input), types);
  }

  private static Class[] parameterTypes(Member member) {
    return member != null ?
        member.getClass() == Method.class ? ((Method) member).getParameterTypes() :
            member.getClass() == Constructor.class ? ((Constructor) member).getParameterTypes() : null : null;
  }

  /**
   * when member parameter types assignable to given {@code types}
   */
  public static Predicate<Member> withParametersAssignableTo(final Class... types) {
    return input -> {
      if (input != null) {
        Class<?>[] parameterTypes = parameterTypes(input);
        if (parameterTypes.length == types.length) {
          for (int i = 0; i < parameterTypes.length; i++) {
            if (!parameterTypes[i].isAssignableFrom(types[i]) ||
                (parameterTypes[i] == Object.class && types[i] != Object.class)) {
              return false;
            }
          }
          return true;
        }
      }
      return false;
    };
  }

  /**
   * when method/constructor parameters count equal given {@code count}
   */
  public static Predicate<Member> withParametersCount(final int count) {
    return input -> input != null && parameterTypes(input).length == count;
  }

  /**
   * when method/constructor has any parameter with an annotation matches given {@code annotations}
   */
  public static Predicate<Member> withAnyParameterAnnotation(final Class<? extends Annotation> annotationClass) {
    return input -> input != null && annotationTypes(parameterAnnotations(input)).stream().anyMatch(input1 -> input1.equals(annotationClass));
  }

  private static Set<Class<? extends Annotation>> annotationTypes(Iterable<Annotation> annotations) {
    Set<Class<? extends Annotation>> result = new HashSet();
    for (Annotation annotation : annotations) result.add(annotation.annotationType());
    return result;
  }

  //

  private static Set<Annotation> parameterAnnotations(Member member) {
    Set<Annotation> result = new HashSet();
    Annotation[][] annotations =
        member instanceof Method ? ((Method) member).getParameterAnnotations() :
            member instanceof Constructor ? ((Constructor) member).getParameterAnnotations() : null;
    for (Annotation[] annotation : annotations) Collections.addAll(result, annotation);
    return result;
  }

  /**
   * when method/constructor has any parameter with an annotation matches given {@code annotations}, including member matching
   */
  public static Predicate<Member> withAnyParameterAnnotation(final Annotation annotation) {
    return input -> input != null &&
        parameterAnnotations(input).stream().anyMatch(input1 -> areAnnotationMembersMatching(annotation, input1));
  }

  /**
   * when field type equal given {@code type}
   */
  public static <T> Predicate<Field> withType(final Class<T> type) {
    return input -> input != null && input.getType().equals(type);
  }

  /**
   * when field type assignable to given {@code type}
   */
  public static <T> Predicate<Field> withTypeAssignableTo(final Class<T> type) {
    return input -> input != null && type.isAssignableFrom(input.getType());
  }

  /**
   * when method return type equal given {@code type}
   */
  public static <T> Predicate<Method> withReturnType(final Class<T> type) {
    return input -> input != null && input.getReturnType().equals(type);
  }

  /**
   * when method return type assignable from given {@code type}
   */
  public static <T> Predicate<Method> withReturnTypeAssignableTo(final Class<T> type) {
    return input -> input != null && type.isAssignableFrom(input.getReturnType());
  }

  /**
   * when member modifier matches given {@code mod}
   * <p>for example:
   * <pre>
   * withModifier(Modifier.PUBLIC)
   * </pre>
   */
  public static <T extends Member> Predicate<T> withModifier(final int mod) {
    return input -> input != null && (input.getModifiers() & mod) != 0;
  }

  /**
   * when class modifier matches given {@code mod}
   * <p>for example:
   * <pre>
   * withModifier(Modifier.PUBLIC)
   * </pre>
   */
  public static Predicate<Class<?>> withClassModifier(final int mod) {
    return input -> input != null && (input.getModifiers() & mod) != 0;
  }

  /**
   * try to resolve all given string representation of types to a list of java types
   */
  public static <T> List<Class<? extends T>> forNames(final Iterable<String> classes, ClassLoader... classLoaders) {
    List<Class<? extends T>> result = new ArrayList<>();
    for (String className : classes) {
      Class<?> type = forName(className, classLoaders);
      if (type != null) {
        result.add((Class<? extends T>) type);
      }
    }
    return result;
  }

  /**
   * tries to resolve a java type name to a Class
   * <p>if optional {@link ClassLoader}s are not specified, then both {@link org.reflections.util.ClasspathHelper#contextClassLoader()} and {@link org.reflections.util.ClasspathHelper#staticClassLoader()} are used
   */
  public static Class<?> forName(String typeName, ClassLoader... classLoaders) {
    if (getPrimitiveNames().contains(typeName)) {
      return getPrimitiveTypes().get(getPrimitiveNames().indexOf(typeName));
    } else {
      String type;
      if (typeName.contains("[")) {
        int i = typeName.indexOf("[");
        type = typeName.substring(0, i);
        String array = typeName.substring(i).replace("]", "");

        if (getPrimitiveNames().contains(type)) {
          type = getPrimitiveDescriptors().get(getPrimitiveNames().indexOf(type));
        } else {
          type = "L" + type + ";";
        }

        type = array + type;
      } else {
        type = typeName;
      }

      List<ReflectionsException> reflectionsExceptions = new ArrayList();
      for (ClassLoader classLoader : ClasspathHelper.classLoaders(classLoaders)) {
        if (type.contains("[")) {
          try {
            return Class.forName(type, false, classLoader);
          } catch (Throwable e) {
            reflectionsExceptions.add(new ReflectionsException("could not get type for name " + typeName, e));
          }
        }
        try {
          return classLoader.loadClass(type);
        } catch (Throwable e) {
          reflectionsExceptions.add(new ReflectionsException("could not get type for name " + typeName, e));
        }
      }

      if (Reflections.log != null) {
        for (ReflectionsException reflectionsException : reflectionsExceptions) {
          Reflections.log.warn("could not get type for name " + typeName + " from any class loader",
              reflectionsException);
        }
      }

      return null;
    }
  }

  private static List<String> getPrimitiveNames() {
    initPrimitives();
    return primitiveNames;
  }

  private static List<Class> getPrimitiveTypes() {
    initPrimitives();
    return primitiveTypes;
  }

  private static List<String> getPrimitiveDescriptors() {
    initPrimitives();
    return primitiveDescriptors;
  }

  private static void initPrimitives() {
    if (primitiveNames == null) {
      primitiveNames = Arrays.asList("boolean", "char", "byte", "short", "int", "long", "float", "double", "void");
      primitiveTypes = Arrays.asList(boolean.class, char.class, byte.class, short.class, int.class, long.class, float.class, double.class, void.class);
      primitiveDescriptors = Arrays.asList("Z", "C", "B", "S", "I", "J", "F", "D", "V");
    }
  }

  //
  static <T> Set<T> filter(final T[] elements, Predicate<? super T>... predicates) {
    return Predicates.isEmpty(predicates) ?
        Stream.of(elements).collect(Collectors.toSet())
        :
        StreamSupport
            .stream(Arrays.asList(elements).stream().filter(Predicates.and(predicates)::apply).collect(Collectors.toList()).spliterator(), false)
            .collect(Collectors.toSet());
  }

  static <T> Set<T> filter(final Iterable<T> elements, Predicate<? super T>... predicates) {
    return Predicates.isEmpty(predicates) ?
        StreamSupport
            .stream(elements.spliterator(), false).collect(Collectors.toSet()) :
        StreamSupport
            .stream(StreamSupport.stream(elements.spliterator(), false).filter(Predicates.and(predicates)::apply).collect(Collectors.toList()).spliterator(), false)
            .collect(Collectors.toSet());
  }
}
