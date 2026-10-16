@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package dev.deviceagent.app.ui.synapse

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.deviceagent.app.ui.LocalSynapseAppearance
import dev.deviceagent.app.ui.SynapseDesignTokens

@Composable
internal fun DesignPage(
    route: SynapseRoute,
    title: String,
    subtitle: String,
    nav: (SynapseRoute) -> Unit,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = if (route.destination == Destination.AGENT) 20.dp else 16.dp,
                    vertical = if (route == SynapseRoute.AGENT) 18.dp else 8.dp,
                ),
            verticalArrangement =
                Arrangement.spacedBy(if (route == SynapseRoute.AGENT) 12.dp else 8.dp),
        ) {
            if (
                route.destination == Destination.AGENT &&
                    route !in listOf(SynapseRoute.AGENT, SynapseRoute.WELCOME)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "‹  Agent",
                        Modifier.clickable(role = Role.Button) { nav(SynapseRoute.AGENT) }
                            .padding(vertical = 2.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.weight(1f))
                    val step =
                        when (route) {
                            SynapseRoute.ATTACHMENTS -> 2
                            SynapseRoute.PLAN -> 3
                            SynapseRoute.APPROVAL -> 4
                            SynapseRoute.TASK_RESULT -> 6
                            else -> 5
                        }
                    val steps =
                        if (
                            route in
                                listOf(
                                    SynapseRoute.ATTACHMENTS,
                                    SynapseRoute.PLAN,
                                    SynapseRoute.APPROVAL,
                                )
                        )
                            4
                        else 6
                    Text(
                        "Step $step of $steps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(8.dp))
                    repeat(steps) { index ->
                        if (index > 0)
                            Box(
                                Modifier.width(14.dp)
                                    .height(2.dp)
                                    .background(
                                        if (index < step) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant
                                    )
                            )
                        Box(
                            Modifier.size(7.dp)
                                .background(
                                    if (index < step) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(50),
                                )
                        )
                    }
                }
            }
            if (route in listOf(SynapseRoute.AUDIT, SynapseRoute.UNDO))
                Text(
                    "‹  History",
                    Modifier.clickable(role = Role.Button) { nav(SynapseRoute.ACTIVITY) }
                        .padding(vertical = 0.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            if (
                route in
                    listOf(
                        SynapseRoute.PROVIDERS,
                        SynapseRoute.CREDENTIALS,
                        SynapseRoute.MODELS,
                        SynapseRoute.PRIVACY,
                        SynapseRoute.USAGE,
                        SynapseRoute.SECURITY,
                        SynapseRoute.MEMORY,
                        SynapseRoute.PERMISSIONS,
                        SynapseRoute.APPEARANCE,
                        SynapseRoute.ABOUT,
                    )
            )
                Text(
                    "‹  Settings",
                    Modifier.clickable(role = Role.Button) { nav(SynapseRoute.SETTINGS) }
                        .padding(vertical = 0.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (
                    route == SynapseRoute.UNDO ||
                        route == SynapseRoute.RECOVERY ||
                        route == SynapseRoute.TASK_RESULT && title == "Task completed"
                ) {
                    val success = title == "Task completed" || title == "Recovery verified"
                    val color = if (success) Color(0xFF00AF58) else MaterialTheme.colorScheme.error
                    Surface(color = color.copy(alpha = .1f), shape = CircleShape) {
                        AssetIcon(
                            if (success) SynapseIcon.APPROVAL
                            else if (route == SynapseRoute.UNDO) SynapseIcon.UNDO
                            else SynapseIcon.WARNING,
                            modifier = Modifier.padding(8.dp),
                            tint = color,
                            size = 22.dp,
                        )
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        buildAnnotatedString {
                            if (route == SynapseRoute.AGENT && title.contains(", ")) {
                                append(title.substringBefore(", ") + ", ")
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                    append(title.substringAfter(", "))
                                }
                            } else append(title)
                        },
                        style =
                            if (route == SynapseRoute.AGENT)
                                MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = 26.sp,
                                    lineHeight = 30.sp,
                                )
                            else if (
                                route.destination == Destination.SETTINGS ||
                                    route in listOf(SynapseRoute.AUDIT, SynapseRoute.UNDO)
                            )
                                MaterialTheme.typography.titleLarge
                            else MaterialTheme.typography.headlineMedium,
                    )
                    if (subtitle.isNotEmpty())
                        Text(
                            subtitle,
                            style =
                                if (route == SynapseRoute.AGENT)
                                    MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp,
                                    )
                                else MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                }
            }
            if (
                route.destination != Destination.AGENT &&
                    route !in listOf(SynapseRoute.AUDIT, SynapseRoute.UNDO)
            )
                DesignTabs(route, nav)
            content()
            Spacer(Modifier.height(6.dp))
        }
        if (footer != null)
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = .96f)) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                    footer()
                }
            }
    }
}

