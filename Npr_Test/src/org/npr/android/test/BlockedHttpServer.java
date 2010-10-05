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


import java.io.IOException;
import java.io.InputStream;

/**
 * An HttpServer that blocks when requested to read from the stream, holding the
 * HTTP connection open indefinitely.
 * 
 * @author jeremywadsack
 */
public class BlockedHttpServer extends HttpServer {

  /**
   * An InputStream that blocks when called to read.
   * 
   * @author jeremywadsack
   */
  protected class BlockedInputStream extends InputStream {
    private static final int BLOCK_TIME_MS = 500;
    
    public BlockedInputStream() {
      // TODO: stream out content, but one byte every BLOCK_TIME_MS
      // using Context or an AssetFileDescriptor or a Stream, get res/raw/one_second_silence_mp3
      // open a stream to this source
    }
    

    @Override
    public void close() throws IOException {
      super.close();
      // close source stream as well.
    }


    @Override
    public int read() throws IOException {
      synchronized (this) {
        try {
          wait(BLOCK_TIME_MS);
          // write next byte from source stream
        } catch (InterruptedException e) {
        }
      }
      return 0;
    }

  }

  /**
   * A DataSource that has an InputStream that blocks when read from.
   */
  protected class BlockedDataSource extends DataSource {

    @Override
    public InputStream createInputStream() throws IOException {
      return new BlockedInputStream();
    }

    @Override
    public String getContentType() {
      // Let's call it an MP3
      return "audio/mp3";
    }

  }

  @Override
  protected DataSource getData(String request) {
    return new BlockedDataSource();
  }

}
