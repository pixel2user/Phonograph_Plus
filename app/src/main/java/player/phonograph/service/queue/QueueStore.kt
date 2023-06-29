/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.service.queue

import org.intellij.lang.annotations.Language
import player.phonograph.model.Song
import player.phonograph.repo.mediastore.loaders.SongLoader
import player.phonograph.util.warning
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import android.provider.MediaStore.Audio.AudioColumns
import java.sql.SQLException

class QueueStore(context: Context) : SQLiteOpenHelper(
    context, PLAYBACK_QUEUE_DB, null, VERSION
) {

    override fun onCreate(db: SQLiteDatabase) {
        createTable(db, PLAYING_QUEUE_TABLE_NAME)
        createTable(db, ORIGINAL_PLAYING_QUEUE_TABLE_NAME)
    }

    private fun dropAll(db: SQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS $PLAYING_QUEUE_TABLE_NAME")
        db.execSQL("DROP TABLE IF EXISTS $ORIGINAL_PLAYING_QUEUE_TABLE_NAME")
        onCreate(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        dropAll(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        dropAll(db)
    }

    @Synchronized
    fun save(playingQueue: List<Song>, originalPlayingQueue: List<Song>) {
        saveQueue(PLAYING_QUEUE_TABLE_NAME, playingQueue)
        saveQueue(ORIGINAL_PLAYING_QUEUE_TABLE_NAME, originalPlayingQueue)
    }

    @Synchronized
    private fun saveQueue(tableName: String, queue: List<Song>) {
        val contentValues = queue.map(Companion::saveSong)
        saveQueueImpl(tableName, contentValues)
    }

    @Synchronized
    private fun saveQueueImpl(tableName: String, contentValues: List<ContentValues>) {
        val database = writableDatabase
        val failed = mutableListOf<ContentValues>()
        database.beginTransaction()
        try {
            // remove all
            database.delete(tableName, null, null)
            // insert all
            for (values in contentValues) {
                try {
                    database.insertOrThrow(tableName, null, values)
                } catch (e: SQLException) {
                    failed.add(values)
                }
            }
            // complete
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
        if (failed.isNotEmpty()) {
            warning(
                PLAYBACK_QUEUE_DB,
                failed.fold("Failed to save songs to $tableName: ")
                { acc, s -> "$acc,${s.getAsString(AudioColumns.TITLE)}" }
            )
        }
    }

    fun savedPlayingQueue(context: Context): List<Song> = read(context, PLAYING_QUEUE_TABLE_NAME)
    fun savedOriginalPlayingQueue(context: Context): List<Song> = read(context, ORIGINAL_PLAYING_QUEUE_TABLE_NAME)


    private fun read(context: Context, tableName: String): List<Song> {
        val songs = mutableListOf<Song?>()
        readableDatabase.query(
            tableName, null,
            null, null, null, null, null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                do {
                    songs.add(readSong(context, cursor))
                } while (cursor.moveToNext())
            }
        }
        return songs.filterNotNull()
    }


    companion object {
        const val PLAYBACK_QUEUE_DB = "playback_queue.db"
        const val VERSION = 1

        const val PLAYING_QUEUE_TABLE_NAME = "playing_queue"
        const val ORIGINAL_PLAYING_QUEUE_TABLE_NAME = "original_playing_queue"


        @Language("SQL")
        private const val mainTable: String =
            "${BaseColumns._ID} LONG NOT NULL" +
                    ", ${AudioColumns.TITLE} TEXT NOT NULL" +
                    ", ${AudioColumns.DATA} TEXT NOT NULL" +
                    ", ${AudioColumns.ALBUM_ID} LONG NOT NULL" +
                    ", ${AudioColumns.ALBUM} TEXT NOT NULL" +
                    ", ${AudioColumns.ARTIST_ID} LONG NOT NULL" +
                    ", ${AudioColumns.ARTIST} TEXT NOT NULL"

        private fun saveSong(song: Song): ContentValues =
            ContentValues(7).apply {
                put(BaseColumns._ID, song.id)
                put(AudioColumns.TITLE, song.title)
                put(AudioColumns.DATA, song.data)
                put(AudioColumns.ALBUM_ID, song.albumId)
                put(AudioColumns.ALBUM, song.albumName)
                put(AudioColumns.ARTIST_ID, song.artistId)
                put(AudioColumns.ARTIST, song.artistName)
            }

        private fun readSong(context: Context, cursor: Cursor): Song? {
            val mediastoreId = cursor.getLong(0)
            val path = cursor.getString(2)
            return SongLoader.id(context, mediastoreId)
                .let { song ->
                    song.takeIf { it != Song.EMPTY_SONG }
                        ?: SongLoader.searchByPath(context, path).first()
                            .takeIf { it != Song.EMPTY_SONG }
                }
        }


        private fun createTable(db: SQLiteDatabase, tableName: String) {
            db.execSQL("CREATE TABLE IF NOT EXISTS $tableName ($mainTable);")
        }

        private var sInstance: QueueStore? = null
        @JvmStatic
        @Synchronized
        fun getInstance(context: Context): QueueStore {
            if (sInstance == null) {
                sInstance = QueueStore(context.applicationContext)
            }
            return sInstance!!
        }
    }
}