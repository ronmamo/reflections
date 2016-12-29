package com.google.common.base;

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
public interface Function<F, T> {
  /**
   * Returns the result of applying this function to {@code input}. This method is <i>generally
   * expected</i>, but not absolutely required, to have the following properties:
   *
   * <ul>
   * <li>Its execution does not cause any observable side effects.
   * <li>The computation is <i>consistent with equals</i>; that is, {@link Objects#equal
   * Objects.equal}{@code (a, b)} implies that {@code Objects.equal(function.apply(a),
   * function.apply(b))}.
   * </ul>
   *
   * @throws NullPointerException if {@code input} is null and this function does not accept null
   *                              arguments
   */
  T apply(F input);

  /**
   * Indicates whether another object is equal to this function.
   *
   * <p>Most implementations will have no reason to override the behavior of {@link Object#equals}.
   * However, an implementation may also choose to return {@code true} whenever {@code object} is a
   * {@link Function} that it considers <i>interchangeable</i> with this one. "Interchangeable"
   * <i>typically</i> means that {@code Objects.equal(this.apply(f), that.apply(f))} is true for all
   * {@code f} of type {@code F}. Note that a {@code false} result from this method does not imply
   * that the functions are known <i>not</i> to be interchangeable.
   */
  @Override
  boolean equals(Object object);
}

