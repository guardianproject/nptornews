package org.npr.android.util;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import java.util.HashMap;
import java.util.Map;

public class Tracker {
  private static final String LOG_TAG = Tracker.class.getName();

  private static Tracker tracker;
  public static final String PAGE_NAME_SEPARATOR = ": ";

  private final Application application;
  
  private  GoogleAnalyticsTracker gaTracker = null;

  public static Tracker instance(Application application) {
    if (tracker == null) {
      tracker = new Tracker(application);
    }
    return tracker;
  }


  public Tracker(Application application) {
    this.application = application;
    
    if (!isDebuggableSet()) {
      gaTracker = GoogleAnalyticsTracker.getInstance();

      // Start the tracker in manual dispatch mode.
      // Remember to dispatch() after every trackPageView
      gaTracker.startNewSession("UA-5828686-6", application);
      gaTracker.trackPageView("/version%20" + getVersionName());
      Log.d(LOG_TAG, "Tracking version " + getVersionName());
      gaTracker.dispatch();
    } else {
      Log.d(LOG_TAG, "Not tracking debuggable is set");
    }
  }

  public void finish() {
    if (gaTracker != null) {
      gaTracker.stopSession();
    }
  }

  public void trackPage(ActivityMeasurement activityMeasurement) {
    if (gaTracker != null) {
      // pass %20 instead of blanks to get blanks to appear in Analytics reports
      String gaPageName = activityMeasurement.pageName.replace(" ", "%20");
      gaTracker.trackPageView("/" + gaPageName);
      Log.d(LOG_TAG, "Tracking page /" + gaPageName);
      gaTracker.dispatch();
    }
  }

  public void trackLink(LinkEvent event) {
    if (gaTracker != null) {
      gaTracker.trackEvent(
          event.getCategory(),
          event.getAction(),
          event.getLabel(),
          event.getValue());
    }
    Log.d(LOG_TAG, "Tracking link " + event.getCategory() + "/" +
        "" + event.getAction() + " " + event.getLabel());
  }


