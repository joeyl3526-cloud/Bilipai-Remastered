package com.android.purebilibili.feature.space

import com.android.purebilibili.data.model.response.RelationStatData
import com.android.purebilibili.data.model.response.UpStatData

internal data class SpaceHeaderMetricItem(
    val label: String,
    val value: Long
)

internal fun resolveSpaceHeaderMetricItems(
    relationStat: RelationStatData?,
    upStat: UpStatData?
): List<SpaceHeaderMetricItem> {
    return listOf(
        SpaceHeaderMetricItem("粉丝", relationStat?.follower?.toLong() ?: 0L),
        SpaceHeaderMetricItem("关注", relationStat?.following?.toLong() ?: 0L),
        SpaceHeaderMetricItem("获赞", upStat?.likes ?: 0L)
    )
}

// 统计行（粉丝/关注/获赞）宽度完全可推导：3 个固定宽 stat 列 + 2 个分隔线 + 4 个 8dp 间距。
internal const val SPACE_HEADER_METRIC_STAT_WIDTH_DP = 72
internal const val SPACE_HEADER_METRIC_STAT_WIDTH_COMPACT_DP = 60
internal const val SPACE_HEADER_METRIC_DIVIDER_WIDTH_DP = 1
internal const val SPACE_HEADER_METRIC_HORIZONTAL_SPACING_DP = 8
// 按钮行固定部分：私信圆钮 32dp + 与关注按钮的间距 8dp。
internal const val SPACE_HEADER_RELATION_ACTION_FIXED_WIDTH_DP = 32 + 8

internal fun resolveSpaceHeaderMetricStatWidthDp(screenWidthDp: Int): Int =
    if (screenWidthDp < 360) SPACE_HEADER_METRIC_STAT_WIDTH_COMPACT_DP else SPACE_HEADER_METRIC_STAT_WIDTH_DP

internal fun resolveSpaceHeaderMetricRowWidthDp(statWidthDp: Int): Int =
    statWidthDp * 3 + SPACE_HEADER_METRIC_DIVIDER_WIDTH_DP * 2 + SPACE_HEADER_METRIC_HORIZONTAL_SPACING_DP * 4

// 关注按钮宽 = 统计行宽减去私信/间距固定部分后的一半（左边界自然右移，右边界与统计行右边界重合）。
internal fun resolveSpaceHeaderFollowButtonWidthDp(metricRowWidthDp: Int): Int =
    (metricRowWidthDp - SPACE_HEADER_RELATION_ACTION_FIXED_WIDTH_DP) / 2
