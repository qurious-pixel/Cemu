package info.cemu.cemu.common.input

import android.view.KeyEvent
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType

fun Modifier.handleGamepadFocus(
    focusManager: FocusManager,
    onEnterPressed: (() -> Unit)? = null
): Modifier = this.onPreviewKeyEvent { keyEvent ->
    if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

    when (keyEvent.nativeKeyEvent.keyCode) {
        KeyEvent.KEYCODE_DPAD_DOWN -> {
            focusManager.moveFocus(FocusDirection.Down)
            true
        }
        KeyEvent.KEYCODE_DPAD_UP -> {
            focusManager.moveFocus(FocusDirection.Up)
            true
        }
        KeyEvent.KEYCODE_DPAD_LEFT -> {
            focusManager.moveFocus(FocusDirection.Left)
            true
        }
        KeyEvent.KEYCODE_DPAD_RIGHT -> {
            focusManager.moveFocus(FocusDirection.Right)
            true
        }
        KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> {
            onEnterPressed?.invoke() ?: focusManager.moveFocus(FocusDirection.Next)
            true
        }
        else -> false
    }
}
