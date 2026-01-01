package info.cemu.cemu.common.input

import android.view.KeyEvent as NativeKeyEvent
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction

@Composable
fun ClickToEditTextField(
    value: String,
    label: @Composable (() -> Unit),
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    singleLine: Boolean = true,
    onClick: (() -> Unit)? = null,
    colors: androidx.compose.material3.TextFieldColors = androidx.compose.material3.TextFieldDefaults.colors()
) {
    val focusManager = LocalFocusManager.current
    var isEditing by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    TextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        isError = isError,
        supportingText = supportingText,
        colors = colors,
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { state ->
                if (!state.isFocused) isEditing = false
            }
            .onKeyEvent { keyEvent ->
                val isCenterClick = keyEvent.nativeKeyEvent.keyCode == NativeKeyEvent.KEYCODE_DPAD_CENTER ||
                                    keyEvent.nativeKeyEvent.keyCode == NativeKeyEvent.KEYCODE_BUTTON_A
                
                if (isCenterClick && keyEvent.type == KeyEventType.KeyUp) {
                    if (onClick != null) {
                        onClick()
                    } else if (!isEditing) {
                        isEditing = true
                        focusRequester.requestFocus()
                    }
                    true 
                } else {
                    false
                }
            }
            .pointerInput(Unit) {
                detectTapGestures {
                    if (onClick != null) {
                        onClick()
                    } else {
                        isEditing = true
                        focusRequester.requestFocus()
                    }
                }
            },
        readOnly = if (onClick != null) true else !isEditing,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        keyboardActions = if (onClick != null) keyboardActions else KeyboardActions(
            onDone = {
                isEditing = false
                focusManager.clearFocus()
                keyboardActions.onDone?.invoke(this)
            },
            onNext = {
                keyboardActions.onNext?.invoke(this)
            }
        )
    )
}
