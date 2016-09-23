package com.google.common.collect;

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
public abstract class FluentIterable<E> implements Iterable<E> {
  public static <T> Iterable<T> concat(final Iterable<? extends T> a, final Iterable<? extends T> b) {
    return null;
  }

  public static <T> Iterable<T> concat(final Iterable<? extends Iterable<? extends T>> iterables) {
    return null;
  }
}
