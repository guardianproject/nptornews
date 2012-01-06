package org.npr.android.util;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import org.npr.android.util.FavoriteStationsProvider.Items;
import org.npr.api.Station;

public class FavoriteStationsRepository {

  private final ContentResolver contentResolver;
  private static final String LOG_TAG = FavoriteStationsRepository.class.getName();

  public FavoriteStationsRepository(ContentResolver contentResolver) {
    this.contentResolver = contentResolver;
  }

  public long add(Station station, String preset) {
    ContentValues values = new ContentValues();
    values.put(Items.NAME, station.getName());
    values.put(Items.MARKET, station.getMarketCity());
    values.put(Items.FREQUENCY, station.getFrequency());
    values.put(Items.BAND, station.getBand());
    values.put(Items.STATION_ID, station.getId());
    values.put(Items.PRESET, preset);
    Log.d(LOG_TAG, "Adding favorite station to db");
    Uri uri = contentResolver.insert(
        FavoriteStationsProvider.CONTENT_URI, values);

    return ContentUris.parseId(uri);
  }

  public long update(long id, String preset) {
    Uri update = ContentUris.withAppendedId(FavoriteStationsProvider.CONTENT_URI, id);
    ContentValues values = new ContentValues();
    values.put(FavoriteStationsProvider.Items.PRESET, preset);
    return contentResolver.update(update, values, null, null);
  }

  public void removePreset(String stationId) {
    String selection = Items.STATION_ID + " = ?";
    String[] selectionArgs = new String[1];
    selectionArgs[0] = stationId;

    ContentValues values = new ContentValues();
    values.put(Items.PRESET, (String)null);
    contentResolver.update(
        FavoriteStationsProvider.CONTENT_URI, values, selection, selectionArgs);
  }

  public int getItemCount() {
    Cursor c = contentResolver.query(
        FavoriteStationsProvider.CONTENT_URI, null, null, null, null);
    int count = c.getCount();
    c.close();
    return count;
  }

  public int getPresetCount() {
    String selection = Items.PRESET + " IS NOT NULL";
    Cursor c = contentResolver.query(
        FavoriteStationsProvider.CONTENT_URI, null, selection, null, null);
    int count = c.getCount();
    c.close();
    return count;
  }

  public FavoriteStationEntry getFirstFavorite() {
    Cursor c = contentResolver.query(FavoriteStationsProvider.CONTENT_URI,
        null, null, null, Items.PRESET);
    FavoriteStationEntry favoriteStationEntry = null;
    if (c.moveToFirst()) {
      favoriteStationEntry = new FavoriteStationEntry(
          c.getInt(c.getColumnIndex(Items._ID)),
          c.getString(c.getColumnIndex(Items.STATION_ID)),
          c.getString(c.getColumnIndex(Items.PRESET)));
    }

    c.close();
    return favoriteStationEntry;
  }

  public FavoriteStationEntry getFavoriteStationForPreset(String preset) {

    String selection = FavoriteStationsProvider.Items.PRESET + " = ?";
    String[] selectionArgs = new String[1];
    selectionArgs[0] = preset;

    Cursor c = contentResolver.query(FavoriteStationsProvider.CONTENT_URI,
        null, selection, selectionArgs, null);

    FavoriteStationEntry favoriteStationEntry = null;
    if (c.moveToFirst()) {
      favoriteStationEntry = new FavoriteStationEntry(
          c.getInt(c.getColumnIndex(Items._ID)),
          c.getString(c.getColumnIndex(Items.STATION_ID)),
          c.getString(c.getColumnIndex(Items.PRESET)));
    }

    c.close();
    return favoriteStationEntry;
  }

  public FavoriteStationEntry getFavoriteStationForStationId(String stationId) {

    String selection = Items.STATION_ID + " = ?";
    String[] selectionArgs = new String[1];
    selectionArgs[0] = stationId;

    Cursor c = contentResolver.query(FavoriteStationsProvider.CONTENT_URI,
        null, selection, selectionArgs, null);

    FavoriteStationEntry favoriteStationEntry = null;
    if (c.moveToFirst()) {
      favoriteStationEntry = new FavoriteStationEntry(
          c.getInt(c.getColumnIndex(Items._ID)),
          c.getString(c.getColumnIndex(FavoriteStationsProvider.Items.STATION_ID)),
          c.getString(c.getColumnIndex(FavoriteStationsProvider.Items.PRESET)));
    }

    c.close();
    return favoriteStationEntry;
  }

  public long setPreset(Station station, String preset) {

    // Does this preset already have a station?
    FavoriteStationEntry oldPreset = getFavoriteStationForPreset(preset);

    // Does this station already have a preset number?
    FavoriteStationEntry matchingStationPreset =
        getFavoriteStationForStationId(station.getId());

    if (oldPreset == null) {
      if (matchingStationPreset == null) {
        return add(station, preset);
      } else {
        // Just update the number to the new number
        return update(matchingStationPreset.id, preset);
      }
    } else {
      update(oldPreset.id, null);
      long newId;
      if (matchingStationPreset == null) {
        newId = add(station, preset);
      } else {
        newId = update(matchingStationPreset.id, preset);
      }
      // Move the existing item to the first open slot
      int newNumber = 1;
      while (getFavoriteStationForPreset(Integer.toString(newNumber)) != null && newNumber <= 10)
        newNumber++;

      if (newNumber <= 10) {
        update(oldPreset.id, Integer.toString(newNumber));
      }

      return newId;
    }
  }
}
