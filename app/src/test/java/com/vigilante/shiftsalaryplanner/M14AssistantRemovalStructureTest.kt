package com.vigilante.shiftsalaryplanner

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class M14AssistantRemovalStructureTest {
    private fun root(): File = sequenceOf(File("."), File(".."), File("../..")).first {
        File(it, "app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt").exists()
    }

    @Test
    fun phoneNavigationAndSettingsNoLongerExposeAssistant() {
        val root = root()
        val nav = File(root, "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/navigation/AppNavigationModels.kt").readText()
        val main = File(root, "app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt").readText()
        val settingsPort = File(root, "app/src/main/java/com/vigilante/shiftsalaryplanner/app/ports/SettingsDataPort.kt").readText()
        assertFalse(nav.contains("ASSISTANT"))
        assertFalse(main.contains("AssistantTabScreen"))
        assertFalse(main.contains("assistantAiSettings"))
        assertFalse(settingsPort.contains("AssistantAiSettings"))
    }

    @Test
    fun assistantImplementationFilesAreRemoved() {
        val root = root()
        assertFalse(File(root, "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/assistant/AssistantTabScreen.kt").exists())
        assertFalse(File(root, "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/assistant/OpenAiAssistantClient.kt").exists())
        assertFalse(File(root, "app/src/main/java/com/vigilante/shiftsalaryplanner/settings/AssistantAiSettingsStore.kt").exists())
    }

    @Test
    fun wearContractKeepsOperationalCommandsButDropsAssistantPrompt() {
        val root = root()
        val phoneContract = File(root, "app/src/main/java/com/vigilante/shiftsalaryplanner/wearsync/WearSyncContract.kt").readText()
        val listener = File(root, "app/src/main/java/com/vigilante/shiftsalaryplanner/wearsync/WearSyncListenerService.kt").readText()
        val wearContract = File(root, "wear/src/main/java/com/vigilante/shiftsalaryplanner/wear/sync/WearSyncContract.kt").readText()
        val wearMain = File(root, "wear/src/main/java/com/vigilante/shiftsalaryplanner/wear/MainActivity.kt").readText()
        val wearModels = File(root, "wear/src/main/java/com/vigilante/shiftsalaryplanner/wear/sync/WearSnapshotModels.kt").readText()
        assertFalse(phoneContract.contains("PATH_ASSISTANT_PROMPT"))
        assertFalse(wearContract.contains("PATH_ASSISTANT_PROMPT"))
        assertFalse(listener.contains("askAssistantFromWear"))
        assertFalse(wearMain.contains("WearTab.ASSISTANT"))
        assertFalse(wearMain.contains("AssistantScreen"))
        assertFalse(wearModels.contains("WearAssistant"))
        assertTrue(phoneContract.contains("PATH_ADD_NOTE"))
        assertTrue(phoneContract.contains("PATH_TOGGLE_ALL_ALARMS"))
    }
}
