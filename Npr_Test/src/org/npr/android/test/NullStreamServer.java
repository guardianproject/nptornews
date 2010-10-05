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
package org.npr.android.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A testing server that produces a continuous stream of \000.
 * @author jeremywadsack
 */
public class NullStreamServer extends HttpServer {

  /**
   * Creates a new HttpServer that streams a 
   * series of null bytes for any request.
   */
  public NullStreamServer() {
    // Always stream
    setSimulateStream(true);
  }
  
  /**
   * Returns a DataSource for a stream of empty bytes.
   */
  @Override
  protected DataSource getData(String request) {
    // Request doesn't matter
    return new NullStreamSource();
  }
  
  protected class NullStreamSource extends DataSource {
    private final byte[] NULL_BYTES = new byte[1024];
   
    /*
     * Returns an input stream of null bytes
     */
    @Override
    public InputStream createInputStream() throws IOException {
      return new ByteArrayInputStream(NULL_BYTES);
    }

    @Override
    public String getContentType() {
      // We'll call it MP3; the point is that the data is invalid for the type
      return "audio/mp3";
    }
    
  }

}
