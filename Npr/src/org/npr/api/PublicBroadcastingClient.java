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

package org.npr.api;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * A client library or reading and parsing the data at
 * guide.publicbroadcasting.net.
 *
 * Author: Jeremy Wadsack
 */
public class PublicBroadcastingClient {
  private static final String nowPlayingURL =
      "http://guide.publicbroadcasting.net/npr/playingNow.txt";
  private static final String LOG_TAG = PublicBroadcastingClient.class.getName();


  /**
   * Retrieves the list of story ID's for the programs that are currently
   * playing somewhere in the NPR affiliate stations.
   *
   * @return an array of id Integers.
   * @throws java.io.IOException if it can't load the list
   */
  public static Integer[] getNowPlayingIds() throws IOException {
    InputStream data = HttpHelper.download(nowPlayingURL);
    if (data == null) {
      Log.d(LOG_TAG, "No programs currently on air anywhere");
      return new Integer[0];
    }

    BufferedReader reader = new BufferedReader(new InputStreamReader(data),
        1024);
    String line;
    ArrayList<Integer> list = new ArrayList<Integer>();
    try {
      while ((line = reader.readLine()) != null) {
        try {
          list.add(Integer.parseInt(line));
        } catch (NumberFormatException e) {
          Log.e(LOG_TAG, "Not an integer in on-air list: " + line);
        }
      }
    } catch (IOException e) {
      Log.e(LOG_TAG, "Error reading on air-list.", e);
    }

    return list.toArray(new Integer[list.size()]);
  }


}
