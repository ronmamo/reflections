package com.tvd12.reflections.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.function.Supplier;

public final class Multimaps {

	private Multimaps() {
	}

	public static <K,V> SetMultimap<K, V> newSetMultimap(
			HashMap<K, Collection<V>> map,
	        Supplier<Set<V>> factory) {
		return new SetMultimapProxy<K, V>(map, factory);
	}

	public static SetMultimap<String, String> synchronizedSetMultimap(SetMultimap<String, String> multimap) {
		return new SynchronizedSetMultimap<>(multimap);
	}
	
}
