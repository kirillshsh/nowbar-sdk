package com.nowbar.api.cards

import androidx.core.graphics.drawable.IconCompat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class CardConstructorCompatibilityTest {

    private val icon: IconCompat
        get() = IconCompat.createWithResource(
            RuntimeEnvironment.getApplication(),
            android.R.drawable.ic_dialog_info
        )

    @Test
    fun `existing positional constructors still bind original fields`() {
        val timer = TimerCard("Timer", icon, null, null, null, 5.minutes, 3.minutes)
        val custom = CustomCard("Custom", icon, null, null, null, "Primary")
        val media = MediaCard("Song", icon, null, null, null, "Artist")
        val call = CallCard("Call", icon, null, null, null, "Alice")
        val navigation = NavigationCard("Nav", icon, null, null, null, "Turn right", "300 m")
        val workout = WorkoutCard("Run", icon, null, null, null, WorkoutType.RUNNING, 12.minutes)

        assertEquals(5.minutes, timer.totalDuration)
        assertEquals("Primary", custom.primaryText)
        assertEquals("Artist", media.artist)
        assertEquals("Alice", call.callerName)
        assertEquals(CallCardType.INCOMING, call.callType)
        assertEquals("Turn right", navigation.nextDirection)
        assertEquals(WorkoutType.RUNNING, workout.activityType)
    }
}
