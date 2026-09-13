package com.vigilante.shiftsalaryplanner

import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmPresentationTest {
    @Test fun `cancel preview label reflects actual visible alarm count`() {
        assertEquals("Пропустить 2", upcomingCancelLabel(2))
        assertEquals("Пропустить 4", upcomingCancelLabel(4))
    }
}
