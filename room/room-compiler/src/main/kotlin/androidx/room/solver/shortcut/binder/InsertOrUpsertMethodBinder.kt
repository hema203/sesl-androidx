/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.room.solver.shortcut.binder

import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.solver.CodeGenScope
import androidx.room.solver.shortcut.result.InsertOrUpsertMethodAdapter
import androidx.room.vo.ShortcutQueryParameter

/** Connects the insert and upsert method, the database and the [InsertOrUpsertMethodAdapter]. */
abstract class InsertOrUpsertMethodBinder(val adapter: InsertOrUpsertMethodAdapter?) {

    /**
     * Received an insert or upsert method parameters, their adapters and generations the code that
     * runs the insert or upsert and returns the result.
     */
    abstract fun convertAndReturn(
        parameters: List<ShortcutQueryParameter>,
        adapters: Map<String, Pair<XPropertySpec, Any>>,
        dbProperty: XPropertySpec,
        scope: CodeGenScope
    )

    abstract fun convertAndReturnCompat(
        parameters: List<ShortcutQueryParameter>,
        adapters: Map<String, Pair<XPropertySpec, Any>>,
        dbProperty: XPropertySpec,
        scope: CodeGenScope
    )

    // TODO(b/319660042): Remove once migration to driver API is done.
    open fun isMigratedToDriver(): Boolean = false
}
