package com.nowbar.api

import androidx.core.graphics.drawable.IconCompat
import com.nowbar.api.notification.ActionConfig
import com.nowbar.api.cards.CustomCard
import com.nowbar.api.cards.TimerCard
import com.nowbar.api.fallback.FallbackStrategy
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBuild

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NowBarReadinessTest {

    @AfterTest
    fun resetBuild() {
        ShadowBuild.reset()
    }

    @Test
    fun `inspect reports Samsung extras readiness for supported Samsung devices`() {
        val context = RuntimeEnvironment.getApplication()
        shadowOf(context.packageManager).setSystemFeature(FeatureDetector.FEATURE_SAMSUNG_NOWBAR, true)
        ShadowBuild.setManufacturer("Samsung")
        ShadowBuild.setBrand("samsung")

        val report = NowBarManager.inspectReadiness(
            context = context,
            config = NowBarConfig(channelId = "ready", channelName = "Ready"),
            card = timerCard(context)
        )

        assertTrue(report.willPost)
        assertFalse(report.usesStandardNotificationOnly)
        assertTrue(report.readyForSamsungNowBar)
        assertFalse(report.readyForAndroidLiveUpdates)
        assertTrue(report.readyForEnhancedSurface)
        assertFalse(report.willUseStandardFallback)
        assertTrue(report.evidence.hasSamsungNowBarEvidence)
        assertTrue(report.evidence.hasEligibleCoreFields)
        assertTrue(report.blockingReasons.isEmpty())
        assertTrue(report.toDisplayString().contains("Ready for enhanced surface: true"))
    }

    @Test
    fun `inspect reports standard fallback when forced by config`() {
        val context = RuntimeEnvironment.getApplication()
        shadowOf(context.packageManager).setSystemFeature(FeatureDetector.FEATURE_SAMSUNG_NOWBAR, true)
        ShadowBuild.setManufacturer("Samsung")
        ShadowBuild.setBrand("samsung")

        val report = NowBarReadiness.inspect(
            context = context,
            config = NowBarConfig(
                channelId = "standard",
                channelName = "Standard",
                fallbackStrategy = FallbackStrategy.STANDARD_NOTIFICATION
            ),
            card = timerCard(context)
        )

        assertTrue(report.willPost)
        assertTrue(report.usesStandardNotificationOnly)
        assertFalse(report.readyForSamsungNowBar)
        assertFalse(report.readyForEnhancedSurface)
        assertTrue(report.willUseStandardFallback)
        assertFalse(report.evidence.hasSamsungNowBarEvidence)
        assertTrue("standard-notification-only" in report.blockingReasons)
    }

    @Test
    fun `inspect reports no posting when fallback none is used on unsupported devices`() {
        val context = RuntimeEnvironment.getApplication()
        shadowOf(context.packageManager).setSystemFeature(FeatureDetector.FEATURE_SAMSUNG_NOWBAR, false)
        ShadowBuild.setManufacturer("Google")
        ShadowBuild.setBrand("google")

        val report = NowBarReadiness.inspect(
            context = context,
            config = NowBarConfig(
                channelId = "none",
                channelName = "None",
                fallbackStrategy = FallbackStrategy.NONE
            ),
            card = timerCard(context)
        )

        assertFalse(report.willPost)
        assertFalse(report.readyForEnhancedSurface)
        assertTrue(report.willUseStandardFallback.not())
        assertTrue("posting-disabled-by-fallback-strategy" in report.blockingReasons)
        assertTrue("device:no-native-nowbar-or-live-updates-surface" in report.blockingReasons)
        assertTrue("device:no-enhanced-surface" in report.blockingReasons)
    }

    @Test
    fun `manager delegate returns live update and evidence reports`() {
        val context = RuntimeEnvironment.getApplication()
        val config = NowBarConfig(
            channelId = "reports",
            channelName = "Reports",
            fallbackStrategy = FallbackStrategy.STANDARD_NOTIFICATION
        )

        val report = NowBarManager.inspectReadiness(context, config, timerCard(context))

        assertTrue(report.liveUpdate.hasContentTitle)
        assertTrue(report.evidence.ongoing)
        assertTrue(report.evidence.hasContentTitle)
    }

    @Test
    fun `inspect surfaces live update status chip advisories`() {
        val context = RuntimeEnvironment.getApplication()
        val card = CustomCard.Builder.create(
            "Delivery",
            IconCompat.createWithResource(context, android.R.drawable.ic_menu_compass),
            "Arriving"
        )
            .shortCriticalText("Arriving soon")
            .build()

        val report = NowBarReadiness.inspect(
            context = context,
            config = NowBarConfig(channelId = "advisory", channelName = "Advisory"),
            card = card
        )

        assertTrue("live-update:status-chip-text-may-truncate" in report.advisoryReasons)
        assertTrue(report.toDisplayString().contains("Advisories: live-update:status-chip-text-may-truncate"))
    }

    @Test
    fun `inspect surfaces too-soon status chip when advisory`() {
        val context = RuntimeEnvironment.getApplication()
        val card = CustomCard.Builder.create(
            "Ride",
            IconCompat.createWithResource(context, android.R.drawable.ic_menu_compass),
            "Arriving"
        )
            .chipWhenTimeMillis(System.currentTimeMillis() + 30_000L)
            .build()

        val report = NowBarReadiness.inspect(
            context = context,
            config = NowBarConfig(channelId = "when", channelName = "When"),
            card = card
        )

        assertTrue(report.liveUpdate.statusChipWhenTooSoon)
        assertTrue("live-update:status-chip-when-too-soon" in report.advisoryReasons)
    }

    @Test
    fun `inspect surfaces live update action delete intent and truncation advisories`() {
        val context = RuntimeEnvironment.getApplication()
        val card = CustomCard.Builder.create(
            "Controls",
            IconCompat.createWithResource(context, android.R.drawable.ic_menu_compass),
            "Running"
        )
            .actions(
                listOf(
                    ActionConfig.textOnly("pause", "Pause"),
                    ActionConfig.textOnly("stop", "Stop"),
                    ActionConfig.textOnly("next", "Next"),
                    ActionConfig.textOnly("unpin", "Unpin")
                )
            )
            .build()

        val report = NowBarReadiness.inspect(
            context = context,
            config = NowBarConfig(channelId = "actions", channelName = "Actions"),
            card = card
        )

        assertEquals(4, report.requestedActionCount)
        assertEquals(3, report.liveUpdate.androidActionCount)
        assertEquals(3, report.evidence.androidActionCount)
        assertTrue("live-update:missing-delete-intent" in report.advisoryReasons)
        assertTrue("notification:action-buttons-truncated" in report.advisoryReasons)
        assertTrue("live-update:too-many-action-buttons" !in report.advisoryReasons)
    }

    private fun timerCard(context: android.content.Context) = TimerCard(
        title = "Timer",
        icon = IconCompat.createWithResource(context, android.R.drawable.ic_menu_recent_history),
        totalDuration = 5.minutes,
        remainingDuration = 3.minutes
    )
}
