package com.nowbar.examples

import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.core.graphics.drawable.IconCompat
import com.nowbar.api.NowBarConfig
import com.nowbar.api.cards.NowBarCard
import com.nowbar.api.cards.TimerCard
import com.nowbar.api.service.NowBarForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class TimerNowBarService : NowBarForegroundService() {

    companion object {
        private const val CHANNEL_ID = "timer_example_channel"
        private const val CHANNEL_NAME = "Timer Example"
        private const val NOTIFICATION_ID = 201
        private val TOTAL_DURATION = 5.minutes
    }

    private val config = NowBarConfig(
        channelId = CHANNEL_ID,
        channelName = CHANNEL_NAME,
        channelDescription = "Foreground-service timer example",
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
            card = createCard(TOTAL_DURATION),
            foregroundServiceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )

        job?.cancel()
        job = scope.launch {
            var remainingSeconds = TOTAL_DURATION.inWholeSeconds

            while (remainingSeconds > 0) {
                delay(1.seconds)
                remainingSeconds--

                updateNowBar(
                    config = config,
                    card = createCard(remainingSeconds.seconds)
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

    override fun onCreateCard(): NowBarCard = createCard(TOTAL_DURATION)

    override fun onUpdateCard(): NowBarCard = createCard(TOTAL_DURATION)

    private fun createCard(remaining: Duration): TimerCard {
        return TimerCard(
            title = "Timer",
            icon = IconCompat.createWithResource(this, android.R.drawable.ic_menu_recent_history),
            totalDuration = TOTAL_DURATION,
            remainingDuration = remaining,
            isCountDown = true,
            accentColor = 0xFF1976D2.toInt()
        )
    }
}
