package com.nowbar.examples

import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.core.graphics.drawable.IconCompat
import com.nowbar.api.NowBarConfig
import com.nowbar.api.cards.NavigationCard
import com.nowbar.api.cards.NowBarCard
import com.nowbar.api.service.NowBarForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class NavigationNowBarService : NowBarForegroundService() {

    companion object {
        private const val CHANNEL_ID = "navigation_example_channel"
        private const val CHANNEL_NAME = "Navigation Example"
        private const val NOTIFICATION_ID = 203
        private const val ACCENT_COLOR = 0xFF4285F4.toInt()
    }

    private val config = NowBarConfig(
        channelId = CHANNEL_ID,
        channelName = CHANNEL_NAME,
        channelDescription = "Foreground-service navigation example",
        notificationId = NOTIFICATION_ID
    )

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var job: Job? = null

    private data class Step(
        val direction: String,
        val distance: String,
        val eta: String,
        val turnIconRes: Int
    )

    private val route = listOf(
        Step("Turn right onto Main St", "300 m", "15:42", android.R.drawable.ic_menu_directions),
        Step("Turn left onto Oak Ave", "1.2 km", "15:44", android.R.drawable.ic_menu_directions),
        Step("Continue straight", "800 m", "15:47", android.R.drawable.ic_menu_directions),
        Step("Turn right onto Park Rd", "200 m", "15:50", android.R.drawable.ic_menu_directions),
        Step("Arrive at destination", "50 m", "15:51", android.R.drawable.ic_menu_myplaces)
    )

    override fun onCreate() {
        super.onCreate()
        createNowBarChannel(config)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startNowBar(
            config = config,
            card = createCard(route.first()),
            foregroundServiceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        )

        job?.cancel()
        job = scope.launch {
            for (step in route.drop(1)) {
                delay(5.seconds)
                updateNowBar(config = config, card = createCard(step))
            }

            delay(3.seconds)
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

    override fun onCreateCard(): NowBarCard = createCard(route.first())

    override fun onUpdateCard(): NowBarCard = createCard(route.first())

    private fun createCard(step: Step): NavigationCard {
        return NavigationCard.Builder.create(
            title = "Navigation",
            icon = IconCompat.createWithResource(this, android.R.drawable.ic_menu_mapmode),
            nextDirection = step.direction,
            distanceToTurn = step.distance
        )
            .eta(step.eta)
            .turnIcon(IconCompat.createWithResource(this, step.turnIconRes))
            .accentColor(ACCENT_COLOR)
            .chipText("${step.distance} - ${step.direction.substringBefore(" onto")}")
            .build()
    }
}
