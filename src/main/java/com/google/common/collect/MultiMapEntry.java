package com.google.common.collect;

import java.util.Map;

/**
 * Created by benjamin-bosch on 02.10.16.
 */
public class MultiMapEntry<KEY, VALUE> implements Map.Entry<KEY, VALUE> {

  private KEY key;
  private VALUE value;

  public MultiMapEntry(KEY key, VALUE value) {
    this.key = key;
    this.value = value;
  }

  @Override
  public KEY getKey() {
    return key;
  }

  @Override
  public VALUE getValue() {
    return value;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    MultiMapEntry<?, ?> that = (MultiMapEntry<?, ?>) o;

    if (key != null ? !key.equals(that.key) : that.key != null) return false;
    return value != null ? value.equals(that.value) : that.value == null;

  }

  @Override
  public int hashCode() {
    int result = key != null ? key.hashCode() : 0;
    result = 31 * result + (value != null ? value.hashCode() : 0);
    return result;
  }

  @Override
  public VALUE setValue(VALUE value) {
    this.value = value;
    return this.value;
  }


}
