package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.settings.TodayExternalCalendarSettings
import com.vigilante.shiftsalaryplanner.settings.defaultTodayExternalCalendarSettings
import com.vigilante.shiftsalaryplanner.settings.parseEnabledCalendarIds
import com.vigilante.shiftsalaryplanner.settings.serializeEnabledCalendarIds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Focused JVM tests for the M15.1 Task 5 external-calendar layer settings model.
 *
 * Contract under verification:
 * - The external-calendar layer defaults to **disabled** with no enabled calendars.
 * - The persisted settings can be enabled/disabled while retaining the selected
 *   calendar IDs (the layer flag and the filtering set are independent).
 * - Enabled calendar IDs round-trip through the deterministic serializer preserving
 *   the exact filtering set consumed by [ExternalCalendarSource.occurrences].
 *
 * The Android store itself is covered by the file-based structure tests in
 * `M15TodayStructureTest`; this file keeps the pure model logic JVM-executable.
 */
class M15TodayExternalCalendarSettingsTest {

    @Test
    fun `external calendar layer defaults to disabled with no enabled calendars`() {
        val settings = TodayExternalCalendarSettings()
        assertFalse("external calendar layer must default to disabled", settings.layerEnabled)
        assertTrue("default layer must not enable any calendar", settings.enabledCalendarIds.isEmpty())

        val defaulted = defaultTodayExternalCalendarSettings()
        assertFalse("default factory must build a disabled layer", defaulted.layerEnabled)
        assertTrue("default factory must not enable any calendar", defaulted.enabledCalendarIds.isEmpty())
    }

    @Test
    fun `persisted enable and disable keeps filtering independence`() {
        var settings = defaultTodayExternalCalendarSettings()
        assertFalse(settings.layerEnabled)

        settings = settings.copy(layerEnabled = true, enabledCalendarIds = setOf(11L, 7L, 22L))
        assertTrue("enabling the layer must persist the flag", settings.layerEnabled)
        assertEquals(setOf(7L, 11L, 22L), settings.enabledCalendarIds)

        settings = settings.copy(layerEnabled = false)
        assertFalse("disabling the layer must persist the off flag", settings.layerEnabled)
        assertEquals(
            "disabling the layer must not silently drop selected calendars",
            setOf(7L, 11L, 22L),
            settings.enabledCalendarIds
        )
    }

    @Test
    fun `enabled calendar ids round trip preserves the filtering set`() {
        val ids = setOf(3L, 1L, 2L, 3L)
        val serialized = serializeEnabledCalendarIds(ids)
        val restored = parseEnabledCalendarIds(serialized)
        assertEquals("round trip must preserve the exact filtering set", setOf(1L, 2L, 3L), restored)
    }

    @Test
    fun `empty or malformed serialized calendar ids parse to a safe set`() {
        assertEquals(emptySet<Long>(), parseEnabledCalendarIds(""))
        assertEquals(emptySet<Long>(), parseEnabledCalendarIds("   "))
        assertEquals(
            "non-numeric tokens must be ignored",
            setOf(12L, 34L),
            parseEnabledCalendarIds("12,abc,0,34,, -5")
        )
    }

    @Test
    fun `serialization is deterministic and deduplicated`() {
        assertEquals("1,4,9", serializeEnabledCalendarIds(listOf(9L, 4L, 4L, 1L, 9L)))
        assertEquals(
            "serialization must be order-independent",
            serializeEnabledCalendarIds(setOf(9L, 4L, 1L)),
            serializeEnabledCalendarIds(listOf(1L, 9L, 4L))
        )
    }
}
