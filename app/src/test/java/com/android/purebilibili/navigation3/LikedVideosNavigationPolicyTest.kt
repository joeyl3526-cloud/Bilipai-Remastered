package com.android.purebilibili.navigation3

import com.android.purebilibili.navigation.ScreenRoutes
import kotlin.test.Test
import kotlin.test.assertEquals

class LikedVideosNavigationPolicyTest {
    @Test
    fun likedVideosRoute_roundTripsThroughNavigation3() {
        assertEquals(
            ScreenRoutes.LikedVideos.route,
            BiliPaiNavKey.LikedVideos.toLegacyRoute()
        )
        assertEquals(
            BiliPaiNavKey.LikedVideos,
            legacyRouteToBiliPaiNavKey(ScreenRoutes.LikedVideos.route)
        )
        assertEquals(
            BiliPaiNavEntryContentRole.LIKED_VIDEOS,
            resolveBiliPaiNavEntryContentRole(BiliPaiNavKey.LikedVideos)
        )
    }
}
