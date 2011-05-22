// Copyright 2010 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package org.npr.android.news;

import android.content.Context;
import android.content.Intent;
import android.test.InstrumentationTestCase;
import android.util.Log;

import org.npr.android.test.BlockedHttpServer;
import org.npr.android.test.HttpRawResourceServer;
import org.npr.android.test.NullStreamServer;
import org.npr.android.util.PlaylistEntry;

import java.util.ArrayList;

/**
 * Sets up a series of tests for the PlaybackService class.
 *
 * @author jeremywadsack
 */
public class PlaybackServiceTest extends InstrumentationTestCase {
  private static final String TAG = PlaybackServiceTest.class.getName();

  /*
   * Verifies basic functionality - that the player will play an MP3 file
   * specified by a URL
   */
  public void testShouldPlayRemoteMP3File() {
    // create a local server that can post a file out of the res/raw folder
    HttpRawResourceServer server = new HttpRawResourceServer(
      getInstrumentation().getContext(), false);
    PlaybackServiceHelper tester = new PlaybackServiceHelper(
      getInstrumentation().getTargetContext());
    try {
      server.init();
      server.start();

      // listen to our file
      PlaylistEntry entry = new PlaylistEntry(-1L, "http://127.0.0.1:"
        + server.getPort() + "/one_second_silence_mp3", "Silence", false, -1);

      tester.listen(entry);

      final int maxWait = 30 * 1000; // Wait 30 seconds to finish playing
      int timeWaited = 0;
      while (!tester.isComplete()) {
        try {
          Thread.sleep(200);
        } catch (InterruptedException ex) {
        }
        timeWaited += 200;
        if (timeWaited > maxWait) {
          Log.w(TAG, "Timed out waiting for test song to complete playing");
          break;
        }
      }

    } finally {
      // Cleanup
      server.stop();
    }

    // assert that the file started and completed
    assertNotNull(tester);
    assertTrue("Never completed playing song", tester.isComplete());
    ArrayList<String> log = tester.getLog();
    assertNotNull("File never started playing.", findStringStartsWith(log,
      "Prepared"));

    // and that the duration and final position are all correct
    String durationString = findStringStartsWith(log, "Duration:");
    assertNotNull("Media player logged a duration.", durationString);
    int pos = durationString.lastIndexOf(' ');
    if (pos > -1) {
      try {
        Integer.parseInt(durationString.substring(pos + 1));
      } catch (NumberFormatException ex) {
        fail("No duration provided: " + durationString);
      }
    } else {
      fail("No duration provided: " + durationString);
    }

    String finalPositionString = findStringStartsWith(log, "Final position:");
    assertNotNull("Media player logged a position.", finalPositionString);
    pos = finalPositionString.lastIndexOf(' ');
    if (pos > -1) {
      try {
        Integer.parseInt(finalPositionString.substring(pos + 1));
      } catch (NumberFormatException ex) {
        fail("No final position provided: " + finalPositionString);
      }
    } else {
      fail("No final position provided: " + finalPositionString);
    }

  }

  /*
   * Verifies that the player can play from a continuous MP3 stream.
   */
  public void testShouldPlayRemoteMP3Stream() {
    HttpRawResourceServer server = new HttpRawResourceServer(
      getInstrumentation().getContext(), true);
    PlaybackServiceHelper tester = new PlaybackServiceHelper(
      getInstrumentation().getTargetContext());
    try {
      server.init();
      server.start();

      // listen to our file
      PlaylistEntry entry = new PlaylistEntry(-1L, "http://127.0.0.1:"
        + server.getPort() + "/one_second_silence_mp3", "Silent Stream test",
        true, -1);
      tester.listen(entry);

      final int maxWait = 3 * 1000; // Play stream for 3 seconds
      int timeWaited = 0;
      while (!tester.isComplete() && timeWaited < maxWait) {
        try {
          Thread.sleep(200);
        } catch (InterruptedException ex) {
        }
        timeWaited += 200;
      }

    } finally {
      // Cleanup
      server.stop();
    }

    // assert that the file started
    assertNotNull(tester);
    ArrayList<String> log = tester.getLog();
    assertNotNull("File never started playing.", findStringStartsWith(log,
      "Prepared"));

  }

  /*
   * If you start the player on the main thread and it fails for any reason,
   * then the player never calls isPrepared on the service and the listen loop
   * gets locked.
   */
  public void testShouldStopPlayerIfMediaFailsAndNoOtherItemsExist() {
    final PlaybackServiceHelper tester = new PlaybackServiceHelper(
      getInstrumentation().getTargetContext());
    // listen to our file
    final PlaylistEntry entry = new PlaylistEntry(-1L, "/dev/null",
      "not-existent file", false, -1);
    Thread t = new Thread(new Runnable() {
      @Override
      public void run() {
        tester.listen(entry);
      }
    });
    t.start();

    // Wait 10 seconds to finish the prepared call
    long maxWait = 10 * 1000;
    long startTime = System.currentTimeMillis();
    try {
      t.join(maxWait);
    } catch (InterruptedException ex) {
    }

    assertTrue("PlaybackService blocked on listen call.", System
      .currentTimeMillis()
      - startTime < maxWait);

    assertNotNull(tester);
    assertTrue("File should complete.", tester.isComplete());
    ArrayList<String> log = tester.getLog();
    assertNull("File should not play.", findStringStartsWith(log, "Prepared"));
  }

