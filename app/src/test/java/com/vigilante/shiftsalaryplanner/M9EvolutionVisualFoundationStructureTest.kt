package com.vigilante.shiftsalaryplanner

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class M9EvolutionVisualFoundationStructureTest {
    private fun root(): File = sequenceOf(File("."), File("..")).first {
        File(it, "app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt").exists()
    }

    private fun source(path: String): String = File(root(), path).readText()

    @Test fun expressiveFoundationDefinesVariantATokensAndPrimitives() {
        val tokens = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/theme/EvolutionVisualTokens.kt")
        val components = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/common/EvolutionComponents.kt")
        assertTrue(tokens.contains("data class EvolutionColorRoles"))
        assertTrue(tokens.contains("brandPrimary"))
        assertTrue(tokens.contains("financePositive"))
        assertTrue(tokens.contains("appBackground"))
        assertTrue(components.contains("fun EvolutionSurface("))
        assertTrue(components.contains("fun EvolutionIconTile("))
        assertTrue(components.contains("fun EvolutionHeroCard("))
        assertTrue(components.contains("fun EvolutionActionRow("))
        assertTrue(components.contains("fun <T> EvolutionTextTabs("))
        assertTrue(components.contains("fun <T> EvolutionOptionControl("))
        assertTrue(components.contains("fun <T> EvolutionBottomNavigation("))
    }

    @Test fun expressiveFoundationDoesNotDefaultEveryCardToOutline() {
        val components = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/common/EvolutionComponents.kt")
        assertTrue(components.contains("border: BorderStroke? = null"))
        assertFalse(components.contains("border = BorderStroke(1.dp, roles"))
    }

    @Test fun legacyExpressiveSurfaceCanDelegateWithoutChangingClassicContract() {
        val wrapper = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/common/AppExpressiveSurfaces.kt")
        assertTrue(wrapper.contains("AppVisualStyleMode.CLASSIC"))
        assertTrue(wrapper.contains("EvolutionSurface("))
    }
}
