@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package dev.deviceagent.app.ui.synapse

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.deviceagent.app.Tone

@Composable
internal fun DemoSettingsRoute(
    route: SynapseRoute,
    s: DemoState,
    send: (DemoAction) -> Unit,
    nav: (SynapseRoute) -> Unit,
    appearance: String,
    onAppearance: (String) -> Unit,
    reducedMotion: Boolean,
    onReducedMotion: (Boolean) -> Unit,
    reset: () -> Unit,
) {
    when (route) {
        SynapseRoute.PERMISSIONS -> PermissionsDesign(nav)
        SynapseRoute.APPEARANCE ->
            AppearanceDesign(appearance, onAppearance, reducedMotion, onReducedMotion, nav)
        SynapseRoute.ABOUT -> AboutDesign(nav)
        SynapseRoute.PROVIDERS -> ProviderCatalogDemo(s, send, nav)
        SynapseRoute.CREDENTIALS -> CredentialCatalogDemo(s, send, nav)
        SynapseRoute.MODELS -> ModelCatalogDemo(s, send, nav)
        SynapseRoute.PRIVACY -> PrivacyCatalogDemo(s, send, nav)
        SynapseRoute.USAGE -> UsageCatalogDemo(s, send, nav)
        SynapseRoute.SECURITY -> SecurityCatalogDemo(s, send, nav)
        SynapseRoute.MEMORY -> MemoryCatalogDemo(s, send, nav)
        SynapseRoute.SCHEDULE -> ScheduleDemo(s, send, nav)
        SynapseRoute.SETTINGS -> SettingsOverviewDemo(s, send, nav, reset)
        else -> Unit
    }
}

