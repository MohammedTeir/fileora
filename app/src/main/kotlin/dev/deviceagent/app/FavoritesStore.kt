package dev.deviceagent.app

import android.content.Context

/**
 * Remembers which files and folders the user starred, as sandbox paths, in app-private storage.
 * It only stores paths on this device; nothing here is sent to an AI service.
 */
class FavoritesStore(context: Context) {
    private val prefs = context.getSharedPreferences("fileora_favorites", Context.MODE_PRIVATE)

    fun load(): Set<String> = prefs.getStringSet(KEY, emptySet())?.toSet() ?: emptySet()

    fun save(paths: Set<String>) {
        prefs.edit().putStringSet(KEY, paths.toSet()).apply()
    }

    private companion object {
        const val KEY = "paths"
    }
}
