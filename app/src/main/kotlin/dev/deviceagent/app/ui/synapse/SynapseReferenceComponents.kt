@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package dev.deviceagent.app.ui.synapse

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.deviceagent.app.ui.SynapseDesignTokens

@Composable
internal fun DesignPage(route: SynapseRoute, title: String, subtitle: String, nav: (SynapseRoute) -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (route.destination == Destination.AGENT && route !in listOf(SynapseRoute.AGENT, SynapseRoute.WELCOME)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("‹  Agent", Modifier.clickable(role = Role.Button) { nav(SynapseRoute.AGENT) }.padding(vertical = 5.dp), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.weight(1f))
                Text("Step ${(referenceRoutes.indexOf(route)-1).coerceIn(1,6)} of 6", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                repeat(6) { index -> Box(Modifier.padding(2.dp).size(5.dp).background(if (index < referenceRoutes.indexOf(route)-1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50))) }
            }
        }
        Column {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            if (subtitle.isNotEmpty()) Muted(subtitle)
        }
        if (route.destination != Destination.AGENT) DesignTabs(route, nav)
        content()
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
internal fun DesignTabs(route: SynapseRoute, nav: (SynapseRoute) -> Unit) {
    val tabs = when(route.destination) {
        Destination.ACTIVITY -> listOf(Triple("Tasks", SynapseRoute.MONITOR, SynapseIcon.ACTIVITY), Triple("History", SynapseRoute.ACTIVITY, SynapseIcon.HISTORY), Triple("Checkpoints", SynapseRoute.CHECKPOINT, SynapseIcon.CHECKPOINTS), Triple("Schedules", SynapseRoute.SCHEDULE, SynapseIcon.SCHEDULES))
        Destination.DEVICE -> listOf(Triple("Overview", SynapseRoute.DEVICE, SynapseIcon.DEVICE), Triple("Files", SynapseRoute.SANDBOX, SynapseIcon.FOLDER), Triple("Apps", SynapseRoute.APPS, SynapseIcon.APPS), Triple("Terminal", SynapseRoute.TERMINAL, SynapseIcon.TERMINAL), Triple("Tools", SynapseRoute.TOOLS, SynapseIcon.TOOLS), Triple("Packages", SynapseRoute.PACKAGES, SynapseIcon.PACKAGES))
        else -> if(route in listOf(SynapseRoute.SETTINGS, SynapseRoute.PERMISSIONS, SynapseRoute.APPEARANCE, SynapseRoute.ABOUT))
            listOf(Triple("AI Providers",SynapseRoute.PROVIDERS,SynapseIcon.PROVIDERS), Triple("Permissions",SynapseRoute.PERMISSIONS,SynapseIcon.PERMISSIONS), Triple("Security",SynapseRoute.SECURITY,SynapseIcon.SECURITY), Triple("Appearance",SynapseRoute.APPEARANCE,SynapseIcon.APPEARANCE), Triple("About",SynapseRoute.ABOUT,SynapseIcon.ABOUT))
        else listOf(Triple("AI Providers",SynapseRoute.PROVIDERS,SynapseIcon.PROVIDERS), Triple("Credentials",SynapseRoute.CREDENTIALS,SynapseIcon.CREDENTIALS), Triple("Models",SynapseRoute.MODELS,SynapseIcon.MODELS), Triple("Privacy",SynapseRoute.PRIVACY,SynapseIcon.PRIVACY), Triple("Usage",SynapseRoute.USAGE,SynapseIcon.USAGE), Triple("Security",SynapseRoute.SECURITY,SynapseIcon.SECURITY), Triple("Memory",SynapseRoute.MEMORY,SynapseIcon.MEMORY))
    }
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(8.dp)) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            tabs.forEach { (label,target,icon) ->
                val selected = route == target || route == SynapseRoute.PACKAGE_DETAIL && target == SynapseRoute.PACKAGES
                Surface(onClick = { nav(target) }, shape = RoundedCornerShape(7.dp), color = if(selected) MaterialTheme.colorScheme.primary else Color.Transparent, contentColor = if(selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface) {
                    Row(Modifier.heightIn(min = 48.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        AssetIcon(icon, tint = LocalContentColor.current, size = 14.dp)
                        Text(label, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
internal fun DesignRow(icon: SynapseIcon, title: String, detail: String = "", value: String = "", tint: Color = MaterialTheme.colorScheme.primary, onClick: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().then(if(onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier).heightIn(min = if(onClick == null && trailing == null) 38.dp else 48.dp).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Surface(color = tint.copy(alpha = .08f), shape = RoundedCornerShape(8.dp)) { AssetIcon(icon, modifier = Modifier.padding(5.dp), tint = tint, size = 18.dp) }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if(detail.isNotEmpty()) Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if(value.isNotEmpty()) Text(value, style = MaterialTheme.typography.labelSmall, color = tint)
        trailing?.invoke()
        if(onClick != null && trailing == null) Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun DesignSearch(value: String, onValue: (String) -> Unit, hint: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(8.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            AssetIcon(SynapseIcon.SEARCH, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 16.dp)
            Spacer(Modifier.width(8.dp))
            BasicTextField(value, onValue, Modifier.weight(1f), singleLine = true, textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface), decorationBox = { field ->
                if(value.isEmpty()) Text(hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                field()
            })
        }
    }
}

@Composable
internal fun DesignSection(title: String, icon: SynapseIcon? = null, action: String? = null, onAction: () -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    Section("") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            icon?.let { AssetIcon(it, size = 18.dp) }
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            if(action != null) Text(action, Modifier.clickable(role = Role.Button, onClick = onAction).padding(vertical = 5.dp), fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
        }
        Column(content = content)
    }
}

@Composable
internal fun InlineStats(vararg stats: Pair<String, String>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        stats.forEach { (label,value) -> Column(Modifier.weight(1f)) {
            Text(value, style = MaterialTheme.typography.titleSmall)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } }
    }
}

@Composable
internal fun FullButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = enabled, shape = RoundedCornerShape(SynapseDesignTokens.buttonRadius)) { Text(text) }
}

@Composable
internal fun DemoDetailDialog(title: String?, onDismiss: () -> Unit, text: String = "This is a demo preview. No device operation is performed.") {
    if(title != null) AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(text) }, confirmButton = { TextButton(onDismiss) { Text("Done") } })
}