  /*
   * When running on pre-2.2 with an MP3 stream as data, if the data can't be
   * streamed and fails to ever prepare it also never errors or completes. This
   * should be properly handled so that we don't block the main thread.
   */
  public void testShouldStopPlayerIfStreamBlocksAndNoOtherItemsExist() {
    BlockedHttpServer server = new BlockedHttpServer();
    final PlaybackServiceHelper tester = new PlaybackServiceHelper(
      getInstrumentation().getTargetContext());
    try {
      server.init();
      server.start();

      // listen to our file
      final PlaylistEntry entry = new PlaylistEntry(-1L,
        "http://127.0.0.1:" + server.getPort() + "/",
        "Invalid stream test",
        true, // Don't bother using the StreamProxy
        -1);
      Thread t = new Thread(new Runnable() {
        @Override
        public void run() {
          tester.listen(entry);
        }
      });
      t.start();

      // Wait 5 minutes to finish the prepared call
      long maxWait = 30 * 1000;
      long startTime = System.currentTimeMillis();
      try {
        t.join(maxWait);
      } catch (InterruptedException ex) {
      }

      assertTrue("PlaybackService blocked on listen call.", System
        .currentTimeMillis()
        - startTime < maxWait);

    } finally {
      // Cleanup
      server.stop();
    }

    assertNotNull(tester);
    ArrayList<String> log = tester.getLog();
    assertNull("File should not play.", findStringStartsWith(log, "Prepared"));
  }

  public void testShouldStopPlayerIfStreamFailsAndNoOtherItemsExist() {
    NullStreamServer server = new NullStreamServer();
    final PlaybackServiceHelper tester = new PlaybackServiceHelper(
      getInstrumentation().getTargetContext());
    try {
      server.init();
      server.start();

      // listen to our file
      final PlaylistEntry entry = new PlaylistEntry(-1L, "http://127.0.0.1:"
        + server.getPort() + "/", "Invalid stream test", false, -1);
      Thread t = new Thread(new Runnable() {
        @Override
        public void run() {
          tester.listen(entry);
        }
      });
      t.start();

      // Wait 10 seconds to finish the prepared call
      long maxWait = 10 * 1000;
      long startTime = System.currentTimeMillis();
      try {
        t.join(maxWait);
      } catch (InterruptedException ex) {
      }

      assertTrue("PlaybackService blocked on listen call.", System
        .currentTimeMillis()
        - startTime < maxWait);

    } finally {
      // Cleanup
      server.stop();
    }

    assertNotNull(tester);
    assertTrue("File should complete.", tester.isComplete());
    ArrayList<String> log = tester.getLog();
    assertNull("File should not play.", findStringStartsWith(log, "Prepared"));
  }

  public void todoShouldPlayNextItemIfMediaFailsAndOtherItemsExist() {
    // TODO: Similar to previous test but first add items to the playlist
    // provider
    // - I can't find examples where anything is ever added
    // - Also, mock the PlaylistProvider, if possible
  }

  // -------------
  // Helper functions and classes below

  /**
   * A helper function to find a string within a list that starts with the given
   * prefix
   *
   * @param list The list of strings to search
   * @param prefix The prefix to find in the list
   * @return the first string in the list that matches or null if no match is
   *         found.
   */
  private String findStringStartsWith(ArrayList<String> list, String prefix) {
    for (String s : list) {
      if (s.startsWith(prefix)) {
        return s;
      }
    }
    return null;
  }

  /**
   * A stub class that connects to the PlaybackService and instruments it for
   * testing and inspection.
   */
  private class PlaybackServiceHelper {

    private final ArrayList<String> log = new ArrayList<String>();
    private boolean isComplete = false;
    private Context context = null;

    public PlaybackServiceHelper(Context context) {
      log.add("Created");
      this.context = context;
    }

    public void listen(PlaylistEntry entry) {
      Log.d(TAG, "Listening to " + entry.url);
      try {
        isComplete = false;
        Intent intent = new Intent(context, PlaybackService.class);
        intent.setAction(PlaybackService.SERVICE_PLAY_SINGLE);
        intent.putExtra(Playable.PLAYABLE_TYPE, Playable.PlayableFactory
            .fromPlaylistEntry(entry));
        context.startService(intent);
      } catch (IllegalArgumentException e) {
        Log.e(TAG, "Media play failure", e);
      } catch (IllegalStateException e) {
        Log.e(TAG, "Media play failure", e);
      }
    }

    public boolean isComplete() {
      return isComplete;
    }

    public ArrayList<String> getLog() {
      return log;
    }
  }
}
