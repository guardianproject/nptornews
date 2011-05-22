// Original from Eric Harlow
// http://ericharlow.blogspot.com/2010/10/experience-android-drag-and-drop-list.html
// Modifications Copyright 2011 NPR
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

package org.npr.android.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ListView;


/**
 * Author: Jeremy Wadsack
 */
public class DragNDropListView extends ListView {
  private static final String LOG_TAG = DragNDropListView.class.getName();


  private boolean mDragMode;

  private int mStartPosition;
  private int mDragPointOffset;    //Used to adjust drag view location

  private ImageView mDragView;
  private ImageView dropPointView;

  private DropListener mDropListener;
  private DragListener mDragListener;

  public DragNDropListView(Context context, AttributeSet attributes) {
    super(context, attributes);
    init(context);
  }

  private void init(Context context) {
    // Create the image to use to mark the insertion point
    dropPointView = new ImageView(context);
    dropPointView.setBackgroundColor(Color.argb(0xFF, 0x33, 0x33, 0x33));
    dropPointView.setVisibility(View.GONE);

  }


  public void setDropListener(DropListener l) {
    mDropListener = l;
  }

  public void setDragListener(DragListener l) {
    mDragListener = l;
  }


  @Override
  public boolean onTouchEvent(MotionEvent ev) {
    final int action = ev.getAction();
    final int x = (int) ev.getX();
    final int y = (int) ev.getY();

    // TODO: This should be a parameter of the class
    // to determine where the touch target is
    if (action == MotionEvent.ACTION_DOWN && x > 3 * this.getWidth() / 4) {
      mDragMode = true;
    }

    if (!mDragMode) {
      return super.onTouchEvent(ev);
    }

    switch (action) {
      case MotionEvent.ACTION_DOWN:
        mStartPosition = pointToPosition(x, y);
        if (mStartPosition != INVALID_POSITION) {
          int mItemPosition = mStartPosition - getFirstVisiblePosition();
          mDragPointOffset = y - getChildAt(mItemPosition).getTop();
          mDragPointOffset -= ((int) ev.getRawY()) - y;
          startDrag(mItemPosition, y);
          drag(x, y);
        }
        break;
      case MotionEvent.ACTION_MOVE:
        drag(x, y);
        break;
      case MotionEvent.ACTION_CANCEL:
        // If the touch event is canceled then some other context consumed
        // the event. We need to stop dragging (clean things up) but don't
        // fire a drop event -- we want to restore the item to where it was.
        mDragMode = false;
        stopDrag(mStartPosition - getFirstVisiblePosition());
        break;
      case MotionEvent.ACTION_UP:
      default:
        mDragMode = false;
        int mEndPosition = calculateDropToPosition(x, y);
        stopDrag(mStartPosition - getFirstVisiblePosition());
        if (mDropListener != null
            && mStartPosition != INVALID_POSITION
            && mEndPosition != INVALID_POSITION) {
          mDropListener.onDrop(mStartPosition, mEndPosition);
        }
        break;
    }
    return true;
  }

