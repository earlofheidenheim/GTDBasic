package com.beispiel.gtdbasic.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "steps",
    foreignKeys = [ForeignKey(
        entity = Project::class,
        parentColumns = ["id"],
        childColumns = ["project_id"],
        onDelete = ForeignKey.CASCADE // Wichtig: Löscht alle Schritte, wenn das zugehörige Projekt gelöscht wird.
    )]
)
data class Step(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "project_id", index = true) // `index = true` verbessert die Abfrageleistung.
    val projectId: Long,

    @ColumnInfo(name = "name")
    var name: String,

    @ColumnInfo(name = "sort_order")
    var sortOrder: Int,

    @ColumnInfo(name = "ziel_zeit_seconds", defaultValue = "0")
    var zielZeitInSeconds: Long = 0,

    @ColumnInfo(name = "dauer_seconds", defaultValue = "0")
    var dauerInSeconds: Long = 0,

    @ColumnInfo(name = "is_running", defaultValue = "0")
    var isRunning: Boolean = false,

    @ColumnInfo(name = "notes", defaultValue = "")
    var notes: String = "",

    // Zeitstempel, wann der Timer gestartet wurde
    @ColumnInfo(name = "start_time_millis", defaultValue = "0")
    var startTimeMillis: Long = 0
)
