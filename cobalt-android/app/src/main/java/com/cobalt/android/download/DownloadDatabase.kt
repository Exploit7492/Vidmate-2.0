package com.cobalt.android.download

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cobalt.android.db.ResolutionCacheDao
import com.cobalt.android.db.daos.HistoryDao
import com.cobalt.android.db.daos.LikedDao
import com.cobalt.android.db.entities.HistoryEntity
import com.cobalt.android.db.entities.LikedEntity
import com.cobalt.android.db.entities.ResolutionCacheEntity
import com.cobalt.android.music.MusicPlaylist
import com.cobalt.android.music.MusicPlaylistDao

// Phase 9: History/Liked extend this database rather than ShortsDatabase or a
// new database of their own. ShortsDatabase (Phase 2) is deliberately an
// evictable *cache* — rows there expire and get dropped. History and likes
// are permanent user data, the same durability class as `DownloadRecord`
// and `ResolutionCacheEntity` already living here, so they belong alongside
// them, not next to data that's designed to be thrown away.
@Database(
    entities = [
        DownloadRecord::class,
        ResolutionCacheEntity::class,
        HistoryEntity::class,
        LikedEntity::class,
        MusicPlaylist::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(StatusConverters::class)
abstract class DownloadDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
    abstract fun resolutionCacheDao(): ResolutionCacheDao
    abstract fun historyDao(): HistoryDao
    abstract fun likedDao(): LikedDao
    abstract fun musicPlaylistDao(): MusicPlaylistDao

    companion object {
        @Volatile private var INSTANCE: DownloadDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS resolution_cache")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS resolution_cache (" +
                        "originalUrl TEXT NOT NULL PRIMARY KEY, " +
                        "formatsJson TEXT NOT NULL, " +
                        "resolvedAtMillis INTEGER NOT NULL)"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS history (" +
                        "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "itemType TEXT NOT NULL, " +
                        "refId TEXT NOT NULL, " +
                        "title TEXT NOT NULL, " +
                        "thumbnailUrl TEXT NOT NULL, " +
                        "sourceUrl TEXT NOT NULL, " +
                        "timestamp INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS liked (" +
                        "videoId TEXT NOT NULL PRIMARY KEY, " +
                        "title TEXT NOT NULL, " +
                        "authorName TEXT NOT NULL, " +
                        "thumbnailUrl TEXT NOT NULL, " +
                        "watchUrl TEXT NOT NULL, " +
                        "likedAt INTEGER NOT NULL)"
                )
            }
        }

        // Phase 20: adds the two columns TranscodeWorker/DownloadAdapter
        // need to tell a transcode-output row apart from a plain download
        // (see DownloadRecord's KDoc on sourceDownloadId/
        // transcodeProfileLabel) plus the CONVERTING status value added to
        // DownloadStatus — CONVERTING needs no schema change of its own
        // since `status` is already stored as its enum-name TEXT via
        // StatusConverters, but existing FAILED_NETWORK-style default
        // values below still need real defaults so pre-Phase-15 rows
        // backfill cleanly instead of ending up NULL.
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE downloads ADD COLUMN sourceDownloadId INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE downloads ADD COLUMN transcodeProfileLabel TEXT NOT NULL DEFAULT ''")
            }
        }

        // Phase B (gap-closure): the segmented-download engine persists its
        // part list, the origin ETag and the range-support flag on the row so
        // an interrupted transfer can resume. All three default to empty/false
        // so pre-existing rows stay valid single-stream downloads.
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE downloads ADD COLUMN segmentsJson TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE downloads ADD COLUMN etag TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE downloads ADD COLUMN rangeSupported INTEGER NOT NULL DEFAULT 0")
            }
        }

        // Gap-closure: the queue shows live transfer speed, so the engine's
        // measured bytes/second is stored on the row.
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE downloads ADD COLUMN speedBps INTEGER NOT NULL DEFAULT 0")
            }
        }

        // Gap-closure: user-created music playlists (G4).
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS music_playlists (" +
                        "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "name TEXT NOT NULL, " +
                        "trackUris TEXT NOT NULL, " +
                        "createdAt INTEGER NOT NULL)"
                )
            }
        }

        fun getInstance(context: Context): DownloadDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DownloadDatabase::class.java,
                    "cobalt_downloads.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7).build().also { INSTANCE = it }
            }
    }
}

