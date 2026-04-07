package com.nowbar.examples

import android.os.SystemClock
import android.widget.RemoteViews
import com.nowbar.api.notification.ChronometerConfig
import com.nowbar.api.notification.OngoingExtrasBuilder

/**
 * Example: Setting up a live chronometer in the Now Bar.
 *
 * Samsung Clock uses RemoteViews with a Chronometer widget for real-time
 * stopwatch/timer display in the collapsed Now Bar view.
 */
object ChronometerExample {

    /**
     * Creates a stopwatch-style chronometer that counts up from a given base time.
     */
    fun createStopwatchChronometer(
        packageName: String,
        layoutResId: Int,
        chronometerViewId: Int,
        elapsedMillis: Long
    ): ChronometerConfig {
        val remoteViews = RemoteViews(packageName, layoutResId).apply {
            setViewVisibility(chronometerViewId, android.view.View.VISIBLE)
            setChronometerCountDown(chronometerViewId, false)
            setChronometer(
                chronometerViewId,
                SystemClock.elapsedRealtime() - elapsedMillis,
                null,
                true
            )
        }
        return ChronometerConfig(
            remoteView = remoteViews,
            tag = "stopwatch_ongoing_activity_chronometer",
            viewPosition = 1,
            nowBarPosition = 1
        )
    }

    /**
     * Creates a timer-style chronometer that counts down.
     */
    fun createTimerChronometer(
        packageName: String,
        layoutResId: Int,
        chronometerViewId: Int,
        remainingMillis: Long
    ): ChronometerConfig {
        val remoteViews = RemoteViews(packageName, layoutResId).apply {
            setViewVisibility(chronometerViewId, android.view.View.VISIBLE)
            setChronometerCountDown(chronometerViewId, true)
            setChronometer(
                chronometerViewId,
                SystemClock.elapsedRealtime() + remainingMillis,
                null,
                true
            )
        }
        return ChronometerConfig(
            remoteView = remoteViews,
            tag = "timer_ongoing_activity_chronometer",
            viewPosition = 1,
            nowBarPosition = 1
        )
    }

    /**
     * Usage with OngoingExtrasBuilder:
     */
    fun applyToBuilder(builder: OngoingExtrasBuilder, config: ChronometerConfig) {
        builder.setChronometerConfig(config)
    }
}
