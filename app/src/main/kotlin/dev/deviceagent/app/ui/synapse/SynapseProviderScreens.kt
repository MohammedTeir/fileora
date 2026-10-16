@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package dev.deviceagent.app.ui.synapse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val providerNames = listOf("Google Gemini", "OpenAI", "Anthropic Claude", "Custom Provider")
private val providerDescriptions =
    listOf(
        "Powerful multimodal models from Google.",
        "GPT models from OpenAI.",
        "Claude models from Anthropic.",
        "OpenAI-compatible API endpoint.",
    )

private fun providerKey(name: String) =
    when (name) {
        "Google Gemini" -> "Gemini"
        "Anthropic Claude" -> "Claude"
        "Custom Provider" -> "Custom"
        else -> "OpenAI"
    }

private val providerModels =
    listOf(
        listOf("gemini-1.5-pro", "gemini-1.5-flash", "gemini-2.0-flash"),
        listOf("gpt-4o", "gpt-4o-mini", "gpt-4-turbo"),
        listOf("claude-3-5-sonnet", "claude-3-haiku", "claude-3-opus"),
        listOf("Custom endpoint", "Local model", "Self-hosted"),
    )

@Composable
private fun ProviderRow(index: Int, state: DemoState, expanded: Boolean, onClick: () -> Unit) {
    val active = providerKey(providerNames[index]) == state.provider
    Row(
        Modifier.fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = if (expanded) 60.dp else 44.dp)
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AssetIcon(SynapseIcon.PROVIDERS, size = 25.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(providerNames[index], style = MaterialTheme.typography.titleSmall)
                Text(
                    if (active) "Active" else "Inactive",
                    fontSize = 7.sp,
                    lineHeight = 9.sp,
                    color =
                        if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                if (expanded) providerDescriptions[index] else providerModels[index].first(),
                style =
                    MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, lineHeight = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (expanded)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    providerModels[index].forEach { model ->
                        Text(
                            model,
                            Modifier.background(
                                    MaterialTheme.colorScheme.surfaceContainer,
                                    RoundedCornerShape(3.dp),
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            fontSize = 7.sp,
                            lineHeight = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
        }
        Text(
            "Configure  ›",
            Modifier.background(
                    MaterialTheme.colorScheme.primaryContainer,
                    RoundedCornerShape(5.dp),
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 8.sp,
            lineHeight = 11.sp,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
internal fun SettingsOverviewDemo(
    s: DemoState,
    send: (DemoAction) -> Unit,
    nav: (SynapseRoute) -> Unit,
    reset: () -> Unit,
) {
    var temperature by rememberSaveable { mutableFloatStateOf(.7f) }
    var tokens by rememberSaveable { mutableStateOf("4096") }
    var response by rememberSaveable { mutableStateOf("Balanced") }
    var confirm by rememberSaveable { mutableStateOf(false) }
    DesignPage(
        SynapseRoute.SETTINGS,
        "Settings",
        "Configure Synapse, AI providers, permissions, and app preferences.",
        nav,
    ) {
        DesignSection(
            "AI Providers",
            SynapseIcon.PROVIDERS,
            "+ Add provider",
            { nav(SynapseRoute.PROVIDERS) },
            subtitle = "Manage AI models and credentials.",
        ) {
            providerNames.indices.forEach { i ->
                ProviderRow(i, s, expanded = false) { nav(SynapseRoute.PROVIDERS) }
            }
        }
        DesignSection(
            "Model Settings",
            SynapseIcon.MODELS,
            subtitle = "Choose default model and behavior.",
        ) {
            DesignPreference(
                "Default model",
                listOf(s.model, "gemini-1.5-pro", "gpt-4o", "claude-3-5-sonnet"),
                s.model,
            ) {
                send(DemoAction.Model(it))
            }
            DesignSliderPreference(
                "Temperature",
                String.format(java.util.Locale.ROOT, "%.1f", temperature),
                temperature,
                0f..1f,
            ) {
                temperature = it
            }
            DesignNumberPreference("Max output tokens", tokens, 1..32768) { tokens = it }
            DesignPreference(
                "Response style",
                listOf("Balanced", "Concise", "Detailed"),
                response,
            ) {
                response = it
            }
        }
        DesignSection(
            "Usage & Limits",
            SynapseIcon.USAGE,
            "View detailed usage",
            { nav(SynapseRoute.USAGE) },
        ) {
            InlineStats(
                "API requests" to "1,250 / 10,000",
                "Input tokens" to "2.4M",
                "Output tokens" to "0.8M",
                "Total time" to "12.5 hours",
            )
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { .125f },
                modifier = Modifier.fillMaxWidth().height(4.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            DesignAction("Agent memory", Modifier.weight(1f)) { nav(SynapseRoute.MEMORY) }
            DesignAction("Explore setup", Modifier.weight(1f)) { nav(SynapseRoute.WELCOME) }
            DesignAction("Reset demo workspace", Modifier.weight(1f)) { confirm = true }
        }
    }
    if (confirm)
        ConfirmAction(
            "Reset demo workspace?",
            "Sample tasks and preferences return to their initial state. Live settings are separate.",
            "Reset demo",
            { confirm = false },
        ) {
            reset()
            confirm = false
        }
}

@Composable
internal fun ProviderCatalogDemo(
    s: DemoState,
    send: (DemoAction) -> Unit,
    nav: (SynapseRoute) -> Unit,
) {
    var edit by rememberSaveable { mutableStateOf<String?>(null) }
    var timeout by rememberSaveable { mutableFloatStateOf(60f) }
    DesignPage(
        SynapseRoute.PROVIDERS,
        "AI Providers",
        "Configure and manage AI providers for your agent.",
        nav,
    ) {
        DesignSection(
            "Available Providers",
            SynapseIcon.PROVIDERS,
            "+ Add provider",
            { edit = "Custom Provider" },
        ) {
            providerNames.indices.forEach { i ->
                ProviderRow(i, s, expanded = true) { edit = providerNames[i] }
            }
        }
        DesignSection("Default Provider", SynapseIcon.SETTINGS) {
            DesignPreference(
                "Default provider",
                listOf("Gemini", "OpenAI", "Claude", "Custom"),
                s.provider,
            ) {
                send(DemoAction.Provider(it))
            }
            DesignPreference(
                "Default model",
                listOf(s.model, "gemini-1.5-pro", "gemini-1.5-flash", "gpt-4o-mini"),
                s.model,
            ) {
                send(DemoAction.Model(it))
            }
            DesignPreference(
                "Fallback model",
                listOf("Recovery model", "Same model", "Disabled"),
                s.fallback,
            ) {
                send(DemoAction.Fallback(it))
            }
            Muted("Fallback stays within the approved provider and data scope.")
        }
        DesignSection("Provider preferences", SynapseIcon.SETTINGS, dense = true) {
            DesignSliderPreference(
                "Request timeout",
                "${timeout.toInt()} seconds",
                timeout,
                15f..120f,
            ) {
                timeout = it
            }
            DesignSwitch(
                "Automatic model routing",
                "Demo preference; capability and privacy checks still apply.",
                s.flag("routing"),
            ) {
                send(DemoAction.Toggle("routing", it))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DesignAction("Credentials", Modifier.weight(1f)) { nav(SynapseRoute.CREDENTIALS) }
            DesignAction("Models", Modifier.weight(1f)) { nav(SynapseRoute.MODELS) }
            DesignAction("Test connection", Modifier.weight(1f), primary = true) {
                send(DemoAction.TestConnection)
            }
        }
        if (s.connectionTested)
            DesignNotice(
                "Sample connection verified",
                "Simulated authentication and model discovery succeeded.",
            )
    }
    if (edit != null)
        DesignDialog(
            onDismissRequest = { edit = null },
            title = { Text("Configure ${edit}") },
            text = {
                Text(
                    "Select this sample provider, then manage its demo credential and model. No network connection is made."
                )
            },
            confirmButton = {
                TextButton({
                    send(
                        DemoAction.Provider(
                            when (edit) {
                                "Google Gemini" -> "Gemini"
                                "Anthropic Claude" -> "Claude"
                                "Custom Provider" -> "Custom"
                                else -> "OpenAI"
                            }
                        )
                    )
                    edit = null
                    nav(SynapseRoute.CREDENTIALS)
                }) {
                    Text("Configure credential")
                }
            },
            dismissButton = { TextButton({ edit = null }) { Text("Cancel") } },
        )
}

@Composable
internal fun CredentialCatalogDemo(
    s: DemoState,
    send: (DemoAction) -> Unit,
    nav: (SynapseRoute) -> Unit,
) {
    var add by rememberSaveable { mutableStateOf(false) }
    var alias by rememberSaveable { mutableStateOf("") }
    var remove by rememberSaveable { mutableStateOf(false) }
    var revealed by rememberSaveable { mutableStateOf(false) }
    var targetProvider by rememberSaveable { mutableStateOf(s.provider) }
    DesignPage(
        SynapseRoute.CREDENTIALS,
        "Credentials",
        "Manage API keys and credentials for AI providers.",
        nav,
    ) {
        DesignNotice(
            "Secure Credential Storage",
            "Live keys use Android Keystore. Demo accepts labels only; never enter a real API key.",
        )
        DesignSection(
            "Saved Credentials",
            SynapseIcon.CREDENTIALS,
            "+ Add credential",
            {
                targetProvider = s.provider
                alias = ""
                add = true
            },
        ) {
            providerNames.forEachIndexed { i, name ->
                val configured = providerKey(name) == s.provider && s.credentialStored
                Column(
                    Modifier.fillMaxWidth()
                        .clickable(role = Role.Button) {
                            targetProvider = providerKey(name)
                            alias = ""
                            add = true
                        }
                        .padding(vertical = 6.dp)
                ) {
                    DesignRow(
                        SynapseIcon.PROVIDERS,
                        name,
                        if (configured)
                            if (revealed) "demo-placeholder (not a key)" else "•••• •••• demo"
                        else "Not configured",
                        if (configured) "Configured" else "Add label",
                    )
                    if (configured)
                        Text(
                            "Demo binding · ${if (alias.isBlank()) "Personal demo" else alias}",
                            Modifier.padding(start = 30.dp),
                            fontSize = 8.sp,
                            lineHeight = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DesignAction(
                    if (revealed) "Hide sample" else "Reveal sample",
                    Modifier.weight(1f),
                    enabled = s.credentialStored,
                ) {
                    revealed = !revealed
                }
                DesignAction(
                    "Test connection",
                    Modifier.weight(1f),
                    primary = true,
                    enabled = s.credentialStored,
                ) {
                    send(DemoAction.TestConnection)
                }
            }
            if (s.connectionTested) Muted("Simulated authentication succeeded.")
        }
        DesignSection("Credential Settings", SynapseIcon.SETTINGS) {
            DesignSwitch(
                "Keep credentials on device only",
                "Sample preference; no demo secrets are stored.",
                !s.flag("demoCloudKeys"),
            ) {
                send(DemoAction.Toggle("demoCloudKeys", !it))
            }
            DesignSwitch(
                "Auto-detect provider from key",
                "Illustrative preference only.",
                !s.flag("disableKeyDetection"),
            ) {
                send(DemoAction.Toggle("disableKeyDetection", !it))
            }
            DesignRow(
                SynapseIcon.SECURITY,
                "Encrypt stored credentials",
                "Android Keystore in Live mode",
                "Enabled",
            )
            DesignRow(
                SynapseIcon.DELETE,
                "Clear all credentials",
                "Remove the sample binding",
                onClick = { remove = true },
            )
        }
    }
    if (add)
        DesignDialog(
            onDismissRequest = { add = false },
            title = { Text("Add demo credential") },
            text = {
                Column {
                    Text("Enter a label such as ‘Personal demo’. Do not enter an API key.")
                    OutlinedTextField(
                        alias,
                        { alias = it.take(40) },
                        label = { Text("Demo alias") },
                        isError =
                            alias.isBlank() || alias.startsWith("sk-") || alias.startsWith("AIza"),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    {
                        send(DemoAction.Provider(targetProvider))
                        send(DemoAction.Credential(true))
                        add = false
                    },
                    enabled =
                        alias.isNotBlank() && !alias.startsWith("sk-") && !alias.startsWith("AIza"),
                ) {
                    Text("Save sample")
                }
            },
            dismissButton = { TextButton({ add = false }) { Text("Cancel") } },
        )
    if (remove)
        ConfirmAction(
            "Clear sample credentials?",
            "Live credentials are separate and remain untouched.",
            "Clear samples",
            { remove = false },
        ) {
            send(DemoAction.Credential(false))
            remove = false
        }
}

@Composable
internal fun ModelCatalogDemo(
    s: DemoState,
    send: (DemoAction) -> Unit,
    nav: (SynapseRoute) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var providerFilter by rememberSaveable { mutableStateOf("All providers") }
    var statusFilter by rememberSaveable { mutableStateOf("All status") }
    var customModels by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var customName by rememberSaveable { mutableStateOf("") }
    var enabled by rememberSaveable {
        mutableStateOf(
            listOf(
                "gemini-1.5-pro",
                "gemini-1.5-flash",
                "gpt-4o",
                "gpt-4o-mini",
                "claude-3-5-sonnet",
                "claude-3-haiku",
                "local-llama-3",
            )
        )
    }
    var detail by rememberSaveable { mutableStateOf<String?>(null) }
    val groups =
        listOf(
            "Google Gemini" to listOf("gemini-1.5-pro", "gemini-1.5-flash", "gemini-2.0-flash"),
            "OpenAI" to listOf("gpt-4o", "gpt-4o-mini", "gpt-4-turbo", "gpt-3.5-turbo"),
            "Anthropic Claude" to listOf("claude-3-5-sonnet", "claude-3-haiku", "claude-3-opus"),
            "Custom Models" to (listOf("local-llama-3") + customModels),
        )
    DesignPage(
        SynapseRoute.MODELS,
        "Models",
        "Manage and configure AI models from your providers.",
        nav,
    ) {
        DesignSection(
            "Model Management",
            SynapseIcon.MODELS,
            "+ Add custom model",
            {
                customName = ""
                adding = true
            },
            subtitle = "Browse and configure sample models. Live discovery stays separate.",
            dense = true,
        ) {}
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(Modifier.weight(1f)) {
                DesignSearch(
                    query,
                    { query = it },
                    "Search models (e.g. gemini, gpt, claude)…",
                    dense = true,
                )
            }
            Box(Modifier.width(95.dp)) {
                DesignPreference(
                    "",
                    listOf("All providers") + groups.map { it.first },
                    providerFilter,
                ) {
                    providerFilter = it
                }
            }
            Box(Modifier.width(78.dp)) {
                DesignPreference("", listOf("All status", "Enabled", "Disabled"), statusFilter) {
                    statusFilter = it
                }
            }
        }
        groups.forEach { (provider, models) ->
            val visible =
                models.filter { name ->
                    name.contains(query, true) &&
                        (providerFilter == "All providers" || provider == providerFilter) &&
                        (statusFilter == "All status" ||
                            (name in enabled) == (statusFilter == "Enabled"))
                }
            if (visible.isNotEmpty())
                DesignCatalogSection(provider, SynapseIcon.PROVIDERS, "${visible.size} models") {
                    visible.forEach { name ->
                        DesignCatalogRow(
                            SynapseIcon.MODELS,
                            name,
                            if (name == s.model) "Default model"
                            else
                                when {
                                    name.contains("flash") ||
                                        name.contains("mini") ||
                                        name.contains("haiku") ->
                                        "Fast and efficient model for everyday tasks."
                                    name.contains("pro") || name.contains("4o") ->
                                        "Advanced multimodal model for complex tasks."
                                    name.contains("local") -> "Local sample endpoint."
                                    else -> "Sample text model profile."
                                },
                            if (name in enabled) "Enabled" else "Disabled",
                            action =
                                if (name.contains("gemini") || name.contains("4o")) "Multimodal"
                                else "Text",
                            onClick = { detail = name },
                        )
                    }
                }
        }
        if (
            groups.none { (provider, models) ->
                models.any { name ->
                    name.contains(query, true) &&
                        (providerFilter == "All providers" || provider == providerFilter) &&
                        (statusFilter == "All status" ||
                            (name in enabled) == (statusFilter == "Enabled"))
                }
            }
        )
            DesignNotice("No matching models", "Try another name or provider.")
        DesignPreference(
            "Fallback",
            listOf("Recovery model", "Same model", "Disabled"),
            s.fallback,
        ) {
            send(DemoAction.Fallback(it))
        }
    }
    if (adding) {
        val valid =
            customName.matches(Regex("[A-Za-z0-9][A-Za-z0-9._/-]{1,79}")) &&
                groups.flatMap { it.second }.none { it.equals(customName, true) }
        DesignDialog(
            onDismissRequest = { adding = false },
            title = { Text("Add custom model") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Add a sample model identifier. This does not configure or contact an endpoint."
                    )
                    OutlinedTextField(
                        customName,
                        { customName = it.take(80) },
                        label = { Text("Model identifier") },
                        singleLine = true,
                        isError = customName.isNotEmpty() && !valid,
                    )
                    if (customName.isNotEmpty() && !valid)
                        Text(
                            "Use a unique identifier with letters, digits, dots, slashes, or dashes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                }
            },
            confirmButton = {
                TextButton(
                    {
                        customModels = customModels + customName
                        enabled = enabled + customName
                        providerFilter = "Custom Models"
                        statusFilter = "All status"
                        query = ""
                        adding = false
                    },
                    enabled = valid,
                ) {
                    Text("Add sample model")
                }
            },
            dismissButton = { TextButton({ adding = false }) { Text("Cancel") } },
        )
    }
    if (detail != null)
        DesignDialog(
            onDismissRequest = { detail = null },
            title = { Text(detail!!) },
            text = {
                Text(
                    "Choose the default sample model or toggle its availability. Live discovery remains separate."
                )
            },
            confirmButton = {
                TextButton({
                    if (detail!! !in enabled) enabled = enabled + detail!!
                    send(DemoAction.Model(detail!!))
                    detail = null
                }) {
                    Text("Set default")
                }
            },
            dismissButton = {
                TextButton({
                    val n = detail!!
                    enabled = if (n in enabled) enabled - n else enabled + n
                    detail = null
                }) {
                    Text(if (detail in enabled) "Disable" else "Enable")
                }
            },
        )
}
