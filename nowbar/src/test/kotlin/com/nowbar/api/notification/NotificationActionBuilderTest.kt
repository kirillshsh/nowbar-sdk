package com.nowbar.api.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NotificationActionBuilderTest {

    @Test
    fun `broadcast pending intent includes Samsung caller id extra`() {
        val context = RuntimeEnvironment.getApplication()
        val pendingIntent = NotificationActionBuilder(context)
            .createBroadcastIntent(SamsungHealthActions.PAUSE, callerId = "demo-caller")

        val savedIntent = shadowOf(pendingIntent).savedIntent

        assertEquals(SamsungHealthActions.PAUSE, savedIntent.action)
        assertEquals(context.packageName, savedIntent.`package`)
        assertEquals("demo-caller", savedIntent.getStringExtra(SamsungHealthActions.CALLER_ID_KEY))
    }

    @Test
    fun `applyActions caps actions for Now Bar and fallback notifications`() {
        val context = RuntimeEnvironment.getApplication()
        val builder = Notification.Builder(context, "test")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Actions")
        val actions = listOf("Pause", "Resume", "Stop", "Next").mapIndexed { index, title ->
            Notification.Action.Builder(
                Icon.createWithResource(context, android.R.drawable.ic_dialog_info),
                title,
                PendingIntent.getBroadcast(
                    context,
                    index,
                    Intent("test.$title"),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            ).build()
        }

        NotificationActionBuilder(context).applyActions(
            builder = builder,
            actions = actions,
            hasNowBarFeature = true
        )

        val notification = builder.build()
        assertEquals(NowBarActionLimits.MAX_ACTIONS, notification.actions.size)
        assertEquals(listOf("Pause", "Resume", "Stop"), notification.actions.map { it.title.toString() })
    }

    @Test
    fun `ActionConfig platform action preserves id and semantic metadata`() {
        val context = RuntimeEnvironment.getApplication()
        val action = ActionConfig(
            id = "stop",
            text = "Stop",
            iconRes = android.R.drawable.ic_dialog_info,
            intent = PendingIntent.getBroadcast(
                context,
                9,
                Intent("test.STOP"),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            ),
            semantic = ActionSemantic.STOP
        )

        val platformAction = action.toPlatformAction(context)
        val state = platformAction.toAndroidActionState()

        assertEquals("stop", platformAction.extras.getString(NowBarActionExtras.ACTION_ID))
        assertEquals("STOP", platformAction.extras.getString(NowBarActionExtras.ACTION_SEMANTIC))
        assertEquals("stop", state.id)
        assertEquals(ActionSemantic.STOP, state.semantic)
        assertFalse(state.textOnly)
        assertFalse(state.disabled)
    }
}
