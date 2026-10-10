package dev.deviceagent.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.deviceagent.core.fs.FileInfo
import dev.deviceagent.core.fs.FileSystem
import dev.deviceagent.core.fs.FsException
import dev.deviceagent.core.tools.TRASH_DIR
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BrowserState(
    /** Null means "show the list of accessible roots". */
    val path: String? = null,
    val root: String? = null,
    val entries: List<FileInfo> = emptyList(),
    val roots: List<String> = emptyList(),
    val query: String = "",
    val sort: SortMode = SortMode.NAME,
    val ascending: Boolean = true,
    val selected: Set<String> = emptySet(),
    val loading: Boolean = false,
    val error: String? = null,
) {
    val visible: List<FileInfo> get() = sortEntries(filterEntries(entries, query), sort, ascending)
}

/**
 * Read-only browsing. This view model never writes, moves or deletes anything: changes still go
 * through the agent, so the policy engine, confirmation, verification and undo all keep applying.
 * It uses the same sandboxed [FileSystem] the agent uses, so it can only see what the agent can see.
 */
class FileBrowserViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(BrowserState())
    val state: StateFlow<BrowserState> = _state.asStateFlow()

    private var fs: FileSystem? = null
    private var configKey: String? = null
    private var job: Job? = null

    /** Call whenever the accessible folders change. Resets to the root list only if the config really changed. */
    fun configure(roots: List<RootUi>, useAllFilesAccess: Boolean) {
        val key = "$useAllFilesAccess|" + roots.joinToString("|") { it.name + "=" + it.uri }
        if (key == configKey) return
        configKey = key
        job?.cancel()
        _state.value = BrowserState(loading = true)
        job = viewModelScope.launch(Dispatchers.IO) {
            val built: Result<FileSystem> = runCatching {
                val app = getApplication<Application>()
                when {
                    useAllFilesAccess && AllFilesAccess.hasAccess() -> AllFilesAccess.createFileSystem(app)
                    useAllFilesAccess -> throw FsException(dev.deviceagent.core.tools.ErrorKind.PERMISSION_DENIED, "All files access is not allowed right now. Allow it in Android settings or use chosen folders.")
                    else -> SafFileSystem(app, roots.map { SafRoot(it.name, Uri.parse(it.uri)) })
                }
            }
            built.onSuccess {
                fs = it
                _state.value = BrowserState(roots = it.roots)
            }.onFailure { e ->
                fs = null
                _state.value = BrowserState(error = e.message ?: "Couldn't open your folders.")
            }
        }
    }

    fun openRoots() {
        job?.cancel()
        _state.update { it.copy(path = null, root = null, entries = emptyList(), query = "", selected = emptySet(), error = null, loading = false) }
    }

    fun open(path: String) {
        val system = fs ?: return
        job?.cancel()
        _state.update { it.copy(loading = true, error = null, query = "", selected = emptySet()) }
        job = viewModelScope.launch(Dispatchers.IO) {
            try {
                val list = system.list(path).filter { it.name != TRASH_DIR && !it.isSymlink }
                val root = system.rootOf(path)
                _state.update { it.copy(path = path, root = root, entries = list, loading = false) }
            } catch (e: FsException) {
                _state.update { it.copy(loading = false, error = e.message ?: "Couldn't open that folder.") }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = "Couldn't open that folder.") }
            }
        }
    }

    fun refresh() { _state.value.path?.let(::open) }

    /** Goes up one level, or back to the root list from a root. Returns false if already at the root list. */
    fun up(): Boolean {
        val s = _state.value
        val path = s.path ?: return false
        val system = fs ?: return false
        val parent = if (path == s.root) null else system.parent(path)
        if (parent == null) openRoots() else open(parent)
        return true
    }

    fun setQuery(q: String) = _state.update { it.copy(query = q) }
    fun setSort(mode: SortMode) = _state.update {
        if (it.sort == mode) it.copy(ascending = !it.ascending) else it.copy(sort = mode, ascending = true)
    }

    fun toggleSelected(path: String) = _state.update {
        it.copy(selected = if (path in it.selected) it.selected - path else it.selected + path)
    }
    fun clearSelection() = _state.update { it.copy(selected = emptySet()) }
}
