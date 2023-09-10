/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag2

import player.phonograph.coil.loadImage
import player.phonograph.coil.retriever.PARAMETERS_RAW
import player.phonograph.coil.target.PaletteTargetBuilder
import player.phonograph.mechanism.tag.loadSongInfo
import player.phonograph.model.Song
import player.phonograph.model.SongInfoModel
import player.phonograph.ui.compose.tag.EditAction
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

class TagEditorActivityViewModel : ViewModel() {

    private val _editable: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val editable get() = _editable.asStateFlow()

    private val _song: MutableStateFlow<Song> = MutableStateFlow(Song.EMPTY_SONG)
    val song get() = _song.asStateFlow()
    fun updateSong(context: Context, song: Song) {
        if (song != Song.EMPTY_SONG)
            readSongInfo(context, _song.updateAndGet { song })
    }

    private val _originalSongInfo: MutableStateFlow<SongInfoModel> = MutableStateFlow(SongInfoModel.EMPTY())
    val originalSongInfo get() = _originalSongInfo.asStateFlow()
    private val _songInfo: MutableStateFlow<SongInfoModel> = MutableStateFlow(SongInfoModel.EMPTY())
    val songInfo get() = _songInfo.asStateFlow()

    private val _songBitmap: MutableStateFlow<Bitmap?> = MutableStateFlow(null)
    val songBitmap get() = _songBitmap.asStateFlow()

    private val _color: MutableStateFlow<Color?> = MutableStateFlow(null)
    val color get() = _color.asStateFlow()

    private fun readSongInfo(context: Context, song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            val info = loadSongInfo(song)
            loadImage(context) {
                data(song)
                parameters(PARAMETERS_RAW)
                target(
                    PaletteTargetBuilder(context)
                        .onResourceReady { result: Drawable, paletteColor: Int ->
                            _songBitmap.tryEmit(result.toBitmap())
                            _color.tryEmit(Color(paletteColor))
                        }
                        .build()
                )
            }
            _originalSongInfo.emit(info)
            _songInfo.emit(info)
        }
    }

    fun saveArtwork(activity: Context) {
        val bitmap = songBitmap.value ?: return
        val fileName = fileName(song.value)
        saveArtwork(viewModelScope, activity, bitmap, fileName)
    }



    private var _pendingEditRequests: MutableList<EditAction> = mutableListOf()
    val pendingEditRequests: List<EditAction> get() = _pendingEditRequests.toList()

    fun process(event: TagInfoTableEvent) {
        when (event) {
            is TagInfoTableEvent.UpdateTag -> editTag(EditAction.Update(event.fieldKey, event.newValue))

            is TagInfoTableEvent.AddNewTag -> {
                //todo
                editTag(EditAction.Update(event.fieldKey, ""))
            }

            is TagInfoTableEvent.RemoveTag -> {
                //todo
                editTag(EditAction.Delete(event.fieldKey))
            }
        }
    }


    fun mergeActions() {
        _pendingEditRequests = EditAction.merge(_pendingEditRequests)
    }

    private fun editTag(action: EditAction): Boolean {
        return if (editable.value) {
            _pendingEditRequests.add(action)
            true
        } else {
            false
        }
    }

}