package com.maary.liveinpeace.ui.screen

import android.transition.Slide
import android.util.Range
import androidx.annotation.FloatRange
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.util.toRange
import com.maary.liveinpeace.ui.theme.Typography
import com.maary.liveinpeace.R

@Composable
fun TextContent(modifier: Modifier = Modifier, title: String, description: String) {
    Column(modifier = modifier){
        Text(
            title,
            style = Typography.titleLarge
        )
        Text(
            description,
            style = Typography.bodySmall,
            maxLines = 5
        )
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownItem(modifier: Modifier, options: MutableList<String>, position: Int, onItemClicked: (Int) -> Unit) {
    var expanded by remember {
        mutableStateOf(false)
    }

    Box(modifier = modifier) {
        ExposedDropdownMenuBox(
            modifier =
                Modifier.padding(8.dp),
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .wrapContentWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                value = options[position],//text,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            )
            ExposedDropdownMenu(
                modifier = Modifier.wrapContentWidth(),
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        modifier = Modifier.wrapContentWidth(),
                        text = { Text(option, style = Typography.bodyLarge) },
                        onClick = {
                            expanded = false
                            onItemClicked(options.indexOf(option))
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
    }
}

@Composable
fun SwitchRow(
    title: String,
    description: String,
    state: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!state) }
            .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextContent(modifier = Modifier.weight(1f), title = title, description = description)
        Switch(checked = state, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun EnableForegroundRow(
    state: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onNotificationSettingsClicked: () -> Unit
) {
    Column {
        SwitchRow(
            title = stringResource(id = R.string.default_channel),
            description = stringResource(id = R.string.default_channel_description),
            state = state,
            onCheckedChange = onCheckedChange)
        if (state) {
            TextContent(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNotificationSettingsClicked() }
                    .padding(start = 32.dp, top = 8.dp, end = 32.dp, bottom = 8.dp),
                title = stringResource(id = R.string.notification_settings),
                description = stringResource(R.string.notification_settings_description))
        }
    }
}

@Composable
fun DropdownRow(options: MutableList<String>, position: Int, onItemClicked: (Int) -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextContent(
            modifier = Modifier.weight(3f),
            title = stringResource(id = R.string.icon_type),
            description = stringResource(id = R.string.icon_type_description)
        )
        DropdownItem(modifier = Modifier.weight(2f), options = options,
            position = position, onItemClicked = onItemClicked)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThresholdSlider(title: String, range: IntRange, onValueChangeFinished: (IntRange) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = title,
            style = Typography.titleMedium,
            modifier = Modifier
                .padding(top = 8.dp, bottom = 8.dp)
        )

        // 内部状态的初始化逻辑保持不变
        var sliderPosition by remember { mutableStateOf(range.first.toFloat()..range.last.toFloat()) }

        // ✨ 新增 LaunchedEffect 来同步外部和内部的状态
        // 当 `range` 参数发生变化时，这个代码块会重新执行
        LaunchedEffect(range) {
            sliderPosition = range.first.toFloat()..range.last.toFloat()
        }

        RangeSlider(
            value = sliderPosition,
            steps = 0,
            onValueChange = { newRange ->
                // onValueChange 负责在用户拖动时更新UI，这部分是正确的
                sliderPosition = newRange
            },
            valueRange = 0f..50f,
            onValueChangeFinished = {
                val intStart = sliderPosition.start.toInt()
                val intEnd = sliderPosition.endInclusive.toInt()
                onValueChangeFinished(intStart..intEnd)
            },
            startThumb = {
                // 2. 将自定义滑块应用于起始点
                ValueIndicatorThumb(
                    value = sliderPosition.start,
                    enabled = true
                )
            },
            endThumb = {
                // 3. 将自定义滑块应用于结束点
                ValueIndicatorThumb(
                    value = sliderPosition.endInclusive,
                    enabled = true
                )
            }
        )
    }
}

@Composable
private fun ValueIndicatorThumb(
    value: Float,
    enabled: Boolean
) {
    // 根据可用状态选择颜色
    val indicatorColor = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    val textColor = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.surface

    // 使用 Column 垂直排列指示器和滑块
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // 4. 数值指示器 (气泡)
        Surface(
            color = indicatorColor,
            shape = RoundedCornerShape(12.dp), // MD3 风格的圆角
            modifier = Modifier
                .padding(bottom = 6.dp) // 指示器和滑块之间的间距
        ) {
            Text(
                text = "%.0f".format(value), // 将数值格式化为整数
                color = textColor,
                style = MaterialTheme.typography.labelSmall, // 使用 MD3 的字体样式
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // 5. 滑块本身 (圆点)
        Box(
            modifier = Modifier
                .height(35.dp) // MD3 默认的滑块大小
                .width(4.dp)
                .background(color = SliderDefaults.colors().thumbColor)
        )
    }
}

@Composable
fun TopBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun MiddleBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun BottomBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun StandaloneBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}