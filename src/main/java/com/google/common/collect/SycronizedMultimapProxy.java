package com.google.common.collect;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Created by benjamin-bosch on 07.11.16.
 */
public class SycronizedMultimapProxy<KEY, VALUES> implements Multimap<KEY, VALUES> {

  private final Multimap<KEY, VALUES> delegate;
  private final Object mutex = new Object();

  public SycronizedMultimapProxy(Multimap<KEY, VALUES> multimap) {
    this.delegate = multimap;
  }


  @Override
  public boolean put(@Nullable KEY key, @Nullable VALUES value) {
    synchronized (mutex) {
      return delegate.put(key, value);
    }
  }

  @Override
  public boolean isEmpty() {
    synchronized (mutex) {
      return delegate.isEmpty();
    }
  }

  @Override
  public Set<KEY> keySet() {
    synchronized (mutex) {
      return delegate.keySet();
    }
  }

  @Override
  public Iterable<? extends Map.Entry<KEY, VALUES>> entries() {
    synchronized (mutex) {
      return delegate.entries();
    }
  }

  @Override
  public Collection<VALUES> get(KEY key) {
    synchronized (mutex) {
      return delegate.get(key);
    }
  }

  @Override
  public int size() {
    synchronized (mutex) {
      return delegate.size();
    }
  }

  @Override
  public Collection<VALUES> values() {
    synchronized (mutex) {
      return delegate.values();
    }
  }

  @Override
  public boolean putAll(Multimap<KEY, VALUES> multimap) {
    synchronized (mutex) {
      return delegate.putAll(multimap);
    }
  }

  @Override
  public boolean putAll(@Nullable KEY key, Iterable<? extends VALUES> values) {
    synchronized (mutex) {
      return delegate.putAll(key, values);
    }
  }

  @Override
  public Map<KEY, Collection<VALUES>> asMap() {
    synchronized (mutex) {
      return delegate.asMap();
    }
  }
}
