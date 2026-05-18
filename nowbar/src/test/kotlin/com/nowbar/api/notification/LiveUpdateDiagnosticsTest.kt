package com.nowbar.api.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.graphics.drawable.Icon
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class LiveUpdateDiagnosticsTest {

    @Test
    @Config(sdk = [35])
    fun `inspect reports promotion request and api blocker below android 16`() {
        val context = RuntimeEnvironment.getApplication()
        val notification = NotificationCompat.Builder(context, "test")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Delivery")
            .setOngoing(true)
            .setRequestPromotedOngoing(true)
            .build()

        val report = LiveUpdateDiagnostics.inspect(context, notification)

        assertFalse(report.apiSupported)
        assertTrue(report.requestPromotedOngoing)
        assertTrue(report.ongoing)
        assertTrue(report.hasContentTitle)
        assertFalse(report.eligible)
        assertTrue("api<36" in report.blockingReasons)
    }

    @Test
    @Config(sdk = [35])
    fun `request promoted ongoing helper falls back to AndroidX extras below platform accessor`() {
        val context = RuntimeEnvironment.getApplication()
        val notification = NotificationCompat.Builder(context, "test")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Delivery")
            .setOngoing(true)
            .setRequestPromotedOngoing(true)
            .build()

        assertTrue(LiveUpdateDiagnostics.isRequestPromotedOngoing(notification))
    }

    @Test
    fun `manifest declares promoted notifications permission`() {
        val context = RuntimeEnvironment.getApplication()
        shadowOf(context.packageManager)
            .getInternalMutablePackageInfo(context.packageName)
            .requestedPermissions = arrayOf(
                LiveUpdateDiagnostics.PERMISSION_POST_PROMOTED_NOTIFICATIONS
            )

        assertTrue(LiveUpdateDiagnostics.declaresPostPromotedNotifications(context))
    }

    @Test
    fun `promotion settings intent uses official action and package extra`() {
        val context = RuntimeEnvironment.getApplication()
        val intent = LiveUpdateDiagnostics.createPromotionSettingsIntent(context)
        val manageIntent = LiveUpdateDiagnostics.createManageAppPromotedNotificationsIntent(context)

        assertEquals(LiveUpdateDiagnostics.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS, intent.action)
        assertEquals(LiveUpdateDiagnostics.ACTION_MANAGE_APP_PROMOTED_NOTIFICATIONS, manageIntent.action)
        assertEquals(intent.action, manageIntent.action)
        assertEquals(context.packageName, intent.getStringExtra("android.provider.extra.APP_PACKAGE"))
        assertEquals(context.packageName, manageIntent.getStringExtra("android.provider.extra.APP_PACKAGE"))
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertTrue(manageIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun `promotion settings resolver returns null when settings activity is missing`() {
        val context = RuntimeEnvironment.getApplication()

        assertFalse(LiveUpdateDiagnostics.canOpenPromotionSettings(context))
        assertFalse(LiveUpdateDiagnostics.canOpenManageAppPromotedNotifications(context))
        assertEquals(null, LiveUpdateDiagnostics.resolvePromotionSettingsIntent(context))
        assertEquals(null, LiveUpdateDiagnostics.resolveManageAppPromotedNotificationsIntent(context))
    }

    @Test
    @Suppress("DEPRECATION")
    fun `promotion settings resolver returns official intent when settings activity exists`() {
        val context = RuntimeEnvironment.getApplication()
        val intent = LiveUpdateDiagnostics.createPromotionSettingsIntent(context)
        val resolveInfo = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = "com.android.settings"
                name = "PromotedNotificationSettingsActivity"
            }
        }
        shadowOf(context.packageManager).addResolveInfoForIntent(intent, resolveInfo)

        val resolved = LiveUpdateDiagnostics.resolvePromotionSettingsIntent(context)
        val manageResolved = LiveUpdateDiagnostics.resolveManageAppPromotedNotificationsIntent(context)

        assertTrue(LiveUpdateDiagnostics.canOpenPromotionSettings(context))
        assertTrue(LiveUpdateDiagnostics.canOpenManageAppPromotedNotifications(context))
        assertEquals(intent.action, resolved?.action)
        assertEquals(intent.action, manageResolved?.action)
    }

    @Test
    @Config(sdk = [35])
    fun `inspect classifies allowed Live Update style templates`() {
        val context = RuntimeEnvironment.getApplication()
        val templates = mapOf(
            "androidx.core.app.NotificationCompat\$BigTextStyle" to LiveUpdateAllowedStyle.BIG_TEXT,
            "androidx.core.app.NotificationCompat\$CallStyle" to LiveUpdateAllowedStyle.CALL,
            "androidx.core.app.NotificationCompat\$ProgressStyle" to LiveUpdateAllowedStyle.PROGRESS,
            "androidx.core.app.NotificationCompat\$MetricStyle" to LiveUpdateAllowedStyle.METRIC
        )

        templates.forEach { (template, style) ->
            val notification = NotificationCompat.Builder(context, "test")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Live Update")
                .setOngoing(true)
                .addExtras(Bundle().apply {
                    putString(Notification.EXTRA_TEMPLATE, template)
                })
                .build()

            val report = LiveUpdateDiagnostics.inspect(context, notification)

            assertTrue(report.allowedStyle)
            assertEquals(style, report.liveUpdateStyle)
        }
    }

    @Test
    @Config(sdk = [35])
    fun `inspect reports notification subtext for metric style header context`() {
        val context = RuntimeEnvironment.getApplication()
        val notification = NotificationCompat.Builder(context, "test")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Workout")
            .setSubText("Running")
            .setOngoing(true)
            .build()

        val report = LiveUpdateDiagnostics.inspect(context, notification)

        assertEquals("Running", report.subText)
    }

    @Test
    @Config(sdk = [35])
    fun `inspect reports Android progress extras`() {
        val context = RuntimeEnvironment.getApplication()
        val icon = IconCompat.createWithResource(context, android.R.drawable.ic_menu_compass)
        val notification = NotificationCompat.Builder(context, "test")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Order")
            .setOngoing(true)
            .setStyle(
                NotificationCompat.ProgressStyle()
                    .setProgress(25)
                    .setProgressIndeterminate(true)
                    .setProgressSegments(
                        listOf(
                            NotificationCompat.ProgressStyle.Segment(40).setColor(0xFF111111.toInt()),
                            NotificationCompat.ProgressStyle.Segment(60).setColor(0xFF222222.toInt())
                        )
                    )
                    .setProgressPoints(
                        listOf(
                            NotificationCompat.ProgressStyle.Point(25).setColor(0xFF333333.toInt())
                        )
                    )
                    .setProgressTrackerIcon(icon)
                    .setProgressStartIcon(icon)
                    .setProgressEndIcon(icon)
                    .setStyledByProgress(false)
            )
            .build()

        val report = LiveUpdateDiagnostics.inspect(context, notification)

        assertEquals(25, report.androidProgress)
        assertEquals(100, report.androidProgressMax)
        assertEquals(true, report.androidProgressIndeterminate)
        assertEquals(2, report.androidProgressSegmentCount)
        assertEquals(1, report.androidProgressPointCount)
        assertTrue(report.androidHasProgressTrackerIcon)
        assertTrue(report.androidHasProgressStartIcon)
        assertTrue(report.androidHasProgressEndIcon)
        assertEquals(false, report.androidStyledByProgress)
        assertTrue(report.hasAndroidProgress)
        assertTrue(report.hasAndroidProgressStylePayload)
        assertTrue(report.allowedStyle)
        assertEquals(LiveUpdateAllowedStyle.PROGRESS, report.liveUpdateStyle)
    }

    @Test
    @Config(sdk = [35])
    fun `inspect reports action button evidence and advisories`() {
        val context = RuntimeEnvironment.getApplication()
        val intent = PendingIntent.getBroadcast(
            context,
            31,
            Intent("test.ACTION"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val icon = Icon.createWithResource(context, android.R.drawable.ic_dialog_info)
        val notification = Notification.Builder(context, "test")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Controls")
            .setOngoing(true)
            .addAction(Notification.Action.Builder(icon, "Pause", intent).build())
            .addAction(Notification.Action.Builder(icon, "Stop", intent).build())
            .addAction(Notification.Action.Builder(null, "Tip", null).build())
            .addAction(Notification.Action.Builder(null, "Unpin", intent).build())
            .build()

        val report = LiveUpdateDiagnostics.inspect(context, notification)

        assertEquals(4, report.androidActionCount)
        assertEquals(listOf("Pause", "Stop", "Tip", "Unpin"), report.androidActionTitles)
        assertEquals(2, report.androidTextOnlyActionCount)
        assertEquals(1, report.androidDisabledActionCount)
        assertTrue("missing-delete-intent" in report.advisoryReasons)
        assertTrue("too-many-action-buttons" in report.advisoryReasons)
    }

    @Test
    @Config(sdk = [35])
    fun `inspect accepts delete intent and capped action count without action advisories`() {
        val context = RuntimeEnvironment.getApplication()
        val intent = PendingIntent.getBroadcast(
            context,
            32,
            Intent("test.ACTION"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val deleteIntent = PendingIntent.getBroadcast(
            context,
            33,
            Intent("test.DELETE"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val icon = Icon.createWithResource(context, android.R.drawable.ic_dialog_info)
        val notification = Notification.Builder(context, "test")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Controls")
            .setOngoing(true)
            .setDeleteIntent(deleteIntent)
            .addAction(Notification.Action.Builder(icon, "Pause", intent).build())
            .addAction(Notification.Action.Builder(icon, "Stop", intent).build())
            .addAction(Notification.Action.Builder(null, "Unpin", intent).build())
            .build()

        val report = LiveUpdateDiagnostics.inspect(context, notification)

        assertEquals(3, report.androidActionCount)
        assertTrue("missing-delete-intent" !in report.advisoryReasons)
        assertTrue("too-many-action-buttons" !in report.advisoryReasons)
    }

    @Test
    @Config(sdk = [35])
    fun `inspect does not classify plain progress as ProgressStyle payload`() {
        val context = RuntimeEnvironment.getApplication()
        val notification = NotificationCompat.Builder(context, "test")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Download")
            .setOngoing(true)
            .setProgress(100, 20, false)
            .build()

        val report = LiveUpdateDiagnostics.inspect(context, notification)

        assertEquals(20, report.androidProgress)
        assertEquals(100, report.androidProgressMax)
        assertTrue(report.hasAndroidProgress)
        assertFalse(report.hasAndroidProgressStylePayload)
        assertEquals(LiveUpdateAllowedStyle.STANDARD, report.liveUpdateStyle)
    }

    @Test
    @Config(sdk = [35])
    fun `inspect reports status chip when and chronometer extras`() {
        val context = RuntimeEnvironment.getApplication()
        val whenTime = System.currentTimeMillis() + 180_000L
        val notification = NotificationCompat.Builder(context, "test")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Ride")
            .setOngoing(true)
            .setWhen(whenTime)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .build()

        val report = LiveUpdateDiagnostics.inspect(context, notification)

        assertEquals(whenTime, report.statusChipWhenTimeMillis)
        assertTrue(report.statusChipShowWhen)
        assertTrue(report.statusChipUsesChronometer)
        assertTrue(report.statusChipChronometerCountDown)
        assertTrue(report.hasStatusChip)
        assertFalse(report.statusChipWhenInPast)
        assertFalse(report.statusChipWhenTooSoon)
        assertFalse(report.statusChipCountdownExpired)
        assertTrue(report.statusChipAdvisoryReasons.isEmpty())
    }

    @Test
    @Config(sdk = [35])
    fun `inspect warns when status chip when time is too soon`() {
        val context = RuntimeEnvironment.getApplication()
        val whenTime = System.currentTimeMillis() + 30_000L
        val notification = NotificationCompat.Builder(context, "test")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Ride")
            .setOngoing(true)
            .setWhen(whenTime)
            .setShowWhen(true)
            .build()

        val report = LiveUpdateDiagnostics.inspect(context, notification)

        assertEquals(whenTime, report.statusChipWhenTimeMillis)
        assertFalse(report.statusChipWhenInPast)
        assertTrue(report.statusChipWhenTooSoon)
        assertFalse(report.statusChipCountdownExpired)
        assertTrue("status-chip-when-too-soon" in report.statusChipAdvisoryReasons)
    }

    @Test
    @Config(sdk = [35])
    fun `inspect reports status chip text fit guidance`() {
        val context = RuntimeEnvironment.getApplication()
        val compactNotification = NotificationCompat.Builder(context, "test")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Delivery")
            .setOngoing(true)
            .setShortCriticalText("10 min")
            .build()
        val longNotification = NotificationCompat.Builder(context, "test")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Delivery")
            .setOngoing(true)
            .setShortCriticalText("Arriving soon")
            .build()

        val compact = LiveUpdateDiagnostics.inspect(context, compactNotification)
        val long = LiveUpdateDiagnostics.inspect(context, longNotification)

        assertEquals(6, compact.statusChipTextLength)
        assertTrue(compact.statusChipTextLikelyFullyVisible == true)
        assertTrue(compact.statusChipAdvisoryReasons.isEmpty())
        assertEquals(13, long.statusChipTextLength)
        assertFalse(long.statusChipTextLikelyFullyVisible == true)
        assertTrue("status-chip-text-may-truncate" in long.statusChipAdvisoryReasons)
    }

    @Test
    @Config(sdk = [35])
    fun `inspect warns for expired countdown status chip`() {
        val context = RuntimeEnvironment.getApplication()
        val notification = NotificationCompat.Builder(context, "test")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Timer")
            .setOngoing(true)
            .setWhen(System.currentTimeMillis() - 1_000L)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .build()

        val report = LiveUpdateDiagnostics.inspect(context, notification)

        assertTrue(report.statusChipWhenInPast)
        assertTrue(report.statusChipCountdownExpired)
        assertTrue("status-chip-countdown-expired" in report.statusChipAdvisoryReasons)
    }
}
