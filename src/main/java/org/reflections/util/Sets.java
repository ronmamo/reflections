package org.reflections.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unchecked")
public final class Sets {

	private Sets() {
	}
	
	public static <T> Set<T> newHashSet(T... ts) {
		Set<T> set = new HashSet<>();
		for(T t : ts) {
			set.add(t);
		}
		return set;
	}
	
	public static <T> Set<T> newHashSet(Iterable<T> iterable) {
		Set<T> set = new HashSet<>();
		for(T t : iterable) {
			set.add(t);
		}
		return set;
	}
	
	public static <T> Set<T> newLinkedHashSet(T... ts) {
		Set<T> set = new LinkedHashSet<>();
		for(T t : ts) {
			set.add(t);
		}
		return set;
	}

	public static Set<String> newSetFromMap(ConcurrentHashMap<String, Boolean> map) {
		return Collections.newSetFromMap(map);
	}
	
	public static <E> SetView<E> difference(Set<E> a, Set<E> b) {
		return new SetView<E>() {
			@Override
			public Iterator<E> iterator() {
				return new AbstractIterator<E>() {
					Iterator<E> itr = a.iterator();
					@Override
					protected E computeNext() {
						while (itr.hasNext()) {
							E e = itr.next();
							if (!b.contains(e)) {
								return e;
							}
				        }
						return endOfData();
					}
				};
			}
		};
	}
	
	public static interface SetView<T> extends Iterable<T> {
		
		Iterator<T> iterator();
		
	}
	
}
