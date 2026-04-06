package com.nowbar.api.notification

import android.graphics.drawable.Icon

data class ChipConfig(
    val icon: Icon? = null,
    val backgroundColor: Int? = null,
    val expandedText: String? = null
)