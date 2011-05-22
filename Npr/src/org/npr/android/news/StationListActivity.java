// Copyright 2009 Google Inc.
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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.npr.android.util.DisplayUtils;
import org.npr.android.util.FavoriteStationsProvider;
import org.npr.android.util.Tracker;
import org.npr.android.util.Tracker.StationListMeasurement;
import org.npr.api.ApiConstants;
import org.npr.api.Station;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StationListActivity extends TitleActivity implements
    OnItemClickListener, OnClickListener, View.OnKeyListener {
  private static final String LOG_TAG = StationListActivity.class.getName();

  private EditText searchTermField;
  private InputMethodManager inputMethodManager;

  public enum Mode {
    allStations, favoriteStations, locateStations, liveStreams, nearestStations
  }
  private Mode mode = Mode.allStations;

  private static final Map<String, Station> stationCache =
      new HashMap<String, Station>();
  private String query;

  public static Station getStationFromCache(String stationId) {
    Station result = stationCache.get(stationId);
    if (result == null ||
        // Stations pulled from the live stream list don't have stream names
        (result.getAudioStreams().size() > 0 &&
            result.getAudioStreams().get(0).getTitle() == null) ) {
      Station nprStation = Station.StationFactory.downloadStation(stationId);
      if (nprStation != null) {
        stationCache.put(stationId, nprStation);
        result = nprStation;
      }
    }
    return result;
  }

  public static void addAllToStationCache(List<Station> stations) {
    for (Station station : stations) {
      stationCache.put(station.getId(), station);
    }
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    listAdapter.clear();
    stationCache.clear();
  }

  private final Handler handler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      if(msg.what == 0) {
        listAdapter.showData();
        TextView emptyText = (TextView) findViewById(R.id.Empty);
        if (listAdapter.getCount() == 0) {
          emptyText.setText(getEmptyText());
          emptyText.setVisibility(View.VISIBLE);
        } else {
          emptyText.setVisibility(View.GONE);
        }
      } else {
        Toast.makeText(StationListActivity.this,
            getResources().getText(R.string.msg_check_connection),
            Toast.LENGTH_LONG)
            .show();
      }
      stopIndeterminateProgressIndicator();
    }
  };
  private StationListAdapter listAdapter;
  private Thread listInitThread;

  private void initializeList() {
    startIndeterminateProgressIndicator();
    listInitThread.start();
  }


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    String queryUrl = parseIntent(getIntent());
    super.onCreate(savedInstanceState);

    ViewGroup container = (ViewGroup) findViewById(R.id.Content);
    ViewGroup.inflate(this, R.layout.station_list, container);
    final ListView lv = (ListView) findViewById(R.id.ListView01);

    listAdapter =
        new StationListAdapter(this
        );
    lv.setAdapter(listAdapter);
    lv.setOnItemClickListener(StationListActivity.this);

    if (mode == Mode.favoriteStations) {
      loadFromFavorites();
    } else {
      loadFromQuery(queryUrl);
    }

    Button locateButton = (Button) findViewById(R.id.StationSearchButton);
    Button nearestButton = (Button) findViewById(R.id.StationNearestButton);
    searchTermField = (EditText) findViewById(R.id.StationSearchTerm);
    searchTermField.setOnKeyListener(this);
    ImageButton searchButton =
        (ImageButton) findViewById(R.id.search_go_button);
    searchButton.setOnClickListener(this);

    if (mode == Mode.locateStations) {
      locateButton
          .setBackgroundResource(R.drawable.button_background_pressed);
      locateButton.setTextColor(Color.WHITE);
      nearestButton.setOnClickListener(this);
      searchTermField.setVisibility(View.VISIBLE);
    } else {
      final View buttonBar = findViewById(R.id.ButtonBar);
      if (mode == Mode.nearestStations) {
        nearestButton
            .setBackgroundResource(R.drawable.button_background_pressed);
        nearestButton.setTextColor(Color.WHITE);
        locateButton.setOnClickListener(this);
        searchTermField.setVisibility(View.GONE);
        buttonBar.getLayoutParams().height =
            DisplayUtils.convertToDIP(this, 50);
      } else {
        buttonBar.setVisibility(View.GONE);
      }
    }
    inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
  }

  @Override
  public void onResume() {
    super.onResume();
    SharedPreferences preferences = getSharedPreferences("StationSearch", 0);
    searchTermField.setText(preferences.getString("lastSearch", ""));

    if (mode == Mode.favoriteStations) {
      loadFromFavorites();
    }
  }

  private void loadFromFavorites() {
    listInitThread = new Thread(new Runnable() {
      public void run() {
        Cursor cursor = managedQuery(FavoriteStationsProvider.CONTENT_URI,
            null, null, null, null);
        listAdapter.initializeList(cursor);
        cursor.close();
        handler.sendEmptyMessage(0);
      }
    });
    initializeList();
    query = "Favorites";
    trackNow();
  }

  /**
   * Loads the list adapter and the list from the provided query.
   * @param queryUrl The URL to fetch the data from.
   */
  private void loadFromQuery(String queryUrl) {
    if (queryUrl != null) {
      final String url = queryUrl;
      listInitThread = new Thread(new Runnable() {
        public void run() {
          int res = listAdapter.initializeList(url);
          handler.sendEmptyMessage(res);
        }
      });
      initializeList();
    }
    trackNow();
  }

  /**
   * Parses the intent for the query URL and sets the list's mode as a
   * side-effect. Note that this must be called before super.onCreate to
   * ensure that the title is properly selected.
   *
   * @param intent The Intent the activity was started with or the intent
   * returned to the activity as a result.
   *
   * @return The URL to fetch the list data from.
   */
  private String parseIntent(Intent intent) {
    String queryUrl = null;
    if (intent != null) {
      query = intent.getStringExtra(Constants.EXTRA_QUERY_TERM);
      queryUrl = intent.getStringExtra(Constants
        .EXTRA_LIVE_STREAM_RSS_URL);
      if (queryUrl != null) {
        mode = Mode.liveStreams;
      } else {
        mode = (Mode)intent.getSerializableExtra(
            Constants.EXTRA_STATION_LIST_MODE
        );
        if (mode == Mode.nearestStations) {
          Map<String, String> params = new HashMap<String, String>();
          query = populateLocalStationParams(params);
          queryUrl = ApiConstants.instance().createUrl(
              ApiConstants.STATIONS_PATH,
              params
          );
        } else {
          queryUrl = intent.getStringExtra(Constants.EXTRA_QUERY_URL);
        }
      }
    }
    return queryUrl;
  }

  @Override
  public boolean onKey(View view, int i, KeyEvent keyEvent) {
    switch (keyEvent.getKeyCode()) {
      case KeyEvent.KEYCODE_SEARCH:
      case KeyEvent.KEYCODE_ENTER:
        search();
        return true;
    }
    return false;
  }

  private void search() {
    if (inputMethodManager != null) {
      inputMethodManager.hideSoftInputFromWindow(
          searchTermField.getWindowToken(),
          InputMethodManager.HIDE_NOT_ALWAYS);
    }

    query = this.searchTermField.getText().toString().trim();
    Map<String, String> params = new HashMap<String, String>();
    if (query.length() == 4) {
      // Assume call letters
      params.put(ApiConstants.PARAM_CALL_LETTERS, query);
    } else if (query.length() == 5) {
      // Assume zip code
      params.put(ApiConstants.PARAM_ZIP, query);
    } else if (query.contains(",") || query.contains(" ")) {
      // Assume city/state
      String parts[] = query.split(",| ");
      String state = parts[parts.length - 1];

      StringBuilder builder = new StringBuilder(parts[0]);
      for (int i = 1, length = parts.length; i < length - 1; i++) {
          builder.append(" ").append(parts[i]);
      }
      String city = builder.toString();
      params.put(ApiConstants.PARAM_CITY, city);
      params.put(ApiConstants.PARAM_STATE, state);
    } else {
      Toast.makeText(this, R.string.msg_station_search_help,
          Toast.LENGTH_LONG).show();
      return;
    }
    SharedPreferences.Editor editor =
        getSharedPreferences("StationSearch", 0).edit();
    editor.putString("lastSearch", query);
    editor.commit();

    loadFromQuery(ApiConstants.instance().createUrl(
        ApiConstants.STATIONS_PATH,
        params
    ));
    trackNow();
  }

  @Override
  public void onClick(View v) {
    super.onClick(v);
    switch (v.getId()) {
      case R.id.StationSearchButton:
        Intent i1 = new Intent(this, StationListActivity.class)
            .putExtra(Constants.EXTRA_STATION_LIST_MODE, Mode.locateStations);
        startActivity(i1);
        break;
      case R.id.StationNearestButton:
        Intent i2 = new Intent(this, StationListActivity.class)
            .putExtra(Constants.EXTRA_STATION_LIST_MODE, Mode.nearestStations);
        startActivity(i2);
        break;
      case R.id.search_go_button:
        search();
    }
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position,
      long id) {
    Station station = (Station) parent.getItemAtPosition(position);

    Intent i =
      new Intent(this, StationDetailsActivity.class).putExtra(
          Constants.EXTRA_STATION_ID, station.getId());

    startActivityWithoutAnimation(i);
  }

  private String populateLocalStationParams(Map<String, String> params) {
    String query = null;
    Log.d(LOG_TAG, "finding local stations");
    LocationManager lm =
        (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    Location location = null;

    // Order here matters. This will search the network provider first, which is
    // more likely to be up to date. Since we are not actively polling, this is
    // more resilient to changes (e.g. going on an airplane and touching down
    // and not firing up the GPS again before running this app).
    String[] providers =
        {LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER};
    for (String provider : providers) {
      Location loc = lm.getLastKnownLocation(provider);
      if (loc != null) {
        location = loc;
        break;
      }
    }

    if (location != null) {
      double lat = location.getLatitude();
      double lon = location.getLongitude();

      params.put(ApiConstants.PARAM_LAT, Double.toString(lat));
      params.put(ApiConstants.PARAM_LON, Double.toString(lon));
      query = String.format("%f,%f", lat, lon);
    }

    return query;
  }


  @Override
  public CharSequence getMainTitle() {
    int titleId;
    switch (mode) {
      case locateStations:
      case nearestStations:
        titleId = R.string.msg_main_subactivity_locate_stations;
        break;
      case liveStreams:
        titleId = R.string.msg_main_subactivity_live_streams;
        break;
      case favoriteStations:
        titleId = R.string.msg_main_subactivity_favorite_stations;
        break;
      default:
        titleId = R.string.msg_main_subactivity_all_stations;
    }
    // TODO: If this is all stations within a state, append " - [state name]"
    return getString(titleId);
  }


  public CharSequence getEmptyText() {
    int helpId;
    switch (mode) {
      case liveStreams:
        helpId = R.string.msg_no_live_streams;
        break;
      case favoriteStations:
        helpId = R.string.msg_no_favorites;
        break;
      case nearestStations:
        helpId = R.string.msg_stations_found_nearby;
        break;
      default:
        helpId = R.string.msg_stations_found;
    }
    return getString(helpId);
  }
  @Override
  public void trackNow() {
    if (query == null) {
      return;
    }

    StringBuilder pageName =
        new StringBuilder("Stations").append(Tracker.PAGE_NAME_SEPARATOR);
    pageName.append("Search Results");

    Tracker.instance(getApplication()).trackPage(
        new StationListMeasurement(pageName.toString(), "Stations", query));
  }
}
