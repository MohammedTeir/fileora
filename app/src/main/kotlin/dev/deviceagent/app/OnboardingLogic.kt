package dev.deviceagent.app

/** Pure decisions behind the first-run setup, kept free of Android classes so they can be tested on a plain JVM. */
enum class OnboardingStep {
    WELCOME, SETUP, AI, DONE;

    fun next(): OnboardingStep = entries[minOf(ordinal + 1, entries.lastIndex)]
    fun back(): OnboardingStep = entries[maxOf(ordinal - 1, 0)]
}

/** Where the notification permission stands, which decides the message and the button the person sees. */
enum class NotificationAccess {
    /** Notifications work. */
    GRANTED,
    /** Android 13+ and we have not asked yet. */
    NOT_ASKED,
    /** The person said no once; Android will still show the prompt again. */
    DENIED,
    /** Android will not show the prompt any more (or the person turned notifications off in Settings): only Android Settings can fix it. */
    BLOCKED,
}

fun notificationAccess(enabled: Boolean, sdkInt: Int, alreadyAsked: Boolean, shouldShowRationale: Boolean): NotificationAccess = when {
    enabled -> NotificationAccess.GRANTED
    sdkInt < 33 -> NotificationAccess.BLOCKED // there is no runtime prompt before Android 13
    !alreadyAsked -> NotificationAccess.NOT_ASKED
    shouldShowRationale -> NotificationAccess.DENIED
    else -> NotificationAccess.BLOCKED // asked before and Android no longer offers the prompt: "don't ask again"
}

enum class FolderAccess { GRANTED, CANCELLED, NONE }

/** [pickerCancelled]: the person opened Android's folder picker and backed out without choosing. */
fun folderAccess(folderCount: Int, useAllFilesAccess: Boolean, pickerCancelled: Boolean): FolderAccess = when {
    folderCount > 0 || useAllFilesAccess -> FolderAccess.GRANTED
    pickerCancelled -> FolderAccess.CANCELLED
    else -> FolderAccess.NONE
}
