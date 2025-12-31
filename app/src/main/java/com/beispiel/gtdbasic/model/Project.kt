package com.beispiel.gtdbasic.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Repräsentiert ein Projekt in der Datenbank.
 * Jede Instanz dieser Klasse ist eine Zeile in der "projects"-Tabelle.
 */
@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    var name: String,

    @ColumnInfo(name = "sort_order")
    var sortOrder: Int,

    @ColumnInfo(name = "dauer", defaultValue = "0")
    var dauer: Int = 0,

    @ColumnInfo(name = "kategorie", defaultValue = "")
    var kategorie: String = "",

    @ColumnInfo(name = "status", defaultValue = "")
    var status: String = "",

    @ColumnInfo(name = "notes", defaultValue = "")
    var notes: String = "",

    @ColumnInfo(name = "is_demo", defaultValue = "0")
    var isDemo: Boolean = false
)
