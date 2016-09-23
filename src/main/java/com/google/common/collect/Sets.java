package com.google.common.collect;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;

import static kotlin.jvm.internal.Intrinsics.checkNotNull;

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
public class Sets {
  private Sets() {
  }

  /**
   * Returns an unmodifiable <b>view</b> of the difference of two sets. The
   * returned set contains all elements that are contained by {@code set1} and
   * not contained by {@code set2}. {@code set2} may also contain elements not
   * present in {@code set1}; these are simply ignored. The iteration order of
   * the returned set matches that of {@code set1}.
   *
   * <p>Results are undefined if {@code set1} and {@code set2} are sets based
   * on different equivalence relations (as {@code HashSet}, {@code TreeSet},
   * and the keySet of an {@code IdentityHashMap} all are).
   */
//  public static <E> SetView<E> difference(final Set<E> set1, final Set<?> set2) {
  public static <E> Set<E> difference(final Set<E> set1, final Set<?> set2) {
    checkNotNull(set1, "set1");
    checkNotNull(set2, "set2");

    final Predicate<Object> notInSet2 = Predicates.not(Predicates.in(set2));

    return null;
  }


  /**
   * An unmodifiable view of a set which may be backed by other sets; this view
   * will change as the backing sets do. Contains methods to copy the data into
   * a new set which will then remain stable. There is usually no reason to
   * retain a reference of type {@code SetView}; typically, you either use it
   * as a plain {@link Set}, or immediately invoke {@link #immutableCopy} or
   * {@link #copyInto} and forget the {@code SetView} itself.
   *
   * @since 2.0
   */
  public abstract static class SetView<E> extends AbstractSet<E> {
    private SetView() {
    } // no subclasses but our own

    /**
     * Returns an immutable copy of the current contents of this set view.
     * Does not support null elements.
     *
     * <p><b>Warning:</b> this may have unexpected results if a backing set of
     * this view uses a nonstandard notion of equivalence, for example if it is
     * a {@link java.util.TreeSet} using a comparator that is inconsistent with {@link
     * Object#equals(Object)}.
     */
    public Set<E> immutableCopy() {
      return null;
//      return ImmutableSet.copyOf(this);
    }

    /**
     * Copies the current contents of this set view into an existing set. This
     * method has equivalent behavior to {@code set.addAll(this)}, assuming that
     * all the sets involved are based on the same notion of equivalence.
     *
     * @return a reference to {@code set}, for convenience
     */
    // Note: S should logically extend Set<? super E> but can't due to either
    // some javac bug or some weirdness in the spec, not sure which.
    public <S extends Set<E>> S copyInto(S set) {
      set.addAll(this);
      return set;
    }

    /**
     * Scope the return type to  UnmodifiableIterator {@link } to ensure this is an unmodifiable view.
     *
     * @since 20.0 (present with return type {@link Iterator} since 2.0)
     */
    @Override
    public abstract Iterator<E> iterator();
  }
}
