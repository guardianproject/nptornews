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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Log;

import org.npr.android.util.Base64;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.Thread.State;
import java.lang.ref.SoftReference;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/**
 * This is a tool to load remote images on a background thread.
 *
 * @author Jeremy Wadsack
 */
public class ImageThreadLoader {
  private static final String LOG_TAG = ImageThreadLoader.class.getName();

  // Global cache of images.
  private final Cache cache;

  private final class QueueItem {
    public String url;
    public ImageLoadedListener listener;
  }

  private final ArrayList<QueueItem> queue;

  // Assumes that this is started from the main (UI) thread
  private final Handler handler = new Handler();
  private Thread thread;
  private final QueueRunner runner = new QueueRunner();


  private ImageThreadLoader(Cache cache) {
    thread = new Thread(runner);
    this.cache = cache;
    queue = new ArrayList<QueueItem>();
  }


  /**
   * Creates a new instance of the ImageThreadLoader that uses on-device
   * memory to store the images and garbage collects them rapidly.
   * @return an ImageThreadLoader that uses memory for caching
   */
  public static ImageThreadLoader getInMemoryInstance() {
    return new ImageThreadLoader(new MemoryCache());
  }

  /**
   * Creates a new instance of the ImageThreadLoader that uses the disk
   * storage to cache the images.
   *
   * @param context An application context for accessing storage.
   * @return an ImageThreadLoader that uses on-disk cache
   */
  public static ImageThreadLoader getOnDiskInstance(Context context) {
    return new ImageThreadLoader(new DiskCache(context));
  }


  /**
   * Defines an interface for a callback that will handle
   * responses from the thread loader when an image is done
   * being loaded.
   */
  public interface ImageLoadedListener {
    public void imageLoaded(Drawable imageBitmap);
  }


  /**
   * Provides a Runnable class to handle loading
   * the image from the URL and settings the
   * ImageView on the UI thread.
   */
  private class QueueRunner implements Runnable {
    @Override
    public void run() {
      synchronized (this) {
        while (queue.size() > 0) {
          final QueueItem item = queue.remove(0);

          // If in the cache, return that copy and be done
          if (cache.containsKey(item.url) && cache.get(item.url) != null) {
            getCachedItem(item);
          } else {
            getRemoteItem(item);
          }

        }
      }
    }

    private void getRemoteItem(final QueueItem item) {
      final Bitmap bmp = DownloadDrawable.createBitmapFromUrl(item.url);
      if (bmp != null) {
        cache.put(item.url, bmp);

        // Use a handler to get back onto the UI thread for the update
        handler.post(new Runnable() {
          @Override
          public void run() {
            if (item.listener != null) {
              item.listener.imageLoaded(new BitmapDrawable(bmp));
            }
          }
        });
      } else {
        Log.e(LOG_TAG, "Image from <" + item.url + "> was null!");
      }
    }

    private void getCachedItem(final QueueItem item) {
      // Use a handler to get back onto the UI thread for the update
      handler.post(new Runnable() {
        public void run() {
          if (item.listener != null) {
            // NB: There's a potential race condition here where the
            // cache item could get garbage collected between when we
            // post the runnable and it's executed. Ideally we would
            // re-run the network load or something.
            Bitmap ref = cache.get(item.url);
            if (ref != null) {
              item.listener.imageLoaded(new BitmapDrawable(ref));
            } else {
              Log.w(LOG_TAG, "Image loader lost the image to GC.");
            }
          }
        }
      });
    }
  }


  /**
   * Queues up a URI to load an image from for a given image view.
   *
   * @param uri      The URI source of the image
   * @param listener The listener class to call when the image is loaded
   * @return A Bitmap image if the image is in the cache, else null.
   */
  public Drawable loadImage(final String uri, final ImageLoadedListener
      listener) {
    // If it's in the cache, just get it and quit it
    if (cache.containsKey(uri)) {
      Bitmap ref = cache.get(uri);
      if (ref != null) {
        return new BitmapDrawable(ref);
      }
    }

    QueueItem item = new QueueItem();
    item.url = uri;
    item.listener = listener;
    queue.add(item);

    // start the thread if needed
    if (thread.getState() == State.NEW) {
      thread.start();
    } else if (thread.getState() == State.TERMINATED) {
      thread = new Thread(runner);
      thread.start();
    }
    return null;
  }


  /**
   * A cache is a service that stores references to images
   */
  protected static interface Cache {

    Bitmap get(String uri);

    boolean containsKey(String uri);

    void put(String uri, Bitmap image);
  }

  /**
   * An on-disk cache for storing images by URL
   */
  protected static class DiskCache implements Cache {
    private final Context context;

    /**
     * Creates a new DiskCache that stores files in local storage
     *
     * @param context An application context.
     */
    public DiskCache(Context context) {
      this.context = context;
      cleanCache();
    }


