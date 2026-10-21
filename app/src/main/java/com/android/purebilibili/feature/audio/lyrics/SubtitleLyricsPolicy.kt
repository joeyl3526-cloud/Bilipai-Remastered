package com.android.purebilibili.feature.audio.lyrics

import com.android.purebilibili.feature.video.subtitle.SubtitleCue
import com.android.purebilibili.feature.video.subtitle.SubtitleTrackMeta
import kotlin.math.abs

/** 主/翻译字幕轨道时间对齐容差：同源切分时间戳基本一致，允许 ±300ms。 */
private const val SUBTITLE_TRACK_ALIGN_TOLERANCE_MS = 300L

/** 轨道是否为中文（官方中文或 AI 中文翻译轨）。 */
internal fun isChineseSubtitleTrack(track: SubtitleTrackMeta): Boolean {
    val lan = track.lan.lowercase()
    if (lan.startsWith("zh") || lan == "cn" || lan.startsWith("cn-")) return true
    val doc = track.lanDoc.lowercase()
    return doc.contains("中文") ||
        doc.contains("简体") ||
        doc.contains("繁体") ||
        doc.contains("国语") ||
        doc.contains("普通话")
}

/**
 * 视频字幕转歌词文档：原生/AI 字幕的 cue 直接作为歌词行。
 *
 * 每行带起止时间，复用歌词页的当前行高亮 / 点击行 seek / 自动跟随滚动交互。
 *
 * 传入 [translationCues]（另一条轨道，通常为中文翻译轨）时按起始时间对齐合并：
 * 每句歌词的 [LyricLine.translations] 填入对应翻译，实现中外双语对照。
 */
internal fun buildLyricDocumentFromSubtitleCues(
    cues: List<SubtitleCue>,
    track: SubtitleTrackMeta,
    translationCues: List<SubtitleCue> = emptyList(),
    fetchedAtMs: Long = System.currentTimeMillis()
): LyricDocument {
    val translationMap = alignSubtitleTranslations(cues, translationCues)
    val lines = cues
        .distinctBy { it.startMs to it.content }
        .map { cue ->
            LyricLine(
                startTimeMs = cue.startMs,
                endTimeMs = cue.endMs.coerceAtLeast(cue.startMs),
                text = cue.content,
                translations = translationMap[cue.startMs].orEmpty()
            )
        }
    return LyricDocument(
        metadata = mapOf("track" to track.lanDoc),
        lines = lines,
        source = LyricSource.BILIBILI,
        remoteId = track.trackKey,
        fetchedAtMs = fetchedAtMs
    )
}

/** 按起始时间对齐主字幕与翻译字幕：主句在容差窗口内命中翻译句 → 取该句内容。 */
private fun alignSubtitleTranslations(
    cues: List<SubtitleCue>,
    translationCues: List<SubtitleCue>
): Map<Long, List<String>> {
    if (translationCues.isEmpty()) return emptyMap()
    return buildMap {
        cues.distinctBy { it.startMs to it.content }.forEach { cue ->
            val translation = translationCues
                .firstOrNull {
                    abs(it.startMs - cue.startMs) <= SUBTITLE_TRACK_ALIGN_TOLERANCE_MS
                }
                ?.content
                ?.takeIf { it.isNotBlank() }
            if (translation != null) {
                put(cue.startMs, listOf(translation))
            }
        }
    }
}
