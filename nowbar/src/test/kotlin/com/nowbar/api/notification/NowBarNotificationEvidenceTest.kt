@file:Suppress("DEPRECATION")

package com.nowbar.api.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Bundle
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NowBarNotificationEvidenceTest {

    @Test
    fun `inspector detects Samsung extras progress segments and capsule`() {
        val context = RuntimeEnvironment.getApplication()
        val capsule = RemoteViews(context.packageName, android.R.layout.simple_list_item_1)
        val extras = OngoingExtrasBuilder()
            .setProgress(40, 100)
            .setProgressSegments(
                ProgressSegment(startPosition = 0.0f, color = 0xFF111111.toInt()),
                ProgressSegment(startPosition = 0.5f, color = 0xFF222222.toInt())
            )
            .setCapsuleConfig(
                CapsuleConfig(
                    layout = capsule,
                    bgStartColor = 0xFF000000.toInt(),
                    bgEndColor = 0xFF333333.toInt(),
                    priority = NowBarExtrasKeys.CapsulePriority.NORMAL
                )
            )
            .build()

        val notification = NotificationCompat.Builder(context, "test")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Workout")
            .setOngoing(true)
            .addExtras(extras)
            .build()

        val report = NowBarNotificationEvidence.inspect(notification)

        assertTrue(report.ongoing)
        assertEquals(NowBarExtrasKeys.Style.BOTH, report.samsungStyle)
        assertTrue(report.hasContentTitle)
        assertFalse(report.hasCustomContentView)
        assertFalse(report.colorized)
        assertTrue(report.hasEligibleCoreFields)
        assertTrue(report.samsungNowBarExtrasCount > 0)
        assertTrue(report.hasSamsungProgress)
        assertTrue(report.hasSamsungProgressSegments)
        assertEquals(2, report.progressSegmentCount)
        assertTrue(report.hasCapsule)
        assertTrue(NowBarEvidencePath.SAMSUNG_EXTRAS in report.evidencePaths)
        assertTrue(NowBarEvidencePath.FOLDABLE_CAPSULE in report.evidencePaths)
        assertTrue(report.likelyNowBarCompatible)
        assertTrue(report.missingCoreHints.isEmpty())
    }

    @Test
    fun `inspector exposes structured Samsung Now Bar extras`() {
        val context = RuntimeEnvironment.getApplication()
        val chipIcon = Icon.createWithResource(context, android.R.drawable.ic_dialog_info)
        val nowBarIcon = Icon.createWithResource(context, android.R.drawable.ic_menu_compass)
        val firstIcon = Icon.createWithResource(context, android.R.drawable.ic_media_play)
        val secondIcon = Icon.createWithResource(context, android.R.drawable.ic_media_pause)
        val secondaryInfoIcon = Icon.createWithResource(context, android.R.drawable.ic_menu_recent_history)
        val segmentIcon = Icon.createWithResource(context, android.R.drawable.ic_dialog_map)
        val chronometerView = RemoteViews(context.packageName, android.R.layout.simple_list_item_1)
        val capsuleView = RemoteViews(context.packageName, android.R.layout.simple_list_item_2)
        val subScreenIntent = PendingIntent.getActivity(
            context,
            9,
            Intent("test.SUB_SCREEN"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val remoteAppIntent = PendingIntent.getActivity(
            context,
            11,
            Intent("test.AOD_REMOTE_APP"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val capsuleAction = PendingIntent.getActivity(
            context,
            10,
            Intent("test.CAPSULE"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val extras = OngoingExtrasBuilder()
            .setStyle(NowBarExtrasKeys.Style.NOW_BAR_ONLY)
            .setChipConfig(
                ChipConfig(
                    icon = chipIcon,
                    backgroundColor = 0xFF123456.toInt(),
                    expandedText = "Timer expanded",
                    firstIcon = firstIcon
                )
            )
            .setPrimaryInfo("Timer")
            .setSecondaryInfo("00:42")
            .setNowBarPrimaryInfo("00:42")
            .setNowBarSecondaryInfo("remaining")
            .setActionType(NowBarExtrasKeys.ActionType.ICON_BUTTON)
            .setActionPrimarySet(NowBarExtrasKeys.ActionButtonPosition.RIGHT)
            .setProgress(42, 120)
            .setProgressColor(0xFF00AA00.toInt())
            .setProgressSegmentIcon(segmentIcon)
            .setProgressSegments(
                ProgressSegment(startPosition = 0.0f, color = 0xFF111111.toInt(), icon = segmentIcon),
                ProgressSegment(startPosition = 0.5f, color = 0xFF222222.toInt())
            )
            .setShowSmallIcon(false)
            .setNowBarIcon(nowBarIcon)
            .setSecondIcon(secondIcon)
            .setChronometerConfig(
                ChronometerConfig(
                    remoteView = chronometerView,
                    tag = "timer-chronometer",
                    viewPosition = ChronometerPosition.PRIMARY_INFO,
                    nowBarPosition = ChronometerPosition.SECONDARY_INFO
                )
            )
            .setCapsuleConfig(
                CapsuleConfig(
                    layout = capsuleView,
                    action = capsuleAction,
                    bgStartColor = 0xFF010203.toInt(),
                    bgEndColor = 0xFF040506.toInt(),
                    priority = NowBarExtrasKeys.CapsulePriority.LOW
                )
            )
            .setSubstName("Stopwatch")
            .setNowBarSubScreenIntent(subScreenIntent)
            .setActionBgColor(0xFF333333.toInt())
            .setSecondaryInfoIcon(secondaryInfoIcon)
            .setAodRemoteApp(
                SamsungRemoteAppConfig(
                    name = "Samsung Health",
                    icon = nowBarIcon,
                    pendingIntent = remoteAppIntent
                )
            )
            .build()

        val notification = NotificationCompat.Builder(context, "test")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Timer")
            .setOngoing(true)
            .addExtras(extras)
            .build()

        val report = NowBarNotificationEvidence.inspect(notification)
        val state = assertNotNull(report.samsungNowBar)

        assertEquals(NowBarExtrasKeys.Style.NOW_BAR_ONLY, state.style)
        assertEquals("Timer", state.text?.primaryInfo)
        assertEquals("00:42", state.text?.secondaryInfo)
        assertEquals("00:42", state.text?.nowBarPrimaryInfo)
        assertEquals("remaining", state.text?.nowBarSecondaryInfo)
        assertEquals("Timer expanded", state.text?.chipExpandedText)
        assertEquals("Stopwatch", state.text?.substName)
        assertEquals(0xFF123456.toInt(), state.visuals?.chipBackgroundColor)
        assertEquals(chipIcon, state.visuals?.chipIcon)
        assertEquals(false, state.visuals?.showSmallIcon)
        assertEquals(nowBarIcon, state.visuals?.nowBarIcon)
        assertEquals(0xFF333333.toInt(), state.visuals?.actionBackgroundColor)
        assertEquals(firstIcon, state.visuals?.firstIcon)
        assertEquals(secondIcon, state.visuals?.secondIcon)
        assertEquals(secondaryInfoIcon, state.visuals?.secondaryInfoIcon)
        assertEquals(NowBarExtrasKeys.ActionType.ICON_BUTTON, state.action?.actionType)
        assertEquals(NowBarExtrasKeys.ActionButtonPosition.RIGHT, state.action?.actionPrimarySet)
        assertEquals(subScreenIntent, state.action?.subScreenIntent)
        assertEquals(42, state.progress?.current)
        assertEquals(120, state.progress?.max)
        assertEquals(0xFF00AA00.toInt(), state.progress?.color)
        assertEquals(segmentIcon, state.progress?.topLevelSegmentIcon)
        assertEquals(2, state.progress?.segments?.size)
        val firstSegment = assertNotNull(state.progress?.segments?.firstOrNull())
        assertEquals(0.0f, firstSegment.startPosition)
        assertEquals(0xFF111111.toInt(), firstSegment.color)
        assertEquals(segmentIcon, firstSegment.icon)
        assertEquals(chronometerView, state.chronometer?.remoteView)
        assertEquals("timer-chronometer", state.chronometer?.tag)
        assertEquals(ChronometerPosition.PRIMARY_INFO, state.chronometer?.viewPosition)
        assertEquals(ChronometerPosition.SECONDARY_INFO, state.chronometer?.nowBarPosition)
        assertEquals(true, state.capsule?.enabled)
        assertEquals(capsuleView, state.capsule?.layout)
        assertEquals(capsuleAction, state.capsule?.action)
        assertEquals(0xFF010203.toInt(), state.capsule?.bgStartColor)
        assertEquals(0xFF040506.toInt(), state.capsule?.bgEndColor)
        assertEquals(NowBarExtrasKeys.CapsulePriority.LOW, state.capsule?.priority)
        assertEquals("Samsung Health", state.remoteApp?.name)
        assertEquals(nowBarIcon, state.remoteApp?.icon)
        assertEquals(remoteAppIntent, state.remoteApp?.pendingIntent)
        assertTrue(report.hasAodRemoteApp)
        assertEquals("Samsung Health", report.aodRemoteAppName)
        assertEquals("Samsung Health", report.aodRemoteApp?.name)
        assertEquals(nowBarIcon, report.aodRemoteApp?.icon)
        assertEquals(remoteAppIntent, report.aodRemoteApp?.pendingIntent)
        assertTrue(report.hasAodRemoteAppIcon)
        assertTrue(report.hasAodRemoteAppPendingIntent)
        assertTrue(NowBarEvidencePath.SAMSUNG_AOD_REMOTE_APP in report.evidencePaths)
    }

    @Test
    fun `inspector detects dump style remote app and native template`() {
        val context = RuntimeEnvironment.getApplication()
        val openIntent = PendingIntent.getActivity(
            context,
            8,
            Intent("test.OPEN"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val nowBarView = RemoteViews(context.packageName, android.R.layout.simple_list_item_1)
        val expandedView = RemoteViews(context.packageName, android.R.layout.simple_list_item_2)
        val chipExpandedView = RemoteViews(context.packageName, android.R.layout.test_list_item)
        val customExpandedCardView = RemoteViews(context.packageName, android.R.layout.simple_expandable_list_item_1)
        val expandedChipView = RemoteViews(context.packageName, android.R.layout.simple_selectable_list_item)
        val expandedNowBarView = RemoteViews(context.packageName, android.R.layout.simple_spinner_item)
        val centerView = RemoteViews(context.packageName, android.R.layout.simple_list_item_checked)
        val icon = Icon.createWithResource(context, android.R.drawable.ic_dialog_info)
        val chronometerBase = SystemClock.elapsedRealtime()
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
                chipExpandedView = chipExpandedView,
                customExpandedCardView = customExpandedCardView,
                expandedChipView = expandedChipView,
                expandedNowBarView = expandedNowBarView,
                customCardViewCenterUi = centerView
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
                base = chronometerBase,
                countdown = true,
                format = "%s",
                speed = 1.0f,
                start = true
            ),
            progress = SamsungOngoingActivityProgress(
                current = 75,
                max = 100,
                color = 0xFF0FCF6E.toInt(),
                segmentIcon = icon,
                segments = listOf(
                    ProgressSegment(startPosition = 0.0f, color = 0xFF111111.toInt(), icon = icon),
                    ProgressSegment(startPosition = 0.6f, color = 0xFF222222.toInt())
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
            )
        )

        val notification = NotificationCompat.Builder(context, "test")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Game")
            .setOngoing(true)
            .addExtras(extras)
            .build()

        val report = NowBarNotificationEvidence.inspect(notification)

        assertTrue(report.nativeOngoingActivityTemplate)
        assertTrue(report.hasAodRemoteApp)
        assertEquals("Google Sports", report.aodRemoteAppName)
        assertEquals("Google Sports", report.aodRemoteApp?.name)
        assertEquals(icon, report.aodRemoteApp?.icon)
        assertEquals(openIntent, report.aodRemoteApp?.pendingIntent)
        assertTrue(report.hasAodRemoteAppIcon)
        assertTrue(report.hasAodRemoteAppPendingIntent)
        assertEquals(true, report.samsungDumpShow)
        assertEquals(true, report.samsungReducedImages)
        assertEquals(11, report.samsungTextExtrasCount)
        assertEquals("Game", report.samsungText?.title)
        assertEquals("LAL 102", report.samsungText?.primaryInfo)
        assertEquals("Q4 01:12", report.samsungText?.secondaryInfo)
        assertEquals("LAL leads", report.samsungText?.nowBarPrimaryInfo)
        assertEquals("Q4", report.samsungText?.nowBarSecondaryInfo)
        assertEquals("LAL 102", report.samsungText?.notificationPrimaryInfo)
        assertEquals("GSW 99", report.samsungText?.notificationSecondaryInfo)
        assertEquals("Live score", report.samsungText?.chipExpandedText)
        assertEquals("NBA", report.samsungText?.expandedChipText)
        assertEquals("Watch live", report.samsungText?.moreInfo)
        assertEquals(2, report.samsungText?.primaryAction)
        assertEquals(2, report.samsungPrimaryAction)
        assertEquals(1, report.samsungNowBarExpandableType)
        assertEquals(2, report.samsungActionBackgroundColorCount)
        assertEquals(10, report.samsungVisualExtrasCount)
        assertEquals(icon, report.samsungVisuals?.chipIcon)
        assertEquals(icon, report.samsungVisuals?.ongoingActivityChipIcon)
        assertEquals(icon, report.samsungVisuals?.badge)
        assertEquals(icon, report.samsungVisuals?.cardIcon)
        assertEquals(0xFF1565C0.toInt(), report.samsungVisuals?.chipBackgroundColor)
        assertEquals(0xFF1565C0.toInt(), report.samsungVisuals?.ongoingActivityChipBackground)
        assertEquals(0xFFE3F2FD.toInt(), report.samsungVisuals?.cardBackground)
        assertEquals(listOf(0xFF111111.toInt(), 0xFF222222.toInt()), report.samsungVisuals?.actionBackgroundColors)
        assertEquals(1, report.samsungVisuals?.nowBarExpandableType)
        assertEquals(true, report.samsungVisuals?.show)
        assertTrue(report.hasOngoingActivityChipIcon)
        assertTrue(report.hasOngoingActivityBadge)
        assertTrue(report.hasOngoingActivityCardIcon)
        assertEquals(5, report.samsungChronometerExtrasCount)
        assertEquals(chronometerBase, report.samsungChronometerState?.base)
        assertEquals(true, report.samsungChronometerState?.countdown)
        assertEquals("%s", report.samsungChronometerState?.format)
        assertEquals(1.0f, report.samsungChronometerState?.speed)
        assertEquals(true, report.samsungChronometerState?.start)
        assertEquals(75, report.samsungNowBar?.progress?.current)
        assertEquals(100, report.samsungNowBar?.progress?.max)
        assertEquals(0xFF0FCF6E.toInt(), report.samsungNowBar?.progress?.color)
        assertEquals(icon, report.samsungNowBar?.progress?.topLevelSegmentIcon)
        assertEquals(2, report.samsungNowBar?.progress?.segments?.size)
        assertTrue(report.hasSamsungProgress)
        assertTrue(report.hasSamsungProgressSegments)
        assertTrue(report.hasSamsungRemoteViews)
        assertEquals(7, report.samsungRemoteViewCount)
        assertEquals(nowBarView, report.samsungViews?.nowBarRemoteView)
        assertEquals(expandedView, report.samsungViews?.expandedRemoteView)
        assertEquals(chipExpandedView, report.samsungViews?.chipExpandedView)
        assertEquals(customExpandedCardView, report.samsungViews?.customExpandedCardView)
        assertEquals(expandedChipView, report.samsungViews?.expandedChipView)
        assertEquals(expandedNowBarView, report.samsungViews?.expandedNowBarView)
        assertEquals(centerView, report.samsungViews?.customCardViewCenterUi)
        assertTrue(report.hasNowBarRemoteView)
        assertTrue(report.hasExpandedRemoteView)
        assertTrue(report.hasChipExpandedView)
        assertTrue(report.hasCustomExpandedCardView)
        assertTrue(report.hasExpandedChipView)
        assertTrue(report.hasExpandedNowBarView)
        assertTrue(report.hasCustomCardViewCenterUi)
        assertEquals(8, report.samsungPdeExtrasCount)
        assertEquals(1123, report.pdeState?.notificationId)
        assertEquals("com.google.android.googlequicksearchbox", report.pdeState?.notificationPackage)
        assertEquals("sports", report.pdeState?.notificationTag)
        assertEquals(10L, report.pdeState?.firstShownTimeMs)
        assertEquals(20L, report.pdeState?.firstExpandedTimeMs)
        assertEquals(30L, report.pdeState?.enqueuedTimeMs)
        assertEquals(3, report.pdeState?.notificationClickedCount)
        assertEquals(4, report.pdeState?.notificationActionClickedCount)
        assertTrue(report.samsungDumpExtrasCount > 0)
        assertTrue(NowBarEvidencePath.SAMSUNG_DUMP_EXTRAS in report.evidencePaths)
        assertTrue(NowBarEvidencePath.SAMSUNG_NATIVE_TEMPLATE in report.evidencePaths)
        assertTrue(NowBarEvidencePath.SAMSUNG_REMOTE_VIEWS in report.evidencePaths)
        assertTrue(NowBarEvidencePath.SAMSUNG_AOD_REMOTE_APP in report.evidencePaths)
        assertTrue(NowBarEvidencePath.SAMSUNG_TEXT_STATE in report.evidencePaths)
        assertTrue(NowBarEvidencePath.SAMSUNG_VISUAL_STATE in report.evidencePaths)
        assertTrue(NowBarEvidencePath.SAMSUNG_CHRONOMETER_STATE in report.evidencePaths)
        assertTrue(NowBarEvidencePath.SAMSUNG_PDE_STATE in report.evidencePaths)
        assertTrue(report.likelyNowBarCompatible)
    }

    @Test
    fun `inspector detects Android Live Update promoted metric template`() {
        val context = RuntimeEnvironment.getApplication()
        val notification = NotificationCompat.Builder(context, "test")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Metric")
            .setOngoing(true)
            .setRequestPromotedOngoing(true)
            .addExtras(Bundle().apply {
                putString(Notification.EXTRA_TEMPLATE, "androidx.core.app.NotificationCompat\$MetricStyle")
            })
            .build()

        val report = NowBarNotificationEvidence.inspect(notification)

        assertTrue(report.requestPromotedOngoing)
        assertTrue(report.metricStyleTemplate)
        assertTrue(report.hasAndroidLiveUpdateEvidence)
        assertTrue(NowBarEvidencePath.ANDROID_PROMOTED_ONGOING in report.evidencePaths)
        assertTrue(NowBarEvidencePath.ANDROID_METRIC_STYLE in report.evidencePaths)
        assertTrue(report.likelyNowBarCompatible)
    }

    @Test
    fun `inspector reports Android progress extras`() {
        val context = RuntimeEnvironment.getApplication()
        val icon = IconCompat.createWithResource(context, android.R.drawable.ic_menu_compass)
        val notification = NotificationCompat.Builder(context, "test")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Order")
            .setOngoing(true)
            .setRequestPromotedOngoing(true)
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

        val report = NowBarNotificationEvidence.inspect(notification)

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
        assertTrue(NowBarEvidencePath.ANDROID_PROGRESS in report.evidencePaths)
        assertTrue(NowBarEvidencePath.ANDROID_PROGRESS_STYLE in report.evidencePaths)
    }

    @Test
    fun `inspector does not infer ProgressStyle from plain progress extras`() {
        val context = RuntimeEnvironment.getApplication()
        val notification = NotificationCompat.Builder(context, "test")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Download")
            .setOngoing(true)
            .setProgress(100, 20, false)
            .build()

        val report = NowBarNotificationEvidence.inspect(notification)

        assertEquals(20, report.androidProgress)
        assertEquals(100, report.androidProgressMax)
        assertFalse(report.hasAndroidProgressStylePayload)
        assertTrue(NowBarEvidencePath.ANDROID_PROGRESS in report.evidencePaths)
        assertFalse(NowBarEvidencePath.ANDROID_PROGRESS_STYLE in report.evidencePaths)
    }

    @Test
    fun `inspector detects Android CallStyle template`() {
        val context = RuntimeEnvironment.getApplication()
        val notification = NotificationCompat.Builder(context, "test")
            .setSmallIcon(android.R.drawable.sym_call_incoming)
            .setContentTitle("Alice")
            .setOngoing(true)
            .addExtras(Bundle().apply {
                putString(Notification.EXTRA_TEMPLATE, "androidx.core.app.NotificationCompat\$CallStyle")
                putInt(NowBarNotificationEvidence.EXTRA_CALL_TYPE, 3)
                putBoolean(NowBarNotificationEvidence.EXTRA_CALL_IS_VIDEO, true)
                putBoolean(NowBarNotificationEvidence.EXTRA_CALL_PERSON, true)
                putBoolean(NowBarNotificationEvidence.EXTRA_ANSWER_INTENT, true)
                putBoolean(NowBarNotificationEvidence.EXTRA_DECLINE_INTENT, true)
                putBoolean(NowBarNotificationEvidence.EXTRA_HANG_UP_INTENT, true)
                putInt(NowBarNotificationEvidence.EXTRA_ANSWER_COLOR, 0xFF008577.toInt())
                putInt(NowBarNotificationEvidence.EXTRA_DECLINE_COLOR, 0xFFB00020.toInt())
                putCharSequence(NowBarNotificationEvidence.EXTRA_VERIFICATION_TEXT, "Verified caller")
                putBoolean(NowBarNotificationEvidence.EXTRA_VERIFICATION_ICON, true)
            })
            .build()

        val report = NowBarNotificationEvidence.inspect(notification)

        assertTrue(report.callStyleTemplate)
        assertEquals(3, report.callStyleType)
        assertEquals(true, report.callStyleIsVideo)
        assertTrue(report.callStyleHasPerson)
        assertTrue(report.callStyleHasAnswerIntent)
        assertTrue(report.callStyleHasDeclineIntent)
        assertTrue(report.callStyleHasHangUpIntent)
        assertEquals(0xFF008577.toInt(), report.callStyleAnswerColor)
        assertEquals(0xFFB00020.toInt(), report.callStyleDeclineColor)
        assertEquals("Verified caller", report.verificationText)
        assertTrue(report.callStyleHasVerificationIcon)
        assertTrue(report.hasAndroidLiveUpdateEvidence)
        assertTrue(NowBarEvidencePath.ANDROID_CALL_STYLE in report.evidencePaths)
    }

    @Test
    fun `inspector detects Android BigTextStyle template`() {
        val context = RuntimeEnvironment.getApplication()
        val notification = NotificationCompat.Builder(context, "test")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Incident")
            .setContentText("Rerouting")
            .setOngoing(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText("Long live update details"))
            .build()

        val report = NowBarNotificationEvidence.inspect(notification)

        assertTrue(report.bigTextStyleTemplate)
        assertTrue(report.hasAndroidLiveUpdateEvidence)
        assertTrue(NowBarEvidencePath.ANDROID_BIG_TEXT_STYLE in report.evidencePaths)
    }

    @Test
    fun `inspector reports notification subtext for metric style header context`() {
        val context = RuntimeEnvironment.getApplication()
        val notification = NotificationCompat.Builder(context, "test")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Workout")
            .setSubText("Running")
            .setOngoing(true)
            .build()

        val report = NowBarNotificationEvidence.inspect(notification)

        assertEquals("Running", report.subText)
        assertTrue(NowBarEvidencePath.ANDROID_SUB_TEXT in report.evidencePaths)
    }

    @Test
    fun `inspector reports short critical text status chip evidence`() {
        val context = RuntimeEnvironment.getApplication()
        val notification = NotificationCompat.Builder(context, "test")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Delivery")
            .setOngoing(true)
            .setRequestPromotedOngoing(true)
            .addExtras(Bundle().apply {
                putCharSequence(LiveUpdateDiagnostics.EXTRA_SHORT_CRITICAL_TEXT, "10:08")
            })
            .build()

        val report = NowBarNotificationEvidence.inspect(notification)

        assertEquals("10:08", report.shortCriticalText)
        assertTrue(NowBarEvidencePath.ANDROID_SHORT_CRITICAL_TEXT in report.evidencePaths)
        assertTrue(report.hasAndroidLiveUpdateEvidence)
    }

    @Test
    fun `inspector reports when chronometer status chip evidence`() {
        val context = RuntimeEnvironment.getApplication()
        val whenTime = System.currentTimeMillis() + 120_000L
        val notification = NotificationCompat.Builder(context, "test")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Ride")
            .setOngoing(true)
            .setWhen(whenTime)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .build()

        val report = NowBarNotificationEvidence.inspect(notification)

        assertEquals(whenTime, report.statusChipWhenTimeMillis)
        assertTrue(report.statusChipShowWhen)
        assertTrue(report.statusChipUsesChronometer)
        assertTrue(report.statusChipChronometerCountDown)
        assertTrue(report.hasStatusChip)
        assertTrue(NowBarEvidencePath.ANDROID_STATUS_CHIP in report.evidencePaths)
        assertFalse(report.hasAndroidLiveUpdateEvidence)
    }

    @Test
    fun `inspector reports action button evidence`() {
        val context = RuntimeEnvironment.getApplication()
        val openIntent = PendingIntent.getBroadcast(
            context,
            91,
            Intent("test.OPEN"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(context, "test")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Actions")
            .setOngoing(true)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, android.R.drawable.ic_menu_compass),
                    "Open",
                    openIntent
                ).build()
            )
            .addAction(Notification.Action.Builder(null, "Tip", null).build())
            .build()

        val report = NowBarNotificationEvidence.inspect(notification)

        assertEquals(2, report.androidActionCount)
        assertEquals(listOf("Open", "Tip"), report.androidActionTitles)
        assertEquals(1, report.androidTextOnlyActionCount)
        assertEquals(1, report.androidDisabledActionCount)
        assertTrue(report.androidActions[0].hasIcon)
        assertTrue(report.androidActions[0].hasIntent)
        assertTrue(report.androidActions[1].textOnly)
        assertTrue(report.androidActions[1].disabled)
        assertTrue(NowBarEvidencePath.ANDROID_ACTION_BUTTONS in report.evidencePaths)
    }

    @Test
    fun `inspector reports missing hints on ordinary notification`() {
        val context = RuntimeEnvironment.getApplication()
        val notification = NotificationCompat.Builder(context, "test")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Plain")
            .build()

        val report = NowBarNotificationEvidence.inspect(notification)

        assertFalse(report.likelyNowBarCompatible)
        assertEquals(
            listOf("not-ongoing", "no-nowbar-or-live-update-hints"),
            report.missingCoreHints
        )
    }

    @Test
    fun `inspector reports official Live Updates core blockers`() {
        val context = RuntimeEnvironment.getApplication()
        val customView = RemoteViews(context.packageName, android.R.layout.simple_list_item_1)
        val extras = OngoingExtrasBuilder()
            .setPrimaryInfo("Ride")
            .build()
        val notification = NotificationCompat.Builder(context, "test")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setColorized(true)
            .setCustomContentView(customView)
            .addExtras(extras)
            .build()

        val report = NowBarNotificationEvidence.inspect(notification)

        assertFalse(report.hasContentTitle)
        assertTrue(report.hasCustomContentView)
        assertTrue(report.colorized)
        assertFalse(report.hasEligibleCoreFields)
        assertFalse(report.likelyNowBarCompatible)
        assertEquals(
            listOf("missing-content-title", "custom-content-view", "colorized"),
            report.missingCoreHints
        )
    }
}
