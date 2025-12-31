package com.beispiel.gtdbasic.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.beispiel.gtdbasic.database.AppDatabase
import com.beispiel.gtdbasic.model.Category
import com.beispiel.gtdbasic.model.Project
import com.beispiel.gtdbasic.model.Status
import com.beispiel.gtdbasic.model.Step
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class GtdViewModel(application: Application) : AndroidViewModel(application) {

    private val projectDao = AppDatabase.getDatabase(application).projectDao()
    private val stepDao = AppDatabase.getDatabase(application).stepDao()
    private val categoryDao = AppDatabase.getDatabase(application).categoryDao()
    private val statusDao = AppDatabase.getDatabase(application).statusDao()

    private val _isDemoMode = MutableStateFlow(false)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode

    fun setDemoMode(isDemo: Boolean) {
        _isDemoMode.value = isDemo
    }

    private val ticker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000)
        }
    }

    // --- Project-Funktionen ---

    private val _categoryFilter = MutableStateFlow<String?>(null)
    private val _statusFilter = MutableStateFlow<String?>(null)

    val projects: LiveData<List<Project>> = combine(
        _isDemoMode, _categoryFilter, _statusFilter
    ) { isDemo, category, status ->
        Triple(isDemo, category, status)
    }.flatMapLatest { (isDemo, category, status) ->
        when {
            category != null && status != null -> projectDao.getProjectsByCategoryAndStatus(isDemo, category, status)
            category != null -> projectDao.getProjectsByCategory(isDemo, category)
            status != null -> projectDao.getProjectsByStatus(isDemo, status)
            else -> projectDao.getAllProjects(isDemo)
        }
    }.asLiveData()

    val allCategories: LiveData<List<String>> = _isDemoMode.flatMapLatest { isDemo ->
        projectDao.getAllCategories(isDemo)
    }.asLiveData()

    val allStatuses: LiveData<List<String>> = _isDemoMode.flatMapLatest { isDemo ->
        projectDao.getAllStatuses(isDemo)
    }.asLiveData()

    fun setCategoryFilter(category: String?) {
        _categoryFilter.value = category
    }

    fun setStatusFilter(status: String?) {
        _statusFilter.value = status
    }

    fun renameCategory(oldName: String, newName: String) = viewModelScope.launch {
        projectDao.renameCategory(oldName, newName, _isDemoMode.value)
        categoryDao.deleteByName(oldName)
        if (newName.isNotBlank()) {
            categoryDao.insert(Category(name = newName))
        }
    }

    fun renameStatus(oldName: String, newName: String) = viewModelScope.launch {
        projectDao.renameStatus(oldName, newName, _isDemoMode.value)
        statusDao.deleteByName(oldName)
        if (newName.isNotBlank()) {
            statusDao.insert(Status(name = newName))
        }
    }

    fun insertProject(project: Project) = viewModelScope.launch {
        val projectToInsert = project.copy(isDemo = _isDemoMode.value)
        projectDao.insert(projectToInsert)
        if (projectToInsert.kategorie.isNotBlank()) {
            categoryDao.insert(Category(name = projectToInsert.kategorie))
        }
        if (projectToInsert.status.isNotBlank()) {
            statusDao.insert(Status(name = projectToInsert.status))
        }
    }

    fun updateProject(project: Project) = viewModelScope.launch {
        projectDao.update(project)
        if (project.kategorie.isNotBlank()) {
            categoryDao.insert(Category(name = project.kategorie))
        }
        if (project.status.isNotBlank()) {
            statusDao.insert(Status(name = project.status))
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

    private val stepsForProjectFlows = mutableMapOf<Long, LiveData<List<Step>>>()
    private val singleStepFlows = mutableMapOf<Long, LiveData<Step>>()

    fun getStepsForProject(projectId: Long): LiveData<List<Step>> {
        return stepsForProjectFlows.getOrPut(projectId) {
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
            }.asLiveData()
        }
    }

    fun getStepById(stepId: Long): LiveData<Step> {
        return singleStepFlows.getOrPut(stepId) {
            val dbStepFlow = stepDao.getStepById(stepId)

            val isRunningFlow = dbStepFlow
                .map { it.isRunning }
                .distinctUntilChanged()

            isRunningFlow.flatMapLatest { isRunning ->
                if (isRunning) {
                    dbStepFlow.combine(ticker) { step, currentTime ->
                        if (step.isRunning && step.startTimeMillis > 0) {
                            val elapsedSeconds = (currentTime - step.startTimeMillis) / 1000
                            step.copy(dauerInSeconds = step.dauerInSeconds + elapsedSeconds)
                        } else {
                            step
                        }
                    }
                } else {
                    dbStepFlow
                }
            }.asLiveData()
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

    // --- Status-Funktionen ---

    val allStatusNames: LiveData<List<String>> = statusDao.getAllStatusNames().asLiveData()
}
