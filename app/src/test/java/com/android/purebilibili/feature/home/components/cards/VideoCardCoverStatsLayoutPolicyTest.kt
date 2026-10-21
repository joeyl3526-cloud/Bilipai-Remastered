package com.android.purebilibili.feature.home.components.cards

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoCardCoverStatsLayoutPolicyTest {

    @Test
    fun `primary stat badge reserves enough width for wan count`() {
        val width = resolveVideoCardPrimaryStatBadgeMinWidthDp("6.2万")

        assertEquals(58f, width, 0.0001f)
    }

    @Test
    fun `primary stat badge expands for longer formatted counts`() {
        val width = resolveVideoCardPrimaryStatBadgeMinWidthDp("123.4万")

        assertEquals(70f, width, 0.0001f)
    }

    @Test
    fun `compact cover stats reserves readable width for play and comment counts`() {
        val layout = resolveVideoCardCompactCoverStatsLayout(
            availableWidthDp = 148f,
            primaryStatText = "2.0亿",
            secondaryStatText = "1.2万"
        )

        assertEquals(58f, layout.primaryMinWidthDp, 0.0001f)
        assertEquals(64f, layout.secondaryMinWidthDp, 0.0001f)
        assertEquals(0f, layout.statsEndPaddingDp, 0.0001f)
    }

    @Test
    fun `compact cover stats keeps secondary count visible on narrow cards`() {
        val layout = resolveVideoCardCompactCoverStatsLayout(
            availableWidthDp = 132f,
            primaryStatText = "5035.0万",
            secondaryStatText = "9999"
        )

        assertEquals(true, layout.showSecondaryStat)
        assertEquals(0f, layout.statsEndPaddingDp, 0.0001f)
    }

    @Test
    fun `compact cover stats reserves right side for duration badge`() {
        val layout = resolveVideoCardCompactCoverStatsLayout(
            availableWidthDp = 148f,
            primaryStatText = "已看79%",
            secondaryStatText = null,
            durationBadgeMinWidthDp = 40f
        )

        assertEquals(46f, layout.statsEndPaddingDp, 0.0001f)
    }

    @Test
    fun `cover overlay reserves bottom room when history progress bar is visible`() {
        val layout = resolveVideoCardCoverOverlayBottomLayout(showHistoryProgressBar = true)

        assertEquals(2f, layout.historyProgressBarHeightDp, 0.0001f)
        assertEquals(10f, layout.compactStatsBottomPaddingDp, 0.0001f)
        assertEquals(14f, layout.floatingDurationBottomPaddingDp, 0.0001f)
    }

    @Test
    fun `cover overlay keeps stable bottom padding without history progress bar`() {
        val layout = resolveVideoCardCoverOverlayBottomLayout(showHistoryProgressBar = false)

        assertEquals(2f, layout.historyProgressBarHeightDp, 0.0001f)
        assertEquals(10f, layout.compactStatsBottomPaddingDp, 0.0001f)
        assertEquals(14f, layout.floatingDurationBottomPaddingDp, 0.0001f)
    }

    @Test
    fun `duration stat pill expands width for hh mm ss text`() {
        assertEquals(60f, resolveVideoCardDurationStatMinWidthDp("12:34"), 0.0001f)
        assertEquals(68f, resolveVideoCardDurationStatMinWidthDp("1:25:10"), 0.0001f)
    }

    @Test
    fun `duration stat pill keeps play and duration while danmaku yields on narrow covers`() {
        val layout = resolveVideoCardCompactCoverStatsLayout(
            availableWidthDp = 148f,
            primaryStatText = "2.0亿",
            secondaryStatText = "1.2万",
            durationStatMinWidthDp = resolveVideoCardDurationStatMinWidthDp("12:34")
        )

        assertEquals(58f, layout.primaryMinWidthDp, 0.0001f)
        assertEquals(false, layout.showSecondaryStat)
        assertEquals(0f, layout.statsEndPaddingDp, 0.0001f)
    }

    @Test
    fun `duration stat pill keeps danmaku when cover width allows`() {
        val layout = resolveVideoCardCompactCoverStatsLayout(
            availableWidthDp = 200f,
            primaryStatText = "2.0亿",
            secondaryStatText = "1.2万",
            durationStatMinWidthDp = resolveVideoCardDurationStatMinWidthDp("12:34")
        )

        assertEquals(true, layout.showSecondaryStat)
    }

    @Test
    fun `duration stat pill alone leaves danmaku hidden when width is tight`() {
        val layout = resolveVideoCardCompactCoverStatsLayout(
            availableWidthDp = 160f,
            primaryStatText = "2.0亿",
            secondaryStatText = "1.2万",
            durationStatMinWidthDp = resolveVideoCardDurationStatMinWidthDp("12:34")
        )

        assertEquals(false, layout.showSecondaryStat)
    }
}
