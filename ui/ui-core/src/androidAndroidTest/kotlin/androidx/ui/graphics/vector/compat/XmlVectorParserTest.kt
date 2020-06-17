/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.graphics.vector.compat

import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.ui.core.test.R
import androidx.ui.graphics.Color
import androidx.ui.graphics.SolidColor
import androidx.ui.graphics.vector.PathNode
import androidx.ui.graphics.vector.VectorGroup
import androidx.ui.graphics.vector.VectorNode
import androidx.ui.graphics.vector.VectorPath
import androidx.ui.res.loadVectorResource
import androidx.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class XmlVectorParserTest {

    @Test
    fun testParseXml() {
        val res = InstrumentationRegistry.getInstrumentation().targetContext.resources
        val asset = loadVectorResource(
            null,
            res,
            R.drawable.test_compose_vector
        )
        val expectedSize = 24.dp
        assertEquals(expectedSize, asset.defaultWidth)
        assertEquals(expectedSize, asset.defaultHeight)
        assertEquals(24.0f, asset.viewportWidth)
        assertEquals(24.0f, asset.viewportHeight)
        assertEquals(1, asset.root.size)

        val node = asset.root.iterator().next() as VectorPath
        assertEquals(Color(0xFFFF0000), (node.fill as SolidColor).value)

        val path = node.pathData
        assertEquals(5, path.size)

        val moveTo = path[0].assertType<PathNode.MoveTo>()
        assertEquals(20.0f, moveTo.x)
        assertEquals(10.0f, moveTo.y)

        val relativeLineTo1 = path[1].assertType<PathNode.RelativeLineTo>()
        assertEquals(10.0f, relativeLineTo1.x)
        assertEquals(0.0f, relativeLineTo1.y)

        val relativeLineTo2 = path[2].assertType<PathNode.RelativeLineTo>()
        assertEquals(0.0f, relativeLineTo2.x)
        assertEquals(10.0f, relativeLineTo2.y)

        val relativeLineTo3 = path[3].assertType<PathNode.RelativeLineTo>()
        assertEquals(-10.0f, relativeLineTo3.x)
        assertEquals(0.0f, relativeLineTo3.y)

        path[4].assertType<PathNode.Close>()
    }

    @Test
    fun testImplicitLineTo() {
        val res = InstrumentationRegistry.getInstrumentation().targetContext.resources
        val asset = loadVectorResource(
            null,
            res,
            R.drawable.test_compose_vector2
        )

        val node = asset.root.iterator().next() as VectorPath
        val path = node.pathData

        assertEquals(3, path.size)

        val moveTo = path[0].assertType<PathNode.MoveTo>()
        assertEquals(20.0f, moveTo.x)
        assertEquals(10.0f, moveTo.y)

        val lineTo = path[1].assertType<PathNode.LineTo>()
        assertEquals(10.0f, lineTo.x)
        assertEquals(0.0f, lineTo.y)

        path[2].assertType<PathNode.Close>()
    }

    @Test
    fun testGroupParsing() {
        val res = InstrumentationRegistry.getInstrumentation().targetContext.resources
        val asset = loadVectorResource(
            null,
            res,
            R.drawable.test_compose_vector3
        )

        val root = asset.root
        assertEquals(1, root.size)

        val delta = 0.001f
        val group = root[0].assertType<VectorGroup>()
        assertEquals(1, group.size)
        assertEquals(1f, group.pivotX, delta)
        assertEquals(2f, group.pivotY, delta)
        assertEquals(3f, group.rotation, delta)
        assertEquals(4f, group.scaleX, delta)
        assertEquals(5f, group.scaleY, delta)
        assertEquals(6f, group.translationX, delta)
        assertEquals(7f, group.translationY, delta)

        val path = group[0].assertType<VectorPath>().pathData

        assertEquals(3, path.size)

        val moveTo = path[0].assertType<PathNode.MoveTo>()
        assertEquals(20.0f, moveTo.x)
        assertEquals(10.0f, moveTo.y)

        val lineTo = path[1].assertType<PathNode.LineTo>()
        assertEquals(10.0f, lineTo.x)
        assertEquals(0.0f, lineTo.y)

        path[2].assertType<PathNode.Close>()
    }

    /**
     * Asserts that [this] is the expected type [T], and then returns [this] cast to [T].
     */
    private inline fun <reified T : PathNode> PathNode.assertType(): T {
        assertTrue(
            "Expected type ${T::class.java.simpleName} but was actually " +
                    this::class.java.simpleName,
            this is T
        )
        return this as T
    }

    /**
     * Asserts that [this] is the expected type [T], and then returns [this] cast to [T].
     */
    private inline fun <reified T : VectorNode> VectorNode.assertType(): T {
        assertTrue(
            "Expected type ${T::class.java.simpleName} but was actually " +
                    this::class.java.simpleName,
            this is T
        )
        return this as T
    }
}