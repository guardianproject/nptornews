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

import android.util.Log;

import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.message.BasicStatusLine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

// TODO: This is a test framework piece and therefore needs a unit-test.

/**
 * An abstract HTTP server used for testing.
 * 
 * Implementing classes must define the <code>getData</code> method.
 */
public abstract class HttpServer implements Runnable {
  private static final String TAG = HttpServer.class.getName();
  private int port = 0;
  private boolean isRunning = true;
  private ServerSocket socket;
  private Thread thread;
  private boolean simulateStream = false;


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

  /**
   * Determines if the server is running (i.e. has been 
   * <code>start</code>ed and has not been <code>stop</code>ed.
   * 
   * @return <code>true</code> if the server is running, otherwise <code>false</code>
   */
  public boolean isRunning() {
    return isRunning;
  }
  
  /**
   * Sets a value that determines whether the server will simulate an
   * open-ended stream by looping the content of the DataSource. This
   * is false, by default.
   * 
   * @param simulateStreaming <code>true</code> to loop content, else <code>false</code>
   */
  protected void setSimulateStream(boolean simulateStreaming) {
    simulateStream = simulateStreaming;
  }
  
  /**
   * Determines if the server is configured to loop content, simulating an
   * open-ended stream. This is false, by default.
   * @return <code>true</code> to loop content, else <code>false</code>
   */
  public boolean isSimulatingStream() {
    return simulateStream;
  }

  
  // TODO: This could be hidden inside a private class.
  /**
   * This is used internally by the server and should not be called directly.
   */
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

        DataSource data = getData(readRequest(client));
        processRequest(data, client);
      } catch (SocketTimeoutException e) {
        // Do nothing
      } catch (IOException e) {
        Log.e(TAG, "Error connecting to client", e);
      }
    }
    Log.d(TAG, "Server interrupted or stopped. Shutting down.");
  }

  /**
   * Returns a DataSource object for a given request. 
   * 
   * This method must be implemented by subclasses.
   * 
   * @param request  The path of the resource requested. e.g. /index.html
   * @return A DataSource that provides meta-data and a stream to the resource.
   */
  protected abstract DataSource getData(String request);

  /*
   * Get the HTTP request line from the client and 
   * parse out the path of the request.
   * 
   * @return a URL-decoded string of the request.
   */
  private String readRequest(Socket client) {

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
      return null;
    }

    StringTokenizer st = new StringTokenizer(firstLine);
    st.nextToken(); // Skip method
    return URLDecoder.decode(st.nextToken());
  }

  /*
   * Sends the HTTP response to the client, including
   * headers (as applicable) and content.
   */
  private void processRequest(DataSource dataSource, Socket client)
      throws IllegalStateException, IOException {
    if (dataSource == null) {
      Log.e(TAG, "Inavlid (null) resource.");
      client.close();
      return;
    }

    Log.d(TAG, "setting response headers");
    StringBuilder httpString = new StringBuilder();
    httpString.append(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1),
        HttpStatus.SC_OK, "OK"));
    httpString.append("\n");

    httpString.append("Content-Type: " + dataSource.getContentType());
    httpString.append("\n");
    
    // Some content (e.g. streams) does not define a length
    long length = dataSource.getContentLength();
    if( length >= 0 ) {
      httpString.append("Content-Length: " + length);
      httpString.append("\n");
    }

    httpString.append("\n");
    Log.d(TAG, "headers done");

    InputStream data = null;
    try {
      data = dataSource.createInputStream();
      byte[] buffer = httpString.toString().getBytes();
      int readBytes = -1;
      Log.d(TAG, "writing to client");
      client.getOutputStream().write(buffer, 0, buffer.length);

      // Start sending content.
      byte[] buff = new byte[1024 * 50];
      while (isRunning ) {
        readBytes = data.read(buff, 0, buff.length);
        if (readBytes == -1) {
          if (simulateStream) {
            data.close();
            data = dataSource.createInputStream();
            readBytes = data.read(buff, 0, buff.length);
            if (readBytes == -1) {
              throw new IOException("Error re-opening data source for looping.");
            }
          } else {
            break;
          }
        }
        client.getOutputStream().write(buff, 0, readBytes);
      }
    } catch (SocketException e) {
      // Ignore when the client breaks connection
      Log.w(TAG, "Ignoring " + e.getMessage());
    } catch (IOException e) {
      Log.e(TAG, "Error getting content stream.", e);
    } catch (Exception e) {
      Log.e(TAG, "Error streaming file content.", e);
    } finally {
      if (data != null) {
        data.close();
      }
      client.close();
    }
  }
  
  /**
   *  An abstract class that provides meta-data and access to a stream 
   *  for resources. 
   */
  protected abstract class DataSource {

    /**
     * Returns a MIME-compatible content type (e.g. "text/html") for the resource.
     * This method must be implemented.
     * @return A MIME content type.
     */
    public abstract String getContentType();

    /**
     * Creates and opens an input stream that returns the contents
     * of the resource.
     * This method must be implemented.
     * @return An <code>InputStream</code> to access the resource.
     * @throws IOException If the implementing class produces an error when opening the stream.
     */
    public abstract InputStream createInputStream() throws IOException;

    /**
     * Returns the length of resource in bytes. 
     * 
     * By default this returns -1, which causes no content-type
     * header to be sent to the client. This would make sense for 
     * a stream content of unknown or undefined length. If your 
     * resource has a defined length you should override this 
     * method and return that.
     * 
     * @return The length of the resource in bytes.
     */
    public long getContentLength() {
      return -1;
    }

  }



}
