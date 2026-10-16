@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package dev.deviceagent.app.ui.synapse

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.deviceagent.app.Tone
import dev.deviceagent.app.ui.LocalSynapseAppearance
import dev.deviceagent.app.ui.SynapseDesignTokens
import dev.deviceagent.app.ui.toneColors

@Composable
internal fun ScreenPage(
    eyebrow: String,
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = SynapseDesignTokens.pageHorizontal,
                vertical = SynapseDesignTokens.pageVertical,
            ),
        verticalArrangement = Arrangement.spacedBy(SynapseDesignTokens.sectionGap),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                eyebrow.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Medium,
            )
            Muted(subtitle)
        }
        content()
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
internal fun Muted(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
internal fun Section(
    title: String,
    subtitle: String? = null,
    dense: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(SynapseDesignTokens.cardRadius),
        colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border =
            BorderStroke(SynapseDesignTokens.borderWidth, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            Modifier.padding(
                if (dense) 6.dp
                else
                    when (LocalSynapseAppearance.current.density) {
                        "Compact" -> 8.dp
                        "Standard" -> 10.dp
                        else -> SynapseDesignTokens.cardPadding
                    }
            ),
            verticalArrangement =
                Arrangement.spacedBy(if (dense) 4.dp else SynapseDesignTokens.cardContentGap),
        ) {
            if (title.isNotBlank()) Text(title, style = MaterialTheme.typography.titleMedium)
            subtitle?.let { Muted(it) }
            content()
        }
    }
}

@Composable
internal fun Actions(content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = content,
    )
}

@Composable
internal fun Primary(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick,
        enabled = enabled,
        modifier = Modifier.heightIn(min = SynapseDesignTokens.minimumTouchTarget),
        shape = RoundedCornerShape(SynapseDesignTokens.buttonRadius),
    ) {
        Text(label)
    }
}

@Composable
internal fun Secondary(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(
        onClick,
        enabled = enabled,
        modifier = Modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(label)
    }
}

@Composable
internal fun Badge(
    label: String,
    tone: Tone = Tone.INFO,
    icon: ImageVector? = null,
    colors: Pair<Color, Color>? = null,
) {
    val (bg, fg) = colors ?: toneColors(tone)
    Surface(color = bg, contentColor = fg, shape = RoundedCornerShape(8.dp)) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            icon?.let { Icon(it, null, Modifier.size(16.dp)) }
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
internal fun RiskBadge(level: Int) {
    val names =
        listOf("Read", "Low impact", "Moderate", "Destructive", "Privileged", "Critical · blocked")
    Badge(
        "L$level · ${names[level.coerceIn(0,5)]}",
        when (level) {
            0,
            1 -> Tone.INFO
            2 -> Tone.WARNING
            else -> Tone.ERROR
        },
        if (level >= 3) Icons.Default.Shield else Icons.Default.Visibility,
        colors =
            when (level) {
                0 ->
                    MaterialTheme.colorScheme.surfaceContainerHighest to
                        MaterialTheme.colorScheme.onSurface
                4 ->
                    MaterialTheme.colorScheme.tertiaryContainer to
                        MaterialTheme.colorScheme.onTertiaryContainer
                else -> null
            },
    )
}

@Composable
internal fun Notice(
    title: String,
    body: String,
    tone: Tone = Tone.INFO,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val (bg, fg) = toneColors(tone)
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = bg,
        contentColor = fg,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    when (tone) {
                        Tone.ERROR,
                        Tone.WARNING -> Icons.Default.Warning
                        Tone.SUCCESS -> Icons.Default.CheckCircle
                        else -> Icons.Default.Info
                    },
                    null,
                    Modifier.size(20.dp),
                )
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(body, style = MaterialTheme.typography.bodyMedium)
            content()
        }
    }
}

@Composable
internal fun LinkRow(
    title: String,
    detail: String,
    icon: ImageVector = Icons.Default.Tune,
    onClick: () -> Unit,
) {
    Surface(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            Modifier.padding(12.dp).heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(
                    icon,
                    null,
                    Modifier.padding(10.dp).size(22.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Muted(detail)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(18.dp))
        }
    }
}

@Composable
internal fun Metric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
) {
    Surface(
        modifier,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Muted(label)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
            detail?.let { Muted(it) }
        }
    }
}

@Composable
internal fun Metrics(vararg values: Pair<String, String>) {
    values.toList().chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { (label, value) -> Metric(label, value, modifier = Modifier.weight(1f)) }
        }
    }
}

