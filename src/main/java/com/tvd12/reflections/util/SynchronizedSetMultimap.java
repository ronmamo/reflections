package com.tvd12.reflections.util;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class SynchronizedSetMultimap<K, V> implements SetMultimap<K, V> {
	
	protected final SetMultimap<K, V> realMultimap;
	
	public SynchronizedSetMultimap(SetMultimap<K, V> multimap) {
		this.realMultimap = multimap;
	}

	@Override
	public boolean put(K key, V item) {
		synchronized (realMultimap) {
			return realMultimap.put(key, item);
		}
	}
	
	@Override
	public boolean putAll(Multimap<K, V> multimap) {
		synchronized (realMultimap) {
			return this.realMultimap.putAll(multimap);
		}
	}
	
	@Override
	public Collection<V> get(K key) {
		synchronized (realMultimap) {
			return realMultimap.get(key);
		}
	}
	
	@Override
	public Set<K> keySet() {
		synchronized (realMultimap) {
			return realMultimap.keySet();
		}
	}
	
	@Override
	public Collection<V> values() {
		synchronized (realMultimap) {
			return realMultimap.values();
		}
	}
	
	@Override
	public Iterable<Entry<K, V>> entries() {
		synchronized (realMultimap) {
			return realMultimap.entries();
		}
	}

	@Override
	public Map<K, Collection<V>> asMap() {
		synchronized (realMultimap) {
			return realMultimap.asMap();
		}
	}
	
	@Override
	public int size() {
		synchronized (realMultimap) {
			return realMultimap.size();
		}
	}
	
	@Override
	public boolean isEmpty() {
		synchronized (realMultimap) {
			return realMultimap.isEmpty();
		}
	}
}
