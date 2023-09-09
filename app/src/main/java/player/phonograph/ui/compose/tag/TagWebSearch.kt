/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag

import org.jaudiotagger.tag.FieldKey
import util.phonograph.tagsources.lastfm.LastFmTrack
import util.phonograph.tagsources.musicbrainz.MusicBrainzRecording


internal fun process(viewModel: TagEditorScreenViewModel, item: Any) {
    val tableViewModel = viewModel.audioDetail.value?.tagInfoTableViewModel
    if (tableViewModel != null) {
        when (item) {
            is LastFmTrack          -> insert(tableViewModel, item)
            is MusicBrainzRecording -> insert(tableViewModel, item)
        }
    }
}

private fun insert(tableViewModel: TagInfoTableViewModel, track: LastFmTrack) =
    with(ProcessScope(tableViewModel)) {

        link(FieldKey.MUSICBRAINZ_TRACK_ID, track.mbid)
        link(FieldKey.TITLE, track.name)
        link(FieldKey.ARTIST, track.artist?.name)
        link(FieldKey.ALBUM, track.album?.name)

        val tags = track.toptags?.tag?.map { it.name }
        link(FieldKey.COMMENT, tags)
        link(FieldKey.GENRE, tags)

    }

private fun insert(tableViewModel: TagInfoTableViewModel, recording: MusicBrainzRecording) =
    with(ProcessScope(tableViewModel)) {
        link(FieldKey.MUSICBRAINZ_TRACK_ID, recording.id)
        link(FieldKey.TITLE, recording.title)

        val artists = recording.artistCredit.map { it.name }
        link(FieldKey.ARTIST, artists)
        val releases = recording.releases?.map { it.title }
        link(FieldKey.ALBUM, releases)

        val genre = recording.genres.map { it.name }
        val tags = recording.tags?.map { it.name }
        link(FieldKey.GENRE, genre)
        link(FieldKey.COMMENT, tags)
        link(FieldKey.COMMENT, recording.disambiguation)
        link(FieldKey.YEAR, recording.firstReleaseDate)
    }

private class ProcessScope(val tableViewModel: TagInfoTableViewModel) {
    fun link(fieldKey: FieldKey, value: String?) {
        if (value != null) tableViewModel.insertPrefill(fieldKey, value)
    }

    fun link(fieldKey: FieldKey, values: List<String>?) {
        if (values != null) tableViewModel.insertPrefill(fieldKey, values)
    }
}