package com.nowbar.api.notification

import android.graphics.drawable.Icon
import android.os.Bundle
import android.os.Parcelable

class OngoingExtrasBuilder {

    companion object {
        const val KEY_STYLE = "android.ongoingActivityNoti.style"
        const val KEY_CHIP_BG_COLOR = "android.ongoingActivityNoti.chipBgColor"
        const val KEY_CHIP_ICON = "android.ongoingActivityNoti.chipIcon"
        const val KEY_CHIP_EXPANDED_TEXT = "android.ongoingActivityNoti.chipExpandedText"
        const val KEY_PRIMARY_INFO = "android.ongoingActivityNoti.primaryInfo"
        const val KEY_SECONDARY_INFO = "android.ongoingActivityNoti.secondaryInfo"
        const val KEY_NOWBAR_SECONDARY_INFO = "android.ongoingActivityNoti.nowbarSecondaryInfo"
        const val KEY_ACTION_TYPE = "android.ongoingActivityNoti.actionType"
        const val KEY_PROGRESS = "android.ongoingActivityNoti.progress"
        const val KEY_PROGRESS_MAX = "android.ongoingActivityNoti.progressMax"
        const val KEY_PROGRESS_SEGMENTS = "android.ongoingActivityNoti.progressSegments"
        const val KEY_PROGRESS_COLOR = "android.ongoingActivityNoti.progressSegments.progressColor"
        const val KEY_SEGMENT_COLOR = "android.ongoingActivityNoti.progressSegments.segmentColor"
        const val KEY_SEGMENT_START = "android.ongoingActivityNoti.progressSegments.segmentStart"
        const val KEY_SEGMENT_ICON = "android.ongoingActivityNoti.progressSegments.icon"
        const val KEY_SHOW_SMALL_ICON = "android.showSmallIcon"

        const val STYLE_NOTIFICATION_ONLY = 0
        const val STYLE_BOTH = 1
        const val ACTION_TYPE_BUTTON_TEXT = 1
        const val MAX_PROGRESS = 100
    }

    private var style: Int = STYLE_BOTH
    private var chipConfig: ChipConfig? = null
    private var primaryInfo: String? = null
    private var secondaryInfo: String? = null
    private var nowbarSecondaryInfo: String? = null
    private var actionType: Int = ACTION_TYPE_BUTTON_TEXT
    private var progress: Int? = null
    private var progressMax: Int = MAX_PROGRESS
    private var progressColor: Int? = null
    private var segments: List<ProgressSegment> = emptyList()
    private var showSmallIcon: Boolean = true

    fun setStyle(style: Int): OngoingExtrasBuilder = apply {
        this.style = style
    }

    fun setChipConfig(config: ChipConfig): OngoingExtrasBuilder = apply {
        this.chipConfig = config
    }

    fun setPrimaryInfo(text: String): OngoingExtrasBuilder = apply {
        this.primaryInfo = text
    }

    fun setSecondaryInfo(text: String): OngoingExtrasBuilder = apply {
        this.secondaryInfo = text
    }

    fun setNowBarSecondaryInfo(text: String): OngoingExtrasBuilder = apply {
        this.nowbarSecondaryInfo = text
    }

    fun setActionType(type: Int): OngoingExtrasBuilder = apply {
        this.actionType = type
    }

    fun setProgress(current: Int, max: Int = MAX_PROGRESS): OngoingExtrasBuilder = apply {
        require(max > 0) { "Progress max must be positive" }
        this.progress = current.coerceIn(0, max)
        this.progressMax = max
    }

    fun setProgressColor(color: Int): OngoingExtrasBuilder = apply {
        this.progressColor = color
    }

    fun setProgressSegments(segments: List<ProgressSegment>): OngoingExtrasBuilder = apply {
        segments.forEach { segment ->
            require(segment.startPosition in 0f..1f) { "Segment startPosition must be in [0.0, 1.0]" }
        }
        this.segments = segments
    }

    fun setShowSmallIcon(show: Boolean): OngoingExtrasBuilder = apply {
        this.showSmallIcon = show
    }

    fun build(): Bundle {
        val bundle = Bundle()

        bundle.putInt(KEY_STYLE, style)
        bundle.putInt(KEY_ACTION_TYPE, actionType)
        bundle.putBoolean(KEY_SHOW_SMALL_ICON, showSmallIcon)

        chipConfig?.let { chip ->
            chip.icon?.let { bundle.putParcelable(KEY_CHIP_ICON, it) }
            chip.backgroundColor?.let { bundle.putInt(KEY_CHIP_BG_COLOR, it) }
            chip.expandedText?.let { bundle.putString(KEY_CHIP_EXPANDED_TEXT, it) }
        }

        primaryInfo?.let { bundle.putString(KEY_PRIMARY_INFO, it) }
        secondaryInfo?.let { bundle.putString(KEY_SECONDARY_INFO, it) }
        nowbarSecondaryInfo?.let { bundle.putString(KEY_NOWBAR_SECONDARY_INFO, it) }

        progress?.let { current ->
            bundle.putInt(KEY_PROGRESS, current)
            bundle.putInt(KEY_PROGRESS_MAX, progressMax)
        }

        progressColor?.let { bundle.putInt(KEY_PROGRESS_COLOR, it) }

        if (segments.isNotEmpty()) {
            val segmentBundles = segments.map { segment ->
                Bundle().apply {
                    putInt(KEY_SEGMENT_COLOR, segment.color)
                    putFloat(KEY_SEGMENT_START, segment.startPosition)
                    segment.icon?.let { putParcelable(KEY_SEGMENT_ICON, it) }
                }
            }.toTypedArray<Parcelable>()

            bundle.putParcelableArray(KEY_PROGRESS_SEGMENTS, segmentBundles)
        }

        return bundle
    }
}