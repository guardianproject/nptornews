// Copyright 2009 Google Inc.
// Copyright 2011 NPR
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.npr.api;

import android.content.Context;
import android.util.Log;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;

import info.guardianproject.onionkit.trust.StrongHttpsClient;

import java.io.IOException;
import java.io.InputStream;

/**
 * Author: Jeremy Wadsack
 */
public class HttpHelper {

  private static final String LOG_TAG = HttpHelper.class.getName();

  public static String proxyHost = "localhost";
  public static int proxyPort = 8118;
  /**
   * A helper function to grab content from a URL.
   *
   * @param url URL of the item to download
   *
   * @return an input stream to the content. The caller is responsible for
   * closing the stream. Content will be null in the case of errors.
   * @throws java.io.IOException if an error occurs loading the url
   */
  public static InputStream download(String url, Context context) throws IOException {
    InputStream data = null;
    Log.d(LOG_TAG, "Starting download: " + url);
    HttpClient http = new StrongHttpsClient(context);
    
    if (proxyHost != null)
    	http.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY,  new HttpHost(proxyHost, proxyPort));

    HttpGet method = new HttpGet(url);

    try {
      HttpResponse response = http.execute(method);
      data = response.getEntity().getContent();
    } catch (IllegalStateException e) {
      Log.e(LOG_TAG, "error downloading", e);
    }
    Log.d(LOG_TAG, "Download complete");
    return data;
  }
}
