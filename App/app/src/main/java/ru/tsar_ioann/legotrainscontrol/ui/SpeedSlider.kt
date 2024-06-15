package ru.tsar_ioann.legotrainscontrol.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderPositions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedSlider(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    valueRange: ClosedFloatingPointRange<Float>,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    Slider(
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        track = { sliderPositions ->
            SliderTrackFromCenter(
                enabled = enabled,
                sliderPositions = sliderPositions,
            )
        },
    )
}

private val TrackHeight = 4.0.dp

@Composable
private fun SliderTrackFromCenter(
    sliderPositions: SliderPositions,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val inactiveTrackColor = rememberUpdatedState(
        if (enabled) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    )
    val activeTrackColor = rememberUpdatedState(
        if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    )
    Canvas(modifier = modifier.fillMaxWidth().height(TrackHeight)) {
        val isRtl = layoutDirection == LayoutDirection.Rtl
        val sliderLeft = Offset(0f, center.y)
        val sliderRight = Offset(size.width, center.y)
        val sliderStart = if (isRtl) sliderRight else sliderLeft
        val sliderEnd = if (isRtl) sliderLeft else sliderRight
        val trackStrokeWidth = TrackHeight.toPx()
        drawLine(
            inactiveTrackColor.value,
            sliderStart,
            sliderEnd,
            trackStrokeWidth,
            StrokeCap.Round
        )
        val sliderValueEnd = Offset(sliderStart.x + (sliderEnd.x - sliderStart.x) * sliderPositions.activeRange.endInclusive, center.y)
        val sliderValueStart = Offset(center.x, center.y)  // here is the 1st thing which differs from SliderDefaults.Track
        drawLine(
            activeTrackColor.value,
            sliderValueStart,
            sliderValueEnd,
            trackStrokeWidth,
            StrokeCap.Round
        )
        // The 2nd thing which differs from SliderDefaults.Track: we don't use ticks, so no reason to draw them
    }
}

@Preview
@Composable
fun PreviewBasicSliderAndSpeedSlider(@PreviewParameter(BooleanPreviewParameterProvider::class) enabled: Boolean) {
    Column {
        Slider(enabled = enabled, valueRange = -100f..100f, value = -30f, onValueChange = {})
        SpeedSlider(enabled = enabled, valueRange = -100f..100f, value = -30f, onValueChange = {})

        // Now in RTL mode
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Slider(enabled = enabled, valueRange = -100f..100f, value = -30f, onValueChange = {})
            SpeedSlider(enabled = enabled, valueRange = -100f..100f, value = -30f, onValueChange = {})
        }
    }
}
