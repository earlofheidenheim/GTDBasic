package com.beispiel.gtdbasic

import com.beispiel.gtdbasic.model.Step

object StepStore {

    // projectId -> Liste von Steps (≈ Relation Project → Steps)
    private val stepsByProjectId = mutableMapOf<Long, MutableList<Step>>()

    fun getSteps(projectId: Long): MutableList<Step> {
        return stepsByProjectId.getOrPut(projectId) {
            mutableListOf(
                Step(id = 1, projectId = projectId, name = "Step 1", sortOrder = 1),
                Step(id = 2, projectId = projectId, name = "Step 2", sortOrder = 2),
                Step(id = 3, projectId = projectId, name = "Step 3", sortOrder = 3)
            )
        }
    }
}


