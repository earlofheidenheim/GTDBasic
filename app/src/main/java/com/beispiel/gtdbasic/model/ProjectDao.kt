package com.beispiel.gtdbasic.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {

    @Insert
    suspend fun insert(vararg projects: Project)

    @Update
    suspend fun update(project: Project)

    @Update
    suspend fun update(projects: List<Project>)

    @Delete
    suspend fun delete(project: Project)

    @Query("SELECT * FROM projects WHERE is_demo = :isDemo ORDER BY sort_order ASC")
    fun getAllProjects(isDemo: Boolean): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE is_demo = :isDemo AND kategorie = :category ORDER BY sort_order ASC")
    fun getProjectsByCategory(isDemo: Boolean, category: String): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE is_demo = :isDemo AND status = :status ORDER BY sort_order ASC")
    fun getProjectsByStatus(isDemo: Boolean, status: String): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE is_demo = :isDemo AND kategorie = :category AND status = :status ORDER BY sort_order ASC")
    fun getProjectsByCategoryAndStatus(isDemo: Boolean, category: String, status: String): Flow<List<Project>>

    @Query("SELECT DISTINCT kategorie FROM projects WHERE is_demo = :isDemo AND kategorie != '' ORDER BY kategorie ASC")
    fun getAllCategories(isDemo: Boolean): Flow<List<String>>

    @Query("SELECT DISTINCT status FROM projects WHERE is_demo = :isDemo AND status != '' ORDER BY status ASC")
    fun getAllStatuses(isDemo: Boolean): Flow<List<String>>

    @Query("UPDATE projects SET kategorie = :newCategoryName WHERE kategorie = :oldCategoryName AND is_demo = :isDemo")
    suspend fun renameCategory(oldCategoryName: String, newCategoryName: String, isDemo: Boolean)

    @Query("UPDATE projects SET status = :newStatusName WHERE status = :oldStatusName AND is_demo = :isDemo")
    suspend fun renameStatus(oldStatusName: String, newStatusName: String, isDemo: Boolean)
}
