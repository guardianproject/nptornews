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

import android.util.Log;

import org.apache.http.client.ClientProtocolException;
import org.npr.android.util.NodeUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Station {
  private static final String LOG_TAG = Station.class.getName();
  private final String id;
  private final String name;
  private final String marketCity;
  private final String frequency;
  private final String band;
  private final List<AudioStream> audioStreams;
  private final List<Podcast> podcasts;
  private final String tagline;
  private final String image;
  private final HashMap<String, String> identiferAudioUrlByType;

  private static final Pattern callLettersPattern = Pattern.compile("[KW][A-Z][A-Z][A-Z]");
  public static final String[] stationListBySize = new String[]{"WNYC","KPCC","KUSD","KPCV","KQED","WAMU","WBUR","WBEZ","WBEQ","WBEW","KNOW","KSJN","KCMP","KMSE","KBPN","KCCD","KLNI","KNBJ","KNCM","KNGA","KNSE","KNSR","KNSW","KNTN","KNWF","KZSE","WGGL","WIRN","WLSN","WSCN","KCCM","KCMF","KLSE","KRSD","KRSW","KSJR","WIRR","WMLS","WSCD","WHYY","KCRW","KCRI","KCRU","KUOW","KOAC","KOAB","KOAP","KOBK","KOPB","KRBM","KTMK","KTVR","KMHD","WABE","WEVN","WSUF","WUOM","WFUM","WVGR","KJZZ","KBAQ","WKWM","WLRN","KPLU","KPLI","KVIX","KERA","KUHF","KXOT","WUSF","KPBS","KQVO","WUNC","WBUX","WRQM","WUND","WURI","WAMC","WAMK","WAMQ","WCAN","WOSR","WRUN","KCFR","KCFC","KCFP","KKPC","KPRE","KPRH","KPRN","KPRU","KPYR","KVOV","WGBH","WCAI","WNAN","WNCK","WZAI","WMEH","WMED","WMEF","WMEM","WMEP","WMEW","WSUI","KDWI","KOWI","KSUI","KTPR","KUNZ","KWOI","WOI","KUT","KUTX","KWMU","WVPS","WBTN","WNCH","WOXR","WRVT","WVPA","WVPR","WVTI","WVTQ","WWPV","KXJZ","KXPR","KKTO","KQNC","KUOP","KXJS","KXSR","WSHU","WFAE","WFHE","WLTR","WEPR","WHMC","WJWJ","WLJK","WNSC","WRJA","WSCI","WFCR","WNPR","WEDW","WPKT","WRLI","KQEI","WCPN","WYPR","WYPF","KCUR","WERN","WHND","WHRM","WHSA","WLSU","WPNE","WSSW","WUEC","WVSS","KUWS","WHA","WHBM","WHDI","WHHI","WHID","WHLA","WHWC","WLBL","WRFW","WRST","WMFE","KUNC","KRNC","KDMR","KDUB","KRNI","KUNY","WHRS","WPLN","WTML","WDUQ","WEVC","WEVH","WEVJ","WEVO","WEVS","WYPM","WITF","WHRV","WHRO","WFFC","WISE","WVTF","WVTR","WVTW","WVTU","WWVT","WCVE","WCNV","WMVE","WVXU","WMUB","WKRJ","WKSU","WKRW","WKSV","WNRK","WDET","WFYI","KHPR","KANO","KIPO","KKUA","KWSU","KRFA","KFAE","KLWS","KMWS","KNWO","KNWP","KNWR","KNWV","KNWY","KQWS","KWWS","KZAZ","KSTX","KTXI","KNPR","KLNR","KSGU","KTPH","KWPR","KUER","WRVO","WRCU","WRVD","WRVJ","WRVN","WSUC","WGCU","WMKO","WJCT","WFSU","WFSL","WFSQ","WFSW","WKAR","WJSP","WSVH","WUGA","WABR","WACG","WATY","WGPB","WJWV","WMUM","WNGH","WNGU","WPPR","WUNV","WUWG","WWET","WWIO","WXVS","WVPN","WAUA","WVEP","WVNP","WVPB","WVPG","WVPM","WVPW","WVWV","KUAZ","WPBI","WMPN","WMAB","WMAE","WMAH","WMAO","WMAU","WMAV","WMAW","WUWM","WUOT","WXXI","WRUR","WXXY","WSKG","WSQA","WSQC","WSQE","WSQG","WSQX","WBFO","WOLN","WUBJ","WFDD","WBHM","WFPL","KPBX","KIBX","KSFC","WOSU","WOSB","WOSE","WOSP","WOSV","KLCC","KLBR","KLCO","KLFO","KLFR","KMPQ","WCQS","WFQS","WYQS","WCBE","KNAU","KGHR","KNAA","KNAD","KNAG","KNAQ","KPUB","KUNM","KANU","KANH","KANV","KUWR","KBUW","KDUW","KSUW","KUWA","KUWC","KUWD","KUWG","KUWJ","KUWL","KUWN","KUWP","KUWT","KUWX","KUWY","KUWZ","WOUB","WOUC","WOUH","WOUL","WOUZ","WQCS","WKNO","WKNP","KUAR","KLRE","WCMU","WCMB","WCML","WCMW","WCMZ","WUCX","WWCM","KWGS","KWTU","KBSW","KBSJ","KBSQ","KBSX","KBSY","KBSK","KBSS","KBSU","WVIA","WVYA","KJZA","KXLC","WHAD","WMEA","KUCV","KCNE","KHNE","KLNE","KMNE","KPNE","KRNE","KTNE","KXNE","KVPR","KPRX","WFIU","WUFT","WJUF","KSOR","KAGI","KLDD","KLMF","KNCA","KNHT","KNSQ","KNYR","KSBA","KSKF","KSMF","KSRG","KSRS","KRCC","KECC","WVPE","WUWF","KGOU","KROU","WWNO","KTLN","WNIJ","WNIE","WNIQ","WNIW","WNIU","KUAF","KMUW","WBLQ","WRNI","WRKF","WSLU","WSLJ","WSLL","WSLO","WXLB","WXLG","WXLH","WXLQ","WXLU","KVCR","WLRH","KCLU","KALW","WUTC","KIOS","WMRA","WMRL","WMRY","WILL","WSCL","WSDL","WGTE","WGBE","WGDE","WGLE","KCHO","KFPR","KUNR","KNCC","KDAQ","KBSA","KLDN","KLSA","WPSU","WPSX","WHQR","WUKY","WIAA","WIAB","WICA","WICV","WYSO","WUAL","WAPR","WQPR","WYSU","KEMC","KYPR","KBMC","KPRQ","WBAA","WBNI","WBOI","WCKZ","WEKU","WEKF","WEKH","KSMU","KSMS","KSMW","KBHE","KCSD","KDSD","KESD","KPSD","KQSD","KTSD","KZSD","KCBX","KSBX","KUFM","KAPC","KUFN","KUHM","KUKL","KRWG","WETS","WTEB","WBJD","WKNS","WZNB","WKYU","WDCL","WKPB","WKUE","KBIA","WQLN","KCPW","KHCC","KHCD","KHCT","WAER","WGVU","WGVS","WSIU","WUSI","WVSI","WUIS","WIPA","WTSU","WRWA","WTJB","WMUK","KUSP","KBDH","KHSU","WGLT","WANC","WCEL","WXPN","WCBU","KCND","KDPR","KMPR","KPPR","KPRJ","WBST","WBSB","WBSH","WBSJ","WBSW","WKMS","WNIN","KRPS","KEDT","KVRT","WHIL","WEMU","WIUM","WIUW","KAZU","WNJT","WNJB","WNJM","WNJN","WNJP","WNJS","WNJZ","KCCU","KLCU","KMCU","KOCU","KYCU","KZCU","WVIK","KAMU","WDIY","WXLV","WNED","KAWC","KANZ","KJJP","KTOT","KTXP","KZAN","KZNA","WNMU","KWIT","KOJI","KRVS","WNKU","KOHM","KOSU","KOSN","KXCV","KRNW","KMBH","KHID","WFUV","WETA","WGMS","KEDM","KUSU","KUSR","KWBU","WXPR","WXPW","KASU","WRTI","WJAZ","WRTQ","WRTX","WRTY","KVLU","WKGC","WMKY","WOCS","KENW","KMTH","KSUT","KUTE","WFIT","WBLV","WBLU","KZYX","KZYZ","KAJX","KCJX","KPCW","KCUA","KANW","KNLK","WBGO","KRCU","KSEF","KPRG","KTBG","WQUB","WFSS","WYEP","WUCF","KAXE","WNCW","WQED","WQEJ","WDAV","WWFM","WWNJ","WWCJ","WWPJ","WBJB","KRCB","WCLK","WRVS","WTMD","WMNF","WESM","WMHT","WEXT","WRHV","WEAA","WCNY","WJNY","WUNY","WJAB","KETR","WBPR","WFPB","WNEF","WUMB","KUNV","WHDD","WLPR","WYPO","WPRL","WICN","KBYU","WNCU","WVAS","WURC","KCSM","KWSO","WNYE","WSNC","WMOT","KCIE","KEXP","KGPR","KTEP","KUSC","WCPE","WEMC","WPPB","WVUB","WZPE","KDSC","KMST","KPSC","KQSC","KUAC","WCWP","WJSU","WSIE"};

  public static class Listenable {
    private final String url;
    private final String title;
    
    public Listenable(String url, String title) {
      this.url = url;
      this.title = title;
    }

    public String getUrl() {
      return url;
    }

    public String getTitle() {
      return title;
    }
  }
  
  public static class AudioStream extends Listenable {
    public AudioStream(String url, String title) {
      super(url, title);
    }
  }
  
  public static class Podcast extends Listenable {
    public Podcast(String url, String title) {
      super(url, title);
    }
  }

  private Station(String id, String name, String city, String frequency,
      String band, List<AudioStream> audioStreams, List<Podcast> podcasts,
      String tagline, String image, HashMap<String, String> audioIdentifiersByType) {
    this.id = id;
    this.name = name;
    this.marketCity = city;
    this.frequency = frequency;
    this.band = band;
    this.audioStreams = audioStreams;
    this.podcasts = podcasts;
    this.tagline = tagline;
    this.image = image;
    this.identiferAudioUrlByType = audioIdentifiersByType;
  }

  public String getTagline() {
    return tagline;
  }

  public String getName() {
    return name;
  }

  public String getId() {
    return id;
  }

  public String getMarketCity() {
    return marketCity;
  }

  public String getFrequency() {
    return frequency;
  }

  public String getBand() {
    return band;
  }

  public List<AudioStream> getAudioStreams() {
    return audioStreams;
  }
  
  public List<Podcast> getPodcasts() {
    return podcasts;
  }
  
  public String getImage() {
    return image;
  }

  public HashMap<String, String> getIdentiferAudioUrlByType() {
    return identiferAudioUrlByType;
  }

  public String getCallLettersInName() {
      // try to find call letters from title
      Matcher matcher = callLettersPattern.matcher(getName());
      if (matcher.find())
          return matcher.group();

      return getName();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(name);
    sb.append(" ");
    sb.append(marketCity);
    // TODO: Add market state if the feed has it?
    sb.append(" ");
    sb.append(frequency);
    sb.append(band);
    return sb.toString();
  }

  public static class StationBuilder {
    private String id;
    private String name;
    private String marketCity;
    private String frequency;
    private String band;
    private List<AudioStream> audioStreams = new LinkedList<AudioStream>();
    private List<Podcast> podcasts = new LinkedList<Podcast>();
    private String tagline;
    private String image;
    private HashMap<String, String> audioIdentifiersByType =
        new HashMap<String, String>();

    public StationBuilder(String id) {
      this.id = id;
    }

    public StationBuilder withId(String id) {
      this.id = id;
      return this;
    }

    public StationBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public StationBuilder withMarketCity(String city) {
      this.marketCity = city;
      return this;
    }

    public StationBuilder withFrequency(String frequency) {
      this.frequency = frequency;
      return this;
    }

    public StationBuilder withBand(String band) {
      this.band = band;
      return this;
    }

    public StationBuilder withAudioStreams(List<AudioStream> audioStreams) {
      this.audioStreams = audioStreams;
      return this;
    }
    
    public StationBuilder withPodcasts(List<Podcast> podcasts) {
      this.podcasts = podcasts;
      return this;
    }
    
    public StationBuilder withTagline(String tagline) {
      this.tagline = tagline;
      return this;
    }

    public StationBuilder withImage(String image) {
      this.image = image;
      return this;
    }

    public StationBuilder withAudioIdentifierByType(
        HashMap<String, String> audioIdentifiers) {
      this.audioIdentifiersByType = audioIdentifiers;
      return this;
    }

    public Station build() {
      return new Station(id, name, marketCity, frequency, band, audioStreams,
          podcasts, tagline, image, audioIdentifiersByType);
    }
  }

  public static class StationFactory {
    public static List<Station> parseStations(Node rootNode) {
      if (rootNode.getNodeName().equals("rss")) {
        return parseRssStationList(rootNode);
      }

      return parseNprmlStationList(rootNode);
    }

    private static List<Station> parseNprmlStationList(Node rootNode) {
      List<Station> result = new ArrayList<Station>();
      NodeList stationList = rootNode.getChildNodes();
      for (Node stationNode : new IterableNodeList(stationList)) {
        Station station = createNprmlStation(stationNode);
        if (station != null &&
            (station.getAudioStreams().size() > 0 ||
             station.getPodcasts().size() > 0)) {
          result.add(station);
        }
      }
      return result;
    }

    private static List<Station> parseRssStationList(Node rootNode) {
      List<Station> result = new ArrayList<Station>();

      for (Node channelNode : new IterableNodeList(rootNode.getChildNodes())) {
        if (channelNode.getNodeName().equals("channel")) {
          NodeList stationList = channelNode.getChildNodes();
          for (Node stationNode : new IterableNodeList(stationList)) {
            Station station = createRssStation(stationNode);
            if (station != null &&
                // Only show stations with audio as this is 'find live streams'
                station.getAudioStreams().size() > 0) {
              result.add(station);
            }
          }
        }
      }
      return result;
    }


    private static Station createRssStation(Node node) {
      if (!node.getNodeName().equals("item") ||
          !node.hasChildNodes()) {
        return null;
      }

      StationBuilder sb = new StationBuilder(
          // Create an default ID because some stations don't have one
          Long.toHexString(new Date().getTime() * 1000 + (long)(Math.random()
              * 1000))
      );

      List<AudioStream> streams = new LinkedList<AudioStream>();
      String callLetters = null;

      for (Node n : new IterableNodeList(node.getChildNodes())) {
        String nodeName = n.getNodeName();
        Node nodeChild = n.getChildNodes().item(0);
        if (nodeName.equals("station:nprID")) {
          sb.withId(NodeUtils.getTextContent(n));
        } else if (nodeName.equals("station:callLetters")) {
          callLetters = NodeUtils.getTextContent(n);
        } else if (nodeName.equals("station:verboseName") &&
            callLetters == null) {
          callLetters = NodeUtils.getTextContent(n);
        } else if (nodeName.equals("station:band") && nodeChild != null) {
          sb.withBand(NodeUtils.getTextContent(n));
        } else if (nodeName.equals("station:frequency") && nodeChild != null) {
          sb.withFrequency(NodeUtils.getTextContent(n));
        } else if (nodeName.equals("station:city") && nodeChild != null) {
          sb.withMarketCity(NodeUtils.getTextContent(n));
        } else if (nodeName.equals("station:tagline") && nodeChild != null) {
          sb.withTagline(NodeUtils.getTextContent(n));
        } else if (nodeName.equals("station:stream")) {
          Attr urlAttr = (Attr) n.getAttributes().getNamedItem("url");
          Attr encodingAttr = (Attr) n.getAttributes().getNamedItem("encoding");
          if (encodingAttr != null && urlAttr != null) {
            String url = urlAttr.getValue();
            String encoding = encodingAttr.getValue();
            if (encoding.equals("mp3") && url != null) {
              streams.add(new AudioStream(url, null));
            }
          }
        }
      }

      sb.withAudioStreams(streams);
      sb.withName(callLetters);

      return sb.build();
    }


    private static Station createNprmlStation(Node node) {
      if (!node.getNodeName().equals("station") ||
          !node.hasChildNodes()) {
        return null;
      }
      StationBuilder sb = new StationBuilder(
          node.getAttributes().getNamedItem("id").getNodeValue()
      );
      List<AudioStream> streams = new LinkedList<AudioStream>();
      List<Podcast> podcasts = new LinkedList<Podcast>();
      HashMap<String, String> identifierAudioUrlByType = new HashMap<String, String>();
      for (Node n : new IterableNodeList(node.getChildNodes())) {
        String nodeName = n.getNodeName();
        Node nodeChild = n.getChildNodes().item(0);
        if (nodeName.equals("name")) {
          sb.withName(NodeUtils.getTextContent(n));
        } else if (nodeName.equals("band") && nodeChild != null) {
          sb.withBand(NodeUtils.getTextContent(n));
        } else if (nodeName.equals("frequency") && nodeChild != null) {
          sb.withFrequency(NodeUtils.getTextContent(n));
        } else if (nodeName.equals("marketCity") && nodeChild != null) {
          sb.withMarketCity(NodeUtils.getTextContent(n));
        } else if (nodeName.equals("tagline") && nodeChild != null) {
          sb.withTagline(NodeUtils.getTextContent(n));
        } else if (nodeName.equals("image") && nodeChild != null) {
          sb.withImage(NodeUtils.getTextContent(n));
        } else if (nodeName.equals("url")) {
          Attr typeIdAttr = (Attr) n.getAttributes().getNamedItem("typeId");
          Attr typeAttr = (Attr) n.getAttributes().getNamedItem("type");
          Attr titleAttr = (Attr) n.getAttributes().getNamedItem("title");
          if (typeIdAttr != null && typeAttr != null) {
            String typeId = typeIdAttr.getValue();
            String type = typeAttr.getValue();
            String url = NodeUtils.getTextContent(n);
            String title = titleAttr.getValue();
            if (typeId.equals("10") && type.equals("Audio MP3 Stream")
                && nodeChild != null) {
              streams.add(new AudioStream(url, title));
            }
            if (typeId.equals("9") && type.equals("Podcast")
                && nodeChild != null) {
              podcasts.add(new Podcast(url, title));
            }
          }
        } else if (nodeName.equals("identifierAudioUrl")) {
          Attr typeAttr = (Attr) n.getAttributes().getNamedItem("type");
          if (typeAttr != null) {
            identifierAudioUrlByType.put(typeAttr.getValue(), NodeUtils.getTextContent(n));
          }
        }
      }
      sb.withAudioStreams(streams);
      sb.withPodcasts(podcasts);
      sb.withAudioIdentifierByType(identifierAudioUrlByType);
      return sb.build();
    }

    public static Station downloadStation(String stationId) {
      Log.d(LOG_TAG, "downloading station: " + stationId);
      Map<String, String> params = new HashMap<String, String>();
      params.put(ApiConstants.PARAM_ID, stationId);
      String url =
          ApiConstants.instance().createUrl(ApiConstants.STATIONS_PATH, params);

      Node stations = null;
      try {
        stations = new Client(url).execute();
      } catch (ClientProtocolException e) {
        Log.e(LOG_TAG, "", e);
      } catch (IOException e) {
        Log.e(LOG_TAG, "", e);
      } catch (SAXException e) {
        Log.e(LOG_TAG, "", e);
      } catch (ParserConfigurationException e) {
        Log.e(LOG_TAG, "", e);
      }

      if (stations == null) {
        return null;
      }
      Log.d(LOG_TAG, "node " + stations.getNodeName() + " "
          + stations.getChildNodes().getLength());
      List<Station> result = parseStations(stations);
      return result.size() > 0 ? result.get(0) : null;
    }

  }
}