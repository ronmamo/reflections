package org.reflections.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface Multimap<K,V>  {
    boolean put(K key, V value);
    Map<K, Collection<V>> asMap();
    Collection<V> get(K key);
    Set<K> keySet();
    Collection<V> values();
    void putAll(Multimap<K, V> expand);
    boolean isEmpty();

    Iterable<Map.Entry<K, V>> entries();
}
