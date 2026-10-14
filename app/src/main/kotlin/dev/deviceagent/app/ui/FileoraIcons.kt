package dev.deviceagent.app.ui

import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import dev.deviceagent.app.R

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

/**
 * Renders an existing integrated Fileora raster icon without applying a tint.
 * For future changes, use Material icons or independently authored resources;
 * do not add or derive resources from assets/ (see AGENTS.md).
 * Android automatically selects drawable-night-nodpi variants for the four icons that have
 * contrast-adjusted dark-theme exports; other icons safely fall back to the standard asset.
 */
@Composable
fun FileoraIcon(
    name: FileoraIconName,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
) {
    Icon(
        painter = painterResource(name.resourceId),
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint,
    )
}

@get:DrawableRes
private val FileoraIconName.resourceId: Int
    get() = when (this) {
        FileoraIconName.AiAssistant -> R.drawable.fileora_icon_ai_assistant
        FileoraIconName.AiSparkle -> R.drawable.fileora_icon_ai_sparkle
        FileoraIconName.Add -> R.drawable.fileora_icon_add
        FileoraIconName.Delete -> R.drawable.fileora_icon_delete
        FileoraIconName.Error -> R.drawable.fileora_icon_error
        FileoraIconName.Folder -> R.drawable.fileora_icon_folder
        FileoraIconName.FolderOpen -> R.drawable.fileora_icon_folder_open
        FileoraIconName.History -> R.drawable.fileora_icon_history
        FileoraIconName.Home -> R.drawable.fileora_icon_home
        FileoraIconName.Notifications -> R.drawable.fileora_icon_notifications
        FileoraIconName.PauseTask -> R.drawable.fileora_icon_pause_task
        FileoraIconName.Permissions -> R.drawable.fileora_icon_permissions
        FileoraIconName.PrivacyLock -> R.drawable.fileora_icon_privacy_lock
        FileoraIconName.ResumeTask -> R.drawable.fileora_icon_resume_task
        FileoraIconName.Retry -> R.drawable.fileora_icon_retry
        FileoraIconName.ReviewChanges -> R.drawable.fileora_icon_review_changes
        FileoraIconName.Settings -> R.drawable.fileora_icon_settings
        FileoraIconName.Success -> R.drawable.fileora_icon_success
        FileoraIconName.Undo -> R.drawable.fileora_icon_undo
        FileoraIconName.Warning -> R.drawable.fileora_icon_warning
    }
