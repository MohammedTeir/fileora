package dev.deviceagent.app.ui.synapse

import org.junit.Assert.*
import org.junit.Test

class SynapseStateTests {
    @Test
    fun planEditsInvalidatePriorConsentAndApproval() {
        val approved = reduceDemo(DemoState(), DemoAction.Approve(1))
        val changes =
            listOf(
                DemoAction.DestinationChanged("Documents/New"),
                DemoAction.IncludeTrash(true),
                DemoAction.SelectDuplicate(duplicateNames.first(), false),
            )
        changes.forEach { change ->
            val edited = reduceDemo(approved.copy(consent = true), change)
            assertNull(edited.approvedRevision)
            assertFalse(edited.consent)
            assertEquals(2, edited.planRevision)
            assertNull(reduceDemo(edited, DemoAction.Approve(1)).approvedRevision)
        }
    }

    @Test
    fun destructiveScopeRequiresExplicitConsent() {
        val review = reduceDemo(DemoState(), DemoAction.IncludeTrash(true))
        assertNull(reduceDemo(review, DemoAction.Approve(review.planRevision)).approvedRevision)
        val approved =
            reduceDemo(
                reduceDemo(review, DemoAction.Consent(true)),
                DemoAction.Approve(review.planRevision),
            )
        assertEquals(645, approved.task.total)
        assertEquals(DemoTaskStatus.RUNNING, approved.task.status)
    }

    @Test
    fun unselectedTrashIsExcludedFromApprovalCount() {
        var s = reduceDemo(DemoState(), DemoAction.IncludeTrash(true))
        duplicateNames.forEach { s = reduceDemo(s, DemoAction.SelectDuplicate(it, false)) }
        val approved = reduceDemo(s, DemoAction.Approve(s.planRevision))
        assertEquals(618, approved.task.total)
        assertNotNull(approved.approvedRevision)
        assertEquals(s, reduceDemo(s, DemoAction.SelectDuplicate("outside-scope.txt", true)))
    }

    @Test
    fun blankDestinationCannotExecute() {
        val s = reduceDemo(DemoState(), DemoAction.DestinationChanged(" "))
        assertNull(reduceDemo(s, DemoAction.Approve(s.planRevision)).approvedRevision)
    }

    @Test
    fun progressCountsRemainConservedAcrossPauseResumeAndCompletion() {
        var s = DemoState(activeTaskId = "SYN-2048")
        fun check() {
            val t = s.task
            assertEquals(t.total, t.verified + t.skipped + t.blocked + t.running + t.queued)
            assertTrue(t.queued >= 0)
        }
        check()
        s = reduceDemo(s, DemoAction.PauseResume)
        assertEquals(DemoTaskStatus.PAUSED, s.task.status)
        assertEquals(s.task, reduceDemo(s, DemoAction.Advance).task)
        check()
        s = reduceDemo(s, DemoAction.PauseResume)
        repeat(600) {
            s = reduceDemo(s, DemoAction.Advance)
            check()
        }
        assertEquals(DemoTaskStatus.PARTIAL, s.task.status)
        assertEquals(494, s.task.verified)
    }

    @Test
    fun completedTaskCannotBeCancelled() {
        val s = reduceDemo(DemoState(), DemoAction.SelectTask("SYN-2044"))
        assertEquals(DemoTaskStatus.VERIFIED, reduceDemo(s, DemoAction.Cancel).task.status)
    }

    @Test
    fun packageCapabilitiesRequireEveryVerificationStep() {
        var s = reduceDemo(DemoState(), DemoAction.PackageSelected("ImageMagick"))
        s = reduceDemo(s, DemoAction.Install)
        repeat(3) {
            s = reduceDemo(s, DemoAction.InstallStep)
            assertFalse("ImageMagick" in s.installed)
        }
        s = reduceDemo(s, DemoAction.InstallStep)
        assertTrue("ImageMagick" in s.installed)
        assertEquals(4, s.packageStep)
        assertEquals(s, reduceDemo(s, DemoAction.InstallStep))
    }

    @Test
    fun cancellingInstallDoesNotMarkPackageInstalled() {
        var s = reduceDemo(DemoState(), DemoAction.PackageSelected("ImageMagick"))
        s = reduceDemo(reduceDemo(s, DemoAction.Install), DemoAction.InstallStep)
        s = reduceDemo(s, DemoAction.CancelInstall)
        assertFalse("ImageMagick" in s.installed)
        assertEquals(-1, s.packageStep)
    }

