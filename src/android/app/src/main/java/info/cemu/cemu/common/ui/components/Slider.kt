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
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.semantics
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
    var sliderValue by rememberSaveable(value) { mutableFloatStateOf(value.toFloat()) }
    val onValueChangeState = rememberUpdatedState(onValueChange)

    val focusRequester = FocusRequester()
    val interactionSource = remember { MutableInteractionSource() }
    val coroutineScope = rememberCoroutineScope()

    // Determine step float amount. If steps > 0, steps param means number of discrete intervals between ends.
    val effectiveSteps = if (steps > 0) steps + 1 else (valueTo - valueFrom).coerceAtLeast(1)
    val stepSizeFloat = (valueTo - valueFrom).toFloat() / effectiveSteps.toFloat()

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
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when (keyEvent.key) {
                        Key.DirectionLeft, Key.DirectionDown -> {
                            coroutineScope.launch {
                                val newVal = (sliderValue - stepSizeFloat).coerceAtLeast(valueFrom.toFloat())
                                sliderValue = newVal
                                onValueChangeState.value(newVal.roundToInt())
                            }
                            true
                        }
                        Key.DirectionRight, Key.DirectionUp -> {
                            coroutineScope.launch {
                                val newVal = (sliderValue + stepSizeFloat).coerceAtMost(valueTo.toFloat())
                                sliderValue = newVal
                                onValueChangeState.value(newVal.roundToInt())
                            }
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
