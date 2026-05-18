package com.nowbar.api.notification

import androidx.core.graphics.drawable.IconCompat
import com.nowbar.api.cards.CustomCard
import com.nowbar.api.cards.DeliveryCard
import com.nowbar.api.cards.DeliveryStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ProgressStyleAdapterTest {

    @Test
    fun `delivery initializing card uses indeterminate progress`() {
        val context = RuntimeEnvironment.getApplication()
        val card = DeliveryCard(
            title = "Delivery",
            icon = IconCompat.createWithResource(context, android.R.drawable.ic_menu_compass),
            status = DeliveryStatus.INITIALIZING,
            merchantName = "Bakery"
        )

        val config = ProgressStyleAdapter.adapt(card)

        assertTrue(card.isProgressIndeterminate())
        assertEquals(4, config.segments.size)
        assertEquals(0, config.points.size)
    }

    @Test
    fun `delivery card maps completed milestones to progress points`() {
        val context = RuntimeEnvironment.getApplication()
        val card = DeliveryCard(
            title = "Delivery",
            icon = IconCompat.createWithResource(context, android.R.drawable.ic_menu_compass),
            status = DeliveryStatus.EN_ROUTE,
            merchantName = "Bakery",
            progress = 60
        )

        val config = ProgressStyleAdapter.adapt(card)

        assertEquals(listOf(25, 50), config.points.map { it.position })
        assertEquals(4, config.segments.size)
    }

    @Test
    fun `custom card can provide full ProgressStyle config`() {
        val context = RuntimeEnvironment.getApplication()
        val icon = IconCompat.createWithResource(context, android.R.drawable.ic_menu_compass)
        val card = CustomCard.Builder.create("Trip", icon, "Driver nearby")
            .progressMax(200)
            .progressValue(120)
            .progressSegments(
                listOf(
                    StyleSegment(length = 80, color = 0x111111, id = 1),
                    StyleSegment(length = 120, color = 0x222222, id = 2)
                )
            )
            .progressPoints(listOf(StylePoint(position = 80, color = 0x333333, id = 3)))
            .progressTrackerIcon(icon)
            .progressStartIcon(icon)
            .progressEndIcon(icon)
            .progressStyledByProgress()
            .build()

        val config = ProgressStyleAdapter.adapt(card)

        assertEquals(listOf(80, 120), config.segments.map { it.length })
        assertEquals(listOf(80), config.points.map { it.position })
        assertEquals(icon, config.trackerIcon)
        assertEquals(icon, config.startIcon)
        assertEquals(icon, config.endIcon)
        assertTrue(config.styledByProgress)
    }

    @Test
    fun `custom card default ProgressStyle segment uses card max`() {
        val context = RuntimeEnvironment.getApplication()
        val card = CustomCard.Builder.create(
            "Upload",
            IconCompat.createWithResource(context, android.R.drawable.ic_menu_upload),
            "Uploading"
        )
            .progressMax(250)
            .progressValue(125)
            .build()

        val config = ProgressStyleAdapter.adapt(card)

        assertEquals(listOf(250), config.segments.map { it.length })
    }

    @Test
    fun `custom card segment lengths define ProgressStyle max`() {
        val context = RuntimeEnvironment.getApplication()
        val card = CustomCard.Builder.create(
            "Trip",
            IconCompat.createWithResource(context, android.R.drawable.ic_menu_compass),
            "Driver nearby"
        )
            .progressValue(220)
            .progressSegments(
                listOf(
                    StyleSegment(length = 80, color = 0x111111),
                    StyleSegment(length = 120, color = 0x222222)
                )
            )
            .build()

        assertEquals(200, card.toProgressMax())
        assertEquals(200, card.toProgress())
    }
}
