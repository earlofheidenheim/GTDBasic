package com.beispiel.gtdbasic.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_name` ON `categories` (`name`)")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Create a new temporary table with the correct new schema
        db.execSQL("""
            CREATE TABLE `steps_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `project_id` INTEGER NOT NULL, 
                `name` TEXT NOT NULL, 
                `sort_order` INTEGER NOT NULL, 
                `ziel_zeit_seconds` BIGINT NOT NULL DEFAULT 0, 
                `dauer_seconds` BIGINT NOT NULL DEFAULT 0, 
                `is_running` INTEGER NOT NULL DEFAULT 0, 
                `notes` TEXT NOT NULL DEFAULT '', 
                `start_time_millis` BIGINT NOT NULL DEFAULT 0, 
                FOREIGN KEY(`project_id`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """)

        // 2. Copy the data from the old table to the new one, converting minutes to seconds
        db.execSQL("""
            INSERT INTO `steps_new` (id, project_id, name, sort_order, ziel_zeit_seconds, dauer_seconds, is_running, notes, start_time_millis)
            SELECT id, project_id, name, sort_order, CAST(ziel_zeit AS BIGINT) * 60, CAST(dauer AS BIGINT) * 60, is_running, notes, 0 FROM `steps`
        """)

        // 3. Remove the old table
        db.execSQL("DROP TABLE `steps`")

        // 4. Rename the new table to the original table name
        db.execSQL("ALTER TABLE `steps_new` RENAME TO `steps`")
        
        // 5. Re-create the index that Room creates automatically for foreign keys
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_steps_project_id` ON `steps` (`project_id`)")
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `status` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_status_name` ON `status` (`name`)")
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `is_demo` INTEGER NOT NULL DEFAULT 0")
    }
}
