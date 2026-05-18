@file:Suppress("DEPRECATION")

package com.nowbar.api.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SamsungOngoingActivityDumpExtrasTest {

    @Test
    fun `dump extras include remote app text views visuals chronometer and pde state`() {
        val context = RuntimeEnvironment.getApplication()
        val icon = Icon.createWithResource(context, android.R.drawable.ic_dialog_info)
        val openIntent = PendingIntent.getActivity(
            context,
            1,
            Intent("test.OPEN"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val nowBarView = RemoteViews(context.packageName, android.R.layout.simple_list_item_1)
        val expandedView = RemoteViews(context.packageName, android.R.layout.simple_list_item_2)
        val chipView = RemoteViews(context.packageName, android.R.layout.test_list_item)
        val base = SystemClock.elapsedRealtime()

        val extras = SamsungOngoingActivityDumpExtras.build(
            remoteApp = SamsungRemoteAppConfig(
                name = "Google Sports",
                icon = icon,
                pendingIntent = openIntent
            ),
            text = SamsungOngoingActivityText(
                title = "Game",
                primaryInfo = "LAL 102",
                secondaryInfo = "Q4 01:12",
                nowBarPrimaryInfo = "LAL leads",
                nowBarSecondaryInfo = "Q4",
                notificationPrimaryInfo = "LAL 102",
                notificationSecondaryInfo = "GSW 99",
                chipExpandedText = "Live score",
                expandedChipText = "NBA",
                moreInfo = "Watch live",
                primaryAction = 2
            ),
            views = SamsungOngoingActivityViews(
                nowBarRemoteView = nowBarView,
                expandedRemoteView = expandedView,
                chipExpandedView = chipView
            ),
            visuals = SamsungOngoingActivityVisuals(
                chipIcon = icon,
                ongoingActivityChipIcon = icon,
                badge = icon,
                cardIcon = icon,
                chipBackgroundColor = 0xFF1565C0.toInt(),
                ongoingActivityChipBackground = 0xFF1565C0.toInt(),
                cardBackground = 0xFFE3F2FD.toInt(),
                actionBackgroundColors = listOf(0xFF111111.toInt(), 0xFF222222.toInt()),
                nowBarExpandableType = 1,
                show = true
            ),
            chronometer = SamsungOngoingActivityChronometerState(
                base = base,
                countdown = true,
                format = "%s",
                speed = 1.0f,
                start = true
            ),
            progress = SamsungOngoingActivityProgress(
                current = 45,
                max = 120,
                color = 0xFF0FCF6E.toInt(),
                segmentIcon = icon,
                segments = listOf(
                    ProgressSegment(startPosition = 0.0f, color = 0xFF111111.toInt(), icon = icon),
                    ProgressSegment(startPosition = 0.5f, color = 0xFF222222.toInt())
                )
            ),
            pde = SamsungPdeState(
                firstShownTimeMs = 10L,
                firstExpandedTimeMs = 20L,
                enqueuedTimeMs = 30L,
                notificationClickedCount = 3,
                notificationActionClickedCount = 4,
                notificationId = 1123,
                notificationPackage = "com.google.android.googlequicksearchbox",
                notificationTag = "sports"
            ),
            substName = "Google Sports"
        )

        assertEquals(
            SamsungOngoingActivityDumpKeys.TEMPLATE_ONGOING_ACTIVITY_STYLE,
            extras.getString(SamsungOngoingActivityDumpKeys.TEMPLATE)
        )
        assertEquals("Game", extras.getCharSequence(SamsungOngoingActivityDumpKeys.TITLE))
        assertEquals("Google Sports", extras.getCharSequence(SamsungOngoingActivityDumpKeys.AOD_REMOTE_APP_NAME))
        assertSame(icon, extras.getParcelable(SamsungOngoingActivityDumpKeys.AOD_REMOTE_APP_ICON))
        assertSame(openIntent, extras.getParcelable(SamsungOngoingActivityDumpKeys.AOD_REMOTE_APP_PENDING_INTENT))
        assertEquals("LAL 102", extras.getCharSequence(SamsungOngoingActivityDumpKeys.PRIMARY_INFO))
        assertEquals("Q4 01:12", extras.getCharSequence(SamsungOngoingActivityDumpKeys.SECONDARY_INFO))
        assertEquals("LAL leads", extras.getCharSequence(SamsungOngoingActivityDumpKeys.NOWBAR_PRIMARY_INFO))
        assertEquals("Q4", extras.getCharSequence(SamsungOngoingActivityDumpKeys.NOWBAR_SECONDARY_INFO))
        assertEquals("LAL 102", extras.getCharSequence(SamsungOngoingActivityDumpKeys.ONGOING_ACTIVITY_PRIMARY_INFO))
        assertEquals("GSW 99", extras.getCharSequence(SamsungOngoingActivityDumpKeys.ONGOING_ACTIVITY_SECONDARY_INFO))
        assertEquals("Live score", extras.getCharSequence(SamsungOngoingActivityDumpKeys.CHIP_EXPANDED_TEXT))
        assertEquals("NBA", extras.getCharSequence(SamsungOngoingActivityDumpKeys.EXPANDED_CHIP_TEXT))
        assertEquals("Watch live", extras.getCharSequence(SamsungOngoingActivityDumpKeys.ONGOING_ACTIVITY_MORE_INFO))
        assertEquals(2, extras.getInt(SamsungOngoingActivityDumpKeys.ONGOING_ACTIVITY_PRIMARY_ACTION))
        assertTrue(extras.getBoolean(SamsungOngoingActivityDumpKeys.SHOW))
        assertTrue(extras.getBoolean(SamsungOngoingActivityDumpKeys.REDUCED_IMAGES))
        assertSame(nowBarView, extras.getParcelable(SamsungOngoingActivityDumpKeys.NOWBAR_REMOTE_VIEW))
        assertSame(expandedView, extras.getParcelable(SamsungOngoingActivityDumpKeys.EXPANDED_REMOTE_VIEW))
        assertSame(chipView, extras.getParcelable(SamsungOngoingActivityDumpKeys.CHIP_EXPANDED_VIEW))
        assertSame(icon, extras.getParcelable(SamsungOngoingActivityDumpKeys.CHIP_ICON))
        assertSame(icon, extras.getParcelable(SamsungOngoingActivityDumpKeys.ONGOING_ACTIVITY_CHIP_ICON))
        assertSame(icon, extras.getParcelable(SamsungOngoingActivityDumpKeys.ONGOING_ACTIVITY_BADGE))
        assertSame(icon, extras.getParcelable(SamsungOngoingActivityDumpKeys.ONGOING_ACTIVITY_CARD_ICON))
        assertEquals(0xFF1565C0.toInt(), extras.getInt(SamsungOngoingActivityDumpKeys.CHIP_BG_COLOR))
        assertEquals(0xFFE3F2FD.toInt(), extras.getInt(SamsungOngoingActivityDumpKeys.ONGOING_ACTIVITY_CARD_BACKGROUND))
        val actionBackgroundColors =
            extras.getIntegerArrayList(SamsungOngoingActivityDumpKeys.ONGOING_ACTIVITY_ACTION_BG_COLORS)
        assertEquals(arrayListOf(0xFF111111.toInt(), 0xFF222222.toInt()), actionBackgroundColors)
        assertEquals(1, extras.getInt(SamsungOngoingActivityDumpKeys.ONGOING_ACTIVITY_NOW_BAR_EXPANDABLE_TYPE))
        assertEquals(base, extras.getLong(SamsungOngoingActivityDumpKeys.CHRONOMETER_BASE))
        assertTrue(extras.getBoolean(SamsungOngoingActivityDumpKeys.CHRONOMETER_COUNTDOWN))
        assertEquals("%s", extras.getCharSequence(SamsungOngoingActivityDumpKeys.CHRONOMETER_FORMAT))
        assertEquals(1.0f, extras.getFloat(SamsungOngoingActivityDumpKeys.CHRONOMETER_SPEED))
        assertTrue(extras.getBoolean(SamsungOngoingActivityDumpKeys.CHRONOMETER_START))
        assertEquals(45, extras.getInt(NowBarExtrasKeys.PROGRESS))
        assertEquals(120, extras.getInt(NowBarExtrasKeys.PROGRESS_MAX))
        assertEquals(0xFF0FCF6E.toInt(), extras.getInt(NowBarExtrasKeys.PROGRESS_COLOR))
        assertSame(icon, extras.getParcelable(NowBarExtrasKeys.PROGRESS_SEGMENT_ICON))
        val progressSegments = extras.getParcelableArray(NowBarExtrasKeys.PROGRESS_SEGMENTS)
        assertEquals(2, progressSegments?.size)
        val firstSegment = progressSegments?.first() as android.os.Bundle
        assertEquals(0.0f, firstSegment.getFloat(NowBarExtrasKeys.PROGRESS_SEGMENT_START))
        assertEquals(0xFF111111.toInt(), firstSegment.getInt(NowBarExtrasKeys.PROGRESS_SEGMENT_COLOR))
        assertSame(icon, firstSegment.getParcelable(NowBarExtrasKeys.PROGRESS_SEGMENT_ICON))
        assertEquals(10L, extras.getLong(SamsungOngoingActivityDumpKeys.PDE_FIRST_SHOWN_TIME_MS))
        assertEquals(20L, extras.getLong(SamsungOngoingActivityDumpKeys.PDE_FIRST_EXPANDED_TIME_MS))
        assertEquals(30L, extras.getLong(SamsungOngoingActivityDumpKeys.PDE_ENQUEUED_TIME_MS))
        assertEquals(3, extras.getInt(SamsungOngoingActivityDumpKeys.PDE_NOTI_CLICKED_COUNT))
        assertEquals(4, extras.getInt(SamsungOngoingActivityDumpKeys.PDE_NOTI_ACTION_CLICKED_COUNT))
        assertEquals(1123, extras.getInt(SamsungOngoingActivityDumpKeys.PDE_NOTI_ID))
        assertEquals(
            "com.google.android.googlequicksearchbox",
            extras.getString(SamsungOngoingActivityDumpKeys.PDE_NOTI_PKG)
        )
        assertEquals("sports", extras.getString(SamsungOngoingActivityDumpKeys.PDE_NOTI_TAG))
    }

    @Test
    fun `group summary builder mirrors Samsung AOD summary topology`() {
        val context = RuntimeEnvironment.getApplication()
        val openIntent = PendingIntent.getActivity(
            context,
            2,
            Intent("test.OPEN_SUMMARY"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val summary = SamsungNowBarGroupSummaryBuilder.build(
            context = context,
            spec = SamsungNowBarGroupSummarySpec(
                channelId = SamsungNowBarGroupSummaryBuilder.GOOGLE_FINANCE_CHANNEL_ID,
                groupKey = SamsungNowBarGroupSummaryBuilder.GOOGLE_FINANCE_GROUP_KEY,
                summaryNotificationId = SamsungNowBarGroupSummaryBuilder.GOOGLE_FINANCE_SUMMARY_ID,
                title = "Google Finance",
                smallIconResId = android.R.drawable.ic_dialog_info,
                remoteApp = SamsungRemoteAppConfig(
                    name = "Google Finance",
                    pendingIntent = openIntent
                ),
                contentIntent = openIntent,
                color = 0xFF4285F4.toInt()
            )
        )
        val child = SamsungNowBarGroupSummaryBuilder.applyChildGroup(
            NotificationCompat.Builder(
                context,
                SamsungNowBarGroupSummaryBuilder.GOOGLE_FINANCE_CHANNEL_ID
            ).setSmallIcon(android.R.drawable.ic_dialog_info),
            SamsungNowBarGroupSummaryBuilder.GOOGLE_FINANCE_GROUP_KEY
        ).build()

        assertTrue(summary.flags and Notification.FLAG_GROUP_SUMMARY != 0)
        assertFalse(child.flags and Notification.FLAG_GROUP_SUMMARY != 0)
        assertEquals(SamsungNowBarGroupSummaryBuilder.GOOGLE_FINANCE_GROUP_KEY, summary.group)
        assertEquals(SamsungNowBarGroupSummaryBuilder.GOOGLE_FINANCE_GROUP_KEY, child.group)
        assertEquals(NotificationCompat.CATEGORY_PROGRESS, summary.category)
        assertEquals(NotificationCompat.PRIORITY_LOW, summary.priority)
        assertSame(openIntent, summary.contentIntent)
        assertEquals(0xFF4285F4.toInt(), summary.color)
        assertTrue(summary.extras.getBoolean(SamsungOngoingActivityDumpKeys.SHOW_WHEN))
        assertTrue(summary.extras.getBoolean(SamsungOngoingActivityDumpKeys.REDUCED_IMAGES))
        assertEquals("Google Finance", summary.extras.getCharSequence(SamsungOngoingActivityDumpKeys.TITLE))
        assertEquals("Google Finance", summary.extras.getCharSequence(SamsungOngoingActivityDumpKeys.SUBST_NAME))
        assertEquals(
            "Google Finance",
            summary.extras.getCharSequence(SamsungOngoingActivityDumpKeys.AOD_REMOTE_APP_NAME)
        )
        assertEquals(
            SamsungOngoingActivityDumpKeys.TEMPLATE_ONGOING_ACTIVITY_STYLE,
            summary.extras.getString(SamsungOngoingActivityDumpKeys.TEMPLATE)
        )

        val summaryEvidence = NowBarNotificationEvidence.inspect(summary)
        assertTrue(summaryEvidence.groupSummary)
        assertTrue(summaryEvidence.nativeOngoingActivityTemplate)
        assertEquals("Google Finance", summaryEvidence.aodRemoteAppName)
        assertEquals(true, summaryEvidence.samsungDumpShow)
        assertEquals(true, summaryEvidence.samsungReducedImages)
        assertFalse(summaryEvidence.likelyNowBarCompatible)
        assertEquals(listOf("not-ongoing", "group-summary"), summaryEvidence.missingCoreHints)
    }
}