@Composable
internal fun DesignTabs(route: SynapseRoute, nav: (SynapseRoute) -> Unit) {
    val tabs =
        when (route.destination) {
            Destination.ACTIVITY ->
                listOf(
                    Triple("Tasks", SynapseRoute.MONITOR, SynapseIcon.ACTIVITY),
                    Triple("History", SynapseRoute.ACTIVITY, SynapseIcon.HISTORY),
                    Triple("Checkpoints", SynapseRoute.CHECKPOINT, SynapseIcon.CHECKPOINTS),
                    Triple("Schedules", SynapseRoute.SCHEDULE, SynapseIcon.SCHEDULES),
                )
            Destination.DEVICE ->
                listOf(
                    Triple("Overview", SynapseRoute.DEVICE, SynapseIcon.DEVICE),
                    Triple("Files", SynapseRoute.SANDBOX, SynapseIcon.FOLDER),
                    Triple("Apps", SynapseRoute.APPS, SynapseIcon.APPS),
                    Triple("Terminal", SynapseRoute.TERMINAL, SynapseIcon.TERMINAL),
                    Triple("Tools", SynapseRoute.TOOLS, SynapseIcon.TOOLS),
                ) +
                    if (route in listOf(SynapseRoute.PACKAGES, SynapseRoute.PACKAGE_DETAIL))
                        listOf(Triple("Packages", SynapseRoute.PACKAGES, SynapseIcon.PACKAGES))
                    else emptyList()
            else ->
                if (
                    route in
                        listOf(
                            SynapseRoute.SETTINGS,
                            SynapseRoute.PERMISSIONS,
                            SynapseRoute.APPEARANCE,
                            SynapseRoute.ABOUT,
                        )
                )
                    listOf(
                        Triple("AI Providers", SynapseRoute.PROVIDERS, SynapseIcon.PROVIDERS),
                        Triple("Permissions", SynapseRoute.PERMISSIONS, SynapseIcon.PERMISSIONS),
                        Triple("Security", SynapseRoute.SECURITY, SynapseIcon.SECURITY),
                        Triple("Appearance", SynapseRoute.APPEARANCE, SynapseIcon.APPEARANCE),
                        Triple("About", SynapseRoute.ABOUT, SynapseIcon.ABOUT),
                    )
                else
                    listOf(
                        Triple("AI Providers", SynapseRoute.PROVIDERS, SynapseIcon.PROVIDERS),
                        Triple("Credentials", SynapseRoute.CREDENTIALS, SynapseIcon.CREDENTIALS),
                        Triple("Models", SynapseRoute.MODELS, SynapseIcon.MODELS),
                        Triple("Privacy", SynapseRoute.PRIVACY, SynapseIcon.PRIVACY),
                        Triple("Usage", SynapseRoute.USAGE, SynapseIcon.USAGE),
                    ) +
                        when (route) {
                            SynapseRoute.SECURITY ->
                                listOf(
                                    Triple("Security", SynapseRoute.SECURITY, SynapseIcon.SECURITY)
                                )
                            SynapseRoute.MEMORY ->
                                listOf(Triple("Memory", SynapseRoute.MEMORY, SynapseIcon.MEMORY))
                            else -> emptyList()
                        }
        }
    val scroll = rememberScrollState()
    val selectedIndex =
        tabs.indexOfFirst {
            it.second == route ||
                route == SynapseRoute.PACKAGE_DETAIL && it.second == SynapseRoute.PACKAGES
        }
    LaunchedEffect(route, scroll.maxValue) {
        if (selectedIndex >= 4) scroll.scrollTo(scroll.maxValue)
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            tabs.forEach { (label, target, icon) ->
                val selected =
                    route == target ||
                        route == SynapseRoute.SETTINGS && target == SynapseRoute.PROVIDERS ||
                        route == SynapseRoute.PACKAGE_DETAIL && target == SynapseRoute.PACKAGES
                Surface(
                    onClick = { nav(target) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(7.dp),
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor =
                        if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface,
                ) {
                    Row(
                        Modifier.heightIn(min = SynapseDesignTokens.tabHeight)
                            .padding(horizontal = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        AssetIcon(icon, tint = LocalContentColor.current, size = 11.dp)
                        Text(
                            label,
                            fontSize = if (tabs.size > 4) 8.sp else 9.sp,
                            lineHeight = 12.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun DesignRow(
    icon: SynapseIcon,
    title: String,
    detail: String = "",
    value: String = "",
    tint: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick)
                else Modifier
            )
            .heightIn(min = SynapseDesignTokens.compactRowHeight)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Surface(color = tint.copy(alpha = .08f), shape = RoundedCornerShape(8.dp)) {
            AssetIcon(icon, modifier = Modifier.padding(4.dp), tint = tint, size = 16.dp)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (detail.isNotEmpty() && LocalSynapseAppearance.current.details)
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
        }
        if (value.isNotEmpty()) DesignValue(value, tint)
        trailing?.invoke()
        if (onClick != null && trailing == null)
            Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun DesignSearch(value: String, onValue: (String) -> Unit, hint: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(8.dp)) {
        Row(
            Modifier.fillMaxWidth()
                .heightIn(min = SynapseDesignTokens.fieldHeight)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AssetIcon(
                SynapseIcon.SEARCH,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                size = 16.dp,
            )
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value,
                onValue,
                Modifier.weight(1f).semantics { contentDescription = hint },
                singleLine = true,
                textStyle =
                    MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                decorationBox = { field ->
                    if (value.isEmpty())
                        Text(
                            hint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    field()
                },
            )
        }
    }
}

@Composable
internal fun DesignSection(
    title: String,
    icon: SynapseIcon? = null,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    subtitle: String = "",
    content: @Composable ColumnScope.() -> Unit,
) {
    Section("") {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            icon?.let { AssetIcon(it, size = 18.dp) }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                if (subtitle.isNotEmpty())
                    Text(
                        subtitle,
                        style =
                            MaterialTheme.typography.bodySmall.copy(
                                fontSize = 9.sp,
                                lineHeight = 11.sp,
                            ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
            }
            if (action != null)
                Text(
                    action,
                    Modifier.then(
                            if (onAction != null)
                                Modifier.clickable(role = Role.Button, onClick = onAction)
                            else Modifier
                        )
                        .padding(vertical = 2.dp),
                    fontSize = 9.sp,
                    lineHeight = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
        }
        Column(content = content)
    }
}

@Composable
internal fun InlineStats(vararg stats: Pair<String, String>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        stats.forEach { (label, value) ->
            Column(Modifier.weight(1f)) {
                Text(value, style = MaterialTheme.typography.titleSmall)
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun FullButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick,
        Modifier.fillMaxWidth().heightIn(min = SynapseDesignTokens.compactRowHeight),
        enabled = enabled,
        shape = RoundedCornerShape(SynapseDesignTokens.buttonRadius),
    ) {
        Text(text)
    }
}

@Composable
internal fun DemoDetailDialog(
    title: String?,
    onDismiss: () -> Unit,
    text: String = "This is a demo preview. No device operation is performed.",
) {
    if (title != null)
        DesignDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = { Text(text) },
            confirmButton = { TextButton(onDismiss) { Text("Done") } },
        )
}

@Composable
internal fun DesignChoice(
    label: String,
    options: List<String>,
    value: String,
    onChange: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().heightIn(min = SynapseDesignTokens.compactRowHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Box {
            Surface(
                onClick = { open = true },
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(6.dp),
            ) {
                Text(
                    "$value  ⌄",
                    Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            DropdownMenu(open, { open = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        { Text(option) },
                        {
                            onChange(option)
                            open = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun SmallValue(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            label,
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
    }
}

/** Reference's thin track with a full 48dp interactive target. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DesignSlider(
    value: Float,
    onValue: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    label: String,
) {
    Slider(
        value,
        onValue,
        modifier =
            Modifier.fillMaxWidth().heightIn(min = 32.dp).semantics { contentDescription = label },
        valueRange = valueRange,
        thumb = {
            Box(
                Modifier.size(16.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        },
        track = {
            SliderDefaults.Track(
                it,
                Modifier.height(4.dp),
                thumbTrackGapSize = 0.dp,
                drawStopIndicator = null,
            )
        },
    )
}

/** Explicit width keeps forms stable across platform dialog minimum-width rules. */
@Composable
internal fun DesignDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        title = title,
        text = text,
        modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(.92f),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    )
}

@Composable
private fun DesignValue(value: String, fallback: Color) {
    val tone =
        when (value.lowercase()) {
            "granted",
            "active",
            "enabled",
            "configured",
            "verified",
            "allowed",
            "ready",
            "connected",
            "completed",
            "installed",
            "success" -> dev.deviceagent.app.Tone.SUCCESS
            "blocked",
            "denied",
            "not granted",
            "failed" -> dev.deviceagent.app.Tone.ERROR
            "ask",
            "restricted",
            "pending",
            "paused",
            "needs review" -> dev.deviceagent.app.Tone.WARNING
            else -> null
        }
    if (tone == null) Text(value, style = MaterialTheme.typography.labelSmall, color = fallback)
    else {
        val (container, foreground) = dev.deviceagent.app.ui.toneColors(tone)
        Surface(color = container, contentColor = foreground, shape = RoundedCornerShape(5.dp)) {
            Row(
                Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(Modifier.size(4.dp).background(foreground, CircleShape))
                Text(value, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/** Compact reference notice. Safety text is always visible and is never truncated. */
@Composable
internal fun DesignNotice(
    title: String,
    body: String,
    tone: dev.deviceagent.app.Tone = dev.deviceagent.app.Tone.INFO,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val (bg, fg) = dev.deviceagent.app.ui.toneColors(tone)
    Surface(
        Modifier.fillMaxWidth(),
        color = bg.copy(alpha = .65f),
        contentColor = fg,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AssetIcon(
                    if (
                        tone == dev.deviceagent.app.Tone.ERROR ||
                            tone == dev.deviceagent.app.Tone.WARNING
                    )
                        SynapseIcon.WARNING
                    else SynapseIcon.ABOUT,
                    tint = fg,
                    size = 20.dp,
                )
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    Text(body, style = MaterialTheme.typography.bodySmall)
                }
            }
            content()
        }
    }
}

@Composable
internal fun DesignSegments(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { option ->
            Surface(
                onClick = { onSelect(option) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(6.dp),
                color =
                    if (selected == option) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceContainer,
                contentColor =
                    if (selected == option) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Text(
                    option,
                    Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                    fontSize = 9.sp,
                    lineHeight = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Composable
internal fun DesignAction(
    label: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    danger: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 32.dp),
        shape = RoundedCornerShape(50),
        color =
            if (!enabled) MaterialTheme.colorScheme.surfaceContainer
            else if (primary) MaterialTheme.colorScheme.primary
            else if (danger) MaterialTheme.colorScheme.errorContainer.copy(alpha = .3f)
            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = .6f),
        contentColor =
            if (!enabled) MaterialTheme.colorScheme.onSurfaceVariant
            else if (primary) MaterialTheme.colorScheme.onPrimary
            else if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
    ) {
        Text(
            label,
            Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelSmall,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
internal fun StorageUsageBar() {
    Row(
        Modifier.fillMaxWidth()
            .height(5.dp)
            .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50))
    ) {
        listOf(
                .284f to MaterialTheme.colorScheme.primary,
                .197f to Color(0xFFA54FFF),
                .075f to Color(0xFFFFBF24),
            )
            .forEach { (amount, color) ->
                Box(Modifier.weight(amount).fillMaxHeight().background(color))
            }
        Spacer(Modifier.weight(.444f))
    }
}
