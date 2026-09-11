package com.vigilante.shiftsalaryplanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlarmRuntimeStateTest {
    @Test fun `permission refresh token and reschedule result mutate explicitly`() {
        val state = AlarmRuntimeState()
        assertEquals(0, state.permissionRefreshToken)
        assertNull(state.lastRescheduleResult)

        state.refreshPermissions()
        state.refreshPermissions()
        assertEquals(2, state.permissionRefreshToken)

        val result = ShiftAlarmRescheduleResult(scheduledCount = 3, message = "ok")
        state.recordReschedule(result)
        assertEquals(result, state.lastRescheduleResult)
    }
}
