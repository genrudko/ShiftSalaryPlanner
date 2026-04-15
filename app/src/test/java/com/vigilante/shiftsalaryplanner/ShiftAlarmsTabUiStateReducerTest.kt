package com.vigilante.shiftsalaryplanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShiftAlarmsTabUiStateReducerTest {

    @Test
    fun `set enabled updates flag`() {
        val initial = ShiftAlarmsTabUiState(
            enabled = false,
            autoReschedule = true,
            scheduleHorizonDaysText = "90",
            templateConfigs = emptyList()
        )

        val updated = reduceShiftAlarmsTabUiState(
            state = initial,
            action = ShiftAlarmsTabUiAction.SetEnabled(true)
        )

        assertTrue(updated.enabled)
    }

    @Test
    fun `start and stop editing toggles dialog state`() {
        val initial = ShiftAlarmsTabUiState(
            enabled = true,
            autoReschedule = true,
            scheduleHorizonDaysText = "90",
            templateConfigs = listOf(
                ShiftTemplateAlarmConfig(shiftCode = "D")
            )
        )
        val alarm = ShiftAlarmConfig(title = "Test")

        val editing = reduceShiftAlarmsTabUiState(
            state = initial,
            action = ShiftAlarmsTabUiAction.StartEditing(
                templateCode = "D",
                alarm = alarm
            )
        )
        assertTrue(editing.showAlarmDialog)
        assertEquals("D", editing.editingTemplateCode)
        assertEquals(alarm.id, editing.editingAlarm?.id)

        val closed = reduceShiftAlarmsTabUiState(
            state = editing,
            action = ShiftAlarmsTabUiAction.StopEditing
        )
        assertFalse(closed.showAlarmDialog)
        assertNull(closed.editingTemplateCode)
        assertNull(closed.editingAlarm)
    }
}
