package com.google.common.collect;

import com.google.common.base.Predicate;

import java.util.Iterator;

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
 * Created by RapidPM - Team on 19.09.16.
 */
public class Iterators {

  private Iterators() {
  }

  /**
   * Returns a view of {@code unfiltered} containing all elements that satisfy
   * the input predicate {@code retainIfTrue}.
   */
  public static <T> Iterator<T> filter(
      final Iterator<T> unfiltered, final Predicate<? super T> retainIfTrue) {
    if (unfiltered == null || retainIfTrue == null) throw new NullPointerException();
    return new AbstractIterator<T>() {
      @Override
      protected T computeNext() {
        while (unfiltered.hasNext()) {
          T element = unfiltered.next();
          if (retainIfTrue.apply(element)) {
            return element;
          }
        }
        return endOfData();
      }
    };
  }


  public static <E> int size(final Iterator<E> iterator) {
    return 0;
  }
}
