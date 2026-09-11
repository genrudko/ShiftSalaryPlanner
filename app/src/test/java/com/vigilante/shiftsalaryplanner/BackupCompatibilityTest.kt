package com.vigilante.shiftsalaryplanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCompatibilityTest {
    @Test
    fun recoveredV1Fixture_preservesDayOverridesTemplatePayAndTypedPrefs() {
        val backup = parseAppBackupJson(resourceText("backup/v1-recovered-overrides.json"))

        assertEquals(1, backup.shiftDays.size)
        val day = backup.shiftDays.single()
        assertEquals("2026-06-07", day.date)
        assertEquals("D", day.shiftCode)
        assertEquals("07:45", day.overrideStartTime)
        assertEquals("19:45", day.overrideEndTime)
        assertEquals(12.0, day.overrideTotalHours!!, 0.0)
        assertEquals(0.5, day.overrideBreakHours!!, 0.0)
        assertEquals(1.25, day.overrideNightHours!!, 0.0)
        assertEquals(11.5, day.overridePaidHours!!, 0.0)
        assertEquals(5432.1, day.overrideShiftPayAmount!!, 0.0)
        assertEquals("Recovered override", day.overrideNote)

        val template = backup.shiftTemplates.single()
        assertEquals("D", template.code)
        assertEquals(5000.0, template.shiftPayAmount, 0.0)

        val payrollPrefs = backup.sharedPrefs["payroll_settings"]
        assertNotNull(payrollPrefs)
        assertEquals("Boolean", payrollPrefs!!.getJSONObject("ndfl_enabled").getString("type"))
        assertTrue(payrollPrefs.getJSONObject("ndfl_enabled").getBoolean("value"))
        assertEquals(20, payrollPrefs.getJSONObject("advance_day").getInt("value"))
        assertEquals("Recovered fixture", payrollPrefs.getJSONObject("profile_name").getString("value"))
        val codes = payrollPrefs.getJSONObject("codes").getJSONArray("value")
        assertEquals(listOf("D", "N"), List(codes.length()) { codes.getString(it) })
    }

    @Test
    fun legacyV1Fixture_withoutRecoveredFields_remainsReadableWithDefaults() {
        val backup = parseAppBackupJson(resourceText("backup/v1-legacy-minimal.json"))

        val day = backup.shiftDays.single()
        assertEquals("2026-05-01", day.date)
        assertEquals("N", day.shiftCode)
        assertNull(day.overrideStartTime)
        assertNull(day.overrideEndTime)
        assertNull(day.overrideTotalHours)
        assertNull(day.overrideBreakHours)
        assertNull(day.overrideNightHours)
        assertNull(day.overridePaidHours)
        assertNull(day.overrideShiftPayAmount)
        assertNull(day.overrideNote)

        val template = backup.shiftTemplates.single()
        assertEquals("N", template.code)
        assertEquals(0.0, template.shiftPayAmount, 0.0)
        assertTrue(template.active)
    }

    private fun resourceText(path: String): String {
        val url = requireNotNull(javaClass.classLoader?.getResource(path)) { "Missing fixture: $path" }
        return url.readText(Charsets.UTF_8)
    }
}
