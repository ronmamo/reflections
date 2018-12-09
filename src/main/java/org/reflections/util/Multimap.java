package org.reflections.util;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public interface Multimap<K, V> {

	boolean put(K key, V item);
	
	boolean putAll(Multimap<K, V> multimap);
	
	Collection<V> get(K key);
	
	Set<K> keySet();
	
	Collection<V> values();
	
	Iterable<Entry<K, V>> entries();
	
	int size();

	boolean isEmpty();

	Map<K, Collection<V>> asMap();

}
