package dev.deviceagent.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import dev.deviceagent.core.agent.AgentConfig
import dev.deviceagent.core.agent.AgentEngine
import dev.deviceagent.core.agent.PrivacyMode
import dev.deviceagent.core.ai.ResilientProvider
import dev.deviceagent.core.ai.UsageTracker
import dev.deviceagent.core.ai.gemini.GeminiProvider
import dev.deviceagent.core.ai.openai.OpenAIProvider
import dev.deviceagent.core.ai.claude.ClaudeProvider
import dev.deviceagent.core.history.UndoManager
import dev.deviceagent.core.policy.ConfirmationRequest
import dev.deviceagent.core.policy.ConfirmationResponse
import dev.deviceagent.core.policy.InteractionMode
import dev.deviceagent.core.policy.PolicyConfig
import dev.deviceagent.core.policy.PolicyEngine
import dev.deviceagent.core.policy.UserChannel
import dev.deviceagent.core.security.Redactor
import dev.deviceagent.core.tools.ToolRegistry
import dev.deviceagent.core.tools.system.DefaultTools
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/** Owns an agent run independently of the activity so it continues with the screen off. */
class AgentTaskService : Service() {
    private val serviceJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + serviceJob)
    private val usage = UsageTracker()
    private var taskJob: Job? = null
    private var pendingConfirmation: CompletableDeferred<ConfirmationResponse>? = null
    private var currentTaskId: String? = null
    private val stopping = AtomicBoolean(false)

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                if (taskJob?.isActive != true) {
                    saveCheckpoint(intent)
                    scheduleRecovery()
                    startForeground(NOTIFICATION_ID, notification("Starting agent task…", ongoing = true))
                    taskJob = scope.launch { runTask(intent) }
                }
            }
            ACTION_CANCEL -> cancelTask()
            ACTION_DISCARD_CHECKPOINT -> {
                clearCheckpoint()
                stopSelf()
            }
            ACTION_ALLOW_ONCE -> pendingConfirmation?.complete(ConfirmationResponse.ALLOW_ONCE)
            ACTION_DENY -> pendingConfirmation?.complete(ConfirmationResponse.DENY)
            ACTION_CANCEL_CONFIRMATION -> pendingConfirmation?.complete(ConfirmationResponse.CANCEL_TASK)
        }
        return START_NOT_STICKY
    }

    private suspend fun runTask(intent: Intent) {
        val taskId = "service-task-${System.currentTimeMillis()}"
        currentTaskId = taskId
        val redactor = Redactor()
        val providerChoice = intent.getStringExtra(EXTRA_PROVIDER).orEmpty().ifBlank { ProviderChoice.GEMINI.name }
        val keyName = when (runCatching { ProviderChoice.valueOf(providerChoice) }.getOrDefault(ProviderChoice.GEMINI)) {
            ProviderChoice.GEMINI -> KEY_NAME
            ProviderChoice.OPENAI -> OPENAI_KEY_NAME
            ProviderChoice.CLAUDE -> CLAUDE_KEY_NAME
        }
        val apiKey = KeystoreSecretStore(this).get(keyName).orEmpty()
        if (apiKey.isNotBlank()) redactor.addSecret(apiKey)
        val roots = intent.getStringArrayListExtra(EXTRA_ROOT_NAMES).orEmpty()
            .zip(intent.getStringArrayListExtra(EXTRA_ROOT_URIS).orEmpty())
            .map { SafRoot(it.first, android.net.Uri.parse(it.second)) }
        val fs = SafFileSystem(this, roots)
        val undoManager = UndoManager(RoomUndoStore(AgentDatabase.get(this).undoDao()))
        val audit = RoomAuditLog(AgentDatabase.get(this).auditDao(), redactor)
        val channel = object : UserChannel {
            override suspend fun confirm(request: ConfirmationRequest): ConfirmationResponse {
                pendingConfirmation = CompletableDeferred()
                updateNotification("Confirmation needed: ${request.summary}", confirmation = true)
                val answer = pendingConfirmation!!.await()
                pendingConfirmation = null
                return answer
            }

            override suspend fun ask(question: String): String? {
                updateNotification("Question needs an answer in the app: $question", confirmation = false)
                return null
            }
        }
        val provider = when (runCatching { ProviderChoice.valueOf(providerChoice) }.getOrDefault(ProviderChoice.GEMINI)) {
            ProviderChoice.GEMINI -> GeminiProvider(apiKey = { apiKey })
            ProviderChoice.OPENAI -> OpenAIProvider(apiKey = { apiKey })
            ProviderChoice.CLAUDE -> ClaudeProvider(apiKey = { apiKey })
        }
        val trackerProvider = ResilientProvider(provider, tracker = usage)
        val engine = AgentEngine(
            provider = trackerProvider,
            registry = ToolRegistry(DefaultTools.all()),
            fs = fs,
            policy = PolicyEngine(PolicyConfig(mode = if (intent.getBooleanExtra(EXTRA_MANUAL, false)) InteractionMode.MANUAL else InteractionMode.NORMAL)),
            user = channel,
            undoManager = undoManager,
            redactor = redactor,
            audit = audit,
            log = { updateNotification(redactor.redact(it), confirmation = false) },
        )
        try {
            val outcome = engine.run(
                intent.getStringExtra(EXTRA_GOAL).orEmpty(),
                AgentConfig(
                    model = intent.getStringExtra(EXTRA_MODEL).orEmpty(),
                    privacyMode = if (intent.getBooleanExtra(EXTRA_SEND_CONTENTS, false)) PrivacyMode.CONTENTS_ALLOWED else PrivacyMode.NAMES_ONLY,
                    dryRun = intent.getBooleanExtra(EXTRA_DRY_RUN, false),
                    automaticModelRouting = intent.getBooleanExtra(EXTRA_AUTOMATIC_MODEL_ROUTING, false),
                    recoveryModel = intent.getStringExtra(EXTRA_RECOVERY_MODEL),
                    initialMessages = ConversationCheckpointCodec.decode(intent.getStringExtra(EXTRA_MESSAGES)),
                    onCheckpoint = { messages -> saveConversationCheckpoint(ConversationCheckpointCodec.encode(messages)) },
                ),
            )
            sendBroadcast(Intent(ACTION_RESULT).setPackage(packageName).apply {
                putExtra(EXTRA_STATUS, outcome.status.name)
                putExtra(EXTRA_REPORT, outcome.report)
                putExtra(EXTRA_TASK_ID, outcome.taskId)
                putExtra(EXTRA_MODEL_MESSAGE, outcome.modelMessage)
                putExtra(EXTRA_STEPS, outcome.steps)
                putExtra(EXTRA_INPUT_TOKENS, outcome.inputTokens)
                putExtra(EXTRA_OUTPUT_TOKENS, outcome.outputTokens)
                putExtra(EXTRA_TOOL_CALLS, outcome.toolCalls)
                putExtra(EXTRA_FAILURES, outcome.failures)
                putExtra(EXTRA_CAN_UNDO, outcome.canUndo)
                putExtra(EXTRA_UNVERIFIED, outcome.unverifiedChanges)
                putExtra(EXTRA_STOPPED_REASON, outcome.stoppedReason)
                putExtra(EXTRA_PROVIDER_ERROR, outcome.providerError)
            })
            updateNotification("Task finished: ${outcome.status.name}", confirmation = false, ongoing = false)
        } catch (_: CancellationException) {
            sendBroadcast(Intent(ACTION_RESULT).setPackage(packageName).putExtra(EXTRA_STATUS, "CANCELLED"))
            updateNotification("Task cancelled", confirmation = false, ongoing = false)
        } catch (e: Exception) {
            sendBroadcast(Intent(ACTION_RESULT).setPackage(packageName).apply {
                putExtra(EXTRA_STATUS, "FAILED_AFTER_INVESTIGATION")
                putExtra(EXTRA_REPORT, redactor.redact(e.message ?: e.javaClass.simpleName))
            })
            updateNotification("Task stopped: ${redactor.redact(e.message ?: e.javaClass.simpleName)}", confirmation = false, ongoing = false)
        } finally {
            cancelRecovery()
            clearCheckpoint()
            currentTaskId = null
            stopSelf()
        }
    }

    private fun cancelTask() {
        stopping.set(true)
        pendingConfirmation?.complete(ConfirmationResponse.CANCEL_TASK)
        taskJob?.cancel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Agent tasks", NotificationManager.IMPORTANCE_LOW),
            )
        }
    }

    private fun notification(text: String, ongoing: Boolean, confirmation: Boolean = false): Notification {
        val cancel = PendingIntent.getService(this, 2, Intent(this, AgentTaskService::class.java).setAction(ACTION_CANCEL), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("Device Agent")
            .setContentText(text.take(180))
            .setOngoing(ongoing)
            .setOnlyAlertOnce(true)
            .addAction(NotificationCompat.Action.Builder(0, "Cancel", cancel).build())
        if (confirmation) {
            builder.addAction(NotificationCompat.Action.Builder(0, "Allow", serviceAction(ACTION_ALLOW_ONCE)).build())
            builder.addAction(NotificationCompat.Action.Builder(0, "Deny", serviceAction(ACTION_DENY)).build())
        }
        return builder.build()
    }

    private fun serviceAction(action: String) = PendingIntent.getService(
        this, action.hashCode(), Intent(this, AgentTaskService::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun updateNotification(text: String, confirmation: Boolean, ongoing: Boolean = true) {
        getSharedPreferences(CHECKPOINT_PREFS, MODE_PRIVATE).edit()
            .putString(KEY_PROGRESS, text.take(500))
            .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
            .apply()
        sendBroadcast(Intent(ACTION_PROGRESS).setPackage(packageName).putExtra(EXTRA_PROGRESS, text.take(500)))
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(text, ongoing, confirmation))
    }

    private fun saveCheckpoint(intent: Intent) {
        val prefs = getSharedPreferences(CHECKPOINT_PREFS, MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_ACTIVE, true)
            .putString(KEY_GOAL, intent.getStringExtra(EXTRA_GOAL).orEmpty())
            .putString(KEY_MODEL, intent.getStringExtra(EXTRA_MODEL).orEmpty())
            .putString(KEY_PROVIDER, intent.getStringExtra(EXTRA_PROVIDER).orEmpty())
            .putBoolean(KEY_AUTOMATIC_MODEL_ROUTING, intent.getBooleanExtra(EXTRA_AUTOMATIC_MODEL_ROUTING, false))
            .putString(KEY_RECOVERY_MODEL, intent.getStringExtra(EXTRA_RECOVERY_MODEL).orEmpty())
            .putBoolean(KEY_DRY_RUN, intent.getBooleanExtra(EXTRA_DRY_RUN, false))
            .putBoolean(KEY_MANUAL, intent.getBooleanExtra(EXTRA_MANUAL, false))
            .putBoolean(KEY_SEND_CONTENTS, intent.getBooleanExtra(EXTRA_SEND_CONTENTS, false))
            .putString(KEY_ROOT_NAMES, intent.getStringArrayListExtra(EXTRA_ROOT_NAMES).orEmpty().joinToString(ROOT_SEPARATOR))
            .putString(KEY_ROOT_URIS, intent.getStringArrayListExtra(EXTRA_ROOT_URIS).orEmpty().joinToString(ROOT_SEPARATOR))
            .putString(KEY_MESSAGES, intent.getStringExtra(EXTRA_MESSAGES).orEmpty())
            .putString(KEY_PROGRESS, "Task started")
            .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
            .apply()
    }

    private fun clearCheckpoint() = getSharedPreferences(CHECKPOINT_PREFS, MODE_PRIVATE).edit().clear().apply()

    private fun saveConversationCheckpoint(encoded: String) {
        getSharedPreferences(CHECKPOINT_PREFS, MODE_PRIVATE).edit().putString(KEY_MESSAGES, encoded).apply()
    }

    override fun onDestroy() {
        isRunning = false
        taskJob?.cancel()
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        @Volatile var isRunning: Boolean = false
            private set
        const val ACTION_START = "dev.deviceagent.action.START_TASK"
        const val ACTION_CANCEL = "dev.deviceagent.action.CANCEL_TASK"
        const val ACTION_ALLOW_ONCE = "dev.deviceagent.action.ALLOW_ONCE"
        const val ACTION_DENY = "dev.deviceagent.action.DENY"
        const val ACTION_CANCEL_CONFIRMATION = "dev.deviceagent.action.CANCEL_CONFIRMATION"
        const val ACTION_DISCARD_CHECKPOINT = "dev.deviceagent.action.DISCARD_CHECKPOINT"
        const val ACTION_RESULT = "dev.deviceagent.action.TASK_RESULT"
        const val ACTION_PROGRESS = "dev.deviceagent.action.TASK_PROGRESS"
        const val EXTRA_GOAL = "goal"
        const val EXTRA_MODEL = "model"
        const val EXTRA_PROVIDER = "provider"
        const val EXTRA_AUTOMATIC_MODEL_ROUTING = "automatic_model_routing"
        const val EXTRA_RECOVERY_MODEL = "recovery_model"
        const val EXTRA_DRY_RUN = "dry_run"
        const val EXTRA_MANUAL = "manual"
        const val EXTRA_SEND_CONTENTS = "send_contents"
        const val EXTRA_MESSAGES = "messages"
        const val EXTRA_ROOT_NAMES = "root_names"
        const val EXTRA_ROOT_URIS = "root_uris"
        const val EXTRA_STATUS = "status"
        const val EXTRA_REPORT = "report"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_STEPS = "steps"
        const val EXTRA_INPUT_TOKENS = "input_tokens"
        const val EXTRA_OUTPUT_TOKENS = "output_tokens"
        const val EXTRA_MODEL_MESSAGE = "model_message"
        const val EXTRA_TOOL_CALLS = "tool_calls"
        const val EXTRA_FAILURES = "failures"
        const val EXTRA_CAN_UNDO = "can_undo"
        const val EXTRA_UNVERIFIED = "unverified"
        const val EXTRA_STOPPED_REASON = "stopped_reason"
        const val EXTRA_PROVIDER_ERROR = "provider_error"
        const val EXTRA_PROGRESS = "progress"
        private const val CHANNEL_ID = "agent_tasks"
        private const val NOTIFICATION_ID = 7001
        private const val KEY_NAME = "gemini_api_key"
        private const val OPENAI_KEY_NAME = "openai_api_key"
        private const val CLAUDE_KEY_NAME = "claude_api_key"
        private const val CHECKPOINT_PREFS = "agent_checkpoint"
        private const val KEY_ACTIVE = "active"
        private const val KEY_GOAL = "goal"
        private const val KEY_MODEL = "model"
        private const val KEY_PROVIDER = "provider"
        private const val KEY_AUTOMATIC_MODEL_ROUTING = "automatic_model_routing"
        private const val KEY_RECOVERY_MODEL = "recovery_model"
        private const val KEY_DRY_RUN = "dry_run"
        private const val KEY_MANUAL = "manual"
        private const val KEY_SEND_CONTENTS = "send_contents"
        private const val KEY_ROOT_NAMES = "root_names"
        private const val KEY_ROOT_URIS = "root_uris"
        private const val KEY_MESSAGES = "messages"
        private const val KEY_PROGRESS = "progress"
        private const val KEY_UPDATED_AT = "updated_at"
        private const val ROOT_SEPARATOR = "\u001f"
        private const val RECOVERY_WORK = "agent-task-recovery"

        fun createStartIntent(
            context: android.content.Context,
            goal: String,
            model: String,
            provider: String = ProviderChoice.GEMINI.name,
            automaticModelRouting: Boolean = false,
            recoveryModel: String? = null,
            dryRun: Boolean,
            manual: Boolean,
            sendContents: Boolean,
            rootNames: List<String>,
            rootUris: List<String>,
        ): Intent = Intent(context, AgentTaskService::class.java).apply {
            action = ACTION_START
            putExtra(EXTRA_GOAL, goal)
            putExtra(EXTRA_MODEL, model)
            putExtra(EXTRA_PROVIDER, provider)
            putExtra(EXTRA_AUTOMATIC_MODEL_ROUTING, automaticModelRouting)
            putExtra(EXTRA_RECOVERY_MODEL, recoveryModel)
            putExtra(EXTRA_DRY_RUN, dryRun)
            putExtra(EXTRA_MANUAL, manual)
            putExtra(EXTRA_SEND_CONTENTS, sendContents)
            putStringArrayListExtra(EXTRA_ROOT_NAMES, ArrayList(rootNames))
            putStringArrayListExtra(EXTRA_ROOT_URIS, ArrayList(rootUris))
        }

        fun hasCheckpoint(context: android.content.Context): Boolean =
            context.getSharedPreferences(CHECKPOINT_PREFS, MODE_PRIVATE).getBoolean(KEY_ACTIVE, false)

        fun checkpointDescription(context: android.content.Context): String =
            context.getSharedPreferences(CHECKPOINT_PREFS, MODE_PRIVATE).getString(KEY_PROGRESS, "An unfinished task was found.")
                ?: "An unfinished task was found."

        fun resumeIntent(context: android.content.Context): Intent? {
            val p = context.getSharedPreferences(CHECKPOINT_PREFS, MODE_PRIVATE)
            if (!p.getBoolean(KEY_ACTIVE, false)) return null
            return Intent(context, AgentTaskService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_GOAL, p.getString(KEY_GOAL, ""))
                putExtra(EXTRA_MODEL, p.getString(KEY_MODEL, ""))
                putExtra(EXTRA_PROVIDER, p.getString(KEY_PROVIDER, ProviderChoice.GEMINI.name))
                putExtra(EXTRA_AUTOMATIC_MODEL_ROUTING, p.getBoolean(KEY_AUTOMATIC_MODEL_ROUTING, false))
                putExtra(EXTRA_RECOVERY_MODEL, p.getString(KEY_RECOVERY_MODEL, ""))
                putExtra(EXTRA_DRY_RUN, p.getBoolean(KEY_DRY_RUN, false))
                putExtra(EXTRA_MANUAL, p.getBoolean(KEY_MANUAL, false))
                putExtra(EXTRA_SEND_CONTENTS, p.getBoolean(KEY_SEND_CONTENTS, false))
                putExtra(EXTRA_MESSAGES, p.getString(KEY_MESSAGES, ""))
                putStringArrayListExtra(EXTRA_ROOT_NAMES, ArrayList(p.getString(KEY_ROOT_NAMES, "").orEmpty().split(ROOT_SEPARATOR).filter(String::isNotEmpty)))
                putStringArrayListExtra(EXTRA_ROOT_URIS, ArrayList(p.getString(KEY_ROOT_URIS, "").orEmpty().split(ROOT_SEPARATOR).filter(String::isNotEmpty)))
            }
        }
    }

    private fun scheduleRecovery() {
        val request = OneTimeWorkRequestBuilder<AgentTaskRecoveryWorker>()
            .setInitialDelay(20, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(this).enqueueUniqueWork(RECOVERY_WORK, ExistingWorkPolicy.REPLACE, request)
    }

    private fun cancelRecovery() {
        WorkManager.getInstance(this).cancelUniqueWork(RECOVERY_WORK)
    }
}
