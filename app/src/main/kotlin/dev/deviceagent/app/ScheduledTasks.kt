package dev.deviceagent.app

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/** A recurring task definition. Scheduled tasks are manual by default for safety. */
data class ScheduledTask(
    val id: String,
    val goal: String,
    val model: String,
    val rootNames: List<String>,
    val rootUris: List<String>,
    val intervalHours: Long,
    val sendContents: Boolean = false,
    val dryRun: Boolean = false,
    val manual: Boolean = true,
)

object ScheduledTasks {
    private const val PREFIX = "agent-scheduled-"
    private const val KEY_GOAL = "goal"
    private const val KEY_MODEL = "model"
    private const val KEY_ROOT_NAMES = "root_names"
    private const val KEY_ROOT_URIS = "root_uris"
    private const val KEY_SEND_CONTENTS = "send_contents"
    private const val KEY_DRY_RUN = "dry_run"
    private const val KEY_MANUAL = "manual"

    fun schedule(context: Context, task: ScheduledTask) {
        require(task.id.matches(Regex("[A-Za-z0-9_-]{1,80}"))) { "Invalid scheduled task id" }
        require(task.goal.isNotBlank() && task.model.isNotBlank()) { "Scheduled task goal and model are required" }
        require(task.rootNames.size == task.rootUris.size && task.rootNames.isNotEmpty()) { "Scheduled tasks need matching approved roots" }
        require(task.intervalHours >= 1) { "Scheduled task interval must be at least one hour" }
        val input = Data.Builder()
            .putString(KEY_GOAL, task.goal)
            .putString(KEY_MODEL, task.model)
            .putStringArray(KEY_ROOT_NAMES, task.rootNames.toTypedArray())
            .putStringArray(KEY_ROOT_URIS, task.rootUris.toTypedArray())
            .putBoolean(KEY_SEND_CONTENTS, task.sendContents)
            .putBoolean(KEY_DRY_RUN, task.dryRun)
            // Explicit opt-in is required for non-manual execution; the public data class defaults true.
            .putBoolean(KEY_MANUAL, task.manual)
            .build()
        val request = PeriodicWorkRequestBuilder<ScheduledTaskWorker>(task.intervalHours, TimeUnit.HOURS)
            .setInputData(input)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(PREFIX + task.id, ExistingPeriodicWorkPolicy.UPDATE, request)
        AgentDatabase.get(context).scheduledTaskDao().upsert(task.toEntity())
    }

    fun cancel(context: Context, id: String) {
        WorkManager.getInstance(context).cancelUniqueWork(PREFIX + id)
        AgentDatabase.get(context).scheduledTaskDao().delete(id)
    }

    fun list(context: Context): List<ScheduledTask> = AgentDatabase.get(context).scheduledTaskDao().all().map { it.toTask() }

    private const val SEPARATOR = "\u001f"
    private fun ScheduledTask.toEntity() = ScheduledTaskEntity(id, goal, model, rootNames.joinToString(SEPARATOR), rootUris.joinToString(SEPARATOR), intervalHours, sendContents, dryRun, manual)
    private fun ScheduledTaskEntity.toTask() = ScheduledTask(id, goal, model, rootNames.split(SEPARATOR).filter(String::isNotEmpty), rootUris.split(SEPARATOR).filter(String::isNotEmpty), intervalHours, sendContents, dryRun, manual)
}

class ScheduledTaskWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val goal = inputData.getString("goal") ?: return Result.failure()
        val model = inputData.getString("model") ?: return Result.failure()
        val names = inputData.getStringArray("root_names")?.toList().orEmpty()
        val uris = inputData.getStringArray("root_uris")?.toList().orEmpty()
        if (names.isEmpty() || names.size != uris.size) return Result.failure()
        val intent = AgentTaskService.createStartIntent(
            applicationContext,
            goal = goal,
            model = model,
            dryRun = inputData.getBoolean("dry_run", false),
            manual = inputData.getBoolean("manual", true), // default is manual; non-manual requires explicit caller opt-in
            sendContents = inputData.getBoolean("send_contents", false),
            rootNames = names,
            rootUris = uris,
        )
        return try {
            ContextCompat.startForegroundService(applicationContext, intent)
            Result.success()
        } catch (_: IllegalStateException) {
            Result.retry()
        } catch (_: SecurityException) {
            Result.failure()
        }
    }
}
