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

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import java.util.Arrays;

/**
 * A database provider that stores favorite stations.
 *
 * Author: Jeremy Wadsack
 */
public class FavoriteStationsProvider extends ContentProvider{
  private static final String LOG_TAG =
      FavoriteStationsProvider.class.getName();

  public static final Uri CONTENT_URI = Uri
      .parse("content://org.npr.android.util.FavoriteStations");

  private static final String DATABASE_NAME = "favorite_stations.db";
  private static final int DATABASE_VERSION = 1;
  private static final String TABLE_NAME = "items";

  private FavoriteStationsHelper helper;

  @Override
  public boolean onCreate() {
    helper = new FavoriteStationsHelper(getContext());
    return true;
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection,
      String[] selectionArgs, String sortOrder) {
    SQLiteDatabase db = helper.getWritableDatabase();
    String realSelection = getSelectionFromId(uri, selection);

    Cursor result = db.query(TABLE_NAME, projection, realSelection,
        selectionArgs, null, null, sortOrder);
    Log.d(LOG_TAG, uri.toString() + ";" + realSelection + ";"
        + Arrays.toString(selectionArgs));
    return result;
  }

  @Override
  public String getType(Uri uri) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Uri insert(Uri uri, ContentValues contentValues) {
    SQLiteDatabase db = helper.getWritableDatabase();
    long id = db.insert(TABLE_NAME, Items.NAME, contentValues);
    Log.d(LOG_TAG, "Adding new station to favorites database.");
    return ContentUris.withAppendedId(uri, id);
  }

  @Override
  public int delete(Uri uri, String selection, String[] selectionArgs) {
    SQLiteDatabase db = helper.getWritableDatabase();
    String realSelection = getSelectionFromId(uri, selection);
    Log.d(LOG_TAG, "Deleting station from favorites database.");
    return db.delete(TABLE_NAME, realSelection, selectionArgs);
  }

  @Override
  public int update(Uri uri, ContentValues contentValues, String selection,
                    String[] selectionArgs) {
    SQLiteDatabase db = helper.getWritableDatabase();
    String realSelection = getSelectionFromId(uri, selection);
    return db.update(TABLE_NAME, contentValues, realSelection, selectionArgs);
  }


  private String getSelectionFromId(Uri uri, String selection) {
    long id = ContentUris.parseId(uri);
    if (id == -1) {
      return selection;
    }
    String realSelection = selection == null ? "" : selection + " and ";
    realSelection += Items._ID + " = " + id;
    return realSelection;
  }


  public static class Items implements BaseColumns {
    public static final String NAME = "name";
    public static final String MARKET = "market_city";
    public static final String FREQUENCY = "frequency";
    public static final String BAND = "band";
    public static final String STATION_ID = "story_id";
    public static final String[] COLUMNS = { NAME, MARKET, FREQUENCY, BAND,
        STATION_ID };
    public static final String[] ALL_COLUMNS = { BaseColumns._ID, NAME, MARKET,
        FREQUENCY, BAND, STATION_ID };

    // This class cannot be instantiated
    private Items() {
    }
  }


  protected static class FavoriteStationsHelper extends SQLiteOpenHelper {

    public FavoriteStationsHelper(Context context) {
      super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
      db.execSQL("CREATE TABLE " + TABLE_NAME + " (" + Items._ID
          + " INTEGER PRIMARY KEY," + Items.NAME + " TEXT," + Items.MARKET
          + " TEXT," + Items.FREQUENCY + " TEXT," + Items.BAND
          + " TEXT," + Items.STATION_ID + " TEXT"
          + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      Log.w(FavoriteStationsHelper.class.getName(),
          "Upgrading database from version " + oldVersion +
              " to " + newVersion);
    }
  }
}
