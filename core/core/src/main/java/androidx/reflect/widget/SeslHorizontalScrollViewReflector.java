/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.reflect.widget;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.os.Build;
import android.widget.HorizontalScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.reflect.DeviceInfo;
import androidx.reflect.SeslBaseReflector;

import java.lang.reflect.Method;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */

@RestrictTo(LIBRARY_GROUP_PREFIX)
public class SeslHorizontalScrollViewReflector {
    private static final Class<?> mClass = HorizontalScrollView.class;

    private SeslHorizontalScrollViewReflector() {
    }

    public static void setTouchSlop(@NonNull HorizontalScrollView scrollView, int touchSlop) {
        if (DeviceInfo.isSamsung() &&  Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Method method = SeslBaseReflector.getDeclaredMethod(mClass, "hidden_setTouchSlop", Integer.TYPE);
            if (method != null) {
                SeslBaseReflector.invoke(scrollView, method, touchSlop);
            }
        }
    }
}
