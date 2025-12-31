package com.beispiel.gtdbasic.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.beispiel.gtdbasic.model.Category
import com.beispiel.gtdbasic.model.CategoryDao
import com.beispiel.gtdbasic.model.Project
import com.beispiel.gtdbasic.model.ProjectDao
import com.beispiel.gtdbasic.model.Status
import com.beispiel.gtdbasic.model.StatusDao
import com.beispiel.gtdbasic.model.Step
import com.beispiel.gtdbasic.model.StepDao

/**
 * Die Room-Datenbank für die App.
 * Definiert die Liste der Entities und stellt die DAOs bereit.
 */
@Database(entities = [Project::class, Step::class, Category::class, Status::class], version = 14, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao
    abstract fun stepDao(): StepDao
    abstract fun categoryDao(): CategoryDao
    abstract fun statusDao(): StatusDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gtd_database"
                )
                .addMigrations(MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE steps ADD COLUMN exercise_duration_minutes INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE steps ADD COLUMN average_pulse INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE steps ADD COLUMN peak_pulse INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE steps ADD COLUMN average_load TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE steps ADD COLUMN fitness_level INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE steps ADD COLUMN repetitions INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE steps ADD COLUMN calories INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE steps ADD COLUMN distance_meters INTEGER NOT NULL DEFAULT 0")
    }
}
