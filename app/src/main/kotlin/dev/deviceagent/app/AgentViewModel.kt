package dev.deviceagent.app

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
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
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

data class RootUi(val name: String, val uri: String)
enum class ProviderChoice { GEMINI, OPENAI, CLAUDE }

/** One finished (or interrupted) task, already translated into plain language for the Activity tab. */
data class HistoryItem(
    val taskId: String,
    val startedAtMs: Long,
    val goal: String,
    val status: TaskStatus?,
    val steps: List<String>,
    val canUndo: Boolean,
    val report: String = "",
    val modelMessage: String? = null,
    val stoppedReason: String? = null,
    val providerError: String? = null,
    val stepsCount: Int = 0,
    val inputTokens: Long = 0,
    val outputTokens: Long = 0,
    val toolCalls: Int = 0,
    val failures: Int = 0,
    val unverifiedChanges: Int = 0,
)

data class UiState(
    val provider: ProviderChoice = ProviderChoice.GEMINI,
    val apiKeyMask: String? = null,
    val model: String = "",
    val automaticModelRouting: Boolean = false,
    val recoveryModel: String = "",
    val availableModels: List<String> = emptyList(),
    val roots: List<RootUi> = emptyList(),
    val allFilesAccessSupported: Boolean = false,
    val allFilesAccessGranted: Boolean = false,
    val useAllFilesAccess: Boolean = false,
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
    val history: List<HistoryItem> = emptyList(),
    val mostRecentTask: HistoryItem? = null,
    val dismissedRecentTaskId: String? = null,
    val restorableUndoTaskId: String? = null,
    val scheduledTasks: List<ScheduledTask> = emptyList(),
    val notificationsEnabled: Boolean = true,
    val busy: Boolean = false,
    /** False only on a brand-new install, until the person finishes (or skips) the first-run setup. */
    val onboardingDone: Boolean = true,
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
            val resultTaskId = intent.getStringExtra(AgentTaskService.EXTRA_TASK_ID).orEmpty()
            if (resultTaskId.isNotBlank() && intent.getBooleanExtra(AgentTaskService.EXTRA_CAN_UNDO, false) && intent.hasExtra(AgentTaskService.EXTRA_USE_ALL_FILES_ACCESS)) {
                prefs.edit().putBoolean(undoFsPreference(resultTaskId), intent.getBooleanExtra(AgentTaskService.EXTRA_USE_ALL_FILES_ACCESS, false)).apply()
            }
            val checkpointRetained = intent.getBooleanExtra(AgentTaskService.EXTRA_CHECKPOINT_RETAINED, false)
            _state.update {
                it.copy(
                    running = false,
                    checkpointAvailable = checkpointRetained,
                    checkpointDescription = if (checkpointRetained) AgentTaskService.checkpointDescription(context) else "",
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
                )
            }
            refreshHistory()
        }
    }

    init {
        // Models used to be one global setting, which broke as soon as you switched provider. Make it per-provider.
        prefs.getString("model", null)?.takeIf { it.isNotBlank() }?.let { legacy ->
            val current = prefs.getString("provider", ProviderChoice.GEMINI.name) ?: ProviderChoice.GEMINI.name
            prefs.edit().putString("model_${current.lowercase()}", legacy).remove("model").apply()
        }
        val choice = runCatching { ProviderChoice.valueOf(prefs.getString("provider", ProviderChoice.GEMINI.name) ?: ProviderChoice.GEMINI.name) }.getOrDefault(ProviderChoice.GEMINI)
        val key = secrets.get(secretKey(choice))
        key?.let { redactor.addSecret(it) }
        val loadedRoots = loadRoots()
        val usesAllFiles = prefs.getBoolean(KEY_USE_ALL_FILES_ACCESS, false)
        _state.update {
            it.copy(
                provider = choice,
                apiKeyMask = key?.let(::maskSecret),
                model = modelFor(choice),
                automaticModelRouting = prefs.getBoolean("automatic_model_routing", false),
                recoveryModel = prefs.getString(recoveryModelPreference(choice), "") ?: "",
                sendFileContents = prefs.getBoolean("send_file_contents", false),
                running = AgentTaskService.isRunning,
                notificationsEnabled = NotificationManagerCompat.from(app).areNotificationsEnabled(),
                roots = loadedRoots,
                allFilesAccessSupported = AllFilesAccess.isSupported(),
                allFilesAccessGranted = AllFilesAccess.hasAccess(),
                useAllFilesAccess = usesAllFiles,
                checkpointAvailable = AgentTaskService.hasCheckpoint(app),
                checkpointDescription = if (AgentTaskService.hasCheckpoint(app)) AgentTaskService.checkpointDescription(app) else "",
                // Someone who already set the app up before onboarding existed must not be sent back through it.
                onboardingDone = prefs.getBoolean(KEY_ONBOARDING_DONE, false) || loadedRoots.isNotEmpty() || key != null || usesAllFiles,
            )
        }
        refreshHistory()
        viewModelScope.launch(Dispatchers.IO) { _state.update { it.copy(scheduledTasks = ScheduledTasks.list(app)) } }
        // Approval requests and questions come from the background service through TaskBridge.
        viewModelScope.launch { TaskBridge.confirmation.collect { c -> _state.update { it.copy(pendingConfirmation = c) } } }
        viewModelScope.launch { TaskBridge.question.collect { q -> _state.update { it.copy(pendingQuestion = q) } } }
        val filter = IntentFilter().apply {
            addAction(AgentTaskService.ACTION_RESULT)
            addAction(AgentTaskService.ACTION_PROGRESS)
        }
        ContextCompat.registerReceiver(app, taskReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    private fun refreshHistory() = viewModelScope.launch(Dispatchers.IO) {
        val undoId = undoStore.latestTaskId()
        val undoIds = undoStore.taskIdsWithUndo()
        val rows = audit.entries().takeLast(800)
        val historyItems = buildHistory(rows, undoIds)
        val mostRecentTask = historyItems.firstOrNull { it.status != null }
        _state.update {
            it.copy(
                history = historyItems,
                mostRecentTask = mostRecentTask,
                restorableUndoTaskId = undoId,
            )
        }
    }

    private fun buildHistory(entries: List<AuditEntry>, undoTaskIds: Set<String>): List<HistoryItem> =
        entries.groupBy { it.taskId }.map { (id, rows) ->
            val start = rows.firstOrNull { it.kind == "task_start" }
            val end = rows.lastOrNull { it.kind == "task_end" }
            val details = end?.details
            fun text(key: String): String? = details?.get(key)?.jsonPrimitive?.contentOrNull
            fun long(key: String): Long = text(key)?.toLongOrNull() ?: 0L
            HistoryItem(
                taskId = id,
                startedAtMs = rows.first().timestampMs,
                goal = start?.details?.get("goal")?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: "Earlier task",
                status = end?.details?.get("status")?.jsonPrimitive?.contentOrNull?.let { runCatching { TaskStatus.valueOf(it) }.getOrNull() },
                steps = rows.mapNotNull(::describeAudit),
                canUndo = id in undoTaskIds,
                report = text("report").orEmpty(),
                modelMessage = text("model_message"),
                stoppedReason = text("stopped_reason"),
                providerError = text("provider_error"),
                stepsCount = long("steps").toInt(),
                inputTokens = long("input_tokens"),
                outputTokens = long("output_tokens"),
                toolCalls = long("tool_calls").toInt(),
                failures = long("failures").toInt(),
                unverifiedChanges = long("unverified_changes").toInt(),
            )
        }.sortedByDescending { it.startedAtMs }.take(30)

    // ───────── settings ─────────

    fun saveApiKey(key: String) {
        val k = key.trim()
        if (k.isEmpty()) return
        secrets.put(secretKey(_state.value.provider), k)
        redactor.addSecret(k)
        _state.update { it.copy(apiKeyMask = maskSecret(k), notice = "Key saved on this phone. Checking which models you can use…") }
        refreshModels()
    }

    fun removeApiKey() {
        secrets.remove(secretKey(_state.value.provider))
        _state.update { it.copy(apiKeyMask = null, availableModels = emptyList(), notice = "Key removed.") }
    }

    fun setProvider(provider: ProviderChoice) {
        prefs.edit().putString("provider", provider.name).apply()
        val key = secrets.get(secretKey(provider))
        val recoveryModel = prefs.getString(recoveryModelPreference(provider), "") ?: ""
        _state.update { it.copy(provider = provider, apiKeyMask = key?.let(::maskSecret), model = modelFor(provider), recoveryModel = recoveryModel, availableModels = emptyList()) }
        if (key != null) refreshModels(provider)
    }

    fun setModel(model: String) {
        prefs.edit().putString(modelPreference(_state.value.provider), model.trim()).apply()
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

    fun refreshModels() = refreshModels(_state.value.provider)

    private fun refreshModels(choice: ProviderChoice) = viewModelScope.launch(Dispatchers.IO) {
        _state.update { if (it.provider == choice) it.copy(busy = true) else it }
        try {
            val infos = provider(choice).listModels()
            val ids = infos.filter { it.capabilities.toolCalling }.ifEmpty { infos }.map { it.id }
            val savedModel = modelFor(choice)
            val suggestion = if (savedModel.isBlank()) suggestModel(choice, ids) else null
            if (suggestion != null) prefs.edit().putString(modelPreference(choice), suggestion).apply()
            _state.update {
                if (it.provider != choice) it else it.copy(
                    availableModels = ids,
                    model = suggestion ?: it.model,
                    notice = when {
                        ids.isEmpty() -> "Connected, but no models were listed for this key."
                        suggestion != null -> "Connected. I picked $suggestion for you. You can change it in Settings."
                        else -> "Connected. ${ids.size} models available."
                    },
                )
            }
        } catch (e: ProviderException) {
            _state.update { if (it.provider == choice) it.copy(notice = providerProblem(e.kind.name)) else it }
        } finally {
            _state.update { if (it.provider == choice) it.copy(busy = false) else it }
        }
    }

    fun testConnection() = viewModelScope.launch(Dispatchers.IO) {
        _state.update { it.copy(busy = true) }
        try {
            val ok = provider().validateCredentials()
            _state.update { it.copy(notice = if (ok) "Connection works." else providerProblem("INVALID_CREDENTIALS")) }
        } catch (e: ProviderException) {
            _state.update { it.copy(notice = providerProblem(e.kind.name)) }
        } finally {
            _state.update { it.copy(busy = false) }
        }
    }

    // ───────── folders ─────────

    fun addRoot(uri: Uri) {
        val ctx = getApplication<Application>()
        val kept = runCatching {
            ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        if (kept.isFailure) {
            _state.update { it.copy(notice = "Android wouldn't let me keep access to that folder. Try a different one.") }
            return
        }
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

    fun refreshAllFilesAccessStatus() {
        _state.update { it.copy(allFilesAccessSupported = AllFilesAccess.isSupported(), allFilesAccessGranted = AllFilesAccess.hasAccess()) }
    }

    fun setAllFilesAccessMode(on: Boolean) {
        if (on && !AllFilesAccess.isSupported()) {
            _state.update { it.copy(notice = "Full file access needs Android 11 or newer. Your chosen folders still work.") }
            return
        }
        if (on && !AllFilesAccess.hasAccess()) {
            _state.update { it.copy(allFilesAccessGranted = false, notice = "Allow “All files access” in Android settings first, then come back and turn this on.") }
            return
        }
        prefs.edit().putBoolean(KEY_USE_ALL_FILES_ACCESS, on).apply()
        _state.update { it.copy(useAllFilesAccess = on, notice = if (on) "Full file access is on. System app folders stay off-limits." else "Back to your chosen folders only.") }
    }

    fun openAllFilesAccessSettings() {
        if (!AllFilesAccess.openSettings(getApplication())) {
            _state.update { it.copy(notice = "Couldn't open that Android settings page. You can keep using your chosen folders.") }
        }
    }

    fun completeOnboarding() {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
        _state.update { it.copy(onboardingDone = true) }
    }

    fun replayOnboarding() {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, false).apply()
        _state.update { it.copy(onboardingDone = false) }
    }

    /** AI choices only: no keys, no folders, no privacy switches (see [SettingsBackup]). */
    fun exportSettings(): String {
        val providers = ProviderChoice.entries
        return SettingsBackupCodec.encode(
            SettingsBackup(
                provider = _state.value.provider.name,
                models = providers.mapNotNull { p -> modelFor(p).takeIf { it.isNotBlank() }?.let { p.name to it } }.toMap(),
                recoveryModels = providers.mapNotNull { p -> (prefs.getString(recoveryModelPreference(p), "") ?: "").takeIf { it.isNotBlank() }?.let { p.name to it } }.toMap(),
                automaticModelRouting = _state.value.automaticModelRouting,
            ),
        )
    }

    /** Returns true if [text] was a valid settings file and was applied. Never touches keys, folders or privacy switches. */
    fun importSettings(text: String): Boolean {
        val b = SettingsBackupCodec.decode(text, ProviderChoice.entries.map { it.name }.toSet()) ?: return false
        val edit = prefs.edit()
        b.models.forEach { (p, m) -> edit.putString(modelPreference(ProviderChoice.valueOf(p)), m) }
        b.recoveryModels.forEach { (p, m) -> edit.putString(recoveryModelPreference(ProviderChoice.valueOf(p)), m) }
        edit.putBoolean("automatic_model_routing", b.automaticModelRouting).apply()
        _state.update { it.copy(automaticModelRouting = b.automaticModelRouting) }
        setProvider(ProviderChoice.valueOf(b.provider))
        return true
    }

    fun showNotice(text: String) = _state.update { it.copy(notice = text) }

    /** Blocking, small file: call from a background dispatcher. */
    fun exportSettingsTo(uri: Uri): Boolean = runCatching {
        getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.use {
            it.write(exportSettings().toByteArray(Charsets.UTF_8)); true
        } ?: false
    }.getOrDefault(false)

    /** Blocking, small file: call from a background dispatcher. Files over [SettingsBackupCodec.MAX_BYTES] are refused unread. */
    fun importSettingsFrom(uri: Uri): Boolean {
        val text = runCatching {
            getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                val out = java.io.ByteArrayOutputStream()
                val buf = ByteArray(8 * 1024)
                var total = 0
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    total += n
                    if (total > SettingsBackupCodec.MAX_BYTES) return@use null
                    out.write(buf, 0, n)
                }
                out.toString("UTF-8")
            }
        }.getOrNull() ?: return false
        return importSettings(text)
    }

    fun dismissNotice() = _state.update { it.copy(notice = null) }

    fun scheduleCurrent(intervalHours: Long) {
        val s = _state.value
        if (s.goal.isBlank() || s.model.isBlank() || (!s.useAllFilesAccess && s.roots.isEmpty())) {
            _state.update { it.copy(notice = "Finish setup (AI key, model and a folder) and describe a task first.") }
            return
        }
        if (s.useAllFilesAccess && !AllFilesAccess.hasAccess()) {
            _state.update { it.copy(allFilesAccessGranted = false, notice = "Full file access isn't allowed right now. Turn it on in Android settings first.") }
            return
        }
        if (s.automaticModelRouting && (s.recoveryModel.isBlank() || s.recoveryModel == s.model)) {
            _state.update { it.copy(notice = "Pick a different backup model in Settings → Advanced first.") }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val task = ScheduledTask("task-${System.currentTimeMillis()}", s.goal, s.model, s.roots.map { it.name }, s.roots.map { it.uri }, intervalHours, s.sendFileContents, s.dryRun, manual = true, provider = s.provider.name, automaticModelRouting = s.automaticModelRouting, recoveryModel = s.recoveryModel.ifBlank { null }, useAllFilesAccess = s.useAllFilesAccess)
                ScheduledTasks.schedule(getApplication(), task)
                _state.update { it.copy(scheduledTasks = ScheduledTasks.list(getApplication()), notice = "Done. I'll check ${intervalLabel(intervalHours).lowercase()} and suggest changes. I won't change files on my own.") }
            }.onFailure { e -> _state.update { it.copy(notice = e.message ?: "Could not schedule task.") } }
        }
    }

    fun cancelScheduledTask(id: String) = viewModelScope.launch(Dispatchers.IO) {
        ScheduledTasks.cancel(getApplication(), id)
        _state.update { it.copy(scheduledTasks = ScheduledTasks.list(getApplication()), notice = "Stopped repeating that task.") }
    }

    // ───────── running a task ─────────

    private fun provider(choice: ProviderChoice = _state.value.provider): ResilientProvider {
        val key = { secrets.get(secretKey(choice)) ?: "" }
        val selected = when (choice) {
            ProviderChoice.GEMINI -> GeminiProvider(apiKey = key)
            ProviderChoice.OPENAI -> OpenAIProvider(apiKey = key)
            ProviderChoice.CLAUDE -> ClaudeProvider(apiKey = key)
        }
        return ResilientProvider(selected, tracker = usage)
    }

    fun answerConfirmation(response: ConfirmationResponse) = TaskBridge.answerConfirmation(response)

    fun answerReview(response: ConfirmationResponse, approvedIds: Set<String>?) = TaskBridge.answerReview(response, approvedIds)

    fun answerQuestion(answer: String?) = TaskBridge.answerQuestion(answer?.takeIf { it.isNotBlank() })

    fun runTask() {
        val s = _state.value
        when {
            s.running -> return
            s.apiKeyMask == null -> { _state.update { it.copy(notice = "Add your ${providerName(s.provider)} API key in Settings first.") }; return }
            s.model.isBlank() -> { _state.update { it.copy(notice = "Choose an AI model in Settings first.") }; return }
            s.automaticModelRouting && (s.recoveryModel.isBlank() || s.recoveryModel == s.model) -> { _state.update { it.copy(notice = "Pick a different backup model in Settings → Advanced, or turn off retrying.") }; return }
            s.useAllFilesAccess && !s.allFilesAccessSupported -> { _state.update { it.copy(notice = "Full file access needs Android 11 or newer. Choose folders in Settings instead.") }; return }
            s.useAllFilesAccess && !AllFilesAccess.hasAccess() -> { _state.update { it.copy(allFilesAccessGranted = false, notice = "Full file access isn't allowed anymore. Allow it in Android settings or choose folders instead.") }; return }
            !s.useAllFilesAccess && s.roots.isEmpty() -> { _state.update { it.copy(notice = "Choose at least one folder in Settings first.") }; return }
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
            putExtra(AgentTaskService.EXTRA_USE_ALL_FILES_ACCESS, s.useAllFilesAccess)
            putStringArrayListExtra(AgentTaskService.EXTRA_ROOT_NAMES, ArrayList(s.roots.map { it.name }))
            putStringArrayListExtra(AgentTaskService.EXTRA_ROOT_URIS, ArrayList(s.roots.map { it.uri }))
        }
        _state.update { it.copy(running = true, outcome = null, log = emptyList()) }
        getApplication<Application>().startForegroundService(intent)
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
        _state.update { it.copy(running = true, checkpointAvailable = false, outcome = null, log = emptyList(), notice = "Continuing the interrupted task…") }
        getApplication<Application>().startForegroundService(intent)
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
        val taskId = current.outcome?.taskId?.takeIf { it.isNotBlank() } ?: current.restorableUndoTaskId ?: return
        undoTask(taskId)
    }

    fun undoTask(taskId: String) {
        val current = _state.value
        val useAllFilesAccess = prefs.getBoolean(undoFsPreference(taskId), false)
        if (useAllFilesAccess && !AllFilesAccess.hasAccess()) {
            _state.update { it.copy(allFilesAccessGranted = false, notice = "Allow “All files access” in Android settings again, then try the undo.") }
            return
        }
        val app = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val taskRoots = prefs.getStringSet("undo_roots_$taskId", emptySet()).orEmpty().mapNotNull { encoded ->
                    val separator = encoded.indexOf('\u0000')
                    if (separator <= 0 || separator == encoded.lastIndex) null
                    else SafRoot(encoded.substring(0, separator), Uri.parse(encoded.substring(separator + 1)))
                }
                val rootsForUndo = taskRoots.ifEmpty { current.roots.map { SafRoot(it.name, Uri.parse(it.uri)) } }
                val fs = if (useAllFilesAccess) AllFilesAccess.createFileSystem(app) else SafFileSystem(app, rootsForUndo)
                undoManager.undo(taskId, fs)
            }
                .onSuccess { report ->
                    val skipped = if (report.notes.isEmpty()) "" else " ${report.notes.size} item(s) were skipped because they changed or no longer exist."
                    prefs.edit().remove(undoFsPreference(taskId)).remove("undo_roots_$taskId").apply()
                    _state.update { it.copy(notice = "Undone: ${report.reverted} change(s) put back.$skipped", outcome = it.outcome?.copy(canUndo = false), restorableUndoTaskId = null) }
                    refreshHistory()
                }
                .onFailure { error -> _state.update { it.copy(notice = "Couldn't undo: ${error.message ?: "I can't reach your files right now."}") } }
        }
    }

    fun clearOutcome() = _state.update {
        it.copy(outcome = null, dismissedRecentTaskId = it.outcome?.taskId ?: it.dismissedRecentTaskId)
    }

    fun refreshNotificationStatus() {
        val on = NotificationManagerCompat.from(getApplication<Application>()).areNotificationsEnabled()
        _state.update { it.copy(notificationsEnabled = on) }
    }

    fun notificationsAsked(): Boolean = prefs.getBoolean("notifications_asked", false)
    fun markNotificationsAsked() = prefs.edit().putBoolean("notifications_asked", true).apply()

    private fun modelFor(provider: ProviderChoice): String = prefs.getString(modelPreference(provider), "") ?: ""

    private companion object {
        const val KEY_NAME = "gemini_api_key"
        const val KEY_USE_ALL_FILES_ACCESS = "use_all_files_access"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private fun undoFsPreference(taskId: String) = "undo_all_files_$taskId"
        fun modelPreference(provider: ProviderChoice) = "model_${provider.name.lowercase()}"
        fun recoveryModelPreference(provider: ProviderChoice) = "recovery_model_${provider.name.lowercase()}"
        fun secretKey(provider: ProviderChoice) = when (provider) {
            ProviderChoice.GEMINI -> KEY_NAME
            ProviderChoice.OPENAI -> "openai_api_key"
            ProviderChoice.CLAUDE -> "claude_api_key"
        }
    }
}
