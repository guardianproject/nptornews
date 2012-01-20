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

package org.npr.android.news;

import android.app.Application;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.npr.android.util.FileUtils;
import org.npr.android.util.Tracker;
import org.npr.api.ApiConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles application startup code for any activity
 * Author: Jeremy Wadsack
 */
public class NewsApplication extends Application {
  private static final String LOG_TAG = NewsApplication.class.getName();

  private static final int MSG_CANCEL_LOCATION_LISTENERS = 2;
  private final Handler handler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case MSG_CANCEL_LOCATION_LISTENERS:
          cancelLocationListeners();
          break;
      }
    }
  };

  // This is public so that we can inspect if for testing
  public final List<LocationListener> locationListeners =
      new ArrayList<LocationListener>();


  @Override
  public void onCreate() {
    super.onCreate();

    launchLocationListeners();

    String key = "";
    try {
      key = FileUtils.readFile(getResources(),
          "org.npr.android.news:raw/api_key").toString();
    } catch (Exception e) {
      Log.e(LOG_TAG, "", e);
    }
    ApiConstants.createInstance(key);
  }

  @Override
  public void onTerminate() {
    super.onTerminate();
    Tracker.instance(this).finish();
    cancelLocationListeners();
  }


  /**
   * On start up, launch a location listener for each service. We need to do
   * this in order to ensure that getLastKnownLocation, used to find local
   * stations, will always find a value.
   */
  private void launchLocationListeners() {

    LocationManager lm =
        (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    List<String> providers = lm.getProviders(false);
    for (String provider : providers) {
      LocationListener listener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
          handler.sendEmptyMessage(MSG_CANCEL_LOCATION_LISTENERS);
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

      };
      lm.requestLocationUpdates(provider, 60000, 0, listener);
      locationListeners.add(listener);
    }
  }

  /**
   * Remove all listeners.
   */
  private void cancelLocationListeners() {
    LocationManager lm =
        (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    // Synchronized because there may be multiple listeners running and
    // we don't want them to both try to alter the listeners collection
    // at the same time.
    synchronized (locationListeners) {
      for (LocationListener listener : locationListeners) {
        lm.removeUpdates(listener);
      }
      locationListeners.clear();
    }
  }


}
