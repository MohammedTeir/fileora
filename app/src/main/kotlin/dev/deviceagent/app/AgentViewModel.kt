package dev.deviceagent.app

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.deviceagent.core.agent.AgentConfig
import dev.deviceagent.core.agent.AgentEngine
import dev.deviceagent.core.agent.PrivacyMode
import dev.deviceagent.core.agent.TaskOutcome
import dev.deviceagent.core.agent.TaskStatus
import dev.deviceagent.core.ai.ResilientProvider
import dev.deviceagent.core.ai.UsageTracker
import dev.deviceagent.core.ai.gemini.GeminiProvider
import dev.deviceagent.core.ai.openai.OpenAIProvider
import dev.deviceagent.core.ai.claude.ClaudeProvider
import dev.deviceagent.core.history.AuditEntry
import dev.deviceagent.core.history.UndoManager
import dev.deviceagent.core.policy.ConfirmationRequest
import dev.deviceagent.core.policy.ConfirmationResponse
import dev.deviceagent.core.policy.InteractionMode
import dev.deviceagent.core.policy.PolicyConfig
import dev.deviceagent.core.policy.PolicyEngine
import dev.deviceagent.core.policy.UserChannel
import dev.deviceagent.core.security.Redactor
import dev.deviceagent.core.security.maskSecret
import dev.deviceagent.core.tools.ToolRegistry
import dev.deviceagent.core.tools.system.DefaultTools
import dev.deviceagent.core.ai.ProviderException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

data class RootUi(val name: String, val uri: String)
enum class ProviderChoice { GEMINI, OPENAI, CLAUDE }

data class UiState(
    val provider: ProviderChoice = ProviderChoice.GEMINI,
    val apiKeyMask: String? = null,
    val model: String = "",
    val automaticModelRouting: Boolean = false,
    val recoveryModel: String = "",
    val availableModels: List<String> = emptyList(),
    val roots: List<RootUi> = emptyList(),
    val goal: String = "",
    val dryRun: Boolean = false,
    val manualMode: Boolean = false,
    val sendFileContents: Boolean = false,
    val running: Boolean = false,
    val checkpointAvailable: Boolean = false,
    val checkpointDescription: String = "",
    val log: List<String> = emptyList(),
    val outcome: TaskOutcome? = null,
    val pendingConfirmation: ConfirmationRequest? = null,
    val pendingQuestion: String? = null,
    val notice: String? = null,
    val usageSummary: String = "",
    val history: List<AuditEntry> = emptyList(),
    val restorableUndoTaskId: String? = null,
    val scheduledTasks: List<ScheduledTask> = emptyList(),
)

class AgentViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("agent", Context.MODE_PRIVATE)
    private val secrets = KeystoreSecretStore(app)
    private val redactor = Redactor()
    private val undoStore = RoomUndoStore(AgentDatabase.get(app).undoDao())
    private val undoManager = UndoManager(undoStore)
    private val audit = RoomAuditLog(AgentDatabase.get(app).auditDao(), redactor)
    private val usage = UsageTracker()

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var pendingConfirm: CompletableDeferred<ConfirmationResponse>? = null
    private var pendingAnswer: CompletableDeferred<String?>? = null
    private var lastTaskFs: SafFileSystem? = null
    private val taskReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AgentTaskService.ACTION_PROGRESS) {
                val line = intent.getStringExtra(AgentTaskService.EXTRA_PROGRESS).orEmpty()
                if (line.isNotBlank()) _state.update { it.copy(log = (it.log + redactor.redact(line)).takeLast(200)) }
                return
            }
            if (intent.action != AgentTaskService.ACTION_RESULT) return
            val status = intent.getStringExtra(AgentTaskService.EXTRA_STATUS).orEmpty()
            val report = intent.getStringExtra(AgentTaskService.EXTRA_REPORT).orEmpty()
            _state.update {
                it.copy(
                    running = false,
                    checkpointAvailable = false,
                    checkpointDescription = "",
                    outcome = TaskOutcome(
                        taskId = intent.getStringExtra(AgentTaskService.EXTRA_TASK_ID).orEmpty(),
                        status = runCatching { TaskStatus.valueOf(status) }.getOrDefault(TaskStatus.FAILED_AFTER_INVESTIGATION),
                        modelMessage = intent.getStringExtra(AgentTaskService.EXTRA_MODEL_MESSAGE),
                        report = report,
                        steps = intent.getIntExtra(AgentTaskService.EXTRA_STEPS, 0),
                        inputTokens = intent.getLongExtra(AgentTaskService.EXTRA_INPUT_TOKENS, 0L),
                        outputTokens = intent.getLongExtra(AgentTaskService.EXTRA_OUTPUT_TOKENS, 0L),
                        toolCalls = intent.getIntExtra(AgentTaskService.EXTRA_TOOL_CALLS, 0),
                        failures = intent.getIntExtra(AgentTaskService.EXTRA_FAILURES, 0),
                        attempts = emptyList(),
                        proposals = emptyList(),
                        canUndo = intent.getBooleanExtra(AgentTaskService.EXTRA_CAN_UNDO, false),
                        unverifiedChanges = intent.getIntExtra(AgentTaskService.EXTRA_UNVERIFIED, 0),
                        stoppedReason = intent.getStringExtra(AgentTaskService.EXTRA_STOPPED_REASON),
                        providerError = intent.getStringExtra(AgentTaskService.EXTRA_PROVIDER_ERROR),
                    ),
                    usageSummary = "${intent.getIntExtra(AgentTaskService.EXTRA_STEPS, 0)} AI request(s), " +
                        "${intent.getLongExtra(AgentTaskService.EXTRA_INPUT_TOKENS, 0L)} tokens in / " +
                        "${intent.getLongExtra(AgentTaskService.EXTRA_OUTPUT_TOKENS, 0L)} out",
                    notice = if (report.isBlank()) "Background task: $status" else "Background task $status: $report",
                )
            }
            refreshHistory()
        }
    }

    init {
        val choice = runCatching { ProviderChoice.valueOf(prefs.getString("provider", ProviderChoice.GEMINI.name) ?: ProviderChoice.GEMINI.name) }.getOrDefault(ProviderChoice.GEMINI)
        val key = secrets.get(secretKey(choice))
        key?.let { redactor.addSecret(it) }
        _state.update {
            it.copy(
                provider = choice,
                apiKeyMask = key?.let(::maskSecret),
                model = prefs.getString("model", "") ?: "",
                automaticModelRouting = prefs.getBoolean("automatic_model_routing", false),
                recoveryModel = prefs.getString(recoveryModelPreference(choice), "") ?: "",
                sendFileContents = prefs.getBoolean("send_file_contents", false),
                roots = loadRoots(),
                checkpointAvailable = AgentTaskService.hasCheckpoint(app),
                checkpointDescription = if (AgentTaskService.hasCheckpoint(app)) AgentTaskService.checkpointDescription(app) else "",
            )
        }
        refreshHistory()
        viewModelScope.launch(Dispatchers.IO) { _state.update { it.copy(scheduledTasks = ScheduledTasks.list(app)) } }
        val filter = IntentFilter().apply {
            addAction(AgentTaskService.ACTION_RESULT)
            addAction(AgentTaskService.ACTION_PROGRESS)
        }
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            app.registerReceiver(taskReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION") app.registerReceiver(taskReceiver, filter)
        }
    }

    private fun refreshHistory() = viewModelScope.launch(Dispatchers.IO) {
        val rows = audit.entries().takeLast(100).reversed()
        _state.update { it.copy(history = rows, restorableUndoTaskId = undoStore.latestTaskId()) }
    }

    // ───────── settings ─────────

    fun saveApiKey(key: String) {
        val k = key.trim()
        if (k.isEmpty()) return
        secrets.put(secretKey(_state.value.provider), k)
        redactor.addSecret(k)
        _state.update { it.copy(apiKeyMask = maskSecret(k), notice = "API key saved securely on this device.") }
    }

    fun removeApiKey() {
        secrets.remove(secretKey(_state.value.provider))
        _state.update { it.copy(apiKeyMask = null, availableModels = emptyList(), notice = "API key removed.") }
    }

    fun setProvider(provider: ProviderChoice) {
        prefs.edit().putString("provider", provider.name).apply()
        val key = secrets.get(secretKey(provider))
        val recoveryModel = prefs.getString(recoveryModelPreference(provider), "") ?: ""
        _state.update { it.copy(provider = provider, apiKeyMask = key?.let(::maskSecret), recoveryModel = recoveryModel, availableModels = emptyList()) }
    }

    fun setModel(model: String) {
        prefs.edit().putString("model", model.trim()).apply()
        _state.update { it.copy(model = model.trim()) }
    }

    fun setAutomaticModelRouting(on: Boolean) {
        prefs.edit().putBoolean("automatic_model_routing", on).apply()
        _state.update { it.copy(automaticModelRouting = on) }
    }

    fun setRecoveryModel(model: String) {
        prefs.edit().putString(recoveryModelPreference(_state.value.provider), model.trim()).apply()
        _state.update { it.copy(recoveryModel = model.trim()) }
    }

    fun refreshModels() = viewModelScope.launch(Dispatchers.IO) {
        try {
            val models = provider().listModels().map { it.id }
            _state.update { it.copy(availableModels = models, notice = "${models.size} models available for this key.") }
        } catch (e: ProviderException) {
            _state.update { it.copy(notice = "Could not list models: ${redactor.redact(e.message ?: e.kind.name)}") }
        }
    }

    fun testConnection() = viewModelScope.launch(Dispatchers.IO) {
        try {
            val ok = provider().validateCredentials()
            _state.update { it.copy(notice = if (ok) "Connection works." else "The provider rejected this API key.") }
        } catch (e: ProviderException) {
            _state.update { it.copy(notice = "Could not reach the provider: ${redactor.redact(e.message ?: e.kind.name)}") }
        }
    }

    // ───────── folders ─────────

    fun addRoot(uri: Uri) {
        val ctx = getApplication<Application>()
        ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        val base = DocumentFile.fromTreeUri(ctx, uri)?.name ?: "Folder"
        val taken = _state.value.roots.map { it.name }.toSet()
        var name = base; var n = 2
        while (name in taken || name.contains('/')) { name = "${base.replace('/', '_')}-$n"; n++ }
        val roots = _state.value.roots + RootUi(name, uri.toString())
        saveRoots(roots)
        _state.update { it.copy(roots = roots) }
    }

    fun removeRoot(name: String) {
        val roots = _state.value.roots.filterNot { it.name == name }
        saveRoots(roots)
        _state.update { it.copy(roots = roots) }
    }

    private fun saveRoots(roots: List<RootUi>) {
        val arr = JsonArray(roots.map { buildJsonObject { put("name", it.name); put("uri", it.uri) } })
        prefs.edit().putString("roots", arr.toString()).apply()
    }

    private fun loadRoots(): List<RootUi> = try {
        Json.parseToJsonElement(prefs.getString("roots", "[]") ?: "[]").jsonArray.map {
            RootUi(it.jsonObject["name"]!!.jsonPrimitive.content, it.jsonObject["uri"]!!.jsonPrimitive.content)
        }
    } catch (e: Exception) { emptyList() }

    // ───────── task input ─────────

    fun setGoal(goal: String) = _state.update { it.copy(goal = goal) }
    fun setDryRun(on: Boolean) = _state.update { it.copy(dryRun = on) }
    fun setManualMode(on: Boolean) = _state.update { it.copy(manualMode = on) }
    fun setSendFileContents(on: Boolean) {
        prefs.edit().putBoolean("send_file_contents", on).apply()
        _state.update { it.copy(sendFileContents = on) }
    }
    fun dismissNotice() = _state.update { it.copy(notice = null) }

    fun scheduleCurrent(intervalHours: Long) {
        val s = _state.value
        if (s.goal.isBlank() || s.model.isBlank() || s.roots.isEmpty()) {
            _state.update { it.copy(notice = "Set a goal, model, and approved folder before scheduling.") }
            return
        }
        if (s.automaticModelRouting && (s.recoveryModel.isBlank() || s.recoveryModel == s.model)) {
            _state.update { it.copy(notice = "Choose a recovery model different from the planning/default model before scheduling.") }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val task = ScheduledTask("task-${System.currentTimeMillis()}", s.goal, s.model, s.roots.map { it.name }, s.roots.map { it.uri }, intervalHours, s.sendFileContents, s.dryRun, manual = true, provider = s.provider.name, automaticModelRouting = s.automaticModelRouting, recoveryModel = s.recoveryModel.ifBlank { null })
                ScheduledTasks.schedule(getApplication(), task)
                _state.update { it.copy(scheduledTasks = ScheduledTasks.list(getApplication()), notice = "Scheduled every $intervalHours hour(s) in manual mode.") }
            }.onFailure { e -> _state.update { it.copy(notice = e.message ?: "Could not schedule task.") } }
        }
    }

    fun cancelScheduledTask(id: String) = viewModelScope.launch(Dispatchers.IO) {
        ScheduledTasks.cancel(getApplication(), id)
        _state.update { it.copy(scheduledTasks = ScheduledTasks.list(getApplication()), notice = "Scheduled task cancelled.") }
    }

    // ───────── running a task ─────────

    private fun provider(): ResilientProvider {
        val choice = _state.value.provider
        val key = { secrets.get(secretKey(choice)) ?: "" }
        val selected = when (choice) {
            ProviderChoice.GEMINI -> GeminiProvider(apiKey = key)
            ProviderChoice.OPENAI -> OpenAIProvider(apiKey = key)
            ProviderChoice.CLAUDE -> ClaudeProvider(apiKey = key)
        }
        return ResilientProvider(selected, tracker = usage)
    }

    private val channel = object : UserChannel {
        override suspend fun confirm(request: ConfirmationRequest): ConfirmationResponse {
            val d = CompletableDeferred<ConfirmationResponse>()
            pendingConfirm = d
            _state.update { it.copy(pendingConfirmation = request) }
            return d.await()
        }

        override suspend fun ask(question: String): String? {
            val d = CompletableDeferred<String?>()
            pendingAnswer = d
            _state.update { it.copy(pendingQuestion = question) }
            return d.await()
        }
    }

    fun answerConfirmation(response: ConfirmationResponse) {
        pendingConfirm?.complete(response); pendingConfirm = null
        _state.update { it.copy(pendingConfirmation = null) }
    }

    fun answerQuestion(answer: String?) {
        pendingAnswer?.complete(answer?.takeIf { it.isNotBlank() }); pendingAnswer = null
        _state.update { it.copy(pendingQuestion = null) }
    }

    fun runTask() {
        val s = _state.value
        when {
            s.running -> return
            s.apiKeyMask == null -> { _state.update { it.copy(notice = "Add your ${s.provider.name.lowercase()} API key first.") }; return }
            s.model.isBlank() -> { _state.update { it.copy(notice = "Choose a model first (tap List models).") }; return }
            s.automaticModelRouting && (s.recoveryModel.isBlank() || s.recoveryModel == s.model) -> { _state.update { it.copy(notice = "Choose a recovery model different from the planning/default model.") }; return }
            s.roots.isEmpty() -> { _state.update { it.copy(notice = "Add at least one folder the agent may use.") }; return }
            s.goal.isBlank() -> return
        }
        val intent = Intent(getApplication(), AgentTaskService::class.java).apply {
            action = AgentTaskService.ACTION_START
            putExtra(AgentTaskService.EXTRA_GOAL, s.goal)
            putExtra(AgentTaskService.EXTRA_MODEL, s.model)
            putExtra(AgentTaskService.EXTRA_PROVIDER, s.provider.name)
            putExtra(AgentTaskService.EXTRA_AUTOMATIC_MODEL_ROUTING, s.automaticModelRouting)
            putExtra(AgentTaskService.EXTRA_RECOVERY_MODEL, s.recoveryModel)
            putExtra(AgentTaskService.EXTRA_DRY_RUN, s.dryRun)
            putExtra(AgentTaskService.EXTRA_MANUAL, s.manualMode)
            putExtra(AgentTaskService.EXTRA_SEND_CONTENTS, s.sendFileContents)
            putStringArrayListExtra(AgentTaskService.EXTRA_ROOT_NAMES, ArrayList(s.roots.map { it.name }))
            putStringArrayListExtra(AgentTaskService.EXTRA_ROOT_URIS, ArrayList(s.roots.map { it.uri }))
        }
        _state.update { it.copy(running = true, outcome = null, log = emptyList()) }
        if (android.os.Build.VERSION.SDK_INT >= 26) getApplication<Application>().startForegroundService(intent)
        else getApplication<Application>().startService(intent)
    }

    fun cancelTask() {
        getApplication<Application>().startService(Intent(getApplication(), AgentTaskService::class.java).setAction(AgentTaskService.ACTION_CANCEL))
    }

    fun resumeTask() {
        if (_state.value.running) return
        val intent = AgentTaskService.resumeIntent(getApplication()) ?: run {
            _state.update { it.copy(checkpointAvailable = false) }
            return
        }
        _state.update { it.copy(running = true, checkpointAvailable = false, outcome = null, log = emptyList(), notice = "Resuming the interrupted task…") }
        if (android.os.Build.VERSION.SDK_INT >= 26) getApplication<Application>().startForegroundService(intent)
        else getApplication<Application>().startService(intent)
    }

    fun discardCheckpoint() {
        getApplication<Application>().startService(Intent(getApplication(), AgentTaskService::class.java).setAction(AgentTaskService.ACTION_DISCARD_CHECKPOINT))
        _state.update { it.copy(checkpointAvailable = false, checkpointDescription = "") }
    }

    override fun onCleared() {
        runCatching { getApplication<Application>().unregisterReceiver(taskReceiver) }
        super.onCleared()
    }

    fun undoLastTask() {
        val current = _state.value
        val taskId = current.outcome?.taskId ?: current.restorableUndoTaskId ?: return
        val fs = lastTaskFs ?: SafFileSystem(getApplication(), current.roots.map { SafRoot(it.name, Uri.parse(it.uri)) })
        viewModelScope.launch(Dispatchers.IO) {
            val report = undoManager.undo(taskId, fs)
            val extra = if (report.notes.isEmpty()) "" else " (" + report.notes.joinToString("; ") + ")"
            _state.update { it.copy(notice = "Undo: reverted ${report.reverted} change(s)$extra", outcome = it.outcome?.copy(canUndo = false), restorableUndoTaskId = null) }
        }
    }

    private companion object {
        const val KEY_NAME = "gemini_api_key"
        fun recoveryModelPreference(provider: ProviderChoice) = "recovery_model_${provider.name.lowercase()}"
        fun secretKey(provider: ProviderChoice) = when (provider) {
            ProviderChoice.GEMINI -> KEY_NAME
            ProviderChoice.OPENAI -> "openai_api_key"
            ProviderChoice.CLAUDE -> "claude_api_key"
        }
    }
}
