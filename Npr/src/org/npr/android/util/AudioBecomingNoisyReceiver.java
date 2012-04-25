package org.npr.android.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import org.npr.android.news.PlaybackService;

public class AudioBecomingNoisyReceiver extends BroadcastReceiver {

  private static final String LOG_TAG = AudioBecomingNoisyReceiver.class.getName();

  public void onReceive(Context context, Intent intent) {
    Log.d(LOG_TAG, "Audio becoming noisy - pausing.");
    Intent pauseIntent = new Intent(context, PlaybackService.class);
    pauseIntent.setAction(PlaybackService.SERVICE_PAUSE);
    context.startService(pauseIntent);
  }
}
