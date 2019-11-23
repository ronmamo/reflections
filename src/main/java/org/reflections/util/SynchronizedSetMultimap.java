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
        return false;
    }

    @Override
    public synchronized Map<K, Collection<V>> asMap() {
        return null;
    }

    @Override
    public synchronized Collection<V> get(final K key) {
        return null;
    }

    @Override
    public synchronized Set<K> keySet() {
        return null;
    }

    @Override
    public synchronized Collection<V> values() {
        return null;
    }

    @Override
    public synchronized void putAll(final Multimap<K, V> expand) {

    }
}