  // move the drag view
  private void drag(int x, int y) {
    if (mDragView == null) {
      return;
    }
    WindowManager.LayoutParams layoutParams =
        (WindowManager.LayoutParams) mDragView.getLayoutParams();
    // -- because it's a list; never want it to move left or right
    layoutParams.x = 0;
    layoutParams.y = y - mDragPointOffset;

    WindowManager mWindowManager =
        (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
    mWindowManager.updateViewLayout(mDragView, layoutParams);


    int lineBeforeIndex = calculateDropBeforePosition(x, y);
    if (lineBeforeIndex == INVALID_POSITION) {
      dropPointView.setVisibility(View.GONE);
    } else {
      View v = getChildAt(lineBeforeIndex - getFirstVisiblePosition());
      if (v != null) {
        int[] coordinates = new int[42];
        v.getLocationInWindow(coordinates);
        WindowManager.LayoutParams dropViewParams =
            (WindowManager.LayoutParams)dropPointView.getLayoutParams();
        dropViewParams.x = 0;
        dropViewParams.y = coordinates[1] - (dropPointView.getHeight() / 2);
        dropPointView.setVisibility(View.VISIBLE);
        mWindowManager.updateViewLayout(dropPointView, dropViewParams);
      } else {
        // TODO: capture case where lineBeforeIndex == getLastVisiblePosition
        // + 1 and scroll the list?
        Log.e(LOG_TAG, "Could not find the view for item " + lineBeforeIndex
            + " so not drawing a drop indicator. Should be between " +
            getFirstVisiblePosition() + " and " + getLastVisiblePosition());
        dropPointView.setVisibility(View.GONE);
      }
    }

    if (mDragListener != null) {
      mDragListener.onDrag(x, y, this);
    }
  }


  // enable the drag view for dragging
  private void startDrag(int itemIndex, int y) {
    stopDrag(itemIndex);

    View item = getChildAt(itemIndex);
    if (item == null) {
      return;
    }
    item.setDrawingCacheEnabled(true);
    if (mDragListener != null) {
      mDragListener.onStartDrag(item);
    }

    // Create a copy of the drawing cache so that it does not get recycled
    // by the framework when the list tries to clean up memory
    Bitmap bitmap = Bitmap.createBitmap(item.getDrawingCache());

    WindowManager.LayoutParams windowParams = new WindowManager.LayoutParams();
    windowParams.gravity = Gravity.TOP;
    windowParams.x = 0;
    windowParams.y = y - mDragPointOffset;
    // Make it translucent to we can see the marker below it
    windowParams.alpha = 0xA0;

    windowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
    windowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
    windowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
    windowParams.format = PixelFormat.TRANSLUCENT;
    windowParams.windowAnimations = 0;

    Context context = getContext();
    ImageView v = new ImageView(context);
    v.setImageBitmap(bitmap);

    WindowManager windowManager =
        (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    windowManager.addView(v, windowParams);

    mDragView = v;

    WindowManager.LayoutParams dvParam = new WindowManager.LayoutParams();
    dvParam.copyFrom(windowParams);
    dvParam.width = getMeasuredWidth();
    dvParam.height = 2;
    windowManager.addView(dropPointView, dvParam);
  }

  // destroy drag view
  private void stopDrag(int itemIndex) {
    if (mDragView != null) {
      if (mDragListener != null) {
        mDragListener.onStopDrag(getChildAt(itemIndex));
      }
      mDragView.setVisibility(GONE);
      WindowManager wm =
          (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
      wm.removeView(mDragView);
      mDragView.setImageDrawable(null);
      mDragView = null;

      dropPointView.setVisibility(View.GONE);
      wm.removeView(dropPointView);
    }
  }

  /**
   * While dragging an item we need to know where in the current list of
   * items to draw a divider. This calculates that by making a decision
   * half-way through the item being dragged over.
   *
   * @param x The x position of the MouseEvent
   * @param y The y position of the MouseEvent
   * @return and index for of the item <em>before</em> which the divider
   * should be drawn, or INVALID_POSITION is no divider should be drawn
   */

  private int calculateDropBeforePosition(int x, int y) {
    int endPosition = pointToPosition(x, y);
    if (endPosition == INVALID_POSITION ||
        endPosition == mStartPosition) {
      return INVALID_POSITION;
    }
    View child = getChildAt(endPosition + getFirstVisiblePosition());
    if (child == null) {
      return INVALID_POSITION;
    }
    int offset = child.getHeight() / 2;
    if (mStartPosition > endPosition) {
      endPosition = pointToPosition(x, y + offset);
    } else {
      endPosition = pointToPosition(x, y - offset);
      if (endPosition != INVALID_POSITION) {
        endPosition++;
      }
    }
    return endPosition == mStartPosition ? INVALID_POSITION : endPosition;
  }

  /**
   * When we are dropping an item we need to tell the drop handler what the
   * final rank (order) position the item should end up at. This calculates
   * that by making a decision half-way through the item being dragged over.
   *
   * @param x The x position of the MouseEvent
   * @param y The y position of the MouseEvent
   * @return and index for the to parameter or INVALID_POSITION
   */
  private int calculateDropToPosition(int x, int y) {
    int endPosition = pointToPosition(x, y);
    Log.d(LOG_TAG, "Drop position " + endPosition + " for " + x + ", " + y);
    if (endPosition != INVALID_POSITION) {
      View child = getChildAt(endPosition + getFirstVisiblePosition());
      if (child == null) {
        Log.d(LOG_TAG, "No child at " + endPosition + " for " + x + ", " + y);
        return INVALID_POSITION;
      }
      int offset = child.getHeight() / 2;
      if (mStartPosition > endPosition) {
        endPosition = pointToPosition(x, y + offset);
      } else {
        endPosition = pointToPosition(x, y - offset);
      }
    }
    return endPosition;
  }

}


