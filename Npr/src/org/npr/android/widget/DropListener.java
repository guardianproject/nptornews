// Original from Eric Harlow
// http://ericharlow.blogspot.com/2010/10/experience-android-drag-and-drop-list.html
//
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

/**
 * Implement to handle an item being dropped.
 * An adapter handling the underlying data 
 * will most likely handle this interface.
 *  
 * @author Eric Harlow
 */
public interface DropListener {
	
	/**
	 * Called when an item is to be dropped.
   *
   * The <code>from</code> and <code>to</code> parameters can be thought of
   * as the order that items will be listed.
   *
   * So for example if you have four
   * records in your adapter and they have a field showing the order they
   * should be listed, the from and to tell you how to change the order of
   * records.
   *
   * If the drop command says to change 'from' 1, 'to' 3 then you would
   * arrange the data like so:
   *
   *     +------------+     +------------+
   *     | id | order |     | id | order |
   *     +----+-------+     +----+-------+
   *     |  0 |  0    |     |  0 |  0    |
   *     |  1 |  1    | ==> |  1 |  3    |
   *     |  2 |  2    |     |  2 |  1    |
   *     |  3 |  3    |     |  3 |  2    |
   *     +----+-------+     +----+-------+
   *
	 * @param from - index item started at in the original list.
	 * @param to - index the item should be at in the final list.
	 */
	void onDrop(int from, int to);
}
