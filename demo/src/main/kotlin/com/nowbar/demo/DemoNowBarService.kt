package com.nowbar.demo

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Bundle
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.nowbar.api.NowBarConfig
import com.nowbar.api.NowBarDiagnostics
import com.nowbar.api.cards.CallCard
import com.nowbar.api.cards.CustomCard
import com.nowbar.api.cards.DeliveryCard
import com.nowbar.api.cards.DeliveryStatus
import com.nowbar.api.cards.MediaCard
import com.nowbar.api.cards.MetricCard
import com.nowbar.api.cards.NavigationCard
import com.nowbar.api.cards.NowBarCard
import com.nowbar.api.cards.TimerCard
import com.nowbar.api.cards.WorkoutCard
import com.nowbar.api.cards.WorkoutType
import com.nowbar.api.fallback.FallbackStrategy
import com.nowbar.api.notification.ActionConfig
import com.nowbar.api.notification.ActionSemantic
import com.nowbar.api.notification.LiveUpdateDiagnostics
import com.nowbar.api.notification.LiveUpdateMetric
import com.nowbar.api.notification.LiveUpdateMetricStyle
import com.nowbar.api.notification.LiveUpdateMetricTimeFormat
import com.nowbar.api.notification.LiveUpdateMetricValue
import com.nowbar.api.notification.LiveUpdateSemanticStyle
import com.nowbar.api.notification.NowBarNotificationEvidence
import com.nowbar.api.notification.ProgressSegment
import com.nowbar.api.notification.SamsungNowBarGroupSummaryBuilder
import com.nowbar.api.notification.SamsungNowBarGroupSummarySpec
import com.nowbar.api.notification.SamsungOngoingActivityChronometerState
import com.nowbar.api.notification.SamsungOngoingActivityDumpExtras
import com.nowbar.api.notification.SamsungOngoingActivityProgress
import com.nowbar.api.notification.SamsungOngoingActivityStyleBuilder
import com.nowbar.api.notification.SamsungOngoingActivityText
import com.nowbar.api.notification.SamsungOngoingActivityViews
import com.nowbar.api.notification.SamsungOngoingActivityVisuals
import com.nowbar.api.notification.SamsungRemoteAppConfig
import com.nowbar.api.notification.StylePoint
import com.nowbar.api.notification.StyleSegment
import com.nowbar.api.service.NowBarForegroundService
import java.time.Instant
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class DemoNowBarService : NowBarForegroundService() {

    private val config: NowBarConfig
        get() = NowBarConfig(
            channelId = CHANNEL_ID,
            channelName = "NowBar SDK Demo",
            channelDescription = "Manual SDK test cards",
            channelImportance = NotificationManager.IMPORTANCE_DEFAULT,
            notificationId = NOTIFICATION_ID,
            aodRemoteApp = SamsungRemoteAppConfig(
                name = "NowBar SDK Demo",
                icon = Icon.createWithResource(this, R.drawable.ic_stat_nowbar),
                pendingIntent = openAppIntent()
            )
        )

    private var currentCard: NowBarCard? = null

    override fun onCreate() {
        super.onCreate()
        createNowBarChannel(config)
        android.util.Log.i(TAG, "Capability report=${NowBarDiagnostics.inspect(this).toDisplayString()}")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action ?: ACTION_SHOW_CUSTOM_ACTIONS) {
            ACTION_SHOW_TIMER -> showCard(buildTimerCard())
            ACTION_SHOW_WORKOUT -> showCard(buildWorkoutCard())
            ACTION_SHOW_NAVIGATION -> showCard(buildNavigationCard())
            ACTION_SHOW_DELIVERY -> showCard(buildDeliveryCard())
            ACTION_SHOW_METRICS -> showCard(buildMetricCard())
            ACTION_SHOW_MEDIA -> showCard(buildMediaCard())
            ACTION_SHOW_CALL -> showCard(buildCallCard(incoming = true))
            ACTION_SHOW_CALL_SCREENING -> showCard(buildScreeningCallCard())
            ACTION_SHOW_BIG_TEXT -> showCard(buildBigTextCard())
            ACTION_SHOW_CUSTOM_ACTIONS -> showCard(buildCustomActionsCard("Ready", 40))
            ACTION_SHOW_DUMP -> showDumpStyleNotification()
            ACTION_SHOW_NATIVE_STYLE -> showNativeStyleNotification()
            ACTION_PAUSE -> showCard(buildCustomActionsCard("Paused", 55))
            ACTION_RESUME -> showCard(buildCustomActionsCard("Running", 60))
            ACTION_NEXT -> showCard(buildCustomActionsCard("Next step", 75))
            ACTION_UNPIN -> unpinCurrent()
            ACTION_DISMISS -> dismissCurrent()
            ACTION_STOP -> stopCurrent()
        }

        return START_NOT_STICKY
    }

    override fun onCreateCard(): NowBarCard = currentCard ?: buildCustomActionsCard("Ready", 0)

    override fun onUpdateCard(): NowBarCard = currentCard ?: buildCustomActionsCard("Ready", 0)

    private fun showCard(card: NowBarCard) {
        currentCard = card
        startNowBar(config, card)
        inspectPromotion(card)
    }

    private fun buildTimerCard(): TimerCard =
        TimerCard(
            title = "Timer",
            icon = iconCompat(),
            totalDuration = 5.minutes,
            remainingDuration = 3.minutes + 20.seconds,
            isCountDown = true,
            accentColor = ACCENT,
            tapAction = openAppIntent(),
            deleteIntent = serviceIntent(ACTION_DISMISS, 24)
        )

    private fun buildWorkoutCard(): WorkoutCard =
        WorkoutCard(
            title = "Running",
            icon = iconCompat(),
            activityType = WorkoutType.RUNNING,
            elapsed = 12.minutes + 34.seconds,
            distance = 2.43,
            heartRate = 142,
            calories = 128,
            progress = 44,
            accentColor = ACCENT,
            chipText = "2.4 km",
            tapAction = openAppIntent(),
            deleteIntent = serviceIntent(ACTION_DISMISS, 25)
        )

    private fun buildNavigationCard(): NavigationCard =
        NavigationCard.Builder.create(
            title = "Navigation",
            icon = iconCompat(),
            nextDirection = "Turn right onto Main St",
            distanceToTurn = "300 m"
        )
            .eta("15:42")
            .turnIcon(iconCompat())
            .accentColor(0xFF4285F4.toInt())
            .chipText("300 m")
            .tapAction(openAppIntent())
            .deleteIntent(serviceIntent(ACTION_DISMISS, 26))
            .build()

    private fun buildDeliveryCard(): DeliveryCard =
        DeliveryCard.Builder.create(
            title = "Delivery",
            icon = iconCompat(),
            merchantName = "NowBar Bakery",
            status = DeliveryStatus.EN_ROUTE
        )
            .eta("10 min")
            .destination("Main St")
            .courierName("Alex")
            .trackerIcon(iconCompat())
            .largeIcon(iconCompat())
            .chipWhenTimeMillis(System.currentTimeMillis() + 10.minutes.inWholeMilliseconds)
            .accentColor(0xFF00AEEF.toInt())
            .tapAction(openAppIntent())
            .deleteIntent(serviceIntent(ACTION_DISMISS, 15))
            .action(ActionConfig("track", "Track", R.drawable.ic_stat_nowbar, openAppIntent(), ActionSemantic.CUSTOM))
            .action(ActionConfig.disabled("tip", "Tip"))
            .action(ActionConfig("stop", "Stop", R.drawable.ic_stat_nowbar, serviceIntent(ACTION_STOP, 14), ActionSemantic.STOP))
            .build()

    private fun buildMetricCard(): MetricCard =
        MetricCard.Builder.create(
            title = "MetricStyle",
            icon = iconCompat(),
            metricStyle = LiveUpdateMetricStyle(
                metrics = listOf(
                    LiveUpdateMetric(
                        label = "ETA",
                        value = LiveUpdateMetricValue.Timer(
                            endTime = Instant.ofEpochMilli(System.currentTimeMillis() + 9.minutes.inWholeMilliseconds),
                            format = LiveUpdateMetricTimeFormat.CHRONOMETER
                        ),
                        semanticStyle = LiveUpdateSemanticStyle.INFO
                    ),
                    LiveUpdateMetric(
                        label = "Dist",
                        value = LiveUpdateMetricValue.FixedFloat(
                            value = 2.4f,
                            unit = "km",
                            maxFractionDigits = 1
                        ),
                        semanticStyle = LiveUpdateSemanticStyle.SAFE
                    ),
                    LiveUpdateMetric(
                        label = "Stops",
                        value = LiveUpdateMetricValue.FixedInt(2),
                        semanticStyle = LiveUpdateSemanticStyle.CAUTION
                    )
                )
            )
        )
            .primaryText("Delivery metrics")
            .secondaryText("Android 17 MetricStyle template")
            .shortCriticalText("2.4 km")
            .semanticStyle(LiveUpdateSemanticStyle.INFO)
            .accentColor(0xFF00AEEF.toInt())
            .tapAction(openAppIntent())
            .deleteIntent(serviceIntent(ACTION_DISMISS, 17))
            .action(ActionConfig("open", "Open", R.drawable.ic_stat_nowbar, openAppIntent(), ActionSemantic.CUSTOM))
            .build()

    private fun buildMediaCard(): MediaCard =
        MediaCard.Builder.create("NowBar Mix", iconCompat())
            .artist("SDK Demo")
            .album("Samsung actions")
            .isPlaying(true)
            .playAction(serviceIntent(ACTION_PAUSE, 1))
            .skipAction(serviceIntent(ACTION_NEXT, 2))
            .accentColor(0xFF7C4DFF.toInt())
            .tapAction(openAppIntent())
            .deleteIntent(serviceIntent(ACTION_DISMISS, 27))
            .build()

    private fun buildCallCard(incoming: Boolean): CallCard =
        CallCard.Builder.create("Call", iconCompat(), "NowBar Tester")
            .isIncoming(incoming)
            .callerNumber("+15551234567")
            .largeIcon(iconCompat())
            .isVideo(incoming)
            .verificationText("Verified caller")
            .answerAction(serviceIntent(ACTION_RESUME, 3))
            .declineAction(serviceIntent(ACTION_STOP, 4))
            .hangupAction(serviceIntent(ACTION_STOP, 5))
            .accentColor(0xFF2E7D32.toInt())
            .tapAction(openAppIntent())
            .deleteIntent(serviceIntent(ACTION_DISMISS, 28))
            .build()

    private fun buildScreeningCallCard(): CallCard =
        CallCard.Builder.create("Call screening", iconCompat(), "Unknown caller")
            .callerNumber("+15557654321")
            .largeIcon(iconCompat())
            .screeningCall(
                answerAction = serviceIntent(ACTION_RESUME, 18),
                hangupAction = serviceIntent(ACTION_STOP, 19)
            )
            .verificationText("Screening caller")
            .accentColor(0xFF1565C0.toInt())
            .tapAction(openAppIntent())
            .deleteIntent(serviceIntent(ACTION_DISMISS, 29))
            .build()

    private fun buildBigTextCard(): CustomCard =
        CustomCard.Builder.create("BigTextStyle", iconCompat(), "Weather delay")
            .secondaryText("Rerouting delivery")
            .bigText(
                "The courier is rerouting because of a temporary road closure. " +
                    "The delivery remains active and ETA will continue updating in the Now Bar."
            )
            .shortCriticalText("Delay")
            .accentColor(0xFF5E35B1.toInt())
            .tapAction(openAppIntent())
            .deleteIntent(serviceIntent(ACTION_DISMISS, 20))
            .action(ActionConfig("open", "Open", R.drawable.ic_stat_nowbar, openAppIntent(), ActionSemantic.CUSTOM))
            .build()

    private fun buildCustomActionsCard(status: String, progress: Int): CustomCard =
        CustomCard.Builder.create("Controls", iconCompat(), "Demo controls")
            .secondaryText(status)
            .nowBarText(status)
            .progressValue(progress)
            .progressSegments(
                listOf(
                    StyleSegment(length = 20, color = 0xFF6162E9.toInt(), id = 1),
                    StyleSegment(length = 30, color = 0xFF00AEEF.toInt(), id = 2),
                    StyleSegment(length = 50, color = 0xFFE0E0E0.toInt(), id = 3)
                )
            )
            .progressPoints(
                listOf(
                    StylePoint(position = 20, color = 0xFFFFFFFF.toInt(), id = 1),
                    StylePoint(position = 50, color = 0xFFFFFFFF.toInt(), id = 2)
                )
            )
            .progressStyledByProgress()
            .progressTrackerIcon(iconCompat())
            .progressStartIcon(iconCompat())
            .progressEndIcon(iconCompat())
            .chipWhenTimeMillis(System.currentTimeMillis() + 4.minutes.inWholeMilliseconds, countDown = true)
            .customProgressColor(ACCENT)
            .accentColor(ACCENT)
            .tapAction(openAppIntent())
            .deleteIntent(serviceIntent(ACTION_DISMISS, 30))
            .action(ActionConfig("pause", "Pause", R.drawable.ic_stat_nowbar, serviceIntent(ACTION_PAUSE, 10), ActionSemantic.PAUSE))
            .action(ActionConfig("unpin", "Unpin", R.drawable.ic_stat_nowbar, serviceIntent(ACTION_UNPIN, 31), ActionSemantic.UNPIN))
            .action(ActionConfig("stop", "Stop", R.drawable.ic_stat_nowbar, serviceIntent(ACTION_STOP, 13), ActionSemantic.STOP))
            .action(ActionConfig("next", "Next", R.drawable.ic_stat_nowbar, serviceIntent(ACTION_NEXT, 12), ActionSemantic.NEXT))
            .build()

    @SuppressLint("MissingPermission")
    private fun showDumpStyleNotification() {
        val notificationManager = NotificationManagerCompat.from(this)
        val appIcon = Icon.createWithResource(this, R.drawable.ic_stat_nowbar)
        val remoteApp = SamsungRemoteAppConfig(
            name = "NowBar SDK Demo",
            icon = appIcon,
            pendingIntent = openAppIntent()
        )
        val summary = SamsungNowBarGroupSummaryBuilder.build(
            context = this,
            spec = SamsungNowBarGroupSummarySpec(
                channelId = CHANNEL_ID,
                groupKey = SamsungNowBarGroupSummaryBuilder.GOOGLE_SPORTS_GROUP_KEY,
                summaryNotificationId = SamsungNowBarGroupSummaryBuilder.GOOGLE_SPORTS_SUMMARY_ID,
                title = "Samsung dump style",
                smallIconResId = R.drawable.ic_stat_nowbar,
                remoteApp = remoteApp,
                contentIntent = openAppIntent(),
                color = ACCENT
            )
        )
        notificationManager.notify(SamsungNowBarGroupSummaryBuilder.GOOGLE_SPORTS_SUMMARY_ID, summary)

        val nowBarView = RemoteViews(packageName, android.R.layout.simple_list_item_1).apply {
            setTextViewText(android.R.id.text1, "Dump style")
        }
        val expandedView = RemoteViews(packageName, android.R.layout.simple_list_item_2).apply {
            setTextViewText(android.R.id.text1, "Samsung dump style")
            setTextViewText(android.R.id.text2, "Summary plus child RemoteViews")
        }
        val extras = SamsungOngoingActivityDumpExtras.build(
            remoteApp = remoteApp,
            text = SamsungOngoingActivityText(
                title = "Samsung dump style",
                primaryInfo = "Sports/Finance topology",
                secondaryInfo = "Summary plus child",
                nowBarPrimaryInfo = "Dump style",
                nowBarSecondaryInfo = "Child id ${SamsungNowBarGroupSummaryBuilder.GOOGLE_SPORTS_CHILD_ID}",
                chipExpandedText = "NowBar SDK",
                primaryAction = 0
            ),
            views = SamsungOngoingActivityViews(
                nowBarRemoteView = nowBarView,
                expandedRemoteView = expandedView
            ),
            visuals = SamsungOngoingActivityVisuals(
                chipIcon = appIcon,
                ongoingActivityChipIcon = appIcon,
                badge = appIcon,
                cardIcon = appIcon,
                chipBackgroundColor = ACCENT,
                ongoingActivityChipBackground = ACCENT,
                cardBackground = 0xB3FCFCFF.toInt(),
                actionBackgroundColors = listOf(ACCENT, 0xFF2E7D32.toInt()),
                nowBarExpandableType = 0
            ),
            chronometer = SamsungOngoingActivityChronometerState(
                base = SystemClock.elapsedRealtime() - 90_000L,
                countdown = false,
                format = "%s",
                speed = 1.0f,
                start = true
            ),
            progress = SamsungOngoingActivityProgress(
                current = 70,
                max = 100,
                color = ACCENT,
                segmentIcon = appIcon,
                segments = listOf(
                    ProgressSegment(0.0f, ACCENT, appIcon),
                    ProgressSegment(0.7f, 0xFF2E7D32.toInt())
                )
            ),
            substName = "NowBar SDK"
        )
        val child = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_nowbar)
            .setContentTitle("Samsung dump style")
            .setContentText("Summary plus child notification")
            .setContentIntent(openAppIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setDeleteIntent(serviceIntent(ACTION_DISMISS, 16))
            .setColor(ACCENT)
            .setGroup(SamsungNowBarGroupSummaryBuilder.GOOGLE_SPORTS_GROUP_KEY)
            .setGroupSummary(false)
            .addExtras(extras)
            .build()

        startNowBar(child, SamsungNowBarGroupSummaryBuilder.GOOGLE_SPORTS_CHILD_ID)
    }

    private fun showNativeStyleNotification() {
        val appIcon = Icon.createWithResource(this, R.drawable.ic_stat_nowbar)
        val centerView = RemoteViews("android", android.R.layout.simple_list_item_2).apply {
            setTextViewText(android.R.id.text1, "Samsung native style")
            setTextViewText(android.R.id.text2, "Voice Recorder-like hidden style")
        }
        val nativeStyleResult = SamsungOngoingActivityStyleBuilder()
            .chipIcon(appIcon)
            .chipBackgroundColor(ACCENT)
            .cardIcon(appIcon)
            .badgeIcon(appIcon)
            .cardBackgroundColor(0xB3FCFCFF.toInt())
            .primaryInfo("Native style")
            .secondaryInfo("Hidden OngoingActivityStyle")
            .moreInfo("NowBar SDK")
            .customCardViewCenterUi(centerView)
            .action(nativeAction("Pause", ACTION_PAUSE, 21))
            .action(nativeAction("Stop", ACTION_STOP, 22))
            .buildWithReport()
        val nativeStyle = nativeStyleResult.style
        val nativeStyleReport = nativeStyleResult.report

        val builder = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_nowbar)
            .setContentTitle("Samsung native style")
            .setContentText(
                if (nativeStyle == null) {
                    "OngoingActivityStyle unavailable on this runtime"
                } else {
                    "OngoingActivityStyle active"
                }
            )
            .setContentIntent(openAppIntent())
            .setDeleteIntent(serviceIntent(ACTION_DISMISS, 23))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setColor(ACCENT)
            .addExtras(Bundle().apply {
                putBoolean(LiveUpdateDiagnostics.EXTRA_REQUEST_PROMOTED_ONGOING, true)
                putBoolean(EXTRA_NATIVE_STYLE_AVAILABLE, nativeStyle != null)
                putBoolean(EXTRA_NATIVE_STYLE_COMPLETE, nativeStyleReport.complete)
                putString(EXTRA_NATIVE_STYLE_REPORT, nativeStyleReport.toDisplayString())
            })

        requestPromotedOngoing(builder)
        nativeStyle?.let(builder::setStyle)

        val notification = builder.build()
        startNowBar(notification, NOTIFICATION_ID)
        android.util.Log.i(
            TAG,
            "Samsung native style report=$nativeStyleReport liveUpdate=${LiveUpdateDiagnostics.inspect(this, notification)} " +
                "evidence=${NowBarNotificationEvidence.inspect(notification)}"
        )
    }

    private fun nativeAction(
        title: String,
        action: String,
        requestCode: Int
    ): Notification.Action =
        Notification.Action.Builder(
            Icon.createWithResource(this, R.drawable.ic_stat_nowbar),
            title,
            serviceIntent(action, requestCode)
        ).build()

    private fun requestPromotedOngoing(builder: Notification.Builder) {
        runCatching {
            Notification.Builder::class.java
                .getMethod("setRequestPromotedOngoing", Boolean::class.javaPrimitiveType)
                .invoke(builder, true)
        }
    }

    private fun inspectPromotion(card: NowBarCard) {
        val notification = buildNowBarNotification(config, card)
        val report = LiveUpdateDiagnostics.inspect(this, notification)
        val evidence = NowBarNotificationEvidence.inspect(notification)
        android.util.Log.i(
            TAG,
            "LiveUpdate report=$report blockers=${report.blockingReasons} evidence=$evidence"
        )
    }

    private fun dismissCurrent() {
        dismissNowBar()
        NotificationManagerCompat.from(this).cancel(SamsungNowBarGroupSummaryBuilder.GOOGLE_SPORTS_SUMMARY_ID)
    }

    private fun unpinCurrent() {
        val card = currentCard ?: return
        val unpinnedConfig = config.copy(
            fallbackStrategy = FallbackStrategy.STANDARD_NOTIFICATION,
            requestPromotedOngoing = false
        )
        val notification = buildNowBarNotification(unpinnedConfig, card)
        startNowBar(notification, unpinnedConfig.notificationId)
        android.util.Log.i(TAG, "Unpinned Now Bar evidence=${NowBarNotificationEvidence.inspect(notification)}")
    }

    private fun stopCurrent() {
        dismissCurrent()
        stopSelf()
    }

    private fun iconCompat(): IconCompat =
        IconCompat.createWithResource(this, R.drawable.ic_stat_nowbar)

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(this, 100, intent, PENDING_FLAGS)
    }

    private fun serviceIntent(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(this, requestCode, intent(this, action), PENDING_FLAGS)

    companion object {
        const val ACTION_SHOW_TIMER = "com.nowbar.demo.SHOW_TIMER"
        const val ACTION_SHOW_WORKOUT = "com.nowbar.demo.SHOW_WORKOUT"
        const val ACTION_SHOW_NAVIGATION = "com.nowbar.demo.SHOW_NAVIGATION"
        const val ACTION_SHOW_DELIVERY = "com.nowbar.demo.SHOW_DELIVERY"
        const val ACTION_SHOW_METRICS = "com.nowbar.demo.SHOW_METRICS"
        const val ACTION_SHOW_MEDIA = "com.nowbar.demo.SHOW_MEDIA"
        const val ACTION_SHOW_CALL = "com.nowbar.demo.SHOW_CALL"
        const val ACTION_SHOW_CALL_SCREENING = "com.nowbar.demo.SHOW_CALL_SCREENING"
        const val ACTION_SHOW_BIG_TEXT = "com.nowbar.demo.SHOW_BIG_TEXT"
        const val ACTION_SHOW_CUSTOM_ACTIONS = "com.nowbar.demo.SHOW_CUSTOM_ACTIONS"
        const val ACTION_SHOW_DUMP = "com.nowbar.demo.SHOW_DUMP"
        const val ACTION_SHOW_NATIVE_STYLE = "com.nowbar.demo.SHOW_NATIVE_STYLE"
        const val ACTION_PAUSE = "com.nowbar.demo.PAUSE"
        const val ACTION_RESUME = "com.nowbar.demo.RESUME"
        const val ACTION_NEXT = "com.nowbar.demo.NEXT"
        const val ACTION_UNPIN = "com.nowbar.demo.UNPIN"
        const val ACTION_DISMISS = "com.nowbar.demo.DISMISS"
        const val ACTION_STOP = "com.nowbar.demo.STOP"

        val ACTIONS = setOf(
            ACTION_SHOW_TIMER,
            ACTION_SHOW_WORKOUT,
            ACTION_SHOW_NAVIGATION,
            ACTION_SHOW_DELIVERY,
            ACTION_SHOW_METRICS,
            ACTION_SHOW_MEDIA,
            ACTION_SHOW_CALL,
            ACTION_SHOW_CALL_SCREENING,
            ACTION_SHOW_BIG_TEXT,
            ACTION_SHOW_CUSTOM_ACTIONS,
            ACTION_SHOW_DUMP,
            ACTION_SHOW_NATIVE_STYLE,
            ACTION_PAUSE,
            ACTION_RESUME,
            ACTION_NEXT,
            ACTION_UNPIN,
            ACTION_DISMISS,
            ACTION_STOP
        )

        private const val CHANNEL_ID = "nowbar_sdk_demo"
        const val NOTIFICATION_ID = 200
        const val EXTRA_NATIVE_STYLE_AVAILABLE = "com.nowbar.demo.extra.NATIVE_STYLE_AVAILABLE"
        const val EXTRA_NATIVE_STYLE_COMPLETE = "com.nowbar.demo.extra.NATIVE_STYLE_COMPLETE"
        const val EXTRA_NATIVE_STYLE_REPORT = "com.nowbar.demo.extra.NATIVE_STYLE_REPORT"
        private const val TAG = "NowBarDemo"
        private val ACCENT = 0xFF0FCF6E.toInt()
        private const val PENDING_FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        fun isDemoAction(action: String): Boolean = action in ACTIONS

        fun intent(context: Context, action: String): Intent =
            Intent(context, DemoNowBarService::class.java).setAction(action)
    }
}
