@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package dev.deviceagent.app.ui.synapse

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier

private val providerNames = listOf("Google Gemini", "OpenAI", "Anthropic Claude", "Custom Provider")
private val providerDescriptions = listOf("Powerful multimodal models from Google.", "GPT models from OpenAI.", "Claude models from Anthropic.", "OpenAI-compatible API endpoint.")

@Composable
internal fun SettingsOverviewDemo(s: DemoState, send: (DemoAction) -> Unit, nav: (SynapseRoute) -> Unit, reset: () -> Unit) {
    var temperature by rememberSaveable { mutableFloatStateOf(.7f) }
    var tokens by rememberSaveable { mutableStateOf("4096") }
    var response by rememberSaveable { mutableStateOf("Balanced") }
    var confirm by rememberSaveable { mutableStateOf(false) }
    DesignPage(SynapseRoute.SETTINGS,"Settings","Configure Synapse, AI providers, permissions, and app preferences.",nav) {
        DesignSection("AI Providers",SynapseIcon.PROVIDERS,"+ Add provider",{nav(SynapseRoute.PROVIDERS)}) {
            providerNames.forEachIndexed { i,name -> DesignRow(SynapseIcon.PROVIDERS,name,providerDescriptions[i],if(name.contains(s.provider))"Active" else "Configure",onClick={nav(SynapseRoute.PROVIDERS)}) }
        }
        DesignSection("Model Settings",SynapseIcon.MODELS) {
            DesignChoice("Default model",listOf(s.model,"gemini-1.5-pro","gpt-4o","claude-3-5-sonnet"),s.model){send(DemoAction.Model(it))}
            SmallValue("Temperature",String.format(java.util.Locale.ROOT,"%.1f",temperature))
            DesignSlider(temperature,{temperature=it},valueRange=0f..1f,label="Temperature")
            OutlinedTextField(tokens,{tokens=it.filter(Char::isDigit)},label={Text("Max output tokens")},singleLine=true,isError=tokens.toIntOrNull() !in 1..32768,modifier=Modifier.fillMaxWidth())
            DesignChoice("Response style",listOf("Balanced","Concise","Detailed"),response){response=it}
        }
        DesignSection("Usage & Limits",SynapseIcon.USAGE,"View detailed usage",{nav(SynapseRoute.USAGE)}) {
            InlineStats("API requests" to "1,250 / 10,000","Input tokens" to "2.4M","Output tokens" to "0.8M")
            LinearProgressIndicator(progress={.125f},modifier=Modifier.fillMaxWidth())
        }
        Secondary("Agent memory"){nav(SynapseRoute.MEMORY)}
        Secondary("Explore setup"){nav(SynapseRoute.WELCOME)}
        Secondary("Reset demo workspace"){confirm=true}
    }
    if(confirm) ConfirmAction("Reset demo workspace?","Sample tasks and preferences return to their initial state. Live settings are separate.","Reset demo",{confirm=false}){reset();confirm=false}
}

@Composable
internal fun ProviderCatalogDemo(s: DemoState, send: (DemoAction) -> Unit, nav: (SynapseRoute) -> Unit) {
    var edit by rememberSaveable { mutableStateOf<String?>(null) }
    var timeout by rememberSaveable { mutableFloatStateOf(60f) }
    DesignPage(SynapseRoute.PROVIDERS,"AI Providers","Configure and manage AI providers for your agent.",nav) {
        DesignSection("Available Providers",SynapseIcon.PROVIDERS,"+ Add provider",{edit="Custom Provider"}) {
            providerNames.forEachIndexed{i,name->DesignRow(SynapseIcon.PROVIDERS,name,providerDescriptions[i],if(name.contains(s.provider))"Selected" else "Configure",onClick={edit=name})}
        }
        DesignSection("Default Provider",SynapseIcon.SETTINGS) {
            DesignChoice("Default provider",listOf("Gemini","OpenAI","Claude","Custom"),s.provider){send(DemoAction.Provider(it))}
            DesignChoice("Default model",listOf(s.model,"gemini-1.5-pro","gemini-1.5-flash","gpt-4o-mini"),s.model){send(DemoAction.Model(it))}
            DesignChoice("Fallback model",listOf("Recovery model","Same model","Disabled"),s.fallback){send(DemoAction.Fallback(it))}
            Muted("Fallback stays within the approved provider and data scope.")
        }
        DesignSection("Provider preferences",SynapseIcon.SETTINGS) {
            SmallValue("Request timeout","${timeout.toInt()} seconds")
            DesignSlider(timeout,{timeout=it},valueRange=15f..120f,label="Request timeout")
            SwitchSetting("Automatic model routing","Demo preference; capability and privacy checks still apply.",s.flag("routing")){send(DemoAction.Toggle("routing",it))}
        }
        Actions { Secondary("Credentials"){nav(SynapseRoute.CREDENTIALS)};Secondary("Models"){nav(SynapseRoute.MODELS)};Primary("Test connection"){send(DemoAction.TestConnection)} }
        if(s.connectionTested) Notice("Sample connection verified","Simulated authentication and model discovery succeeded.")
    }
    if(edit!=null) AlertDialog(onDismissRequest={edit=null},title={Text("Configure ${edit}")},text={Text("Select this sample provider, then manage its demo credential and model. No network connection is made.")},confirmButton={TextButton({send(DemoAction.Provider(when(edit){"Google Gemini"->"Gemini";"Anthropic Claude"->"Claude";"Custom Provider"->"Custom";else->"OpenAI"}));edit=null;nav(SynapseRoute.CREDENTIALS)}){Text("Configure credential")}},dismissButton={TextButton({edit=null}){Text("Cancel")}})
}

