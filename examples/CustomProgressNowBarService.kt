package com.example.nowbar

import androidx.core.graphics.drawable.IconCompat
import com.nowbar.api.NowBarConfig
import com.nowbar.api.cards.CustomCard
import com.nowbar.api.notification.StylePoint
import com.nowbar.api.notification.StyleSegment
import com.nowbar.api.service.NowBarForegroundService

class CustomProgressNowBarService : NowBarForegroundService() {

    private val config = NowBarConfig(
        channelId = "rideshare",
        channelName = "Rideshare"
    )

    override fun onCreateCard() = card(progress = 12, status = "Finding driver")

    override fun onUpdateCard() = card(progress = 68, status = "Driver nearby")

    private fun card(progress: Int, status: String): CustomCard {
        val vehicleIcon = IconCompat.createWithResource(this, R.drawable.ic_car)
        return CustomCard.Builder.create("Ride", vehicleIcon, status)
            .secondaryText("Pickup in 3 min")
            .progressValue(progress)
            .progressSegments(
                listOf(
                    StyleSegment(length = 20, color = 0xFF8E8E93.toInt(), id = 1),
                    StyleSegment(length = 45, color = 0xFF34C759.toInt(), id = 2),
                    StyleSegment(length = 35, color = 0xFF007AFF.toInt(), id = 3)
                )
            )
            .progressPoints(
                listOf(
                    StylePoint(position = 20, color = 0xFFFFFFFF.toInt(), id = 1),
                    StylePoint(position = 65, color = 0xFFFFFFFF.toInt(), id = 2)
                )
            )
            .progressTrackerIcon(vehicleIcon)
            .progressStartIcon(IconCompat.createWithResource(this, R.drawable.ic_pickup))
            .progressEndIcon(IconCompat.createWithResource(this, R.drawable.ic_destination))
            .progressStyledByProgress()
            .chipText("3 min")
            .build()
    }
}
