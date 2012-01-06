package org.npr.android.util;

public class FavoriteStationEntry {

  public final int id;
  public final String stationId;
  public final String preset;

  public FavoriteStationEntry(int id, String stationId, String preset) {
    this.id = id;
    this.stationId = stationId;
    this.preset = preset;
  }
}
