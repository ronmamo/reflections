package com.google.common.collect;


import com.google.common.base.Function;
import com.google.common.base.Predicate;

import java.util.Collection;
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
public class Iterables {


  private Iterables() {
  }

  /**
   * Returns a view of {@code unfiltered} containing all elements that satisfy
   * the input predicate {@code retainIfTrue}. The returned iterable's iterator
   * does not support {@code remove()}.
   */
  public static <T> Iterable<T> filter(
      final Iterable<T> unfiltered,
      final Predicate<? super T> retainIfTrue) {

    if (unfiltered == null || retainIfTrue == null) throw new NullPointerException();
    return new FluentIterable<T>() {
      @Override
      public Iterator<T> iterator() {
        return Iterators.filter(unfiltered.iterator(), retainIfTrue);
      }
    };
  }


  /**
   * Combines two iterables into a single iterable. The returned iterable has an
   * iterator that traverses the elements in {@code a}, followed by the elements
   * in {@code b}. The source iterators are not polled until necessary.
   *
   * <p>The returned iterable's iterator supports {@code remove()} when the
   * corresponding input iterator supports it.
   */
  public static <T> Iterable<T> concat(Iterable<? extends T> a, Iterable<? extends T> b) {
    return FluentIterable.concat(a, b);
  }


  /**
   * Combines multiple iterables into a single iterable. The returned iterable
   * has an iterator that traverses the elements of each iterable in
   * {@code inputs}. The input iterators are not polled until necessary.
   *
   * <p>The returned iterable's iterator supports {@code remove()} when the
   * corresponding input iterator supports it. The methods of the returned
   * iterable may throw {@code NullPointerException} if any of the input
   * iterators is null.
   */
  public static <T> Iterable<T> concat(Iterable<Iterable<T>> inputs) {
    return FluentIterable.concat(inputs);
  }


  public static <T> boolean isEmpty(final Iterable<T> iterable) {
    if (iterable instanceof Collection) {
      return ((Collection<?>) iterable).isEmpty();
    }
    return !iterable.iterator().hasNext();
  }

  public static <T> T getOnlyElement(final Iterable<T> iterable) {
    final Iterator<T> iterator = iterable.iterator();
    return Iterators.getOnlyElement(iterator);
  }

  public static <T> boolean any(Iterable<T> iterable, Predicate<? super T> predicate) {
    return Iterators.any(iterable.iterator(), predicate);
  }

  public static boolean contains(final Iterable<?> iterable, final Object element) {
    if (iterable instanceof Collection) {
      Collection<?> collection = (Collection<?>) iterable;
      return Collections2.safeContains(collection, element);
    }
    return Iterators.contains(iterable.iterator(), element);
  }

  public static <F, T> Iterable<T> transform(
      final Iterable<F> fromIterable, final Function<? super F, ? extends T> function) {
    return new FluentIterable<T>() {
      @Override
      public Iterator<T> iterator() {
        return Iterators.transform(fromIterable.iterator(), function);
      }
    };
  }

  public static <T> Iterable<T> limit(final Iterable<T> iterable, final int limitSize) {
    return new FluentIterable<T>() {
      @Override
      public Iterator<T> iterator() {
        return Iterators.limit(iterable.iterator(), limitSize);
      }
    };
  }
}
