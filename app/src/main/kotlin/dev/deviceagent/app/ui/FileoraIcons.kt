package dev.deviceagent.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class FileoraIconName {
    AiAssistant,
    AiSparkle,
    Add,
    Delete,
    Error,
    Folder,
    FolderOpen,
    History,
    Home,
    Notifications,
    PauseTask,
    Permissions,
    PrivacyLock,
    ResumeTask,
    Retry,
    ReviewChanges,
    Settings,
    Success,
    Undo,
    Warning,
}

/** Uses theme-tinted Material vectors. Only launcher artwork may come from assets/. */
@Composable
fun FileoraIcon(
    name: FileoraIconName,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    Icon(
        imageVector = name.imageVector,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint,
    )
}

private val FileoraIconName.imageVector: ImageVector
    get() = when (this) {
        FileoraIconName.AiAssistant -> Icons.Filled.SmartToy
        FileoraIconName.AiSparkle -> Icons.Filled.AutoAwesome
        FileoraIconName.Add -> Icons.Filled.Add
        FileoraIconName.Delete -> Icons.Filled.Delete
        FileoraIconName.Error -> Icons.Filled.Error
        FileoraIconName.Folder -> Icons.Filled.Folder
        FileoraIconName.FolderOpen -> Icons.Filled.FolderOpen
        FileoraIconName.History -> Icons.Filled.History
        FileoraIconName.Home -> Icons.Filled.Home
        FileoraIconName.Notifications -> Icons.Filled.Notifications
        FileoraIconName.PauseTask -> Icons.Filled.Pause
        FileoraIconName.Permissions -> Icons.Filled.Security
        FileoraIconName.PrivacyLock -> Icons.Filled.Lock
        FileoraIconName.ResumeTask -> Icons.Filled.PlayArrow
        FileoraIconName.Retry -> Icons.Filled.Refresh
        FileoraIconName.ReviewChanges -> Icons.Filled.FactCheck
        FileoraIconName.Settings -> Icons.Filled.Settings
        FileoraIconName.Success -> Icons.Filled.CheckCircle
        FileoraIconName.Undo -> Icons.AutoMirrored.Filled.Undo
        FileoraIconName.Warning -> Icons.Filled.Warning
    }
