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

import android.app.Activity;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

/**
 * Tests around the StationSearchActivity view.
 */
public class StationSearchActivityTest extends
    ActivityInstrumentationTestCase2<StationSearchActivity> {
  private static final String LOG_TAG =
      StationSearchActivityTest.class.getName();

  private Button searchNow;
  private RadioGroup searchByGroup;
  private RadioButton searchByLocation;
  private Activity activity;

  public StationSearchActivityTest() {
    super("org.npr.android.news", StationSearchActivity.class);
  }

  public void setUp() throws Exception {
    super.setUp();
    activity = getActivity();
    searchByGroup = (RadioGroup) activity.findViewById(R.id.RadioGroup01);
    searchNow = (Button) activity.findViewById(R.id.StationSearchNowButton);
    searchByLocation = (RadioButton) activity.findViewById(R.id.RadioButton01);
  }

  /*
   * Issue 24: Selecting location-based station search result causes a force
   * close
   * 
   * NOTE: This test requires Internet and API access.
   */
  public void testShouldFindStationsFromSEIdaho() {
    double latitude = 43.133061;
    double longitude = -112.412109;
    // Using NETWORK because the activity checks that first
    mockLocationProvider(getActivity(), LocationManager.NETWORK_PROVIDER,
        latitude, longitude);

    activity.runOnUiThread(new Runnable() {
      public void run() {
        searchByGroup.check(searchByLocation.getId());
        searchNow.performClick();
      }
    });

    ((LocationManager) activity.getSystemService(Context.LOCATION_SERVICE))
        .removeTestProvider(LocationManager.NETWORK_PROVIDER);

    // Make sure that the application finished
    assertTrue("Activity should have stopped itself.", activity.isFinishing());
  }

  public static void mockLocationProvider(Activity activity,
      String providerName, double latitude,
      double longitude) {
    LocationManager lm = (LocationManager) activity.getSystemService(
        Context.LOCATION_SERVICE);
    try {
      lm.addTestProvider(providerName, false, false, false,
          false, false, false, false, Criteria.POWER_LOW,
          Criteria.ACCURACY_FINE);
      lm.setTestProviderEnabled(providerName, true);
      Location location = new Location(providerName);
      location.setLatitude(latitude);
      location.setLongitude(longitude);
      location.setTime(System.currentTimeMillis());
      lm.setTestProviderLocation(providerName, location);
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
  }

}
