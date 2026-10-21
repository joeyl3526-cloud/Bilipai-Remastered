package com.android.purebilibili.feature.audio.lyrics

import com.android.purebilibili.feature.video.subtitle.SubtitleCue
import com.android.purebilibili.feature.video.subtitle.SubtitleTrackMeta
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SubtitleLyricsPolicyTest {

    private fun track(lan: String, lanDoc: String, id: Long = 1L) = SubtitleTrackMeta(
        id = id,
        lan = lan,
        lanDoc = lanDoc,
        subtitleUrl = "https://example.com/subtitle.json"
    )

    @Test
    fun `chinese track detection covers zh codes and chinese labels`() {
        assertTrue(isChineseSubtitleTrack(track("zh-CN", "中文（简体）")))
        assertTrue(isChineseSubtitleTrack(track("zh-Hans", "中文")))
        assertTrue(isChineseSubtitleTrack(track("ai-zh", "AI-中文")))
        assertTrue(isChineseSubtitleTrack(track("cn", "简体中文")))
        assertFalse(isChineseSubtitleTrack(track("ja-JP", "日本語")))
        assertFalse(isChineseSubtitleTrack(track("en-US", "英语")))
    }

    @Test
    fun `subtitle cues become lyric lines without translations`() {
        val document = buildLyricDocumentFromSubtitleCues(
            cues = listOf(
                SubtitleCue(1_000L, 4_000L, "Hello"),
                SubtitleCue(4_500L, 8_000L, "World")
            ),
            track = track("en-US", "英语")
        )

        assertEquals(2, document.lines.size)
        assertEquals("Hello", document.lines[0].text)
        assertTrue(document.lines[0].translations.isEmpty())
        assertEquals("英语", document.metadata["track"])
    }

    @Test
    fun `foreign subtitle merges chinese translation within tolerance window`() {
        val document = buildLyricDocumentFromSubtitleCues(
            cues = listOf(
                SubtitleCue(1_000L, 4_000L, "Hello"),
                SubtitleCue(4_500L, 8_000L, "World")
            ),
            track = track("ja-JP", "日本語"),
            translationCues = listOf(
                SubtitleCue(1_200L, 4_100L, "你好"),
                SubtitleCue(4_400L, 7_900L, "世界")
            )
        )

        assertEquals("Hello", document.lines[0].text)
        assertEquals(listOf("你好"), document.lines[0].translations)
        assertEquals("World", document.lines[1].text)
        assertEquals(listOf("世界"), document.lines[1].translations)
    }

    @Test
    fun `translation outside tolerance window is not merged`() {
        val document = buildLyricDocumentFromSubtitleCues(
            cues = listOf(
                SubtitleCue(1_000L, 4_000L, "Hello")
            ),
            track = track("en-US", "英语"),
            translationCues = listOf(
                SubtitleCue(9_000L, 12_000L, "太远不合并")
            )
        )

        assertTrue(document.lines[0].translations.isEmpty())
    }
}
