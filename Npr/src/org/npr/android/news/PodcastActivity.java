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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.npr.android.util.DisplayUtils;
import org.npr.api.Podcast;
import org.npr.api.Podcast.Item;
import org.npr.api.Podcast.PodcastFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class PodcastActivity extends RootActivity implements
    OnItemClickListener {

  private static final String LOG_TAG = PodcastActivity.class.getName();
  public static final String EXTRA_PODCAST_URL = "extra_podcast_url";
  public static final String EXTRA_PODCAST_TITLE = "extra_podcast_title";

  private String url;
  private String title;

  private ArrayList<ListItem> listItems;
  private ArrayAdapter<ListItem> listAdapter;
  private Podcast podcast;
  private boolean podcastLoaded = false;

  private class ListItem {
    private final Item item;
    private final String header;

    private ListItem(Item item) {
      this.item = item;
      this.header = null;
    }

    private ListItem(String header) {
      this.item = null;
      this.header = header;
    }

    public boolean isHeader() {
      return header != null;
    }
  }

  private class ListItemAdapter extends ArrayAdapter<ListItem> {
    private final SimpleDateFormat hourlyFormat;

    public ListItemAdapter(Context context, ArrayList<ListItem> listItems) {
      super(context, R.layout.podcast_item, R.id.podcast_title, listItems);
      hourlyFormat = new SimpleDateFormat("MMM d, yyyy, h:mm a z");
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      convertView = super.getView(position, convertView, parent);
      ListItem listItem = this.getItem(position);
      if (listItem == null) {
        return convertView;
      }

      TextView titleText = (TextView) convertView.findViewById(R.id.podcast_title);
      TextView summary = (TextView) convertView.findViewById(R.id
          .podcast_summary);

      if (position == 0) {
        convertView.setEnabled(false);
        convertView.setBackgroundDrawable(getResources().getDrawable(
            R.drawable.station_name_background));
        convertView.getLayoutParams().height = AbsListView.LayoutParams
            .WRAP_CONTENT;
        convertView.setPadding(10, 8, 10, 8);
        titleText.setText(Html.fromHtml(title));
        titleText.setTextColor(getResources().getColor(R.color.black));
        titleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        if (podcastLoaded) {
          summary.setVisibility(View.VISIBLE);
          summary.setText(Html.fromHtml(podcast.getSummary()));
          summary.setTextColor(getResources().getColor(R.color.black));
          summary.setTextAppearance(getContext(),
              android.R.attr.textAppearanceSmall);
        } else {
          summary.setVisibility(View.GONE);
        }
      } else if (listItem.isHeader()) {
        convertView.setEnabled(false);
        convertView.setBackgroundDrawable(getResources().getDrawable(
            R.drawable.news_list_title_background));
        convertView.getLayoutParams().height =
            DisplayUtils.convertToDIP(getContext(), 20);
        convertView.setPadding(10, 0, 10, 0);
        titleText.setText(listItem.header);
        titleText.setTextColor(getResources().getColor(R.color.news_title_text));
        titleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        summary.setVisibility(View.GONE);
      } else {
        convertView.setEnabled(true);
        convertView.setBackgroundDrawable(null);
        convertView.getLayoutParams().height = AbsListView.LayoutParams
            .WRAP_CONTENT;
        convertView.setPadding(10, 18, 10, 18);
        titleText.setText(Html.fromHtml(listItem.item.getTitle()));
        titleText.setTextColor(getResources().getColor(R.color.black));
        titleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        try {
          Date parsed = NewsStoryActivity.apiDateFormat.parse(listItem.item
              .getPubDate());
          summary.setText(hourlyFormat.format(parsed));
        } catch (ParseException e) {
          Log.e(LOG_TAG, "date format", e);
          summary.setText("");
        }
        summary.setTextColor(Color.rgb(0x73, 0x73, 0x73));
        summary.setTextAppearance(getContext(),
            android.R.attr.textAppearanceSmall);
      }
      return convertView;
    }
  }

  private final Handler handler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      if (podcast != null) {
        podcastLoaded = true;
        stopIndeterminateProgressIndicator();
        if (podcast.getItems().size() > 0) {
          listItems.add(new ListItem(getString(
              R.string.msg_podcast_stream_header) +
              " (" + podcast.getItems().size() + ")"));
          for (Item item : podcast.getItems()) {
            listItems.add(new ListItem(item));
          }
          listAdapter.notifyDataSetChanged();
        }
      } else {
        final AlertDialog.Builder builder = new AlertDialog.Builder(PodcastActivity.this);
        builder.setTitle(R.string.msg_error);
        builder.setMessage(R.string.msg_parse_error);
        builder.setNeutralButton(R.string.msg_ok, new OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            PodcastActivity.this.finish();
          }
        });
        builder.create().show();
      }
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    ViewGroup container = (ViewGroup) findViewById(R.id.TitleContent);
    ViewGroup.inflate(this, R.layout.podcast, container);

    listItems = new ArrayList<ListItem>();
    if (getIntent().hasExtra(EXTRA_PODCAST_URL)) {
      url = getIntent().getStringExtra(EXTRA_PODCAST_URL);
      title = getIntent().getStringExtra(EXTRA_PODCAST_TITLE);
    } else if (getIntent().hasExtra(Constants.EXTRA_ACTIVITY_DATA)) {
      String activityInfo = getIntent().getStringExtra(Constants
          .EXTRA_ACTIVITY_DATA);
      url = activityInfo.substring(0, activityInfo.indexOf(' '));
      title = activityInfo.substring(activityInfo.indexOf(' ') + 1,
          activityInfo.length());
      listItems.add(new ListItem(title));
    }

    ListView streamList = (ListView) findViewById(R.id.podcast_stream_list);
    streamList.setOnItemClickListener(this);

    listAdapter = new ListItemAdapter(this, listItems);
    streamList.setAdapter(listAdapter);

    startIndeterminateProgressIndicator();
    new Thread(new Runnable() {
      @Override
      public void run() {
        podcast = PodcastFactory.downloadPodcast(url);
        handler.sendEmptyMessage(0);
      }
    }).start();
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position,
                          long id) {
    ListItem li = (ListItem) parent.getItemAtPosition(position);
    if (!li.isHeader()) {
      playSingleNow(Playable.PlayableFactory.fromPodcastItem(li.item, url,
          title));
    }
  }
}
