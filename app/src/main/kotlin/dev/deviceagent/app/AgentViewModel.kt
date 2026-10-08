package dev.deviceagent.app

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.deviceagent.core.agent.AgentConfig
import dev.deviceagent.core.agent.AgentEngine
import dev.deviceagent.core.agent.TaskOutcome
import dev.deviceagent.core.ai.ResilientProvider
import dev.deviceagent.core.ai.UsageTracker
import dev.deviceagent.core.ai.gemini.GeminiProvider
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

data class UiState(
    val apiKeyMask: String? = null,
    val model: String = "",
    val availableModels: List<String> = emptyList(),
    val roots: List<RootUi> = emptyList(),
    val goal: String = "",
    val dryRun: Boolean = false,
    val manualMode: Boolean = false,
    val running: Boolean = false,
    val log: List<String> = emptyList(),
    val outcome: TaskOutcome? = null,
    val pendingConfirmation: ConfirmationRequest? = null,
    val pendingQuestion: String? = null,
    val notice: String? = null,
    val usageSummary: String = "",
    val history: List<AuditEntry> = emptyList(),
    val restorableUndoTaskId: String? = null,
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

    init {
        val key = secrets.get(KEY_NAME)
        key?.let { redactor.addSecret(it) }
        _state.update {
            it.copy(
                apiKeyMask = key?.let(::maskSecret),
                model = prefs.getString("model", "") ?: "",
                roots = loadRoots(),
            )
        }
        refreshHistory()
    }

    private fun refreshHistory() = viewModelScope.launch(Dispatchers.IO) {
        val rows = audit.entries().takeLast(100).reversed()
        _state.update { it.copy(history = rows, restorableUndoTaskId = undoStore.latestTaskId()) }
    }

    // ───────── settings ─────────

    fun saveApiKey(key: String) {
        val k = key.trim()
        if (k.isEmpty()) return
        secrets.put(KEY_NAME, k)
        redactor.addSecret(k)
        _state.update { it.copy(apiKeyMask = maskSecret(k), notice = "API key saved securely on this device.") }
    }

    fun removeApiKey() {
        secrets.remove(KEY_NAME)
        _state.update { it.copy(apiKeyMask = null, availableModels = emptyList(), notice = "API key removed.") }
    }

    fun setModel(model: String) {
        prefs.edit().putString("model", model.trim()).apply()
        _state.update { it.copy(model = model.trim()) }
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
    fun dismissNotice() = _state.update { it.copy(notice = null) }

    // ───────── running a task ─────────

    private fun provider(): ResilientProvider {
        val gemini = GeminiProvider(apiKey = { secrets.get(KEY_NAME) ?: "" })
        return ResilientProvider(gemini, tracker = usage)
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
            s.apiKeyMask == null -> { _state.update { it.copy(notice = "Add your Gemini API key first.") }; return }
            s.model.isBlank() -> { _state.update { it.copy(notice = "Choose a model first (tap List models).") }; return }
            s.roots.isEmpty() -> { _state.update { it.copy(notice = "Add at least one folder the agent may use.") }; return }
            s.goal.isBlank() -> return
        }
        val fs = SafFileSystem(getApplication(), s.roots.map { SafRoot(it.name, Uri.parse(it.uri)) })
        lastTaskFs = fs
        val engine = AgentEngine(
            provider = provider(),
            registry = ToolRegistry(DefaultTools.all()),
            fs = fs,
            policy = PolicyEngine(PolicyConfig(mode = if (s.manualMode) InteractionMode.MANUAL else InteractionMode.NORMAL)),
            user = channel,
            undoManager = undoManager,
            redactor = redactor,
            audit = audit,
            log = { line -> _state.update { st -> st.copy(log = (st.log + redactor.redact(line)).takeLast(200)) } },
        )
        _state.update { it.copy(running = true, outcome = null, log = emptyList()) }
        // Default dispatcher: verification may hash large files and must never run on the main thread.
        viewModelScope.launch(Dispatchers.Default) {
            val outcome: TaskOutcome? = try {
                engine.run(s.goal, AgentConfig(model = s.model, dryRun = s.dryRun))
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                // Never crash the app on an unexpected failure; report it instead (redacted).
                _state.update { it.copy(notice = "The task stopped unexpectedly: ${redactor.redact(e.message ?: e.javaClass.simpleName)}") }
                null
            } finally {
                _state.update { it.copy(running = false) }
            }
            if (outcome != null) {
                _state.update {
                    it.copy(
                        outcome = outcome,
                        usageSummary = "${usage.requestCount} AI request(s), ${usage.inputTokens} tokens in / ${usage.outputTokens} out, ${usage.errorCount} error(s)",
                    )
                }
                refreshHistory()
            }
        }
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
    }
}
