package org.reflections.util;

import javassist.bytecode.AccessFlag;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.AttributeInfo;
import javassist.bytecode.ClassFile;
import javassist.bytecode.Descriptor;
import javassist.bytecode.FieldInfo;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.ParameterAnnotationsAttribute;
import javassist.bytecode.annotation.Annotation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JavassistHelper {
	/** setting this static to false will result in returning only {@link java.lang.annotation.RetentionPolicy#RUNTIME} visible annotation */
	public static boolean includeInvisibleTag = true;

	public static String fieldName(ClassFile classFile, FieldInfo object) {
		return String.format("%s.%s", classFile.getName(), object.getName());
	}

	public static String methodName(ClassFile classFile, MethodInfo object) {
		return String.format("%s.%s(%s)", classFile.getName(), object.getName(), String.join(", ", getParameters(object)));
	}

	public static boolean isPublic(Object object) {
		if (object instanceof ClassFile) return AccessFlag.isPublic(((ClassFile) object).getAccessFlags());
		if (object instanceof FieldInfo) return AccessFlag.isPublic(((FieldInfo) object).getAccessFlags());
		if (object instanceof MethodInfo) return AccessFlag.isPublic(((MethodInfo) object).getAccessFlags());
		return false;
	}

	public static Stream<MethodInfo> getMethods(ClassFile classFile) {
		return classFile.getMethods().stream().filter(MethodInfo::isMethod);
	}

	public static Stream<MethodInfo> getConstructors(ClassFile classFile) {
		return classFile.getMethods().stream().filter(methodInfo -> !methodInfo.isMethod());
	}

	public static List<String> getParameters(MethodInfo method) {
		List<String> result = new ArrayList<>();
		String descriptor = method.getDescriptor().substring(1);
		Descriptor.Iterator iterator = new Descriptor.Iterator(descriptor);
		Integer prev = null;
		while (iterator.hasNext()) {
			int cur = iterator.next();
			if (prev != null) result.add(Descriptor.toString(descriptor.substring(prev, cur)));
			prev = cur;
		}
		return result;
	}

	public static String getReturnType(MethodInfo method) {
		String descriptor = method.getDescriptor();
		descriptor = descriptor.substring(descriptor.lastIndexOf(")") + 1);
		return Descriptor.toString(descriptor);
	}

	public static List<String> getAnnotations(Function<String, AttributeInfo> function) {
		Function<String, List<String>> names = function
			.andThen(attribute -> attribute != null ? ((AnnotationsAttribute) attribute).getAnnotations() : null)
			.andThen(JavassistHelper::annotationNames);

		List<String> result = new ArrayList<>(names.apply(AnnotationsAttribute.visibleTag));
		if (includeInvisibleTag) result.addAll(names.apply(AnnotationsAttribute.invisibleTag));
		return result;
	}

	public static List<List<String>> getParametersAnnotations(MethodInfo method) {
		Function<String, List<List<String>>> names = ((Function<String, AttributeInfo>) method::getAttribute)
			.andThen(attribute -> attribute != null ? ((ParameterAnnotationsAttribute) attribute).getAnnotations() : null)
			.andThen((Annotation[][] aa) -> aa != null ? Stream.of(aa).map(JavassistHelper::annotationNames).collect(Collectors.toList()) : Collections.emptyList());

		List<List<String>> visibleAnnotations = names.apply(ParameterAnnotationsAttribute.visibleTag);
		if (!includeInvisibleTag) return new ArrayList<>(visibleAnnotations);

		List<List<String>> invisibleAnnotations = names.apply(ParameterAnnotationsAttribute.invisibleTag);
		if (invisibleAnnotations.isEmpty()) return new ArrayList<>(visibleAnnotations);

		// horror
		List<List<String>> result = new ArrayList<>();
		for (int i = 0; i < Math.max(visibleAnnotations.size(), invisibleAnnotations.size()); i++) {
			List<String> concat = new ArrayList<>();
			if (i < visibleAnnotations.size()) concat.addAll(visibleAnnotations.get(i));
			if (i < invisibleAnnotations.size()) concat.addAll(invisibleAnnotations.get(i));
			result.add(concat);
		}
		return result;
	}

	private static List<String> annotationNames(Annotation[] annotations) {
		return annotations != null ? Stream.of(annotations).map(Annotation::getTypeName).collect(Collectors.toList()) : Collections.emptyList();
	}
}
