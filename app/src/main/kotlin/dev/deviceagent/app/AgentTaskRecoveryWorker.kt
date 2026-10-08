package dev.deviceagent.app

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Recovery watchdog for a foreground task. The service remains the single task executor;
 * this worker only relaunches it when the process died and left a checkpoint behind.
 */
class AgentTaskRecoveryWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        if (!AgentTaskService.hasCheckpoint(applicationContext)) return Result.success()
        if (AgentTaskService.isRunning) return Result.success()
        val intent = AgentTaskService.resumeIntent(applicationContext) ?: return Result.success()
        return try {
            ContextCompat.startForegroundService(applicationContext, intent)
            Result.success()
        } catch (_: IllegalStateException) {
            // Android may defer background FGS starts; retry with WorkManager backoff.
            Result.retry()
        } catch (_: SecurityException) {
            Result.failure()
        }
    }
}
