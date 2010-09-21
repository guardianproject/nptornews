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

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import android.test.AndroidTestCase;
import android.util.Log;

/**
 * A collection of test for the StreamProxy class.
 * 
 * @author jeremywadsack
 */
public class StreamProxyTest extends AndroidTestCase {
  private static final String TAG = StreamProxyTest.class.getName();

  /*
   * This may only occur in testing (see below) but the proxy server throws a
   * NullPointerException when opened and closed and no data is sent to it.
   */
  public void testShouldNotThrowNPEWhenStreamClosedWithoutContent() {
    StreamProxy proxy = new StreamProxy();
    proxy.init();
    proxy.start();
    Socket socket = null;
    UncaughtExceptionHandler oldHandler = Thread
        .getDefaultUncaughtExceptionHandler();
    ThreadExceptionHandler newHandler = new ThreadExceptionHandler();
    Thread.setDefaultUncaughtExceptionHandler(newHandler);
    try {
      socket = new Socket(
          InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 }), 
          proxy.getPort());
    } catch (Exception e) {
      fail(e.getClass().getSimpleName() + ":" + e.getMessage());
    } finally {
      if (socket != null) {
        try {
          socket.close();
        } catch (IOException e) {
          Log.w(TAG, e.getClass().getSimpleName() + ":" + e.getMessage());
        }
      }
      proxy.stop();
      Thread.setDefaultUncaughtExceptionHandler(oldHandler);
      assertFalse(
          "Proxy isn't checking for null content when reading from buffer.",
          newHandler.caughtNPE);

    }
  }

  /*
   * Most android devices are not on a named host. So the default implementation
   * of InetAddres.getLocalHost() returns 127.0.0.1. However, it is possible
   * that some devices may, in the future, be on a named host. In that case the
   * call would return inet interface address, 10.0.2.15.
   */
  public void testShouldRunOnLocalhost() {
    // Connect the proxy and test the address
    StreamProxy proxy = new StreamProxy();
    proxy.init();
    proxy.start();
    Socket socket = null;
    try {
      socket = new Socket(
          InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 }), proxy
              .getPort());
      assertTrue("Socket connected.", socket.isConnected());
    } catch (UnknownHostException e) {
      fail("Proxy is not running on localhost. " + e.getClass().getSimpleName()
          + ":" + e.getMessage());
    } catch (Exception e) {
      fail(e.getClass().getSimpleName() + ":" + e.getMessage());
    } finally {
      if (socket != null) {
        try {
          socket.close();
        } catch (IOException e) {
          Log.w(TAG, e.getClass().getSimpleName() + ":" + e.getMessage());
        }
      }
      proxy.stop();
    }
  }

  public void testShouldNotBeAbleToStopProxyIfNotStarted() {
    StreamProxy proxy = new StreamProxy();

    try {
      proxy.stop();
    } catch (NullPointerException ex) {
      fail("Proxy threw NPE, when attempting to stop without starting.");
    } catch (IllegalStateException ex) {
      // This is the ideal behavior
    }
  }

  public void testShouldNotBeAbleToStartProxyIfNotInitialized() {
    StreamProxy proxy = new StreamProxy();

    boolean started = false;
    try {
      proxy.start();
      assertTrue("Proxy should not report port 0", proxy.getPort() != 0);
    } catch (IllegalStateException ex) {
      // This is the ideal behavior
    } finally {
      if (started) {
        proxy.stop();
      }
    }
  }

  // ------------------
  // Test helpers

  private class ThreadExceptionHandler implements UncaughtExceptionHandler {
    public boolean caughtNPE = false;

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
      if (ex instanceof NullPointerException) {
        caughtNPE = true;
      }
    }
  }

}
