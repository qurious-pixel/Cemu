package info.cemu.cemu.common.android.inputdevice

import android.view.InputDevice

fun InputDevice.isGameController(): Boolean {
    if (isVirtual) {
        return false
    }

    val isGamepad = (sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
            || (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
            || (sources and InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD

    val isPhysicalKeyboard = (sources and InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD 
            && keyboardType == InputDevice.KEYBOARD_TYPE_ALPHABETIC

    return isGamepad || isPhysicalKeyboard
}
