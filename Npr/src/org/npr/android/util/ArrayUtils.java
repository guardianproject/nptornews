// Copyright 2011 NPR
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.npr.android.util;

import java.lang.reflect.Array;

/**
 * Author: Jeremy Wadsack
 */
public class ArrayUtils {
  @SuppressWarnings("unused")
  private static final String LOG_TAG = ArrayUtils.class.getName();

  public static int indexOf(int[] list, int item) {
    for (int i = 0; i < list.length; i++) {
      if (list[i] == item) {
        return i;
      }
    }
    return -1;
  }

  public static int indexOf(Object[] list, Object item) {
    if (item == null) {
      return -1;
    }
    for (int i = 0; i < list.length; i++) {
      if (list[i].equals(item)) {
        return i;
      }
    }
    return -1;
  }


  // Missing when run on Android SDK
  /**
   * Copies the specified array, truncating or padding with nulls (if necessary)
   * so the copy has the specified length. For all indices that are valid in
   * both the original array and the copy, the two arrays will contain identical
   * values. For any indices that are valid in the copy but not the original,
   * the copy will contain null. Such indices will exist if and only if the
   * specified length is greater than that of the original array. The
   * resulting array is of exactly the same class as the original array.
   *
   * @param original the array to be copied
   * @param newLength the length of the copy to be returned
   * @return a copy of the original array, truncated or padded with nulls to
   * obtain the specified length
   */
  public static<T> T[] copyOf(T[] original, int newLength) {
    @SuppressWarnings({"unchecked"})
    T[] copy = (T[]) Array.newInstance(original.getClass().getComponentType(),
        newLength);
    System.arraycopy(original, 0, copy,  0,
        Math.min(original.length, newLength));
    return copy;
  }

}
