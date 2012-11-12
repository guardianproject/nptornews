package org.npr.android.util;

import org.npr.api.Station;

import android.content.Context;

import java.util.Hashtable;
import java.util.List;

public class StationCache {

  private static class StationEntry {

    public StationEntry(Station station, Context context) {
      this.station = station;
      this.expiration = System.currentTimeMillis() + 86400000; // 24 hours
      this.context = context;
    }

    public Station station;
    public long expiration;
    public Context context;

    public boolean isExpired() {
      return expiration < System.currentTimeMillis();
    }
  }

  private static final Hashtable<String, StationEntry> stationCache =
      new Hashtable<String, StationEntry>();

  public static void addAll(List<Station> stations, Context context) {
    synchronized (stationCache) {
      for (Station station : stations) {
        stationCache.put(station.getId(), new StationEntry(station, context));
      }
    }
  }

  public static Station getStation(String stationId, Context context) {
    StationEntry stationEntry = stationCache.get(stationId);
    if (stationEntry == null ||
        stationEntry.expiration < System.currentTimeMillis()) {
      stationEntry = new StationEntry(
          Station.StationFactory.downloadStation(stationId, context), context);
      stationCache.put(stationId, stationEntry);
    }
    return stationEntry.station;
  }

  public static boolean entryPresentAndNotExpired(String stationId) {
    StationEntry stationEntry = stationCache.get(stationId);
    return (stationEntry != null && !stationEntry.isExpired());
  }

  public static void clear() {
    synchronized (stationCache) {
      stationCache.clear();
    }
  }
}
