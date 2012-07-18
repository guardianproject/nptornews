// Copyright 2009 Google Inc.
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.npr.android.util.DisplayUtils;
import org.npr.android.util.PlaylistEntry;
import org.npr.android.util.PlaylistRepository;
import org.npr.android.util.Tracker;
import org.npr.android.util.Tracker.StoryDetailsMeasurement;
import org.npr.android.widget.WorkspaceView;
import org.npr.api.Book;
import org.npr.api.Story;
import org.npr.api.Story.Byline;
import org.npr.api.Story.TextWithHtml;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class NewsStoryActivity extends RootActivity implements
    WorkspaceView.OnScreenChangeListener {
  private static final String LOG_TAG = NewsStoryActivity.class.getName();

  private WorkspaceView workspace;
  private LayoutInflater inflater;
  private ImageThreadLoader imageLoader;

  // Sample date from api: Tue, 09 Jun 2009 15:20:00 -0400
  public static final SimpleDateFormat apiDateFormat
      = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
  private final DateFormat longDateFormat
      = DateFormat.getDateInstance(DateFormat.LONG);

  private static class TrackerItem {
    String title;
    String topicId;
    String orgId;
    String storyId;
  }

  private TrackerItem trackerItem = null;
  private List<Story> stories;
  private boolean externalStorageAvailable = false;
  private PlaylistRepository playlistRepository;
  private BroadcastReceiver playlistChangedReceiver;
  private BroadcastReceiver playbackChangedReceiver;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    stories = new ArrayList<Story>();
    playlistRepository = new
        PlaylistRepository(getApplicationContext(), getContentResolver());

    final String storyIdsString = getIntent().getStringExtra(Constants.EXTRA_STORY_ID_LIST);
    Log.d(LOG_TAG, "Got the following id's: " + storyIdsString);

    String currentStoryId = "";
    if (getIntent().hasExtra(Constants.EXTRA_STORY_ID)) {
      currentStoryId = getIntent().getStringExtra(Constants.EXTRA_STORY_ID);
    } else if (getIntent().hasExtra(Constants.EXTRA_ACTIVITY_DATA)) {
      currentStoryId = getIntent().getStringExtra(Constants.EXTRA_ACTIVITY_DATA);
    }
    String[] storyIds;
    if (storyIdsString == null) {
      storyIds = new String[]{currentStoryId};
    } else {
      storyIds = storyIdsString.split(",");
    }

    if (storyIds.length == 0) {
      finish();
    }

    String state = Environment.getExternalStorageState();
    externalStorageAvailable = Environment.MEDIA_MOUNTED.equals(state)
        || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);

    workspace = new WorkspaceView(this, null);
    inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    imageLoader = ImageThreadLoader.getOnDiskInstance(this);

    FrameLayout.LayoutParams layout = new FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.FILL_PARENT,
        FrameLayout.LayoutParams.FILL_PARENT
    );
    layout.setMargins(0, 0, 0, DisplayUtils.convertToDIP(this, 95));
    ((ViewGroup) findViewById(R.id.TitleContent)).addView(workspace, layout);
    boolean teaserOnly = getIntent().getBooleanExtra(Constants.EXTRA_TEASER_ONLY, false);

    for (int i = 0; i < storyIds.length; i++) {
      String storyId = storyIds[i];
      Story story = NewsListActivity.getStoryFromCache(storyId);
      stories.add(story);
      layoutStory(story, i, storyIds.length, teaserOnly);
      if (storyId.equals(currentStoryId)) {
        trackerItem = new TrackerItem();
        workspace.setCurrentScreen(i);
        List<Story.Organization> organizations = story.getOrganizations();
        if (organizations != null && organizations.size() > 0) {
          trackerItem.orgId = organizations.get(0).getId();
        }

        for (Story.Parent p : story.getParents()) {
          if (p.isPrimary()) {
            trackerItem.topicId = p.getId();
            break;
          }
        }
        trackerItem.title = story.getTitle();
        trackerItem.storyId = story.getId();
        trackNow();
      }
    }

    playlistChangedReceiver = new PlaylistChangedReceiver();
    this.registerReceiver(playlistChangedReceiver,
        new IntentFilter(PlaylistRepository.PLAYLIST_CHANGED));

    playbackChangedReceiver = new PlaybackChangedReceiver();
    Intent intent = this.registerReceiver(playbackChangedReceiver,
        new IntentFilter(PlaybackService.SERVICE_CHANGE_NAME));
    if (intent != null) {
      playbackChangedReceiver.onReceive(this, intent);
    }

    workspace.setOnScreenChangeListener(this);
  }

  @Override
  protected void onStop() {
    if (playlistChangedReceiver != null) {
      unregisterReceiver(playlistChangedReceiver);
      playlistChangedReceiver = null;
    }
    if (playbackChangedReceiver != null) {
      unregisterReceiver(playbackChangedReceiver);
      playbackChangedReceiver = null;
    }
    super.onStop();
  }

  private void layoutStory(Story story, int position, int total, boolean teaserOnly) {
    if (position >= stories.size()) {
      Log.e(LOG_TAG, "Attempt to get story view for position " + position +
          " beyond loaded stories");
      return;
    }

    if (story == null) {
      Log.e(LOG_TAG, "Story at position " + position + " is null?");
      return;
    }

    View storyView = inflater.inflate(R.layout.news_story, null, false);
    workspace.addView(storyView);

    loadStory(story, position, total, storyView, teaserOnly);

    Button listenNow =
        (Button) storyView.findViewById(R.id.NewsStoryListenNowButton);
    Button enqueue =
        (Button) storyView.findViewById(R.id.NewsStoryListenEnqueueButton);
    Button share = (Button) storyView.findViewById(R.id.NewsStoryShareButton);

    StoryClickListener listener = new StoryClickListener(position);

    listenNow.setOnClickListener(listener);
    enqueue.setOnClickListener(listener);
    share.setOnClickListener(listener);
    boolean isListenable = story.getPlayableUrl() != null;
    listenNow.setVisibility(isListenable ? View.VISIBLE : View.INVISIBLE);
    listenNow.setEnabled(isListenable);
    enqueue.setVisibility(isListenable ? View.VISIBLE : View.INVISIBLE);
    enqueue.setEnabled(isListenable &&
        playlistRepository.getPlaylistItemFromStoryId(story.getId()) == null);
  }

  private void loadStory(Story story, int position, int total, View storyView, boolean teaserOnly) {
    WebView storyWebView = (WebView) storyView.findViewById(R.id.NewsStoryWebView);
    storyWebView.getSettings().setJavaScriptEnabled(true);
    storyWebView.setBackgroundColor(0);
    storyWebView.addJavascriptInterface(new ImageClickInterface(this), "click");

    TextView index = (TextView) storyView.findViewById(R.id.NewsStoryIndex);
    TextView title = (TextView) storyView.findViewById(R.id.NewsStoryTitleText);
    TextView dateline =
        (TextView) storyView.findViewById(R.id.NewsStoryDateline);
    TextView byline =
        (TextView) storyView.findViewById(R.id.NewsStoryByline);

    index.setText(String.format(getString(R.string.msg_story_count_format),
        position + 1, total));
    title.setText(Html.fromHtml(story.getTitle()));

    StringBuilder datelineText = new StringBuilder();
    try {
      datelineText.append(
          longDateFormat.format(
              apiDateFormat.parse(story.getStoryDate())
          )
      );
    } catch (ParseException e) {
      Log.e(LOG_TAG, "date format", e);
    }

    Iterator<Byline> bylines = story.getBylines().iterator();
    if (bylines.hasNext()) {
      StringBuilder bylineText = new StringBuilder("by ");
      while (bylines.hasNext()) {
        Byline b = bylines.next();
        bylineText.append(b.getName());
        if (bylines.hasNext()) {
          bylineText.append(", ");
        }
      }
      byline.setText(bylineText.toString()
          .replaceFirst(", ([^,]+)$", " & $1"));
    } else {
      byline.setVisibility(View.GONE);
    }

    String durationString = story.getDuration();
    if (durationString != null) {
      try {
        int duration = Integer.parseInt(durationString);
        if (duration > 0) {
          if (datelineText.length() > 0) {
            datelineText.append(" | ");
          }
          datelineText.append(
              String.format("%d min %02d sec", duration / 60, duration % 60)
          );
        }
      } catch (NumberFormatException e) {
        Log.w(LOG_TAG, "Invalid duration: " + durationString, e);
      }
    }

    if (datelineText.length() == 0) {
      dateline.setVisibility(View.GONE);
    } else {
      dateline.setText(datelineText.toString());
    }

    TextWithHtml text = story.getTextWithHtml();
    String textHtml;
    if (!teaserOnly && text != null) {
      StringBuilder sb = new StringBuilder();
      if (story.getLayout().getItems().size() > 0) {

        for (Map.Entry<Integer, Story.Layout.LayoutItem> entry : story.getLayout().getItems().entrySet()) {

          if (entry.getValue().getType() == Story.Layout.Type.text) {

            Integer paragraphNum = entry.getKey();
            try {
              paragraphNum = Integer.parseInt(entry.getValue().getItemId());
            } catch (NumberFormatException e) {
              Log.w(LOG_TAG, "Unable to parse paragraph number: " + entry.getValue().getItemId());
            }
            String paragraph = text.getParagraphs().get(paragraphNum);
            // WebView can't load external images, so we need to strip them or it
            // may not render.
            paragraph = paragraph.replaceAll("<img .*/>", "");
            sb.append("<p>").append(paragraph).append("</p>");

          } else if (entry.getValue().getType() == Story.Layout.Type.image &&
              externalStorageAvailable) {

            Story.Image image = story.getImages().get(entry.getValue().getItemId());
            if (image != null) {
              imageLoader.loadImage(image.getSrc(), new ImageLoadListener(position));

              String imageTag = String.format(
                  "<a onClick=\"window.click.clickOnImage('%s', '%s', '%s')\">" +
                      "<div id=\"story-icon\"><img src=\"file://%s/%s\" /></div></a>",
                  image.getSrc(),
                  image.getCaption().replace("'", "\\'").replace("\"", "&quot;"),
                  image.getAttribution().replace("'", "\\'").replace("\"", "&quot;"),
                  ImageThreadLoader.DiskCache.getCachePath(this),
                  ImageThreadLoader.DiskCache.makeCacheFileName(image.getSrc())
              );
              Log.d(LOG_TAG, "Adding tag for image " + imageTag);
              sb.append(imageTag);
            }
          }
        }
      } else {

        // No layout?  Just add paragraphs
        for (Map.Entry<Integer, String> entry : text.getParagraphs().entrySet()) {
          String paragraph = entry.getValue();
          // WebView can't load external images, so we need to strip them or it
          // may not render.
          paragraph = paragraph.replaceAll("<img .*/>", "");
          sb.append("<p>").append(paragraph).append("</p>");
        }
      }

      textHtml = String.format(HTML_FORMAT, sb.toString());

      // Load any book parents
      for (Story.Parent parent : story.getParents()) {
        if (parent.getType().equals("book")) {
          sb = new StringBuilder(textHtml);
          List<Book> books = NewsListActivity.getBooksFromCache(story.getId());
          if (books != null) {
            for (Book book : books) {
              sb.append("<hr/>");
              if (book.getPromoArt() != null) {
                sb.append(
                    String.format(
                        "<div id=\"story-icon\"><img src=\"%s\" /></div>",
                        book.getPromoArt())
                );
              }
              sb.append(String.format("<p><b>%s</b><br/>", book.getTitle()));
              sb.append(String.format("<i>By %s</i></p>", book.getAuthor()));
              sb.append(book.getText());
            }
          }
          textHtml = String.format(HTML_FORMAT, sb.toString());
          // Book loads all books for the story, so break after one is found
          break;
        }
      }

    } else {
      // Only show the teaser if there is no full-text.
      textHtml =
          String.format(HTML_FORMAT, "<p class='teaser'>" + story.getTeaser()
              + "</p>");
    }

    storyWebView.loadDataWithBaseURL(null, textHtml, "text/html", "utf-8", null);
  }
  
  private void playStory(boolean playNow, int position) {
    if (position >= stories.size() || position == -1) {
      Log.e(LOG_TAG, "Attempt to get story audio for position " + position +
          " beyond loaded stories");
      return;
    }

    Story story = stories.get(position);
    if (story == null) {
      Log.e(LOG_TAG, "Story at position " + position + " is null?");
      return;
    }
    String url = story.getPlayableUrl();


    Tracker.LinkEvent e;
    if (playNow) {
      PlaylistEntry activeEntry =
          playlistRepository.getPlaylistItemFromId(getActiveId());
      long playlistId;
      if (activeEntry != null) {
        playlistId = playlistRepository.insert(story, activeEntry.playOrder + 1);
      } else {
        playlistId = playlistRepository.add(story);
      }
      PlaylistEntry entry = playlistRepository.getPlaylistItemFromId(playlistId);
      this.playEntryNow(entry);
      e = new Tracker.PlayEvent(url);
    } else {
      playlistRepository.add(story);
      e = new Tracker.AddToPlaylistEvent(url);
    }

    Tracker.instance(getApplication()).trackLink(e);
  }

  // WebView is default black text.
  // Also add formatting for the image, if there is one.
  private static final String HTML_FORMAT =
      "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01//EN\">" +
          "<html><head><title></title>" +
          "<style type=\"text/css\">" +
          "body {color:#000; margin:0; font-size:10pt;}" +
          "a {color:blue}" +
          ".teaser {font-size: 10pt}" +
          "#story-icon {width: 100px; float:left; " +
          "margin-right: 6pt; margin-bottom: 3pt;}" +
          "#story-icon img {vertical-align: middle; width: 100%%;}" +
          "</style>" +
          "</head>" +
          "<body>" +
          "%s" +
          "</body></html>";


  @Override
  public void trackNow() {
    if (trackerItem != null) {
      StringBuilder pageName = new StringBuilder(trackerItem.storyId).append("-");
      pageName.append(trackerItem.title);
      Tracker.instance(getApplication()).trackPage(
          new StoryDetailsMeasurement(
              pageName.toString(),
              "News",
              trackerItem.orgId,
              trackerItem.topicId,
              trackerItem.storyId
          )
      );
      trackerItem = null;
    }
  }


  @Override
  public void onScreenChanged(int newPosition) {
    Story story = stories.get(newPosition);
    if (story != null) {
      trackerItem = new TrackerItem();

      trackerItem.orgId = story.getOrganizations().size() > 0 ?
          story.getOrganizations().get(0).getId() : null;

      for (Story.Parent p : story.getParents()) {
        if (p.isPrimary()) {
          trackerItem.topicId = p.getId();
          break;
        }
      }
      trackerItem.title = story.getTitle();
      trackerItem.storyId = story.getId();
      trackNow();
    }
  }


  /**
   * Position-aware click listener for each story view so that when the play
   * buttons are clicked we know which story's button got clicked.
   */
  private class StoryClickListener implements View.OnClickListener {
    private final int position;

    public StoryClickListener(int position) {
      this.position = position;
    }


    @Override
    public void onClick(View v) {
      Log.d(LOG_TAG, "Click registered for view " + position);
      switch (v.getId()) {
        case R.id.NewsStoryListenNowButton:
          playStory(true, position);
          break;
        case R.id.NewsStoryListenEnqueueButton:
          playStory(false, position);
          break;
        case R.id.NewsStoryShareButton:
          if (position >= stories.size()) {
            Log.e(LOG_TAG, "Attempt to get story audio for position " + position +
                " beyond loaded stories");
          } else {
            Story story = stories.get(position);
            if (story == null) {
              Log.e(LOG_TAG, "Story at position " + position + " is null?");
            } else {
              String shortLink = story.getShortLink();
              if (shortLink == null) {
                shortLink = "http://npr.org/" + story.getId();
              }
              Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
              shareIntent.putExtra(Intent.EXTRA_SUBJECT, story.getTitle());
              shareIntent.putExtra(Intent.EXTRA_TEXT, shortLink);
              shareIntent.setType("text/plain");
              startActivity(Intent.createChooser(shareIntent,
                  getString(R.string.msg_share_story)));
            }
          }
          break;
      }
    }
  }

  private class PlaylistChangedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      int len = stories.size();
      for (int i = 0; i < len; i++) {
        View v = workspace.getChildAt(i);
        Button enqueue =
            (Button) v.findViewById(R.id.NewsStoryListenEnqueueButton);
        enqueue.setEnabled(
            playlistRepository.getPlaylistItemFromStoryId(stories.get(i).getId()) == null);
      }
    }
  }

  private class PlaybackChangedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      Long playlistId = -1L;
      Playable playable = null;
      try {
        Context serviceContext = context.createPackageContext(context.getPackageName(),
            Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
        Bundle bundle = intent.getExtras();
        bundle.setClassLoader(serviceContext.getClassLoader());
        playable = bundle.getParcelable(Playable.PLAYABLE_TYPE);
      } catch (PackageManager.NameNotFoundException e)
      {
        Log.e(LOG_TAG, "Unable to parse playing item information", e);
      }
      if (playable != null) {
        playlistId = playable.getId();
      }
      if (playlistId != -1) {
        PlaylistEntry pe = playlistRepository.getPlaylistItemFromId
            (playlistId);
        if (pe == null) return;
        int len = stories.size();
        for (int i = 0; i < len; i++) {
          View v = workspace.getChildAt(i);
          Button listenNow =
              (Button) v.findViewById(R.id.NewsStoryListenNowButton);
          listenNow.setEnabled(!stories.get(i).getId().equals(pe.storyID));
        }
      }
    }
  }

  private class ImageLoadListener implements ImageThreadLoader.ImageLoadedListener {

    int position;
    
    public ImageLoadListener(int position) {
      this.position = position;
    }
    
    public void imageLoaded(Drawable imageBitmap) {
      boolean teaserOnly = getIntent().getBooleanExtra(Constants.EXTRA_TEASER_ONLY, false);
      loadStory(
          stories.get(position),
          position,
          stories.size(),
          workspace.getChildAt(position),
          teaserOnly);
    }
  }

  final class ImageClickInterface {

    private Context context;
    
    public ImageClickInterface(Context context) {
      this.context = context;
    }

    @SuppressWarnings("unused")
    public void clickOnImage(final String url, final String caption, final String provider) {
      Log.v("Image click url: ", url + ", " + caption + ", " + provider);

      if (url != null && url.length() > 0) {
        Intent intent = new Intent(context, NewsImageActivity.class);
        intent.putExtra(NewsImageActivity.EXTRA_IMAGE_URL, url);
        intent.putExtra(NewsImageActivity.EXTRA_IMAGE_CAPTION, caption);
        intent.putExtra(NewsImageActivity.EXTRA_IMAGE_PROVIDER, provider);
        startActivityWithoutAnimation(intent);
      }
    }
  }
}
