@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package dev.deviceagent.app.ui.synapse

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import dev.deviceagent.app.Tone

@Composable
internal fun DemoDeviceRoute(
    route: SynapseRoute,
    s: DemoState,
    send: (DemoAction) -> Unit,
    nav: (SynapseRoute) -> Unit,
) {
    when (route) {
        SynapseRoute.DEVICE -> DeviceOverviewDemo(s,send,nav)
        SynapseRoute.SANDBOX -> SandboxDemo(s, send, nav)
        SynapseRoute.PACKAGES -> PackagesDemo(s, send, nav)
        SynapseRoute.PACKAGE_DETAIL -> PackageDetailDemo(s, send, nav)
        SynapseRoute.TERMINAL -> TerminalDemo(s, send, nav)
        SynapseRoute.APPS -> AppsDemo(send, nav)
        SynapseRoute.TOOLS -> ToolsDemo(nav)
        SynapseRoute.BROWSE,
        SynapseRoute.SEARCH ->
            ScreenPage(
                "SAMPLE FILE CONTEXT",
                route.title,
                "Switch to Live to browse the actual folders you have granted.",
            ) {
                listOf(
                        "Movies/Trips/Lisbon_0327.mp4" to "Video · 142 MiB",
                        "Downloads/ticket-copy.pdf" to "Document · 846 KiB",
                        "Music/Converted/Lesson_01.mp3" to "Audio · 8.2 MiB",
                    )
                    .forEach { (name, detail) ->
                        LinkRow(name, detail, Icons.AutoMirrored.Filled.InsertDriveFile) {
                            send(DemoAction.Attachment(name, true))
                            nav(SynapseRoute.ATTACHMENTS)
                        }
                    }
            }
        else -> Unit
    }
}

@Composable
private fun DeviceOverviewDemo(s: DemoState, send: (DemoAction) -> Unit, nav: (SynapseRoute) -> Unit) {
    DesignPage(SynapseRoute.DEVICE,"Device","Explore your device, manage files, apps, and available tools.",nav) {
        DesignSection("Device information",SynapseIcon.DEVICE,"Refresh",{send(DemoAction.Notice("Demo observations refreshed."))}) {
            Row(horizontalArrangement=Arrangement.spacedBy(14.dp),verticalAlignment=Alignment.CenterVertically) {
                AssetIcon(SynapseIcon.DEVICE,description="Sample Android device",size=66.dp)
                Column(Modifier.weight(1f)) {
                    Text("Samsung Galaxy A04s",style=MaterialTheme.typography.titleSmall)
                    Muted("Android 13 (One UI Core 5.1)")
                    SmallValue("Model","SM-A047F")
                    SmallValue("Security patch","2023-10-01")
                    SmallValue("Root status",s.rootStatus)
                    SmallValue("Storage","64 GB (28.4 GB free)")
                    LinearProgressIndicator(progress={.55f},modifier=Modifier.fillMaxWidth())
                    SmallValue("Memory (RAM)","4 GB (1.8 GB free)")
                }
            }
        }
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) { DesignSection("Battery",SynapseIcon.DEVICE){Text("100%",style=MaterialTheme.typography.titleLarge);Muted("Charging");LinearProgressIndicator(progress={1f},modifier=Modifier.fillMaxWidth(),color=Color(0xFF00B84A));SmallValue("Temperature","34°C");SmallValue("Health","Good")} }
            Column(Modifier.weight(1f)) { DesignSection("Storage",SynapseIcon.MEMORY){Muted("64 GB total");LinearProgressIndicator(progress={.55f},modifier=Modifier.fillMaxWidth());SmallValue("Apps","18.2 GB");SmallValue("Media","12.6 GB");SmallValue("Other","4.8 GB");SmallValue("Free","28.4 GB")} }
        }
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) { DesignSection("Network",SynapseIcon.NETWORK){SmallValue("Wi-Fi","Connected");SmallValue("IP address","192.168.1.105");SmallValue("Mobile data","Off")} }
            Column(Modifier.weight(1f)) { DesignSection("System",SynapseIcon.SETTINGS){SmallValue("CPU","Octa-core 2.0 GHz");SmallValue("Architecture","arm64");SmallValue("Uptime","3 days, 4 hours")} }
        }
        DesignSection("Quick tools",SynapseIcon.TOOLS) {
            Actions {
                Secondary("Open files"){nav(SynapseRoute.SANDBOX)}
                Secondary("Manage apps"){nav(SynapseRoute.APPS)}
                Secondary("Run terminal"){nav(SynapseRoute.TERMINAL)}
                Secondary("Packages"){nav(SynapseRoute.PACKAGES)}
            }
        }
    }
}

