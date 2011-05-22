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
package org.npr.android.util;

import android.test.AndroidTestCase;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Unit tests for the PLS parser
 * 
 * @author jeremywadsack
 */
public class PlsParserTest extends AndroidTestCase {
  File testPlsFile = null;
  
  public void setUp() {
    testPlsFile = new File(getContext().getCacheDir(), "pls_playlist_test_data");
    FileWriter writer = null;
    try {
      writer = new FileWriter(testPlsFile);
      writer.write("[playlist]");
      writer.write("NumberOfEntries=1");
      writer.write("File1=http://stream.example.com/");
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (writer != null) {
        try {
          writer.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }
  
  public void tearDown() {
    testPlsFile.delete();
  }
  
  
  public void testShouldFindStreamWithinAPlsFile() {
    assertNotNull(testPlsFile);
    try {
      PlsParser parser = new PlsParser(testPlsFile);
      List<String> playlist = parser.getUrls();
      assertNotNull(playlist);
      assertTrue(playlist.size() == 1);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }
  
}
