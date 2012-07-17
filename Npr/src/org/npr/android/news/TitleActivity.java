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

package org.npr.android.news;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.TextView;

public abstract class TitleActivity extends RootActivity {
  @SuppressWarnings("unused")
  private static final String LOG_TAG = TitleActivity.class.getName();
  public static final String APP_RESUME = "org.npr.android.APP_RESUME";

  /**
   * Implementing classes must override this method to provide a title that
   * will be shown in the title bar.
   * <p/>
   * Note that the title is read in the onCreate so subclasses must be
   * able to generate the title in this method entirely from data available
   * at the time it is called. This could mean that subclasses' onCreate
   * calls super.onCreate after initializing data or it could mean that
   * subclasses inspect the intent directly in the getMainTitle method,
   * rather than depending on variables created in onCreate.
   *
   * @return A string for the title bar.
   */
  protected abstract CharSequence getMainTitle();

  /**
   * This allows implementing classes to set text that appears
   * on the right side of the title bar.
   *
   * @param rightText The text to render on the right of the title bar
   */
  protected void setTitleRight(CharSequence rightText) {
    if (rightText != null && rightText.length() > 0) {
      TextView titleRight = (TextView) findViewById(R.id.TitleRight);
      titleRight.setText(rightText);
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    ViewGroup container = (ViewGroup) findViewById(R.id.TitleContent);
    ViewGroup.inflate(this, R.layout.title, container);

    TextView titleText = (TextView) findViewById(R.id.TitleText);
    titleText.setText(getMainTitle());

  }

  @Override
  protected void onResume() {
    super.onResume();
    sendBroadcast(new Intent(APP_RESUME));
  }
}
