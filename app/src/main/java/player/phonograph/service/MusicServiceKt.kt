/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.service

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import player.phonograph.App
import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.model.playlist.Playlist
import player.phonograph.service.queue.SHUFFLE_MODE_NONE
import player.phonograph.service.queue.ShuffleMode
import player.phonograph.util.MusicUtil.getSongFileUri

object MusicServiceKt {
    private const val ANDROID_MUSIC_PACKAGE_NAME = "com.android.music"

    @JvmStatic
    fun sendPublicIntent(service: MusicService, what: String) {
        service.sendStickyBroadcast(
            Intent(what.replace(MusicService.PHONOGRAPH_PACKAGE_NAME, ANDROID_MUSIC_PACKAGE_NAME)).apply {
                val song: Song = App.instance.queueManager.currentSong
                putExtra("id", song.id)
                putExtra("artist", song.artistName)
                putExtra("album", song.albumName)
                putExtra("track", song.title)
                putExtra("duration", song.duration)
                putExtra("position", service.songProgressMillis.toLong())
                putExtra("playing", service.isPlaying)
                putExtra("scrobbling_source", MusicService.PHONOGRAPH_PACKAGE_NAME)
            }
        )
    }

    @JvmStatic
    fun getTrackUri(song: Song): Uri {
        return getSongFileUri(song.id)
    }

    @JvmStatic
    fun parsePlaylistAndPlay(intent: Intent, service: MusicService) {
        val playlist: Playlist? = intent.getParcelableExtra(
            MusicService.INTENT_EXTRA_PLAYLIST
        )
        val playlistSongs = playlist?.getSongs(service)
        val shuffleMode = ShuffleMode.deserialize(
            intent.getIntExtra(MusicService.INTENT_EXTRA_SHUFFLE_MODE, SHUFFLE_MODE_NONE)
        )
        if (playlistSongs.isNullOrEmpty()) {
            Toast.makeText(service, R.string.playlist_is_empty, Toast.LENGTH_LONG).show()
        } else {
            val queueManager = App.instance.queueManager
            queueManager.switchShuffleMode(shuffleMode)
            // TODO: keep the queue intact
            val queue =
                if (shuffleMode == ShuffleMode.SHUFFLE) playlistSongs.toMutableList().apply { shuffle() } else playlistSongs
            service.openQueue(queue, 0, true)
        }
    }
}
