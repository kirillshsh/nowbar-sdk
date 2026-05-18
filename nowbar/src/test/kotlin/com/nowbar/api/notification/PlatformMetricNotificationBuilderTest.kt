package com.nowbar.api.notification

import androidx.core.graphics.drawable.IconCompat
import com.nowbar.api.cards.MetricCard
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PlatformMetricNotificationBuilderTest {

    private val context = RuntimeEnvironment.getApplication()
    private val icon: IconCompat
        get() = IconCompat.createWithResource(context, android.R.drawable.ic_dialog_info)

    @Test
    fun `metric card secondary text maps to platform subtext`() {
        val card = MetricCard.Builder.create(
            "Workout",
            icon,
            LiveUpdateMetricStyle(
                metrics = listOf(LiveUpdateMetric("Steps", LiveUpdateMetricValue.FixedInt(1979)))
            )
        )
            .secondaryText("Running")
            .build()

        assertEquals("Running", card.toSubText())
    }

    @Test
    fun `blank metric secondary text does not set platform subtext`() {
        val card = MetricCard.Builder.create(
            "Workout",
            icon,
            LiveUpdateMetricStyle(
                metrics = listOf(LiveUpdateMetric("Steps", LiveUpdateMetricValue.FixedInt(1979)))
            )
        ).build()

        assertNull(card.toSubText())
    }
}
