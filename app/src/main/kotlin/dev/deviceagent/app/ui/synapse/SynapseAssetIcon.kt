package dev.deviceagent.app.ui.synapse

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import dev.deviceagent.app.ui.LocalSynapseAppearance
import dev.deviceagent.app.ui.SynapseDesignTokens

/** Renders an approved Synapse vector drawable without replacing its authored color. */
@Composable
internal fun SynapseAssetIcon(
    @DrawableRes drawable: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = SynapseDesignTokens.iconSize,
    tint: Color = Color.Unspecified,
) {
    if (contentDescription != null || LocalSynapseAppearance.current.icons)
        Icon(
            painter = painterResource(id = drawable),
            contentDescription = contentDescription,
            modifier = modifier.size(size),
            tint = tint,
        )
}

internal enum class SynapseIcon(@DrawableRes val drawable: Int) {
    ABOUT(dev.deviceagent.app.R.drawable.synapse_icon_about),
    ACTIVITY(dev.deviceagent.app.R.drawable.synapse_icon_activity),
    AGENT(dev.deviceagent.app.R.drawable.synapse_icon_agent),
    APPEARANCE(dev.deviceagent.app.R.drawable.synapse_icon_appearance),
    APPROVAL(dev.deviceagent.app.R.drawable.synapse_icon_approval),
    APPS(dev.deviceagent.app.R.drawable.synapse_icon_apps),
    AUDIT(dev.deviceagent.app.R.drawable.synapse_icon_audit),
    CHECKPOINTS(dev.deviceagent.app.R.drawable.synapse_icon_checkpoints),
    CREDENTIALS(dev.deviceagent.app.R.drawable.synapse_icon_credentials),
    DELETE(dev.deviceagent.app.R.drawable.synapse_icon_delete),
    DEVICE(dev.deviceagent.app.R.drawable.synapse_icon_device),
    DOWNLOAD(dev.deviceagent.app.R.drawable.synapse_icon_download),
    FILE(dev.deviceagent.app.R.drawable.synapse_icon_file),
    FOLDER(dev.deviceagent.app.R.drawable.synapse_icon_folder),
    HISTORY(dev.deviceagent.app.R.drawable.synapse_icon_history),
    IMAGE(dev.deviceagent.app.R.drawable.synapse_icon_image),
    MEMORY(dev.deviceagent.app.R.drawable.synapse_icon_memory),
    MODELS(dev.deviceagent.app.R.drawable.synapse_icon_models),
    NETWORK(dev.deviceagent.app.R.drawable.synapse_icon_network),
    PACKAGES(dev.deviceagent.app.R.drawable.synapse_icon_packages),
    PAUSE(dev.deviceagent.app.R.drawable.synapse_icon_pause),
    PERMISSIONS(dev.deviceagent.app.R.drawable.synapse_icon_permissions),
    PLAY(dev.deviceagent.app.R.drawable.synapse_icon_play),
    PLUS(dev.deviceagent.app.R.drawable.synapse_icon_plus),
    PRIVACY(dev.deviceagent.app.R.drawable.synapse_icon_privacy),
    PROVIDERS(dev.deviceagent.app.R.drawable.synapse_icon_providers),
    RECOVERY(dev.deviceagent.app.R.drawable.synapse_icon_recovery),
    ROOT(dev.deviceagent.app.R.drawable.synapse_icon_root),
    SCHEDULES(dev.deviceagent.app.R.drawable.synapse_icon_schedules),
    SEARCH(dev.deviceagent.app.R.drawable.synapse_icon_search),
    SECURITY(dev.deviceagent.app.R.drawable.synapse_icon_security),
    SETTINGS(dev.deviceagent.app.R.drawable.synapse_icon_settings),
    TERMINAL(dev.deviceagent.app.R.drawable.synapse_icon_terminal),
    TOOLS(dev.deviceagent.app.R.drawable.synapse_icon_tools),
    UNDO(dev.deviceagent.app.R.drawable.synapse_icon_undo),
    UPLOAD(dev.deviceagent.app.R.drawable.synapse_icon_upload),
    USAGE(dev.deviceagent.app.R.drawable.synapse_icon_usage),
    WARNING(dev.deviceagent.app.R.drawable.synapse_icon_warning),
}

@Composable
internal fun AssetIcon(
    icon: SynapseIcon,
    description: String? = null,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    size: Dp = SynapseDesignTokens.iconSize,
) = SynapseAssetIcon(icon.drawable, description, modifier, size, tint)
