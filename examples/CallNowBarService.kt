package com.nowbar.examples

import android.app.PendingIntent
import android.content.Intent
import androidx.core.graphics.drawable.IconCompat
import com.nowbar.api.NowBarConfig
import com.nowbar.api.cards.CallCard
import com.nowbar.api.cards.NowBarCard
import com.nowbar.api.service.NowBarForegroundService

class CallNowBarService : NowBarForegroundService() {

    private val config = NowBarConfig(
        channelId = "calls",
        channelName = "Calls"
    )

    override fun onCreateCard(): NowBarCard = buildCard(incoming = true)

    override fun onUpdateCard(): NowBarCard = buildCard(incoming = false)

    private fun buildCard(incoming: Boolean): CallCard =
        CallCard.Builder.create(
            title = "Call",
            icon = IconCompat.createWithResource(this, R.drawable.ic_call),
            callerName = "Alex"
        )
            .isIncoming(incoming)
            .callerNumber("+15551234567")
            .largeIcon(IconCompat.createWithResource(this, R.drawable.ic_call))
            .answerAction(serviceIntent("call.ANSWER", 1))
            .declineAction(serviceIntent("call.DECLINE", 2))
            .hangupAction(serviceIntent("call.HANGUP", 3))
            .tapAction(openAppIntent())
            .build()

    private fun serviceIntent(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            this,
            requestCode,
            Intent(this, javaClass).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun openAppIntent(): PendingIntent {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: Intent()
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
