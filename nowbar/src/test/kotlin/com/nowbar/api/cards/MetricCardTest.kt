package com.nowbar.api.cards

import androidx.core.graphics.drawable.IconCompat
import com.nowbar.api.notification.LiveUpdateMetric
import com.nowbar.api.notification.LiveUpdateMetricStyle
import com.nowbar.api.notification.LiveUpdateMetricValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class MetricCardTest {

    private val context = RuntimeEnvironment.getApplication()
    private val icon: IconCompat
        get() = IconCompat.createWithResource(context, android.R.drawable.ic_dialog_info)

    @Test
    fun `metric card exposes Android 17 metric style`() {
        val style = LiveUpdateMetricStyle(
            metrics = listOf(LiveUpdateMetric("Steps", LiveUpdateMetricValue.FixedInt(1979)))
        )
        val card = MetricCard.Builder.create("Workout", icon, style)
            .primaryText("Workout metrics")
            .secondaryText("Running")
            .build()

        assertEquals(CardType.METRIC, card.type)
        assertEquals(style, card.toMetricStyle())
        assertEquals("Workout metrics", card.toPrimaryInfo())
        assertEquals("Running", card.toSecondaryInfo())
    }

    @Test
    fun `metric style rejects empty or oversized metric sets`() {
        assertFailsWith<IllegalArgumentException> {
            LiveUpdateMetricStyle(metrics = emptyList())
        }

        assertFailsWith<IllegalArgumentException> {
            LiveUpdateMetricStyle(
                metrics = List(4) { index ->
                    LiveUpdateMetric("M$index", LiveUpdateMetricValue.FixedInt(index))
                }
            )
        }
    }

    @Test
    fun `metric values reject invalid platform inputs`() {
        assertFailsWith<IllegalArgumentException> {
            LiveUpdateMetricValue.FixedFloat(Float.NaN)
        }

        assertFailsWith<IllegalArgumentException> {
            LiveUpdateMetricValue.ElapsedRealtimeTimer(-1L)
        }

        assertFailsWith<IllegalArgumentException> {
            LiveUpdateMetricValue.ElapsedRealtimeStopwatch(-1L)
        }

        assertFailsWith<IllegalArgumentException> {
            LiveUpdateMetricValue.PausedTimer((-1).seconds.toJavaDuration())
        }

        assertFailsWith<IllegalArgumentException> {
            LiveUpdateMetricValue.PausedStopwatch((-1).seconds.toJavaDuration())
        }
    }
}
