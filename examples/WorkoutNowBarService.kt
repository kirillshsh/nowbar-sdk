package com.nowbar.examples

import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.core.graphics.drawable.IconCompat
import com.nowbar.api.NowBarConfig
import com.nowbar.api.cards.NowBarCard
import com.nowbar.api.cards.WorkoutCard
import com.nowbar.api.cards.WorkoutType
import com.nowbar.api.service.NowBarForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class WorkoutNowBarService : NowBarForegroundService() {

    companion object {
        private const val CHANNEL_ID = "workout_example_channel"
        private const val CHANNEL_NAME = "Workout Example"
        private const val NOTIFICATION_ID = 202
        private val TOTAL_DURATION = 10.minutes
        private const val ACCENT_COLOR = 0xFF0FCF6E.toInt()
    }

    private val config = NowBarConfig(
        channelId = CHANNEL_ID,
        channelName = CHANNEL_NAME,
        channelDescription = "Foreground-service workout example",
        notificationId = NOTIFICATION_ID
    )

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var job: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNowBarChannel(config)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startNowBar(
            config = config,
            card = createCard(0, 0.0, 120, 0, 0),
            foregroundServiceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH or
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        )

        job?.cancel()
        job = scope.launch {
            val totalSeconds = TOTAL_DURATION.inWholeSeconds

            for (elapsed in 1..totalSeconds) {
                delay(1.seconds)

                val distance = elapsed * 0.01
                val heartRate = Random.nextInt(120, 161)
                val calories = (elapsed * 0.15).toInt()
                val progress = ((elapsed * 100) / totalSeconds).toInt().coerceIn(0, 100)

                updateNowBar(
                    config = config,
                    card = createCard(
                        elapsedSeconds = elapsed,
                        distance = distance,
                        heartRate = heartRate,
                        calories = calories,
                        progress = progress
                    )
                )
            }

            stopNowBar()
            stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        job?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    override fun onCreateCard(): NowBarCard = createCard(0, 0.0, 120, 0, 0)

    override fun onUpdateCard(): NowBarCard = createCard(0, 0.0, 120, 0, 0)

    private fun createCard(
        elapsedSeconds: Long,
        distance: Double,
        heartRate: Int,
        calories: Int,
        progress: Int
    ): WorkoutCard {
        return WorkoutCard(
            title = "Running",
            icon = IconCompat.createWithResource(this, android.R.drawable.ic_menu_compass),
            activityType = WorkoutType.RUNNING,
            elapsed = elapsedSeconds.seconds,
            distance = distance,
            heartRate = heartRate,
            calories = calories,
            progress = progress,
            accentColor = ACCENT_COLOR,
            chipText = "${String.format("%.2f", distance)} km"
        )
    }
}
