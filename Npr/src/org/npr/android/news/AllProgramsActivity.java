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
import org.npr.android.util.Program;
import org.npr.api.ApiConstants;
import org.npr.api.PublicBroadcastingClient;

import java.io.IOException;
import java.util.*;

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
      nowPlayingIDs = PublicBroadcastingClient.getNowPlayingIds(this);

      List<Program> programs = new Program.ProgramFactory().downloadPrograms(this);
      listAdapter = new ProgramListAdapter(categorizePrograms(programs));

      if (programs != null) {
        return 0;
      }

    } catch (IOException e) {
      Log.e(LOG_TAG, "Error constructing program list", e);
    }
    return 1;
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
  public void onItemClick(AdapterView<?> parent, View view, int position,
                          long id) {
    int type = getIntent().getIntExtra(Constants.EXTRA_SUBACTIVITY_ID, -1);
    Program item =
        ((CategorizedProgram) parent.getItemAtPosition(position)).program;
    if (item == null) {
      return;
    }
    Intent i = new Intent(this, ProgramStoryListActivity.class);
    i.putExtra(Constants.EXTRA_LIVE_STREAM_RSS_URL, item.getLiveStationsUrl());
    i.putExtra(Constants.EXTRA_TEASER_ONLY, true);

    String grouping = getString(type);
    String description = item.getName();
    String topicId = item.getNprId();
    String url = item.getSource();
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
        // Sort by order read
        ret = program.getSortOrder() - other.program.getSortOrder();
      }
      return ret;
    }

    @Override
    public String toString() {
      return program == null ? categoryName : program.getName();
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
          String topicId = item.program.getNprId();
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
      int i = getCategoryById(p.getNprId());
      if (i < 0) {
        i = getCategoryByTitle(p.getName());
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
        || id.equals("39")) {
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
}
