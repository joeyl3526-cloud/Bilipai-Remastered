package com.android.purebilibili.feature.audio.screen

import com.android.purebilibili.feature.video.player.PlayMode
import kotlin.math.abs

internal data class MusicLyricFocusStyle(
    val blurRadiusDp: Int,
    val alphaPercent: Int
)

internal fun resolveMusicPagerIndicatorPosition(
    currentPage: Int,
    currentPageOffsetFraction: Float
): Float = (currentPage + currentPageOffsetFraction).coerceIn(0f, 1f)

internal fun resolveMusicPlayModeIndex(mode: PlayMode): Int = when (mode) {
    PlayMode.SEQUENTIAL -> 0
    PlayMode.SHUFFLE -> 1
    PlayMode.REPEAT_ONE -> 2
    PlayMode.REPEAT_ALL -> 3
}

internal fun resolveMusicPlayMode(index: Int): PlayMode = when (index) {
    1 -> PlayMode.SHUFFLE
    2 -> PlayMode.REPEAT_ONE
    3 -> PlayMode.REPEAT_ALL
    else -> PlayMode.SEQUENTIAL
}

internal fun resolveMusicLyricsBlurEnabled(
    sdkInt: Int,
    effectsEnabled: Boolean,
    reduceMotion: Boolean
): Boolean = sdkInt >= 31 && effectsEnabled && !reduceMotion

internal fun resolveMusicLyricFocusStyle(
    lineIndex: Int,
    currentIndex: Int,
    blurEnabled: Boolean
): MusicLyricFocusStyle {
    val distance = abs(lineIndex - currentIndex)
    // 除当前句外，所有歌词统一使用相邻句的模糊样式：不透明度 66% + 模糊半径 1dp
    val alphaPercent = if (distance == 0) 100 else 66
    val blurRadiusDp = if (!blurEnabled || distance == 0) 0 else 1
    return MusicLyricFocusStyle(blurRadiusDp, alphaPercent)
}

internal fun resolveMusicLiquidGlassEnabled(
    sdkInt: Int,
    effectsEnabled: Boolean,
    isAppInBackground: Boolean,
    reduceMotion: Boolean
): Boolean {
    return effectsEnabled &&
        sdkInt >= 33 &&
        !isAppInBackground &&
        !reduceMotion
}
