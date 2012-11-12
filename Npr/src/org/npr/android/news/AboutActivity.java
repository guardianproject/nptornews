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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class AboutActivity extends TitleActivity implements OnClickListener {
  private static final String LOG_TAG = AboutActivity.class.getName();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    ViewGroup container = (ViewGroup) findViewById(R.id.Content);
    ViewGroup.inflate(this, R.layout.about, container);

    TextView tv = (TextView) findViewById(R.id.AboutText);
    Map<String, String> map = new LinkedHashMap<String, String>();
    map.put(getString(R.string.msg_about_developer),
        getString(R.string.msg_about_developer_value));
    map.put(getString(R.string.msg_about_designer),
        getString(R.string.msg_about_designer_value));
    map.put(getString(R.string.msg_about_producer),
        getString(R.string.msg_about_producer_value));
    map.put(getString(R.string.msg_about_contact),
        getString(R.string.msg_about_contact_value));
    map.put(getString(R.string.msg_about_version_name), getVersionName());
    map.put(getString(R.string.msg_about_version_code), "" + getVersionCode());
    populateField(map, tv);

    Button terms = (Button) findViewById(R.id.TermsButton);
    terms.setOnClickListener(this);
    Button privacy = (Button) findViewById(R.id.PrivacyButton);
    privacy.setOnClickListener(this);
    Button feedback = (Button) findViewById(R.id.FeedbackButton);
    feedback.setOnClickListener(this);
    Button share = (Button) findViewById(R.id.ShareButton);
    share.setOnClickListener(this);
    Button learnMore = (Button) findViewById(R.id.LearnMoreButton);
    learnMore.setOnClickListener(this);
  }


  @Override
  public CharSequence getMainTitle() {
    return getString(R.string.msg_about_title);
  }


  @Override
  public void onClick(View v) {
    String url = null;
    switch (v.getId()) {
      case R.id.FeedbackButton:
        url = "http://m.npr.org/contact/index";
        break;
      case R.id.PrivacyButton:
        url = "http://m.npr.org/front/privacyPolicy";
        break;
      case R.id.TermsButton:
        url = "http://m.npr.org/front/termsOfUse";
        break;
      case R.id.LearnMoreButton:
        url = "http://npr.org/android";
        break;
      case R.id.ShareButton:
        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
        shareIntent.putExtra(Intent.EXTRA_TEXT,
            "https://market.android.com/search?q=pname:org.npr.android.news");
        shareIntent.setType("text/plain");
        startActivity(Intent.createChooser(shareIntent,
            getString(R.string.msg_share_app)));
        return;
    }

    if (url != null) {
      Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
      startActivity(i);
    } else {
      super.onClick(v);
    }
  }

  private void populateField(Map<String, String> values, TextView view) {
    StringBuilder sb = new StringBuilder();
    for (Entry<String, String> entry : values.entrySet()) {
      String fieldName = entry.getKey();
      String fieldValue = entry.getValue();
      sb.append(fieldName)
          .append(": ")
          .append("<b>").append(fieldValue).append("</b><br>");
    }
    view.setText(Html.fromHtml(sb.toString()));
  }

  private String getVersionName() {
    String version;
    try {
      PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
      version = pi.versionName;
    } catch (PackageManager.NameNotFoundException e) {
      Log.e(LOG_TAG, "Error getting package details", e);
      version = "Package name not found";
    }
    return version;
  }

  private int getVersionCode() {
    int version = -1;
    try {
      PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
      version = pi.versionCode;
    } catch (PackageManager.NameNotFoundException e) {
      Log.e(LOG_TAG, "Error getting package details", e);
    }
    return version;
  }

}
