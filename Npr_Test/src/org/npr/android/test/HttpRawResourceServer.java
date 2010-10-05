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

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

// TODO: This is a test framework piece and therefore needs a unit-test.

/**
 * A single-connection HTTP server that will respond to requests 
 * for files and pull them from the application's res/raw folder.
 * 
 * Here's a simple example of how to use it. Assume that there is 
 * a file in the package called res/raw/image.jpg
 * <code>
 *   HttpRawResourceServer server = new HttpRawResourceServer(getContext());
 *   try {
 *     server.init()
 *     server.start();
 *     
 *     String url = "http://127.0.0.1:" + server.getPort() + "/image";
 *     // Send a request to the URL, perhaps with HttpClent or with File.open(url);
 *   } finally {
 *     server.stop();
 *   }
 * </code>
 */
public class HttpRawResourceServer extends HttpServer {
  private static final String TAG = HttpRawResourceServer.class.getName();
  private Context resourceContext;

  /**
   * Creates a new HttpRawResourceServer instance. This server accepts HTTP
   * request and returns files in the res/raw folder of the the application.
   * 
   * When using with Android unit tests, note that AndroidTestCase.getContext
   * returns the context of the <i>application under test</a>, not the test test
   * itself, so this would look in the application's res/raw/ folder.
   * 
   * To get access to the test package's res/raw/ folder files, you need to
   * derive your test from IntrumentationTestCase and then use
   * getInstrumentation().getContext().
   * 
   * @param context
   *          The Android application context. Used to access resources.
   * @param streaming
   *          Whether the server will continuously stream the content.
   */
  public HttpRawResourceServer(Context context, boolean streaming) {
    if (context == null) {
      throw new IllegalArgumentException(
          "Context for accessing resources cannot be null");
    }
    setSimulateStream(streaming);
    resourceContext = context;
  }

  /**
   * Creates a RawResourceDataSource object for the request.
   * @return A <code>DataSource</code> subclass for the raw resource request. 
   */
  @Override
  protected DataSource getData(String request) {
    // Remove initial '/' in path
    String path = request.substring(1);
    // TODO: remove any extensions
    Log.d(TAG, "GET request: " + request + " -> " + path);
    return new RawResourceDataSource(path);
  }
  
  
  
  protected class RawResourceDataSource extends DataSource {
    private AssetFileDescriptor fd;
    private int resourceId;
    
    public RawResourceDataSource(String resourceName) {
      String resPath = String.format("%3$s:%2$s/%1$s", resourceName, "raw",
          resourceContext.getPackageName());
      Log.d(TAG, "Resource: " + resPath);
      resourceId = resourceContext.getResources().getIdentifier(resPath, null, null);
    }

    @Override
    public InputStream createInputStream() throws IOException {
      // NB: Because createInputStream can only be called once per asset 
      // we always create a new file descriptor here.
      getFileDescriptor();
      return fd.createInputStream();
    }

    @Override
    public String getContentType() {
      // TODO: Support other media if we need to
      return "audio/mp3";
    }
    
    @Override
    public long getContentLength() {
      if( isSimulatingStream() ) {
        super.getContentLength();
      }
      if (fd == null) {
        getFileDescriptor();
      }
      return fd.getLength();      
    }
    
    
    private void getFileDescriptor() {
      Log.d(TAG, "getting file");
      fd = resourceContext.getResources().openRawResourceFd(resourceId);
    }
  }

}
