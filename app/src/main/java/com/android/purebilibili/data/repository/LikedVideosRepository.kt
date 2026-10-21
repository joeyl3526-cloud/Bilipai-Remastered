package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.data.model.response.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LikedVideosRepository {
    suspend fun getLikedVideos(mid: Long): Result<List<VideoItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val response = NetworkModule.api.getLikedVideos(mid)
            check(response.code == 0) {
                response.message.ifBlank { "获取点赞视频失败：${response.code}" }
            }
            response.data?.list.orEmpty().map { it.toVideoItem() }
        }
    }
}
