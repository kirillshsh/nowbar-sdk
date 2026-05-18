package com.nowbar.api

import android.app.NotificationManager
import androidx.core.graphics.drawable.IconCompat
import com.nowbar.api.cards.TimerCard
import com.nowbar.api.fallback.FallbackStrategy
import com.nowbar.api.notification.NowBarNotificationEvidence
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NowBarSessionImplTest {

    @Test
    fun `start stays idle when fallback none should not post`() {
        val context = RuntimeEnvironment.getApplication()
        val channelId = "none-test"
        val session = NowBarManager.createSession(
            context,
            NowBarConfig(
                channelId = channelId,
                channelName = "Test",
                fallbackStrategy = FallbackStrategy.NONE
            )
        )
        val card = TimerCard(
            title = "Timer",
            icon = IconCompat.createWithResource(context, android.R.drawable.ic_menu_recent_history),
            totalDuration = 1.minutes,
            remainingDuration = 1.minutes
        )

        session.start(card)

        assertEquals(SessionState.IDLE, session.state.value)
        assertNull(notificationManager(context).getNotificationChannel(channelId))
    }

    @Test
    fun `notify creates notification channel before posting`() {
        val context = RuntimeEnvironment.getApplication()
        val channelId = "auto-channel-notify"
        val config = NowBarConfig(
            channelId = channelId,
            channelName = "Auto Channel",
            fallbackStrategy = FallbackStrategy.STANDARD_NOTIFICATION
        )
        val card = timerCard(context)

        NowBarManager.notify(context, config, card)

        assertNotNull(notificationManager(context).getNotificationChannel(channelId))
    }

    @Test
    fun `session start creates notification channel before posting`() {
        val context = RuntimeEnvironment.getApplication()
        val channelId = "auto-channel-session"
        val config = NowBarConfig(
            channelId = channelId,
            channelName = "Auto Channel",
            fallbackStrategy = FallbackStrategy.STANDARD_NOTIFICATION
        )
        val session = NowBarManager.createSession(context, config)

        session.start(timerCard(context))

        assertNotNull(notificationManager(context).getNotificationChannel(channelId))
    }

    @Test
    @Config(sdk = [32])
    fun `session unpin keeps notification but removes enhanced surface hints`() {
        val context = RuntimeEnvironment.getApplication()
        shadowOf(context.packageManager).setSystemFeature(FeatureDetector.FEATURE_SAMSUNG_NOWBAR, true)
        val config = NowBarConfig(
            channelId = "unpin-session",
            channelName = "Unpin Session"
        )
        val session = NowBarManager.createSession(context, config)
        val manager = notificationManager(context)

        session.start(timerCard(context))

        val activeEvidence = NowBarNotificationEvidence.inspect(
            shadowOf(manager).getNotification(config.notificationId)
        )
        assertEquals(SessionState.ACTIVE, session.state.value)
        assertTrue(activeEvidence.hasSamsungNowBarEvidence)

        session.unpin()

        val unpinnedEvidence = NowBarNotificationEvidence.inspect(
            shadowOf(manager).getNotification(config.notificationId)
        )
        assertEquals(SessionState.PAUSED, session.state.value)
        assertFalse(unpinnedEvidence.requestPromotedOngoing)
        assertFalse(unpinnedEvidence.hasSamsungNowBarEvidence)
        assertFalse(unpinnedEvidence.hasAndroidLiveUpdateEvidence)
        assertFalse(unpinnedEvidence.likelyNowBarCompatible)
    }

    private fun timerCard(context: android.content.Context) = TimerCard(
        title = "Timer",
        icon = IconCompat.createWithResource(context, android.R.drawable.ic_menu_recent_history),
        totalDuration = 1.minutes,
        remainingDuration = 1.minutes
    )

    private fun notificationManager(context: android.content.Context): NotificationManager {
        return context.getSystemService(NotificationManager::class.java)
    }
}
