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

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.npr.android.util.ArrayUtils;
import org.npr.android.util.DisplayUtils;
import org.npr.android.util.Tracker;
import org.npr.api.ApiConstants;
import org.npr.api.Program;
import org.npr.api.PublicBroadcastingClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Author: Jeremy Wadsack
 */
public class AllProgramsActivity extends TitleActivity implements
    AdapterView.OnItemClickListener {
  private static final String LOG_TAG = AllProgramsActivity.class.getName();

  private Integer[] nowPlayingIDs;
  private ListAdapter listAdapter;
  private ListView listView;
  private String[] categories;


  private final Handler handler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case 0:
          listView.setAdapter(listAdapter);
          stopIndeterminateProgressIndicator();
          break;
        default:
          Toast.makeText(AllProgramsActivity.this,
              getResources().getText(R.string.msg_check_connection),
              Toast.LENGTH_LONG)
              .show();
      }
    }
  };


  @SuppressWarnings("unchecked")
  protected int constructList() {
    try {
      nowPlayingIDs = PublicBroadcastingClient.getNowPlayingIds();
    } catch (IOException ignore) {
    }
    List<Program> programs = new Program.ProgramFactory(getContentResolver())
        .downloadStoryGroupings(-1);
    listAdapter = new ProgramListAdapter(categorizePrograms(programs));

    int message = 0;

    if (programs == null) {
      message = 1;
    }
    return message;
  }


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    categories = getResources().getStringArray(R.array.program_categories);

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
        int result = AllProgramsActivity.this.constructList();
        handler.sendEmptyMessage(result);
      }
    });
    listInitThread.start();
  }


  @Override
  public CharSequence getMainTitle() {
    return getString(R.string.msg_main_subactivity_programs);
  }

  @Override
  public void trackNow() {
    StringBuilder pageName = new StringBuilder("All Programs");
    Tracker.instance(getApplication()).trackPage(
        new Tracker.ActivityMeasurement(pageName.toString(), "News"));
  }


  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position,
                          long id) {
    int type = getIntent().getIntExtra(Constants.EXTRA_SUBACTIVITY_ID, -1);
    Program item =
        ((CategorizedProgram) parent.getItemAtPosition(position)).program;
    if (item == null) {
      return;
    }
    Intent i = new Intent(this, ProgramStoryListActivity.class);
    i.putExtra(Constants.EXTRA_LIVE_STREAM_RSS_URL, item.getLiveStreamUrl());
    i.putExtra(Constants.EXTRA_TEASER_ONLY, true);

    String grouping = getString(type);
    String description = item.getTitle();
    String topicId = item.getId();
    String url = item.getPodcastUrl();
    if (url != null) {
      i.putExtra(Constants.EXTRA_PODCAST_URL, url);
    } else {
      Map<String, String> params = new HashMap<String, String>();
      params.put(ApiConstants.PARAM_ID, topicId);
      params.put(ApiConstants.PARAM_FIELDS, ApiConstants.STORY_FIELDS);
      params.put(ApiConstants.PARAM_SORT, "assigned");
      params.put(ApiConstants.PARAM_DATE, "current");

      url =
          ApiConstants.instance().createUrl(ApiConstants.STORY_PATH, params);
      i.putExtra(Constants.EXTRA_QUERY_URL, url);
    }

    if (topicId != null && Character.isDigit(topicId.charAt(0))) {
      int intId = Integer.parseInt(topicId);
      if (nowPlayingIDs != null &&
          ArrayUtils.indexOf(nowPlayingIDs, intId) != -1) {
        i.putExtra(Constants.EXTRA_ON_AIR, true);
      }
    }

    i.putExtra(Constants.EXTRA_DESCRIPTION, description);
    i.putExtra(Constants.EXTRA_GROUPING, grouping);
    i.putExtra(Constants.EXTRA_SIZE, 10);
    startActivityWithoutAnimation(i);
  }


  private class CategorizedProgram implements Comparable<CategorizedProgram> {
    public final Program program;
    public final int categoryIndex;
    public final String categoryName;

    public CategorizedProgram(int category, Program program) {
      this.categoryIndex = category;
      this.program = program;
      this.categoryName = null;
    }

    public CategorizedProgram(int category) {
      this.categoryIndex = category;
      this.program = null;
      this.categoryName = categories[categoryIndex];
    }

    public boolean isHeader() {
      return program == null && categoryName != null;
    }

    @Override
    public int compareTo(CategorizedProgram other) {
      // Lower category index first
      int ret = categoryIndex - other.categoryIndex;
      if (ret == 0 && program != null) {
        // If same category, then sort preferred programs first
        ret = getTitleRank(program.getTitle()) -
            getTitleRank(other.program.getTitle());
      }
      if (ret == 0 && program != null) {
        // Last resort, just sort alphabetically
        ret = program.getTitle().compareTo(other.program.getTitle());
      }
      return ret;
    }

    @Override
    public String toString() {
      return program == null ? categoryName : program.toString();
    }
  }


  private class ProgramListAdapter extends
      ArrayAdapter<CategorizedProgram> {

    public ProgramListAdapter(List<CategorizedProgram> programList) {
      super(AllProgramsActivity.this, R.layout.news_topic_item,
          android.R.id.text1, programList);
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      View v = super.getView(position, convertView, parent);

      CategorizedProgram item = getItem(position);
      ImageView icon = (ImageView) v.findViewById(R.id.icon);
      if (icon != null) {
        int id = -1;
        if (item != null && item.program != null) {
          String topicId = item.program.getId();
          if (topicId != null && Character.isDigit(topicId.charAt(0))) {
            id = Integer.parseInt(topicId);
          }
        }
        if (nowPlayingIDs != null &&
            id != -1 &&
            ArrayUtils.indexOf(nowPlayingIDs, id) != -1) {
          icon.setVisibility(View.VISIBLE);
        } else {
          icon.setVisibility(View.INVISIBLE);
        }
      } else {
        Log.e(LOG_TAG, "Could not find 'icon' view in list item at position "
            + position);
      }

      if (item != null && item.isHeader()) {
        v.setEnabled(false);
        v.setBackgroundDrawable(
            getResources().getDrawable(R.drawable.top_stories_title_background)
        );
        v.getLayoutParams().height =
          DisplayUtils.convertToDIP(getContext(), 30);
        ((TextView) v.findViewById(android.R.id.text1))
          .setTextColor(getResources().getColor(R.color.news_title_text));
      } else {
        v.setEnabled(true);
        v.setBackgroundDrawable(null);
        v.getLayoutParams().height =
          DisplayUtils.convertToDIP(getContext(), 50);
        ((TextView) v.findViewById(android.R.id.text1))
          .setTextColor(getResources().getColor(R.color.black));
      }

      return v;
    }

  }


  private List<CategorizedProgram> categorizePrograms(List<Program> programs) {
    List<CategorizedProgram> orderedList =
        new ArrayList<CategorizedProgram>(programs.size());

    // Associate each program with its category
    for (Program p : programs) {
      int i = getCategoryById(p.getId());
      if (i < 0) {
        i = getCategoryByTitle(p.getTitle());
      }
      if (i < 0 ) {
        i = categories.length - 1;
      }
      orderedList.add(new CategorizedProgram(i, p));
    }

    // Sort them
    Collections.sort(orderedList);

    // Now add the categories headers themselves
    List<CategorizedProgram> listWithHeaders =
        new ArrayList<CategorizedProgram>(programs.size() + categories.length);
    int oldCategory = -1;
    for (CategorizedProgram p : orderedList) {
      if (oldCategory != p.categoryIndex) {
        listWithHeaders.add(new CategorizedProgram(p.categoryIndex));
        oldCategory = p.categoryIndex;
      }
      listWithHeaders.add(p);
    }

    return listWithHeaders;
  }

  // For a given program id or title, what category should it be in?
  private static int getCategoryById(String id) {
    if (id == null) {
      return -1;
    }
    if (id.equals("2") || id.equals("13") || id.equals("3") || id.equals("5")
        || id.equals("46") || id.equals("7") || id.equals("10")
        || id.equals("38")) {
      return 0; // News
    } else if (id.equals("35") || id.equals("18")) {
      return 1; // Arts & Life
    } else if (id.equals("37") || id.equals("20") || id.equals("24")
        || id.equals("39") || id.equals("36")) {
      return 2; // Music
    } else if (id.equals("")) {
      return 3; // Special Series
    }
    return -1;
  }

  private static int getCategoryByTitle(String title) {
    if (title == null) {
      return -1;
    }
    if (title.equals("Weekends on All Things Considered")
        || title.equals("Science Friday") || title.equals("Diane Rehm (WAMU)")
        || title.equals("On the Media (WNYC)")) {
      return 0; // News
    } else if (title.equals("Your Health") || title.equals("Radiolab (WNYC)")
        || title.equals("Snap Judgment")) {
      return 1; // Arts & Life
    } else if (title.equals("Thistle & Shamrock")
        || title.equals("From the Top")) {
      return 2; // Music
    } else if (title.equals("Planet Money")) {
      return 3; // Special Series
    }
    return -1;
  }



  // Order of stories by preferred rank as shown on npr.org in Programs menu
  private final List<String> sortedProgramTitles = Arrays.asList(
      "Morning Edition",
      "All Things Considered",
      "Fresh Air from WHYY",
      "Diane Rehm (WAMU)",
      "On the Media (WNYC)",
      "On Point (WBUR)",
      "Talk of the Nation",
      "Science Friday",
      "Tell Me More",
      "Weekend Edition Saturday",
      "Weekend Edition Sunday",
      "Also heard on NPR stations:",
      "Marketplace APM",
      "Car Talk",
      "Radiolab (WNYC)",
      "Snap Judgment",
      "Wait Wait...Don't Tell Me!",
      "Also heard on NPR stations:",
      "This American Life PRI",
      "A Prairie Home Companion APM",
      "All Songs Considered",
      "From the Top",
      "JazzSet",
      "Piano Jazz",
      "Mountain Stage",
      "Song of the Day",
      "Thistle & Shamrock",
      "World Cafe",
      "World of Opera",
      "StoryCorps",
      "Planet Money",
      "Picture Show",
      "Krulwich Wonders..."
  );

  private HashMap<String, Integer> programTitlesRank = null;

  private int getTitleRank(String title) {
    if (programTitlesRank == null) {
      programTitlesRank =
          new HashMap<String, Integer>(sortedProgramTitles.size());
      for (int i = 0; i < sortedProgramTitles.size(); i++) {
        programTitlesRank.put(sortedProgramTitles.get(i), i + 1);
      }
    }

    Integer rank = programTitlesRank.get(title);
    return rank == null ? Integer.MAX_VALUE : rank;
  }

}
