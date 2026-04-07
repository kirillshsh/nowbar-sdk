package com.nowbar.examples

import android.app.PendingIntent
import android.content.Context
import android.widget.RemoteViews
import com.nowbar.api.notification.CapsuleConfig
import com.nowbar.api.notification.OngoingExtrasBuilder

/**
 * Example: Setting up a capsule widget for Samsung foldable cover screens.
 *
 * The capsule is displayed on the cover screen of Samsung foldable devices
 * (like Galaxy Z Flip/Fold) as a compact notification widget with gradient background.
 */
object CapsuleExample {

    /**
     * Creates a capsule config for a stopwatch with gradient purple background.
     * Colors from Samsung Clock: startColor = 0xFF6162E9, endColor = 0xFF859FFE.
     */
    fun createStopwatchCapsule(
        context: Context,
        capsuleLayoutResId: Int,
        tapIntent: PendingIntent
    ): CapsuleConfig {
        val layout = RemoteViews(context.packageName, capsuleLayoutResId)
        return CapsuleConfig(
            layout = layout,
            action = tapIntent,
            bgStartColor = 0xFF6162E9.toInt(),
            bgEndColor = 0xFF859FFE.toInt(),
            priority = "normal"
        )
    }

    /**
     * Creates a paused capsule with lower priority.
     */
    fun createPausedCapsule(
        context: Context,
        capsuleLayoutResId: Int,
        tapIntent: PendingIntent
    ): CapsuleConfig {
        val layout = RemoteViews(context.packageName, capsuleLayoutResId)
        return CapsuleConfig(
            layout = layout,
            action = tapIntent,
            bgStartColor = 0xFF6162E9.toInt(),
            bgEndColor = 0xFF859FFE.toInt(),
            priority = "low"
        )
    }

    /**
     * Usage with OngoingExtrasBuilder:
     */
    fun applyToBuilder(builder: OngoingExtrasBuilder, config: CapsuleConfig) {
        builder.setCapsuleConfig(config)
    }
}
