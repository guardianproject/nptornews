// Copyright 2010 Google Inc.
// Copyright 2011 NPR
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

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

/**
 * Tests for the Main activity.
 * 
 * @author jeremywadsack
 */
public class LocationTest
    extends ActivityInstrumentationTestCase2<NewsListActivity> {
  private static final String LOG_TAG = LocationTest.class.getName();
  private static final long MAX_TEST_RUN = 5000; // seconds

  public LocationTest() {
    super("org.npr.android.news", NewsListActivity.class);
  }

  public void testShouldStartGPSListenersOnLaunch() {
    ListenerInspectionThread thread = new ListenerInspectionThread();
    thread.start();
    thread.setActivity(getActivity());
    Activity activity = getActivity();

    // Use GPS because the emulator doesn't have a NETWORK provider
    String providerName = LocationManager.GPS_PROVIDER;
    try {
      // Set up a mock provider to send some location data
      LocationManager lm = (LocationManager) activity.getSystemService(
          Context.LOCATION_SERVICE);
      Location location = new Location(providerName);
      location.setLatitude(43.133061);
      location.setLongitude(-112.412109);
      location.setTime(System.currentTimeMillis());
      lm.setTestProviderLocation(providerName, location);

      // Send a second location so that location is changing
      location = new Location(providerName);
      location.setLatitude(44.133061);
      location.setLongitude(-113.412109);
      location.setTime(System.currentTimeMillis());
      lm.setTestProviderLocation(providerName, location);

      // Wait until thread completes.
      thread.join(MAX_TEST_RUN);
    } catch (InterruptedException e) {
      Log.d(LOG_TAG, "Application test interrupted waiting for thread to " +
          "complete.");
    } catch (SecurityException ex) {
      // See http://forum.archosfans.com/viewtopic.php?f=34&t=31864
      // This may not exist on all devices
      Log.e(LOG_TAG,
              "To use mock locations you need to add ACCESS_MOCK_LOCATION to "
                  + "the manifest of the application under test. You must also enable "
                  + "this on a real device in Settings | Applications | Development | "
                  + "Allow Mock Locations",
              ex);
    }

    assertTrue("Main activity never added any listeners.",
        thread.wereListenersAdded());
    assertTrue("Main activity never removed its listeners.",
        thread.wereListenersRemoved());
  }

  private class ListenerInspectionThread extends Thread {
    private boolean listenersAdded = false;
    private boolean listenersRemoved = false;
    private RootActivity activity;
    private NewsApplication application;

    public void setActivity(RootActivity activity) {
      this.activity = activity;
      application = (NewsApplication)activity.getApplication();
    }

    @Override
    public void run() {
      long timeStarted = System.currentTimeMillis();

      // Wait for the activity to start
      while (activity == null
          && System.currentTimeMillis() < timeStarted + MAX_TEST_RUN) {
        try {
          Thread.sleep(20);
        } catch (InterruptedException e) {
          Log.d(LOG_TAG,
              "Test interrupted while waiting for activity to start.");
          return;
        }
      }
      if (activity == null) {
        Log.d(LOG_TAG, "Test timed out.");
        return;
      }

      // Wait for the listeners list to be populated
      while ((application.locationListeners == null ||
          application.locationListeners.size() == 0)
          && System.currentTimeMillis() < timeStarted + MAX_TEST_RUN) {
        try {
          Thread.sleep(20);
        } catch (InterruptedException e) {
          Log.d(LOG_TAG,
              "Test interrupted while waiting for activity to add listeners.");
          return;
        }
      }
      listenersAdded = activity != null
          && System.currentTimeMillis() < timeStarted + MAX_TEST_RUN;

      // Wait for the activity to remove listeners
      while ((application.locationListeners == null ||
          application.locationListeners.size() > 0)
          && System.currentTimeMillis() < timeStarted + MAX_TEST_RUN) {
        try {
          Thread.sleep(20);
        } catch (InterruptedException e) {
          Log.d(LOG_TAG,
                  "Test interrupted while waiting for activity to remove listeners.");
          return;
        }
      }
      listenersRemoved = listenersAdded && activity != null
          && application.locationListeners.size() == 0;
    }

    public boolean wereListenersAdded() {
      return listenersAdded;
    }

    public boolean wereListenersRemoved() {
      return listenersRemoved;
    }

  }

}
