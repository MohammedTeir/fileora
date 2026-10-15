package dev.deviceagent.app.ui.synapse

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
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
    Icon(
        painter = painterResource(id = drawable),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = tint,
    )
}
