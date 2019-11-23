package org.reflections.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class SetMultimap<K, V> implements Multimap<K, V> {
//new HashMap<String, Collection<String>>(), HashSet::new
    @Override
    public boolean put(final K key, final V value) {
        return false;
    }

    @Override
    public Map<K, Collection<V>> asMap() {
        return null;
    }

    @Override
    public Collection<V> get(final K key) {
        return null;
    }

    @Override
    public Set<K> keySet() {
        return null;
    }

    @Override
    public Collection<V> values() {
        return null;
    }

    @Override
    public void putAll(final Multimap<K, V> expand) {

    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public Iterable<Map.Entry<K, V>> entries() {
        return null;
    }
}
