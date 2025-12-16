package info.cemu.cemu.common.ui.components

import androidx.annotation.IntRange
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
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
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.Key.Companion.DirectionLeft
import androidx.compose.ui.input.key.Key.Companion.DirectionRight
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.RangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.rangeInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.compose.material3.Slider as MaterialSlider
import androidx.compose.ui.interaction.MutableInteractionSource
import kotlinx.coroutines.launch

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
    // Local mutable float state for the MaterialSlider
    var sliderValue by rememberSaveable(value) { mutableFloatStateOf(value.toFloat()) }

    // Keep an updated onValueChange to use from lambdas
    val onValueChangeState = rememberUpdatedState(onValueChange)

    // Focus and key handling for gamepad/DPAD navigation
    val focusRequester = FocusRequester()
    val interactionSource = remember { MutableInteractionSource() }
    val coroutineScope = rememberCoroutineScope()

    // Calculate step size in float per increment
    val totalSteps = (if (steps > 0) steps + 1 else (valueTo - valueFrom)).coerceAtLeast(1)
    val stepSizeFloat = (valueTo - valueFrom).toFloat() / totalSteps.toFloat()

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

        // Provide semantics for screen readers and accessibility, and handle key events for gamepad/dpad
        MaterialSlider(
            modifier = Modifier
                .focusRequester(focusRequester)
                .focusable(interactionSource = interactionSource)
                .onKeyEvent { keyEvent ->
                    // Only handle key down events
                    if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when (keyEvent.key) {
                        Key.DirectionLeft, Key.DirectionDown -> {
                            // move one step left/down
                            coroutineScope.launch {
                                val newVal = (sliderValue - (stepSizeFloat.coerceAtLeast(1f))).coerceAtLeast(valueFrom.toFloat())
                                sliderValue = newVal
                                onValueChangeState.value(newVal.roundToInt())
                            }
                            true
                        }
                        Key.DirectionRight, Key.DirectionUp -> {
                            // move one step right/up
                            coroutineScope.launch {
                                val newVal = (sliderValue + (stepSizeFloat.coerceAtLeast(1f))).coerceAtMost(valueTo.toFloat())
                                sliderValue = newVal
                                onValueChangeState.value(newVal.roundToInt())
                            }
                            true
                        }
                        else -> false
                    }
                }
                .semantics {
                    // content description and range semantics
                    contentDescription = "$label: ${labelFormatter(sliderValue.roundToInt())}"
                    // progress bar range info for assistive tech
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        current = sliderValue,
                        range = valueFrom.toFloat()..valueTo.toFloat(),
                        steps = steps
                    )
                    // allow setProgress actions (for accessibility services)
                    setProgress { newFloat ->
                        val coerced = newFloat.coerceIn(valueFrom.toFloat(), valueTo.toFloat())
                        sliderValue = coerced
                        onValueChangeState.value(coerced.roundToInt())
                        true
                    }
                },
            valueRange = valueFrom.toFloat()..valueTo.toFloat(),
            steps = steps,
            value = sliderValue,
            onValueChangeFinished = { onValueChangeState.value(sliderValue.roundToInt()) },
            onValueChange = { sliderValue = it },
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
    var value by rememberSaveable { mutableIntStateOf(initialValue()) }
    Slider(
        label = label,
        value = value,
        valueFrom = valueFrom,
        valueTo = valueTo,
        steps = steps,
        labelFormatter = labelFormatter,
        onValueChange = onValueChange,
        modifier = modifier,
    )
}
