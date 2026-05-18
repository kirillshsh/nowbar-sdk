@file:Suppress("DEPRECATION")

package com.nowbar.api.notification

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.graphics.drawable.Icon
import android.widget.RemoteViews
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OngoingExtrasBuilderTest {

    @Test
    fun `chip text and background are written to Samsung extras`() {
        val extras = OngoingExtrasBuilder()
            .setChipConfig(ChipConfig(backgroundColor = 0x123456, expandedText = "Demo"))
            .build()

        assertEquals(0x123456, extras.getInt(OngoingExtrasBuilder.KEY_CHIP_BG_COLOR))
        assertEquals("Demo", extras.getString(OngoingExtrasBuilder.KEY_CHIP_EXPANDED_TEXT))
    }

    @Test
    fun `progress is clamped and writes max`() {
        val extras = OngoingExtrasBuilder()
            .setProgress(current = 150, max = 100)
            .build()

        assertEquals(100, extras.getInt(OngoingExtrasBuilder.KEY_PROGRESS))
        assertEquals(100, extras.getInt(OngoingExtrasBuilder.KEY_PROGRESS_MAX))
    }

    @Test
    fun `progress segment icon can be written as a top level Samsung extra`() {
        val context = RuntimeEnvironment.getApplication()
        val extras = OngoingExtrasBuilder()
            .setProgressSegmentIcon(Icon.createWithResource(context, android.R.drawable.ic_dialog_map))
            .build()

        assertTrue(extras.containsKey(OngoingExtrasBuilder.KEY_SEGMENT_ICON))
    }

    @Test
    fun `aod remote app identity is written to Samsung extras`() {
        val context = RuntimeEnvironment.getApplication()
        val icon = Icon.createWithResource(context, android.R.drawable.ic_dialog_info)
        val pendingIntent = PendingIntent.getActivity(
            context,
            11,
            Intent("test.AOD_REMOTE_APP"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val extras = OngoingExtrasBuilder()
            .setAodRemoteApp(
                SamsungRemoteAppConfig(
                    name = "Samsung Health",
                    icon = icon,
                    pendingIntent = pendingIntent
                )
            )
            .build()

        assertEquals("Samsung Health", extras.getCharSequence(OngoingExtrasBuilder.KEY_AOD_REMOTE_APP_NAME))
        assertSame(icon, extras.getParcelable(OngoingExtrasBuilder.KEY_AOD_REMOTE_APP_ICON))
        assertSame(pendingIntent, extras.getParcelable(OngoingExtrasBuilder.KEY_AOD_REMOTE_APP_PENDING_INTENT))
    }

    @Test
    @Suppress("DEPRECATION")
    fun `progress segment bundles are sorted and use Samsung Health keys`() {
        val context = RuntimeEnvironment.getApplication()
        val extras = OngoingExtrasBuilder()
            .setProgressSegments(
                ProgressSegment(startPosition = 0.67f, color = 0x333333),
                ProgressSegment(
                    startPosition = 0.0f,
                    color = 0x111111,
                    icon = Icon.createWithResource(context, android.R.drawable.ic_dialog_map)
                ),
                ProgressSegment(startPosition = 0.34f, color = 0x222222)
            )
            .build()

        val segments = assertNotNull(extras.getParcelableArray(OngoingExtrasBuilder.KEY_PROGRESS_SEGMENTS))
        val first = segments[0] as Bundle
        val second = segments[1] as Bundle
        val third = segments[2] as Bundle

        assertEquals(0.0f, first.getFloat(OngoingExtrasBuilder.KEY_SEGMENT_START))
        assertEquals(0x111111, first.getInt(OngoingExtrasBuilder.KEY_SEGMENT_COLOR))
        assertTrue(first.containsKey(OngoingExtrasBuilder.KEY_SEGMENT_ICON))
        assertEquals(0.34f, second.getFloat(OngoingExtrasBuilder.KEY_SEGMENT_START))
        assertEquals(0x222222, second.getInt(OngoingExtrasBuilder.KEY_SEGMENT_COLOR))
        assertEquals(0.67f, third.getFloat(OngoingExtrasBuilder.KEY_SEGMENT_START))
        assertEquals(0x333333, third.getInt(OngoingExtrasBuilder.KEY_SEGMENT_COLOR))
    }

    @Test
    fun `progress segment rejects invalid start position`() {
        assertFailsWith<IllegalArgumentException> {
            ProgressSegment(startPosition = 1.2f, color = 0x111111)
        }
        assertFailsWith<IllegalArgumentException> {
            ProgressSegment(startPosition = Float.NaN, color = 0x111111)
        }
    }

    @Test
    fun `capsule config rejects unknown Samsung priority`() {
        val context = RuntimeEnvironment.getApplication()

        assertFailsWith<IllegalArgumentException> {
            CapsuleConfig(
                layout = RemoteViews(context.packageName, android.R.layout.simple_list_item_1),
                bgStartColor = 0x111111,
                bgEndColor = 0x222222,
                priority = "high"
            )
        }
    }

    @Test
    fun `style and action primary set are written by default`() {
        val extras = OngoingExtrasBuilder().build()

        assertEquals(OngoingExtrasBuilder.STYLE_BOTH, extras.getInt(OngoingExtrasBuilder.KEY_STYLE))
        assertEquals(1, extras.getInt(OngoingExtrasBuilder.KEY_ACTION_PRIMARY_SET))
        assertTrue(extras.getBoolean(OngoingExtrasBuilder.KEY_SHOW_SMALL_ICON))
    }
}
