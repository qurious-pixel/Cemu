package info.cemu.cemu.common.ui.components

import androidx.annotation.IntRange
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.compose.material3.Slider as MaterialSlider
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.saveable.rememberSaveable

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
    val stepSizeFloat = rangeSpan / stepCount.toFloat()

    var sliderValue by rememberSaveable(value) { mutableFloatStateOf(value.toFloat()) }
    val onValueChangeState = rememberUpdatedState(onValueChange)

    val focusRequester = FocusRequester()
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }

    fun snapToStepFloat(raw: Float): Float {
        val clamped = raw.coerceIn(valueFrom.toFloat(), valueTo.toFloat())
        val relative = clamped - valueFrom.toFloat()
        val stepIndex = (relative / stepSizeFloat).roundToInt()
        return valueFrom.toFloat() + stepIndex * stepSizeFloat
    }

    fun snappedFloatToInt(snapped: Float): Int =
        snapped.roundToInt()

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
			.onKeyEvent { event ->
    			// handle only key down
    			val isDown = try {
        			// Compose KeyEvent has 'type' property
        			event.type == KeyEventType.KeyDown
    			} catch (_: Throwable) {
        			// fallback: some platforms expose key event via nativeKeyEvent
        			event.nativeKeyEvent?.action == android.view.KeyEvent.ACTION_DOWN
    			}
    			if (!isDown) return@onKeyEvent false
			
    			// Try to match Compose Key first
    			val handled = when (event.key) {
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
    			if (handled) return@onKeyEvent true
			
    			// Fallback: compare native Android key codes if event.key isn't meaningful in this runtime
    			val nativeCode = try { event.nativeKeyEvent?.keyCode } catch (_: Throwable) { null }
    			when (nativeCode) {
        			android.view.KeyEvent.KEYCODE_DPAD_LEFT, android.view.KeyEvent.KEYCODE_MINUS -> {
            			coroutineScope.launch {
                			val newVal = (sliderValue - stepSizeFloat).coerceAtLeast(valueFrom.toFloat())
                			sliderValue = newVal
                			onValueChangeState.value(newVal.roundToInt())
            			}
            			true
        			}
        			android.view.KeyEvent.KEYCODE_DPAD_RIGHT, android.view.KeyEvent.KEYCODE_PLUS -> {
            			coroutineScope.launch {
                			val newVal = (sliderValue + stepSizeFloat).coerceAtMost(valueTo.toFloat())
                			sliderValue = newVal
                			onValueChangeState.value(newVal.roundToInt())
            			}
            			true
        			}
        			android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
            			coroutineScope.launch {
                			val newVal = (sliderValue - stepSizeFloat).coerceAtLeast(valueFrom.toFloat())
                			sliderValue = newVal
                			onValueChangeState.value(newVal.roundToInt())
            			}
            			true
        			}
        			android.view.KeyEvent.KEYCODE_DPAD_UP -> {
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
