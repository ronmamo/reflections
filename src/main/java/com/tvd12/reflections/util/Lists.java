package com.tvd12.reflections.util;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("unchecked")
public final class Lists {

	private Lists() {
	}

	public static <T> List<T> newArrayList(T... ts) {
		List<T> list = new ArrayList<>();
		for(T t : ts)
			list.add(t);
		return list;
	}
	
	public static <T> List<T> newArrayList(Iterable<T> iterable) {
		List<T> list = new ArrayList<>();
		for(T t : iterable)
			list.add(t);
		return list;
	}

	public static <T> LinkedList<T> newLinkedList() {
		return new LinkedList<>();
	}
	
}
