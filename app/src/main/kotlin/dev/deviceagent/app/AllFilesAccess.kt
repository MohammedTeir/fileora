package dev.deviceagent.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import dev.deviceagent.core.fs.FsException
import dev.deviceagent.core.fs.JvmFileSystem
import dev.deviceagent.core.tools.ErrorKind
import java.io.File

/** Helpers for the special, user-controlled Android all-files-access permission. */
internal object AllFilesAccess {
    fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    fun hasAccess(): Boolean = isSupported() && Environment.isExternalStorageManager()

    /** Create the broad filesystem only after the special access grant has been checked. */
    @Suppress("DEPRECATION")
    fun createFileSystem(context: Context): JvmFileSystem {
        if (!hasAccess()) {
            throw FsException(ErrorKind.PERMISSION_DENIED, "All files access has not been granted")
        }
        val candidates = linkedSetOf(Environment.getExternalStorageDirectory().canonicalFile)
        context.getExternalFilesDirs(null).filterNotNull().forEach { appFilesDir ->
            sharedRootForAppFilesDir(context, appFilesDir)?.let(candidates::add)
        }
        val accessibleRoots = candidates.filter { root ->
            root.isDirectory && runCatching { Environment.isExternalStorageManager(root) }.getOrDefault(false)
        }
        if (accessibleRoots.isEmpty()) {
            throw FsException(ErrorKind.PERMISSION_DENIED, "No mounted shared-storage root is available to this app")
        }
        val blocked = accessibleRoots.flatMap { root ->
            listOf(File(root, "Android/data").path, File(root, "Android/obb").path)
        }
        return JvmFileSystem(accessibleRoots.map { it.path }, blockedPaths = blocked)
    }

    private fun sharedRootForAppFilesDir(context: Context, appFilesDir: File): File? = runCatching {
        val files = appFilesDir.canonicalFile
        val packageDir = files.parentFile ?: return null
        val dataDir = packageDir.parentFile ?: return null
        val androidDir = dataDir.parentFile ?: return null
        val root = androidDir.parentFile ?: return null
        if (files.name != "files" || packageDir.name != context.packageName || dataDir.name != "data" || androidDir.name != "Android") return null
        root.canonicalFile
    }.getOrNull()

    /** Opens the app-specific special-access page, falling back to the global settings page. */
    fun openSettings(context: Context): Boolean {
        if (!isSupported()) return false
        val intents = listOf(
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                .setData(Uri.parse("package:${context.packageName}")),
            Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION),
        )
        for (intent in intents) {
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return true
            } catch (_: ActivityNotFoundException) {
                // Some Android builds omit the app-specific settings activity.
            } catch (_: SecurityException) {
                // Try the broader settings entry point on vendor-modified Android builds.
            }
        }
        return false
    }
}
