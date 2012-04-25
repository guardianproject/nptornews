package org.npr.android.util;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.util.Log;
import org.npr.android.news.PlaybackService;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class AudioManagerProxy {

  private static final String LOG_TAG = AudioManagerProxy.class.getName();
  private static final int AUDIOMANAGER_AUDIOFOCUS_GAIN = 1;
  private static final int AUDIOMANAGER_AUDIOFOCUS_LOSS = -1;
  private static final int AUDIOMANAGER_AUDIO_REQUEST_GRANTED = 1;

  private AudioManager audioManager;
  private Object myOnAudioFocusChangeListener = null;
  private boolean hasFocus = false;

  public AudioManagerProxy(Context context) {
    audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

    Log.v(LOG_TAG, "Looking for audio focus change support");
    // instantiate the OnAudioFocusChangeListener using reflection (as it only exists from Android 2.2 onwards)
    Class<?>[] innerClasses = audioManager.getClass().getDeclaredClasses();
    for (Class<?> classInterface : innerClasses) {
      if (classInterface.getSimpleName().equalsIgnoreCase("OnAudioFocusChangeListener")) {
        Class<?>[] classArray = new Class<?>[1];
        classArray[0] = classInterface;
        myOnAudioFocusChangeListener = Proxy.newProxyInstance(classInterface.getClassLoader(),
            classArray, new ProxyOnAudioFocusChangeListener(context));
        Log.v(LOG_TAG, "Audio focus change support found");
      }
    }
  }

  public boolean getAudioFocus() {
    if (myOnAudioFocusChangeListener != null) {
      Log.v(LOG_TAG, "Getting audio focus");
      try {
        Method[] methods = audioManager.getClass().getDeclaredMethods();
        for (Method method : methods) {
          if (method.getName().equalsIgnoreCase("requestAudioFocus")) {
            Object object = method.invoke(audioManager,
                myOnAudioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AUDIOMANAGER_AUDIOFOCUS_GAIN);
            if (object != null && (Integer) object == AUDIOMANAGER_AUDIO_REQUEST_GRANTED) {
              hasFocus = true;
            }
          }
        }
      } catch (Exception e) {
        Log.e(LOG_TAG, e.getMessage());
      }
      return hasFocus;
    }
    // If audio focus isn't supported by the OS, just return true
    return true;
  }

  public void releaseAudioFocus() {
    if (myOnAudioFocusChangeListener != null && hasFocus) {
      Log.v(LOG_TAG, "Releasing audio focus");
      try {
        Method[] methods = audioManager.getClass().getDeclaredMethods();
        for (Method method : methods) {
          if (method.getName().equalsIgnoreCase("abandonAudioFocus")) {
            method.invoke(audioManager, myOnAudioFocusChangeListener);
            hasFocus = false;
          }
        }
      } catch (Exception e) {
        Log.e(LOG_TAG, e.getMessage());
      }
    }
  }

  private class ProxyOnAudioFocusChangeListener implements InvocationHandler {

    private Context context;

    public ProxyOnAudioFocusChangeListener(Context context) {
      this.context = context;
    }

    public void onAudioFocusChange(int focusChange) {
      Log.v(LOG_TAG, "Audio focus change.  focusChange = " + focusChange);
      if (hasFocus) {
        if (focusChange > 0) {
          Log.v(LOG_TAG, "Audio focus gained");
          Intent intent = new Intent(context, PlaybackService.class);
          intent.setAction(PlaybackService.SERVICE_RESUME_PLAYING);
          context.startService(intent);
        } else {
          Log.v(LOG_TAG, "Audio focus lost");
          Intent intent = new Intent(context, PlaybackService.class);
          intent.setAction(PlaybackService.SERVICE_PAUSE);
          intent.putExtra(PlaybackService.EXTRA_KEEP_AUDIO_FOCUS,
              (focusChange != AUDIOMANAGER_AUDIOFOCUS_LOSS));
          context.startService(intent);
        }
      }
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      Object result = null;
      try {
        if (args != null) {
          if (method.getName().equalsIgnoreCase("onAudioFocusChange") && args[0] instanceof Integer) {
            onAudioFocusChange((Integer) args[0]);
          }
        }
      } catch (Exception e) {
        throw new RuntimeException("unexpected invocation exception: " + e.getMessage());
      }
      return result;
    }
  }
}
