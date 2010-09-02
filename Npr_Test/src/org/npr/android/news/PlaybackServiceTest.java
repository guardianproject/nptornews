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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.IBinder;
import android.test.InstrumentationTestCase;
import android.util.Log;

import org.npr.android.test.HttpRawResourceServer;
import org.npr.android.util.PlaylistEntry;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Sets up a series of tests for the PlaybackService class.
 * 
 * @author jeremywadsack
 */
public class PlaybackServiceTest extends InstrumentationTestCase {
  public static final String TAG = PlaybackServiceTest.class.getName();

  /*
   * Verifies basic functionality - that the player will play an MP3 file
   * specified by a URL
   */
  public void testShouldPlayRemoteMP3File() {
    // create a local server that can post a file out of the res/raw folder
    HttpRawResourceServer server = new HttpRawResourceServer(
        getInstrumentation().getContext());
    PlaybackServiceHelper tester = new PlaybackServiceHelper(
        getInstrumentation().getTargetContext());
    try {
      server.init();
      server.start();

      // bind a testing activity to the service that logs callbacks
      tester.bind();

      // listen to our file
      PlaylistEntry entry = new PlaylistEntry(-1L, "http://127.0.0.1:"
          + server.getPort() + "/laser", "Laser Button", false, -1);
      PlaybackService.setCurrent(entry);
      tester.listen(entry.url);

      int maxWait = 30 * 1000; // Wait 30 seconds to finish playing
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
      if (tester != null) {
        tester.destroy();
      }
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

  public void testShouldPlayRemoteMP3Stream() {

  }

  /*
   * If you start the player on the main thread and it fails for any reason,
   * then the player never calls isPrepared on the service and the listen loop
   * gets locked.
   */
  public void testShouldStopPlayerIfMediaFailsAndNoOtherItemsExist() {
    final PlaybackServiceHelper tester = new PlaybackServiceHelper(
        getInstrumentation().getTargetContext());
    try {
      // bind a testing activity to the service that logs callbacks
      tester.bind();

      // listen to our file
      PlaybackService.setCurrent(new PlaylistEntry(-1L, "/dev/null",
          "not-existent file", false, -1));
      Thread t = new Thread(new Runnable() {
        @Override
        public void run() {
          tester.listen("/dev/null");
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
      if (tester != null) {
        tester.destroy();
      }
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
    // - Also, mock the provider, if possible
  }

  // -------------
  // Helper functions and classes below

  /**
   * A helper function to find a string within a list that starts with the given
   * prefix
   * 
   * @param list
   * @param prefix
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
  private class PlaybackServiceHelper implements OnCompletionListener,
      OnPreparedListener {

    private final ArrayList<String> log = new ArrayList<String>();
    private ServiceConnection conn;
    private PlaybackService player;
    private boolean isComplete = false;
    private Context context = null;

    public PlaybackServiceHelper(Context context) {
      log.add("Created");
      this.context = context;
    }

    public void bind() {
      Intent serviceIntent = new Intent(context, PlaybackService.class);
      conn = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
          log.add("Connected service");
          player = ((PlaybackService.ListenBinder) service).getService();
          onBindComplete((PlaybackService.ListenBinder) service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
          log.add("Disconnected service");
          player = null;
        }

      };

      context.bindService(serviceIntent, conn, Context.BIND_AUTO_CREATE);
    }

    public void listen(String url) {
      if (conn == null) {
        throw new IllegalStateException(
            "You need to call bind() before calling listen().");
      }

      Log.d(TAG, "Listening to " + url);
      // make sure the service is connected
      int timeout = 30 * 1000; // Up to 30 seconds
      int elapsed = 0;
      while (player == null) {
        try {
          Thread.sleep(200);
        } catch (InterruptedException ex) {
        }
        elapsed += 200;
        if (elapsed > timeout) {
          Log.e(TAG, "Timed out waiting for service to connect.");
          return;
        }
      }
      player.stop();
      try {
        isComplete = false;
        player.listen(url, false); // stream == false; don't start the proxy
      } catch (IllegalArgumentException e) {
        Log.e(TAG, "Media play failure", e);
      } catch (IllegalStateException e) {
        Log.e(TAG, "Media play failure", e);
      } catch (IOException e) {
        Log.e(TAG, "Media play failure", e);
      }
    }

    public boolean isComplete() {
      return isComplete;
    }

    public ArrayList<String> getLog() {
      return log;
    }

    // For each of these methods log the events and use them for
    // assertions and analytics
    private void onBindComplete(PlaybackService.ListenBinder binder) {
      log.add("Bound service.");
      player.setOnCompletionListener(this);
      player.setOnPreparedListener(this);
    }

    public void destroy() {
      log.add("Destroyed");
      context.unbindService(conn);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
      log.add("Done playing.");
      log.add("Final position: " + mp.getCurrentPosition());
      isComplete = true;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
      log.add("Prepared.");
      log.add("Duration: " + mp.getDuration());
    }

  }

}
