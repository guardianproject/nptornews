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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.message.BasicStatusLine;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

public class HttpRawResourceServer implements Runnable {
  private static final String TAG = HttpRawResourceServer.class.getName();
  private int port = 0;
  private boolean isRunning = true;
  private ServerSocket socket;
  private Thread thread;
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
   */
  public HttpRawResourceServer(Context context) {
    if (context == null) {
      throw new IllegalArgumentException(
          "Context for accessing resources cannot be null");
    }
    resourceContext = context;
  }

  /**
   * Returns the port that the server is running on. The host is localhost
   * (127.0.0.1).
   * 
   * @return A port number assigned by the OS.
   */
  public int getPort() {
    return port;
  }

  /**
   * Prepare the server to start.
   * 
   * This only needs to be called once per instance. Once initialized, the
   * server can be started and stopped as needed.
   */
  public void init() {
    try {
      socket = new ServerSocket(port, 0, InetAddress.getByAddress(new byte[] {
          127, 0, 0, 1 }));
      socket.setSoTimeout(5000);
      port = socket.getLocalPort();
      Log.d(TAG, "Server stated at " + socket.getInetAddress().getHostAddress()
          + ":" + port);
    } catch (UnknownHostException e) {
      Log.e(TAG, "Error initializing server", e);
    } catch (IOException e) {
      Log.e(TAG, "Error initializing server", e);
    }
  }

  /**
   * Start the server.
   */
  public void start() {
    thread = new Thread(this);
    thread.start();
  }

  /**
   * Stop the server.
   * 
   * This stops the thread listening to the port. It may take up to five seconds
   * to close the service and this call blocks until that occurs.
   */
  public void stop() {
    isRunning = false;
    if (thread == null) {
      Log.w(TAG, "Server was stopped without being started.");
      return;
    }
    Log.d(TAG, "Stopping server.");
    thread.interrupt();
    try {
      thread.join(5000);
    } catch (InterruptedException e) {
      Log.w(TAG, "Server was interrupted while stopping", e);
    }
  }

  @Override
  public void run() {
    Log.d(TAG, "running");
    while (isRunning) {
      try {
        Socket client = socket.accept();
        if (client == null) {
          continue;
        }
        Log.d(TAG, "client connected");

        processRequest(readRequest(client), client);
      } catch (SocketTimeoutException e) {
        // Do nothing
      } catch (IOException e) {
        Log.e(TAG, "Error connecting to client", e);
      }
    }
    Log.d(TAG, "Proxy interrupted. Shutting down.");
  }

  private int readRequest(Socket client) {

    InputStream is;
    String firstLine;
    try {
      is = client.getInputStream();
      // We really don't need 8k (default) buffer (it throws a warning)
      // 2k is big enough: http://www.boutell.com/newfaq/misc/urllength.html
      BufferedReader reader = new BufferedReader(new InputStreamReader(is),
          2048);
      firstLine = reader.readLine();
    } catch (IOException e) {
      Log.e(TAG, "Error parsing request from client", e);
      return -1;
    }

    StringTokenizer st = new StringTokenizer(firstLine);
    st.nextToken(); // Skip method
    String uri = st.nextToken();
    String path = URLDecoder.decode(uri.substring(1));
    // TODO: remove any extensions
    Log.d(TAG, "GET request: " + uri + " -> " + path);
    String resPath = String.format("%3$s:%2$s/%1$s", path, "raw",
        resourceContext.getPackageName());
    Log.d(TAG, "Resource: " + resPath);
    return resourceContext.getResources().getIdentifier(resPath, null, null);
  }

  private void processRequest(int id, Socket client)
      throws IllegalStateException, IOException {
    if (id <= 0) {
      Log.e(TAG, "Inavlid resource ID: " + id);
      client.close();
      return;
    }

    Log.d(TAG, "getting file");
    AssetFileDescriptor fd = resourceContext.getResources().openRawResourceFd(
        id);

    Log.d(TAG, "setting response headers");
    StringBuilder httpString = new StringBuilder();
    httpString.append(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1),
        HttpStatus.SC_OK, "OK"));
    httpString.append("\n");

    httpString.append("Content-Type: audio/MP3");
    httpString.append("\n");
    httpString.append("Content-Length: " + fd.getLength());
    httpString.append("\n");

    httpString.append("\n");
    Log.d(TAG, "headers done");

    InputStream data = fd.createInputStream();
    try {
      byte[] buffer = httpString.toString().getBytes();
      int readBytes = -1;
      Log.d(TAG, "writing to client");
      client.getOutputStream().write(buffer, 0, buffer.length);

      // Start sending content.
      byte[] buff = new byte[1024 * 50];
      while (isRunning && (readBytes = data.read(buff, 0, buff.length)) != -1) {
        client.getOutputStream().write(buff, 0, readBytes);
      }
    } catch (Exception e) {
      Log.e(TAG, "Error streaming file content", e);
    } finally {
      if (data != null) {
        data.close();
      }
      client.close();
    }
  }

}
