package com.beispiel.gtdbasic.model

data class Project(
    val id: Long,          // Primärschlüssel (≈ ID-Feld)
    var name: String,      // Projektname (≈ Textfeld)
    var sortOrder: Int     // Reihenfolge (≈ Sortierfeld)
)
