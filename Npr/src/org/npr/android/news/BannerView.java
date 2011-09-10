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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.*;
import org.npr.android.util.DisplayUtils;


/**
 * Encapsulates the logic and layout of the sponsorship banner
 */
public class BannerView extends LinearLayout implements View.OnClickListener {
  private static final String LOG_TAG = BannerView.class.getName();
  // Durations in milliseconds
  private static final int BANNER_VISIBLE = 10000;
  private static final int FRAME_DURATION = 50;
  private static final float ANIMATION_DURATION = 500f;
  private static final String LAYOUT =
      "<html>" +
          "<head>" +
          "<meta name=\"viewport\" content=\"target-densitydpi=device-dpi, " +
            "width=device-width\" />" +
          "<style type=\"text/css\">" +
          "body {padding:0;margin:0;text-align:center;background-color:#333;}" +
          "p {display:none;}" +
          "</style>" +
          "</head>" +
          "<body>" +
          "<script type=\"text/javascript\"" +
          "src=\"http://ad.doubleclick.net/adj/n6735.NPR.MOBILE/android_npr;" +
          "sz=320x50;ord=%1$d?\">" +
          "</script>" +
          "</body>" +
      "</html>";

  private ImageView animationView;
  private int startY;
  private PlaylistView dependentView;
  private Context context;
  private WebView webView;

  private enum SponsorshipWindowStates {
    NeedsMeasurement,
    TooNarrow,
    ReadyToBeShown,
    Visible,
    Opening,
    Closing
  }

  private SponsorshipWindowStates sponsorshipWindowState = SponsorshipWindowStates.TooNarrow;

  private static final int MSG_START_CLOSE = 0;
  private static final int MSG_SCROLL_IN = 1;
  private static final int MSG_SCROLL_OUT = 2;

