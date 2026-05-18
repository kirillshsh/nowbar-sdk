@file:Suppress("unused")

package com.nowbar.examples

import android.app.PendingIntent
import android.content.Context
import android.graphics.drawable.Icon
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nowbar.api.notification.ProgressSegment
import com.nowbar.api.notification.SamsungNowBarGroupSummaryBuilder
import com.nowbar.api.notification.SamsungNowBarGroupSummarySpec
import com.nowbar.api.notification.SamsungOngoingActivityDumpExtras
import com.nowbar.api.notification.SamsungOngoingActivityProgress
import com.nowbar.api.notification.SamsungOngoingActivityText
import com.nowbar.api.notification.SamsungOngoingActivityViews
import com.nowbar.api.notification.SamsungOngoingActivityVisuals
import com.nowbar.api.notification.SamsungRemoteAppConfig

/**
 * Minimal dump-parity example for a Google Finance-like Now Bar notification.
 *
 * This deliberately uses NotificationCompat directly so the sample can show the exact
 * summary + child group topology observed in the Samsung AOD service dumps.
 */
object SamsungDumpNowBarExample {
    private const val CHANNEL_ID = "google_finance_nowbar_ongoing_channel"
    private const val CHILD_ID = 3115

    fun postFinanceLikeCard(
        context: Context,
        smallIconResId: Int,
        appIcon: Icon,
        openAppIntent: PendingIntent,
        nowBarView: RemoteViews,
        expandedView: RemoteViews
    ) {
        val groupKey = SamsungNowBarGroupSummaryBuilder.GOOGLE_FINANCE_GROUP_KEY
        val remoteApp = SamsungRemoteAppConfig(
            name = "Google Finance",
            icon = appIcon,
            pendingIntent = openAppIntent
        )

        val summary = SamsungNowBarGroupSummaryBuilder.build(
            context = context,
            spec = SamsungNowBarGroupSummarySpec(
                channelId = CHANNEL_ID,
                groupKey = groupKey,
                summaryNotificationId = SamsungNowBarGroupSummaryBuilder.GOOGLE_FINANCE_SUMMARY_ID,
                title = "Google Finance",
                smallIconResId = smallIconResId,
                remoteApp = remoteApp,
                substName = "Google Finance"
            )
        )

        val childExtras = SamsungOngoingActivityDumpExtras.build(
            remoteApp = remoteApp,
            text = SamsungOngoingActivityText(
                title = "Google Finance",
                primaryInfo = "AAPL",
                secondaryInfo = "189.98 · +1.2%",
                nowBarPrimaryInfo = "AAPL 189.98",
                nowBarSecondaryInfo = "+1.2% today",
                notificationPrimaryInfo = "AAPL",
                notificationSecondaryInfo = "189.98 · +1.2%",
                primaryAction = 0
            ),
            views = SamsungOngoingActivityViews(
                nowBarRemoteView = nowBarView,
                expandedRemoteView = expandedView
            ),
            visuals = SamsungOngoingActivityVisuals(
                chipIcon = appIcon,
                ongoingActivityChipIcon = appIcon,
                chipBackgroundColor = 0xff4285f4.toInt(),
                nowBarExpandableType = 0
            ),
            progress = SamsungOngoingActivityProgress(
                current = 65,
                max = 100,
                color = 0xff4285f4.toInt(),
                segmentIcon = appIcon,
                segments = listOf(
                    ProgressSegment(0.0f, 0xff4285f4.toInt(), appIcon),
                    ProgressSegment(0.65f, 0xff34a853.toInt())
                )
            ),
            substName = "Google Finance"
        )

        val child = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(smallIconResId)
            .setContentTitle("Google Finance")
            .setContentText("AAPL 189.98 · +1.2%")
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setGroup(groupKey)
            .addExtras(childExtras)
            .build()

        NotificationManagerCompat.from(context).notify(
            SamsungNowBarGroupSummaryBuilder.GOOGLE_FINANCE_SUMMARY_ID,
            summary
        )
        NotificationManagerCompat.from(context).notify(CHILD_ID, child)
    }
}
