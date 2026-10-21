package com.android.purebilibili.core.ui.performance

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalView
import androidx.metrics.performance.PerformanceMetricsState
import androidx.metrics.performance.PerformanceMetricsState.Holder
import kotlinx.coroutines.CoroutineScope

@Composable
fun rememberMetricsStateHolder(): Holder {
    val localView = LocalView.current
    return remember(localView) {
        PerformanceMetricsState.getHolderForHierarchy(localView)
    }
}

@Composable
fun TrackJank(
    vararg keys: Any?,
    reportMetric: suspend CoroutineScope.(state: Holder) -> Unit
) {
    val metrics = rememberMetricsStateHolder()
    LaunchedEffect(metrics, *keys) {
        reportMetric(metrics)
    }
}

@Composable
fun TrackScrollJank(
    scrollableState: ScrollableState,
    stateName: String
) {
    TrackJank(scrollableState, stateName) { metricsHolder ->
        snapshotFlow { scrollableState.isScrollInProgress }
            .collect { isScrollInProgress ->
                metricsHolder.state?.apply {
                    if (isScrollInProgress) {
                        putState(stateName, "Scrolling=true")
                    } else {
                        removeState(stateName)
                    }
                }
            }
    }
}

@Composable
fun TrackJankStateValue(
    stateName: String,
    stateValue: String?
) {
    val metrics = rememberMetricsStateHolder()
    SideEffect {
        metrics.state?.apply {
            if (stateValue.isNullOrBlank()) {
                removeState(stateName)
            } else {
                putState(stateName, stateValue)
            }
        }
    }
    DisposableEffect(metrics, stateName) {
        onDispose {
            metrics.state?.removeState(stateName)
        }
    }
}

@Composable
fun TrackJankStateFlag(
    stateName: String,
    isActive: Boolean,
    activeValue: String = "true"
) {
    TrackJankStateValue(
        stateName = stateName,
        stateValue = if (isActive) activeValue else null
    )
}

/**
 * 效果驱动的状态追踪：通过 snapshotFlow 订阅 provider，读取发生在 effect 层，
 * 不订阅调用方组合作用域。适合放在高频重组的大函数体顶层、由拖动手势逐帧
 * 写入的瞬态标志位（避免每次写入都触发整函数重组）。
 */
@Composable
fun TrackJankStateFlagEffect(
    stateName: String,
    isActiveProvider: () -> Boolean,
    activeValue: String = "true"
) {
    TrackJankStateValueEffect(stateName = stateName, valueProvider = {
        if (isActiveProvider()) activeValue else null
    })
}

@Composable
fun TrackJankStateValueEffect(
    stateName: String,
    valueProvider: () -> String?
) {
    val metrics = rememberMetricsStateHolder()
    LaunchedEffect(metrics, stateName) {
        snapshotFlow { valueProvider() }.collect { stateValue ->
            metrics.state?.apply {
                if (stateValue.isNullOrBlank()) {
                    removeState(stateName)
                } else {
                    putState(stateName, stateValue)
                }
            }
        }
    }
    DisposableEffect(metrics, stateName) {
        onDispose {
            metrics.state?.removeState(stateName)
        }
    }
}
