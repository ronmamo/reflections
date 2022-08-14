package org.reflections.util;

import org.reflections.ReflectionsException;

import javax.annotation.Nullable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Helper methods for converting between annotated elements and their names
 */
public interface NameHelper {

	List<String> primitiveNames = Arrays.asList("boolean", "char", "byte", "short", "int", "long", "float", "double", "void");
	List<Class<?>> primitiveTypes = Arrays.asList(boolean.class, char.class, byte.class, short.class, int.class, long.class, float.class, double.class, void.class);
	List<String> primitiveDescriptors = Arrays.asList("Z", "C", "B", "S", "I", "J", "F", "D", "V");

	// toName
	default String toName(AnnotatedElement element) {
		return element.getClass().equals(Class.class) ? toName((Class<?>) element) :
			element.getClass().equals(Constructor.class) ? toName((Constructor<?>) element) :
			element.getClass().equals(Method.class) ? toName((Method) element) :
			element.getClass().equals(Field.class) ? toName((Field) element) : null;
	}

	default String toName(Class<?> type) {
		int dim = 0;
		while (type.isArray()) { dim++; type = type.getComponentType(); }
		return type.getName() + String.join("", Collections.nCopies(dim, "[]"));
	}

	default String toName(Constructor<?> constructor) {
		return String.format("%s.<init>(%s)", constructor.getName(), String.join(", ", toNames(constructor.getParameterTypes())));
	}

	default String toName(Method method) {
		return String.format("%s.%s(%s)", method.getDeclaringClass().getName(), method.getName(), String.join(", ", toNames(method.getParameterTypes())));
	}

	default String toName(Field field) {
		return String.format("%s.%s", field.getDeclaringClass().getName(), field.getName());
	}
	
	default Collection<String> toNames(Collection<? extends AnnotatedElement> elements) {
		return elements.stream().map(this::toName).filter(Objects::nonNull).collect(Collectors.toList());
	}

	default Collection<String> toNames(AnnotatedElement... elements) {
		return toNames(Arrays.asList(elements));
	}
	
	// forName
	@SuppressWarnings("unchecked")
	default <T> T forName(String name, Class<T> resultType, ClassLoader... loaders) {
		return resultType.equals(Class.class) ? (T) forClass(name, loaders) :
			resultType.equals(Constructor.class) ? (T) forConstructor(name, loaders) :
			resultType.equals(Method.class) ? (T) forMethod(name, loaders) :
			resultType.equals(Field.class) ? (T) forField(name, loaders) :
			resultType.equals(Member.class) ? (T) forMember(name, loaders) : null;
	}

	/** tries to resolve a java type name to a Class
	 * <p>if optional {@link ClassLoader}s are not specified, then both {@link org.reflections.util.ClasspathHelper#contextClassLoader()} and {@link org.reflections.util.ClasspathHelper#staticClassLoader()} are used
	 * */
	default Class<?> forClass(String typeName, ClassLoader... loaders) {
		if (primitiveNames.contains(typeName)) {
			return primitiveTypes.get(primitiveNames.indexOf(typeName));
		} else {
			String type;
			if (typeName.contains("[")) {
				int i = typeName.indexOf("[");
				type = typeName.substring(0, i);
				String array = typeName.substring(i).replace("]", "");
				if (primitiveNames.contains(type)) {
					type = primitiveDescriptors.get(primitiveNames.indexOf(type));
				} else {
					type = "L" + type + ";";
				}
				type = array + type;
			} else {
				type = typeName;
			}

			for (ClassLoader classLoader : ClasspathHelper.classLoaders(loaders)) {
				if (type.contains("[")) {
					try { return Class.forName(type, false, classLoader); }
					catch (Throwable ignored) {}
				}
				try { return classLoader.loadClass(type); }
				catch (Throwable ignored) {}
			}
			return null;
		}
	}

	default Member forMember(String descriptor, ClassLoader... loaders) throws ReflectionsException {
		int p0 = descriptor.lastIndexOf('(');
		String memberKey = p0 != -1 ? descriptor.substring(0, p0) : descriptor;
		String methodParameters = p0 != -1 ? descriptor.substring(p0 + 1, descriptor.lastIndexOf(')')) : "";

		int p1 = Math.max(memberKey.lastIndexOf('.'), memberKey.lastIndexOf("$"));
		String className = memberKey.substring(0, p1);
		String memberName = memberKey.substring(p1 + 1);

		Class<?>[] parameterTypes = null;
		if (!methodParameters.isEmpty()) {
			String[] parameterNames = methodParameters.split(",");
			parameterTypes = Arrays.stream(parameterNames).map(name -> forClass(name.trim(), loaders)).toArray(Class<?>[]::new);
		}

		Class<?> aClass;
		try {
			aClass = forClass(className, loaders);
		} catch (Exception e) {
			return null;
		}
		while (aClass != null) {
			try {
				if (!descriptor.contains("(")) {
					return aClass.isInterface() ? aClass.getField(memberName) : aClass.getDeclaredField(memberName);
				} else if (descriptor.contains("init>")) {
					return aClass.isInterface() ? aClass.getConstructor(parameterTypes) : aClass.getDeclaredConstructor(parameterTypes);
				} else {
					return aClass.isInterface() ? aClass.getMethod(memberName, parameterTypes) : aClass.getDeclaredMethod(memberName, parameterTypes);
				}
			} catch (Exception e) {
				aClass = aClass.getSuperclass();
			}
		}
		return null;
	}

	@Nullable
	default <T extends AnnotatedElement> T forElement(String descriptor, Class<T> resultType, ClassLoader[] loaders) {
		Member member = forMember(descriptor, loaders);
		//noinspection unchecked
		return member != null && member.getClass().equals(resultType) ? (T) member : null;
	}

	@Nullable
	default Method forMethod(String descriptor, ClassLoader... loaders) throws ReflectionsException {
		return forElement(descriptor, Method.class, loaders);
	}

	default Constructor<?> forConstructor(String descriptor, ClassLoader... loaders) throws ReflectionsException {
		return forElement(descriptor, Constructor.class, loaders);
	}

	@Nullable
	default Field forField(String descriptor, ClassLoader... loaders) {
		return forElement(descriptor, Field.class, loaders);
	}

	default <T> Collection<T> forNames(Collection<String> names, Class<T> resultType, ClassLoader... loaders) {
		return names.stream().map(name -> forName(name, resultType, loaders)).filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	default Collection<Class<?>> forNames(Collection<String> names, ClassLoader... loaders) {
		return forNames(names, (Class) Class.class, loaders);
	}
}
