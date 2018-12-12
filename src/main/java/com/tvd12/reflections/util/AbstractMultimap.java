package com.tvd12.reflections.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Supplier;

@SuppressWarnings({"rawtypes", "unchecked"})
public class AbstractMultimap<K, V> implements Multimap<K, V> {

	protected final Map map;
	protected final Supplier factory;
	
	public AbstractMultimap(
			Map<K, ? extends Collection<V>> map, 
			Supplier<? extends Collection<V>> factory) {
		this.map = map;
		this.factory = factory;
	}
	
	@Override
	public boolean put(K key, V item) {
		Collection<V> value = get(key);
		boolean answer = value.add(item);
		return answer;
	}
	
	@Override
	public boolean putAll(Multimap<K, V> multimap) {
		boolean changed = false;
	    for (Entry<? extends K, ? extends V> entry : multimap.entries()) {
	      changed |= put(entry.getKey(), entry.getValue());
	    }
	    return changed;
	}
	
	@Override
	public Collection<V> get(K key) {
		Object answer = map.get(key);
		if(answer == null) {
			answer = factory.get();
			map.put(key, answer);
		}
		return (Collection<V>)answer;
	}
	
	@Override
	public Set<K> keySet() {
		return map.keySet();
	}
	
	@Override
	public Collection<V> values() {
		return map.values();
	}
	
	@Override
	public Iterable<Entry<K, V>> entries() {
		Set<Entry<K, V>> set = new HashSet<>();
		for(Object key : map.keySet()) {
			Collection<V> items = (Collection<V>) map.get(key);
			for(V value : items)
				set.add(new ImmutableEntry(key, value));
		}
		return set;
	}
	
	@Override
	public Map<K, Collection<V>> asMap() {
		return map;
	}
	
	@Override
	public int size() {
		return map.size();
	}
	
	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}
	
}
