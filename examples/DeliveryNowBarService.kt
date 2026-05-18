package com.nowbar.examples

import android.app.PendingIntent
import android.content.Intent
import androidx.core.graphics.drawable.IconCompat
import com.nowbar.api.NowBarConfig
import com.nowbar.api.cards.DeliveryCard
import com.nowbar.api.cards.DeliveryStatus
import com.nowbar.api.cards.NowBarCard
import com.nowbar.api.service.NowBarForegroundService
import kotlin.time.Duration.Companion.minutes

class DeliveryNowBarService : NowBarForegroundService() {

    private val config = NowBarConfig(
        channelId = "delivery",
        channelName = "Delivery updates"
    )

    override fun onCreateCard(): NowBarCard = buildCard(DeliveryStatus.INITIALIZING, progress = null)

    override fun onUpdateCard(): NowBarCard = buildCard(DeliveryStatus.EN_ROUTE, progress = 60)

    private fun buildCard(status: DeliveryStatus, progress: Int?): DeliveryCard =
        DeliveryCard.Builder.create(
            title = "Delivery",
            icon = IconCompat.createWithResource(this, R.drawable.ic_delivery),
            merchantName = "Bakery",
            status = status
        )
            .eta("10 min")
            .destination("Main St")
            .largeIcon(IconCompat.createWithResource(this, R.drawable.ic_delivery))
            .progress(progress)
            .chipWhenTimeMillis(System.currentTimeMillis() + 10.minutes.inWholeMilliseconds)
            .tapAction(openOrderIntent())
            .build()

    private fun openOrderIntent(): PendingIntent {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: Intent()
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
