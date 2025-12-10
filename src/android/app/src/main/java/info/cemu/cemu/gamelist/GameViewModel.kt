package info.cemu.cemu.gamelist

import androidx.lifecycle.ViewModel
import info.cemu.cemu.nativeinterface.NativeGameTitles.Game

class GameViewModel(var game: Game? = null) : ViewModel()
