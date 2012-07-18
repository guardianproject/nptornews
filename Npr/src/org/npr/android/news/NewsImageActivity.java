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

package org.npr.android.news;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class NewsImageActivity extends Activity {

  public static final String EXTRA_IMAGE_URL = "EXTRA_IMAGE_URL";
  public static final String EXTRA_IMAGE_CAPTION = "EXTRA_IMAGE_CAPTION";
  public static final String EXTRA_IMAGE_PROVIDER = "EXTRA_IMAGE_PROVIDER";

  ImageView newsImageView;
  private ProgressBar progressIndicator;

  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN);

    setContentView(R.layout.news_image);
    
    Intent intent = getIntent();
    if (intent.hasExtra(EXTRA_IMAGE_CAPTION)) {
      TextView caption = (TextView) findViewById(R.id.NewsImageCaption);
      caption.setText(intent.getStringExtra(EXTRA_IMAGE_CAPTION));
    }
    
    if (intent.hasExtra(EXTRA_IMAGE_PROVIDER)) {
      TextView provider = (TextView) findViewById(R.id.NewsImageProvider);
      provider.setText(String.format(getString(R.string.msg_image_credit), intent.getStringExtra(EXTRA_IMAGE_PROVIDER)));
    }

    progressIndicator = (ProgressBar) findViewById(R.id.ImageProgressIndicator);
    progressIndicator.setVisibility(View.VISIBLE);

    new Thread(new Runnable() {
      public void run() {
        String url = getIntent().getStringExtra(EXTRA_IMAGE_URL);
        url = url.replaceAll("&s=[0-9]+", "");
        url = url.concat("&s=3");

        final Bitmap bitmap = DownloadDrawable.createBitmapFromUrl(url);

        newsImageView = (ImageView) findViewById(R.id.NewsImageView);
        newsImageView.post(new Runnable() {
          public void run() {
            progressIndicator.setVisibility(View.INVISIBLE);
            newsImageView.setImageBitmap(bitmap);
          }
        });
      }
    }).start();
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent ev) {
    finish();
    return super.dispatchTouchEvent(ev);
  }
}