package com.android.purebilibili.core.ui.performance

import org.junit.Assert.assertEquals
import org.junit.Test

class AppDisplayModePolicyTest {

    @Test
    fun `appDisplayMode_matches1080p90`() {
        val selected = resolveAppPreferredDisplayModeId(
            currentModeId = 1,
            supportedModes = listOf(
                AppDisplayModeCandidate(modeId = 1, refreshRate = 60f, width = 1440, height = 3200),
                AppDisplayModeCandidate(modeId = 2, refreshRate = 120f, width = 1080, height = 2400),
                AppDisplayModeCandidate(modeId = 3, refreshRate = 90f, width = 1080, height = 2400)
            )
        )

        assertEquals(3, selected)
    }

    @Test
    fun `appDisplayMode_returnsNullWhenNo1080p90Mode`() {
        val selected = resolveAppPreferredDisplayModeId(
            currentModeId = 1,
            supportedModes = listOf(
                AppDisplayModeCandidate(modeId = 1, refreshRate = 60f, width = 1440, height = 3200),
                AppDisplayModeCandidate(modeId = 2, refreshRate = 120f, width = 1080, height = 2400)
            )
        )

        assertEquals(null, selected)
    }

    @Test
    fun `appDisplayMode_picksClosestRefreshTo90Among1080pModes`() {
        val selected = resolveAppPreferredDisplayModeId(
            currentModeId = 1,
            supportedModes = listOf(
                AppDisplayModeCandidate(modeId = 1, refreshRate = 89.7f, width = 1080, height = 2400),
                AppDisplayModeCandidate(modeId = 2, refreshRate = 90.4f, width = 1080, height = 2400)
            )
        )

        assertEquals(1, selected)
    }

    @Test
    fun `appDisplayMode_ignoresOtherResolutions`() {
        val selected = resolveAppPreferredDisplayModeId(
            currentModeId = 1,
            supportedModes = listOf(
                AppDisplayModeCandidate(modeId = 1, refreshRate = 90f, width = 1440, height = 3200),
                AppDisplayModeCandidate(modeId = 2, refreshRate = 90f, width = 720, height = 1600)
            )
        )

        assertEquals(null, selected)
    }

    @Test
    fun `appDisplayMode_prefersCurrentModeWhenRefreshTies`() {
        val selected = resolveAppPreferredDisplayModeId(
            currentModeId = 5,
            supportedModes = listOf(
                AppDisplayModeCandidate(modeId = 4, refreshRate = 90f, width = 1080, height = 2400),
                AppDisplayModeCandidate(modeId = 5, refreshRate = 90f, width = 1080, height = 2400)
            )
        )

        assertEquals(5, selected)
    }

    @Test
    fun `appDisplayMode_returnsNullForEmptyModes`() {
        val selected = resolveAppPreferredDisplayModeId(
            currentModeId = 1,
            supportedModes = emptyList()
        )

        assertEquals(null, selected)
    }
}
