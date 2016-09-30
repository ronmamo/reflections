package com.google.common.collect;

import java.lang.reflect.Array;

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
public class ObjectArrays {

  private ObjectArrays() {
  }

  public static <T> T[] concat(final T[] arrayA, final T[] arrayB, final Class<T> arrayTypeClass) {
    T[] result = newArray(arrayTypeClass, arrayA.length + arrayB.length);
    System.arraycopy(arrayA, 0, result, 0, arrayA.length);
    System.arraycopy(arrayB, 0, result, arrayA.length, arrayB.length);
    return result;
  }

  public static <T> T[] newArray(Class<T> type, int length) {
    return (T[]) Array.newInstance(type, length);
  }

}