@Composable
internal fun Disclosure(
    title: String,
    summary: String? = null,
    initiallyOpen: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by rememberSaveable(title) { mutableStateOf(initiallyOpen) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth()
                .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
                .clickable(role = Role.Button) { expanded = !expanded }
                .padding(vertical = 12.dp)
                .heightIn(min = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                summary?.let { Muted(it) }
            }
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                if (expanded) "Collapse $title" else "Expand $title",
            )
        }
        if (expanded) Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
internal fun SwitchSetting(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    dense: Boolean = false,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
            .heightIn(min = if (dense) 28.dp else SynapseDesignTokens.compactRowHeight)
            .padding(vertical = if (dense) 2.dp else 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontSize = if (dense) 10.sp else 11.sp,
                lineHeight = if (dense) 12.sp else 15.sp,
            )
            if (subtitle.isNotEmpty())
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = if (dense) 9.sp else 10.sp,
                    lineHeight = if (dense) 11.sp else 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
        }
        CompactSwitch(checked, enabled)
    }
}

/** Visual thumb only: the labelled parent supplies one accessible toggle target. */
@Composable
internal fun CompactSwitch(checked: Boolean, enabled: Boolean = true) {
    Box(
        Modifier.size(26.dp, 15.dp)
            .background(
                (if (checked) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant)
                    .copy(alpha = if (enabled) 1f else .45f),
                RoundedCornerShape(50),
            )
            .padding(2.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(Modifier.size(11.dp).background(Color.White, RoundedCornerShape(50)))
    }
}

@Composable
internal fun ChoiceChips(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected == option,
                { onSelect(option) },
                label = { Text(option, style = MaterialTheme.typography.labelSmall) },
            )
        }
    }
}

@Composable
internal fun CodeBlock(text: String, dense: Boolean = false) {
    Surface(Modifier.fillMaxWidth(), color = Color(0xFF111318), shape = RoundedCornerShape(12.dp)) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            SelectionContainer {
                Text(
                    text,
                    Modifier.horizontalScroll(rememberScrollState())
                        .padding(if (dense) 8.dp else 14.dp),
                    color = Color(0xFFD7E8D3),
                    fontFamily = FontFamily.Monospace,
                    style =
                        if (dense)
                            MaterialTheme.typography.bodySmall.copy(
                                fontSize = 8.sp,
                                lineHeight = 11.sp,
                            )
                        else MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
internal fun LabeledValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Muted(label)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
internal fun VerifiedRow(title: String, detail: String, passed: Boolean = true) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Icon(
            if (passed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            null,
            tint =
                if (passed) toneColors(Tone.SUCCESS).second
                else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Muted(detail)
        }
    }
}

@Composable
internal fun TaskProgress(task: DemoTask) {
    Text("${task.verified} of ${task.total} verified", style = MaterialTheme.typography.titleLarge)
    LinearProgressIndicator(
        progress = { task.progress },
        modifier =
            Modifier.fillMaxWidth().height(8.dp).semantics {
                contentDescription = "${(task.progress*100).toInt()} percent verified"
            },
        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    )
    Actions {
        Badge("${task.skipped} skipped")
        Badge("${task.blocked} blocked", if (task.blocked > 0) Tone.WARNING else Tone.INFO)
        Badge("${task.running} running")
        Badge("${task.queued} queued")
    }
}

@Composable
internal fun DemoTaskCard(task: DemoTask, onOpen: () -> Unit) {
    Section(task.title, task.id) {
        Badge(
            task.status.name.lowercase().replaceFirstChar { it.uppercase() },
            when (task.status) {
                DemoTaskStatus.VERIFIED -> Tone.SUCCESS
                DemoTaskStatus.WAITING,
                DemoTaskStatus.PARTIAL -> Tone.WARNING
                else -> Tone.INFO
            },
        )
        TaskProgress(task)
        TextButton(onOpen) { Text("Open task") }
    }
}

@Composable
internal fun ConfirmAction(
    title: String,
    body: String,
    label: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    DesignDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onConfirm) { Text(label) } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } },
    )
}

internal data class ScreenCopy(val empty: String, val error: String, val success: String)

