/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.fragments.player

import player.phonograph.App
import player.phonograph.R
import player.phonograph.mechanism.Favorite.isFavorite
import player.phonograph.model.Song
import player.phonograph.model.buildInfoString
import player.phonograph.model.getReadableDurationString
import player.phonograph.model.lyrics.LrcLyrics
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.util.reportError
import androidx.annotation.ColorInt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.content.res.Resources
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerFragmentViewModel : ViewModel() {

    private val _currentSong: MutableStateFlow<Song> = MutableStateFlow(Song.EMPTY_SONG)
    val currentSong get() = _currentSong.asStateFlow()

    fun updateCurrentSong(song: Song, context: Context?) {
        viewModelScope.launch {
            _currentSong.emit(song)
            updateFavoriteState(song, context)
        }
    }

    private var _favoriteState: MutableStateFlow<Pair<Song, Boolean>> =
        MutableStateFlow(Song.EMPTY_SONG to false)
    val favoriteState get() = _favoriteState.asStateFlow()

    private var loadFavoriteStateJob: Job? = null
    fun updateFavoriteState(song: Song, context: Context?) {
        loadFavoriteStateJob?.cancel()
        loadFavoriteStateJob = viewModelScope.launch {
            if (song == Song.EMPTY_SONG) return@launch
            _favoriteState.emit(song to isFavorite(context ?: App.instance, song))
        }
    }

    private val _paletteColor: MutableStateFlow<Int> = MutableStateFlow(0)
    val paletteColor get() = _paletteColor.asStateFlow()

    fun updatePaletteColor(@ColorInt newColor: Int) {
        viewModelScope.launch {
            _paletteColor.emit(newColor)
        }
    }


    private val _lyrics: MutableStateFlow<LrcLyrics?> = MutableStateFlow(null)
    val lyrics get() = _lyrics.asStateFlow()

    fun updateLrcLyrics(lyrics: LrcLyrics?) {
        viewModelScope.launch {
            _lyrics.emit(lyrics)
        }
    }

    private var _shownToolbar: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val showToolbar get() = _shownToolbar.asStateFlow()

    fun toggleToolbar() =
        _shownToolbar.tryEmit(
            !_shownToolbar.value
        )

    fun upNextAndQueueTime(resources: Resources): String {
        val duration = MusicPlayerRemote.getQueueDurationMillis(MusicPlayerRemote.position)
        return buildInfoString(
            resources.getString(R.string.up_next),
            getReadableDurationString(duration)
        )
    }
}
