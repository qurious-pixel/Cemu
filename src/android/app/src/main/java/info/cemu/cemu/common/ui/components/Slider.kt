package info.cemu.cemu.common.ui.components

import androidx.annotation.IntRange
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import androidx.compose.material3.Slider as MaterialSlider
import androidx.compose.ui.interaction.MutableInteractionSource

@Composable
fun Slider(
    label: String,
    value: Int,
    valueFrom: Int,
    valueTo: Int,
    @IntRange(from = 0) steps: Int = (valueTo - valueFrom - 1).coerceAtLeast(0),
    labelFormatter: (Int) -> String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    require(valueFrom < valueTo) { "valueFrom must be < valueTo" }

    val stepCount = (steps.coerceAtLeast(0)) + 1
    val rangeSpan = (valueTo - valueFrom).toFloat()
    val stepSizeFloat = rangeSpan / stepCount.toFloat() // distance per discrete step

    var sliderValue by rememberSaveable(value) { mutableFloatStateOf(value.toFloat()) }
    val onValueChangeState = rememberUpdatedState(onValueChange)

    val focusRequester = FocusRequester()
    val interactionSource = remember { MutableInteractionSource() }

    fun snapToStepFloat(raw: Float): Float {
        val clamped = raw.coerceIn(valueFrom.toFloat(), valueTo.toFloat())
        val relative = clamped - valueFrom.toFloat()
        val stepIndex = (relative / stepSizeFloat).roundToInt() // 0..stepCount
        return valueFrom.toFloat() + stepIndex * stepSizeFloat
    }

    fun snappedFloatToInt(snapped: Float): Int =
        (snapped.roundToInt())

    Column(modifier = modifier.padding(8.dp)) {
        Text(
            modifier = Modifier.padding(bottom = 8.dp),
            text = label,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
        )
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = labelFormatter(sliderValue.roundToInt()),
            fontWeight = FontWeight.Light,
            fontSize = 14.sp,
        )

        MaterialSlider(
            modifier = Modifier
                .focusRequester(focusRequester)
                .focusable(interactionSource = interactionSource)
                .onKeyEvent { event: KeyEvent ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when (event.key) {
                        Key.DirectionLeft, Key.DirectionDown -> {
                            val newRaw = sliderValue - stepSizeFloat
                            val snapped = snapToStepFloat(newRaw)
                            sliderValue = snapped
                            onValueChangeState.value(snappedFloatToInt(snapped))
                            true
                        }
                        Key.DirectionRight, Key.DirectionUp -> {
                            val newRaw = sliderValue + stepSizeFloat
                            val snapped = snapToStepFloat(newRaw)
                            sliderValue = snapped
                            onValueChangeState.value(snappedFloatToInt(snapped))
                            true
                        }
                        else -> false
                    }
                }
                .semantics {
                    contentDescription = "$label: ${labelFormatter(sliderValue.roundToInt())}"
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        current = sliderValue,
                        range = valueFrom.toFloat()..valueTo.toFloat(),
                        steps = steps
                    )
                    setProgress { newFloat ->
                        val snapped = snapToStepFloat(newFloat)
                        sliderValue = snapped
                        onValueChangeState.value(snappedFloatToInt(snapped))
                        true
                    }
                },
            valueRange = valueFrom.toFloat()..valueTo.toFloat(),
            steps = steps,
            value = sliderValue,
            onValueChangeFinished = {
                val snapped = snapToStepFloat(sliderValue)
                sliderValue = snapped
                onValueChangeState.value(snappedFloatToInt(snapped))
            },
            onValueChange = { raw ->
                val snapped = snapToStepFloat(raw)
                sliderValue = snapped
                onValueChangeState.value(snappedFloatToInt(snapped))
            },
        )
    }
}

@Composable
fun Slider(
    label: String,
    initialValue: () -> Int,
    valueFrom: Int,
    valueTo: Int,
    @IntRange(from = 0) steps: Int = (valueTo - valueFrom - 1).coerceAtLeast(0),
    labelFormatter: (Int) -> String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var internalValue by rememberSaveable { mutableIntStateOf(initialValue()) }
    Slider(
        label = label,
        value = internalValue,
        valueFrom = valueFrom,
        valueTo = valueTo,
        steps = steps,
        labelFormatter = labelFormatter,
        onValueChange = { new ->
            internalValue = new
            onValueChange(new)
        },
        modifier = modifier,
    )
}
