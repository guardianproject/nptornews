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

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.npr.android.util.Tracker;
import org.npr.android.util.Tracker.ActivityMeasurement;
import org.npr.api.ApiConstants;
import org.npr.api.StoryGrouping;
import org.npr.api.Topic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewsTopicActivity extends TitleActivity implements
    OnItemClickListener {
  private static final String LOG_TAG = NewsTopicActivity.class.getName();
  // Only show topics from this predefined selection
  // See http://m.npr.org/news for the preferred list
  private static final int[] selectedTopics =
      new int[]{1003, 1004, 1014, 1006, 1106, 1007, 1128, 1019, 1008, 1032};

  private final Handler handler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case 0:
          listView.setAdapter(listAdapter);
          stopIndeterminateProgressIndicator();
          break;
        default:
          Toast.makeText(NewsTopicActivity.this,
              getResources().getText(R.string.msg_check_connection),
              Toast.LENGTH_LONG)
              .show();
      }
    }
  };

  private ListAdapter listAdapter;
  private ListView listView;

  @SuppressWarnings("unchecked")
  private int constructList() {
    List<? extends StoryGrouping> groupings =
        Topic.factory.downloadStoryGroupings(-1);
    ArrayList<Topic> filteredList =
        new ArrayList<Topic>(selectedTopics.length);
    for (int i : selectedTopics) {
      for (StoryGrouping topic : groupings) {
        if (topic.getId().equals(Integer.toString(i))) {
          filteredList.add((Topic) topic);
          break;
        }
      }
    }
    listAdapter = new NewsTopicAdapter<Topic>(filteredList);
    int message = 0;

    if (groupings == null) {
      message = 1;
    }
    return message;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ViewGroup container = (ViewGroup) findViewById(R.id.Content);
    ViewGroup.inflate(this, R.layout.news_topics, container);
    listView = (ListView) findViewById(R.id.topic_list);
    listView.setOnItemClickListener(this);

    initializeList();
  }

  private void initializeList() {
    startIndeterminateProgressIndicator();
    Thread listInitThread = new Thread(new Runnable() {
      public void run() {
        int result = NewsTopicActivity.this.constructList();
        handler.sendEmptyMessage(result);
      }
    });
    listInitThread.start();
  }

  @Override
  public CharSequence getMainTitle() {
    return getString(R.string.msg_main_title_topics);
  }

  @Override
  public void trackNow() {
    StringBuilder pageName = new StringBuilder("News");
    Tracker.instance(getApplication()).trackPage(
        new ActivityMeasurement(pageName.toString(), "News"));
  }

  @Override
  public boolean isRefreshable() {
    return true;
  }

  @Override
  public void refresh() {
    initializeList();
  }

  // TODO: Replace this with an ArrayList and default list item layout
  private class NewsTopicAdapter<T extends StoryGrouping> extends
      ArrayAdapter<T> {

    public NewsTopicAdapter(List<T> groupings) {
      super(NewsTopicActivity.this, R.layout.news_topic_item,
          android.R.id.text1, groupings);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      View v = super.getView(position, convertView, parent);
      ImageView icon = (ImageView) v.findViewById(R.id.icon);
      if (icon != null) {
        icon.setVisibility(View.INVISIBLE);
      } else {
        Log.e(LOG_TAG, "Could not find 'icon' view in list item at position "
            + position);
      }

      return v;
    }
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position,
                          long id) {
    StoryGrouping item = (StoryGrouping) parent.getItemAtPosition(position);

    String grouping = getString(
        getIntent().getIntExtra(Constants.EXTRA_SUBACTIVITY_ID, -1)
    );
    String description = item.getTitle();
    String topicId = item.getId();
    Map<String, String> params = new HashMap<String, String>();
    params.put(ApiConstants.PARAM_ID, topicId);
    params.put(ApiConstants.PARAM_FIELDS, ApiConstants.STORY_FIELDS);
    params.put(ApiConstants.PARAM_SORT, "assigned");
    String url =
        ApiConstants.instance().createUrl(ApiConstants.STORY_PATH, params);

    Intent i = new Intent(this, NewsListActivity.class);
    i.putExtra(Constants.EXTRA_QUERY_URL, url);
    i.putExtra(Constants.EXTRA_DESCRIPTION, description);
    i.putExtra(Constants.EXTRA_GROUPING, grouping);
    i.putExtra(Constants.EXTRA_SIZE, 10);
    startActivityWithoutAnimation(i);
  }
}
