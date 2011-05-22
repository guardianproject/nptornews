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

/**
 * A bunch of common utility functions that should be extension methods when Java 7 comes around
 *
 * Author: Jeremy Wadsack
 */
public class StringUtils {

    public static String join(String separator, String[] strings) {
        if (strings == null) {
            return null;
        }
        if (strings.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder(strings[0]);
        for (int i = 1, length = strings.length; i < length; i++) {
            builder.append(separator);
            builder.append(strings[i]);
        }
        return builder.toString();
    }

  public static String join(String separator, Integer[] integers) {
    if (integers == null) {
        return null;
    }
    if (integers.length == 0) {
        return "";
    }
    StringBuilder builder = new StringBuilder(integers[0].toString());
    for (int i = 1, length = integers.length; i < length; i++) {
        builder.append(separator);
        builder.append(integers[i]);
    }
    return builder.toString();
  }
}
