package com.android.purebilibili.feature.dynamic.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.android.purebilibili.core.ui.AppAlertDialog
import com.android.purebilibili.core.ui.AppDialogAction
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.components.AppFilterChip
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppTextButton
import com.android.purebilibili.core.ui.components.AppTextField
import com.android.purebilibili.data.model.response.DynamicCreatedReserve
import com.android.purebilibili.data.repository.DynamicCreateRepository
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun DynamicCreateReserveDialog(
    onDismiss: () -> Unit,
    onCreated: (DynamicCreatedReserve) -> Unit
) {
    val scope = rememberCoroutineScope()
    val initial = remember {
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, 20); set(Calendar.MINUTE, 0) }
    }
    var title by remember { mutableStateOf("") }
    var subType by remember { mutableIntStateOf(0) }
    var startAtMillis by remember { mutableLongStateOf(initial.timeInMillis) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pickedDateMillis by remember { mutableLongStateOf(initial.timeInMillis) }
    val startLabel = remember(startAtMillis) {
        val calendar = Calendar.getInstance().apply { timeInMillis = startAtMillis }
        "%d-%02d-%02d %02d:%02d".format(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH),
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE)
        )
    }

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { AppText("添加直播预约") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small)
            ) {
                AppTextField(value = title, onValueChange = { title = it }, placeholder = "预约标题", singleLine = true)
                AppFilterChip(
                    selected = subType == 0,
                    onClick = { subType = 0 },
                    label = { AppText("公开直播") }
                )
                AppFilterChip(
                    selected = subType == 1,
                    onClick = { subType = 1 },
                    label = { AppText("大航海直播") }
                )
                AppTextButton(
                    onClick = { showDatePicker = true }
                ) {
                    AppText("开播时间 $startLabel")
                }
                errorMessage?.let { AppText(it) }
            }
        },
        confirmButton = {
            AppDialogAction(
                onClick = {
                    if (submitting) return@AppDialogAction
                    submitting = true
                    errorMessage = null
                    scope.launch {
                        DynamicCreateRepository.createReserve(
                            title = title,
                            livePlanStartTimeSeconds = startAtMillis / 1000L,
                            subType = subType
                        ).fold(
                            onSuccess = { created ->
                                submitting = false
                                onCreated(created)
                            },
                            onFailure = { error ->
                                submitting = false
                                errorMessage = error.message ?: "创建失败"
                            }
                        )
                    }
                }
            ) {
                AppText(if (submitting) "创建中…" else "创建")
            }
        },
        dismissButton = {
            AppDialogAction(onClick = onDismiss) { AppText("取消") }
        }
    )

    if (showDatePicker) {
        ReserveDatePickerDialog(
            initialMillis = startAtMillis,
            onDismiss = { showDatePicker = false },
            onConfirmed = { millis ->
                pickedDateMillis = millis
                showDatePicker = false
                showTimePicker = true
            }
        )
    }

    if (showTimePicker) {
        ReserveTimePickerDialog(
            initialMillis = pickedDateMillis,
            onDismiss = { showTimePicker = false },
            onConfirmed = { hour, minute ->
                startAtMillis = Calendar.getInstance().apply {
                    timeInMillis = pickedDateMillis
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                showTimePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReserveDatePickerDialog(
    initialMillis: Long,
    onDismiss: () -> Unit,
    onConfirmed: (Long) -> Unit
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    AppAlertDialog(
        onDismissRequest = onDismiss,
        text = {
            DatePicker(state = state)
        },
        confirmButton = {
            AppDialogAction(
                onClick = {
                    val selected = state.selectedDateMillis ?: return@AppDialogAction
                    onConfirmed(selected)
                }
            ) {
                AppText("下一步")
            }
        },
        dismissButton = {
            AppDialogAction(onClick = onDismiss) { AppText("取消") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReserveTimePickerDialog(
    initialMillis: Long,
    onDismiss: () -> Unit,
    onConfirmed: (hour: Int, minute: Int) -> Unit
) {
    val calendar = remember(initialMillis) {
        Calendar.getInstance().apply { timeInMillis = initialMillis }
    }
    val state = rememberTimePickerState(
        initialHour = calendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = calendar.get(Calendar.MINUTE),
        is24Hour = true
    )
    AppAlertDialog(
        onDismissRequest = onDismiss,
        text = {
            TimePicker(state = state)
        },
        confirmButton = {
            AppDialogAction(onClick = { onConfirmed(state.hour, state.minute) }) {
                AppText("确定")
            }
        },
        dismissButton = {
            AppDialogAction(onClick = onDismiss) { AppText("取消") }
        }
    )
}