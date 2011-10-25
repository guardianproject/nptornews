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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import org.npr.android.util.*;
import org.npr.android.util.Tracker.StoryListMeasurement;
import org.npr.api.ApiConstants;
import org.npr.api.Story;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewsListActivity extends TitleActivity implements
    OnItemClickListener {
  private static final String LOG_TAG = NewsListActivity.class.getName();

  private String description;
  private String grouping;
  private String topicId;
  private int initialSize;

  protected NewsListAdapter listAdapter;
  private ListView listView;
  private BannerView bannerView;

  private static final Map<String, Story> storyCache = new HashMap<String, Story>();

  private GestureDetector gestureDetector;
  private Story flungStory;

  // Need to store this from the long-press event to ignore the click event
  private int lastLongPressPosition = -1;

  private BroadcastReceiver playlistChangedReceiver;


  // Message handler to communicate between the gestures and the activity
  private final Handler handler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case ListItemGestureListener.MSG_LONG_PRESS: {

            lastLongPressPosition = msg.arg1;

          Story longPressStory = listAdapter.getItem(msg.arg1);
          if (longPressStory != null && longPressStory.getPlayable() != null) {
            PlaylistRepository playlistRepository =
                new PlaylistRepository(getApplicationContext(),
                    getContentResolver());

            PlaylistEntry playlistEntry =
                playlistRepository.getPlaylistItemFromStoryId(
                    longPressStory.getId());
            if (playlistEntry == null) {
              addAndPulseIcon(listView.getChildAt(msg.arg1 -
                  listView.getFirstVisiblePosition()));
              addStory(msg.arg1, true);
            } else {
              PlaylistEntry activeEntry =
                  playlistRepository.getPlaylistItemFromId(getActiveId());
              if (activeEntry != null) {
                playlistRepository.move(playlistEntry.playOrder,
                    activeEntry.playOrder + 1);
              }
              playEntryNow(playlistEntry);
            }
          }
        }
        break;

        case ListItemGestureListener.MSG_FLING: {

            flungStory = listAdapter.getItem(msg.arg1);
          if (flungStory != null && flungStory.getPlayable() != null) {
            PlaylistRepository playlistRepository =
                new PlaylistRepository(getApplicationContext(),
                    getContentResolver());

            if (playlistRepository.getPlaylistItemFromStoryId(
                flungStory.getId()
            ) == null) {
              animateListItemFling(
                  listView.getChildAt(msg.arg1 -
                      listView.getFirstVisiblePosition()),
                  msg.arg2,
                  true
              );
              addStory(msg.arg1, false);
            } else {
              animateListItemFling(
                  listView.getChildAt(msg.arg1 -
                      listView.getFirstVisiblePosition()),
                  msg.arg2,
                  false
              );
            }
          }
        }
        break;
      }
    }
  };

  public static Story getStoryFromCache(String storyId) {
    Story result = storyCache.get(storyId);
    if (result == null) {
      result = Story.StoryFactory.downloadStory(storyId);
      storyCache.put(storyId, result);
    }
    return result;
  }

  public static void addAllToStoryCache(List<Story> stories) {
    for (Story story : stories) {
      storyCache.put(story.getId(), story);
    }
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    storyCache.clear();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    if (getIntent() == null ||
        !(getIntent().hasExtra(Constants.EXTRA_QUERY_URL)
            || getIntent().hasExtra(Constants.EXTRA_PODCAST_URL))) {
      setDefaultIntent();
    }
    grouping = getIntent().getStringExtra(Constants.EXTRA_GROUPING);

    topicId = getIntent().getStringExtra(Constants.EXTRA_TOPIC_ID);
    initialSize = getIntent().getIntExtra(Constants.EXTRA_SIZE, 0);
    description = getIntent().getStringExtra(Constants.EXTRA_DESCRIPTION);
    super.onCreate(savedInstanceState);

    // TODO: move this to a layout?
    View titleBar = findViewById(R.id.TitleBar);
    titleBar.setBackgroundDrawable(getResources().getDrawable(
        R.drawable.top_stories_title_background));
    TextView titleText = (TextView) findViewById(R.id.TitleText);
    titleText.setTextColor(getResources().getColor(R.color.news_title_text));
    TextView titleRight = (TextView) findViewById(R.id.TitleRight);
    titleRight.setTextColor(getResources().getColor(R.color.news_title_text));

    ViewGroup bannerHolder = (ViewGroup) findViewById(R.id.SponsorshipBanner);
    ViewGroup.inflate(this, R.layout.banner, bannerHolder);
    bannerView = (BannerView) bannerHolder.getChildAt(0);
    bannerView.setPlayerView(getPlaylistView());

    ViewGroup container = (ViewGroup) findViewById(R.id.Content);
    ViewGroup.inflate(this, R.layout.news, container);

    listView = (ListView) findViewById(R.id.ListView01);

    listView.setOnItemClickListener(this);
    listAdapter = new NewsListAdapter(this);
    listAdapter.setStoriesLoadedListener(listener);
    listView.setAdapter(listAdapter);

    // Gesture detection
    gestureDetector = new GestureDetector(
        new ListItemGestureListener(listView, handler)
    );
    View.OnTouchListener gestureListener = new View.OnTouchListener() {
      public boolean onTouch(View v, MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
      }
    };
    listView.setOnTouchListener(gestureListener);

    playlistChangedReceiver = new PlaylistChangedReceiver();
    registerReceiver(playlistChangedReceiver,
        new IntentFilter(PlaylistRepository.PLAYLIST_CHANGED));

    addStories();
  }

  private NewsListAdapter.StoriesLoadedListener listener = new NewsListAdapter.StoriesLoadedListener() {
    @Override
    public void storiesLoaded() {
      bannerView.startCloseTimer();
    }
  };


  @Override
  protected void onStart() {
    super.onStart();
    handler.postDelayed(updateTime, UPDATE_SHORT_PERIOD);

  }

  @Override
  protected void onStop() {
    if (playlistChangedReceiver != null) {
      unregisterReceiver(playlistChangedReceiver);
      playlistChangedReceiver = null;
    }
    handler.removeCallbacks(updateTime);
    super.onStop();
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position,
                          long id) {

    // Ignore the click action after a long press on an audio track
    if (position == lastLongPressPosition) {
      lastLongPressPosition = -1;
      return;
    }

    Story s = (Story) parent.getAdapter().getItem(position);
    if (s == null) {
      addStories();
    } else {
      Intent i = new Intent(this, NewsStoryActivity.class);
      i.putExtra(
          Constants.EXTRA_STORY_ID_LIST,
          listAdapter.getStoryIdList()
      );
      i.putExtra(Constants.EXTRA_STORY_ID, s.getId());
      if (getIntent().hasExtra(Constants.EXTRA_TEASER_ONLY)) {
          i.putExtra(Constants.EXTRA_TEASER_ONLY,
                  getIntent().getBooleanExtra(Constants.EXTRA_TEASER_ONLY, false));
      }
      startActivityWithoutAnimation(i);
    }
  }

  private void addStories() {
    String url = getApiUrl();
    if (url != null) {
      // Adding these parameters to podcast urls (like WNYC) can break them
      Map<String, String> params = new HashMap<String, String>();
      params.put("startNum", "" + listAdapter.getCount());
      params.put("numResults", "" + initialSize);
      url = ApiConstants.instance().addParams(url, params);
    } else {
      url = getPodcastUrl();
    }
    listAdapter.addMoreStories(url, initialSize);
  }

  private void addStory(int position, boolean playNow) {
    Story story = listAdapter.getItem(position);
    String url = story.getPlayableUrl();
    if (url == null) {
      Log.w(LOG_TAG, "No audio for story " + position + " '" +
          listAdapter.getItem(position) + "'");
      return;
    }

    Tracker.LinkEvent e;
    PlaylistRepository playlistRepository =
        new PlaylistRepository(getApplicationContext(), getContentResolver());
    if (playNow) {
      PlaylistEntry activeEntry =
          playlistRepository.getPlaylistItemFromId(getActiveId());
      long playlistId;
      if (activeEntry != null) {
        playlistId = playlistRepository.insert(story, activeEntry.playOrder + 1);
      } else {
        playlistId = playlistRepository.add(story);
      }
      PlaylistEntry entry = playlistRepository.getPlaylistItemFromId(playlistId);
      this.playEntryNow(entry);
      e = new Tracker.PlayEvent(url);
    } else {
      playlistRepository.add(story);
      e = new Tracker.AddToPlaylistEvent(url);
    }

    Tracker.instance(getApplication()).trackLink(e);
  }


  private void addAndPulseIcon(final View listItem) {
    final ImageView icon = (ImageView) listItem.findViewById(R.id
        .NewsItemIcon);
    if (icon == null) {
      Log.w(LOG_TAG, "Could not find the icon for list item: " + listItem);
      return;
    }

    Animation pulse = AnimationUtils.loadAnimation(
        NewsListActivity.this,
        R.anim.pulse_and_flatten
    );
    pulse.setAnimationListener(new Animation.AnimationListener() {

      @Override
      public void onAnimationStart(Animation animation) {
        icon.setImageDrawable(getResources().getDrawable(R.drawable
            .news_item_adding_to_playlist));
      }

      @Override
      public void onAnimationEnd(Animation animation) {
        icon.setImageDrawable(getResources().getDrawable(R.drawable
            .news_item_in_playlist));
      }

      @Override
      public void onAnimationRepeat(Animation animation) {

      }

    });
    icon.startAnimation(pulse);
  }

  private void showMinusAndFadeIcon(final View listItem) {
    final ImageView icon = (ImageView) listItem.findViewById(R.id
        .NewsItemIcon);
    if (icon == null) {
      Log.w(LOG_TAG, "Could not find the icon for list item: " + listItem);
      return;
    }

    Animation pulse = AnimationUtils.loadAnimation(
        NewsListActivity.this,
        R.anim.delete_and_fade
    );
    pulse.setAnimationListener(new Animation.AnimationListener() {

      @Override
      public void onAnimationStart(Animation animation) {
        icon.setImageDrawable(getResources().getDrawable(R.drawable
            .news_item_deleting_from_playlist));
      }

      @Override
      public void onAnimationEnd(Animation animation) {
        icon.setImageDrawable(getResources().getDrawable(
            R.drawable.speaker_icon
        ));
        // Need to update the list after animation so the correct item is animated
        if (flungStory != null) {
          PlaylistRepository playlistRepository =
              new PlaylistRepository(getApplicationContext(), getContentResolver());
          playlistRepository.delete(
              playlistRepository.getPlaylistItemFromStoryId(flungStory.getId())
          );
        }
      }

      @Override
      public void onAnimationRepeat(Animation animation) {

      }

    });
    icon.startAnimation(pulse);
  }

  private void animateListItemFling(final View listItem, int direction,
                                    final boolean isAdding) {

    Animation fling = AnimationUtils.loadAnimation(
        this,
        direction < 0 ? R.anim.left_fling : R.anim.right_fling
    );

    fling.setAnimationListener(new Animation.AnimationListener() {
      @Override
      public void onAnimationEnd(Animation animation) {
        if (isAdding) {
          addAndPulseIcon(listItem);
        } else {
          showMinusAndFadeIcon(listItem);
        }
      }

      @Override
      public void onAnimationStart(Animation animation) {
      }

      @Override
      public void onAnimationRepeat(Animation animation) {
      }
    });

    listItem.startAnimation(fling);
  }


  /**
   * Gets the NPR API URL used for looking up the items for the
   * list. This class may add startNum and numResults parameters
   * to the query provided by this method.
   * <p/>
   * The default implementation pulls the URL from the Intent's
   * EXTRA_QUERY_URL value. A subclass may override this to get
   * the URL another way.
   *
   * @return A URL for the NPR API.
   */
  protected String getApiUrl() {
    return getIntent().getStringExtra(Constants.EXTRA_QUERY_URL);
  }

  /**
   * Gets a podcast RSS feed used for looking up the items for the list. This
   * will only be queried if getApiUrl returns null. No additional parameters
   * are added to the podcast list and all stories are shown immediately.
   * <p/>
   * The default implementation pulls the URL from the Intent's
   * EXTRA_PODCAST_URL value.
   *
   * @return A URL for a podcast RSS feed to be displayed as stories.
   */
  protected String getPodcastUrl() {
    return getIntent().getStringExtra(Constants.EXTRA_PODCAST_URL);
  }

  @Override
  public CharSequence getMainTitle() {
    return description;
  }


  @Override
  public void trackNow() {
    StringBuilder pageName =
        new StringBuilder("News").append(Tracker.PAGE_NAME_SEPARATOR);
    pageName.append(grouping).append(Tracker.PAGE_NAME_SEPARATOR);
    pageName.append(description);
    Tracker.instance(getApplication()).trackPage(
        new StoryListMeasurement(pageName.toString(), "News", topicId));
  }

  @Override
  public boolean isRefreshable() {
    return true;
  }

  @Override
  public void refresh() {
    listAdapter.clear();
    addStories();
  }

  // When first starting up, load the top news stories
  private void setDefaultIntent() {
    Map<String, String> params = new HashMap<String, String>();
    params.put("id", "1002");
    params.put("fields", ApiConstants.STORY_FIELDS);
    params.put("sort", "assigned");
    String url = ApiConstants.instance().createUrl(ApiConstants.STORY_PATH,
        params);

    String grouping = null;

    Intent i = new Intent(this, NewsListActivity.class)
        .putExtra(
            Constants.EXTRA_SUBACTIVITY_ID,
            R.string.msg_main_subactivity_top_stories
        )
        .putExtra(Constants.EXTRA_QUERY_URL, url)
        .putExtra(Constants.EXTRA_DESCRIPTION, "Top Stories")
        .putExtra(Constants.EXTRA_GROUPING, grouping)
        .putExtra(Constants.EXTRA_SIZE, 10);
    setIntent(i);
  }

  // Update every five seconds until we have a result
  private static final long UPDATE_SHORT_PERIOD = 5000L;

  // Update once a minute once we have a result
  private static final long UPDATE_LONG_PERIOD = 60000L;

  private Runnable updateTime = new Runnable() {
    public void run() {
      if (listAdapter == null) {
        handler.postDelayed(this, UPDATE_SHORT_PERIOD);
        return;
      }
      long lastUpdate = listAdapter.getLastUpdate();
      if (lastUpdate < 0) {
        handler.postDelayed(this, UPDATE_SHORT_PERIOD);
        return;
      }
      String label =
          String.format(getString(R.string.msg_update_format),
              TimeUtils.formatMillis(
                  System.currentTimeMillis() - lastUpdate,
                  TimeUnit.DAYS,
                  TimeUnit.MINUTES
              ));
      setTitleRight(label);
      handler.postDelayed(this, UPDATE_LONG_PERIOD);
    }
  };

  private class PlaylistChangedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      String operation = intent.getStringExtra(PlaylistRepository
          .PLAYLIST_CHANGE);
      if (operation != null &&
          (operation.equals(PlaylistRepository.PLAYLIST_ITEM_REMOVED) ||
              operation.equals(PlaylistRepository.PLAYLIST_CLEAR))) {
        listAdapter.notifyDataSetChanged();
      }
    }
  }
}