    @Test
    fun retryNeedsVerifiedPackageAndIsIdempotentAfterSuccess() {
        assertFalse(reduceDemo(DemoState(), DemoAction.RetryRecovery).recoveryVerified)
        var s = reduceDemo(DemoState(), DemoAction.Install)
        repeat(4) { s = reduceDemo(s, DemoAction.InstallStep) }
        s = reduceDemo(s, DemoAction.RetryRecovery)
        assertTrue(s.recoveryVerified)
        assertEquals(1, s.attempts)
        assertEquals(1, reduceDemo(s, DemoAction.RetryRecovery).attempts)
        assertEquals(1, reduceDemo(s, DemoAction.Alternative).attempts)
    }

    @Test
    fun abortedOrExhaustedRecoveryCannotRunAlternative() {
        listOf(DemoState(recoveryAborted = true), DemoState(attempts = 3)).forEach { state ->
            assertFalse(reduceDemo(state, DemoAction.Alternative).recoveryVerified)
            assertFalse(reduceDemo(state, DemoAction.RetryRecovery).recoveryVerified)
        }
    }

    @Test
    fun rootAvailabilityDoesNotGrantRoot() {
        var s = reduceDemo(DemoState(), DemoAction.RootEnabled(true))
        assertTrue(s.rootEnabled)
        s = reduceDemo(s, DemoAction.RootStatus("Denied"))
        assertFalse(s.rootEnabled)
        assertFalse(reduceDemo(s, DemoAction.RootEnabled(true)).rootEnabled)
    }

    @Test
    fun credentialRemovalClearsConnectionState() {
        val s =
            reduceDemo(
                reduceDemo(DemoState(), DemoAction.TestConnection),
                DemoAction.Credential(false),
            )
        assertFalse(s.connectionTested)
        assertFalse(reduceDemo(s, DemoAction.TestConnection).connectionTested)
    }

    @Test
    fun observationsCannotBeEditedAsPreferences() {
        val s = DemoState()
        assertEquals(s, reduceDemo(s, DemoAction.SaveMemory("android", "Android 30", "All tasks")))
        val edited =
            reduceDemo(s, DemoAction.SaveMemory("audio", "Keep original filenames", "Media tasks"))
        assertEquals("Keep original filenames", edited.memories.single { it.id == "audio" }.text)
    }

    @Test
    fun scheduleValidatesTimeAndGoal() {
        val s = DemoState()
        listOf("24:00", "9pm", "12:60", "").forEach { time ->
            assertEquals(
                s.scheduleTime,
                reduceDemo(s, DemoAction.Schedule("goal", time, true)).scheduleTime,
            )
        }
        val edited = reduceDemo(s, DemoAction.Schedule("New goal", "23:45", false))
        assertEquals("23:45", edited.scheduleTime)
        assertFalse(edited.scheduleEnabled)
    }

    @Test
    fun snapshotRestoresEditedWorkflowIncludingSelections() {
        var s = DemoState()
        val actions =
            listOf(
                DemoAction.IncludeTrash(true),
                DemoAction.SelectDuplicate(duplicateNames.first(), false),
                DemoAction.Consent(true),
                DemoAction.Approve(3),
                DemoAction.Advance,
                DemoAction.PauseResume,
                DemoAction.PackageSelected("Python"),
                DemoAction.Install,
                DemoAction.InstallStep,
                DemoAction.Toggle("contents", true),
                DemoAction.SaveMemory("new", "Use Projects", "All tasks"),
                DemoAction.RootEnabled(true),
                DemoAction.Schedule("Daily backup", "02:30", true),
            )
        actions.forEach { s = reduceDemo(s, it) }
        assertEquals(s.copy(notice = null), DemoStateCodec.decode(DemoStateCodec.encode(s)))
    }

    @Test
    fun invalidSnapshotsFallBackSafely() {
        listOf(
                null,
                "{}",
                "broken",
                "x".repeat(64001),
                DemoStateCodec.encode(DemoState())
                    .replace("\"packageStep\":-1", "\"packageStep\":999"),
            )
            .forEach { assertEquals(DemoState(), DemoStateCodec.decode(it)) }
    }

    @Test
    fun sampleHistorySelectsTheCorrespondingTask() {
        val s = reduceDemo(DemoState(), DemoAction.SelectTask("SYN-2046"))
        assertEquals("Build notes app", s.task.title)
        assertEquals(DemoTaskStatus.FAILED, s.task.status)
    }

    @Test
    fun requestBudgetStaysWithinSupportedBounds() {
        assertEquals(20, reduceDemo(DemoState(), DemoAction.Budget(-1)).requestBudget)
        assertEquals(100, reduceDemo(DemoState(), DemoAction.Budget(10000)).requestBudget)
    }
}
