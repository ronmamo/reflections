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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JavassistHelper {
	/** setting this static to false will result in returning only {@link java.lang.annotation.RetentionPolicy#RUNTIME} visible annotation */
	public static boolean includeInvisibleTag = true;

	public static List<String> getAnnotations(Function<String, AttributeInfo> function) {
		List<String> list = getAnnotations((AnnotationsAttribute) function.apply(AnnotationsAttribute.visibleTag));
		if (includeInvisibleTag) list.addAll(getAnnotations((AnnotationsAttribute) function.apply(AnnotationsAttribute.invisibleTag)));
		return list;
	}

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

	public static List<List<String>> getParametersAnnotations(MethodInfo method) {
		List<List<String>> list = getAnnotations((ParameterAnnotationsAttribute) method.getAttribute(ParameterAnnotationsAttribute.visibleTag));
		if (includeInvisibleTag) list.addAll(getAnnotations((ParameterAnnotationsAttribute) method.getAttribute(ParameterAnnotationsAttribute.invisibleTag)));
		return list;
	}

	private static List<List<String>> getAnnotations(ParameterAnnotationsAttribute attribute) {
		return mapList(attribute, ParameterAnnotationsAttribute::getAnnotations, aa -> mapList(aa, a -> a, Annotation::getTypeName));
	}

	private static List<String> getAnnotations(AnnotationsAttribute attribute) {
		return mapList(attribute, AnnotationsAttribute::getAnnotations, Annotation::getTypeName);
	}

	// todo inline & simplify
	private static <T, A, R> List<R> mapList(T t, Function<T, A[]> f1, Function<A, R> f2) {
		return t != null ? Arrays.stream(f1.apply(t)).map(f2).collect(Collectors.toList()) : Collections.emptyList();
	}
}
