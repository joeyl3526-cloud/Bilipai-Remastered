package com.android.purebilibili.core.ui.components

import androidx.compose.ui.graphics.Color

/** 等级类徽章的统一配色入口，集中维护避免多处硬编码漂移。 */
object BadgeLevelPalette {

    /** 用户等级 LV0~LV6+ 的兜底色阶（官方徽章资源不可用时使用）。 */
    fun userLevel(level: Int): Color = when {
        level >= 6 -> Color(0xFFF04444)
        level >= 5 -> Color(0xFFFF7A45)
        level >= 4 -> Color(0xFFFF8B5A)
        level >= 3 -> Color(0xFFFF9C6E)
        level >= 2 -> Color(0xFFFFAE84)
        else -> Color(0xFFA7ADB8)
    }

    /** 粉丝勋章按勋章等级的强调色阶。 */
    fun fansMedal(level: Int): Color = when {
        level >= 30 -> Color(0xFFE67A2B)
        level >= 20 -> Color(0xFFD9963B)
        level >= 10 -> Color(0xFFCFA657)
        else -> Color(0xFFB6B6B6)
    }
}
