package com.android.purebilibili.feature.video.ui.overlay

internal fun shouldPollFullscreenPlayerProgress(
    playerExists: Boolean,
    hostLifecycleStarted: Boolean
): Boolean {
    return playerExists && hostLifecycleStarted
}

internal fun resolveFullscreenPlayerPollingIntervalMs(
    isPlaying: Boolean,
    showControls: Boolean,
    isSeekingGesture: Boolean
): Long {
    // [性能] 控制层隐藏时进度消费者几乎全部隐藏，tick 只剩主体的检查性重组；
    // showControls 变化本身是 LaunchedEffect key，切回可见瞬间会重启 effect 并
    // 首迭代立即全量同步——隐藏档放宽不会造成任何可见的进度滞后。
    if (showControls || isSeekingGesture) return 100L
    return if (isPlaying) 1_000L else 2_000L
}

internal fun shouldAutoHideFullscreenControls(
    showControls: Boolean,
    gestureMode: FullscreenGestureMode,
    isPlaying: Boolean
): Boolean {
    return showControls &&
        gestureMode == FullscreenGestureMode.None &&
        isPlaying
}
