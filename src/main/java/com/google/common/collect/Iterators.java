package com.google.common.collect;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

import java.util.Collection;
import java.util.Iterator;

import static com.google.common.base.Predicates.equalTo;

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


  /**
   * Returns the number of elements remaining in {@code iterator}. The iterator
   * will be left exhausted: its {@code hasNext()} method will return
   * {@code false}.
   */
  public static int size(Iterator<?> iterator) {
    long count = 0L;
    while (iterator.hasNext()) {
      iterator.next();
      count++;
    }
    return saturatedCast(count);
  }


  /**
   * Returns the {@code int} nearest in value to {@code value}.
   *
   * @param value any {@code long} value
   *
   * @return the same value cast to {@code int} if it is in the range of the {@code int} type,
   * {@link Integer#MAX_VALUE} if it is too large, or {@link Integer#MIN_VALUE} if it is too
   * small
   */
  public static int saturatedCast(long value) {
    if (value > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    }
    if (value < Integer.MIN_VALUE) {
      return Integer.MIN_VALUE;
    }
    return (int) value;
  }

  /**
   * Returns a view containing the result of applying {@code function} to each
   * element of {@code fromIterator}.
   *
   * <p>The returned iterator supports {@code remove()} if {@code fromIterator}
   * does. After a successful {@code remove()} call, {@code fromIterator} no
   * longer contains the corresponding element.
   */
  public static <F, T> Iterator<T> transform(
      final Iterator<F> fromIterator, final Function<? super F, ? extends T> function) {
    return new TransformedIterator<F, T>(fromIterator) {
      @Override
      T transform(F from) {
        return function.apply(from);
      }
    };
  }

  public static <T> T getOnlyElement(final Iterator<T> iterator) {
    T first = iterator.next();
    if (!iterator.hasNext()) {
      return first;
    }

    StringBuilder sb = new StringBuilder().append("expected one element but was: <").append(first);
    for (int i = 0; i < 4 && iterator.hasNext(); i++) {
      sb.append(", ").append(iterator.next());
    }
    if (iterator.hasNext()) {
      sb.append(", ...");
    }
    sb.append('>');

    throw new IllegalArgumentException(sb.toString());
  }

  public static <T> boolean any(Iterator<T> iterator, Predicate<? super T> predicate) {
    return indexOf(iterator, predicate) != -1;
  }

  public static boolean contains(final Iterator<?> iterator, final Object element) {
    return any(iterator, equalTo(element));
  }

  public static <T> Iterator<T> limit(final Iterator<T> iterator, final int limitSize) {
    return null;
  }

  /**
   * An iterator that transforms a backing iterator; for internal use. This avoids
   * the object overhead of constructing a {@link Function} for internal methods.
   *
   * @author Louis Wasserman
   */
  abstract static class TransformedIterator<F, T> implements Iterator<T> {
    final Iterator<? extends F> backingIterator;

    TransformedIterator(Iterator<? extends F> backingIterator) {
      this.backingIterator = backingIterator;
    }

    abstract T transform(F from);

    @Override
    public final boolean hasNext() {
      return backingIterator.hasNext();
    }

    @Override
    public final T next() {
      return transform(backingIterator.next());
    }

    @Override
    public final void remove() {
      backingIterator.remove();
    }
  }

  /**
   * Returns the index in {@code iterator} of the first element that satisfies
   * the provided {@code predicate}, or {@code -1} if the Iterator has no such
   * elements.
   *
   * <p>More formally, returns the lowest index {@code i} such that
   * {@code predicate.apply(Iterators.get(iterator, i))} returns {@code true},
   * or {@code -1} if there is no such index.
   *
   * <p>If -1 is returned, the iterator will be left exhausted: its
   * {@code hasNext()} method will return {@code false}.  Otherwise,
   * the iterator will be set to the element which satisfies the
   * {@code predicate}.
   *
   * @since 2.0
   */
  public static <T> int indexOf(Iterator<T> iterator, Predicate<? super T> predicate) {
    for (int i = 0; iterator.hasNext(); i++) {
      T current = iterator.next();
      if (predicate.apply(current)) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Adds all elements in {@code iterator} to {@code collection}. The iterator
   * will be left exhausted: its {@code hasNext()} method will return
   * {@code false}.
   *
   * @return {@code true} if {@code collection} was modified as a result of this
   * operation
   */
  public static <T> boolean addAll(Collection<T> addTo, Iterator<? extends T> iterator) {
    boolean wasModified = false;
    while (iterator.hasNext()) {
      wasModified |= addTo.add(iterator.next());
    }
    return wasModified;
  }

}
