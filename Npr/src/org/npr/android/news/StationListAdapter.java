// Copyright 2009 Google Inc.
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

import android.content.Context;
import android.database.Cursor;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import org.apache.http.client.ClientProtocolException;
import org.npr.android.util.FavoriteStationEntry;
import org.npr.android.util.FavoriteStationsProvider;
import org.npr.android.util.FavoriteStationsRepository;
import org.npr.android.util.StationCache;
import org.npr.api.Client;
import org.npr.api.Station;
import org.npr.api.Station.StationFactory;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StationListAdapter extends ArrayAdapter<Station> {
  private static final String LOG_TAG = StationListAdapter.class.getName();
  private List<Station> data;
  protected Context context;

  FavoriteStationsRepository favoriteStationsRepository;

  public StationListAdapter(Context context, boolean showFavorites) {
    super(context, R.layout.station_item, R.id.StationItemNameText);
    this.context = context;
    if (showFavorites) {
      favoriteStationsRepository =
          new FavoriteStationsRepository(context.getContentResolver());
    } else {
      favoriteStationsRepository = null;
    }
  }

  public int initializeList(String url) {
    // If we fail, then the list will be empty.
    data = new ArrayList<Station>();

    Node stations = null;
    try {
      stations = new Client(url).execute();
    } catch (ClientProtocolException e) {
      Log.e(LOG_TAG, "", e);
    } catch (IOException e) {
      Log.e(LOG_TAG, "", e);
    } catch (SAXException e) {
      Log.e(LOG_TAG, "", e);
    } catch (ParserConfigurationException e) {
      Log.e(LOG_TAG, "", e);
    }

    if (stations == null) {
      return -1;
    }

    Log.d(LOG_TAG, "stations: " + stations.getNodeName());

    data = StationFactory.parseStations(stations);
    StationCache.addAll(data);
    return 0;
  }


  public void initializeList(Cursor cursor) {
    data = new ArrayList<Station>();

    while (cursor.moveToNext()) {
      Station.StationBuilder builder = new Station.StationBuilder(
          cursor.getString(
              cursor.getColumnIndex(FavoriteStationsProvider.Items.STATION_ID)
          )
      );
      builder.withName(cursor.getString(cursor.getColumnIndex
          (FavoriteStationsProvider.Items.NAME)));
      builder.withMarketCity(cursor.getString(cursor.getColumnIndex
          (FavoriteStationsProvider.Items.MARKET)));
      builder.withFrequency(cursor.getString(cursor.getColumnIndex
          (FavoriteStationsProvider.Items.FREQUENCY)));
      builder.withBand(cursor.getString(cursor.getColumnIndex
          (FavoriteStationsProvider.Items.BAND)));
      data.add(builder.build());
    }
  }


  public void showData() {
    this.clear();
    for (Station station : data) {
      this.add(station);
    }
    notifyDataSetChanged();
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    View line = super.getView(position, convertView, parent);
    Station station = getItem(position);

    TextView name =
        (TextView) line.findViewById(R.id.StationItemNameText);
    name.setText(Html.fromHtml(station.getName()));

    TextView location =
        (TextView) line.findViewById(R.id.StationItemLocationText);
    location.setText(Html.fromHtml(station.getMarketCity()));

    TextView frequency =
        (TextView) line.findViewById(R.id.StationItemFrequencyText);

    if (station.getFrequency() != null && station.getBand() != null) {
      frequency.setText(Html.fromHtml(String.format(
          "%s %s",
          station.getFrequency(),
          station.getBand()
      )));
    } else {
      frequency.setText("");
    }

    if (favoriteStationsRepository != null) {
      TextView preset = (TextView) line.findViewById(R.id.StationPresetView);
      FavoriteStationEntry favoriteStationEntry =
          favoriteStationsRepository.getFavoriteStationForStationId(station.getId());
      if (favoriteStationEntry != null && favoriteStationEntry.preset != null) {
        preset.setText(favoriteStationEntry.preset);
        preset.setVisibility(View.VISIBLE);
      } else {
        preset.setVisibility(View.INVISIBLE);
      }
    }

    return line;
  }
}
