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

enum class SynapseIconName {
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
fun SynapseIcon(
    name: SynapseIconName,
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

private val SynapseIconName.imageVector: ImageVector
    get() = when (this) {
        SynapseIconName.AiAssistant -> Icons.Filled.SmartToy
        SynapseIconName.AiSparkle -> Icons.Filled.AutoAwesome
        SynapseIconName.Add -> Icons.Filled.Add
        SynapseIconName.Delete -> Icons.Filled.Delete
        SynapseIconName.Error -> Icons.Filled.Error
        SynapseIconName.Folder -> Icons.Filled.Folder
        SynapseIconName.FolderOpen -> Icons.Filled.FolderOpen
        SynapseIconName.History -> Icons.Filled.History
        SynapseIconName.Home -> Icons.Filled.Home
        SynapseIconName.Notifications -> Icons.Filled.Notifications
        SynapseIconName.PauseTask -> Icons.Filled.Pause
        SynapseIconName.Permissions -> Icons.Filled.Security
        SynapseIconName.PrivacyLock -> Icons.Filled.Lock
        SynapseIconName.ResumeTask -> Icons.Filled.PlayArrow
        SynapseIconName.Retry -> Icons.Filled.Refresh
        SynapseIconName.ReviewChanges -> Icons.Filled.FactCheck
        SynapseIconName.Settings -> Icons.Filled.Settings
        SynapseIconName.Success -> Icons.Filled.CheckCircle
        SynapseIconName.Undo -> Icons.AutoMirrored.Filled.Undo
        SynapseIconName.Warning -> Icons.Filled.Warning
    }
