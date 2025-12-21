package com.beispiel.gtdbasic.model

data class Step(
    val id: Long,          // Primärschlüssel (≈ ID-Feld)
    val projectId: Long,   // Fremdschlüssel (≈ Beziehung zu Project)
    var name: String,     // Beschreibung (≈ Textfeld)
    var sortOrder: Int     // Reihenfolge (≈ Sortierfeld)
)

