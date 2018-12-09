package org.reflections.util;

import java.util.Map.Entry;

public class ImmutableEntry<K, V> implements Entry<K, V> {
	private final K key;
	private final V value;

	public ImmutableEntry(K key, V value) {
        this.key = key;
        this.value = value;
    }

	@Override
	public K getKey() {
		return key;
	}

	@Override
	public V getValue() {
		return value;
	}

	@Override
	public V setValue(V value) {
		throw new UnsupportedOperationException();
	}
}
