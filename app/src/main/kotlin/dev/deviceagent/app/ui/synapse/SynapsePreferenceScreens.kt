@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package dev.deviceagent.app.ui.synapse

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.PackageInfoCompat
import dev.deviceagent.app.R
import dev.deviceagent.app.ui.LocalSynapseAppearance
import dev.deviceagent.app.ui.LocalUpdateSynapseAppearance

private data class PermissionSample(val name: String, val detail: String, val icon: SynapseIcon)

@Composable
internal fun PermissionsDesign(nav: (SynapseRoute) -> Unit) {
    val required =
        listOf(
            PermissionSample(
                "Storage access",
                "Read and manage selected files.",
                SynapseIcon.FOLDER,
            ),
            PermissionSample(
                "Accessibility service",
                "Review UI interaction access.",
                SynapseIcon.PERMISSIONS,
            ),
            PermissionSample(
                "Notifications",
                "Task progress, results, and alerts.",
                SynapseIcon.ACTIVITY,
            ),
            PermissionSample(
                "Modify system settings",
                "Access to device settings.",
                SynapseIcon.SETTINGS,
            ),
        )
    val optional =
        listOf(
            PermissionSample("Camera", "Capture photos and scan documents.", SynapseIcon.IMAGE),
            PermissionSample("Microphone", "Record audio and voice input.", SynapseIcon.DEVICE),
            PermissionSample("Location", "Location-based task context.", SynapseIcon.NETWORK),
            PermissionSample("Contacts", "Contact names and information.", SynapseIcon.FILE),
            PermissionSample("Phone", "Call-related task context.", SynapseIcon.DEVICE),
            PermissionSample("Calendar", "Schedules and reminders.", SynapseIcon.SCHEDULES),
        )
    var grants by rememberSaveable {
        mutableStateOf(required.map { it.name } + listOf("Camera", "Location", "Calendar"))
    }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var showRequired by rememberSaveable { mutableStateOf(true) }
    var showOptional by rememberSaveable { mutableStateOf(true) }
    val grantedRequired = required.count { it.name in grants }
    val grantedOptional = optional.count { it.name in grants }
    DesignPage(
        SynapseRoute.PERMISSIONS,
        "Permissions",
        "Manage the permissions Synapse needs to work on your device safely.",
        nav,
    ) {
        DesignSection(
            "Sample permission overview",
            SynapseIcon.PERMISSIONS,
            "Learn more",
            { selected = "Permission model" },
        ) {
            Muted("Sample statuses; review actual grants in Live mode.")
            InlineStats(
                "Required" to "$grantedRequired / 4",
                "Optional" to "$grantedOptional / 6",
                "Not granted" to "${10-grants.size}",
                "Not needed" to "0",
            )
        }
        DesignSection(
            "Required permissions",
            SynapseIcon.PERMISSIONS,
            "$grantedRequired / 4  ${if(showRequired)"⌃" else "⌄"}",
            { showRequired = !showRequired },
        ) {
            if (showRequired)
                required.forEach { p ->
                    DesignRow(
                        p.icon,
                        p.name,
                        p.detail,
                        if (p.name in grants) "Granted" else "Not granted",
                        onClick = { selected = p.name },
                    )
                }
        }
        DesignSection(
            "Optional permissions",
            SynapseIcon.PRIVACY,
            "$grantedOptional / 6  ${if(showOptional)"⌃" else "⌄"}",
            { showOptional = !showOptional },
        ) {
            if (showOptional)
                optional.forEach { p ->
                    DesignRow(
                        p.icon,
                        p.name,
                        p.detail,
                        if (p.name in grants) "Granted" else "Not granted",
                        onClick = { selected = p.name },
                    )
                }
        }
        DesignRow(
            SynapseIcon.SETTINGS,
            "Advanced",
            "Review security controls and access scope",
            onClick = { nav(SynapseRoute.SECURITY) },
        )
    }
    if (selected != null)
        DesignDialog(
            onDismissRequest = { selected = null },
            title = { Text(selected!!) },
            text = {
                Text(
                    if (selected == "Permission model")
                        "This screen models the approved permission design. Synapse currently requests folder access and optional notifications through Android; it does not request accessibility, contacts, camera, microphone, phone, or location access."
                    else
                        "Change the sample status for this permission. This does not request or revoke Android access."
                )
            },
            confirmButton = {
                TextButton({
                    if (selected != "Permission model")
                        grants =
                            if (selected in grants) grants - selected!! else grants + selected!!
                    selected = null
                }) {
                    Text(
                        if (selected == "Permission model") "Got it"
                        else if (selected in grants) "Revoke sample" else "Grant sample"
                    )
                }
            },
            dismissButton = {
                if (selected != "Permission model")
                    TextButton({ selected = null }) { Text("Cancel") }
            },
        )
}

