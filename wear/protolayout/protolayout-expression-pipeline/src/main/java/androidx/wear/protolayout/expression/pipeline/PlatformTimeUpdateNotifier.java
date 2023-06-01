/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.protolayout.expression.pipeline;

import androidx.annotation.NonNull;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.function.Supplier;

/**
 * Interface used to notify all time based dynamic types that they should be updated with the new
 * platform time (system time).
 *
 * <p>It's up to the implementations to chose at what frequency updates should be sent.
 */
public interface PlatformTimeUpdateNotifier {
    /**
     * Sets the callback to be called whenever platform time needs to be reevaluated.
     *
     * <p>Calling this method while there is already a receiver set, should replace the previous
     * receiver.
     *
     * @param tick The callback to run whenever platform time needs to be reevaluated. The returned
     *             {@link ListenableFuture} will complete once all of the
     *             {@link DynamicTypeValueReceiver} callbacks triggered by this reevaluation have
     *             been called. This callback should be invoked by the implementation of
     *             this interface whenever platform time needs to be reevaluated.
     *             Because reevaluations triggered by a call to {@code tick} callback might execute
     *             asynchronously, the caller should use the returned {@link @ListenableFuture} to
     *             wait for the success/failure result.
     */
    void setReceiver(@NonNull Supplier<ListenableFuture<Void>> tick);

    /**
     * Clears the receiver from the notifier.
     */
    void clearReceiver();
}
