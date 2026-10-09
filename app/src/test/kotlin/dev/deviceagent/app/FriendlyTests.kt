package dev.deviceagent.app

import dev.deviceagent.core.agent.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FriendlyTests {
    @Test
    fun byteFormattingUsesReadableUnits() {
        assertEquals("0 B", formatBytes(0))
        assertEquals("1.0 KB", formatBytes(1_024))
        assertEquals("1.0 MB", formatBytes(1_048_576))
    }

    @Test
    fun dateFormattingLabelsToday() {
        val now = 1_760_000_000_000L
        assertTrue(formatWhen(now, now).startsWith("Today, "))
    }

    @Test
    fun scheduleIntervalsUseFriendlyChoices() {
        assertEquals("Every hour", intervalLabel(1))
        assertEquals("Every day", intervalLabel(24))
        assertEquals("Every week", intervalLabel(168))
        assertEquals("Every 36 hours", intervalLabel(36))
    }

    @Test
    fun modelSuggestionPrefersProviderAppropriateToolModels() {
        assertEquals(
            "gemini-2.5-flash",
            suggestModel(ProviderChoice.GEMINI, listOf("gemini-2.5-pro", "gemini-2.5-flash", "gemini-2.5-flash-lite", "text-embedding-004")),
        )
        assertEquals(
            "gpt-4o-mini",
            suggestModel(ProviderChoice.OPENAI, listOf("gpt-4o", "gpt-4o-mini", "text-embedding-3-small")),
        )
        assertEquals(
            "claude-3-sonnet",
            suggestModel(ProviderChoice.CLAUDE, listOf("claude-3-opus", "claude-3-sonnet", "claude-3-haiku")),
        )
    }

    @Test
    fun setupChecklistRequiresCredentialsModelAndStorage() {
        assertEquals(listOf(SetupGap.KEY, SetupGap.MODEL, SetupGap.FOLDERS), setupGaps(UiState()))

        val configured = UiState(apiKeyMask = "••••1234", model = "gemini-2.5-flash", useAllFilesAccess = true)
        assertTrue(setupGaps(configured).isEmpty())
        assertFalse(setupGaps(configured.copy(useAllFilesAccess = false)).isEmpty())
    }

    @Test
    fun reportsTranslateProviderFailuresAndAvoidRawStatusNames() {
        assertEquals(
            "Couldn't reach the AI service. Check your internet connection.",
            friendlyReport("FAILED_AFTER_INVESTIGATION\nAI provider problem: NETWORK"),
        )
        assertEquals("Preview ready", statusTitle(TaskStatus.PLAN_ONLY))
        assertEquals("Nothing on your device was changed.", statusExplanation(TaskStatus.PLAN_ONLY))
    }

    @Test
    fun assistantNotesRemoveCommonMarkdownSyntax() {
        assertEquals(
            "Duplicate files\n• archive.zip\n• backup.zip",
            formatAssistantNote("**Duplicate files**\n- `archive.zip`\n- `backup.zip`"),
        )
    }
}
