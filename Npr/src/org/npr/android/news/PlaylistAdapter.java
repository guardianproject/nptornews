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

import android.content.Context;
import android.database.Cursor;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.npr.android.util.PlaylistProvider;

public class PlaylistAdapter extends CursorAdapter {
  @SuppressWarnings("unused")
  private static final String LOG_TAG = PlaylistAdapter.class.getName();
  private String activeId;

  public PlaylistAdapter(Context context, Cursor cursor) {
    super(context, cursor, false);
  }

  public String getActiveId() {
    return activeId;
  }

  public void setActiveId(String activeId) {
    this.activeId = activeId;
  }

  public void close() {
    getCursor().close();
  }

  @Override
  public void bindView(View view, Context context, Cursor cursor) {

    ImageView playlistItemState = (ImageView) view.findViewById(R.id
      .playlist_item_state);
    String id = cursor.getString(cursor.getColumnIndex(PlaylistProvider.Items
      ._ID));

    if (id.equals(activeId)) {
      view.setBackgroundDrawable(context.getResources().getDrawable(R.drawable
          .playlist_entry_background_active));
      playlistItemState.setImageResource(R.drawable.speaker_icon);
    } else {
      view.setBackgroundDrawable(null);
      view.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
      String isRead = cursor.getString(
        cursor.getColumnIndex(PlaylistProvider.Items.IS_READ));
      if (isRead.equals("0")) {
        playlistItemState.setImageResource(R.drawable.track_dot);
      } else {
        playlistItemState.setVisibility(View.INVISIBLE);
      }
    }

    TextView title = (TextView) view.findViewById(R.id.playlist_item_title);
    title.setText(cursor.getString(
      cursor.getColumnIndex(PlaylistProvider.Items.NAME)));

    TextView duration = (TextView) view.findViewById(R.id
      .playlist_item_duration);
    String seconds = cursor.getString(
      cursor.getColumnIndex(PlaylistProvider.Items.DURATION));
    if (seconds != null) {
      int intSeconds = Integer.parseInt(seconds);
      duration.setText(String.format("%d min %d sec", intSeconds / 60,
        intSeconds % 60));
    } else {
      duration.setText("");
    }
  }

  @Override
  public View newView(Context context, Cursor cursor, ViewGroup parent) {
    View v = LayoutInflater.from(context).inflate(R.layout.playlist_item, parent, false);
    bindView(v, context, cursor);
    Drawable background = v.getBackground();
    v.setBackgroundDrawable(new NoPaddingDrawable(new
        Drawable[]{background}, background));
    return v;
  }

  private class NoPaddingDrawable extends LayerDrawable {
    private final Drawable cover;

    public NoPaddingDrawable(final Drawable[] layers, final Drawable cover) {
      super(layers);
      this.cover = cover;
    }

    @Override
    protected void onBoundsChange(final Rect bounds) {
      super.onBoundsChange(bounds);
      cover.setBounds(bounds);
    }
  }
}