  private boolean isDebuggableSet() {
    int flags = 0;
    try {
      PackageInfo pi = application.getPackageManager().getPackageInfo(
          application.getPackageName(), 0);
      flags = pi.applicationInfo.flags;
    } catch (PackageManager.NameNotFoundException ignored) { }

    return (flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
  }

  private String getVersionName() {
    String version;
    try {
      PackageInfo pi = application.getPackageManager().getPackageInfo(
          application.getPackageName(), 0);
      version = pi.versionName;
    } catch (PackageManager.NameNotFoundException e) {
      version = "Package name not found";
    }
    return version;
  }



  public static class ActivityMeasurement {
    public ActivityMeasurement(String pageName, String channel, String orgId) {
      this.pageName = pageName;
      this.channel = channel;
      this.orgId = orgId;

      values = new HashMap<String, String>();
      values.put(pageNameVars[0], pageName);
      values.put(pageNameVars[1], pageName);
      values.put(pageNameVars[2], pageName);

      values.put(contentTypeVars[0], contentType);
      values.put(contentTypeVars[1], contentType);

      values.put(versionNumberVars[0], versionNumber);
      values.put(versionNumberVars[1], versionNumber);

      values.put(channelVars[0], channel);

      values.put(eventsVars[0], events);

      values.put(orgIdVars[0], orgId);
      values.put(orgIdVars[1], orgId);
    }

    public ActivityMeasurement(String pageName, String channel) {
      // For all other pages, this should be set to "1" (which represents NPR).
      this(pageName, channel, "1");
    }

    protected Map<String, String> values;
    // pageName

    // Title of the current page, according to the table at
    // http://spreadsheets.google.com/pub?key=tvTr3NSj4Cb6kgG5kUQ7DQw&output=html

    // prop3, eVar3
    // Title of current page (same as pageName)
    private String pageName;
    private static String[] pageNameVars =
        new String[] {"pageName", "prop3", "eVar3"};

    // prop5, eVar5
    // Content Type - Always set to constant "Android"
    private final String contentType = "Android";
    private static String[] contentTypeVars = new String[] {"prop5", "eVar6"};

    // prop44, eVar44
    // Version number of Android application
    private final String versionNumber = "";
    private static String[] versionNumberVars =
        new String[] {"prop44", "eVar44"};

    // channel
    // Section within application. Examples: Home, News, Stations, Search,
    // Playlist
    @SuppressWarnings("unused")
    private String channel = "";
    private static String[] channelVars = new String[] {"channel"};

    // events
    // For all standard page views, should contain "event2" as value.
    private final String events = "event2";
    protected static String[] eventsVars = new String[] {"events"};

    // prop20, eVar20
    protected String orgId;
    protected static String[] orgIdVars = new String[] {"prop20", "eVar20"};
  }

  public static class StoryDetailsMeasurement extends ActivityMeasurement {
    public StoryDetailsMeasurement(String pageName, String channel,
        String orgId, String topicId, String storyId) {
      super(pageName, channel, orgId);
      this.storyId = storyId;
      this.topicId = topicId;

      values.put(storyIdVars[0], storyId);
      values.put(storyIdVars[1], storyId);

      values.put(topicIdVars[0], topicId);
      values.put(topicIdVars[1], topicId);
    }

    // prop4, eVar4
    // Story ID -- leave blank for all but news article detail pages. When on an
    // article detail page, put the story id that you have from the API call.
    @SuppressWarnings("unused")
    private String storyId;
    private static String[] storyIdVars = new String[] {"prop4", "eVar4"};

    // prop7, eVar7
    // On a story page, this should contain the primary topic ID of the story.
    @SuppressWarnings("unused")
    private String topicId;
    private static String[] topicIdVars = new String[] {"prop7", "eVar7"};

    // prop20, eVar20
    // When on a story page, this should contain the orgId for the story.
  }

  public static class StoryListMeasurement extends ActivityMeasurement {
    public StoryListMeasurement(String pageName, String channel, String topicId) {
      super(pageName, channel);
      this.topicId = topicId;

      values.put(topicIdVars[0], topicId);
      values.put(topicIdVars[1], topicId);
    }

    // prop7, eVar7
    // When on a list of stories within a topic, this should contain the
    // topicId.
    @SuppressWarnings("unused")
    private String topicId;
    private static String[] topicIdVars = new String[] {"prop7", "eVar7"};
  }

  public static class StationDetailsMeasurement extends ActivityMeasurement {

    public StationDetailsMeasurement(String pageName, String channel,
        String orgId) {
      super(pageName, channel, orgId);
    }
    // prop20, eVar20
    // When on a station-specific page, this should contain the orgId of the
    // station.

    // prop21, eVar21
    // When on program-specific pages and story pages, the ID of the program.
  }

  public static class SearchResultsMeasurement extends ActivityMeasurement {

    public SearchResultsMeasurement(String pageName, String channel, String query,
        int resultsCount) {
      super(pageName, channel);
      this.query = query;
      this.resultsCount = resultsCount;
      values.put(queryVars[0], query);
      values.put(queryVars[1], query);
      
      values.put(resultsCountVars[0], "" + resultsCount);
      values.put(resultsCountVars[1], "" + resultsCount);

      // a) event1 should be passed along with the standard track() call (in
      // addition to event2)
      String events = values.get(eventsVars[0]);
      events += ",event1";
      values.put(eventsVars[0], events);
    }
    // When presenting the Search Results screen:
    //
    // b) the search query string used, should be set in prop1 and eVar1
    @SuppressWarnings("unused")
    private String query;
    private static String[] queryVars = new String[] {"prop1", "eVar1"};
    // c) the number of results returned should be set in prop2 and eVar2
    @SuppressWarnings("unused")
    private int resultsCount;
    private static String[] resultsCountVars = new String[] {"prop2", "eVar2"};
    // d) the search date range should be set in prop12 and eVar12 (format TBD)

  }
  
  public static class StationListMeasurement extends ActivityMeasurement {

    public StationListMeasurement(String pageName, String channel,
        String query) {
      super(pageName, channel);
      this.query = query;
      values.put(queryVars[0], query);
      values.put(queryVars[1], query);

      // event27 should be fired (in addition to event2).
      String events = values.get(eventsVars[0]);
      events += ",event27";
      values.put(eventsVars[0], events);
    }
    // When presenting the list of stations, the query used to select the
    // station (whether zip code, call letters, or GPS coordinates) should be
    // passed to prop43/eVar43
    @SuppressWarnings("unused")
    private String query;
    private static String[] queryVars = new String[] {"prop43", "eVar43"};
  }

  
  public static class LinkEvent {
    private String category;
    private String action;
    private String label;
    private int value;

    public LinkEvent(String category, String action, String label,
                     int value) {
      this.category = category;
      this.action = action;
      this.label = label;
      this.value = value;
    }

    public LinkEvent(String category, String action, String label) {
      this.category = category;
      this.action = action;
      this.label = label;
    }

    public LinkEvent(String category, String action) {
      this.category = category;
      this.action = action;
    }

    public int getValue() {
      return value;
    }

    public String getCategory() {
      return category;
    }

    public String getAction() {
      return action;
    }

    public String getLabel() {
      return label;
    }
  }
  
  public static class PlayEvent extends LinkEvent {
    public PlayEvent(String url) {
      super("Audio", "Play", url);
    }
  }

  public static class PauseEvent extends LinkEvent {
    public PauseEvent(String url) {
      super("Audio", "Pause", url);
    }
  }

  public static class StopEvent extends LinkEvent {
    public StopEvent(String url) {
      super("Audio", "Stop", url);
    }
  }

  public static class AddToPlaylistEvent extends LinkEvent {
    public AddToPlaylistEvent(String url) {
      super("Add To Playlist", "Add", url);
    }
  }

}
