package com.google.common.collect;


import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

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
public class Multimaps {

  private Multimaps() {
  }

  public static <KEY, VALUES> Multimap<KEY, VALUES> newSetMultimap(final Map<KEY, Collection<VALUES>> map,
                                                                   final Supplier<Collection<VALUES>> supplier) {
    return new Multimap<KEY, VALUES>(supplier);
  }

  //  public static <KEY,VALUES> Multimap<KEY, VALUES> synchronizedSetMultimap(final SetMultimap<KEY, VALUES> multimap) {
  public static <KEY, VALUES> Multimap<KEY, VALUES> synchronizedSetMultimap(final Multimap<KEY, VALUES> multimap) {
    return null;
  }


}
