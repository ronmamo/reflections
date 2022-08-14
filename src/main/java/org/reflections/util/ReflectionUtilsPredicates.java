package org.reflections.util;

import org.reflections.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toSet;

/** helper predicates for java meta types*/
public class ReflectionUtilsPredicates {
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
	 * where annotated element name startsWith given {@code prefix}
	 */
	public static <T> Predicate<T> withNamePrefix(final String prefix) {
		return input -> toName(input).startsWith(prefix);
	}

	/**
	 * where member's {@code toString} matches given {@code regex}
	 * <pre> get(Methods.of(someClass).filter(withPattern("public void .*"))) </pre>
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
		return input -> input != null && Arrays.equals(annotations, ReflectionUtilsPredicates.annotationTypes(input.getAnnotations()));
	}

	/**
	 * where element is annotated with given {@code annotation}, including member matching
	 */
	public static <T extends AnnotatedElement> Predicate<T> withAnnotation(final Annotation annotation) {
		return input -> input != null && input.isAnnotationPresent(annotation.annotationType()) &&
			ReflectionUtilsPredicates.areAnnotationMembersMatching(input.getAnnotation(annotation.annotationType()), annotation);
	}

	/**
	 * where element is annotated with given {@code annotations}, including member matching
	 */
	public static <T extends AnnotatedElement> Predicate<T> withAnnotations(final Annotation... annotations) {
		return input -> {
			if (input != null) {
				Annotation[] inputAnnotations = input.getAnnotations();
				if (inputAnnotations.length == annotations.length) {
					return IntStream.range(0, inputAnnotations.length)
						.allMatch(i -> ReflectionUtilsPredicates.areAnnotationMembersMatching(inputAnnotations[i], annotations[i]));
				}
			}
			return true;
		};
	}

	/**
	 * when method/constructor parameter types equals given {@code types}
	 */
	public static Predicate<Member> withParameters(final Class<?>... types) {
		return input -> Arrays.equals(ReflectionUtilsPredicates.parameterTypes(input), types);
	}

	/**
	 * when member parameter types assignable to given {@code types}
	 */
	public static Predicate<Member> withParametersAssignableTo(final Class... types) {
		return input -> ReflectionUtilsPredicates.isAssignable(types, ReflectionUtilsPredicates.parameterTypes(input));
	}

	/**
	 * when method/constructor parameter types assignable from given {@code types}
	 */
	public static Predicate<Member> withParametersAssignableFrom(final Class... types) {
		return input -> ReflectionUtilsPredicates.isAssignable(ReflectionUtilsPredicates.parameterTypes(input), types);
	}

	/**
	 * when method/constructor parameters count equal given {@code count}
	 */
	public static Predicate<Member> withParametersCount(final int count) {
		return input -> input != null && ReflectionUtilsPredicates.parameterTypes(input).length == count;
	}

	/**
	 * when method/constructor has any parameter with an annotation matches given {@code annotations}
	 */
	public static Predicate<Member> withAnyParameterAnnotation(final Class<? extends Annotation> annotationClass) {
		return input -> input != null && ReflectionUtilsPredicates.annotationTypes(ReflectionUtilsPredicates.parameterAnnotations(input)).stream().anyMatch(input1 -> input1.equals(annotationClass));
	}

	/**
	 * when method/constructor has any parameter with an annotation matches given {@code annotations}, including member matching
	 */
	public static Predicate<Member> withAnyParameterAnnotation(final Annotation annotation) {
		return input -> input != null && ReflectionUtilsPredicates.parameterAnnotations(input).stream().anyMatch(input1 -> ReflectionUtilsPredicates.areAnnotationMembersMatching(annotation, input1));
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
	public static <T> Predicate<Method> withReturnTypeAssignableFrom(final Class<T> type) {
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
	 * when member modifier is public
	 */
	public static <T extends Member> Predicate<T> withPublic() {
		return ReflectionUtilsPredicates.withModifier(Modifier.PUBLIC);
	}

	public static <T extends Member> Predicate<T> withStatic() {
		return ReflectionUtilsPredicates.withModifier(Modifier.STATIC);
	}

	public static <T extends Member> Predicate<T> withInterface() {
		return ReflectionUtilsPredicates.withModifier(Modifier.INTERFACE);
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

	public static boolean isAssignable(Class[] childClasses, Class[] parentClasses) {
		if (childClasses == null || childClasses.length == 0) {
			return parentClasses == null || parentClasses.length == 0;
		}
		if (childClasses.length != parentClasses.length) {
			return false;
		}
		return IntStream.range(0, childClasses.length)
			.noneMatch(i -> !parentClasses[i].isAssignableFrom(childClasses[i]) ||
				parentClasses[i] == Object.class && childClasses[i] != Object.class);
	}

	//
	private static String toName(Object input) {
		return input == null ? "" :
			input.getClass().equals(Class.class) ? ((Class<?>) input).getName() :
			input instanceof Member ? ((Member) input).getName() :
			input instanceof Annotation ? ((Annotation) input).annotationType().getName() :
				input.toString();
	}

	private static Class[] parameterTypes(Member member) {
		return member != null ?
			member.getClass() == Method.class ? ((Method) member).getParameterTypes() :
				member.getClass() == Constructor.class ? ((Constructor) member).getParameterTypes() : null : null;
	}

	private static Set<Annotation> parameterAnnotations(Member member) {
		Annotation[][] annotations =
			member instanceof Method ? ((Method) member).getParameterAnnotations() :
				member instanceof Constructor ? ((Constructor) member).getParameterAnnotations() : null;
		return Arrays.stream(annotations != null ? annotations : new Annotation[0][]).flatMap(Arrays::stream).collect(toSet());
	}

	private static Set<Class<? extends Annotation>> annotationTypes(Collection<Annotation> annotations) {
		return annotations.stream().map(Annotation::annotationType).collect(toSet());
	}

	private static Class<? extends Annotation>[] annotationTypes(Annotation[] annotations) {
		return Arrays.stream(annotations).map(Annotation::annotationType).toArray(Class[]::new);
	}

	private static boolean areAnnotationMembersMatching(Annotation annotation1, Annotation annotation2) {
		if (annotation2 != null && annotation1.annotationType() == annotation2.annotationType()) {
			for (Method method : annotation1.annotationType().getDeclaredMethods()) {
				if (!ReflectionUtils.invoke(method, annotation1).equals(ReflectionUtils.invoke(method, annotation2)))
					return false;
			}
			return true;
		}
		return false;
	}
}
