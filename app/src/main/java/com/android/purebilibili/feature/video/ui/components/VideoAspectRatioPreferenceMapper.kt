package com.android.purebilibili.feature.video.ui.components

import com.android.purebilibili.core.store.FullscreenAspectRatio

internal fun FullscreenAspectRatio.toVideoAspectRatio(): VideoAspectRatio {
    return when (this) {
        FullscreenAspectRatio.FIT -> VideoAspectRatio.FIT
        FullscreenAspectRatio.FILL -> VideoAspectRatio.FILL
        FullscreenAspectRatio.RATIO_16_9 -> VideoAspectRatio.RATIO_16_9
        FullscreenAspectRatio.RATIO_4_3 -> VideoAspectRatio.RATIO_4_3
        FullscreenAspectRatio.STRETCH -> VideoAspectRatio.STRETCH
    }
}

internal fun VideoAspectRatio.toFullscreenAspectRatio(): FullscreenAspectRatio {
    return when (this) {
        VideoAspectRatio.FIT -> FullscreenAspectRatio.FIT
        VideoAspectRatio.FILL -> FullscreenAspectRatio.FILL
        VideoAspectRatio.RATIO_16_9 -> FullscreenAspectRatio.RATIO_16_9
        VideoAspectRatio.RATIO_4_3 -> FullscreenAspectRatio.RATIO_4_3
        VideoAspectRatio.STRETCH -> FullscreenAspectRatio.STRETCH
        // 9:16 是竖屏内容自动策略窗口，无独立全屏偏好，回落"适应"。
        VideoAspectRatio.RATIO_9_16 -> FullscreenAspectRatio.FIT
    }
}
