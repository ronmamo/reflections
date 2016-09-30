package com.google.common.collect;

import com.google.common.base.Function;

import javax.annotation.Nullable;
import java.util.AbstractCollection;
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
public class Collections2 {

  private Collections2() {
  }

  public static <F, T> Collection<T> transform(
      Collection<F> fromCollection, Function<? super F, T> function) {
    return new TransformedCollection<>(fromCollection, function);
  }

  static class TransformedCollection<F, T> extends AbstractCollection<T> {
    final Collection<F> fromCollection;
    final Function<? super F, ? extends T> function;

    TransformedCollection(Collection<F> fromCollection, Function<? super F, ? extends T> function) {
      this.fromCollection = fromCollection;
      this.function = function;
    }

    @Override
    public void clear() {
      fromCollection.clear();
    }

    @Override
    public boolean isEmpty() {
      return fromCollection.isEmpty();
    }

    @Override
    public Iterator<T> iterator() {
      return Iterators.transform(fromCollection.iterator(), function);
    }

    @Override
    public int size() {
      return fromCollection.size();
    }
  }


  /**
   * Delegates to {@link Collection#contains}. Returns {@code false} if the
   * {@code contains} method throws a {@code ClassCastException} or
   * {@code NullPointerException}.
   */
  static boolean safeContains(Collection<?> collection, @Nullable Object object) {
    try {
      return collection.contains(object);
    } catch (ClassCastException e) {
      return false;
    } catch (NullPointerException e) {
      return false;
    }
  }

}
