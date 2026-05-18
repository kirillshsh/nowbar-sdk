package com.nowbar.api.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.widget.RemoteViews
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SamsungOngoingActivityStyleBuilderTest {

    @Test
    fun `returns null when Samsung hidden style is absent`() {
        val result = SamsungOngoingActivityStyleBuilder(
            "android.app.Notification\$DefinitelyMissingOngoingActivityStyle"
        )
            .primaryInfo("Recording")
            .buildWithReport()

        assertNull(result.style)
        assertFalse(result.report.classAvailable)
        assertFalse(result.report.styleCreated)
        assertEquals(listOf("setPrimaryInfo"), result.report.requestedMethods)
        assertEquals(listOf("setPrimaryInfo"), result.report.missingMethods)
        assertFalse(result.report.complete)
        assertFalse(SamsungOngoingActivityStyleBuilder.isAvailable())
    }

    @Test
    fun `applies supported Samsung Voice Recorder style methods`() {
        val context = RuntimeEnvironment.getApplication()
        val chipIcon = Icon.createWithResource(context, android.R.drawable.ic_btn_speak_now)
        val cardIcon = Icon.createWithResource(context, android.R.drawable.ic_media_play)
        val badgeIcon = Icon.createWithResource(context, android.R.drawable.star_big_on)
        val expandedChip = RemoteViews(context.packageName, android.R.layout.simple_list_item_1)
        val centerUi = RemoteViews(context.packageName, android.R.layout.simple_list_item_2)
        val expandedCard = RemoteViews(context.packageName, android.R.layout.test_list_item)
        val action = Notification.Action.Builder(
            Icon.createWithResource(context, android.R.drawable.ic_media_pause),
            "Pause",
            PendingIntent.getBroadcast(
                context,
                9,
                Intent("test.PAUSE"),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()

        val result = SamsungOngoingActivityStyleBuilder(FakeOngoingActivityStyle::class.java.name)
            .chipIcon(chipIcon)
            .chipBackgroundColor(0xFFD92323.toInt())
            .cardIcon(cardIcon)
            .badgeIcon(badgeIcon)
            .cardBackgroundColor(0xB3FCFCFF.toInt())
            .primaryInfo("Recording")
            .secondaryInfo("00:12")
            .moreInfo("Voice Recorder")
            .expandedChipView(expandedChip)
            .customCardViewCenterUi(centerUi)
            .customExpandedCardView(expandedCard)
            .action(action)
            .buildWithReport()

        val fake = assertIs<FakeOngoingActivityStyle>(result.style)
        assertTrue(result.report.classAvailable)
        assertTrue(result.report.styleCreated)
        assertTrue(result.report.complete)
        assertEquals(result.report.requestedMethods, result.report.appliedMethods)
        assertTrue(result.report.missingMethods.isEmpty())
        assertTrue(result.report.failedMethods.isEmpty())
        assertSame(chipIcon, fake.chipIcon)
        assertEquals(0xFFD92323.toInt(), fake.chipBackground)
        assertSame(cardIcon, fake.cardIcon)
        assertSame(badgeIcon, fake.badge)
        assertEquals(0xB3FCFCFF.toInt(), fake.cardBackground)
        assertEquals("Recording", fake.primaryInfo)
        assertEquals("00:12", fake.secondaryInfo)
        assertEquals("Voice Recorder", fake.moreInfo)
        assertSame(expandedChip, fake.expandedChipView)
        assertSame(centerUi, fake.customCardViewCenterUi)
        assertSame(expandedCard, fake.customExpandedCardView)
        assertEquals(listOf(action), fake.actions)
    }

    @Test
    fun `ignores absent methods and still returns style`() {
        val result = SamsungOngoingActivityStyleBuilder(PartialOngoingActivityStyle::class.java.name)
            .primaryInfo("Ready")
            .chipBackgroundColor(0xFF000000.toInt())
            .buildWithReport()

        val fake = assertIs<PartialOngoingActivityStyle>(result.style)
        assertEquals("Ready", fake.primaryInfo())
        assertTrue(fake.seenPrimaryInfo)
        assertEquals(listOf("setPrimaryInfo"), result.report.appliedMethods)
        assertEquals(listOf("setChipBackground"), result.report.missingMethods)
        assertTrue(result.report.failedMethods.isEmpty())
        assertFalse(result.report.complete)
        assertTrue(result.report.toDisplayString().contains("Missing methods: setChipBackground"))
    }

    @Suppress("DEPRECATION")
    class FakeOngoingActivityStyle : Notification.Style() {
        var chipIcon: Icon? = null
        var chipBackground: Int? = null
        var cardIcon: Icon? = null
        var badge: Icon? = null
        var cardBackground: Int? = null
        var primaryInfo: CharSequence? = null
        var secondaryInfo: CharSequence? = null
        var moreInfo: CharSequence? = null
        var expandedChipView: RemoteViews? = null
        var customCardViewCenterUi: RemoteViews? = null
        var customExpandedCardView: RemoteViews? = null
        val actions = mutableListOf<Notification.Action>()

        fun setChipIcon(icon: Icon) = apply { chipIcon = icon }
        fun setChipBackground(color: Int) = apply { chipBackground = color }
        fun setCardIcon(icon: Icon) = apply { cardIcon = icon }
        fun setBadge(icon: Icon) = apply { badge = icon }
        fun setCardBackground(color: Int) = apply { cardBackground = color }
        fun setPrimaryInfo(text: CharSequence) = apply { primaryInfo = text }
        fun setSecondaryInfo(text: CharSequence) = apply { secondaryInfo = text }
        fun setMoreInfo(text: CharSequence) = apply { moreInfo = text }
        fun setExpandedChipView(remoteViews: RemoteViews) = apply { expandedChipView = remoteViews }
        fun setCustomCardViewCenterUI(remoteViews: RemoteViews) = apply { customCardViewCenterUi = remoteViews }
        fun setCustomExpandedCardView(remoteViews: RemoteViews) = apply { customExpandedCardView = remoteViews }
        fun addAction(action: Notification.Action) = apply { actions += action }
    }

    @Suppress("DEPRECATION")
    class PartialOngoingActivityStyle : Notification.Style() {
        private var primaryInfoValue: CharSequence? = null
        var seenPrimaryInfo = false

        fun setPrimaryInfo(text: CharSequence) = apply {
            primaryInfoValue = text
            seenPrimaryInfo = true
        }

        fun primaryInfo(): CharSequence? = primaryInfoValue
    }
}
