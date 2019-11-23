package org.reflections.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class HashMultimap<K,V> implements Multimap<K, V> {

    private final HashMap<K, ArrayList<V>> internal;

    public HashMultimap() {
        internal = new HashMap<>();
    }

    @Override
    public boolean put(final K key, final V value) {
        final ArrayList<V> list;
        if(internal.containsKey(key)) {
            list = internal.get(key);
        } else {
            list = new ArrayList<>();
            internal.put(key, list);
        }
        return list.add(value);
    }

    @Override
    public Map<K, Collection<V>> asMap() {
        return Collections.unmodifiableMap(internal);
    }

    @Override
    public Collection<V> get(final K key) {
        return internal.get(key);
    }

    @Override
    public Set<K> keySet() {
        return internal.keySet();
    }

    @Override
    public Collection<V> values() {
        return internal.values().stream().map(Collection::stream)
                .flatMap(s->s).collect(Collectors.toSet());
    }

    @Override
    public void putAll(final Multimap<K, V> expand) {
        for(final Map.Entry<K, V> entry : expand.entries()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public boolean isEmpty() {
        return internal.isEmpty();
    }

    @Override
    public Iterable<Map.Entry<K, V>> entries() {
        return internal.entrySet().stream().map(this::entryFactory)
                .map(List::stream)
                .flatMap(s->s)
                .collect(Collectors.toSet());
    }

    private List<Map.Entry<K, V>> entryFactory(final Map.Entry<K, ArrayList<V>> entry) {
        return entry.getValue().stream()
                .map(v -> new KeyValueHolder<>(entry.getKey(), v)).collect(Collectors.toList());
    }
}
