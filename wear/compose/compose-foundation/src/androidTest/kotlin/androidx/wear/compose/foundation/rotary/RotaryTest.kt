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

@file:OptIn(
    androidx.compose.ui.test.ExperimentalTestApi::class,
    ExperimentalWearFoundationApi::class
)

package androidx.wear.compose.foundation.rotary

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.RotaryInjectionScope
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performRotaryScrollInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import com.google.common.truth.Truth
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// TODO(b/278705775): Add more tests to check Rotary Snap behavior
class RotaryScrollTest {
    @get:Rule
    val rule = createComposeRule()

    private var itemSizePx: Float = 50f
    private var itemSizeDp: Dp = Dp.Infinity

    private val focusRequester = FocusRequester()
    private lateinit var state: LazyListState

    @Before
    fun before() {
        with(rule.density) {
            itemSizeDp = itemSizePx.toDp()
        }
    }

    @Test
    fun scroll_by_one_item() {
        var itemIndex = 0

        testScroll(
            beforeScroll = {
                itemIndex = state.firstVisibleItemIndex
            },
            rotaryAction = {
                rotateToScrollVertically(itemSizePx)
            }
        )

        rule.runOnIdle {
            Truth.assertThat(state.firstVisibleItemIndex).isEqualTo(itemIndex + 1)
        }
    }

    @Test
    fun no_fling_with_filtered_negative_values() {
        var itemIndex = 0

        testScroll(
            beforeScroll = {
                itemIndex = state.firstVisibleItemIndex
            },
            rotaryAction = {
                rotateToScrollVertically(itemSizePx)
                advanceEventTime(20)
                rotateToScrollVertically(-1f)
                advanceEventTime(20)
                rotateToScrollVertically(-1f)
                advanceEventTime(20)
                rotateToScrollVertically(itemSizePx)
                advanceEventTime(20)
                rotateToScrollVertically(2f)
                advanceEventTime(20)
            }
        )

        rule.runOnIdle {
            Truth.assertThat(state.firstVisibleItemIndex).isEqualTo(itemIndex + 2)
        }
    }

    @Test
    fun slow_scroll_by_two_items() {
        var itemIndex = 0

        testScroll(
            beforeScroll = {
                itemIndex = state.firstVisibleItemIndex
            },
            rotaryAction = {
                rotateToScrollVertically(itemSizePx)
                advanceEventTime(300)
                rotateToScrollVertically(itemSizePx)
            }
        )

        rule.runOnIdle {
            Truth.assertThat(state.firstVisibleItemIndex).isEqualTo(itemIndex + 2)
        }
    }

    @Test
    fun fast_scroll_with_reverse() {
        var itemIndex = 0

        testScroll(
            beforeScroll = {
                itemIndex = state.firstVisibleItemIndex
            },
            rotaryAction = {
                // Scroll forwards by 2 items
                rotateToScrollVertically(itemSizePx)
                advanceEventTime(10)
                rotateToScrollVertically(itemSizePx)
                advanceEventTime(10)
                // Instantly scroll backwards by 2 items
                rotateToScrollVertically(-itemSizePx)
                advanceEventTime(10)
                rotateToScrollVertically(-itemSizePx)
            }
        )

        rule.runOnIdle {
            // Check that we're on the same position
            Truth.assertThat(state.firstVisibleItemIndex).isEqualTo(itemIndex)
        }
    }

    @Test
    fun fast_scroll_with_fling() {
        var itemIndex = 0

        testScroll(
            beforeScroll = {
                itemIndex = state.firstVisibleItemIndex
            },
            rotaryAction = {
                // To produce fling we need to send 3 events,
                // which will be increasing the scroll velocity.
                // First event initializes velocityTracker
                rotateToScrollVertically(itemSizePx)
                advanceEventTime(10)
                // Next 2 events should increase the scroll velocity.
                rotateToScrollVertically(itemSizePx * 5)
                advanceEventTime(10)
                rotateToScrollVertically(itemSizePx * 6)
            }
        )

        rule.runOnIdle {
            // We check that we indeed scrolled the list further than
            // amount of pixels which we scrolled by.
            Truth.assertThat(state.firstVisibleItemIndex).isGreaterThan(itemIndex + 12)
        }
    }

    @Test
    fun fading_scroll_without_fling() {
        var itemIndex = 0

        testScroll(
            beforeScroll = {
                itemIndex = state.firstVisibleItemIndex
            },
            rotaryAction = {
                // Fling will not be produced when scroll velocity decreases with each event
                // By decreasing the distance with each event we're
                // sure that the velocity also decreases.
                rotateToScrollVertically(itemSizePx * 5)
                advanceEventTime(20)
                rotateToScrollVertically(itemSizePx * 4)
                advanceEventTime(20)
                rotateToScrollVertically(itemSizePx * 3)
                advanceEventTime(20)
                rotateToScrollVertically(itemSizePx * 2)
                advanceEventTime(20)
                rotateToScrollVertically(itemSizePx)
                advanceEventTime(20)
            }
        )

        rule.runOnIdle {
            // We check that we scrolled exactly 10 items, not more as it would be with a fling.
            Truth.assertThat(state.firstVisibleItemIndex).isEqualTo(itemIndex + 15)
        }
    }

    private fun testScroll(
        beforeScroll: () -> Unit,
        rotaryAction: RotaryInjectionScope.() -> Unit
    ) {
        rule.setContent {
            state = rememberLazyListState()
            DefaultLazyColumnItemsWithRotary(
                itemSize = itemSizeDp,
                scrollableState = state,
                rotaryBehavior = RotaryDefaults.scrollBehavior(state),
                focusRequester = focusRequester
            )
        }
        rule.runOnIdle { focusRequester.requestFocus() }
        beforeScroll()
        rule.onNodeWithTag(TEST_TAG).performRotaryScrollInput {
            rotaryAction()
        }
    }

    @Composable
    private fun DefaultLazyColumnItemsWithRotary(
        itemSize: Dp,
        focusRequester: FocusRequester,
        rotaryBehavior: RotaryBehavior,
        scrollableState: LazyListState,
    ) {
        LazyColumn(
            modifier = Modifier
                .size(200.dp)
                .testTag(TEST_TAG)
                .rotary(rotaryBehavior, focusRequester),
            state = scrollableState,
        ) {
            items(300) {
                BasicText(
                    modifier = Modifier.height(itemSize),
                    text = "Item #$it"
                )
            }
        }
    }

    companion object {
        const val TEST_TAG = "test-tag"
    }
}
