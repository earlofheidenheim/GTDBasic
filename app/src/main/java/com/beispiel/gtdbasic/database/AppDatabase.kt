package com.beispiel.gtdbasic.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.beispiel.gtdbasic.model.Category
import com.beispiel.gtdbasic.model.CategoryDao
import com.beispiel.gtdbasic.model.Project
import com.beispiel.gtdbasic.model.ProjectDao
import com.beispiel.gtdbasic.model.Step
import com.beispiel.gtdbasic.model.StepDao

/**
 * Die Room-Datenbank für die App.
 * Definiert die Liste der Entities und stellt die DAOs bereit.
 */
@Database(entities = [Project::class, Step::class, Category::class], version = 10, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao
    abstract fun stepDao(): StepDao
    abstract fun categoryDao(): CategoryDao

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
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
