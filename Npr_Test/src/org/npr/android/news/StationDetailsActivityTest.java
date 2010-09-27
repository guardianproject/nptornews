// Copyright 2010 Google Inc.
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

import android.app.Activity;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;

import org.npr.api.Station;
import org.npr.api.Station.AudioStream;
import org.npr.api.Station.Podcast;
import org.npr.api.Station.StationBuilder;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Unit test for the StationDetailsActivity
 * 
 * @author jeremywadsack
 */
public class StationDetailsActivityTest extends
    ActivityInstrumentationTestCase2<StationDetailsActivity> {

  public StationDetailsActivityTest() {
    super("org.npr.android.news", StationDetailsActivity.class);
  }

  public void testShouldSupportEmptyStationRecordsOnLaunch() {
    Station station = new StationBuilder("0")
        .withAudioStreams(new LinkedList<AudioStream>())
        .withPodcasts(new LinkedList<Podcast>())
        .build();
    ArrayList<Station> list = new ArrayList<Station>();
    list.add(station);
    StationListActivity.addAllToStationCache(list);
    Intent intent = new Intent().putExtra(Constants.EXTRA_STATION_ID, "0");
    setActivityIntent(intent);
    Activity activity = getActivity();
    assertNotNull(activity);
  }

  public void testShouldSupportStationRecordsWithLongLabelsOnLaunch() {
    LinkedList<AudioStream> streams = new LinkedList<AudioStream>();
    streams.add(new AudioStream(LONG_URL, LONG_STRING));
    LinkedList<Podcast> podcasts = new LinkedList<Podcast>();
    podcasts.add(new Podcast(LONG_URL, LONG_STRING));
    Station station = new StationBuilder("0")
        .withBand(LONG_STRING)
        .withFrequency(LONG_STRING)
        .withImage(LONG_URL)
        .withMarketCity(LONG_STRING)
        .withName(LONG_STRING)
        .withTagline(LONG_STRING)
        .withAudioStreams(streams)
        .withPodcasts(podcasts)
        .build();

    ArrayList<Station> list = new ArrayList<Station>();
    list.add(station);
    StationListActivity.addAllToStationCache(list);
    Intent intent = new Intent().putExtra(Constants.EXTRA_STATION_ID, "0");
    setActivityIntent(intent);
    Activity activity = getActivity();
    assertNotNull(activity);
  }

  private static final String SHORT_STRING = "ABCD";
  private static final String INVALID_URL = "ABCD";

  public void testShouldGracefullyIgnoreInvalidUrlsInStationRecordsOnLaunch() {
    LinkedList<AudioStream> streams = new LinkedList<AudioStream>();
    streams.add(new AudioStream(INVALID_URL, SHORT_STRING));
    LinkedList<Podcast> podcasts = new LinkedList<Podcast>();
    podcasts.add(new Podcast(INVALID_URL, SHORT_STRING));
    Station station = new StationBuilder("0")
        .withImage(INVALID_URL)
        .withName(SHORT_STRING)
        .withTagline(SHORT_STRING)
        .withAudioStreams(streams)
        .withPodcasts(podcasts)
        .build();

    ArrayList<Station> list = new ArrayList<Station>();
    list.add(station);
    StationListActivity.addAllToStationCache(list);
    Intent intent = new Intent().putExtra(Constants.EXTRA_STATION_ID, "0");
    setActivityIntent(intent);
    Activity activity = getActivity();
    assertNotNull(activity);
  }

  private static final String LONG_URL = "http://example.com/Lorem/ipsum/dol" +
      "or/sit/amet,/consectetur/adipiscing/elit./Nunc/congue/justo/a/ipsum/p" +
      "retium/ultrices./Vestibulum/a/elit/quam,/at/rutrum/est./Aliquam/erat/" +
      "volutpat./Nunc/consectetur/pulvinar/elit/et/ultrices./Ut/congue/fring" +
      "illa/tempus./Pellentesque/et/orci/id/dui/fermentum/luctus./Etiam/moll" +
      "is/rhoncus/ante,/nec/varius/ligula/dignissim/sit/amet./Sed/quis/neque" +
      "/quis/tellus/dapibus/mollis./Quisque/vel/sagittis/ipsum./Sed/accumsan" +
      "/tristique/ante/sed/mattis./Sed/suscipit,/eros/non/semper/sollicitudi" +
      "n,/felis/sapien/consectetur/purus,/sed/venenatis/diam/dolor/cursus/ne" +
      "que./Cum/sociis/natoque/penatibus/et/magnis/dis/parturient/montes,/na" +
      "scetur/ridiculus/mus.";
  private static final String LONG_STRING = "Lorem ipsum dolor sit amet, con" +
      "sectetur adipiscing elit. Nunc congue justo a ipsum pretium ultrices." +
      " Vestibulum a elit quam, at rutrum est. Aliquam erat volutpat. Nunc c" +
      "onsectetur pulvinar elit et ultrices. Ut congue fringilla tempus. Pel" +
      "lentesque et orci id dui fermentum luctus. Etiam mollis rhoncus ante," +
      " nec varius ligula dignissim sit amet. Sed quis neque quis tellus dap" +
      "ibus mollis. Quisque vel sagittis ipsum. Sed accumsan tristique ante " +
      "sed mattis. Sed suscipit, eros non semper sollicitudin, felis sapien " +
      "consectetur purus, sed venenatis diam dolor cursus neque. Cum sociis " +
      "natoque penatibus et magnis dis parturient montes, nascetur ridiculus" +
      " mus.";

}
