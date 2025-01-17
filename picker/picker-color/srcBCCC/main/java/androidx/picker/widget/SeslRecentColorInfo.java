/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.picker.widget;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */
public class SeslRecentColorInfo {
    private Integer mSelectedColor = null;
    private Integer mCurrentColor = null;
    private Integer mNewColor = null;
    private final ArrayList<Integer> mRecentColorInfo = new ArrayList<>();

    ArrayList<Integer> getRecentColorInfo() {
        return mRecentColorInfo;
    }

    @Nullable
    public Integer getCurrentColor() {
        return mCurrentColor;
    }
    public void setCurrentColor(@Nullable Integer currentColor) {
        mCurrentColor = currentColor;
    }

    @Nullable
    public Integer getNewColor() {
        return mNewColor;
    }

    public void setNewColor(@Nullable Integer newColor) {
        mNewColor = newColor;
    }

    @Nullable
    public Integer getSelectedColor() {
        return mSelectedColor;
    }

    public void saveSelectedColor(int selectedColor) {
        mSelectedColor = selectedColor;
    }


    public void initRecentColorInfo(@NonNull int[] colorIntegerArray) {
        if (colorIntegerArray.length <= SeslColorPicker.RECENT_COLOR_SLOT_COUNT) {
            for (int selectedColor : colorIntegerArray) {
                mRecentColorInfo.add(selectedColor);
            }
        } else {
            for (int i = 0; i < SeslColorPicker.RECENT_COLOR_SLOT_COUNT; i++) {
                mRecentColorInfo.add(colorIntegerArray[i]);
            }
        }
    }
}
