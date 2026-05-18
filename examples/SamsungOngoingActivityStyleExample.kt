package com.example.nowbar

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.drawable.Icon
import android.widget.RemoteViews
import com.nowbar.api.notification.SamsungOngoingActivityStyleBuilder

/**
 * Optional Samsung-native style path observed in Samsung Voice Recorder.
 *
 * Use this only when you already build a notification with platform
 * [Notification.Builder]. The SDK's default manager remains the portable path.
 */
object SamsungOngoingActivityStyleExample {
    fun buildRecordingNotification(
        context: Context,
        channelId: String,
        openAppIntent: PendingIntent,
        pauseAction: Notification.Action,
        stopAction: Notification.Action,
        cardView: RemoteViews
    ): Notification {
        val builder = Notification.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Voice recorder")
            .setContentText("Recording")
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_PROGRESS)

        SamsungOngoingActivityStyleBuilder()
            .chipIcon(Icon.createWithResource(context, android.R.drawable.ic_btn_speak_now))
            .chipBackgroundColor(0xFFD92323.toInt())
            .cardBackgroundColor(0xB3FCFCFF.toInt())
            .primaryInfo("Recording")
            .secondaryInfo("00:12")
            .customCardViewCenterUi(cardView)
            .actions(listOf(pauseAction, stopAction))
            .build()
            ?.let(builder::setStyle)

        return builder.build()
    }
}
