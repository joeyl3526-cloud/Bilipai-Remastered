// 文件路径: core/ui/performance/AppDisplayModePolicy.kt
package com.android.purebilibili.core.ui.performance

import android.app.Activity
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

/**
 * 应用级显示模式策略：锁定 1080P + 90Hz。
 *
 * 降低分辨率（1080x2400 vs 1440x3200，少约 56% 像素填充）可显著削减
 * Haze/液态玻璃采样与合成成本；90Hz 平衡流畅与负载。preferredDisplayModeId
 * 是窗口级偏好，仅前台生效，退后台由系统接管还原。
 */
internal data class AppDisplayModeCandidate(
    val modeId: Int,
    val refreshRate: Float,
    val width: Int,
    val height: Int
)

/**
 * 解析应用偏好显示模式：仅匹配 targetWidth 且刷新率接近 targetRefreshRate 的模式，
 * 多候选时取刷新最接近目标者（并列时优先当前模式），无匹配返回 null（优雅降级，不设）。
 */
internal fun resolveAppPreferredDisplayModeId(
    currentModeId: Int,
    supportedModes: List<AppDisplayModeCandidate>,
    targetWidth: Int = 1080,
    targetRefreshRate: Float = 90f,
): Int? {
    if (supportedModes.isEmpty()) return null
    val candidates = supportedModes.filter {
        it.width == targetWidth &&
            kotlin.math.abs(it.refreshRate - targetRefreshRate) < 0.5f
    }
    if (candidates.isEmpty()) return null
    return candidates.minWithOrNull(
        compareBy<AppDisplayModeCandidate> {
            kotlin.math.abs(it.refreshRate - targetRefreshRate)
        }.thenByDescending { if (it.modeId == currentModeId) 1 else 0 }
    )?.modeId
}

/**
 * 应用级显示模式效果：进入组合时设置 1080P@90，退出时恢复原模式。
 * 复刻 VideoDetailHighRefreshRateEffect 的设置/恢复闭环。
 */
@Composable
internal fun AppDisplayModeEffect(
    activity: Activity?,
) {
    DisposableEffect(activity) {
        if (activity == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            onDispose { }
        } else {
            val hostWindow = activity.window
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.display
            } else {
                @Suppress("DEPRECATION")
                activity.windowManager.defaultDisplay
            }
            if (display == null) {
                onDispose { }
            } else {
                val originalModeId = hostWindow.attributes.preferredDisplayModeId
                val currentModeId = display.mode.modeId
                val preferredModeId = resolveAppPreferredDisplayModeId(
                    currentModeId = currentModeId,
                    supportedModes = display.supportedModes.map { mode ->
                        AppDisplayModeCandidate(
                            modeId = mode.modeId,
                            refreshRate = mode.refreshRate,
                            width = mode.physicalWidth,
                            height = mode.physicalHeight,
                        )
                    },
                )
                if (preferredModeId != null && preferredModeId != originalModeId) {
                    hostWindow.attributes = hostWindow.attributes.apply {
                        preferredDisplayModeId = preferredModeId
                    }
                }
                onDispose {
                    if (hostWindow.attributes.preferredDisplayModeId != originalModeId) {
                        hostWindow.attributes = hostWindow.attributes.apply {
                            preferredDisplayModeId = originalModeId
                        }
                    }
                }
            }
        }
    }
}
