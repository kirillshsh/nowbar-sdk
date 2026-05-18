package com.nowbar.api.notification

import android.app.PendingIntent
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.graphics.drawable.IconCompat
import com.nowbar.api.FeatureDetector
import com.nowbar.api.NowBarConfig
import com.nowbar.api.cards.CallCard
import com.nowbar.api.cards.CustomCard
import com.nowbar.api.cards.DeliveryCard
import com.nowbar.api.cards.DeliveryStatus
import com.nowbar.api.cards.TimerCard
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.minutes
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBuild

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NowBarNotificationBuilderTest {

    @Test
    @Suppress("DEPRECATION")
    fun `config chronometer and capsule are included in Samsung extras for timer cards`() {
        val context = RuntimeEnvironment.getApplication()
        shadowOf(context.packageManager).setSystemFeature(FeatureDetector.FEATURE_SAMSUNG_NOWBAR, true)

        val chronometerViews = RemoteViews(context.packageName, android.R.layout.simple_list_item_1)
        val capsuleViews = RemoteViews(context.packageName, android.R.layout.simple_list_item_1)
        val config = NowBarConfig(
            channelId = "test",
            channelName = "Test",
            chronometerConfig = ChronometerConfig(
                remoteView = chronometerViews,
                tag = "timer"
            ),
            capsuleConfig = CapsuleConfig(
                layout = capsuleViews,
                bgStartColor = 0xFF6162E9.toInt(),
                bgEndColor = 0xFF859FFE.toInt(),
                priority = NowBarExtrasKeys.CapsulePriority.NORMAL
            )
        )
        val card = TimerCard(
            title = "Timer",
            icon = IconCompat.createWithResource(context, android.R.drawable.ic_menu_recent_history),
            totalDuration = 5.minutes,
            remainingDuration = 3.minutes
        )

        val notification = NowBarNotificationBuilder(context, config)
            .build(card, NowBarConfig.STYLE_BOTH, requestPromotedOngoing = false)

        assertNotNull(notification.extras.getParcelable<RemoteViews>(OngoingExtrasBuilder.KEY_CHRONOMETER_REMOTE_VIEW))
        assertEquals("timer", notification.extras.getCharSequence(OngoingExtrasBuilder.KEY_CHRONOMETER_REMOTE_VIEW_TAG))
        assertNotNull(notification.extras.getParcelable<RemoteViews>(OngoingExtrasBuilder.KEY_CAPSULE_LAYOUT))
        assertEquals(NowBarExtrasKeys.CapsulePriority.NORMAL, notification.extras.getString(OngoingExtrasBuilder.KEY_CAPSULE_PRIORITY))
    }

    @Test
    @Suppress("DEPRECATION")
    fun `config aod remote app is included in Samsung extras`() {
        val context = RuntimeEnvironment.getApplication()
        shadowOf(context.packageManager).setSystemFeature(FeatureDetector.FEATURE_SAMSUNG_NOWBAR, true)
        val icon = android.graphics.drawable.Icon.createWithResource(context, android.R.drawable.ic_dialog_info)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            45,
            Intent("test.AOD_REMOTE_APP"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val config = NowBarConfig(
            channelId = "test",
            channelName = "Test",
            aodRemoteApp = SamsungRemoteAppConfig(
                name = "NowBar SDK Demo",
                icon = icon,
                pendingIntent = pendingIntent
            )
        )
        val card = CustomCard(
            title = "Delivery",
            icon = IconCompat.createWithResource(context, android.R.drawable.ic_menu_compass),
            primaryText = "On the way",
            secondaryText = "10 min"
        )

        val notification = NowBarNotificationBuilder(context, config)
            .build(card, NowBarConfig.STYLE_BOTH, requestPromotedOngoing = false)
        val evidence = NowBarNotificationEvidence.inspect(notification)

        assertEquals("NowBar SDK Demo", notification.extras.getCharSequence(OngoingExtrasBuilder.KEY_AOD_REMOTE_APP_NAME))
        assertEquals(icon, notification.extras.getParcelable(OngoingExtrasBuilder.KEY_AOD_REMOTE_APP_ICON))
        assertEquals(pendingIntent, notification.extras.getParcelable(OngoingExtrasBuilder.KEY_AOD_REMOTE_APP_PENDING_INTENT))
        assertTrue(evidence.hasAodRemoteApp)
        assertEquals("NowBar SDK Demo", evidence.samsungNowBar?.remoteApp?.name)
        assertEquals(icon, evidence.samsungNowBar?.remoteApp?.icon)
        assertEquals(pendingIntent, evidence.samsungNowBar?.remoteApp?.pendingIntent)
        assertTrue(NowBarEvidencePath.SAMSUNG_AOD_REMOTE_APP in evidence.evidencePaths)
    }

    @Test
    fun `generic notification builder applies content delete intent and large icon hooks`() {
        val context = RuntimeEnvironment.getApplication()
        val contentIntent = PendingIntent.getBroadcast(
            context,
            43,
            Intent("test.OPEN"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val deleteIntent = PendingIntent.getBroadcast(
            context,
            44,
            Intent("test.DELETE"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val card = CustomCard(
            title = "Delivery",
            icon = IconCompat.createWithResource(context, android.R.drawable.ic_menu_compass),
            tapAction = contentIntent,
            deleteIntent = deleteIntent,
            largeIcon = IconCompat.createWithResource(context, android.R.drawable.ic_dialog_map),
            primaryText = "On the way",
            secondaryText = "10 min"
        )

        val notification = NowBarNotificationBuilder(
            context,
            NowBarConfig(channelId = "test", channelName = "Test")
        ).build(card, NowBarConfig.STYLE_BOTH, requestPromotedOngoing = false)

        assertEquals(contentIntent, notification.contentIntent)
        assertEquals(deleteIntent, notification.deleteIntent)
        assertNotNull(notification.getLargeIcon())

        val evidence = NowBarNotificationEvidence.inspect(notification)
        val report = LiveUpdateDiagnostics.inspect(context, notification)
        assertTrue(evidence.hasContentIntent)
        assertTrue(NowBarEvidencePath.ANDROID_CONTENT_INTENT in evidence.evidencePaths)
        assertTrue(evidence.hasDeleteIntent)
        assertTrue(NowBarEvidencePath.ANDROID_DELETE_INTENT in evidence.evidencePaths)
        assertTrue(report.hasContentIntent)
        assertTrue(report.hasDeleteIntent)
    }

    @Test
    fun `generic notification builder caps actions at Android live update maximum`() {
        val context = RuntimeEnvironment.getApplication()
        val actions = listOf("Pause", "Resume", "Stop", "Next").mapIndexed { index, text ->
            ActionConfig(
                id = text.lowercase(),
                text = text,
                iconRes = android.R.drawable.ic_dialog_info,
                intent = PendingIntent.getBroadcast(
                    context,
                    index,
                    Intent("test.$text"),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        }
        val card = CustomCard(
            title = "Controls",
            icon = IconCompat.createWithResource(context, android.R.drawable.ic_menu_compass),
            primaryText = "Running",
            actions = actions
        )

        val notification = NowBarNotificationBuilder(
            context,
            NowBarConfig(channelId = "test", channelName = "Test")
        ).build(card, NowBarConfig.STYLE_BOTH, requestPromotedOngoing = false)

        assertEquals(NowBarActionLimits.MAX_ACTIONS, notification.actions.size)
        assertEquals(listOf("Pause", "Resume", "Stop"), notification.actions.map { it.title.toString() })
    }

    @Test
    fun `generic notification builder supports text only disabled actions`() {
        val context = RuntimeEnvironment.getApplication()
        val card = CustomCard(
            title = "Order",
            icon = IconCompat.createWithResource(context, android.R.drawable.ic_menu_compass),
            primaryText = "Arriving",
            actions = listOf(
                ActionConfig.textOnly("got_it", "Got it", semantic = ActionSemantic.UNPIN),
                ActionConfig.disabled("tip", "Tip", semantic = ActionSemantic.DELETE)
            )
        )

        val notification = NowBarNotificationBuilder(
            context,
            NowBarConfig(channelId = "test", channelName = "Test")
        ).build(card, NowBarConfig.STYLE_BOTH, requestPromotedOngoing = false)

        assertEquals(listOf("Got it", "Tip"), notification.actions.map { it.title.toString() })
        assertEquals(null, notification.actions[0].getIcon())
        assertEquals(null, notification.actions[1].getIcon())
        assertEquals(null, notification.actions[0].actionIntent)
        assertEquals(null, notification.actions[1].actionIntent)

        val evidence = NowBarNotificationEvidence.inspect(notification)
        assertEquals(listOf("Got it", "Tip"), evidence.androidActionTitles)
        assertEquals(listOf("got_it", "tip"), evidence.androidActionIds)
        assertEquals(listOf(ActionSemantic.UNPIN, ActionSemantic.DELETE), evidence.androidActionSemantics)
        assertEquals(2, evidence.androidTextOnlyActionCount)
        assertEquals(2, evidence.androidDisabledActionCount)
        assertTrue(NowBarEvidencePath.ANDROID_ACTION_BUTTONS in evidence.evidencePaths)

        val report = LiveUpdateDiagnostics.inspect(context, notification)
        assertEquals(listOf("got_it", "tip"), report.androidActionIds)
        assertEquals(listOf(ActionSemantic.UNPIN, ActionSemantic.DELETE), report.androidActionSemantics)
    }

    @Test
    fun `custom card with big text uses BigTextStyle template`() {
        val context = RuntimeEnvironment.getApplication()
        val card = CustomCard.Builder.create(
            "Incident",
            IconCompat.createWithResource(context, android.R.drawable.ic_dialog_info),
            "Rerouting"
        )
            .secondaryText("Service is recovering")
            .bigText("Service is recovering after a network interruption. Route updates remain live.")
            .build()

        val notification = NowBarNotificationBuilder(
            context,
            NowBarConfig(channelId = "test", channelName = "Test")
        ).build(card, NowBarConfig.STYLE_BOTH, requestPromotedOngoing = false)
        val evidence = NowBarNotificationEvidence.inspect(notification)

        assertTrue(evidence.bigTextStyleTemplate)
        assertTrue(NowBarEvidencePath.ANDROID_BIG_TEXT_STYLE in evidence.evidencePaths)
        assertEquals(LiveUpdateAllowedStyle.BIG_TEXT, LiveUpdateDiagnostics.inspect(context, notification).liveUpdateStyle)
    }

    @Test
    fun `live update builder resolves explicit short critical text for status chip`() {
        val context = RuntimeEnvironment.getApplication()
        val card = CustomCard.Builder.create(
            "Order",
            IconCompat.createWithResource(context, android.R.drawable.ic_menu_compass),
            "Driver nearby"
        )
            .secondaryText("Arrives at 10:08")
            .progressValue(50)
            .shortCriticalText("10:08")
            .build()

        assertEquals("10:08", LiveUpdateBuilder().shortCriticalTextFor(card))
    }

    @Test
    fun `call card with system actions uses CallStyle template`() {
        val context = RuntimeEnvironment.getApplication()
        val answerIntent = PendingIntent.getBroadcast(
            context,
            51,
            Intent("test.ANSWER"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val declineIntent = PendingIntent.getBroadcast(
            context,
            52,
            Intent("test.DECLINE"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val card = CallCard.Builder.create(
            "Call",
            IconCompat.createWithResource(context, android.R.drawable.sym_call_incoming),
            "Alice"
        )
            .callerNumber("+15551234567")
            .largeIcon(IconCompat.createWithResource(context, android.R.drawable.sym_def_app_icon))
            .answerAction(answerIntent)
            .declineAction(declineIntent)
            .isVideo()
            .verificationText("Verified caller")
            .build()

        val notification = NowBarNotificationBuilder(
            context,
            NowBarConfig(channelId = "test", channelName = "Test")
        ).build(card, NowBarConfig.STYLE_BOTH, requestPromotedOngoing = false)
        val evidence = NowBarNotificationEvidence.inspect(notification)

        assertTrue(evidence.callStyleTemplate)
        assertTrue(NowBarEvidencePath.ANDROID_CALL_STYLE in evidence.evidencePaths)
        assertTrue(LiveUpdateDiagnostics.inspect(context, notification).allowedStyle)
    }

    @Test
    fun `screening call card uses CallStyle screening type`() {
        val context = RuntimeEnvironment.getApplication()
        val answerIntent = PendingIntent.getBroadcast(
            context,
            53,
            Intent("test.SCREENING_ANSWER"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val hangupIntent = PendingIntent.getBroadcast(
            context,
            54,
            Intent("test.SCREENING_HANGUP"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val card = CallCard.Builder.create(
            "Call screening",
            IconCompat.createWithResource(context, android.R.drawable.sym_call_incoming),
            "Unknown caller"
        )
            .callerNumber("+15557654321")
            .screeningCall(answerIntent, hangupIntent)
            .verificationText("Screening caller")
            .build()

        val notification = NowBarNotificationBuilder(
            context,
            NowBarConfig(channelId = "test", channelName = "Test")
        ).build(card, NowBarConfig.STYLE_BOTH, requestPromotedOngoing = false)
        val evidence = NowBarNotificationEvidence.inspect(notification)

        assertTrue(evidence.callStyleTemplate)
        assertEquals(3, evidence.callStyleType)
        assertEquals("Screening caller", evidence.verificationText)
    }

    @Test
    fun `samsung manufacturer fallback applies Samsung extras without feature flag`() {
        val context = RuntimeEnvironment.getApplication()
        shadowOf(context.packageManager).setSystemFeature(FeatureDetector.FEATURE_SAMSUNG_NOWBAR, false)
        ShadowBuild.setManufacturer("Samsung")
        ShadowBuild.setBrand("samsung")

        try {
            val card = CustomCard(
                title = "Delivery",
                icon = IconCompat.createWithResource(context, android.R.drawable.ic_menu_compass),
                primaryText = "On the way",
                secondaryText = "10 min"
            )

            val notification = NowBarNotificationBuilder(
                context,
                NowBarConfig(channelId = "test", channelName = "Test")
            ).build(card, NowBarConfig.STYLE_BOTH, requestPromotedOngoing = false)

            assertEquals(OngoingExtrasBuilder.STYLE_BOTH, notification.extras.getInt(OngoingExtrasBuilder.KEY_STYLE))
            assertEquals("On the way", notification.extras.getString(OngoingExtrasBuilder.KEY_PRIMARY_INFO))
        } finally {
            ShadowBuild.reset()
        }
    }

    @Test
    @Suppress("DEPRECATION")
    fun `delivery progress segments are mirrored to Samsung extras`() {
        val context = RuntimeEnvironment.getApplication()
        shadowOf(context.packageManager).setSystemFeature(FeatureDetector.FEATURE_SAMSUNG_NOWBAR, true)
        val icon = IconCompat.createWithResource(context, android.R.drawable.ic_menu_compass)
        val card = DeliveryCard.Builder.create("Delivery", icon, "Bakery", DeliveryStatus.EN_ROUTE)
            .accentColor(0x123456)
            .progress(60)
            .build()

        val notification = NowBarNotificationBuilder(
            context,
            NowBarConfig(channelId = "test", channelName = "Test")
        ).build(card, NowBarConfig.STYLE_BOTH, requestPromotedOngoing = false)

        val segments = assertNotNull(
            notification.extras.getParcelableArray(OngoingExtrasBuilder.KEY_PROGRESS_SEGMENTS)
        )
        val starts = segments.map { segment ->
            (segment as android.os.Bundle).getFloat(OngoingExtrasBuilder.KEY_SEGMENT_START)
        }
        val colors = segments.map { segment ->
            (segment as android.os.Bundle).getInt(OngoingExtrasBuilder.KEY_SEGMENT_COLOR)
        }

        assertEquals(listOf(0.0f, 0.25f, 0.5f, 0.75f), starts)
        assertEquals(listOf(0x123456, 0x123456, 0x123456, 0x123456), colors)
        assertTrue(notification.extras.containsKey(OngoingExtrasBuilder.KEY_SEGMENT_ICON))
    }

    @Test
    @Suppress("DEPRECATION")
    fun `custom card progress segments are mirrored to Samsung extras`() {
        val context = RuntimeEnvironment.getApplication()
        shadowOf(context.packageManager).setSystemFeature(FeatureDetector.FEATURE_SAMSUNG_NOWBAR, true)
        val icon = IconCompat.createWithResource(context, android.R.drawable.ic_menu_compass)
        val card = CustomCard.Builder.create("Trip", icon, "Driver nearby")
            .progressValue(120)
            .progressSegments(
                listOf(
                    StyleSegment(length = 80, color = 0x111111),
                    StyleSegment(length = 120, color = 0x222222)
                )
            )
            .progressTrackerIcon(icon)
            .build()

        val notification = NowBarNotificationBuilder(
            context,
            NowBarConfig(channelId = "test", channelName = "Test")
        ).build(card, NowBarConfig.STYLE_BOTH, requestPromotedOngoing = false)

        val segments = assertNotNull(
            notification.extras.getParcelableArray(OngoingExtrasBuilder.KEY_PROGRESS_SEGMENTS)
        )
        val first = segments[0] as android.os.Bundle
        val second = segments[1] as android.os.Bundle

        assertEquals(2, segments.size)
        assertEquals(0.0f, first.getFloat(OngoingExtrasBuilder.KEY_SEGMENT_START))
        assertEquals(0x111111, first.getInt(OngoingExtrasBuilder.KEY_SEGMENT_COLOR))
        assertEquals(0.4f, second.getFloat(OngoingExtrasBuilder.KEY_SEGMENT_START))
        assertEquals(0x222222, second.getInt(OngoingExtrasBuilder.KEY_SEGMENT_COLOR))
        assertTrue(notification.extras.containsKey(OngoingExtrasBuilder.KEY_SEGMENT_ICON))
    }

    @Test
    fun `delivery cards expose semantic styles for live update titles`() {
        val context = RuntimeEnvironment.getApplication()
        val card = DeliveryCard(
            title = "Delivery",
            icon = IconCompat.createWithResource(context, android.R.drawable.ic_menu_compass),
            status = DeliveryStatus.ARRIVING,
            merchantName = "Bakery"
        )

        assertEquals(LiveUpdateSemanticStyle.CAUTION, card.toSemanticStyle())
    }

    @Test
    fun `delivery cards expose merchant as live update subtext`() {
        val context = RuntimeEnvironment.getApplication()
        val card = DeliveryCard(
            title = "Delivery",
            icon = IconCompat.createWithResource(context, android.R.drawable.ic_menu_compass),
            status = DeliveryStatus.EN_ROUTE,
            merchantName = "Bakery",
            eta = "10 min"
        )

        val notification = NowBarNotificationBuilder(
            context,
            NowBarConfig(channelId = "test", channelName = "Test")
        ).build(card, NowBarConfig.STYLE_BOTH, requestPromotedOngoing = false)
        val evidence = NowBarNotificationEvidence.inspect(notification)

        assertEquals("Bakery", card.toSubText())
        assertEquals("Bakery", evidence.subText)
    }

    @Test
    fun `delivery card exposes chip time countdown for live update status chip`() {
        val context = RuntimeEnvironment.getApplication()
        val targetTime = System.currentTimeMillis() + 10_000L
        val card = DeliveryCard(
            title = "Delivery",
            icon = IconCompat.createWithResource(context, android.R.drawable.ic_menu_compass),
            status = DeliveryStatus.EN_ROUTE,
            merchantName = "Bakery",
            chipWhenTimeMillis = targetTime
        )

        assertEquals(targetTime, card.toChipWhenTimeMillis())
        assertTrue(card.isChipChronometerCountDown())
    }

    @Test
    fun `timer card exposes countdown status chip on standard notification path`() {
        val context = RuntimeEnvironment.getApplication()
        val before = System.currentTimeMillis()
        val card = TimerCard(
            title = "Timer",
            icon = IconCompat.createWithResource(context, android.R.drawable.ic_menu_recent_history),
            totalDuration = 5.minutes,
            remainingDuration = 90.seconds
        )

        val notification = NowBarNotificationBuilder(
            context,
            NowBarConfig(channelId = "timer-chip", channelName = "Timer Chip")
        ).build(card, NowBarConfig.STYLE_BOTH, requestPromotedOngoing = false)

        assertTrue(notification.`when` >= before + 90.seconds.inWholeMilliseconds)
        assertTrue(notification.`when` <= System.currentTimeMillis() + 90.seconds.inWholeMilliseconds)
        assertTrue(notification.extras.getBoolean(android.app.Notification.EXTRA_SHOW_CHRONOMETER))
        assertTrue(notification.extras.getBoolean(android.app.Notification.EXTRA_CHRONOMETER_COUNT_DOWN))
    }

    @Test
    fun `timer card exposes stopwatch status chip base time`() {
        val context = RuntimeEnvironment.getApplication()
        val before = System.currentTimeMillis()
        val card = TimerCard(
            title = "Stopwatch",
            icon = IconCompat.createWithResource(context, android.R.drawable.ic_menu_recent_history),
            totalDuration = 5.minutes,
            remainingDuration = 3.minutes,
            isCountDown = false
        )

        val chipWhen = card.toChipWhenTimeMillis()

        assertNotNull(chipWhen)
        assertTrue(chipWhen <= before - 2.minutes.inWholeMilliseconds + 1_000L)
        assertTrue(chipWhen >= System.currentTimeMillis() - 2.minutes.inWholeMilliseconds - 1_000L)
        assertTrue(card.isChipChronometerCountDown().not())
    }

    @Test
    fun `custom card exposes chip time countdown for live update status chip`() {
        val context = RuntimeEnvironment.getApplication()
        val targetTime = System.currentTimeMillis() + 20_000L
        val card = CustomCard.Builder.create(
            "Ride",
            IconCompat.createWithResource(context, android.R.drawable.ic_menu_compass),
            "Driver nearby"
        )
            .secondaryText("Pickup soon")
            .chipWhenTimeMillis(targetTime, countDown = true)
            .build()

        assertEquals(targetTime, card.toChipWhenTimeMillis())
        assertTrue(card.isChipChronometerCountDown())
    }
}
