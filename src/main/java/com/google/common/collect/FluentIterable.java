package com.google.common.collect;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
 * Created by RapidPM - Team on 19.09.16.
 */
public abstract class FluentIterable<E> implements Iterable<E> {


  /**
   * Returns a fluent iterable that combines two iterables. The returned iterable has an iterator
   * that traverses the elements in {@code a}, followed by the elements in {@code b}. The source
   * iterators are not polled until necessary.
   * <p>
   * <p>The returned iterable's iterator supports {@code remove()} when the corresponding input
   * iterator supports it.
   * <p>
   * <p><b>{@code Stream} equivalent:</b> { Stream#concat}.
   *
   * @since 20.0
   */
  public static <T> Iterable<T> concat(Iterable<? extends T> a, Iterable<? extends T> b) {
    return Stream
            .concat(
                    StreamSupport.stream(a.spliterator(), false),
                    StreamSupport.stream(b.spliterator(), false))
            .collect(Collectors.toList());
//    return concat(ImmutableList.of(a, b));
  }


  /**
   * Returns a fluent iterable that combines several iterables. The returned iterable has an
   * iterator that traverses the elements of each iterable in {@code inputs}. The input iterators
   * are not polled until necessary.
   * <p>
   * <p>The returned iterable's iterator supports {@code remove()} when the corresponding input
   * iterator supports it. The methods of the returned iterable may throw {@code
   * NullPointerException} if any of the input iterators is {@code null}.
   * <p>
   * <p><b>{@code Stream} equivalent:</b> {@code streamOfStreams.flatMap(s -> s)} or {@code
   * streamOfIterables.flatMap(Streams::stream)}. (See { Streams#stream}.)
   *
   * @since 20.0
   */
  public static <T> Iterable<T> concat(final Iterable<Iterable<T>> inputs) {


    return StreamSupport
            .stream(inputs.spliterator(), false)
            .filter(item -> item != null)
            .flatMap(itreable -> StreamSupport.stream(itreable.spliterator(), false))
            .collect(Collectors.toList());
  }
}
