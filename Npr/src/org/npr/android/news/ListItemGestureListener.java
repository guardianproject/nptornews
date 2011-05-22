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

import android.os.Handler;
import android.os.Message;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.ListAdapter;
import android.widget.ListView;

/**
 * A swipe/fling gesture detector for handling the two playlist actions
 * in a news story list.
 * <p/>
 * Fling left (or right) animates the item and, if far enough, adds to playlist
 * <p/>
 * LongPress plays immediately and notifies with visual feedback
 * <p/>
 * Author: Jeremy Wadsack
 */
public class ListItemGestureListener
  extends GestureDetector.SimpleOnGestureListener {
  private static final String LOG_TAG =
    ListItemGestureListener.class.getName();

  private static final int SWIPE_MIN_DISTANCE = 120;
  private static final int SWIPE_MAX_OFF_PATH = 250;
  private static final int SWIPE_THRESHOLD_VELOCITY = 200;

  public static final int MSG_LONG_PRESS = 1;
  public static final int MSG_FLING = 2;


  // An internal flag to reset long press when user is scrolling
  private boolean allowLongPress;


  private final ListView listView;
  private final ListAdapter listAdapter;
  private final Handler handler;
  private float lastLongPressY;
  private float lastLongPressX;


  public ListItemGestureListener(ListView listView, Handler handler) {
    this.listView = listView;
    listAdapter = listView.getAdapter();
    // todo: should check for nulls here; we need both
    // or we can get the adapter the first time we need it in case it was
    // added later
    this.handler = handler;
  }


  @Override
  public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
    if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH) {
      return false;
    }

    // right to left swipe
    if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
      int position = findItemFromEvent(e1);
      if (position == -1) {
        return false;
      }
      if (handler != null) {
        Message msg = new Message();
        msg.what = MSG_FLING;
        msg.arg1 = position;
        msg.arg2 = -1;
        handler.sendMessage(msg);
      }
      MotionEvent e = MotionEvent.obtain(e2);
      e.setAction(MotionEvent.ACTION_CANCEL);
      listView.onTouchEvent(e);
      return true;
    }
    // left to right swipe
    else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
      int position = findItemFromEvent(e1);
      if (position == -1) {
        return false;
      }
      if (handler != null) {
        Message msg = new Message();
        msg.what = MSG_FLING;
        msg.arg1 = position;
        msg.arg2 = 1;
        handler.sendMessage(msg);
      }
      MotionEvent e = MotionEvent.obtain(e2);
      e.setAction(MotionEvent.ACTION_CANCEL);
      listView.onTouchEvent(e);
      return true;
    }
    return false;
  }

  @Override
  public void onLongPress(MotionEvent e) {
      int position = findItemFromEvent(e);
      if (position == -1) {
        super.onLongPress(e);
      }
      if (handler != null) {
        Message msg = new Message();
        msg.what = MSG_LONG_PRESS;
        msg.arg1 = position;
        handler.sendMessage(msg);
      }
  }

  private int findItemFromEvent(MotionEvent e1) {
    return listView.pointToPosition(
      Math.round(e1.getX()),
      Math.round(e1.getY())
    );
  }

}