    @Override
    public Bitmap get(String uri) {
      if (uri == null) {
        return null;
      }
      Bitmap value = null;
      try {
        FileInputStream stream =
          new FileInputStream(
              new File(getCachePath(context), makeCacheFileName(uri)))
            ;
        value = BitmapFactory.decodeStream(stream);
        stream.close();
        Log.d(LOG_TAG, "Cache hit: " + uri);
      } catch (FileNotFoundException e) {
        Log.e(LOG_TAG, "Error getting cache file.", e);
      } catch (IOException e) {
        Log.e(LOG_TAG, "Error closing cache file.", e);
      }
      return value;
    }

    @Override
    public boolean containsKey(String uri) {
      if (uri == null) {
        return false;
      }
      File file = new File(getCachePath(context), makeCacheFileName(uri));
      return file.exists();
    }

    @Override
    public void put(String uri, Bitmap image) {
      if (uri == null) {
        return;
      }
      try {
        Bitmap.CompressFormat compression = Bitmap.CompressFormat.JPEG;
        if (uri.toLowerCase().endsWith("png")) {
          compression = Bitmap.CompressFormat.PNG;
        }
        FileOutputStream stream =
            new FileOutputStream(
                new File(getCachePath(context), makeCacheFileName(uri))
            );
        image.compress(compression, 50, stream);
        stream.flush();
        stream.close();
      } catch (FileNotFoundException e) {
        Log.e(LOG_TAG, "Error writing cache file. Is the path wrong?", e);
      } catch (IOException e) {
        Log.e(LOG_TAG, "Error closing cache file.", e);
      }
    }

    /**
     * Method to create the file name used to save images in the cache.
     *
     * @param uri The URI of the original image.
     *
     * @return A hash tag that's the filename used in the cache. Returns null
     * if the provided uri is null or if an error occurs creating the name.
     */
    public static String makeCacheFileName(String uri) {
      if (uri == null) {
        return null;
      }
      String key = null;
      try {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        digest.update(uri.getBytes("iso-8859-1"), 0, uri.length());
        key = Base64.encodeBytes(digest.digest()).replace('/','-');
      } catch (NoSuchAlgorithmException e) {
        Log.e(LOG_TAG, "Error making image key name", e);
      } catch (UnsupportedEncodingException e) {
        Log.e(LOG_TAG, "Error making image key name", e);
      }
      return key;
    }

    /**
     * Gets the path where cache files are stored.
     *
     * This stores cache files within the applications cache folder which
     * allows users to clear their cache from the Manage Applications app
     *
     * @param context The application context for creating the path.
     *
     * @return The absolute path to the location where cache images are stored.
     */
    public static String getCachePath(Context context) {
      // We could use external storage, but the images average about 6k each
      // and about a dozen images per day of news browsing. Android will
      // clean this folder for us if it needs space and we don't have to deal
      // with checking if the external storage is present, available,
      // readable and writable.
      File path = new File(context.getCacheDir(),
          ImageThreadLoader.class.getName());
      if (!path.exists()) {
        //noinspection ResultOfMethodCallIgnored
        path.mkdirs();
      }
      return path.getAbsolutePath();
    }


    /**
     * At each launch, run a thread that cleans anything older than a week.
     *
     * Note this also cleans up the old cache file location from the first
     * BETA2.0 release.
     */
    private void cleanCache() {
      new Thread(new Runnable(){
        @Override
        public void run() {
          String oldPath = context.getFilesDir().getAbsolutePath();
          removeFiles(oldPath, ".{22}==$", 0);
          removeFiles(getCachePath(context), ".{22}==$", 7);
        }

        private void removeFiles(String path, String filePattern, int daysOld) {
          final long oldFileDate = new Date().getTime() - (daysOld * 86400000);
          File folder = new File(path);
          if (folder.exists()) {
            String[] filenames = folder.list();
            for (String filename : filenames) {
              if (filename.matches(filePattern)) {
                File file = new File(path, filename);
                if (file.lastModified() < oldFileDate) {
                  Log.d(LOG_TAG, "Removing from cache: " + filename);
                  //noinspection ResultOfMethodCallIgnored
                  file.delete();
                }
              }
            }
          }
        }
      }).start();
    }

  }

  /**
   * An in-memory cache that uses SoftReference to encourage garbage
   * collection.
   */
  protected static class MemoryCache implements Cache {
    // Using SoftReference to allow garbage collector to clean cache if needed
    private final HashMap<String, SoftReference<Bitmap>> cache;

    public MemoryCache() {
      cache = new HashMap<String, SoftReference<Bitmap>>();
    }

    @Override
    public Bitmap get(String uri) {
      return cache.get(uri).get();
    }

    @Override
    public boolean containsKey(String uri) {
      return cache.containsKey(uri);
    }

    @Override
    public void put(String uri, Bitmap image) {
      cache.put(uri, new SoftReference<Bitmap>(image));
    }
  }

}
