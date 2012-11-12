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

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;

import org.npr.api.ApiConstants;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

public class SearchResultsActivity extends NewsListActivity implements
    View.OnKeyListener {

  private String query;
  private String start;
  private String end;
  public static final String EXTRA_END_DATE = "endDate";
  public static final String EXTRA_START_DATE = "startDate";
  private EditText searchBox;
  private InputMethodManager inputMethodManager;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Intent intent = getIntent();

    if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
      query = intent.getStringExtra(SearchManager.QUERY);

      // TODO: Not DRY. This is the same logic that sets up the default
      // dates assigned to the buttons in SearchActivity
      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-M-d");
      Calendar endDate = new GregorianCalendar();
      Calendar startDate = (Calendar) endDate.clone();
      startDate.add(Calendar.DATE, -7);
      start = dateFormat.format(startDate.getTime());
      end = dateFormat.format(endDate.getTime());
      // Also not DRY, ugh
      intent.putExtra(Constants.EXTRA_SIZE, 10);
    } else {
      query = intent.getStringExtra(Constants.EXTRA_QUERY_TERM);
      start = intent.getStringExtra(EXTRA_START_DATE);
      end = intent.getStringExtra(EXTRA_END_DATE);
    }

    // After setting up variables so that they can be accessed in getApiUrl
    // which is called at the end of super.onCreate
    super.onCreate(savedInstanceState);

    // Push the search box into the layout group below the logo bar and above
    // the 'search results' title bar
    ViewGroup container = (ViewGroup) findViewById(R.id.TitleGroup);
    container.addView(
      ViewGroup.inflate(
        this,
        R.layout.search_box,
        null
      ),
      0
    );
    searchBox = (EditText)findViewById(R.id.SearchText);
    searchBox.setText(query);
    searchBox.setOnKeyListener(this);
    ImageButton searchButton =
        (ImageButton) findViewById(R.id.search_go_button);
    searchButton.setOnClickListener(new View.OnClickListener(){
      @Override
      public void onClick(View view) {
        search();
      }
    });

    inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    if (inputMethodManager != null) {
      inputMethodManager.hideSoftInputFromWindow(
          searchBox.getWindowToken(),
          InputMethodManager.HIDE_NOT_ALWAYS);
    }
  }

  @Override
  protected String getApiUrl() {
    Map<String, String> params = new HashMap<String, String>();
    params.put("searchTerm", query);
    params.put("startDate", start);
    params.put("endDate", end);
    params.put("fields", ApiConstants.STORY_FIELDS);
    params.put("sort", "assigned");
    return ApiConstants.instance().createUrl(ApiConstants.STORY_PATH, params);
  }

  @Override
  public boolean onKey(View view, int i, KeyEvent keyEvent) {
    switch (keyEvent.getKeyCode()) {
      case KeyEvent.KEYCODE_SEARCH:
      case KeyEvent.KEYCODE_ENTER:
        search();
        return true;
    }
    return false;
  }

  private void search() {
    if (inputMethodManager != null) {
      inputMethodManager.hideSoftInputFromWindow(
          searchBox.getWindowToken(),
          InputMethodManager.HIDE_NOT_ALWAYS);
    }
    query = searchBox.getText().toString();
    if (query.length() > 0) {
      refresh();
    }
  }


  @Override
  public CharSequence getMainTitle() {
    return getString(R.string.msg_search_results_title);
  }


}
