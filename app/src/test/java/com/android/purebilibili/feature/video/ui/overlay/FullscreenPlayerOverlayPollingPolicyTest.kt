package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FullscreenPlayerOverlayPollingPolicyTest {

    @Test
    fun pollingDisabled_withoutPlayer() {
        assertFalse(
            shouldPollFullscreenPlayerProgress(
                playerExists = false,
                hostLifecycleStarted = true
            )
        )
    }

    @Test
    fun pollingEnabled_withPlayerAndStartedHost() {
        assertTrue(
            shouldPollFullscreenPlayerProgress(
                playerExists = true,
                hostLifecycleStarted = true
            )
        )
    }

    @Test
    fun pollingDisabled_whenHostLifecycleStopped() {
        assertFalse(
            shouldPollFullscreenPlayerProgress(
                playerExists = true,
                hostLifecycleStarted = false
            )
        )
    }

    @Test
    fun pausedPlayback_hiddenControls_usesLongerInterval() {
        // [性能] 隐藏档放宽（1s/2s）：控制层隐藏时消费者几乎全部隐藏，
        // 可见性切换即 effect key 重启并立即同步，无可见进度滞后。
        assertEquals(
            2_000L,
            resolveFullscreenPlayerPollingIntervalMs(
                isPlaying = false,
                showControls = false,
                isSeekingGesture = false
            )
        )
        assertEquals(
            1_000L,
            resolveFullscreenPlayerPollingIntervalMs(
                isPlaying = true,
                showControls = false,
                isSeekingGesture = false
            )
        )
    }

    @Test
    fun visibleControlsOrSeeking_keepsFastInterval() {
        assertEquals(
            100L,
            resolveFullscreenPlayerPollingIntervalMs(
                isPlaying = true,
                showControls = true,
                isSeekingGesture = false
            )
        )
        assertEquals(
            100L,
            resolveFullscreenPlayerPollingIntervalMs(
                isPlaying = false,
                showControls = false,
                isSeekingGesture = true
            )
        )
    }

    @Test
    fun foregroundResumeRebindsFullscreenSurface_onlyWhenViewAndPlayerExist() {
        assertTrue(
            shouldRebindFullscreenSurfaceOnResume(
                hasPlayerView = true,
                hasPlayer = true
            )
        )
        assertFalse(
            shouldRebindFullscreenSurfaceOnResume(
                hasPlayerView = false,
                hasPlayer = true
            )
        )
        assertFalse(
            shouldRebindFullscreenSurfaceOnResume(
                hasPlayerView = true,
                hasPlayer = false
            )
        )
    }

    @Test
    fun pausedPlayback_keepsFullscreenControlsVisible() {
        assertFalse(
            shouldAutoHideFullscreenControls(
                showControls = true,
                gestureMode = FullscreenGestureMode.None,
                isPlaying = false
            )
        )
    }

    @Test
    fun activePlayback_stillAutoHidesFullscreenControlsWithoutGesture() {
        assertTrue(
            shouldAutoHideFullscreenControls(
                showControls = true,
                gestureMode = FullscreenGestureMode.None,
                isPlaying = true
            )
        )
    }
}
