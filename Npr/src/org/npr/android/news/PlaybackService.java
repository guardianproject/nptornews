// Copyright 2009 Google Inc.
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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.os.*;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.npr.android.util.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.List;

public class PlaybackService extends Service implements
    OnPreparedListener, OnSeekCompleteListener,
    OnBufferingUpdateListener, OnCompletionListener, OnErrorListener,
    OnInfoListener {

  private static final String LOG_TAG = PlaybackService.class.getName();

  private static final String SERVICE_PREFIX = "org.npr.android.news.";

  public static final String SERVICE_CHANGE_NAME = SERVICE_PREFIX + "CHANGE";
  public static final String SERVICE_CLOSE_NAME = SERVICE_PREFIX + "CLOSE";
  public static final String SERVICE_UPDATE_NAME = SERVICE_PREFIX + "UPDATE";
  public static final String SERVICE_ERROR_NAME = SERVICE_PREFIX + "ERROR";

  public static final String SERVICE_PLAY_SINGLE = SERVICE_PREFIX +
      "PLAY_SINGLE";
  public static final String SERVICE_PLAY_ENTRY = SERVICE_PREFIX + "PLAY_ENTRY";
  public static final String SERVICE_TOGGLE_PLAY = SERVICE_PREFIX +
      "TOGGLE_PLAY";
  public static final String SERVICE_RESUME_PLAYING = SERVICE_PREFIX +
      "RESUME_PLAYING";
  public static final String SERVICE_PAUSE = SERVICE_PREFIX + "PAUSE";
  public static final String SERVICE_BACK_30 = SERVICE_PREFIX + "BACK_30";
  public static final String SERVICE_FORWARD_30 = SERVICE_PREFIX + "FORWARD_30";
  public static final String SERVICE_SEEK_TO = SERVICE_PREFIX + "SEEK_TO";
  public static final String SERVICE_PLAY_NEXT = SERVICE_PREFIX + "PLAYNEXT";
  public static final String SERVICE_PLAY_PREVIOUS = SERVICE_PREFIX +
      "PLAYPREVIOUS";
  public static final String SERVICE_STOP_PLAYBACK = SERVICE_PREFIX + "STOP_PLAYBACK";
  public static final String SERVICE_STATUS = SERVICE_PREFIX + "STATUS";
  public static final String SERVICE_CLEAR_PLAYER = SERVICE_PREFIX +
      "CLEAR_PLAYER";

  public static final String EXTRA_DOWNLOADED = SERVICE_PREFIX + "DOWNLOADED";
  public static final String EXTRA_DURATION = SERVICE_PREFIX + "DURATION";
  public static final String EXTRA_POSITION = SERVICE_PREFIX + "POSITION";
  public static final String EXTRA_SEEK_TO = SERVICE_PREFIX + "SEEK_TO";
  public static final String EXTRA_IS_PLAYING = SERVICE_PREFIX + "IS_PLAYING";
  public static final String EXTRA_IS_PREPARED = SERVICE_PREFIX + "IS_PREPARED";
  public static final String EXTRA_KEEP_AUDIO_FOCUS = SERVICE_PREFIX + "KEEP_AUDIO_FOCUS";

  public static final String EXTRA_ERROR = SERVICE_PREFIX + "ERROR";

  public static enum PLAYBACK_SERVICE_ERROR {Connection, Playback}

  private MediaPlayer mediaPlayer;
  private boolean isPrepared = false;
  private boolean markedRead;
  // Track whether we ever called start() on the media player so we don't try
  // to reset or release it. This causes a hang (ANR) on Droid X
  // http://code.google.com/p/android/issues/detail?id=959
  private boolean mediaPlayerHasStarted = false;

  private StreamProxy proxy;
  private NotificationManager notificationManager;
  private static final int NOTIFICATION_ID = 1;
  private PlaylistRepository playlist;
  private int startId;
  private String currentAction;
  private Playable currentPlayable = null;
  private List<String> playlistUrls;

  // Error handling
  private int errorCount;
  private int connectionErrorWaitTime;
  private int seekToPosition;

  private TelephonyManager telephonyManager;
  private PhoneStateListener listener;
  private boolean isPausedInCall = false;
  private Intent lastChangeBroadcast;
  private Intent lastUpdateBroadcast;
  private int lastBufferPercent = 0;
  private Thread updateProgressThread;

  private AudioManagerProxy audioManagerProxy;

  // Amount of time to rewind playback when resuming after call 
  private final static int RESUME_REWIND_TIME = 3000;
  private final static int ERROR_RETRY_COUNT = 3;
  private final static int RETRY_SLEEP_TIME = 30000;

  private Looper serviceLooper;
  private ServiceHandler serviceHandler;

  private final class ServiceHandler extends Handler {
    public ServiceHandler(Looper looper) {
      super(looper);
    }

    @Override
    public void handleMessage(Message msg) {
      startId = msg.arg1;
      onHandleIntent((Intent) msg.obj);
    }
  }


  @Override
  public void onCreate() {
    super.onCreate();
    mediaPlayer = new MediaPlayer();
    mediaPlayer.setOnBufferingUpdateListener(this);
    mediaPlayer.setOnCompletionListener(this);
    mediaPlayer.setOnErrorListener(this);
    mediaPlayer.setOnInfoListener(this);
    mediaPlayer.setOnPreparedListener(this);
    mediaPlayer.setOnSeekCompleteListener(this);
    notificationManager = (NotificationManager) getSystemService(
        Context.NOTIFICATION_SERVICE);
    playlist = new PlaylistRepository(getApplicationContext(),
        getContentResolver());

    audioManagerProxy = new AudioManagerProxy(getApplicationContext());

    Log.d(LOG_TAG, "Playback service created");

    telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
    // Create a PhoneStateListener to watch for off-hook and idle events
    listener = new PhoneStateListener() {
      @Override
      public void onCallStateChanged(int state, String incomingNumber) {
        switch (state) {
          case TelephonyManager.CALL_STATE_OFFHOOK:
          case TelephonyManager.CALL_STATE_RINGING:
            // Phone going off-hook or ringing, pause the player.
            if (isPlaying()) {
              pause(false);
              isPausedInCall = true;
            }
            break;
          case TelephonyManager.CALL_STATE_IDLE:
            // Phone idle. Rewind a couple of seconds and start playing.
            if (isPausedInCall) {
              isPausedInCall = false;
              seekTo(Math.max(0, getPosition() - RESUME_REWIND_TIME));
              play();
            }
            break;
        }
      }
    };

    // Register the listener with the telephony manager.
    telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);

    HandlerThread thread = new HandlerThread("PlaybackService:WorkerThread");
    thread.start();

    serviceLooper = thread.getLooper();
    serviceHandler = new ServiceHandler(serviceLooper);
  }

  @Override
  public void onStart(Intent intent, int startId) {
    super.onStart(intent, startId);
    Message message = serviceHandler.obtainMessage();
    message.arg1 = startId;
    message.obj = intent;
    serviceHandler.sendMessage(message);
  }

  protected void onHandleIntent(Intent intent) {
    if (intent == null || intent.getAction() == null) {
      Log.d(LOG_TAG, "Null intent received");
      return;
    }
    String action = intent.getAction();
    Log.d(LOG_TAG, "Playback service action received: " + action);
    if (action.equals(SERVICE_PLAY_SINGLE) || action.equals(SERVICE_PLAY_ENTRY)) {
      currentAction = action;
      currentPlayable = intent.getParcelableExtra(Playable.PLAYABLE_TYPE);
      seekToPosition = intent.getIntExtra(EXTRA_SEEK_TO, 0);
      playCurrent(0, 1);
    } else if (action.equals(SERVICE_TOGGLE_PLAY)) {
      if (isPlaying()) {
        pause(false);
        // Get rid of the toggle intent, since we don't want it redelivered
        // on restart
        Intent emptyIntent = new Intent(intent);
        emptyIntent.setAction("");
        startService(emptyIntent);
      } else {
        if (currentPlayable == null) {
          currentAction = action;
          currentPlayable = intent.getParcelableExtra(Playable.PLAYABLE_TYPE);
        }
        if (currentPlayable != null) {
          resumePlaying();
        } else {
          currentAction = SERVICE_PLAY_ENTRY;
          errorCount = 0;
          playFirstUnreadEntry();
        }
      }
    } else if (action.equals(SERVICE_RESUME_PLAYING)) {
      resumePlaying();
    } else if (action.equals(SERVICE_PAUSE)) {
      if (isPlaying()) {
        pause(intent.getBooleanExtra(EXTRA_KEEP_AUDIO_FOCUS, false));
      }
    } else if (action.equals(SERVICE_BACK_30)) {
      seekRelative(-30000);
    } else if (action.equals(SERVICE_FORWARD_30)) {
      seekRelative(30000);
    } else if (action.equals(SERVICE_SEEK_TO)) {
      seekTo(intent.getIntExtra(EXTRA_SEEK_TO, 0));
    } else if (action.equals(SERVICE_PLAY_NEXT)) {
      seekToPosition = 0;
      playNextEntry();
    } else if (action.equals(SERVICE_PLAY_PREVIOUS)) {
      seekToPosition = 0;
      playPreviousEntry();
    } else if (action.equals(SERVICE_STOP_PLAYBACK)) {
      stopSelfResult(startId);
    } else if (action.equals(SERVICE_STATUS)) {
      updateProgress();
    } else if (action.equals(SERVICE_CLEAR_PLAYER)) {
      if (!isPlaying()) {
        stopSelfResult(startId);
      }
    }
  }

  @Override
  public IBinder onBind(Intent intent) {
    Log.w(LOG_TAG, "onBind called, but binding no longer supported.");
    return null;
  }

  private void resumePlaying() {
    if (currentPlayable != null) {
      if (isPrepared) {
        play();
      } else {
        playCurrent(0, 1);
      }
    }
  }

  private boolean playCurrent(int startingErrorCount, int startingWaitTime) {
    errorCount = startingErrorCount;
    connectionErrorWaitTime = startingWaitTime;
    while (errorCount < ERROR_RETRY_COUNT) {
      try {
        prepareThenPlay(currentPlayable.getUrl(), currentPlayable.isStream());
        return true;
      } catch (UnknownHostException e) {
        Log.w(LOG_TAG, "Unknown host in playCurrent");
        handleConnectionError();
      } catch (ConnectException e) {
        Log.w(LOG_TAG, "Connect exception in playCurrent");
        handleConnectionError();
      } catch (IOException e) {
        Log.e(LOG_TAG, "IOException on playlist entry " + currentPlayable.getId(), e);
        incrementErrorCount();
      } catch (IllegalStateException e) {
        Log.e(LOG_TAG, "Illegal state exception trying to play entry " + currentPlayable.getId(), e);
        incrementErrorCount();
      }
    }

    return false;
  }

  private void playNextEntry() {
    do {
      if (currentPlayable != null && currentPlayable.getId() != -1) {
        currentPlayable = playlist.getNextEntry(currentPlayable.getId());
      } else {
        currentPlayable = playlist.getFirstUnreadEntry();
      }
    } while (currentPlayable != null && !playCurrent(0, 1));
  }

  private void playPreviousEntry() {
    do {
      if (currentPlayable != null && currentPlayable.getId() != -1) {
        currentPlayable = playlist.getPreviousEntry(currentPlayable.getId());
      } else {
        currentPlayable = playlist.getFirstUnreadEntry();
      }
    } while (currentPlayable != null && !playCurrent(0, 1));
  }

  private void playFirstUnreadEntry() {
    do {
      currentPlayable = playlist.getFirstUnreadEntry();
    } while (currentPlayable != null && !playCurrent(0, 1));

    if (currentPlayable == null) {
      stopSelfResult(startId);
    }
  }

  private void finishEntryAndPlayNext() {
    if (currentPlayable != null && currentPlayable.getId() >= 0 && !markedRead) {
      playlist.markAsRead(currentPlayable.getId());
    }

    do {
      if (currentPlayable == null) {
        currentPlayable = playlist.getFirstUnreadEntry();
      } else {
        currentPlayable = playlist.getNextEntry(currentPlayable.getId());
      }
    } while (currentPlayable != null && !playCurrent(0, 1));

    if (currentPlayable == null) {
      stopSelfResult(startId);
    }
  }

  synchronized private int getPosition() {
    if (isPrepared) {
      return mediaPlayer.getCurrentPosition();
    }
    return 0;
  }

  synchronized private boolean isPlaying() {
    return isPrepared && mediaPlayer.isPlaying();
  }

  synchronized private void seekRelative(int pos) {
    if (isPrepared) {
      seekToPosition = 0;
      mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() + pos);
    }
  }

  synchronized private void seekTo(int pos) {
    if (isPrepared) {
      seekToPosition = 0;
      mediaPlayer.seekTo(pos);
    }
  }

  private void prepareThenPlay(String url, boolean stream)
      throws IllegalArgumentException, IllegalStateException, IOException {
    Log.d(LOG_TAG, "playNew");
    // First, clean up any existing audio.
    stop();

    if (isPlaylist(url)) {
      downloadPlaylist(url);
      if (playlistUrls.size() > 0) {
        url = playlistUrls.remove(0);
      } else {
        throw new IOException("Empty playlist downloaded");
      }
    }

    Log.d(LOG_TAG, "listening to " + url + " stream=" + stream);
    String playUrl = url;
    // From 2.2 on (SDK ver 8), the local mediaplayer can handle Shoutcast
    // streams natively. Let's detect that, and not proxy.
    int sdkVersion = 0;
    try {
      sdkVersion = Integer.parseInt(Build.VERSION.SDK);
    } catch (NumberFormatException ignored) {
    }

    if (stream && sdkVersion < 8) {
      if (proxy == null) {
        proxy = new StreamProxy();
        proxy.init();
        proxy.start();
      }
      playUrl = String.format("http://127.0.0.1:%d/%s",
          proxy.getPort(), url);
    }

    // We only have to mark an item read on playlist items,
    // so set markedRead to false only when a playlist entry
    markedRead = !currentAction.equals(SERVICE_PLAY_ENTRY);
    synchronized (this) {
      Log.d(LOG_TAG, "reset: " + playUrl);
      mediaPlayer.reset();
      mediaPlayer.setDataSource(playUrl);
      mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
      Log.d(LOG_TAG, "Preparing: " + playUrl);
      mediaPlayer.prepareAsync();
      Log.d(LOG_TAG, "Waiting for prepare");
    }
  }

  synchronized private void play() {
    if (!isPrepared || currentPlayable == null) {
      Log.e(LOG_TAG, "play - not prepared");
      return;
    }
    Log.d(LOG_TAG, "play " + currentPlayable.getId());

    if (!audioManagerProxy.getAudioFocus()) {
      Log.d(LOG_TAG, "Unable to get audio focus, so stop");
      return;
    }

    mediaPlayer.start();
    mediaPlayerHasStarted = true;

    CharSequence contentText = currentPlayable.getTitle();
    Notification notification =
        new Notification(R.drawable.stat_notify_musicplayer,
            contentText,
            System.currentTimeMillis());
    notification.flags = Notification.FLAG_NO_CLEAR
        | Notification.FLAG_ONGOING_EVENT;
    Context context = getApplicationContext();
    CharSequence title = getString(R.string.app_name);

    Class<?> notificationActivity;
    if (currentPlayable.getActivityName() != null) {
      try {
        notificationActivity = Class.forName(currentPlayable.getActivityName());
      } catch (ClassNotFoundException e) {
        notificationActivity = NewsListActivity.class;
      }
    } else {
      notificationActivity = NewsListActivity.class;
    }
    Intent notificationIntent = new Intent(this, notificationActivity);
    if (currentPlayable.getActivityData() != null) {
      notificationIntent.putExtra(Constants.EXTRA_ACTIVITY_DATA,
          currentPlayable.getActivityData());
      notificationIntent.putExtra(Constants.EXTRA_DESCRIPTION,
          R.string.msg_main_subactivity_nowplaying);
    }
    notificationIntent.setAction(Intent.ACTION_VIEW);
    notificationIntent.addCategory(Intent.CATEGORY_DEFAULT);
    notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
        notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
    notification.setLatestEventInfo(context, title, contentText, contentIntent);
    notificationManager.notify(NOTIFICATION_ID, notification);

    // Change broadcasts are sticky, so when a new receiver connects, it will
    // have the data without polling.
    if (lastChangeBroadcast != null) {
      getApplicationContext().removeStickyBroadcast(lastChangeBroadcast);
    }
    lastChangeBroadcast = new Intent(SERVICE_CHANGE_NAME);
    lastChangeBroadcast.putExtra(Playable.PLAYABLE_TYPE, currentPlayable);
    getApplicationContext().sendStickyBroadcast(lastChangeBroadcast);

    if (currentPlayable != null && currentPlayable.getUrl() != null) {
      Tracker.PlayEvent e = new Tracker.PlayEvent(currentPlayable.getUrl());
      Tracker.instance(getApplication()).trackLink(e);
    }
  }

  synchronized private void pause(boolean maintainFocus) {
    Log.d(LOG_TAG, "pause");
    if (isPrepared) {
      if (currentPlayable != null && currentPlayable.isStream()) {
        isPrepared = false;
        if (proxy != null) {
          proxy.stop();
          proxy = null;
        }
        mediaPlayer.stop();
      } else {
        mediaPlayer.pause();
      }
    }
    if (!maintainFocus) {
      audioManagerProxy.releaseAudioFocus();
    }
    notificationManager.cancel(NOTIFICATION_ID);

    if (currentPlayable != null) {
      Tracker.PauseEvent e = new Tracker.PauseEvent(currentPlayable.getUrl());
      Tracker.instance(getApplication()).trackLink(e);
    }
  }

  synchronized private void stop() {
    Log.d(LOG_TAG, "stop");
    audioManagerProxy.releaseAudioFocus();
    if (isPrepared) {
      isPrepared = false;
      if (proxy != null) {
        proxy.stop();
        proxy = null;
      }
      mediaPlayer.stop();
    }
  }

  @Override
  public void onPrepared(MediaPlayer mp) {
    Log.d(LOG_TAG, "Prepared");
    synchronized (this) {
      if (mediaPlayer != null) {
        isPrepared = true;
      }
    }

    if (seekToPosition > 0) {
      Log.d(LOG_TAG, "Seeking to starting position: " + seekToPosition);
      mp.seekTo(seekToPosition);
    } else {
      startPlaying();
    }
  }

  @Override
  public void onSeekComplete(MediaPlayer mp) {
    Log.d(LOG_TAG, "Seek complete");
    if (seekToPosition > 0) {
      seekToPosition = 0;
      startPlaying();
    }
  }

  private void startPlaying() {
    play();
    updateProgressThread = new Thread(new Runnable() {
      public void run() {
        while (true) {
          updateProgress();
          try {
            Thread.sleep(500);
          } catch (InterruptedException e) {
            break;
          }
        }
      }
    });
    updateProgressThread.start();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Log.w(LOG_TAG, "Service exiting");

    stop();

    if (updateProgressThread != null) {
      updateProgressThread.interrupt();
      try {
        updateProgressThread.join(1000);
      } catch (InterruptedException e) {
        Log.e(LOG_TAG, "", e);
      }
    }

    synchronized (this) {
      if (mediaPlayer != null) {
        if (mediaPlayerHasStarted) {
          mediaPlayer.release();
        } else {
          mediaPlayer.setOnBufferingUpdateListener(null);
          mediaPlayer.setOnCompletionListener(null);
          mediaPlayer.setOnErrorListener(null);
          mediaPlayer.setOnInfoListener(null);
          mediaPlayer.setOnPreparedListener(null);
          mediaPlayer.setOnSeekCompleteListener(null);
        }
        mediaPlayer = null;
      }
    }

    serviceLooper.quit();

    notificationManager.cancel(NOTIFICATION_ID);
    if (lastChangeBroadcast != null) {
      getApplicationContext().removeStickyBroadcast(lastChangeBroadcast);
    }
    getApplicationContext().sendBroadcast(new Intent(SERVICE_CLOSE_NAME));

    telephonyManager.listen(listener, PhoneStateListener.LISTEN_NONE);
  }

  @Override
  public void onBufferingUpdate(MediaPlayer mp, int progress) {
    if (isPrepared) {
      lastBufferPercent = progress;
      updateProgress();
    }
  }

  /**
   * Sends an UPDATE broadcast with the latest info.
   */
  private void updateProgress() {

    // Stop updating after mediaplayer is released
    if (mediaPlayer == null)
      return;

    if (isPrepared) {

      if (lastUpdateBroadcast != null) {
        getApplicationContext().removeStickyBroadcast(lastUpdateBroadcast);
        lastUpdateBroadcast = null;
      }

      int duration = mediaPlayer.getDuration();
      seekToPosition = mediaPlayer.getCurrentPosition();
      if (!markedRead) {
        if (seekToPosition > duration / 10) {
          markedRead = true;
          if (playlist != null && currentPlayable != null) {
            playlist.markAsRead(currentPlayable.getId());
          }
        }
      }

      Intent tempUpdateBroadcast = new Intent(SERVICE_UPDATE_NAME);
      tempUpdateBroadcast.putExtra(EXTRA_DURATION, duration);
      tempUpdateBroadcast.putExtra(EXTRA_DOWNLOADED,
          (int) ((lastBufferPercent / 100.0) * duration));
      tempUpdateBroadcast.putExtra(EXTRA_POSITION, seekToPosition);
      tempUpdateBroadcast.putExtra(EXTRA_IS_PLAYING, mediaPlayer.isPlaying());
      tempUpdateBroadcast.putExtra(EXTRA_IS_PREPARED, isPrepared);

      // Update broadcasts while playing are not sticky, due to concurrency
      // issues.  These fire very often, so this shouldn't be a problem.
      getApplicationContext().sendBroadcast(tempUpdateBroadcast);
    } else {
      if (lastUpdateBroadcast == null) {
        lastUpdateBroadcast = new Intent(SERVICE_UPDATE_NAME);
        lastUpdateBroadcast.putExtra(EXTRA_IS_PLAYING, false);
        getApplicationContext().sendStickyBroadcast(lastUpdateBroadcast);
      }
    }
  }

  @Override
  public void onCompletion(MediaPlayer mp) {
    Log.w(LOG_TAG, "onComplete()");

    synchronized (this) {
      if (!isPrepared) {
        // This file was not good and MediaPlayer quit
        Log.w(LOG_TAG,
            "MediaPlayer refused to play current item. Bailing on prepare.");
      }
    }

    seekToPosition = 0;
    if (currentPlayable != null) {
      Tracker.StopEvent e = new Tracker.StopEvent(currentPlayable.getUrl());
      Tracker.instance(getApplication()).trackLink(e);
    }

    // Unfinished playlist
    if (playlistUrls != null && playlistUrls.size() > 0) {
      boolean successfulPlay = false;
      while (!successfulPlay && playlistUrls.size() > 0) {
        String url = playlistUrls.remove(0);
        errorCount = 0;
        while (errorCount < ERROR_RETRY_COUNT) {
          try {
            prepareThenPlay(url, currentPlayable.isStream());
            successfulPlay = true;
            break;
          } catch (UnknownHostException e) {
            Log.w(LOG_TAG, "Unknown host in onCompletion");
            handleConnectionError();
          } catch (ConnectException e) {
            Log.w(LOG_TAG, "Connect exception in onCompletion");
            handleConnectionError();
          } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, "", e);
            incrementErrorCount();
          } catch (IllegalStateException e) {
            Log.e(LOG_TAG, "", e);
            incrementErrorCount();
          } catch (IOException e) {
            Log.e(LOG_TAG, "", e);
            incrementErrorCount();
          }
        }
      }
    }

    if (currentAction.equals(SERVICE_PLAY_ENTRY)) {
      finishEntryAndPlayNext();
    } else {
      stopSelfResult(startId);
    }
  }

  private void incrementErrorCount() {
    errorCount++;
    Log.e(LOG_TAG, "Media player increment error count:" + errorCount);
    if (errorCount >= ERROR_RETRY_COUNT) {
      Intent intent = new Intent(SERVICE_ERROR_NAME);
      intent.putExtra(EXTRA_ERROR, PLAYBACK_SERVICE_ERROR.Playback.ordinal());
      getApplicationContext().sendBroadcast(intent);
    }
  }

  private void handleConnectionError() {
    connectionErrorWaitTime *= 5;
    if (connectionErrorWaitTime > RETRY_SLEEP_TIME) {
      Log.e(LOG_TAG, "Connection failed.  Resetting mediaPlayer" +
          " and trying again in 30 seconds.");

      Intent intent = new Intent(SERVICE_ERROR_NAME);
      intent.putExtra(EXTRA_ERROR, PLAYBACK_SERVICE_ERROR.Connection.ordinal());
      getApplicationContext().sendBroadcast(intent);

      // If a stream, increment since it could be bad
      if (currentPlayable.isStream()) {
        errorCount++;
      }

      connectionErrorWaitTime = RETRY_SLEEP_TIME;
      // Send error notification and keep waiting
      isPrepared = false;
      mediaPlayer.reset();
    } else {
      Log.w(LOG_TAG, "Connection error. Waiting for " +
          connectionErrorWaitTime + " milliseconds.");
    }
    SystemClock.sleep(connectionErrorWaitTime);
  }

  @Override
  public boolean onError(MediaPlayer mp, int what, int extra) {
    Log.w(LOG_TAG, "onError(" + what + ", " + extra + ")");
    synchronized (this) {
      if (!isPrepared) {
        // This file was not good and MediaPlayer quit
        Log.w(LOG_TAG,
            "MediaPlayer refused to play current item. Bailing on prepare.");
      }
    }
    isPrepared = false;
    mediaPlayer.reset();

    incrementErrorCount();
    if (errorCount < ERROR_RETRY_COUNT) {
      playCurrent(errorCount, 1);
      // Returning true means we handled the error, false causes the
      // onCompletion handler to be called
      return true;
    } else {
      return false;
    }
  }

  @Override
  public boolean onInfo(MediaPlayer arg0, int arg1, int arg2) {
    Log.w(LOG_TAG, "onInfo(" + arg1 + ", " + arg2 + ")");
    return false;
  }

  private boolean isPlaylist(String url) {
    return url.contains("m3u") || url.contains("pls");
  }

  private boolean downloadPlaylist(String url) throws IOException {
    Log.d(LOG_TAG, "downloading " + url);
    URLConnection cn = new URL(url).openConnection();
    cn.connect();
    InputStream stream = cn.getInputStream();
    if (stream == null) {
      Log.e(LOG_TAG, "Unable to create InputStream for url: + url");
      return false;
    }

    File downloadingMediaFile = new File(getCacheDir(), "playlist_data");
    FileOutputStream out = new FileOutputStream(downloadingMediaFile);
    byte buf[] = new byte[16384];
    int bytesRead;
    while ((bytesRead = stream.read(buf)) > 0) {
      out.write(buf, 0, bytesRead);
    }

    stream.close();
    out.close();
    PlaylistParser parser;
    if (url.contains("m3u")) {
      parser = new M3uParser(downloadingMediaFile);
    } else if (url.contains("pls")) {
      parser = new PlsParser(downloadingMediaFile);
    } else {
      return false;
    }
    playlistUrls = parser.getUrls();
    return true;
  }

}
