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
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import org.npr.android.util.PlaylistRepository;
import org.npr.android.util.Tracker;
import org.npr.api.Story;

/**
 * Extends NewsListActivity to add a tool bar of buttons to Add all items to
 * playlist or to Find live stream of this program.
 *
 * Author: Jeremy Wadsack
 */
public class ProgramStoryListActivity extends NewsListActivity{
  private static final String LOG_TAG =
    ProgramStoryListActivity.class.getName();

  private String liveStreamRss;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    liveStreamRss =
        getIntent().getStringExtra(Constants.EXTRA_LIVE_STREAM_RSS_URL);

    // Push the action bar into the layout group below the title bar and above
    // the story list
    ViewGroup container = (ViewGroup) findViewById(R.id.TitleGroup);
    container.addView(
      ViewGroup.inflate(
        this,
        R.layout.program_action_buttons,
        null
      ),
      1
    );

    Button addAll = (Button) container.findViewById(R.id.add_all_to_playlist);
    addAll.setOnClickListener(this);
    Button findLiveStream =
        (Button) container.findViewById(R.id.find_live_stream);
    findLiveStream.setOnClickListener(this);
    findLiveStream.setEnabled(getIntent().hasExtra(Constants.EXTRA_ON_AIR));

  }


  @Override
  public void onClick(View v) {
    super.onClick(v);
    switch (v.getId()) {
      case R.id.add_all_to_playlist:
        PlaylistRepository playlistRepository =
            new PlaylistRepository(getApplicationContext(), getContentResolver());
        for (int i = 0; i < listAdapter.getCount(); i++) {
          Story story = listAdapter.getItem(i);
          if (story != null &&
              story.getPlayable() != null) {
            playlistRepository.add(story);
            Tracker.LinkEvent e =
                new Tracker.AddToPlaylistEvent(story.getPlayableUrl());
            Tracker.instance(getApplication()).trackLink(e);
          }
        }
        break;
      case R.id.find_live_stream:
        if (liveStreamRss != null) {
          Intent intent = new Intent(this, StationListActivity.class);
          intent.putExtra(
            Constants.EXTRA_LIVE_STREAM_RSS_URL,
            liveStreamRss
          );
          startActivity(intent);
        }
        break;
    }
  }
}