@Composable
internal fun CredentialCatalogDemo(s: DemoState, send: (DemoAction) -> Unit, nav: (SynapseRoute) -> Unit) {
    var add by rememberSaveable{mutableStateOf(false)}
    var alias by rememberSaveable{mutableStateOf("")}
    var remove by rememberSaveable{mutableStateOf(false)}
    var revealed by rememberSaveable{mutableStateOf(false)}
    DesignPage(SynapseRoute.CREDENTIALS,"Credentials","Manage API keys and credentials for AI providers. Demo mode accepts aliases only, never real keys.",nav) {
        Notice("Secure Credential Storage","Live keys are encrypted with Android Keystore. The entries below are sample credentials.")
        DesignSection("Saved Credentials",SynapseIcon.CREDENTIALS,"+ Add credential",{add=true}) {
            providerNames.forEachIndexed{i,name->
                DesignRow(SynapseIcon.PROVIDERS,name,if(name.contains(s.provider) && s.credentialStored) if(revealed)"demo-placeholder (not a key)" else "•••• •••• demo" else "Not configured",if(name.contains(s.provider) && s.credentialStored)"Configured" else "Add key",onClick={add=true})
            }
            Actions { Secondary(if(revealed)"Hide sample" else "Reveal sample",s.credentialStored){revealed=!revealed};Primary("Test connection",s.credentialStored){send(DemoAction.TestConnection)} }
            if(s.connectionTested) Muted("Simulated authentication succeeded.")
        }
        DesignSection("Credential Settings",SynapseIcon.SETTINGS) {
            SwitchSetting("Keep credentials on device only","Sample preference; no demo secrets are stored.",!s.flag("demoCloudKeys")){send(DemoAction.Toggle("demoCloudKeys",!it))}
            SwitchSetting("Auto-detect provider from key","Illustrative preference only.",!s.flag("disableKeyDetection")){send(DemoAction.Toggle("disableKeyDetection",!it))}
            DesignRow(SynapseIcon.SECURITY,"Encrypt stored credentials","Android Keystore in Live mode","Enabled")
            DesignRow(SynapseIcon.DELETE,"Clear all credentials","Remove the sample binding",onClick={remove=true})
        }
    }
    if(add) AlertDialog(onDismissRequest={add=false},title={Text("Add demo credential")},text={Column{Text("Enter a label such as ‘Personal demo’. Do not enter an API key.");OutlinedTextField(alias,{alias=it.take(40)},label={Text("Demo alias")},isError=alias.isBlank() || alias.startsWith("sk-") || alias.startsWith("AIza"))}},confirmButton={TextButton({send(DemoAction.Credential(true));add=false},enabled=alias.isNotBlank() && !alias.startsWith("sk-") && !alias.startsWith("AIza")){Text("Save sample")}},dismissButton={TextButton({add=false}){Text("Cancel")}})
    if(remove) ConfirmAction("Clear sample credentials?","Live credentials are separate and remain untouched.","Clear samples",{remove=false}){send(DemoAction.Credential(false));remove=false}
}

@Composable
internal fun ModelCatalogDemo(s: DemoState, send: (DemoAction) -> Unit, nav: (SynapseRoute) -> Unit) {
    var query by rememberSaveable{mutableStateOf("")}
    var enabled by rememberSaveable{mutableStateOf(listOf("gemini-1.5-pro","gemini-1.5-flash","gpt-4o","gpt-4o-mini","claude-3-5-sonnet","claude-3-haiku","local-llama-3"))}
    var detail by rememberSaveable{mutableStateOf<String?>(null)}
    val groups=listOf("Google Gemini" to listOf("gemini-1.5-pro","gemini-1.5-flash","gemini-2.0-flash"),"OpenAI" to listOf("gpt-4o","gpt-4o-mini","gpt-4-turbo","gpt-3.5-turbo"),"Anthropic Claude" to listOf("claude-3-5-sonnet","claude-3-haiku","claude-3-opus"),"Custom Models" to listOf("local-llama-3"))
    DesignPage(SynapseRoute.MODELS,"Models","Manage and configure AI models from your providers. This catalog is illustrative.",nav) {
        DesignSection("Model Management",SynapseIcon.MODELS){Muted("Browse, enable, and configure models. Actual availability is checked in Live mode.")}
        DesignSearch(query,{query=it},"Search models (e.g. gemini, gpt, claude)…")
        groups.forEach{(provider,models)->DesignSection(provider,SynapseIcon.PROVIDERS){models.filter{it.contains(query,true)}.forEach{name->DesignRow(SynapseIcon.MODELS,name,if(name==s.model)"Default model" else "Sample model profile",if(name in enabled)"Enabled" else "Disabled",onClick={detail=name})}}}
        DesignChoice("Fallback",listOf("Recovery model","Same model","Disabled"),s.fallback){send(DemoAction.Fallback(it))}
    }
    if(detail!=null) AlertDialog(onDismissRequest={detail=null},title={Text(detail!!)},text={Text("Choose the default sample model or toggle its availability. Live discovery remains separate.")},confirmButton={TextButton({if(detail!! !in enabled) enabled=enabled+detail!!;send(DemoAction.Model(detail!!));detail=null}){Text("Set default")}},dismissButton={TextButton({val n=detail!!;enabled=if(n in enabled) enabled-n else enabled+n;detail=null}){Text(if(detail in enabled)"Disable" else "Enable")}})
}
