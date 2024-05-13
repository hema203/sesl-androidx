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

package androidx.compose.foundation.text.selection.gestures

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text.selection.gestures.util.TextField2SelectionAsserter
import androidx.compose.foundation.text.selection.gestures.util.TextFieldSelectionAsserter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Ignore
import org.junit.Test

internal abstract class TextField2SelectionGesturesTest(
    initialText: String,
    private val layoutDirection: LayoutDirection,
) : TextFieldSelectionGesturesTest<TextFieldState>() {
    private val textFieldState: TextFieldState = TextFieldState(initialText)

    override var textContent: String
        get() = textFieldState.text.toString()
        set(value) {
            textFieldState.setTextAndPlaceCursorAtEnd(value)
        }

    override var readOnly by mutableStateOf(false)
    override var enabled by mutableStateOf(true)

    override lateinit var asserter: TextFieldSelectionAsserter<TextFieldState>

    override fun setupAsserter() {
        asserter = TextField2SelectionAsserter(
            textContent = textFieldState.text.toString(),
            rule = rule,
            textToolbar = textToolbar,
            hapticFeedback = hapticFeedback,
            getActual = { textFieldState }
        )
    }

    @Composable
    override fun Content() {
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            BasicTextField(
                state = textFieldState,
                readOnly = readOnly,
                enabled = enabled,
                textStyle = TextStyle(fontFamily = fontFamily, fontSize = fontSize),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(pointerAreaTag),
            )
        }
    }

    // TODO(b/339917241) Text toolbar does not appear in BTF2 for this use case like it should.
    // When fixed, this function can be removed and the super function can have `open` removed.
    @Test
    @Ignore("b/339917241")
    override fun whenReadOnly_touchLongPress_startsSelection() {
        super.whenReadOnly_touchLongPress_startsSelection()
    }
}
