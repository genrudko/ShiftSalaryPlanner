package com.vigilante.shiftsalaryplanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsFeatureStateTest {
    @Test fun `manual holiday new edit and close preserve legacy payload rules`() {
        val state = SettingsFeatureState()
        state.openNewManualHoliday()
        assertTrue(state.showManualHolidayDialog)
        assertNull(state.editingManualHolidayDate)
        state.openManualHoliday("2026-09-12")
        assertTrue(state.showManualHolidayDialog)
        assertEquals("2026-09-12", state.editingManualHolidayDate)
        state.closeManualHoliday()
        assertFalse(state.showManualHolidayDialog)
        assertNull(state.editingManualHolidayDate)
    }

    @Test fun `holiday sync start message and finish retain exact workflow state`() {
        val state = SettingsFeatureState(holidaySyncMessage = "old")
        state.startHolidaySync()
        assertTrue(state.isHolidaySyncing)
        assertEquals("old", state.holidaySyncMessage)
        state.updateHolidaySyncMessage("checking")
        assertEquals("checking", state.holidaySyncMessage)
        state.finishHolidaySync("done")
        assertFalse(state.isHolidaySyncing)
        assertEquals("done", state.holidaySyncMessage)
    }

    @Test fun `workplace rename and custom font status are state only`() {
        val state = SettingsFeatureState()
        state.openWorkplaceRename()
        assertTrue(state.showWorkplaceRenameDialog)
        state.closeWorkplaceRename()
        assertFalse(state.showWorkplaceRenameDialog)
        state.setCustomFontStatus("font ready")
        assertEquals("font ready", state.customFontStatusMessage)
    }

    @Test fun `saveable representation restores settings fields and weather picker target`() {
        val original = SettingsFeatureState()
        original.startHolidaySync()
        original.updateHolidaySyncMessage("syncing")
        original.openManualHoliday("2026-09-12")
        original.openWorkplaceRename()
        original.setCustomFontStatus("font ready")
        original.openWorkplaceWeatherLocation("secondary")
        val restored = restoreSettingsFeatureState(original.toSaveableStrings())
        assertEquals(original.toSaveableStrings(), restored.toSaveableStrings())
        assertEquals(WeatherLocationPickerTargetKind.WORKPLACE, restored.weatherLocationPickerTarget?.kind)
        assertEquals("secondary", restored.weatherLocationPickerTarget?.id)
    }

    @Test fun `weather picker workflow switches targets and clears transient selection`() {
        val state = SettingsFeatureState()
        state.openNewSavedWeatherLocation()
        assertEquals(WeatherLocationPickerTargetKind.SAVED_NEW, state.weatherLocationPickerTarget?.kind)
        assertNull(state.weatherLocationPickerTarget?.id)
        state.openSavedWeatherLocation("saved-4")
        assertEquals(WeatherLocationPickerTargetKind.SAVED_EDIT, state.weatherLocationPickerTarget?.kind)
        assertEquals("saved-4", state.weatherLocationPickerTarget?.id)
        state.openWorkplaceWeatherLocation("main")
        assertEquals(WeatherLocationPickerTargetKind.WORKPLACE, state.weatherLocationPickerTarget?.kind)
        assertEquals("main", state.weatherLocationPickerTarget?.id)
        state.closeWeatherLocationPicker()
        assertNull(state.weatherLocationPickerTarget)
    }
}
