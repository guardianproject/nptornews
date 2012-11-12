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

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import org.npr.android.util.ArrayUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple client to retrieve the comma-separated list of data in the
 * iphone_news_app_programs.conf file from npr.org.
 *
 * Author: Jeremy Wadsack
 */
public class IPhoneNewsAppProgramsConfProvider extends ContentProvider {
  public static final Uri CONTENT_URL =
    Uri.parse("content://org.npr.apr.IPhoneNewsAppProgramsConf");
  private static final String CONTENT_TYPE =
    "vnd.android.cursor.dir/vnd.npr.programs";
  private static final String CONF_URL =
    "http://www.npr.org/services/apps/iphone_news_app_programs.conf";
  private static final String LOG_TAG =
    IPhoneNewsAppProgramsConfProvider.class.getName();
  private static List<String[]> data;

  @Override
  public boolean onCreate() {
    return true;
  }


  @Override
  public Cursor query(Uri uri, String[] projections, String selection,
                      String[] selectionArgs, String sortOrder) {
    // TODO: support more complex queries or other fields at least
    // if/when needed
    if (selection != null && !selection.equals(Items.TOPIC_ID + " = ?")) {
      return null;
    }

    if (data == null ) {
      data = new ArrayList<String[]>();
      if (!load()) {
        return null;
      }
    }

    MatrixCursor cursor = new MatrixCursor(Items.COLUMNS);
    for (String[] row : data) {
      if (selection == null) {
        cursor.addRow(row);
      } else if (row[6].equals(selectionArgs[0])) {
        cursor.addRow(row);
      }
    }

    return cursor;
  }

  @Override
  public String getType(Uri uri) {
    return CONTENT_TYPE;
  }

  @Override
  public Uri insert(Uri uri, ContentValues contentValues) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int delete(Uri uri, String s, String[] strings) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
    throw new UnsupportedOperationException();
  }



  /*
All Things Considered,http://api.npr.org/query?id=2&date=currentWithAudio&output=podcast&sc=17&numResults=50,120,1,,http://guide.publicbroadcasting.net/national/guide.guidemain?action=nationalProgramSearch&program=allthingsconsidered,2
Morning Edition,http://api.npr.org/query?id=3&date=currentWithAudio&output=podcast&sc=17&numResults=50,120,1,,http://guide.publicbroadcasting.net/national/guide.guidemain?action=nationalProgramSearch&program=morningedition,3
Weekend Edition Saturday,http://api.npr.org/query?id=7&date=currentWithAudio&output=podcast&sc=17&numResults=50,120,1,,http://guide.publicbroadcasting.net/national/guide.guidemain?action=nationalProgramSearch&program=weekendeditionsaturday,7
Weekend Edition Sunday,http://api.npr.org/query?id=10&date=currentWithAudio&output=podcast&sc=17&numResults=50,120,1,,http://guide.publicbroadcasting.net/national/guide.guidemain?action=nationalProgramSearch&program=weekendeditionsunday,10
Weekends on All Things Considered,http://www.npr.org/templates/rss/podlayer.php?id=129577422,20,0,http://itunes.apple.com/us/podcast/npr-series-weekends-on-all/id392335760,,
Car Talk,http://www.npr.org/rss/podcast.php?id=510208,18,1,http://itunes.apple.com/WebObjects/MZStore.woa/wa/viewPodcast?s=143441&partnerId=30&id=253191823,http://guide.publicbroadcasting.net/national/guide.guidemain?action=nationalProgramSearch&program=cartalk,18

  */

  /**
   * Parses the CSV data and loads the data member table.
   *
   * TODO: Use a real CSV parser or even better a CSV database implementation.
   * e.g. http://sourceforge.net/projects/javacsv/develop
   * or import into SQLite in-memory?
   *
   * @return true on success; false if the stream is null or there is an
   * exception (which is logged)
   */
  private boolean load() {
    try {
      InputStream stream = HttpHelper.download(CONF_URL, getContext());
      if (stream == null) {
        return false;
      }

      BufferedReader reader = new BufferedReader(
        new InputStreamReader(stream),
        8192
      );
      String buffer  ;
      while ((buffer = reader.readLine()) != null){
        String[] rowData = buffer.split(",");
        if (rowData.length > 0) {
          if (rowData.length < Items.COLUMNS.length) {
            rowData = ArrayUtils.copyOf(rowData, Items.COLUMNS.length);
          }
          data.add(rowData);
        }
      }
      reader.close();
      stream.close();
    } catch (IOException e) {
      Log.e(LOG_TAG, "", e);
      return false;
    }
    return true;
  }




  public static class Items implements BaseColumns {
    public static final String NAME = "name";
    public static final String PODCAST_URL = "podcast_rss_url";
    public static final String UNKNOWN_1 = "unknown_1";
    public static final String UNKNOWN_2 = "unknown_2";
    public static final String ITUNES_URL = "itunes_or_apple_stream_url";
    public static final String LIVE_STREAM_URL = "guide_publicbroadcasting_net_url";
    public static final String TOPIC_ID = "story_id";

    public static final String[] COLUMNS = { NAME, PODCAST_URL, UNKNOWN_1,
      UNKNOWN_2, ITUNES_URL, LIVE_STREAM_URL, TOPIC_ID };

    // This class cannot be instantiated
    private Items() {
    }
  }

}
