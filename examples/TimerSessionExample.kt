package com.nowbar.examples

import android.content.Context
import androidx.core.graphics.drawable.IconCompat
import com.nowbar.api.NowBarConfig
import com.nowbar.api.NowBarManager
import com.nowbar.api.NowBarSession
import com.nowbar.api.cards.TimerCard
import kotlin.time.Duration

class TimerSessionExample(
    private val context: Context
) {
    private val config = NowBarConfig(
        channelId = "timer_channel",
        channelName = "Timer",
        channelDescription = "Now Bar timer example"
    )

    private val icon by lazy {
        IconCompat.createWithResource(context, android.R.drawable.ic_menu_recent_history)
    }

    private val session: NowBarSession by lazy {
        NowBarManager.createNotificationChannel(context, config)
        NowBarManager.createSession(context, config)
    }

    fun start(total: Duration) {
        session.start(
            TimerCard(
                title = "Tea timer",
                icon = icon,
                totalDuration = total,
                remainingDuration = total,
                isCountDown = true,
                accentColor = 0xFFFF9800.toInt()
            )
        )
    }

    fun update(total: Duration, remaining: Duration) {
        session.update(
            TimerCard(
                title = "Tea timer",
                icon = icon,
                totalDuration = total,
                remainingDuration = remaining,
                isCountDown = true,
                accentColor = 0xFFFF9800.toInt()
            )
        )
    }

    fun hideSurfaceKeepNotification() {
        session.dismiss()
    }

    fun stop() {
        session.stop()
    }
}
