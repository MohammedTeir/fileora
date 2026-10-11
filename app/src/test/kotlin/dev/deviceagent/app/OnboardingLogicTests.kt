package dev.deviceagent.app

import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingLogicTests {
    @Test fun stepsMoveForwardAndBackWithoutLeavingTheRange() {
        assertEquals(OnboardingStep.SETUP, OnboardingStep.WELCOME.next())
        assertEquals(OnboardingStep.DONE, OnboardingStep.DONE.next())
        assertEquals(OnboardingStep.WELCOME, OnboardingStep.WELCOME.back())
        assertEquals(OnboardingStep.AI, OnboardingStep.DONE.back())
    }

    @Test fun grantedWinsOverEverything() {
        assertEquals(NotificationAccess.GRANTED, notificationAccess(true, 34, true, false))
        assertEquals(NotificationAccess.GRANTED, notificationAccess(true, 30, false, false))
    }

    @Test fun neverAskedThenDeniedOnceThenBlocked() {
        assertEquals(NotificationAccess.NOT_ASKED, notificationAccess(false, 34, false, false))
        assertEquals(NotificationAccess.DENIED, notificationAccess(false, 34, true, true))
        assertEquals(NotificationAccess.BLOCKED, notificationAccess(false, 34, true, false))
    }

    @Test fun beforeAndroid13OnlySettingsCanTurnNotificationsBackOn() {
        assertEquals(NotificationAccess.BLOCKED, notificationAccess(false, 32, false, false))
    }

    @Test fun folderStates() {
        assertEquals(FolderAccess.NONE, folderAccess(0, false, false))
        assertEquals(FolderAccess.CANCELLED, folderAccess(0, false, true))
        assertEquals(FolderAccess.GRANTED, folderAccess(1, false, true))
        assertEquals(FolderAccess.GRANTED, folderAccess(0, true, false))
    }
}
