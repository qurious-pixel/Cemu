package info.cemu.cemu.settings.audio

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import info.cemu.cemu.common.ui.components.ScreenContent
import info.cemu.cemu.common.ui.components.SingleSelection
import info.cemu.cemu.common.ui.components.Slider
import info.cemu.cemu.common.ui.components.Toggle
import info.cemu.cemu.common.ui.localization.tr
import info.cemu.cemu.nativeinterface.NativeSettings

private const val AUDIO_LATENCY_STEPS = 22
private const val AUDIO_VOLUME_STEPS = 19

@Composable
fun AudioSettingsScreen(navigateBack: () -> Unit) {
    ScreenContent(
        appBarText = tr("Audio settings"),
        navigateBack = navigateBack,
    ) {
        // Latency
        var latencyFocused by remember { mutableStateOf(false) }
        val latencyRequester = remember { FocusRequester() }
        Slider(
            label = tr("Latency"),
            initialValue = { NativeSettings.getAudioLatency() },
            valueFrom = 0,
            steps = AUDIO_LATENCY_STEPS,
            valueTo = NativeSettings.AUDIO_LATENCY_MS_MAX,
            onValueChange = { v: Int -> NativeSettings.setAudioLatency(v) },
            labelFormatter = { v: Int -> "${v}ms" },
            modifier = Modifier
                .focusRequester(latencyRequester)
                .onFocusChanged { state -> latencyFocused = state.isFocused }
                .then(if (latencyFocused) Modifier.border(2.dp, Color.Cyan).padding(4.dp) else Modifier)
        )

        // TV toggle
        var tvToggleFocused by remember { mutableStateOf(false) }
        val tvToggleRequester = remember { FocusRequester() }
        Toggle(
            label = tr("TV"),
            description = tr("Enable audio output for the Wii U TV"),
            initialCheckedState = { NativeSettings.getAudioDeviceEnabled(true) },
            onCheckedChanged = { checked: Boolean -> NativeSettings.setAudioDeviceEnabled(checked, true) },
            modifier = Modifier
                .focusRequester(tvToggleRequester)
                .onFocusChanged { state -> tvToggleFocused = state.isFocused }
                .then(if (tvToggleFocused) Modifier.border(2.dp, Color.Cyan).padding(4.dp) else Modifier)
        )

        // TV channels
        var tvChannelsFocused by remember { mutableStateOf(false) }
        val tvChannelsRequester = remember { FocusRequester() }
        SingleSelection(
            label = tr("TV channels"),
            initialChoice = { NativeSettings.getAudioDeviceChannels(true) },
            onChoiceChanged = { choice: Int -> NativeSettings.setAudioDeviceChannels(choice, true) },
            choiceToString = { channels: Int -> channelsToString(channels) },
            choices = listOf(
                NativeSettings.AudioChannels.MONO,
                NativeSettings.AudioChannels.STEREO,
                NativeSettings.AudioChannels.SURROUND,
            ),
            modifier = Modifier
                .focusRequester(tvChannelsRequester)
                .onFocusChanged { state -> tvChannelsFocused = state.isFocused }
                .then(if (tvChannelsFocused) Modifier.border(2.dp, Color.Cyan).padding(4.dp) else Modifier)
        )

        // TV volume
        var tvVolumeFocused by remember { mutableStateOf(false) }
        val tvVolumeRequester = remember { FocusRequester() }
        Slider(
            label = tr("TV volume"),
            initialValue = { NativeSettings.getAudioDeviceVolume(true) },
            valueFrom = NativeSettings.AUDIO_MIN_VOLUME,
            steps = AUDIO_VOLUME_STEPS,
            valueTo = NativeSettings.AUDIO_MAX_VOLUME,
            onValueChange = { v: Int -> NativeSettings.setAudioDeviceVolume(v, true) },
            labelFormatter = { v: Int -> "$v%" },
            modifier = Modifier
                .focusRequester(tvVolumeRequester)
                .onFocusChanged { state -> tvVolumeFocused = state.isFocused }
                .then(if (tvVolumeFocused) Modifier.border(2.dp, Color.Cyan).padding(4.dp) else Modifier)
        )

        // Gamepad toggle
        var gpToggleFocused by remember { mutableStateOf(false) }
        val gpToggleRequester = remember { FocusRequester() }
        Toggle(
            label = tr("Gamepad"),
            description = tr("Enable audio output for the Wii U Gamepad"),
            initialCheckedState = { NativeSettings.getAudioDeviceEnabled(false) },
            onCheckedChanged = { checked: Boolean -> NativeSettings.setAudioDeviceEnabled(checked, false) },
            modifier = Modifier
                .focusRequester(gpToggleRequester)
                .onFocusChanged { state -> gpToggleFocused = state.isFocused }
                .then(if (gpToggleFocused) Modifier.border(2.dp, Color.Cyan).padding(4.dp) else Modifier)
        )

        // Gamepad channels
        var gpChannelsFocused by remember { mutableStateOf(false) }
        val gpChannelsRequester = remember { FocusRequester() }
        SingleSelection(
            label = tr("Gamepad channels"),
            initialChoice = { NativeSettings.getAudioDeviceChannels(false) },
            onChoiceChanged = { choice: Int -> NativeSettings.setAudioDeviceChannels(choice, false) },
            choiceToString = { channels: Int -> channelsToString(channels) },
            choices = listOf(
                NativeSettings.AudioChannels.STEREO,
            ),
            modifier = Modifier
                .focusRequester(gpChannelsRequester)
                .onFocusChanged { state -> gpChannelsFocused = state.isFocused }
                .then(if (gpChannelsFocused) Modifier.border(2.dp, Color.Cyan).padding(4.dp) else Modifier)
        )

        // Gamepad volume
        var gpVolumeFocused by remember { mutableStateOf(false) }
        val gpVolumeRequester = remember { FocusRequester() }
        Slider(
            label = tr("Gamepad volume"),
            initialValue = { NativeSettings.getAudioDeviceVolume(false) },
            valueFrom = NativeSettings.AUDIO_MIN_VOLUME,
            steps = AUDIO_VOLUME_STEPS,
            valueTo = NativeSettings.AUDIO_MAX_VOLUME,
            onValueChange = { v: Int -> NativeSettings.setAudioDeviceVolume(v, false) },
            labelFormatter = { v: Int -> "$v%" },
            modifier = Modifier
                .focusRequester(gpVolumeRequester)
                .onFocusChanged { state -> gpVolumeFocused = state.isFocused }
                .then(if (gpVolumeFocused) Modifier.border(2.dp, Color.Cyan).padding(4.dp) else Modifier)
        )
    }
}

fun channelsToString(channels: Int) = when (channels) {
    NativeSettings.AudioChannels.MONO -> tr("Mono")
    NativeSettings.AudioChannels.STEREO -> tr("Stereo")
    NativeSettings.AudioChannels.SURROUND -> tr("Surround")
    else -> throw IllegalArgumentException("Invalid channels type: $channels")
}
