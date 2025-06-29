package com.maary.liveinpeace.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maary.liveinpeace.ui.theme.Typography

@Composable
fun TextContent(modifier: Modifier = Modifier, title: String, description: String? = null, color: Color = MaterialTheme.colorScheme.secondary) {
    Column(modifier = modifier){
        Text(
            title,
            style = Typography.titleLarge,
            color = color,
        )
        if (description != null) {
            Text(
                description,
                style = Typography.bodySmall,
                maxLines = 5,
                color = color
            )
        }
    }
}

@Composable
fun SwitchRow(
    title: String,
    description: String? = null,
    state: Boolean,
    switchColor: Color = MaterialTheme.colorScheme.secondary,
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
        TextContent(modifier = Modifier.weight(1f), title = title, description = description, color = switchColor)
        Spacer(modifier = Modifier.width(8.dp))
        Switch(checked = state, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedTrackColor = switchColor))
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
                .padding(bottom = 8.dp),
            color = MaterialTheme.colorScheme.secondary
        )

        // 内部状态的初始化逻辑保持不变
        var sliderPosition by remember { mutableStateOf(range.first.toFloat()..range.last.toFloat()) }

        // ✨ 新增 LaunchedEffect 来同步外部和内部的状态
        // 当 `range` 参数发生变化时，这个代码块会重新执行
        LaunchedEffect(range) {
            sliderPosition = range.first.toFloat()..range.last.toFloat()
        }

        RangeSlider(
            modifier = Modifier.fillMaxWidth(),
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
            },
            colors = SliderDefaults.colors(
                activeTrackColor = MaterialTheme.colorScheme.secondary,
//                thumbColor = MaterialTheme.colorScheme.secondary,
                inactiveTrackColor = MaterialTheme.colorScheme.onSecondary,
//                activeTickColor = MaterialTheme.colorScheme.onSecondaryContainer,
//                inactiveTickColor = MaterialTheme.colorScheme.onSecondaryContainer,
//                disabledActiveTickColor = MaterialTheme.colorScheme.onSecondaryContainer,
//                disabledInactiveTickColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        )
    }
}

@Composable
private fun ValueIndicatorThumb(
    value: Float,
    enabled: Boolean
) {
    // 根据可用状态选择颜色
    val indicatorColor = if (enabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    val textColor = if (enabled) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.surface

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
                textAlign = TextAlign.Center,
                modifier = Modifier.width(24.dp).padding(horizontal = 4.dp, vertical = 2.dp)
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

// 定义设置项在分组中的位置
enum class GroupPosition {
    TOP,    // 顶部
    MIDDLE, // 中间
    BOTTOM, // 底部
    SINGLE  // 独立，自成一组
}

@Composable
fun SettingsItem(
    position: GroupPosition,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable () -> Unit
) {
    // 根据 position 决定圆角形状
    val shape = when (position) {
        GroupPosition.TOP -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
        GroupPosition.MIDDLE -> RoundedCornerShape(8.dp)
        GroupPosition.BOTTOM -> RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
        GroupPosition.SINGLE -> RoundedCornerShape(24.dp) // 上下都是大圆角
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .clip(shape) // 动态应用形状
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}