  private Handler handler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      float input;
      switch (msg.what) {
        case MSG_SCROLL_OUT:
          input = (System.currentTimeMillis() - animationStartTime) /
              ANIMATION_DURATION;
          if (input < 1) {
            placeImage(startY +
                (int) (accelerator.getInterpolation(input) * bannerHeight));
            handler.sendEmptyMessageDelayed(MSG_SCROLL_OUT, FRAME_DURATION);
          } else {
            sponsorshipWindowState =
                SponsorshipWindowStates.ReadyToBeShown;
            animationView.setVisibility(GONE);
            WindowManager wm =
                (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            wm.removeView(animationView);
            animationView.setImageDrawable(null);
            animationView = null;
          }
          setVisibility(View.GONE);
          break;
        case MSG_SCROLL_IN:
          sponsorshipWindowState = SponsorshipWindowStates.Visible;
          setVisibility(View.VISIBLE);
          break;
        case MSG_START_CLOSE:
          hideSponsorshipWindow();
          break;
      }
    }
  };

  private void placeImage(int y) {
    WindowManager.LayoutParams layoutParams =
        (WindowManager.LayoutParams) animationView.getLayoutParams();
    layoutParams.x = 0;
    layoutParams.y = y;

    WindowManager mWindowManager =
        (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
    mWindowManager.updateViewLayout(animationView, layoutParams);
  }

  private Interpolator accelerator = new AccelerateInterpolator();
  private long animationStartTime = 0L;
  private int bannerHeight;
  private int screenHeight;

  @SuppressWarnings({"UnusedDeclaration"})
  public BannerView(Context context) {
    super(context);
    this.context = context;
    init();
  }

  @SuppressWarnings({"UnusedDeclaration"})
  public BannerView(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.context = context;
    init();
  }


  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();

    handler.removeMessages(MSG_START_CLOSE);
    handler.removeMessages(MSG_SCROLL_IN);
    handler.removeMessages(MSG_SCROLL_OUT);
  }



  /**
   * Allows assignment of the play list view so that the
   * banner can slide the player drawer down as it scrolls
   * off screen.
   *
   * This enables the drawing cache for the drawer view.
   *
   * @param playlistView The view to animate down.
   */
  public void setPlayerView(PlaylistView playlistView) {
    // NB We need to store the PlaylistView here because
    // the sliding drawer isn't instantiated until after
    // the view is attached to the window.
    dependentView = playlistView;
  }

  void init() {
    DisplayMetrics metrics = new DisplayMetrics();
    ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE))
        .getDefaultDisplay()
        .getMetrics(metrics);
    screenHeight = metrics.heightPixels;

    setDrawingCacheEnabled(true);

    setOrientation(LinearLayout.HORIZONTAL);

    // Some calculated dimensions
    int dim3 = DisplayUtils.convertToDIP(context, 3);
    int dim6 = DisplayUtils.convertToDIP(context, 6);
    int dim33 = DisplayUtils.convertToDIP(context, 33);
    int dim53 = DisplayUtils.convertToDIP(context, 53);
    int dim214 = DisplayUtils.convertToDIP(context, 214);

    TextView left = new TextView(context);
    LayoutParams textLayout = new LayoutParams(
        dim53,
        LayoutParams.FILL_PARENT,
        1
    );
    textLayout.gravity = Gravity.CENTER;
    left.setLayoutParams(textLayout);
    left.setText(context.getString(R.string.banner_support));
    left.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
    left.setTextAppearance(context, R.style.BannerText);
    addView(left);

    webView = new WebView(context);
    LayoutParams webLayout = new LayoutParams(dim214, dim33, 0);
    webLayout.setMargins(dim6, dim6, dim6, dim6);
    webLayout.gravity = Gravity.CENTER;
    webView.setLayoutParams(webLayout);
    webView.setBackgroundColor(0); // Transparent to show resource
    webView.setBackgroundResource(R.drawable.banner_item_background);
    addView(webView);

    ImageButton right = new ImageButton(context);
    LayoutParams closeLayout = new LayoutParams(
        dim53,
        LayoutParams.WRAP_CONTENT,
        1
    );
    // Set padding so it feels more centered
    right.setPadding(0, dim3, dim3, 0);
    closeLayout.gravity = Gravity.CENTER;
    right.setLayoutParams(closeLayout);
    right.setBackgroundDrawable(null);
    right.setImageResource(R.drawable.sponsorship_close);
    right.setScaleType(ImageView.ScaleType.CENTER);
    right.setOnClickListener(this);
    addView(right);


    WebSettings webSettings = webView.getSettings();
    webSettings.setSavePassword(false);
    webSettings.setSaveFormData(false);
    webSettings.setJavaScriptEnabled(true);
    webSettings.setSupportZoom(false);

    sponsorshipWindowState = SponsorshipWindowStates.NeedsMeasurement;
  }

  @Override
  public void onClick(View v) {
    hideSponsorshipWindow();
  }

  /**
   * Starts the ten-second timer for when the sponsorship
   * view will auto-close.
   */
  public void startCloseTimer() {
    if (sponsorshipWindowState == SponsorshipWindowStates.Visible) {
      handler.sendEmptyMessageDelayed(MSG_START_CLOSE, BANNER_VISIBLE);
    }
  }

  private void showSponsorshipWindow() {
    if (sponsorshipWindowState != SponsorshipWindowStates.ReadyToBeShown) {
      Log.w(LOG_TAG, "Window is not ready to be shown: " +
          sponsorshipWindowState);
      return;
    }

    long ord = (long) (Math.random() * 10000000000000000L);
    webView.loadDataWithBaseURL(null, String.format(LAYOUT, ord), "text/html",
        "utf-8", null);

    sponsorshipWindowState = SponsorshipWindowStates.Opening;
    handler.sendEmptyMessage(MSG_SCROLL_IN);
  }

  private void hideSponsorshipWindow() {
    if (sponsorshipWindowState != SponsorshipWindowStates.Visible) {
      return;
    }

    sponsorshipWindowState = SponsorshipWindowStates.Closing;
    AnimationReturn r = getAnimationWindow();
    animationView = r.view;
    bannerHeight = r.bannerHeight;
    startY = screenHeight - r.bitmapHeight;
    animationStartTime = System.currentTimeMillis();
    handler.sendEmptyMessage(MSG_SCROLL_OUT);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    if (sponsorshipWindowState == SponsorshipWindowStates.NeedsMeasurement) {
      if (getMeasuredWidth() < 320) {
        sponsorshipWindowState = SponsorshipWindowStates.TooNarrow;
      } else {
        sponsorshipWindowState = SponsorshipWindowStates.ReadyToBeShown;
        showSponsorshipWindow();
      }
    }
  }

  private static class AnimationReturn {
    public ImageView view;
    public int bannerHeight;
    public int bitmapHeight;
  }

  private AnimationReturn getAnimationWindow() {
    AnimationReturn ret = new AnimationReturn();

    WindowManager.LayoutParams windowParams = new WindowManager.LayoutParams();
    windowParams.gravity = Gravity.TOP;
    windowParams.x = 0;
    windowParams.y = getTop();
    windowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
    windowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
    windowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
    windowParams.windowAnimations = 0;

    ImageView view = new ImageView(context);
    Bitmap bitmap = getViewBitmap(this);
    ret.bannerHeight = bitmap.getHeight();

    if (dependentView != null) {
      SlidingDrawer playerDrawer = dependentView.getPlayerDrawer();
      if (playerDrawer != null && !playerDrawer.isOpened()) {

        // Ugh. This is bad. The BannerView shouldn't have to know this
        // much about the player in order to properly render it. But if
        // I try to render to PlaylistView or the SlidingDrawer it draws
        // the entire frame all the way to the top of the window
        // Also, for some very strange reason, just getting the handle image
        // seems to get everything rendered.
        View handle = playerDrawer.findViewById(R.id.handle);

        Bitmap player = getViewBitmap(handle);

        int width = Math.max(bitmap.getWidth(), player.getWidth());
        int height = bitmap.getHeight() + player.getHeight();

        Bitmap combined = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        );
        Canvas canvas = new Canvas(combined);

        canvas.drawBitmap(player, 0, 0, null);
        canvas.drawBitmap(bitmap, 0, player.getHeight(), null);
        Log.d(LOG_TAG, "Building combined view " + combined.getHeight());
        bitmap = combined;
      }
    }
    // Need to use createBitmap to make a copy or else it gets recycled
    view.setImageBitmap(Bitmap.createBitmap(bitmap));
    ret.bitmapHeight = bitmap.getHeight();

    WindowManager windowManager =
        (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    windowManager.addView(view, windowParams);

    ret.view = view;
    return ret;
  }

  private static Bitmap getViewBitmap(View view) {
    Bitmap bitmap = view.getDrawingCache();
    if (bitmap == null) {
      Log.d(LOG_TAG, "Making drawable bitmap for " + view);
      bitmap = Bitmap.createBitmap(
          view.getMeasuredWidth(),
          view.getMeasuredHeight(),
          Bitmap.Config.ARGB_8888);
      Canvas canvas = new Canvas(bitmap);
      view.draw(canvas);
    }
    return bitmap;
  }

}
