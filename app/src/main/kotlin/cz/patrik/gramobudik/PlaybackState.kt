package cz.patrik.gramobudik

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PlaybackState {

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentSongName = MutableStateFlow("")
    val currentSongName: StateFlow<String> = _currentSongName.asStateFlow()

    fun update(playing: Boolean, songName: String = "") {
        _currentSongName.value = songName
        _isPlaying.value = playing
    }
}
