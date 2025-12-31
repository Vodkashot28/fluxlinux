package com.ivarna.fluxlinux.core.utils



data class InstallTask(
    val id: String,
    val name: String,
    val type: TaskType,
    val scriptName: String? = null,
    val isManual: Boolean = false,
    val distroId: String, // Required for building intents
    val extraEnv: Map<String, String> = emptyMap()
)

enum class TaskType {
    BASE_INSTALL,
    HW_ACCEL,
    COMPONENT
}

object InstallationQueueManager {
    private val queue = ArrayDeque<InstallTask>()
    var currentTask: InstallTask? = null
        private set

    var activeDistroId: String? = null
        private set

    fun enqueue(tasks: List<InstallTask>) {
        if (tasks.isNotEmpty()) {
            activeDistroId = tasks.first().distroId
        }
        queue.addAll(tasks)
    }

    fun next(): InstallTask? {
        currentTask = queue.removeFirstOrNull()
        return currentTask
    }

    fun hasPending(): Boolean = queue.isNotEmpty()

    fun clear() {
        queue.clear()
        currentTask = null
        activeDistroId = null
    }
    
    fun peek(): InstallTask? = queue.firstOrNull()
}
