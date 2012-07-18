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

package org.npr.android.util;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.npr.api.HttpHelper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

public class Program {

  private String nprId;
  private String source;
  private String name;
  private String liveStationsUrl;
  private String platform;
  private int sortOrder;

  public Program(String nprId, String source, String name,
                 String liveStationsUrl, String platform, int sortOrder) {
    this.nprId = nprId;
    this.source = source;
    this.name = name;
    this.liveStationsUrl = liveStationsUrl;
    this.platform = platform;
    this.sortOrder = sortOrder;
  }

  public String getNprId() {
    return nprId;
  }

  public String getSource() {
    return source;
  }

  public String getName() {
    return name;
  }

  public String getLiveStationsUrl() {
    return liveStationsUrl;
  }

  public String getPlatform() {
    return platform;
  }
  
  public int getSortOrder() {
    return sortOrder;
  }

  public static class ProgramBuilder {
    private String nprId;
    private String source;
    private String name;
    private String liveStationsUrl;
    private String platform;
    private int sortOrder;

    public ProgramBuilder withNprId(String nprId) {
      this.nprId = nprId;
      return this;
    }

    public ProgramBuilder withSource(String source) {
      this.source = source;
      return this;
    }

    public ProgramBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public ProgramBuilder withLiveStationUrl(String liveStationsUrl) {
      this.liveStationsUrl = liveStationsUrl;
      return this;
    }

    public ProgramBuilder withPlatform(String platform) {
      this.platform = platform;
      return this;
    }
    
    public ProgramBuilder withSortOrder(int sortOrder) {
      this.sortOrder = sortOrder;
      return this;
    }

    public Program build() {
      return new Program(nprId, source, name, liveStationsUrl, platform, sortOrder);
    }
  }

  public static class ProgramFactory {

    private static final String programURL =
        "http://www.npr.org/services/apps/iphone/news/programs.json";

    public List<Program> downloadPrograms() throws IOException {

      ArrayList<Program> listPrograms = new ArrayList<Program>();
      
      InputStream json = HttpHelper.download(programURL);

      JsonFactory jsonFactory = new JsonFactory();
      JsonParser parser = jsonFactory.createJsonParser(json);
      JsonToken token = parser.nextValue();
      while (token != JsonToken.START_ARRAY && token != JsonToken.END_OBJECT) {
        token = parser.nextValue();
      }
      
      if (token != JsonToken.END_OBJECT) {
        listPrograms = new ArrayList<Program>();
        int sortOrder = 0;

        while (token != JsonToken.END_ARRAY) {
          ProgramBuilder programBuilder = new ProgramBuilder();
          programBuilder.withSortOrder(sortOrder++);
          while (token != JsonToken.END_OBJECT) {
            token = parser.nextToken();
            if (token == JsonToken.FIELD_NAME) {
              String fieldName = parser.getText();
              token = parser.nextToken();

              if (fieldName.equals("nprId")) {
                programBuilder.withNprId(parser.getText());
              } else if (fieldName.equals("src")) {
                programBuilder.withSource(parser.getText());
              } else if (fieldName.equals("name")) {
                programBuilder.withName(parser.getText());
              } else if (fieldName.equals("livestations")) {
                programBuilder.withLiveStationUrl(parser.getText());
              } else if (fieldName.equals("platform")) {
                programBuilder.withPlatform(parser.getText());
              }
            }
          }
          Program program = programBuilder.build();
          if (program.getName() != null) {
            listPrograms.add(program);
          }
          token = parser.nextToken();
        }
      }

      json.close();

      return listPrograms;
    }
  }
}
