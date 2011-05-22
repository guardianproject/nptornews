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

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;

import org.npr.android.util.Tracker;
import org.npr.android.util.Tracker.ActivityMeasurement;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class SearchActivity extends TitleActivity implements
    OnClickListener, View.OnKeyListener {
  private EditText searchText;
  private Calendar startDate;
  private Calendar endDate;
  private Button startDateButton;
  private Button endDateButton;
  private final SimpleDateFormat dateFormat =
      new SimpleDateFormat("MMMM d, yyyy");
  private InputMethodManager inputMethodManager;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    ViewGroup container = (ViewGroup) findViewById(R.id.Content);
    ViewGroup.inflate(this, R.layout.search, container);

    searchText = (EditText) findViewById(R.id.SearchText);
    searchText.setOnKeyListener(this);
    inputMethodManager =
        (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    // only will trigger it if no physical keyboard is open
    if (inputMethodManager != null) {
      inputMethodManager.showSoftInput(searchText,
          InputMethodManager.SHOW_IMPLICIT);
    }

    ImageButton searchButton =
        (ImageButton) findViewById(R.id.search_go_button);
    searchButton.setOnClickListener(this);


    endDate = new GregorianCalendar();
    startDate = (Calendar) endDate.clone();
    startDate.add(Calendar.DATE, -7);

    startDateButton = (Button) findViewById(R.id.StartDateButton);
    endDateButton = (Button) findViewById(R.id.EndDateButton);
    startDateButton.setText(dateFormat.format(startDate.getTime()));
    endDateButton.setText(dateFormat.format(endDate.getTime()));
    startDateButton.setOnClickListener(this);
    endDateButton.setOnClickListener(this);
  }


  private void search() {
    if (inputMethodManager != null) {
      inputMethodManager.hideSoftInputFromWindow(
          searchText.getWindowToken(),
          InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    String text = searchText.getText().toString();
    if (text.length() > 0) {
      String start = getDate(startDate);
      String end = getDate(endDate);
      Intent i = new Intent(this, SearchResultsActivity.class);
      i.putExtra(SearchResultsActivity.EXTRA_START_DATE, start);
      i.putExtra(SearchResultsActivity.EXTRA_END_DATE, end);
      i.putExtra(Constants.EXTRA_QUERY_TERM, text);
      i.putExtra(Constants.EXTRA_SIZE, 10);

      startActivityWithoutAnimation(i);
    }
  }

  @Override
  public void onClick(View v) {
    super.onClick(v);
    switch (v.getId()) {
      case R.id.StartDateButton:
        OnDateSetListener callback = new OnDateSetListener() {
          @Override
          public void onDateSet(DatePicker view, int year, int monthOfYear,
                                int dayOfMonth) {
            startDate.set(Calendar.YEAR, year);
            startDate.set(Calendar.MONTH, monthOfYear);
            startDate.set(Calendar.DATE, dayOfMonth);
            startDateButton.setText(dateFormat.format(startDate.getTime()));
          }
        };
        new DatePickerDialog(this, callback, startDate.get(Calendar.YEAR),
            startDate.get(Calendar.MONTH), startDate.get(Calendar.DATE)).show();
        break;
      case R.id.EndDateButton:
        OnDateSetListener callbackEnd = new OnDateSetListener() {
          @Override
          public void onDateSet(DatePicker view, int year, int monthOfYear,
                                int dayOfMonth) {
            endDate.set(Calendar.YEAR, year);
            endDate.set(Calendar.MONTH, monthOfYear);
            endDate.set(Calendar.DATE, dayOfMonth);
            endDateButton.setText(dateFormat.format(endDate.getTime()));
          }
        };
        new DatePickerDialog(this, callbackEnd, endDate.get(Calendar.YEAR),
            endDate.get(Calendar.MONTH), endDate.get(Calendar.DATE)).show();
        break;
      case R.id.search_go_button:
        search();
    }
  }

  private String getDate(Calendar cal) {
    StringBuilder sb = new StringBuilder();
    sb.append(cal.get(Calendar.YEAR)).append("-");
    // Months are 0-based in Java, 1-based in NPR api.
    sb.append(cal.get(Calendar.MONTH) + 1).append("-");
    sb.append(cal.get(Calendar.DATE));
    return sb.toString();
  }

  @Override
  public CharSequence getMainTitle() {
    return getString(R.string.msg_main_subactivity_search);
  }

  @Override
  public void trackNow() {
    StringBuilder pageName = new StringBuilder("Search").append(Tracker.PAGE_NAME_SEPARATOR);
    pageName.append("Search Form");
    Tracker.instance(getApplication()).trackPage(
        new ActivityMeasurement(pageName.toString(), "Search"));
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
}
