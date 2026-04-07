package com.nowbar.api.notification

/**
 * All discovered Samsung Now Bar (One UI 7+) notification extras keys.
 *
 * Extracted from decompiled Samsung Health APK:
 * com.samsung.android.app.shealth.tracker.sport.livetracker.nowbar.SportOngoingNotificationHelper
 */
object NowBarExtrasKeys {

    // ── Chip Configuration ──────────────────────────────────────────────

    /** Chip background color (Int, @ColorInt). */
    const val CHIP_BG_COLOR = "android.ongoingActivityNoti.chipBgColor"

    /** Chip icon (Parcelable, android.graphics.drawable.Icon). */
    const val CHIP_ICON = "android.ongoingActivityNoti.chipIcon"

    /** Expanded chip text label (String). */
    const val CHIP_EXPANDED_TEXT = "android.ongoingActivityNoti.chipExpandedText"

    // ── Content / Expand View ───────────────────────────────────────────

    /** Primary info text shown in expanded Now Bar view (String). */
    const val PRIMARY_INFO = "android.ongoingActivityNoti.primaryInfo"

    /** Secondary info text shown in expanded Now Bar view (String). */
    const val SECONDARY_INFO = "android.ongoingActivityNoti.secondaryInfo"

    /** Now Bar-specific secondary info, separate from notification secondary (String). */
    const val NOWBAR_SECONDARY_INFO = "android.ongoingActivityNoti.nowbarSecondaryInfo"

    /** Whether to show the small icon in the expanded view (Boolean). */
    const val SHOW_SMALL_ICON = "android.showSmallIcon"

    // ── Style & Action ──────────────────────────────────────────────────

    /**
     * Display style (Int).
     * - 0 = NOTIFICATION_ONLY (no Now Bar chip)
     * - 1 = BOTH (notification + Now Bar chip)
     */
    const val STYLE = "android.ongoingActivityNoti.style"

    /** Action type for the Now Bar chip interaction (Int). 1 = button text style. */
    const val ACTION_TYPE = "android.ongoingActivityNoti.actionType"

    // ── Progress ────────────────────────────────────────────────────────

    /** Current progress value, 0..progressMax (Int). */
    const val PROGRESS = "android.ongoingActivityNoti.progress"

    /** Maximum progress value, typically 100 (Int). */
    const val PROGRESS_MAX = "android.ongoingActivityNoti.progressMax"

    /** Array of progress segment bundles (Parcelable[], each element is a Bundle). */
    const val PROGRESS_SEGMENTS = "android.ongoingActivityNoti.progressSegments"

    /** Progress bar filled color (Int, @ColorInt). Put on the ongoing bundle, not segment. */
    const val PROGRESS_COLOR = "android.ongoingActivityNoti.progressSegments.progressColor"

    /** Segment track color within a segment bundle (Int, @ColorInt). */
    const val PROGRESS_SEGMENT_COLOR = "android.ongoingActivityNoti.progressSegments.segmentColor"

    /** Segment icon within a segment bundle (Parcelable, Icon). */
    const val PROGRESS_SEGMENT_ICON = "android.ongoingActivityNoti.progressSegments.icon"

    /** Segment start position within a segment bundle (Float, 0.0..1.0). */
    const val PROGRESS_SEGMENT_START = "android.ongoingActivityNoti.progressSegments.segmentStart"

    /** Now Bar-specific icon, displayed in the Now Bar view (Parcelable, Icon). */
    const val NOWBAR_ICON = "android.ongoingActivityNoti.nowbarIcon"

    /** Secondary icon shown alongside the chip or in expanded Now Bar view (Parcelable, Icon). */
    const val SECOND_ICON = "android.ongoingActivityNoti.secondIcon"

    /** Now Bar-specific primary info text, separate from notification primaryInfo (String). */
    const val NOWBAR_PRIMARY_INFO = "android.ongoingActivityNoti.nowbarPrimaryInfo"

    // ── Chronometer (standard Android extras) ───────────────────────────

    /** Whether to use countdown mode for the chronometer (Boolean). */
    const val CHRONOMETER_COUNT_DOWN = "android.chronometerCountDown"

    /** Whether to show the chronometer (Boolean). */
    const val SHOW_CHRONOMETER = "android.showChronometer"

    // ── Style Constants ─────────────────────────────────────────────────

    object Style {
        /** Display as notification only, no Now Bar chip. */
        const val NOTIFICATION_ONLY = 0

        /** Display both notification and Now Bar chip. */
        const val BOTH = 1
    }

    object ActionType {
        /** Button with text style. */
        const val BUTTON_TEXT = 1
    }

    // ── Notification Actions ────────────────────────────────────────────

    object Actions {
        const val DELETE_NOTIFICATION = "com.samsung.android.app.shealth.tracker.action.DELETE_NOTIFICATION"
        const val NEXT = "com.samsung.android.app.shealth.tracker.action.NEXT"
        const val PAUSE = "com.samsung.android.app.shealth.tracker.action.PAUSE"
        const val RESUME = "com.samsung.android.app.shealth.tracker.action.RESUME"
        const val STOP = "com.samsung.android.app.shealth.tracker.action.STOP"
    }

    // ── Notification Channel ────────────────────────────────────────────

    object Channels {
        /** Sport/workout notification channel (primary for Now Bar). */
        const val SPORT = "channel.03.sport"
        const val FOOD = "channel.05.food"
        const val GLOBAL_CHALLENGES = "channel.11.global.challenges"
        const val PASSWORD = "channel.16.password"
        const val ENERGY_SCORE = "channel.046.energy_score"
        const val MEDICATIONS = "channel.062.medications"
        const val MEDICATIONS_STRONG = "channel.063.medications.strong"
        const val HEALTH_RECORDS = "channel.066.health_records"
        const val CHALLENGES = "channel.100.challenges"
        const val REWARDS = "channel.141.rewards"
    }

    // ── Feature Detection ───────────────────────────────────────────────

    object Features {
        /** Samsung One UI 7+ Now Bar support. */
        const val SAMSUNG_NOWBAR = "com.samsung.feature.nowbar"

        /** OnePlus/Oppo ambient alerts support. */
        const val OPPO_AMBIENT_ALERTS = "com.oplus.software.feature.ambient_alerts"

        /** Google Pixel Ambient Data support (requires API 35+). */
        const val GOOGLE_AMBIENT_DATA = "com.google.android.feature.AMBIENT_DATA"
    }

    // ── Foreground Service Types ────────────────────────────────────────

    object ForegroundServiceTypes {
        /** Workout tracking service: health|connectedDevice|location|mediaPlayback */
        const val WORKOUT_TRACKING = "health|connectedDevice|location|mediaPlayback"

        /** Basic health service: health|connectedDevice */
        const val BASIC_HEALTH = "health|connectedDevice"
    }

    // ── Google Ambient Intents ──────────────────────────────────────────

    object GoogleAmbient {
        const val ACTION_SETTINGS_CHANGED = "com.google.android.ambient.intent.action.AMBIENT_DATA_SETTINGS_CHANGED"
    }

    // ── Notification Categories (standard Android, relevant for Now Bar) ─

    object Categories {
        const val WORKOUT = "workout"
        const val STOPWATCH = "stopwatch"
        const val NAVIGATION = "navigation"
        const val CALL = "call"
        const val TRANSPORT = "transport"
        const val PROGRESS = "progress"
        const val ALARM = "alarm"
        const val LOCATION_SHARING = "location_sharing"
    }
}