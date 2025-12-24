package com.beispiel.gtdbasic.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) für die "steps"-Tabelle.
 */
@Dao
interface StepDao {

    /**
     * Fügt einen oder mehrere Schritte in die Datenbank ein.
     */
    @Insert
    suspend fun insert(vararg steps: Step)

    /**
     * Aktualisiert einen bestehenden Step in der Datenbank.
     */
    @Update
    suspend fun update(step: Step)

    /**
     * Aktualisiert eine Liste von Steps.
     */
    @Update
    suspend fun update(steps: List<Step>)

    /**
     * Löscht einen Step aus der Datenbank.
     */
    @Delete
    suspend fun delete(step: Step)

    /**
     * Liest alle Schritte für ein bestimmtes Projekt, sortiert nach der "sort_order".
     * Gibt einen Flow zurück, um die UI automatisch zu aktualisieren.
     */
    @Query("SELECT * FROM steps WHERE project_id = :projectId ORDER BY sort_order ASC")
    fun getStepsForProject(projectId: Long): Flow<List<Step>>
}
