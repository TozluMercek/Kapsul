package com.example.kapsul.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Note::class, NoteLink::class, Attachment::class, Folder::class], version = 5, exportSchema = false)
@TypeConverters(Converters::class)
abstract class KapsulDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        /** v2 adds attachments (images, recordings, PDFs, drawings, sticky notes). */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `attachments` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `noteId` INTEGER NOT NULL, " +
                        "`type` TEXT NOT NULL, `fileName` TEXT, `label` TEXT NOT NULL, `text` TEXT NOT NULL, " +
                        "`color` INTEGER NOT NULL, `durationMs` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`noteId`) REFERENCES `notes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_attachments_noteId` ON `attachments` (`noteId`)")
            }
        }

        /** v3 adds the note style: a cover design and a page template. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `cover` TEXT")
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `pageStyle` TEXT")
            }
        }

        /** v4 adds reminders and the background style. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `reminderAt` INTEGER")
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `reminderMode` TEXT")
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `reminderRepeat` TEXT")
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `background` TEXT")
            }
        }

        /** v5 adds trash, folders, note lock, time capsules and the context capsule. */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `deletedAt` INTEGER")
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `folderId` INTEGER")
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `locked` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `capsuleUntil` INTEGER")
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `capsuleCipher` TEXT")
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `context` TEXT")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `folders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `parentId` INTEGER, `createdAt` INTEGER NOT NULL)"
                )
            }
        }

        fun create(context: Context): KapsulDatabase =
            Room.databaseBuilder(context, KapsulDatabase::class.java, "kapsul.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
    }
}