@Composable
internal fun AppearanceDesign(
    appearance: String,
    onAppearance: (String) -> Unit,
    reducedMotion: Boolean,
    onReducedMotion: (Boolean) -> Unit,
    nav: (SynapseRoute) -> Unit = {},
) {
    val display = LocalSynapseAppearance.current
    val update = LocalUpdateSynapseAppearance.current
    val context = LocalContext.current
    var custom by rememberSaveable { mutableStateOf(false) }
    var hex by rememberSaveable { mutableStateOf(display.customAccent) }
    var info by rememberSaveable { mutableStateOf<String?>(null) }
    DesignPage(
        SynapseRoute.APPEARANCE,
        "Appearance",
        "Customize the look and feel of Synapse to match your preferences.",
        nav,
    ) {
        DesignSection("Theme", SynapseIcon.APPEARANCE) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Light", "Dark", "System").forEach { theme ->
                    val chosen = appearance == theme
                    Surface(
                        Modifier.weight(1f).selectable(chosen, role = Role.RadioButton) {
                            onAppearance(theme)
                        },
                        shape = RoundedCornerShape(8.dp),
                        border =
                            BorderStroke(
                                1.dp,
                                if (chosen) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                            ),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Column(
                            Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Row(
                                Modifier.fillMaxWidth()
                                    .height(32.dp)
                                    .border(
                                        .5.dp,
                                        MaterialTheme.colorScheme.outlineVariant,
                                        RoundedCornerShape(4.dp),
                                    )
                            ) {
                                val previewColors =
                                    if (theme == "System")
                                        listOf(Color(0xFFF8FAFF), Color(0xFF111318))
                                    else
                                        listOf(
                                            if (theme == "Dark") Color(0xFF111318)
                                            else Color(0xFFF8FAFF)
                                        )
                                previewColors.forEach { color ->
                                    Column(
                                        Modifier.weight(1f)
                                            .fillMaxHeight()
                                            .background(color)
                                            .padding(5.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Box(
                                            Modifier.fillMaxWidth()
                                                .height(4.dp)
                                                .background(Color(0xFF94A3B8).copy(alpha = .25f))
                                        )
                                        Box(
                                            Modifier.width(18.dp)
                                                .height(5.dp)
                                                .background(display.accentColor)
                                        )
                                    }
                                }
                            }
                            Text(theme, style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (chosen) "Selected"
                                else if (theme == "System") "Follow system"
                                else if (theme == "Dark") "Easy on the eyes"
                                else "Clean and bright",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }
        }
        DesignSection("Color accent", SynapseIcon.APPEARANCE) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                listOf("Blue", "Purple", "Green", "Orange", "Red", "Pink", "Custom").forEach { name
                    ->
                    Column(
                        Modifier.widthIn(min = 48.dp).heightIn(min = 48.dp).selectable(
                            display.accent == name,
                            role = Role.RadioButton,
                        ) {
                            if (name == "Custom") custom = true
                            else update(display.copy(accent = name))
                        },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Box(
                            Modifier.size(22.dp)
                                .then(
                                    if (display.accent == name)
                                        Modifier.border(
                                            2.dp,
                                            MaterialTheme.colorScheme.onSurface,
                                            CircleShape,
                                        )
                                    else Modifier
                                )
                                .padding(3.dp)
                                .background(display.copy(accent = name).accentColor, CircleShape)
                        )
                        Text(name, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        DesignSection("Typography", SynapseIcon.MODELS) {
            ChoiceChips(listOf("Small", "Medium", "Large", "Extra large"), display.textSize) {
                update(display.copy(textSize = it))
            }
            DesignChoice(
                "Font style",
                listOf("System default", "Serif", "Monospace"),
                display.font,
            ) {
                update(display.copy(font = it))
            }
            Muted("Text also follows the Android font-size setting.")
        }
        DesignSection("Display & layout", SynapseIcon.DEVICE) {
            DesignChoice(
                "App density",
                listOf("Comfortable", "Standard", "Compact"),
                display.density,
            ) {
                update(display.copy(density = it))
            }
            SwitchSetting(
                "Show detailed information",
                "Secondary descriptions in demo lists.",
                display.details,
            ) {
                update(display.copy(details = it))
            }
            SwitchSetting("Animations", "Use decorative launch transitions.", !reducedMotion) {
                onReducedMotion(!it)
            }
            SwitchSetting(
                "Show app icons",
                "Decorative icons alongside visible labels.",
                display.icons,
            ) {
                update(display.copy(icons = it))
            }
        }
        DesignSection("Accessibility", SynapseIcon.PERMISSIONS) {
            SwitchSetting(
                "High contrast mode",
                "Stronger text and component borders.",
                display.highContrast,
            ) {
                update(display.copy(highContrast = it))
            }
            SwitchSetting(
                "Reduce motion",
                "Minimize decorative transitions.",
                reducedMotion,
                onChange = onReducedMotion,
            )
            DesignRow(
                SynapseIcon.ABOUT,
                "Screen reader support",
                "TalkBack uses Android accessibility settings",
                "Open",
                onClick = {
                    runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                        .onFailure {
                            info =
                                "Android accessibility settings are unavailable in this environment."
                        }
                },
            )
        }
    }
    if (custom)
        DesignDialog(
            onDismissRequest = { custom = false },
            title = { Text("Custom accent") },
            text = {
                OutlinedTextField(
                    hex,
                    {
                        hex =
                            it.filter { c -> c.isDigit() || c.lowercaseChar() in 'a'..'f' }.take(6)
                    },
                    label = { Text("Hex color (RRGGBB)") },
                    isError = hex.length != 6,
                )
            },
            confirmButton = {
                TextButton(
                    {
                        update(display.copy(accent = "Custom", customAccent = hex))
                        custom = false
                    },
                    enabled = hex.length == 6,
                ) {
                    Text("Apply")
                }
            },
            dismissButton = { TextButton({ custom = false }) { Text("Cancel") } },
        )
    DemoDetailDialog(info, { info = null }, info.orEmpty())
}

@Composable
internal fun AboutDesign(nav: (SynapseRoute) -> Unit) {
    val context = LocalContext.current
    val uri = LocalUriHandler.current
    val pkg = remember(context) { context.packageManager.getPackageInfo(context.packageName, 0) }
    var detail by rememberSaveable { mutableStateOf<String?>(null) }
    val open: (String) -> Unit = { path ->
        runCatching { uri.openUri("https://github.com/MohammedTeir/synapse$path") }
            .onFailure {
                detail =
                    "A browser is unavailable. Visit github.com/MohammedTeir/synapse in a browser."
            }
    }
    DesignPage(
        SynapseRoute.ABOUT,
        "About",
        "App information, version details, credits, and support.",
        nav,
    ) {
        Section("") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Image(
                    painterResource(R.drawable.synapse_brand_mark),
                    "Synapse logo",
                    Modifier.size(44.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text("Synapse", style = MaterialTheme.typography.titleLarge)
                    Muted("Your AI-powered device operator")
                    Text(
                        "Version ${pkg.versionName ?: "Unknown"} · Build ${PackageInfoCompat.getLongVersionCode(pkg)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("Productivity", "Automation", "AI Assistant").forEach {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        DesignSection(
            "App updates",
            SynapseIcon.DOWNLOAD,
            "Check for updates",
            { open("/releases") },
        ) {
            DesignRow(
                SynapseIcon.DOWNLOAD,
                "Published releases",
                "View versions and change notes",
                "Online",
                onClick = { open("/releases") },
            )
        }
        DesignSection("Help & resources", SynapseIcon.ABOUT) {
            DesignRow(
                SynapseIcon.FILE,
                "User guide",
                "Learn how to use Synapse",
                onClick = { open("#readme") },
            )
            DesignRow(
                SynapseIcon.ABOUT,
                "FAQ",
                "Questions about scope, approval, and safety",
                onClick = {
                    detail =
                        "Synapse asks before risky changes and verifies recorded outcomes. Try demo explores sample workflows; Go live uses your granted folders and configured provider. Missing runtimes are shown as unavailable."
                },
            )
            DesignRow(
                SynapseIcon.FILE,
                "Documentation",
                "Technical details and integration",
                onClick = { open("/tree/main/docs") },
            )
            DesignRow(
                SynapseIcon.NETWORK,
                "Community",
                "Visit the project repository",
                onClick = { open("") },
            )
            DesignRow(
                SynapseIcon.AGENT,
                "Send feedback",
                "Open the project issue tracker",
                onClick = { open("/issues") },
            )
        }
        DesignSection("Legal", SynapseIcon.FILE) {
            DesignRow(
                SynapseIcon.FILE,
                "Terms of service",
                onClick = {
                    detail =
                        "This repository does not publish separate app-specific terms. Connected AI services have their own terms, billing, and data policies."
                },
            )
            DesignRow(
                SynapseIcon.PRIVACY,
                "Privacy policy",
                "Review data sharing controls",
                onClick = { nav(SynapseRoute.PRIVACY) },
            )
            DesignRow(
                SynapseIcon.TERMINAL,
                "Open source licenses",
                onClick = {
                    detail =
                        "AndroidX, Jetpack Compose, Kotlin, kotlinx.coroutines, and Material Components use Apache License 2.0. The Synapse repository currently has no project LICENSE file. Dependency notices remain in the APK where supplied."
                },
            )
        }
        DesignSection("Additional information", SynapseIcon.ABOUT) {
            SmallValue("Package name", context.packageName)
            SmallValue(
                "Minimum Android version",
                "Android 8.0 (API ${context.applicationInfo.minSdkVersion})",
            )
            SmallValue("Build", PackageInfoCompat.getLongVersionCode(pkg).toString())
        }
    }
    if (detail != null)
        DesignDialog(
            onDismissRequest = { detail = null },
            title = { Text("Synapse information") },
            text = { Text(detail!!) },
            confirmButton = { TextButton({ detail = null }) { Text("Done") } },
        )
}
