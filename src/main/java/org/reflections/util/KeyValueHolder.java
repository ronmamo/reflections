package org.reflections.util;

import java.util.Map;
import java.util.Objects;

public class KeyValueHolder<K,V> implements Map.Entry<K,V> {
    final K key;
    final V value;
    KeyValueHolder(K k, V v) {
        key = Objects.requireNonNull(k);
        value = Objects.requireNonNull(v);
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
    public V setValue(final V value) {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Map.Entry))
            return false;
        Map.Entry<?,?> e = (Map.Entry<?,?>)o;
        return key.equals(e.getKey()) && value.equals(e.getValue());
    }

    @Override
    public int hashCode() {
        return key.hashCode() ^ value.hashCode();
    }

    @Override
    public String toString() {
        return key + "=" + value;
    }
}
