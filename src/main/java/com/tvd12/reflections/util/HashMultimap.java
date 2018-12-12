package com.tvd12.reflections.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class HashMultimap<K, V> extends AbstractMultimap<K, V> {

	public HashMultimap(
			Map<K, ? extends Collection<V>> map, 
			Supplier<? extends Collection<V>> factory) {
		super(map, factory);
	}

	public static <K, V> HashMultimap<K, V> create() {
		return new HashMultimap<K, V>(
				new HashMap<>(),
				new Supplier<Set<V>>() {
					@Override
					public Set<V> get() {
						return new HashSet<>();
					}
				});
	}
	
}
