package info.cemu.cemu.common.ui.components

import androidx.annotation.IntRange
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider as MaterialSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
//import androidx.compose.ui.focus.focusable
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun Slider(
    label: String,
    value: Int,
    valueFrom: Int,
    valueTo: Int,
    @IntRange(from = 0) steps: Int = (valueTo - valueFrom - 1).coerceAtLeast(0),
    labelFormatter: (Int) -> String,
    onValueChange: (Int) -> Unit,
) {
    var sliderValue by rememberSaveable(value) { mutableFloatStateOf(value.toFloat()) }

    fun applyDelta(deltaSteps: Int) {
        val stepWidth = if (steps > 0) (valueTo - valueFrom) / (steps + 1) else 1
        val newInt = (sliderValue + deltaSteps * stepWidth).roundToInt()
            .coerceIn(valueFrom, valueTo)
        sliderValue = newInt.toFloat()
        onValueChange(newInt)
    }

    Column(
        modifier = Modifier
            .padding(8.dp)
            .focusable()
            .onKeyEvent { keyEvent ->
                when {
                    keyEvent.key == Key.DirectionLeft ||
                    keyEvent.key == Key.DPadLeft -> {
                        if (keyEvent.type == KeyEventType.KeyDown) applyDelta(-1)
                        true
                    }
                    keyEvent.key == Key.DirectionRight ||
                    keyEvent.key == Key.DPadRight -> {
                        if (keyEvent.type == KeyEventType.KeyDown) applyDelta(1)
                        true
                    }
                    else -> false
                }
            }
    ) {
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
            valueRange = valueFrom.toFloat()..valueTo.toFloat(),
            steps = steps,
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onValueChange(sliderValue.roundToInt()) },
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
) {
    var value by rememberSaveable { mutableIntStateOf(initialValue()) }

    Slider(
        label = label,
        value = value,
        valueFrom = valueFrom,
        valueTo = valueTo,
        steps = steps,
        labelFormatter = labelFormatter,
        onValueChange = {
            value = it
            onValueChange(it)
        }
    )
}
