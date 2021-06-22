/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.car.app.hardware.common;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.google.auto.value.AutoValue;

/**
 * Container class for information about getting property values.
 *
 * <P>submits this request to {@link PropertyManager} for getting property values.
 *
 * @hide
 */
@AutoValue
@RestrictTo(LIBRARY)
public abstract class GetPropertyRequest {
    /**
     * @param propertyId    one of the values in {@link android.car.VehiclePropertyIds}
     */
    @NonNull
    public static GetPropertyRequest create(int propertyId) {
        return new AutoValue_GetPropertyRequest(propertyId);
    }

    /** Returns one of the values in {@link android.car.VehiclePropertyIds}. */
    public abstract int getPropertyId();

}
