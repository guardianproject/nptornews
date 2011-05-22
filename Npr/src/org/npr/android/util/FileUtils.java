package org.npr.android.util;

import android.content.res.Resources;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;

public class FileUtils {
  public static CharSequence readFile(Resources resources, int id) {
    BufferedReader in = null;
    try {
      in =
          new BufferedReader(
            new InputStreamReader(
                resources.openRawResource(id)
            ),
            8192
          );
      String line;
      StringBuilder buffer = new StringBuilder();
      while ((line = in.readLine()) != null) {
        buffer.append(line).append('\n');
      }
      // Chomp the last newline
      if (buffer.length() > 0) {
        buffer.deleteCharAt(buffer.length() - 1);
      }
      return buffer;
    } catch (IOException e) {
      return "";
    } finally {
      closeStream(in);
    }
  }

  public static CharSequence readFile(Resources resources, String filename) {
    return readFile(resources, resources.getIdentifier(filename, null, null));
  }

  /**
   * Closes the specified stream.
   * 
   * @param stream The stream to close.
   */
  private static void closeStream(Closeable stream) {
    if (stream != null) {
      try {
        stream.close();
      } catch (IOException e) {
        // Ignore
      }
    }
  }
}
