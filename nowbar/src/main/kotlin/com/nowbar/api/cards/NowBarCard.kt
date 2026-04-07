package com.nowbar.api.cards

import android.app.PendingIntent
import androidx.core.graphics.drawable.IconCompat

enum class CardType {
    TIMER, MEDIA, NAVIGATION, WORKOUT, CALL, STOPWATCH, CUSTOM
}

sealed class NowBarCard(
    open val type: CardType,
    open val title: String,
    open val icon: IconCompat,
    open val accentColor: Int? = null,
    open val tapAction: PendingIntent? = null,
    open val chipText: String? = null
) {
    abstract fun toPrimaryInfo(): String
    abstract fun toSecondaryInfo(): String

    open fun toNowBarSecondaryInfo(): String? = null

    open fun toProgress(): Int? = null

    open fun toProgressMax(): Int = 100

    open fun toChipText(): String? =
        chipText ?: toNowBarSecondaryInfo() ?: toSecondaryInfo().takeIf { it.isNotBlank() }

    open fun toChipIcon(): IconCompat = icon

    open fun toChipBackgroundColor(): Int? = accentColor

    open fun toNowBarIcon(): IconCompat? = null

    open fun toSecondIcon(): IconCompat? = null

    open fun toNowBarPrimaryInfo(): String? = null
}
