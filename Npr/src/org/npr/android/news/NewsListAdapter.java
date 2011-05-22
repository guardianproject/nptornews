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

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.npr.android.util.PlaylistRepository;
import org.npr.api.Client;
import org.npr.api.Story;
import org.npr.api.Story.Audio;
import org.npr.api.Story.StoryFactory;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.List;


public class NewsListAdapter extends ArrayAdapter<Story> {
  private static final String LOG_TAG = NewsListAdapter.class.getName();
  private final LayoutInflater inflater;
  private final ImageThreadLoader imageLoader;
  private RootActivity rootActivity = null;
  private final PlaylistRepository repository;

  public NewsListAdapter(Context context) {
    super(context, R.layout.news_item);
    if (context instanceof RootActivity) {
      rootActivity = (RootActivity) context;
    }
    inflater = LayoutInflater.from(getContext());
    imageLoader = ImageThreadLoader.getOnDiskInstance(context);
    repository = new PlaylistRepository(getContext().getApplicationContext(),
        context.getContentResolver());
  }

  private List<Story> moreStories;
  private boolean endReached = false;

  private final Handler handler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      if (msg.what >= 0) {
        if (moreStories != null) {
          remove(null);
          for (Story s : moreStories) {
            if (getPosition(s) < 0) {
              add(s);
            }
          }
          if (!endReached) {
            add(null);
          }
        }
      } else {
        Toast.makeText(rootActivity,
            rootActivity.getResources()
                .getText(R.string.msg_check_connection),
            Toast.LENGTH_LONG).show();
      }
      if (rootActivity != null) {
        rootActivity.stopIndeterminateProgressIndicator();
      }

    }
  };

  private boolean isPlayable(Story story) {
    for (Audio a : story.getAudios()) {
      if (a.getType().equals("primary")) {
        for (Audio.Format f : a.getFormats()) {
          if (f.getMp3() != null) {
            return true;
          }
        }
      }
    }
    return false;
  }

  @Override
  public View getView(final int position, View convertView, final ViewGroup parent) {

    if (convertView == null) {
      convertView = inflater.inflate(R.layout.news_item, parent, false);
    }

    Story story = getItem(position);

    ImageView icon = (ImageView) convertView.findViewById(R.id.NewsItemIcon);
    TextView teaser = (TextView) convertView.findViewById(R.id.NewsItemTeaserText);
    TextView name = (TextView) convertView.findViewById(R.id.NewsItemNameText);
    final ImageView image = (ImageView) convertView.findViewById(R.id.NewsItemImage);

    if (story != null) {
      if (isPlayable(story)) {
        if (repository.getPlaylistItemFromStoryId(story.getId()) == null) {
          icon.setImageDrawable(
              getContext().getResources().getDrawable(R.drawable.speaker)
          );
        } else {
          icon.setImageDrawable(
              getContext().getResources().getDrawable(R.drawable
                  .news_item_in_playlist)
          );
        }
        icon.setVisibility(View.VISIBLE);
      } else {
        // Because views are re-used we have to set this each time
        icon.setVisibility(View.INVISIBLE);
      }

      name.setText(Html.fromHtml(story.toString()));

      // Need to (re)set this because the views are reused. If we don't then
      // while scrolling, some items will replace the old "Load more stories"
      // view and will be in italics
      name.setTypeface(name.getTypeface(), Typeface.BOLD);

      String teaserText = story.getMiniTeaser();
      if (teaserText == null) {
        teaserText = story.getTeaser();
      }
      if (teaserText != null && teaserText.length() > 0) {
        teaser.setText(Html.fromHtml(teaserText));
        teaser.setVisibility(View.VISIBLE);
      } else {
        teaser.setVisibility(View.GONE);
      }
      if (story.getImages().size() > 0) {
        final String url = story.getImages().get(0).getSrc();
        Drawable cachedImage = null;
        cachedImage = imageLoader.loadImage(
            url,
            new ImageThreadLoader.ImageLoadedListener() {
              public void imageLoaded(Drawable imageBitmap) {
                View itemView = parent.getChildAt(position -
                    ((ListView) parent).getFirstVisiblePosition());
                if (itemView == null) {
                  Log.w(LOG_TAG, "Could not find list item at position " +
                      position);
                  return;
                }
                ImageView img = (ImageView)
                    itemView.findViewById(R.id.NewsItemImage);
                if (img == null) {
                  Log.w(LOG_TAG, "Could not find image for list item at " +
                      "position " + position);
                  return;
                }
                img.setImageDrawable(imageBitmap);
              }
            }
        );

        image.setImageDrawable(cachedImage);

        image.setVisibility(View.VISIBLE);
      } else {
        image.setVisibility(View.GONE);
      }
    } else {
      // null marker means it's the end of the list.
      icon.setVisibility(View.INVISIBLE);
      teaser.setVisibility(View.INVISIBLE);
      image.setVisibility(View.GONE);
      name.setTypeface(name.getTypeface(), Typeface.ITALIC);
      name.setText(R.string.msg_load_more);
    }

    return convertView;
  }

  public void addMoreStories(final String url, final int count) {
    if (rootActivity != null) {
      rootActivity.startIndeterminateProgressIndicator();
    }
    new Thread(new Runnable() {
      @Override
      public void run() {
        if (getMoreStories(url, count)) {
          handler.sendEmptyMessage(0);
        } else {
          handler.sendEmptyMessage(-1);
        }
      }
    }).start();
  }

  private boolean getMoreStories(String url, int count) {

    Node stories = null;
    try {
      stories = new Client(url).execute();
    } catch (IOException e) {
      Log.e(LOG_TAG, "", e);
      return false;
    } catch (SAXException e) {
      Log.e(LOG_TAG, "", e);
    } catch (ParserConfigurationException e) {
      Log.e(LOG_TAG, "", e);
    }

    if (stories == null) {
      Log.d(LOG_TAG, "stories: none");
    } else {
      Log.d(LOG_TAG, "stories: " + stories.getNodeName());

      moreStories = StoryFactory.parseStories(stories);
      if (moreStories != null) {
        if (moreStories.size() < count) {
          endReached = true;
        }
        NewsListActivity.addAllToStoryCache(moreStories);
      }
    }

    return true;
  }

  /**
   * @return a comma-separated list of story ID's
   */
  public String getStoryIdList() {
    StringBuilder ids = new StringBuilder();
    for (int i = 0; i < getCount(); i++) {
      Story story = getItem(i);
      if (story != null) {
        ids.append(story.getId()).append(",");
      }
    }
    String result = ids.toString();
    if (result.endsWith(",")) {
      result = result.substring(0, result.length() - 1);
    }
    return result;
  }
}
