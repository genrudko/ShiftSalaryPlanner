package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.settings.TodayLayoutSettings
import com.vigilante.shiftsalaryplanner.settings.parseTodayLayoutSettings
import com.vigilante.shiftsalaryplanner.settings.TodayBubbleId
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class M15TodayWeatherLayoutTest {
    @Test fun oldLayoutsDefaultToWeatherVisibleWithoutLosingOtherModules() {
        val legacy = parseTodayLayoutSettings("""[{"id":"NOTES","visible":false}]""").normalized()
        assertTrue(legacy.weatherModuleVisible)
        assertFalse(legacy.bubbles.first { it.id == TodayBubbleId.NOTES }.visible)
    }
    @Test fun changingWeatherModuleVisibilityNeverChangesNotesOrFixedHero() {
        val old = TodayLayoutSettings().normalized()
        val new = old.copy(weatherModuleVisible = false).normalized()
        assertEquals(old.bubbles, new.bubbles)
        assertFalse(new.weatherModuleVisible)
        assertTrue(new.bubbles.none { it.id.isFixedTopModule })
    }
    @Test fun visibilityControlIsContextualAndDoesNotRemoveHero() {
        val root = sequenceOf(File("."),File(".."),File("../..")).first {
            File(it,"app/src/main/java/com/vigilante/shiftsalaryplanner/ui/today/TodayScreen.kt").exists()
        }
        val today = File(root,"app/src/main/java/com/vigilante/shiftsalaryplanner/ui/today/TodayScreen.kt").readText()
        assertTrue(today.contains("normalizedLayout.weatherModuleVisible"))
        assertTrue(today.contains("TodayWeatherHero("))
        assertTrue(today.contains("Подробный прогноз погоды"))
    }
}
