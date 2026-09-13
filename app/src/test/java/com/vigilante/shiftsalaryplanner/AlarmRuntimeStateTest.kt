package com.vigilante.shiftsalaryplanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test fun `readiness reports only required missing alarm capabilities`() {
        val blocked = buildAlarmReadinessUi(false, false, false, true, true, true)
        assertFalse(blocked.ready)
        assertEquals(listOf("Точные будильники ограничены", "Уведомления не разрешены", "Полноэкранный режим не разрешён"), blocked.issues)

        val ready = buildAlarmReadinessUi(true, true, true, true, true, true)
        assertTrue(ready.ready)
        assertTrue(ready.issues.isEmpty())
    }
}
