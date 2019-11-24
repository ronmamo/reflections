package org.reflections.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SetMultimap<K, V> implements Multimap<K, V> {

    private final HashMap<K, HashSet<V>> internal;

    public SetMultimap() {
        internal = new HashMap<>();
    }

    @Override
    public boolean put(final K key, final V value) {
        return getValuesContainerForKey(key).add(value);
    }

    @Override
    public Map<K, Collection<V>> asMap() {
        return Collections.unmodifiableMap(internal);
    }

    @Override
    public Collection<V> get(final K key) {
        return getValuesContainerForKey(key);
    }

    private Set<V> getValuesContainerForKey(final K key) {
        HashSet<V> set;
        if(internal.containsKey(key)) {
            set = internal.get(key);
        } else {
            set = new HashSet<>();
            internal.put(key, set);
        }
        return set;
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
                .map(Set::stream)
                .flatMap(s->s)
                .collect(Collectors.toSet());
    }

    private Set<Map.Entry<K, V>> entryFactory(final Map.Entry<K, HashSet<V>> entry) {
        return entry.getValue().stream()
                .map(v -> new KeyValueHolder<>(entry.getKey(), v)).collect(Collectors.toSet());
    }

}
