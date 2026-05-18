package com.nowbar.api.fallback

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import androidx.core.graphics.drawable.IconCompat
import com.nowbar.api.cards.CallCard
import com.nowbar.api.cards.CustomCard
import com.nowbar.api.cards.DeliveryCard
import com.nowbar.api.cards.DeliveryStatus
import com.nowbar.api.cards.TimerCard
import com.nowbar.api.notification.ActionConfig
import com.nowbar.api.notification.ActionSemantic
import com.nowbar.api.notification.NowBarEvidencePath
import com.nowbar.api.notification.NowBarActionLimits
import com.nowbar.api.notification.NowBarNotificationEvidence
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StandardNotificationAdapterTest {

    private val context = RuntimeEnvironment.getApplication()
    private val icon: IconCompat
        get() = IconCompat.createWithResource(context, android.R.drawable.ic_dialog_info)

    @Test
    fun `fallback adapter renders generic card actions`() {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            Intent("test.ACTION"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val card = CustomCard.Builder.create("Custom", icon, "Primary")
            .action(ActionConfig("track", "Track", android.R.drawable.ic_menu_compass, pendingIntent, ActionSemantic.CUSTOM))
            .build()

        val notification = StandardNotificationAdapter.build(context, "test", card)
        val evidence = NowBarNotificationEvidence.inspect(notification)

        assertEquals(1, notification.actions.size)
        assertEquals("Track", notification.actions.first().title.toString())
        assertEquals(listOf("track"), evidence.androidActionIds)
        assertEquals(listOf(ActionSemantic.CUSTOM), evidence.androidActionSemantics)
    }

    @Test
    fun `fallback adapter caps generic actions at live update maximum`() {
        val actions = listOf("Pause", "Resume", "Stop", "Next").mapIndexed { index, title ->
            ActionConfig(
                id = title.lowercase(),
                text = title,
                iconRes = android.R.drawable.ic_menu_compass,
                intent = PendingIntent.getBroadcast(
                    context,
                    index,
                    Intent("test.$title"),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                ),
                semantic = ActionSemantic.CUSTOM
            )
        }
        val card = CustomCard.Builder.create("Custom", icon, "Primary")
            .actions(actions)
            .build()

        val notification = StandardNotificationAdapter.build(context, "test", card)

        assertEquals(NowBarActionLimits.MAX_ACTIONS, notification.actions.size)
        assertEquals(listOf("Pause", "Resume", "Stop"), notification.actions.map { it.title.toString() })
    }

    @Test
    fun `fallback adapter preserves delivery indeterminate progress and countdown chip`() {
        val whenTime = System.currentTimeMillis() + 60_000L
        val card = DeliveryCard.Builder.create("Delivery", icon, "Bakery", DeliveryStatus.INITIALIZING)
            .progress(null)
            .chipWhenTimeMillis(whenTime)
            .build()

        val notification = StandardNotificationAdapter.build(context, "test", card)

        assertTrue(notification.extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE))
        assertEquals(whenTime, notification.`when`)
        assertTrue(notification.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        assertTrue(notification.extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN))
    }

    @Test
    fun `fallback adapter uses BigTextStyle for custom big text cards`() {
        val card = CustomCard.Builder.create("Incident", icon, "Rerouting")
            .secondaryText("Service is recovering")
            .bigText("Service is recovering after a network interruption. Route updates remain live.")
            .build()

        val notification = StandardNotificationAdapter.build(context, "test", card)
        val evidence = NowBarNotificationEvidence.inspect(notification)

        assertTrue(evidence.bigTextStyleTemplate)
        assertTrue(NowBarEvidencePath.ANDROID_BIG_TEXT_STYLE in evidence.evidencePaths)
    }

    @Test
    fun `fallback adapter preserves custom card chronometer status chip`() {
        val whenTime = System.currentTimeMillis() + 120_000L
        val card = CustomCard.Builder.create("Ride", icon, "Driver nearby")
            .secondaryText("Pickup soon")
            .chipWhenTimeMillis(whenTime, countDown = true)
            .build()

        val notification = StandardNotificationAdapter.build(context, "test", card)

        assertEquals(whenTime, notification.`when`)
        assertTrue(notification.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        assertTrue(notification.extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN))
    }

    @Test
    fun `fallback adapter preserves short critical text status chip`() {
        val card = CustomCard.Builder.create("Order", icon, "Driver nearby")
            .secondaryText("Arrives soon")
            .shortCriticalText("10:08")
            .build()

        val notification = StandardNotificationAdapter.build(context, "test", card)
        val evidence = NowBarNotificationEvidence.inspect(notification)

        assertEquals("10:08", evidence.shortCriticalText)
        assertTrue(NowBarEvidencePath.ANDROID_SHORT_CRITICAL_TEXT in evidence.evidencePaths)
    }

    @Test
    fun `fallback adapter derives delivery status chip text`() {
        val card = DeliveryCard.Builder.create("Delivery", icon, "Bakery", DeliveryStatus.EN_ROUTE)
            .eta("10 min")
            .build()

        val notification = StandardNotificationAdapter.build(context, "test", card)
        val evidence = NowBarNotificationEvidence.inspect(notification)

        assertEquals("10 min", evidence.shortCriticalText)
        assertTrue(NowBarEvidencePath.ANDROID_SHORT_CRITICAL_TEXT in evidence.evidencePaths)
    }

    @Test
    fun `fallback adapter preserves timer countdown status chip`() {
        val before = System.currentTimeMillis()
        val card = TimerCard(
            title = "Timer",
            icon = icon,
            totalDuration = 5.minutes,
            remainingDuration = 90.seconds
        )

        val notification = StandardNotificationAdapter.build(context, "test", card)

        assertTrue(notification.`when` >= before + 90.seconds.inWholeMilliseconds)
        assertTrue(notification.`when` <= System.currentTimeMillis() + 90.seconds.inWholeMilliseconds)
        assertTrue(notification.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        assertTrue(notification.extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN))
    }

    @Test
    fun `fallback adapter uses CallStyle for ongoing call cards`() {
        val hangupIntent = PendingIntent.getBroadcast(
            context,
            80,
            Intent("test.HANGUP"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val card = CallCard.Builder.create("Call", icon, "Alice")
            .isIncoming(false)
            .callerNumber("+15551234567")
            .hangupAction(hangupIntent)
            .build()

        val notification = StandardNotificationAdapter.build(context, "test", card)
        val evidence = NowBarNotificationEvidence.inspect(notification)

        assertTrue(evidence.callStyleTemplate)
        assertTrue(NowBarEvidencePath.ANDROID_CALL_STYLE in evidence.evidencePaths)
    }
}
