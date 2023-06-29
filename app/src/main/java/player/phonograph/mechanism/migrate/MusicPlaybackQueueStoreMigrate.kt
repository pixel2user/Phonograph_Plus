/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism.migrate

import player.phonograph.repo.database.DatabaseConstants
import player.phonograph.repo.database.MusicPlaybackQueueStore
import player.phonograph.service.queue.QueueStore
import android.content.Context
import android.util.Log

fun migrateMusicPlaybackQueueStore(context: Context) {

    val oldDatabasePath = context.getDatabasePath(DatabaseConstants.MUSIC_PLAYBACK_STATE_DB)
    if (!oldDatabasePath.exists()) return // no data

    // export old data
    val musicPlaybackQueueStore = MusicPlaybackQueueStore.getInstance(context)
    val originalPlayingQueue = musicPlaybackQueueStore.savedOriginalPlayingQueue
    val playingQueue = musicPlaybackQueueStore.savedPlayingQueue

    // import new data
    val queueStore = QueueStore.getInstance(context)
    if (originalPlayingQueue.isNotEmpty() || playingQueue.isNotEmpty())
        queueStore.save(playingQueue, originalPlayingQueue)

    // delete
    try {
        if (oldDatabasePath.exists()) {
            musicPlaybackQueueStore.close()
            oldDatabasePath.delete()
        }
    } catch (e: Exception) {
        Log.e("MusicPlaybackQueueStoreMigrate", "Failed to delete old database", e)
    }

}