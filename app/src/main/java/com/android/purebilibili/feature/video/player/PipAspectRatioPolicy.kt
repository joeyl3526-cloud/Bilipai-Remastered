package com.android.purebilibili.feature.video.player

import android.util.Rational
import androidx.media3.common.Player

/**
 * 画中画窗口宽高比策略：横屏视频用横屏画中画（16:9），竖屏视频用竖屏画中画（9:16）。
 * 视频尺寸未知（未解码出第一帧）时回退 16:9。
 */
internal fun resolvePipAspectRatio(player: Player?): Rational {
    val size = player?.videoSize ?: return Rational(16, 9)
    if (size.width <= 0 || size.height <= 0) return Rational(16, 9)
    return if (size.height > size.width) Rational(9, 16) else Rational(16, 9)
}
