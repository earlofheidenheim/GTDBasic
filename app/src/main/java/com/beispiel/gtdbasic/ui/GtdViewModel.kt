package com.beispiel.gtdbasic.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.beispiel.gtdbasic.database.AppDatabase
import com.beispiel.gtdbasic.model.Category
import com.beispiel.gtdbasic.model.Project
import com.beispiel.gtdbasic.model.Step
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class GtdViewModel(application: Application) : AndroidViewModel(application) {

    private val projectDao = AppDatabase.getDatabase(application).projectDao()
    private val stepDao = AppDatabase.getDatabase(application).stepDao()
    private val categoryDao = AppDatabase.getDatabase(application).categoryDao()

    private val ticker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000)
        }
    }

    // --- Project-Funktionen ---

    val allProjects: LiveData<List<Project>> = projectDao.getAllProjects().asLiveData()

    fun insertProject(project: Project) = viewModelScope.launch {
        projectDao.insert(project)
        if (project.kategorie.isNotBlank()) {
            categoryDao.insert(Category(name = project.kategorie))
        }
    }

    fun updateProject(project: Project) = viewModelScope.launch {
        projectDao.update(project)
        if (project.kategorie.isNotBlank()) {
            categoryDao.insert(Category(name = project.kategorie))
        }
    }

    fun updateProjectSortOrder(projects: List<Project>) = viewModelScope.launch {
        for ((index, project) in projects.withIndex()) {
            project.sortOrder = index + 1
        }
        projectDao.update(projects)
    }

    fun deleteProject(project: Project) = viewModelScope.launch {
        projectDao.delete(project)
    }

    // --- Step-Funktionen ---

    private val stepFlows = mutableMapOf<Long, LiveData<List<Step>>>()

    fun getStepsForProject(projectId: Long): LiveData<List<Step>> {
        return stepFlows.getOrPut(projectId) {
            val dbStepsFlow = stepDao.getStepsForProject(projectId)

            val isAnyStepRunningFlow = dbStepsFlow
                .map { steps -> steps.any { it.isRunning } }
                .distinctUntilChanged()

            isAnyStepRunningFlow.flatMapLatest { isRunning ->
                if (isRunning) {
                    dbStepsFlow.combine(ticker) { steps, currentTime ->
                        steps.map { step ->
                            if (step.isRunning && step.startTimeMillis > 0) {
                                val elapsedSeconds = (currentTime - step.startTimeMillis) / 1000
                                step.copy(dauerInSeconds = step.dauerInSeconds + elapsedSeconds)
                            } else {
                                step
                            }
                        }
                    }
                } else {
                    dbStepsFlow
                }
            }.asLiveData(viewModelScope.coroutineContext)
        }
    }

    fun insertStep(step: Step) = viewModelScope.launch {
        stepDao.insert(step)
    }

    fun updateStep(step: Step) = viewModelScope.launch {
        stepDao.update(step)
    }

    fun updateStepSortOrder(steps: List<Step>) = viewModelScope.launch {
        for ((index, step) in steps.withIndex()) {
            step.sortOrder = index + 1
        }
        stepDao.update(steps)
    }

    fun deleteStep(step: Step) = viewModelScope.launch {
        stepDao.delete(step)
    }

    fun togglePlayPause(step: Step) = viewModelScope.launch {
        val isNowRunning = !step.isRunning

        if (isNowRunning) {
            // Start
            val updatedStep = step.copy(
                isRunning = true,
                startTimeMillis = System.currentTimeMillis()
            )
            stepDao.update(updatedStep)
        } else {
            // Pause
            val elapsedSeconds = if (step.startTimeMillis > 0) {
                (System.currentTimeMillis() - step.startTimeMillis) / 1000
            } else {
                0L
            }
            val updatedStep = step.copy(
                isRunning = false,
                dauerInSeconds = step.dauerInSeconds + elapsedSeconds,
                startTimeMillis = 0
            )
            stepDao.update(updatedStep)
        }
    }

    fun resetStepDuration(step: Step) = viewModelScope.launch {
        val updatedStep = step.copy(dauerInSeconds = 0, startTimeMillis = if (step.isRunning) System.currentTimeMillis() else 0)
        stepDao.update(updatedStep)
    }

    // --- Category-Funktionen ---

    val allCategoryNames: LiveData<List<String>> = categoryDao.getAllCategoryNames().asLiveData()
}
