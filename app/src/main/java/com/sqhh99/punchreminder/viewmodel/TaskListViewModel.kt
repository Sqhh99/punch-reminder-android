package com.sqhh99.punchreminder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sqhh99.punchreminder.data.repository.TaskRepository
import com.sqhh99.punchreminder.domain.model.PunchTask
import com.sqhh99.punchreminder.domain.scheduler.NextTriggerTimeCalculator
import com.sqhh99.punchreminder.domain.usecase.TaskScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDateTime

data class TaskListItem(
    val task: PunchTask,
    val nextTrigger: LocalDateTime?,
)

data class TaskListUiState(
    val items: List<TaskListItem> = emptyList(),
    val loading: Boolean = true,
)

class TaskListViewModel(
    private val repository: TaskRepository,
    private val taskScheduler: TaskScheduler,
    private val calculator: NextTriggerTimeCalculator = NextTriggerTimeCalculator(),
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    val uiState: StateFlow<TaskListUiState> = repository.tasks
        .map { tasks ->
            val now = LocalDateTime.now(clock)
            TaskListUiState(
                items = tasks.map { task ->
                    TaskListItem(
                        task = task,
                        nextTrigger = if (task.enabled) calculator.nextTrigger(task, now) else null,
                    )
                },
                loading = false,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskListUiState())

    fun toggleEnabled(taskId: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.setEnabled(taskId, enabled)
            taskScheduler.reschedule(taskId)
        }
    }

    fun delete(taskId: String) {
        viewModelScope.launch {
            repository.delete(taskId)
            taskScheduler.onTaskRemoved(taskId)
        }
    }
}
