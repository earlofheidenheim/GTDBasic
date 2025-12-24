package com.beispiel.gtdbasic.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) für die "projects"-Tabelle.
 * Definiert die Methoden für den Datenbankzugriff.
 */
@Dao
interface ProjectDao {

    /**
     * Fügt ein oder mehrere Projekte in die Datenbank ein.
     */
    @Insert
    suspend fun insert(vararg projects: Project)

    /**
     * Aktualisiert ein bestehendes Projekt in der Datenbank.
     */
    @Update
    suspend fun update(project: Project)

    /**
     * Aktualisiert eine Liste von Projekten in der Datenbank.
     */
    @Update
    suspend fun update(projects: List<Project>)

    /**
     * Löscht ein Projekt aus der Datenbank.
     */
    @Delete
    suspend fun delete(project: Project)

    /**
     * Liest alle Projekte aus der Datenbank, sortiert nach der "sort_order".
     * Gibt einen Flow zurück, sodass die UI automatisch aktualisiert wird,
     * wenn sich die Daten ändern.
     */
    @Query("SELECT * FROM projects ORDER BY sort_order ASC")
    fun getAllProjects(): Flow<List<Project>>
}
