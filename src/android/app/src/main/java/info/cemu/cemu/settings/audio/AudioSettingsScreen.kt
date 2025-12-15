package info.cemu.cemu.settings.audio

import androidx.compose.runtime.Composable
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
        Slider(
            label = tr("Latency"),
            initialValue = { NativeSettings.getAudioLatency() },
            valueFrom = 0,
            valueTo = NativeSettings.AUDIO_LATENCY_MS_MAX,
            steps = AUDIO_LATENCY_STEPS,
            labelFormatter = { v: Int -> "${v}ms" },
            onValueChange = { v: Int -> NativeSettings.setAudioLatency(v) }
        )

        // TV toggle
        Toggle(
            label = tr("TV"),
            description = tr("Enable audio output for the Wii U TV"),
            initialCheckedState = { NativeSettings.getAudioDeviceEnabled(true) },
            onCheckedChanged = { checked: Boolean -> NativeSettings.setAudioDeviceEnabled(checked, true) }
        )

        // TV channels
        SingleSelection(
            label = tr("TV channels"),
            initialChoice = { NativeSettings.getAudioDeviceChannels(true) },
            onChoiceChanged = { choice: Int -> NativeSettings.setAudioDeviceChannels(choice, true) },
            choiceToString = { channels: Int -> channelsToString(channels) },
            choices = listOf(
                NativeSettings.AudioChannels.MONO,
                NativeSettings.AudioChannels.STEREO,
                NativeSettings.AudioChannels.SURROUND,
            )
        )

        // TV volume
        Slider(
            label = tr("TV volume"),
            initialValue = { NativeSettings.getAudioDeviceVolume(true) },
            valueFrom = NativeSettings.AUDIO_MIN_VOLUME,
            valueTo = NativeSettings.AUDIO_MAX_VOLUME,
            steps = AUDIO_VOLUME_STEPS,
            labelFormatter = { v: Int -> "$v%" },
            onValueChange = { v: Int -> NativeSettings.setAudioDeviceVolume(v, true) }
        )

        // Gamepad toggle
        Toggle(
            label = tr("Gamepad"),
            description = tr("Enable audio output for the Wii U Gamepad"),
            initialCheckedState = { NativeSettings.getAudioDeviceEnabled(false) },
            onCheckedChanged = { checked: Boolean -> NativeSettings.setAudioDeviceEnabled(checked, false) }
        )

        // Gamepad channels
        SingleSelection(
            label = tr("Gamepad channels"),
            initialChoice = { NativeSettings.getAudioDeviceChannels(false) },
            onChoiceChanged = { choice: Int -> NativeSettings.setAudioDeviceChannels(choice, false) },
            choiceToString = { channels: Int -> channelsToString(channels) },
            choices = listOf(
                NativeSettings.AudioChannels.STEREO,
            )
        )

        // Gamepad volume
        Slider(
            label = tr("Gamepad volume"),
            initialValue = { NativeSettings.getAudioDeviceVolume(false) },
            valueFrom = NativeSettings.AUDIO_MIN_VOLUME,
            valueTo = NativeSettings.AUDIO_MAX_VOLUME,
            steps = AUDIO_VOLUME_STEPS,
            labelFormatter = { v: Int -> "$v%" },
            onValueChange = { v: Int -> NativeSettings.setAudioDeviceVolume(v, false) }
        )
    }
}

fun channelsToString(channels: Int) = when (channels) {
    NativeSettings.AudioChannels.MONO -> tr("Mono")
    NativeSettings.AudioChannels.STEREO -> tr("Stereo")
    NativeSettings.AudioChannels.SURROUND -> tr("Surround")
    else -> throw IllegalArgumentException("Invalid channels type: $channels")
}
