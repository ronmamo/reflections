package com.google.common.collect;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Copyright (C) 2010 RapidPM
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * Created by RapidPM - Team on 18.09.16.
 */
public class MultimapImpl<KEY, VALUES> implements Multimap<KEY, VALUES> {


  private final Map<KEY, Collection<VALUES>> multimap;

  private int totalSize;


  public MultimapImpl(Map<KEY, Collection<VALUES>> map, Supplier<Collection<VALUES>> supplier) {
    this.multimap = map;
    this.valuesSupplier = supplier;
  }

  public MultimapImpl() {
    this.multimap = new HashMap<>();
  }

  @Override
  public boolean put(@Nullable KEY key, @Nullable VALUES value) {
    Collection<VALUES> collection = multimap.get(key);
    if (collection == null) {
      collection = createCollection(key);
      if (collection.add(value)) {
        totalSize++;
        multimap.put(key, collection);
        return true;
      } else {
        throw new AssertionError("New Collection violated the Collection spec");
      }
    } else if (collection.add(value)) {
      totalSize++;
      return true;
    } else {
      return false;
    }
  }

  private Supplier<Collection<VALUES>> valuesSupplier = HashSet::new;

  private Collection<VALUES> createCollection(final KEY key) {
    multimap.put(key, valuesSupplier.get());
    return multimap.get(key);
  }


  @Override
  public boolean isEmpty() {
    return multimap.isEmpty();
  }

  @Override
  public Set<KEY> keySet() {
    return multimap.keySet();
  }

  @Override
  public Iterable<? extends Map.Entry<KEY, VALUES>> entries() {
    return multimap.keySet().stream()
            .flatMap(key -> multimap.get(key)
                    .stream()
                    .map(value ->
                            (Map.Entry<KEY, VALUES>) new MultiMapEntry(key, value)
                    )
            )
            .collect(Collectors.toList());
  }

  @Override
  public Collection<VALUES> get(final KEY key) {
    return multimap.get(key) != null ? multimap.get(key) : createCollection(key);

  }

  @Override
  public int size() {
    return totalSize;
  }

  @Override
  public Collection<VALUES> values() {
    return multimap
            .values()
            .stream()
            .flatMap(values -> values.stream())
            .collect(Collectors.toList());
  }

  @Override
  public boolean putAll(Multimap<KEY, VALUES> multimap) {
    boolean changed = false;
    Iterable<? extends Map.Entry<KEY, VALUES>> entries = multimap.entries();
    for (Map.Entry<KEY, VALUES> entry : entries) {
      changed |= put(entry.getKey(), entry.getValue());
    }
    return changed;
  }


  @Override
  public boolean putAll(@Nullable KEY key, Iterable<? extends VALUES> values) {
    // make sure we only call values.iterator() once
    // and we only call get(key) if values is nonempty
    if (values instanceof Collection) {
      final Collection<? extends VALUES> valueCollection = (Collection<? extends VALUES>) values;
      return !valueCollection.isEmpty() && get(key).addAll(valueCollection);
    } else {
      Iterator<? extends VALUES> valueItr = values.iterator();
      return valueItr.hasNext() && Iterators.addAll(get(key), valueItr);
    }
  }

  @Override
  public Map<KEY, Collection<VALUES>> asMap() {
    return multimap;
  }
}
