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

package org.npr.api;


import android.content.ContentResolver;
import android.database.Cursor;

import java.util.Hashtable;
import java.util.List;

public class Program extends StoryGrouping {

  private String liveStreamUrl;
  private final String podcastUrl;

  public Program(String id, String title, int storycounttoday,
      int storycountmonth, int storycountall, String additionalInfo) {
    super(id, title, storycounttoday, storycountmonth, storycountall,
        additionalInfo);
    liveStreamUrl = null;
    podcastUrl = null;
  }

  public Program(String id, String title, int storycounttoday,
      int storycountmonth, int storycountall, String additionalInfo,
      String podcastUrl, String liveStreamUrl) {
    super(id, title, storycounttoday, storycountmonth, storycountall,
        additionalInfo);
    this.podcastUrl = podcastUrl;
    this.liveStreamUrl = liveStreamUrl;
  }


  public String getPodcastUrl() {
    return podcastUrl;
  }

  public String getLiveStreamUrl() {
    return liveStreamUrl;
  }

  private void setLiveStreamUrl(String url) {
    liveStreamUrl = url;
  }




  public static class ProgramFactory extends StoryGroupingFactory<Program> {

    private final ContentResolver contentResolver;

    public ProgramFactory(ContentResolver resolver) {
      // Inherit from an instance of an NPRML factory
      super(Program.class, "3004");
      // Get a reference to the content resolver for the Conf file
      this.contentResolver = resolver;
    }

    @Override
    public List<Program> downloadStoryGroupings(int count) {
      // Get NPRML list
      List<Program> list =  super.downloadStoryGroupings(-1);

      // Create a hashtable cache for quick look-up by topic id
      Hashtable<String, Program> lookup = new Hashtable<String, Program>();
      for (Program p : list) {
        lookup.put(p.getId(), p);
      }

      // Add any programs in the Conf file that aren't already listed
      Cursor cursor = contentResolver.query(
          IPhoneNewsAppProgramsConfProvider.CONTENT_URL, null, null, null, null
      );

      if (cursor != null) {
        if (cursor.moveToFirst()) {
          do {
            String id = cursor.getString(
                cursor.getColumnIndex(
                    IPhoneNewsAppProgramsConfProvider.Items.TOPIC_ID
                )
            );
            // Check it it's already in the list
            if (id != null && lookup.containsKey(id)) {
              lookup.get(id).setLiveStreamUrl(cursor.getString(
                  cursor.getColumnIndex(
                      IPhoneNewsAppProgramsConfProvider.Items.LIVE_STREAM_URL
                  )
              ));
            } else {
              // If not, add it to the list
              ProgramBuilder builder = new ProgramBuilder(id);
              String title = cursor.getString(
                  cursor.getColumnIndex(
                      IPhoneNewsAppProgramsConfProvider.Items.NAME
                  )
              );
              builder.withTitle(title);

              builder.withPodcastUrl(cursor.getString(
                  cursor.getColumnIndex(
                      IPhoneNewsAppProgramsConfProvider.Items.PODCAST_URL
                  )
              ));
              builder.withLiveStreamUrl(cursor.getString(
                  cursor.getColumnIndex(
                      IPhoneNewsAppProgramsConfProvider.Items.LIVE_STREAM_URL
                  )
              ));

              list.add(builder.build());
            }
          } while (cursor.moveToNext());
        }
        cursor.close();
      }

      return (count >= 0 && count < list.size()) ?
          list.subList(0, count) :
          list;
    }
  }


  public static class ProgramBuilder extends StoryGroupingBuilder<Program> {

    private String liveStreamUrl;
    private String podcastUrl;

    public ProgramBuilder(String id) {
      super(Program.class, id, 0, 0, 0);
    }


    public void withPodcastUrl(String url) {
      this.podcastUrl = url;
    }

    public void withLiveStreamUrl(String url) {
      this.liveStreamUrl = url;
    }

    @Override
    public Program build() {
      return new Program(id, title, storycounttoday, storycountmonth,
          storycountall, additionalInfo, podcastUrl, liveStreamUrl);
    }
  }

}
