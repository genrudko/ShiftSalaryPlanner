package com.vigilante.shiftsalaryplanner

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** Static platform-contract guards; physical-device lifecycle tests remain mandatory. */
class M16IntegrationContractsTest {
    private fun source(path: String) =
        File("src/main/java/com/vigilante/shiftsalaryplanner/$path").readText()

    @Test fun refreshDispatcherCoversAllFiveWidgetProviders() {
        val provider = source("widget/ShiftMonthWidgetProvider.kt")
        for (name in listOf(
            "ShiftMonthWidgetProviderV2::class.java",
            "ShiftMonthClassicWidgetProviderV2::class.java",
            "ShiftMonthMiniWidgetProviderV2::class.java",
            "NotesListWidgetProvider::class.java",
            "TodayNotesWidgetProvider::class.java",
        )) assertTrue("Missing widget refresh: $name", provider.contains(name))
        assertTrue(provider.contains("action = AppWidgetManager.ACTION_APPWIDGET_UPDATE"))
        assertTrue(source("app/MainActivity.kt").contains("ShiftMonthWidgetProviderV2.requestUpdate(context)"))
    }

    @Test fun cloudBackupStreamsFileAndDefersMediaUntilAfterRestore() {
        val drive = source("ui/settings/backup/GoogleDriveBackupSync.kt")
        val app = source("app/MainActivity.kt")
        assertTrue(drive.contains("FileContent(") && drive.contains("application/zip"))
        assertTrue(drive.contains("packageFile.outputStream().use"))
        assertTrue(drive.contains("packageFile.inputStream().use"))
        assertTrue(drive.contains("NotesV2BackupPackageCodec.prepareJsonAndMedia("))
        assertTrue(app.contains("downloaded.pendingMedia.use"))
        assertTrue(app.contains("staged?.install()"))
    }

    @Test fun packageManifestDeclaresReceiversServicesAndProviderPermissions() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        for (name in listOf(
            ".ShiftAlarmReceiver", ".ShiftAlarmSystemReceiver",
            ".ShiftAlarmPlaybackService", ".ShiftAlarmRingActivity",
            ".feature.notes.reminder.NoteReminderReceiver",
            ".weather.WeatherRefreshJobService",
            ".widget.ShiftMonthWidgetProviderV2",
            ".widget.ShiftMonthClassicWidgetProviderV2",
            ".widget.ShiftMonthMiniWidgetProviderV2",
            ".widget.NotesListWidgetProvider",
            ".widget.TodayNotesWidgetProvider",
            ".widget.ShiftMonthWidgetService",
        )) assertTrue("Missing platform component: $name", manifest.contains(name))
        assertTrue(manifest.contains("android.permission.BIND_JOB_SERVICE"))
        assertTrue(manifest.contains("android.permission.BIND_REMOTEVIEWS"))
        assertTrue(manifest.contains("android.permission.RECEIVE_BOOT_COMPLETED"))
    }

    @Test fun weatherBackgroundRemainsOptInAndDoesNotUseExactAlarms() {
        val scheduler = source("weather/WeatherRefreshScheduler.kt")
        assertTrue(scheduler.contains("!policy.backgroundEnabled"))
        assertTrue(scheduler.contains("setPersisted(true)"))
        assertTrue(scheduler.contains("setPeriodic(effectiveInterval)"))
        assertTrue(scheduler.contains("MIN_WEATHER_BACKGROUND_INTERVAL_MILLIS"))
    }
}
