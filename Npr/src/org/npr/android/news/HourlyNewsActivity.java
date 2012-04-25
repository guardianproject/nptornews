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

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.npr.android.util.Tracker;
import org.npr.api.Podcast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class HourlyNewsActivity extends RootActivity implements
    OnClickListener {
  private static final String LOG_TAG = HourlyNewsActivity.class.getName();

  private Podcast podcast;
  private String pubDate;
  private String duration;

  private TextView hourlyPubDate;
  private TextView hourlyDuration;
  private Button playHourly;

  private static final int MSG_ERROR_MESSAGE = 0;
  private static final int MSG_HOURLY_READY = 1;
  private final Handler handler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case MSG_ERROR_MESSAGE:
          // Let the user know something was wrong, most likely a bad connection
          Toast.makeText(getApplicationContext(),
              getResources().getString(R.string.msg_check_connection),
              Toast.LENGTH_LONG).show();
          break;
        case MSG_HOURLY_READY:
          try {
            Date parsed = NewsStoryActivity.apiDateFormat.parse(pubDate);
            SimpleDateFormat hourlyFormat = new SimpleDateFormat(
                "MMM d, yyyy, h:mm a z");
            hourlyPubDate.setText(hourlyFormat.format(parsed));
          } catch (ParseException e) {
            Log.e(LOG_TAG, "date format", e);
          }
          hourlyDuration.setText(duration);
          playHourly.setEnabled(true);
          break;
      }
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    ViewGroup container = (ViewGroup) findViewById(R.id.TitleContent);
    ViewGroup.inflate(this, R.layout.hourly_news, container);

    hourlyPubDate = (TextView) findViewById(R.id.HourlyPubDate);
    hourlyDuration = (TextView) findViewById(R.id.HourlyDuration);

    playHourly = (Button) findViewById(R.id.HourlyListenNowButton);
    playHourly.setOnClickListener(this);
    playHourly.setEnabled(false);

    // It takes a few seconds to download and parse so run in another thread
    Thread hourlyDownloader = new Thread(new Runnable() {
      public void run() {
        try {
          final String hourlyUrlCall =
              "http://www.npr.org/rss/podcast.php?id=500005";
          podcast = Podcast.PodcastFactory.downloadPodcast(hourlyUrlCall);
          List<Podcast.Item> items = podcast.getItems();

          // The hourly podcast has only one item
          Podcast.Item info = items.get(0);

          // Set the member/instance variables
          pubDate = info.getPubDate();
          duration = info.getDuration();

          // Success! let the activity know it can request playback
          handler.sendEmptyMessage(MSG_HOURLY_READY);

        } catch (Exception e) {
          Log.e(LOG_TAG, "error downloading hourly", e);
          handler.sendEmptyMessage(MSG_ERROR_MESSAGE);
        }
      }
    });

    hourlyDownloader.start();
  }

  @Override
  public void onClick(View v) {
    super.onClick(v);
    switch (v.getId()) {
      case R.id.HourlyListenNowButton:
        Log.w(LOG_TAG, "Play hourly news podcast");
        final Playable newsPlayable = Playable.PlayableFactory
            .fromPodcastItem(podcast.getItems().get(0), "", "");
        newsPlayable.setActivityName(HourlyNewsActivity.class.getName());
        playSingleNow(newsPlayable);
        break;
    }
  }

  @Override
  public void trackNow() {
    // hourly newscast tracking
    StringBuilder pageName = new StringBuilder(
        getString(R.string.msg_main_subactivity_hourly));
      Tracker.instance(getApplication()).trackPage(
          new Tracker.ActivityMeasurement(pageName.toString(), "Newscast"));

  }
}