@Composable
private fun SandboxDemo(s: DemoState, send: (DemoAction) -> Unit, nav: (SynapseRoute) -> Unit) {
    var location by rememberSaveable { mutableStateOf("Internal storage") }
    var items by rememberSaveable { mutableStateOf(listOf("Alarms","Android","DCIM","Documents","Download","Movies","Music","Notifications","Pictures","Podcasts","Recordings","Ringtones","device_info.txt","log.txt","screenshot.png","video.mp4","archive.zip")) }
    var selected by rememberSaveable { mutableStateOf(listOf<String>()) }
    var query by rememberSaveable { mutableStateOf("") }
    var action by rememberSaveable { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    DesignPage(SynapseRoute.SANDBOX,"Files","Browse and manage your device files and folders. Sample storage only.",nav) {
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Column(Modifier.width(92.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text("Internal storage",style=MaterialTheme.typography.titleSmall)
                LinearProgressIndicator(progress={.55f},modifier=Modifier.fillMaxWidth())
                Text("28.4 GB free",style=MaterialTheme.typography.bodySmall)
                listOf("Internal storage","SD card","Downloads","Pictures","Videos","Audio","Documents","Apps","System (Root)","Recent files","Large files","Screenshots","Trash").forEach { title ->
                    Surface(onClick={location=title;selected=emptyList()},color=if(location==title) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,shape=RoundedCornerShape(6.dp)) {
                        Row(Modifier.heightIn(min=40.dp).padding(4.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)){AssetIcon(SynapseIcon.FOLDER,size=12.dp);Text(title,style=MaterialTheme.typography.labelSmall)}
                    }
                }
            }
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text(location,style=MaterialTheme.typography.titleSmall)
                DesignSearch(query,{query=it},"Find files…")
                TextButton({name="";action="New folder"}){Text("+ New folder")}
                Row { Text("Name",Modifier.weight(1f),style=MaterialTheme.typography.labelSmall);Text("Type / Size",style=MaterialTheme.typography.labelSmall) }
                val visible=items.filter{it.contains(query,true)}
                if(visible.isEmpty()) Muted("No matching files")
                visible.forEach { item ->
                    Row(Modifier.fillMaxWidth().toggleable(item in selected,role=Role.Checkbox){checked->selected=if(checked) selected+item else selected-item}.heightIn(min=36.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                        Checkbox(item in selected,null,Modifier.size(18.dp))
                        AssetIcon(if(item.contains('.')) SynapseIcon.FILE else SynapseIcon.FOLDER,size=14.dp)
                        Text(item,Modifier.weight(1f),style=MaterialTheme.typography.bodySmall,maxLines=1)
                        Text(if(item.contains('.')) item.substringAfterLast('.').uppercase()+" · 12 KB" else "Folder",style=MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        Text("${selected.size} items selected",style=MaterialTheme.typography.bodySmall)
        Actions { listOf("Copy","Move","Rename","Delete").forEach { op -> Secondary(op,selected.isNotEmpty() && (op!="Rename" || selected.size==1)){name=selected.firstOrNull().orEmpty();action=op} } }
        Disclosure("Granted folders","Android SAF access remains separately scoped") { s.folders.forEach { folder -> DesignRow(SynapseIcon.FOLDER,folder,"Sample folder grant") };Secondary("Permissions"){nav(SynapseRoute.PERMISSIONS)} }
    }
    if(action!=null) AlertDialog(onDismissRequest={action=null},title={Text("${action} · Demo")},text={Column{Text("Selected: ${selected.joinToString().ifBlank{"none"}}. No real files are changed.");if(action in listOf("New folder","Rename")) OutlinedTextField(name,{name=it},label={Text("Name")},isError=name.isBlank() || '/' in name)}},confirmButton={TextButton({when(action){"New folder"->items=items+name;"Delete"->items=items-selected.toSet();"Rename"->items=items.map{if(it==selected.first())name else it};else->send(DemoAction.Notice("${action} simulated for ${selected.size} items."))};selected=emptyList();action=null},enabled=action !in listOf("New folder","Rename") || name.isNotBlank() && '/' !in name && name !in items){Text("Confirm")}},dismissButton={TextButton({action=null}){Text("Cancel")}})
}

@Composable
private fun AppsDemo(send: (DemoAction) -> Unit, nav: (SynapseRoute) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All") }
    var detail by rememberSaveable { mutableStateOf<String?>(null) }
    val names=listOf("WhatsApp","YouTube","Chrome","Termux","Synapse","Google Drive","Gallery","Camera","Settings")
    DesignPage(SynapseRoute.APPS,"Apps","View, manage, and inspect installed applications. Demo inventory.",nav) {
        Section(""){InlineStats("Installed apps" to "72","App storage" to "18.2 GB","Updates available" to "3 apps")}
        DesignSearch(query,{query=it},"Search apps, package name, or actions…")
        ChoiceChips(listOf("All","User","System","Updates"),filter){filter=it}
        DesignSection("Installed applications",SynapseIcon.APPS) {
            names.filterIndexed{i,n->n.contains(query,true) && when(filter){"System"->i>=6;"User"->i<6;"Updates"->i in listOf(0,2,3);else->true}}.forEach { title ->
                DesignRow(SynapseIcon.APPS,title,if(title=="Synapse") "dev.deviceagent.app · 24 MB" else "Sample package · 38 MB", "Open",onClick={detail=title})
            }
        }
        DesignSection("App actions",SynapseIcon.TOOLS){Actions{listOf("Install APK","Uninstall","Disable","Check updates").forEach{label->Secondary(label){detail=label}}}}
    }
    DemoDetailDialog(detail,{detail=null},"Sample app details and action preview. Installation, removal, and launch are not performed by this demo.")
}

@Composable
private fun PackagesDemo(s: DemoState, send: (DemoAction) -> Unit, nav: (SynapseRoute) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All") }
    DesignPage(SynapseRoute.PACKAGES,"Packages","Search, install, update, and manage packages in your Linux environment.",nav) {
        Section(""){InlineStats("Installed packages" to "${s.installed.size}","Updates available" to "1","Package storage" to "1.84 GB")}
        DesignSearch(query,{query=it},"Search packages (e.g. python, git, nodejs)…")
        ChoiceChips(listOf("All","Installed","Available"),filter){filter=it}
        val packages=demoPackages.filter{it.name.contains(query,true) && (filter=="All" || (it.name in s.installed)==(filter=="Installed"))}
        if(packages.isEmpty()) Notice("No package matches","Try another name or filter.")
        DesignSection("Packages",SynapseIcon.PACKAGES) {
            packages.forEach { pkg -> DesignRow(SynapseIcon.PACKAGES,pkg.name,"${pkg.purpose}\n${pkg.version} · ${pkg.size}",if(pkg.name in s.installed) "Installed" else "Available",onClick={send(DemoAction.PackageSelected(pkg.name));nav(SynapseRoute.PACKAGE_DETAIL)}) }
        }
        DesignSection("Package actions",SynapseIcon.TOOLS) {
            Actions { Secondary("Install package"){send(DemoAction.PackageSelected("ImageMagick"));nav(SynapseRoute.PACKAGE_DETAIL)};Secondary("Update packages"){send(DemoAction.PackageSelected("FFmpeg"));nav(SynapseRoute.PACKAGE_DETAIL)};Secondary("Check health"){send(DemoAction.Notice("Demo check: Python, Git and Curl pass; FFmpeg needs the MP3 profile."))} }
        }
    }
}

@Composable
private fun PackageDetailDemo(s: DemoState, send: (DemoAction) -> Unit, nav: (SynapseRoute) -> Unit) {
    val pkg=demoPackages.first{it.name==s.packageName}
    var removal by rememberSaveable{mutableStateOf(false)}
    val steps=listOf("Download verified artifact","Check checksum & source","Install in userspace","Probe binary & capability")
    DesignPage(SynapseRoute.PACKAGE_DETAIL,"Package Transaction","Install, update, or remove packages and monitor the process. Demo transaction.",nav) {
        DesignSection(if(s.packageStep<0) "Review package" else if(s.packageStep<4) "Installing package" else "Capability verified",SynapseIcon.DOWNLOAD) {
            DesignRow(SynapseIcon.PACKAGES,pkg.name,"${pkg.version} · ${pkg.purpose}")
            InlineStats("Repository" to "Demo main","Download" to pkg.size,"Destination" to "Userspace")
            LinearProgressIndicator(progress={s.packageStep.coerceAtLeast(0)/4f},modifier=Modifier.fillMaxWidth())
            steps.forEachIndexed { i,title -> DesignRow(if(i<s.packageStep) SynapseIcon.APPROVAL else SynapseIcon.ACTIVITY,title,when {i<s.packageStep->"Verified in demo";i==s.packageStep->"Ready to simulate this step";else->"Waiting for earlier checks"}) }
        }
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)){DesignSection("Dependencies",SynapseIcon.PACKAGES){listOf("libc","openssl","zlib",if(pkg.name=="FFmpeg")"libmp3lame" else "sqlite").forEach{DesignRow(SynapseIcon.PACKAGES,it,"Shared library")}}}
            Column(Modifier.weight(1f)){DesignSection("Package information",SynapseIcon.ABOUT){SmallValue("Name",pkg.name);SmallValue("Version",pkg.version);SmallValue("Size",pkg.size);SmallValue("Architecture","ARM64");Muted("Illustrative signed manifest. No production repository or artifact is contacted.")}}
        }
        DesignSection("Installation log",SynapseIcon.TERMINAL) { CodeBlock("[demo] package: ${pkg.name}\n[demo] source: reviewed manifest\n[demo] step: ${s.packageStep.coerceAtLeast(0)} / 4\n[demo] verification: ${if(s.packageStep==4)"passed" else "pending"}") }
        if(s.packageStep in 0..3) {
            FullButton("Simulate ${steps[s.packageStep].lowercase()}"){send(DemoAction.InstallStep)}
            Secondary("Cancel transaction"){send(DemoAction.CancelInstall)}
        } else Primary(if(pkg.name in s.installed)"Approve update / repair" else "Approve installation"){send(DemoAction.Install)}
        if(pkg.name in s.installed) Secondary("Review removal"){removal=true}
        if(s.packageStep==4 && pkg.name=="FFmpeg") Primary("Return to recovery"){nav(SynapseRoute.RECOVERY)}
    }
    if(removal) ConfirmAction("Remove ${pkg.name}?","Dependent tasks are checked first. Shared libraries remain if another package needs them.","Remove package",{removal=false}){send(DemoAction.RemovePackage(pkg.name));removal=false}
}

@Composable
private fun TerminalDemo(s: DemoState, send: (DemoAction) -> Unit, nav: (SynapseRoute) -> Unit) {
    var session by rememberSaveable{mutableIntStateOf(1)}
    var detail by rememberSaveable{mutableStateOf<String?>(null)}
    DesignPage(SynapseRoute.TERMINAL,"Terminal","Run commands in a secure environment. This terminal is a simulation.",nav) {
        Section(""){InlineStats("Environment" to "Android (Termux)","Sandboxed" to "Isolated","Status" to "Ready")}
        DesignSection("Terminal $session",SynapseIcon.TERMINAL,"+ New session",{session++;send(DemoAction.Command(""))}) {
            CodeBlock("synapse@demo:~ $ ${s.terminalCommand}\n\nOS: Android 13 (One UI Core 5.1)\nHost: Samsung SM-A047F\nKernel: 4.19.191-perf+\nUptime: 3 days, 4 hours\nPackages: 312 (apt)\nShell: bash 5.1.16\nCPU: ARMv8 (8) @ 2.0GHz\nMemory: 1762MiB / 3671MiB\n\n${if(s.terminalComplete)"[demo] Command preview completed. No shell was invoked." else "[demo] Waiting for command preview."}")
            OutlinedTextField(s.terminalCommand,{send(DemoAction.Command(it))},Modifier.fillMaxWidth(),label={Text("Command preview")},singleLine=true)
            Actions { Primary("Run preview",s.terminalCommand.isNotBlank()){send(DemoAction.RunCommand)};Secondary("Clear"){send(DemoAction.Command(""))} }
        }
        DesignSection("Quick commands",SynapseIcon.TOOLS) {
            listOf("List files" to "ls -la","Show device info" to "getprop ro.product.model","Check storage" to "df -h","Show processes" to "ps","Install package" to "pkg install python","Update packages" to "pkg upgrade").chunked(2).forEach{row->Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){row.forEach{(title,cmd)->TextButton({send(DemoAction.Command(cmd))},Modifier.weight(1f)){Text(title,style=MaterialTheme.typography.labelSmall)}}}}
        }
        DesignSection("Recent sessions",SynapseIcon.HISTORY) {
            DesignRow(SynapseIcon.TERMINAL,"System information","neofetch, termux-info","Today",onClick={detail="System information"})
            DesignRow(SynapseIcon.TERMINAL,"Storage check","df -h, du -sh","Yesterday",onClick={detail="Storage check"})
        }
        Secondary("Review execution policy"){nav(SynapseRoute.SECURITY)}
    }
    DemoDetailDialog(detail,{detail=null})
}

@Composable
private fun ToolsDemo(nav: (SynapseRoute) -> Unit) {
    var query by rememberSaveable{mutableStateOf("")}
    var detail by rememberSaveable{mutableStateOf<String?>(null)}
    var enabled by rememberSaveable{mutableStateOf(listOf("Shell","File Operations","Package Manager","App Management","Network","Device Info","Screenshot","Screen Recording","Audio Recorder","Text Processing"))}
    val groups=listOf("Core system tools" to listOf(SynapseIcon.TERMINAL to "Shell",SynapseIcon.FOLDER to "File Operations",SynapseIcon.PACKAGES to "Package Manager",SynapseIcon.APPS to "App Management",SynapseIcon.NETWORK to "Network",SynapseIcon.DEVICE to "Device Info"),"Media tools" to listOf(SynapseIcon.IMAGE to "Screenshot",SynapseIcon.PLAY to "Screen Recording",SynapseIcon.PAUSE to "Audio Recorder",SynapseIcon.RECOVERY to "Media Converter"),"AI and analysis tools" to listOf(SynapseIcon.FILE to "Text Processing",SynapseIcon.IMAGE to "Image Analysis",SynapseIcon.TERMINAL to "Code Analysis",SynapseIcon.PROVIDERS to "Web Search"))
    DesignPage(SynapseRoute.TOOLS,"Tools","Manage available tools for your AI agent and device operations.",nav) {
        Section(""){InlineStats("Available tools" to "14","Enabled tools" to "${enabled.size}","Tool execution" to "Demo only")}
        DesignSearch(query,{query=it},"Search tools, description, or category…")
        groups.forEach{(title,tools)->DesignSection(title,SynapseIcon.TOOLS){tools.filter{it.second.contains(query,true)}.forEach{(icon,name)->DesignRow(icon,name,"v1.0 · ${if(name in enabled)"Enabled" else "Disabled"}",if(name in enabled)"Configure" else "Install",onClick={detail=name})}}}
    }
    if(detail!=null) AlertDialog(onDismissRequest={detail=null},title={Text(detail!!)},text={Text("Change this demo tool’s enabled state. No tool is installed or executed on your device.")},confirmButton={TextButton({val n=detail!!;enabled=if(n in enabled)enabled-n else enabled+n;detail=null}){Text(if(detail in enabled)"Disable sample" else "Enable sample")}},dismissButton={TextButton({detail=null}){Text("Cancel")}})
}
