package com.example.nowbar

import androidx.core.graphics.drawable.IconCompat
import com.nowbar.api.NowBarConfig
import com.nowbar.api.cards.MetricCard
import com.nowbar.api.notification.LiveUpdateMetric
import com.nowbar.api.notification.LiveUpdateMetricStyle
import com.nowbar.api.notification.LiveUpdateMetricTimeFormat
import com.nowbar.api.notification.LiveUpdateMetricValue
import com.nowbar.api.notification.LiveUpdateSemanticStyle
import com.nowbar.api.service.NowBarForegroundService
import java.time.Instant
import kotlin.time.Duration.Companion.minutes

class MetricNowBarService : NowBarForegroundService() {

    private val config = NowBarConfig(
        channelId = "delivery_metrics",
        channelName = "Delivery metrics"
    )

    override fun onCreateCard() = card()

    override fun onUpdateCard() = card()

    private fun card(): MetricCard =
        MetricCard.Builder.create(
            title = "Delivery",
            icon = IconCompat.createWithResource(this, R.drawable.ic_delivery),
            metricStyle = LiveUpdateMetricStyle(
                metrics = listOf(
                    LiveUpdateMetric(
                        label = "ETA",
                        value = LiveUpdateMetricValue.Timer(
                            endTime = Instant.ofEpochMilli(System.currentTimeMillis() + 8.minutes.inWholeMilliseconds),
                            format = LiveUpdateMetricTimeFormat.CHRONOMETER
                        ),
                        semanticStyle = LiveUpdateSemanticStyle.INFO
                    ),
                    LiveUpdateMetric(
                        label = "Dist",
                        value = LiveUpdateMetricValue.FixedFloat(1.7f, unit = "km", maxFractionDigits = 1),
                        semanticStyle = LiveUpdateSemanticStyle.SAFE
                    )
                )
            )
        )
            .primaryText("Courier is nearby")
            .secondaryText("MetricStyle on Android 17, standard ongoing notification elsewhere")
            .shortCriticalText("1.7 km")
            .semanticStyle(LiveUpdateSemanticStyle.INFO)
            .build()
}