@Composable
private fun ScheduleDemo(s: DemoState, send: (DemoAction) -> Unit, nav: (SynapseRoute) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All") }
    var edit by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable { mutableIntStateOf(-1) }
    var calendar by rememberSaveable { mutableStateOf(false) }
    var sortByName by rememberSaveable { mutableStateOf(false) }
    var goal by rememberSaveable { mutableStateOf("") }
    var time by rememberSaveable { mutableStateOf("02:00") }
    var titles by rememberSaveable { mutableStateOf(listOf(s.scheduleGoal,"Clean storage","Update system packages","Generate device report")) }
    var times by rememberSaveable { mutableStateOf(listOf(s.scheduleTime,"09:00","03:00","10:00")) }
    var paused by rememberSaveable { mutableStateOf(if(s.scheduleEnabled) emptyList<Int>() else listOf(0)) }
    val indices=titles.indices.filter { i -> titles[i].contains(query,true) && when(filter) {"Active"->i !in paused;"Paused"->i in paused;"One-time"->i==3;"Recurring"->i!=3;else->true} }.sortedBy { if(sortByName) titles[it] else it.toString().padStart(3,'0') }
    fun displayTime(value:String):String { val h=value.substringBefore(':').toIntOrNull() ?: 0;return "${if(h%12==0)12 else h%12}:${value.substringAfter(':')} ${if(h<12)"AM" else "PM"}" }
    fun recurrence(i:Int)=when(i) {0->"Every day at ${displayTime(times[i])}";1->"Every Monday at ${displayTime(times[i])}";2->"Every Sunday at ${displayTime(times[i])}";3->"On the 1st of the month at ${displayTime(times[i])}";else->"Every day at ${displayTime(times[i])}"}
    fun next(i:Int)=when(i){1->"Mon, ${displayTime(times[i])}";2->"Sun, ${displayTime(times[i])}";3->"Nov 1, ${displayTime(times[i])}";else->"Today, ${displayTime(times[i])}"}
    val icons=listOf(SynapseIcon.IMAGE,SynapseIcon.DELETE,SynapseIcon.APPS,SynapseIcon.FILE)
    val details=listOf("Auto backup to Downloads/Backup","Remove temporary files and cache","Check and install available updates","Collect system info and create a report")
    DesignPage(SynapseRoute.SCHEDULE,"Schedules","Set up recurring or one-time tasks. Synapse will run them automatically based on your schedule.",nav) {
        DesignStatCards(Triple(SynapseIcon.SCHEDULES,"Scheduled tasks","${titles.size}"),Triple(SynapseIcon.HISTORY,"Next task",titles.first()))
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {DesignSearch(query,{query=it},"Search schedules…",dense=true)}
            DesignAction("+ Create schedule",primary=true) {editing=-1;goal="";time="02:00";edit=true}
        }
        Row(horizontalArrangement=Arrangement.spacedBy(3.dp)) {
            listOf("All","Active","Paused","One-time","Recurring").forEach { label ->
                val count=when(label){"Active"->titles.size-paused.size;"Paused"->paused.size;"One-time"->1;"Recurring"->titles.size-1;else->titles.size}
                Surface(onClick={filter=label},modifier=Modifier.weight(1f),shape=androidx.compose.foundation.shape.RoundedCornerShape(50),color=if(filter==label) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow) {
                    Text("$label ($count)",Modifier.padding(horizontal=4.dp,vertical=6.dp),fontSize=7.sp,lineHeight=10.sp,color=if(filter==label) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if(indices.isEmpty()) DesignNotice("No matching schedules","Change the search or selected status.")
        DesignSection("Upcoming",action="View calendar",onAction={calendar=true},dense=true) {
            indices.filter {it !in paused}.forEach { i ->
                DesignRow(icons[i%4],titles[i],recurrence(i)+"\n"+details[i%4],onClick={editing=i;goal=titles[i];time=times[i];edit=true},dense=true,
                    trailing={Column(horizontalAlignment=androidx.compose.ui.Alignment.End) {Text("Next run",fontSize=7.sp,lineHeight=10.sp,color=MaterialTheme.colorScheme.primary);Text(next(i),fontSize=8.sp,lineHeight=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}})
            }
        }
        DesignSection("All schedules (${titles.size})",action=if(sortByName)"Sort by name" else "Sort by next run",onAction={sortByName=!sortByName},dense=true) {
            indices.forEach { i ->
                Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp),modifier=Modifier.fillMaxWidth().heightIn(min=39.dp)) {
                    AssetIcon(icons[i%4],size=22.dp)
                    Column(Modifier.weight(1f)) {
                        Text(titles[i],style=MaterialTheme.typography.titleSmall)
                        Text(recurrence(i),fontSize=8.sp,lineHeight=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(details[i%4],fontSize=7.sp,lineHeight=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(Modifier.width(104.dp),horizontalAlignment=androidx.compose.ui.Alignment.End) {
                        DesignSwitch(if(i in paused)"Paused" else "Active","",i !in paused) {enabled -> paused=if(enabled)paused-i else paused+i;if(i==0)send(DemoAction.Schedule(titles[0],times[0],enabled))}
                        Text("Next: ${next(i)}",fontSize=7.sp,lineHeight=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Text("Demo schedules simulate timing; no background job is registered.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
    DemoDetailDialog(if(calendar) "Upcoming schedule calendar" else null,{calendar=false},titles.indices.joinToString("\n") { "${next(it)} · ${titles[it]}" })
    if(edit) DesignDialog(onDismissRequest={edit=false},title={Text(if(editing<0)"Create demo schedule" else "Edit demo schedule")},text={Column {
        OutlinedTextField(goal,{goal=it},label={Text("Task goal")},isError=goal.isBlank())
        OutlinedTextField(time,{time=it},label={Text("Time (HH:mm)")},isError=!Regex("(?:[01][0-9]|2[0-3]):[0-5][0-9]").matches(time))
        Muted("Saved only to the Demo scenario. Live schedules require their own approval.")
    }},confirmButton={TextButton({
        if(editing<0){titles=titles+goal.trim();times=times+time}else{titles=titles.mapIndexed {i,value->if(i==editing)goal.trim() else value};times=times.mapIndexed {i,value->if(i==editing)time else value}}
        if(editing==0)send(DemoAction.Schedule(goal.trim(),time,0 !in paused));edit=false
    },enabled=goal.isNotBlank()&&Regex("(?:[01][0-9]|2[0-3]):[0-5][0-9]").matches(time)){Text("Save schedule")}},dismissButton={TextButton({edit=false}){Text("Cancel")}})
}

@Composable
internal fun TaskResultDesign(s: DemoState, nav: (SynapseRoute) -> Unit) {
    var selected by rememberSaveable { mutableStateOf("Converted files") }
    var preview by rememberSaveable { mutableStateOf<Int?>(null) }
    var upload by rememberSaveable { mutableStateOf(false) }
    val complete = s.task.status == DemoTaskStatus.VERIFIED
    val video = s.task.id.startsWith("V3-VIDEO")
    DesignPage(SynapseRoute.TASK_RESULT,if(complete) "Task completed" else "Task result",
        if (complete) "Synapse has finished executing the plan successfully." else "Review verified outcomes and recorded evidence.",nav,
        footer={ Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            DesignAction("Create a new task  →",Modifier.weight(1f),primary=true) {nav(SynapseRoute.AGENT)}
            DesignAction("Back to Agent",Modifier.weight(1f)) {nav(SynapseRoute.AGENT)}
        } }) {
        DesignSection("Task summary",SynapseIcon.FILE,"View plan",{nav(SynapseRoute.PLAN)},subtitle=if(video) videoDemoDescription else s.task.title) {
            ReferenceMetricCards(Triple(SynapseIcon.FILE,"${s.task.verified} files","Processed"),Triple(SynapseIcon.FOLDER,"1 folder","Created"),Triple(SynapseIcon.SETTINGS,if(complete) "6 steps" else "In progress","${if(complete) "Completed" else "Verification"}"),Triple(SynapseIcon.HISTORY,"4 min 28 sec","Total time"))
        }
        if(!complete) DesignNotice("Work remains","Only ${s.task.verified} of ${s.task.total} items are verified. This task is not complete.",Tone.WARNING)
        DesignSection("Results",SynapseIcon.FOLDER,"Open folder",{nav(SynapseRoute.SANDBOX)},subtitle=if(complete) "All planned actions were completed successfully." else "Only verified outputs are listed.") {
            DesignSegments(listOf("Converted files", "Created folders", "Logs"),selected) {selected=it}
            when(selected) {
                "Converted files" -> {
                    if(s.task.verified==0) Muted("No verified outputs yet.")
                    repeat(s.task.verified.coerceAtMost(3)) { i ->
                        if(video) DemoVideoRow(i) {preview=i}
                        else DesignRow(SynapseIcon.FILE,"Verified output ${i+1}","Demo evidence · source preserved","Verified",onClick={nav(SynapseRoute.AUDIT)})
                    }
                }
                "Created folders" -> DesignRow(SynapseIcon.FOLDER,s.destination,"Approved output destination",onClick={nav(SynapseRoute.SANDBOX)})
                else -> CodeBlock("task: ${s.task.id}\nverified: ${s.task.verified}\nstatus: ${s.task.status}\nsource: demo fixture")
            }
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                DesignSection("Output location",SynapseIcon.FOLDER,dense=true) {
                    Text("/storage/emulated/0/Download/\nConverted Videos",style=MaterialTheme.typography.bodySmall)
                    DesignAction("Open in file manager") {nav(SynapseRoute.SANDBOX)}
                }
            }
            Column(Modifier.weight(1f)) {
                DesignSection("What’s next?",SynapseIcon.TOOLS,subtitle="Try these suggestions:",dense=true) {
                    DesignDetailRow(SynapseIcon.PLAY,"Play the converted videos",onClick={preview=0})
                    DesignDetailRow(SynapseIcon.UPLOAD,"Upload to cloud storage",onClick={upload=true})
                    DesignDetailRow(SynapseIcon.PLUS,"Create a new task",onClick={nav(SynapseRoute.AGENT)})
                    DesignDetailRow(SynapseIcon.HISTORY,"View full activity log",onClick={nav(SynapseRoute.AUDIT)})
                }
            }
        }
    }
    if(preview!=null) DesignDialog(onDismissRequest={preview=null},title={Text("${demoVideos[preview!!].name}_converted.mp4")},text={
        Column {VideoThumbnail(preview!!,Modifier.fillMaxWidth().height(170.dp));Muted("Demo thumbnail preview. The original video file is not stored by this simulation.")}
    },confirmButton={TextButton({preview=null}) {Text("Close")}})
    DemoDetailDialog(if(upload) "Demo upload preview" else null,{upload=false},"The three verified sample outputs would be shared only after provider selection and explicit upload approval. No media is uploaded in Demo mode.")
}
