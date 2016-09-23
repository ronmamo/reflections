package com.google.common.collect;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

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
 *
 * Created by RapidPM - Team on 18.09.16.
 */
public class Multimap<KEY, VALUES> {


  public boolean put(final KEY key, final VALUES value) {
    return false;
  }

  public boolean isEmpty() {
    return false;
  }

  public Set<KEY> keySet() {
    return Collections.emptySet();
  }

  public Iterable<? extends Map.Entry<KEY, VALUES>> entries() {
    return null;
  }

  public Collection<VALUES> get(final KEY key) {
    return null;
  }

  public int size() {
    return 0;
  }

  public Collection<VALUES> values() {
    return Collections.emptySet();
  }

  public boolean putAll(final Multimap<KEY, KEY> expand) {
    return false;
  }

  public Map<KEY, Collection<VALUES>> asMap() {
    return null;
  }
}
