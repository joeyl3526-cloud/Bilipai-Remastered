package com.android.purebilibili.data.model.response

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LikedVideosModelsTest {
    @Test
    fun likedVideo_mapsStandardArchiveFieldsToVideoItem() {
        val video = LikedVideoData(
            aid = 123L,
            bvid = "BV1TEST",
            cid = 456L,
            title = "点赞视频",
            pic = "cover",
            duration = 90,
            pubdate = 1_700_000_000L,
            tid = 21,
            tname = "日常",
            owner = Owner(mid = 7L, name = "UP主"),
            stat = Stat(view = 100, like = 8),
            dimension = Dimension(width = 1080, height = 1920)
        ).toVideoItem()

        assertEquals(123L, video.aid)
        assertEquals("BV1TEST", video.bvid)
        assertEquals(456L, video.cid)
        assertEquals("UP主", video.owner.name)
        assertEquals(8, video.stat.like)
        assertTrue(video.isVertical)
    }
}
