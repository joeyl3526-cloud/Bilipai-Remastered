package com.android.purebilibili.feature.space

import com.android.purebilibili.data.model.response.RelationStatData
import com.android.purebilibili.data.model.response.UpStatData
import kotlin.test.Test
import kotlin.test.assertEquals

class SpaceHeaderPresentationPolicyTest {

    @Test
    fun `header metrics keep compact relation and likes set`() {
        val metrics = resolveSpaceHeaderMetricItems(
            relationStat = RelationStatData(following = 24, follower = 1024),
            upStat = UpStatData(likes = 42_000)
        )

        assertEquals(
            listOf("粉丝", "关注", "获赞"),
            metrics.map { it.label }
        )
        assertEquals(
            listOf(1024L, 24L, 42_000L),
            metrics.map { it.value }
        )
    }

    @Test
    fun `header metrics fall back to zero for missing stats`() {
        val metrics = resolveSpaceHeaderMetricItems(
            relationStat = null,
            upStat = null
        )

        assertEquals(listOf(0L, 0L, 0L), metrics.map { it.value })
    }

    @Test
    fun `stat width picks compact below 360 dp`() {
        assertEquals(72, resolveSpaceHeaderMetricStatWidthDp(360))
        assertEquals(60, resolveSpaceHeaderMetricStatWidthDp(359))
        assertEquals(60, resolveSpaceHeaderMetricStatWidthDp(320))
    }

    @Test
    fun `metric row width derives from three fixed stat columns, dividers and spacing`() {
        // 3×72 + 2×1（分隔线）+ 4×8（间距）= 250
        assertEquals(250, resolveSpaceHeaderMetricRowWidthDp(72))
        // 3×60 + 2 + 32 = 214
        assertEquals(214, resolveSpaceHeaderMetricRowWidthDp(60))
    }

    @Test
    fun `follow button is half of metric row leftover after fixed relation actions`() {
        // 固定部分 = 私信圆钮 32 + 间距 8 = 40；(250 - 40) / 2 = 105
        assertEquals(105, resolveSpaceHeaderFollowButtonWidthDp(250))
        // (214 - 40) / 2 = 87
        assertEquals(87, resolveSpaceHeaderFollowButtonWidthDp(214))
    }
}
