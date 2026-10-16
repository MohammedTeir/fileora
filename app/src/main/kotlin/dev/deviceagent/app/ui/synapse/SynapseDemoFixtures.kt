package dev.deviceagent.app.ui.synapse

/** Sample records transcribed from the approved v3 references; never used by Live executors. */
internal data class DemoHistoryRecord(
    val id: String,
    val title: String,
    val detail: String,
    val metadata: String,
    val time: String,
    val icon: SynapseIcon,
    val status: DemoTaskStatus,
    val total: Int = 3,
    val verified: Int = if (status == DemoTaskStatus.VERIFIED) total else 0,
)

internal const val videoDemoId = "V3-VIDEO-RUNNING"
internal const val completedVideoDemoId = "V3-VIDEO-COMPLETE"
internal const val videoDemoDescription = "Convert videos to MP4, organize them into folders, and move the results to Downloads."
internal const val videoDemoDestination = "Downloads/Converted Videos"

internal val demoHistoryRecords = listOf(
    DemoHistoryRecord(completedVideoDemoId, "Convert videos to MP4", "3 of 3 files converted successfully", "Created 09:35  ·  Duration 4 min 28 sec  ·  3 files", "09:40", SynapseIcon.PLAY, DemoTaskStatus.VERIFIED),
    DemoHistoryRecord("V3-ORGANIZE", "Organize Downloads folder", "Moved and organized 12 files", "Created 08:10  ·  Duration 2 min 12 sec  ·  12 files", "08:15", SynapseIcon.FOLDER, DemoTaskStatus.VERIFIED, 12),
    DemoHistoryRecord("V3-PROJECT", "Run Python project", "Setup environment and run script", "Created 07:50  ·  Duration 5 min 6 sec", "07:55", SynapseIcon.TERMINAL, DemoTaskStatus.PARTIAL, 6, 4),
    DemoHistoryRecord("V3-INSTALL", "Install app: Termux", "Permission denied", "Created 07:20  ·  Duration 1 min 14 sec", "07:21", SynapseIcon.APPS, DemoTaskStatus.FAILED, 1),
    DemoHistoryRecord("V3-CLEAN", "Clean storage", "Freed 1.2 GB of space", "Created 16:40  ·  Duration 3 min 20 sec", "Yesterday, 16:43", SynapseIcon.IMAGE, DemoTaskStatus.VERIFIED, 12),
    DemoHistoryRecord("V3-DEVICE", "Check device info", "Retrieved system information", "Created 15:12  ·  Duration 1 min 8 sec", "Yesterday, 15:13", SynapseIcon.SETTINGS, DemoTaskStatus.VERIFIED, 1),
    DemoHistoryRecord("V3-UPDATE", "Update system packages", "Cancelled by user", "Created 14:05  ·  Duration 10 sec", "Yesterday, 14:05", SynapseIcon.SECURITY, DemoTaskStatus.CANCELLED, 12),
    DemoHistoryRecord("V3-VIDEO-FAILED", "Convert videos to MP4", "Failed at step 2 of 3 (unsupported format)", "Created 11:28  ·  Duration 2 min 4 sec", "Yesterday, 11:30", SynapseIcon.PLAY, DemoTaskStatus.FAILED, 3, 1),
) + (1..16).map { index ->
    val status = when (index) { 15 -> DemoTaskStatus.FAILED; 16 -> DemoTaskStatus.PLANNED; else -> DemoTaskStatus.VERIFIED }
    DemoHistoryRecord("V3-EARLIER-$index", listOf("Back up photos", "Organize documents", "Check device info", "Convert audio")[index % 4],
        "Sample history record", "Earlier this month · Demo activity", "Sep ${20-index}", SynapseIcon.HISTORY, status)
}

internal val referenceDemoTasks = listOf(
    DemoTask(videoDemoId, "Convert videos to MP4", 3, 1, status = DemoTaskStatus.RUNNING)
) + demoHistoryRecords.map { DemoTask(it.id, it.title, it.total, it.verified,
    blocked = if (it.status in setOf(DemoTaskStatus.FAILED, DemoTaskStatus.PARTIAL)) 1 else 0,
    status = it.status) }

internal data class DemoVideo(val name: String, val resolution: String, val size: String, val duration: String, val originalSize: String)
internal val demoVideos = listOf(
    DemoVideo("video_1", "1280×720", "24.1 MB", "00:02:15", "22.4 MB"),
    DemoVideo("video_2", "1920×1080", "36.8 MB", "00:05:42", "35.1 MB"),
    DemoVideo("video_3", "1280×720", "18.6 MB", "00:01:36", "17.9 MB"),
)
