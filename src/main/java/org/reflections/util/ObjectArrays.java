package org.reflections.util;

import java.lang.reflect.Array;

@SuppressWarnings("unchecked")
public final class ObjectArrays {

	private ObjectArrays() {
	}

	public static <T> T[] concat(T[] first, T[] second, Class<T> type) {
		T[] result = newArray(type, first.length + second.length);
		System.arraycopy(first, 0, result, 0, first.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}

	public static <T> T[] newArray(Class<T> type, int length) {
		return (T[]) Array.newInstance(type, length);
	}

}
