package org.npr.android.util;

import org.npr.api.Station;

import java.util.Hashtable;
import java.util.List;

public class StationCache {

  private static class StationEntry {

    public StationEntry(Station station) {
      this.station = station;
      this.expiration = System.currentTimeMillis() + 86400000; // 24 hours
    }

    public Station station;
    public long expiration;

    public boolean isExpired() {
      return expiration < System.currentTimeMillis();
    }
  }

  private static final Hashtable<String, StationEntry> stationCache =
      new Hashtable<String, StationEntry>();

  public static void addAll(List<Station> stations) {
    synchronized (stationCache) {
      for (Station station : stations) {
        stationCache.put(station.getId(), new StationEntry(station));
      }
    }
  }

  public static Station getStation(String stationId) {
    StationEntry stationEntry = stationCache.get(stationId);
    if (stationEntry == null ||
        stationEntry.expiration < System.currentTimeMillis()) {
      stationEntry = new StationEntry(
          Station.StationFactory.downloadStation(stationId));
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
