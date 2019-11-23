package org.reflections.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class SynchronizedSetMultimap<K, V> extends SetMultimap<K,V> {

    private final SetMultimap<K, V> map;

    public SynchronizedSetMultimap(final SetMultimap<K,V> map) {
        this.map = map;
    }

    @Override
    public synchronized boolean put(final K key, final V value) {
        return map.put(key, value);
    }

    @Override
    public synchronized Map<K, Collection<V>> asMap() {
        return map.asMap();
    }

    @Override
    public synchronized Collection<V> get(final K key) {
        return map.get(key);
    }

    @Override
    public synchronized Set<K> keySet() {
        return map.keySet();
    }

    @Override
    public synchronized Collection<V> values() {
        return map.values();
    }

    @Override
    public synchronized void putAll(final Multimap<K, V> expand) {
        map.putAll(expand);
    }
}