internal fun screenCopy(route: SynapseRoute): ScreenCopy =
    when (route) {
        SynapseRoute.AGENT ->
            ScreenCopy(
                "Start with a goal",
                "The AI provider is unreachable. Your draft is safe.",
                "The task has a verified result to review.",
            )
        SynapseRoute.PLAN,
        SynapseRoute.APPROVAL ->
            ScreenCopy(
                "No matching changes",
                "The target scope changed. Review a refreshed plan.",
                "The current plan revision has been reviewed.",
            )
        SynapseRoute.CREDENTIALS,
        SynapseRoute.PROVIDERS,
        SynapseRoute.MODELS ->
            ScreenCopy(
                "No provider configured",
                "The connection could not be verified. Check your provider settings.",
                "The sample connection checks passed.",
            )
        SynapseRoute.PACKAGES,
        SynapseRoute.PACKAGE_DETAIL ->
            ScreenCopy(
                "No packages found",
                "Artifact verification failed. This package cannot be installed.",
                "The sample binary and requested capability were verified.",
            )
        SynapseRoute.MEMORY ->
            ScreenCopy(
                "No memories in this scope",
                "Stored facts are stale. Refresh device observations.",
                "The sample preference has been saved.",
            )
        SynapseRoute.ACTIVITY,
        SynapseRoute.AUDIT ->
            ScreenCopy(
                "No tasks yet",
                "Some task evidence is unavailable. Missing checks are not marked passed.",
                "The sample result is ready to inspect.",
            )
        SynapseRoute.SANDBOX,
        SynapseRoute.ATTACHMENTS ->
            ScreenCopy(
                "No folders selected",
                "Access to the selected document was revoked.",
                "The selected scope has been checked.",
            )
        SynapseRoute.RECOVERY ->
            ScreenCopy(
                "No diagnosis evidence",
                "The cause is still unknown. More evidence is needed.",
                "The recovery result was verified in this sample.",
            )
        SynapseRoute.CHECKPOINT ->
            ScreenCopy(
                "No saved checkpoint",
                "The destination changed. Reconcile before resuming.",
                "The sample checkpoint was reconciled.",
            )
        SynapseRoute.UNDO ->
            ScreenCopy(
                "No reversible changes",
                "A file changed after the task. Review the conflict.",
                "The selected sample restore completed.",
            )
        SynapseRoute.SECURITY ->
            ScreenCopy(
                "No optional privileges granted",
                "The root manager denied access. The backend stays disabled.",
                "The policy settings were saved; per-operation checks still apply.",
            )
        SynapseRoute.SCHEDULE ->
            ScreenCopy(
                "No schedules yet",
                "A required folder or credential is missing.",
                "The sample schedule was saved.",
            )
        SynapseRoute.TERMINAL ->
            ScreenCopy(
                "No command output",
                "The process failed: ModuleNotFoundError: requests.",
                "The example command output passed its sample checks.",
            )
        SynapseRoute.APPS ->
            ScreenCopy(
                "No visible apps match",
                "The APK signature or architecture is incompatible.",
                "The example package identity was inspected.",
            )
        else ->
            ScreenCopy(
                "Nothing to show yet",
                "This information is temporarily unavailable.",
                "The sample state is ready.",
            )
    }

@Composable
internal fun PreviewPane(
    route: SynapseRoute,
    state: PreviewState,
    onReady: () -> Unit,
    content: @Composable () -> Unit,
) {
    val copy = screenCopy(route)
    when (state) {
        PreviewState.LOADING ->
            ScreenPage("Preview state", route.title, "Loading the selected scenario…") {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                repeat(3) {
                    Surface(
                        Modifier.fillMaxWidth().height(72.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(16.dp),
                    ) {}
                }
                Secondary("Show ready state", onClick = onReady)
            }
        PreviewState.EMPTY ->
            ScreenPage(
                "Preview state",
                copy.empty,
                "Demo content is intentionally empty in this variant.",
            ) {
                Icon(
                    Icons.Default.Inbox,
                    null,
                    Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Primary("Load demo content", onClick = onReady)
            }
        else ->
            Column(Modifier.fillMaxSize()) {
                when (state) {
                    PreviewState.ERROR ->
                        Box(Modifier.padding(12.dp)) {
                            Notice("Needs attention", copy.error, Tone.ERROR) {
                                TextButton(onReady) { Text("Retry sample") }
                            }
                        }
                    PreviewState.SUCCESS ->
                        Box(Modifier.padding(12.dp)) {
                            Notice("Success preview", copy.success, Tone.SUCCESS) {
                                TextButton(onReady) { Text("Return to current scenario") }
                            }
                        }
                    PreviewState.OFFLINE ->
                        Box(Modifier.padding(12.dp)) {
                            Notice(
                                "Offline preview",
                                "Cloud planning waits for connectivity. Local work and history remain accessible.",
                                Tone.WARNING,
                            ) {
                                TextButton(onReady) { Text("Restore demo connection") }
                            }
                        }
                    else -> Unit
                }
                Box(Modifier.weight(1f)) { content() }
            }
    }
}
