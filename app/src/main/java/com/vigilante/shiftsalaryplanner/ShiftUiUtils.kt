package com.vigilante.shiftsalaryplanner

import androidx.compose.ui.graphics.Color
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity

fun shiftCellColor(
    shiftCode: String?,
    shiftColors: Map<String, Int>,
    templateMap: Map<String, ShiftTemplateEntity>
): Color {
    if (shiftCode == null) {
        val emptyColor = shiftColors[KEY_EMPTY_DAY]
            ?: defaultShiftColors()[KEY_EMPTY_DAY]
            ?: 0xFFF5F5F5.toInt()
        return Color(emptyColor)
    }

    val templateColor = templateMap[shiftCode]?.colorHex
    val fallback = parseColorHex(templateColor ?: "#E0E0E0", 0xFFE0E0E0.toInt())
    val colorValue = shiftColors[shiftCode]
        ?: fallback
        ?: defaultShiftColors()[shiftCode]
        ?: 0xFFE0E0E0.toInt()

    return Color(colorValue)
}

fun defaultShiftColors(): Map<String, Int> {
    return mapOf(
        "Д" to 0xFFBBDEFB.toInt(),
        "Н" to 0xFFD1C4E9.toInt(),
        "РВД" to 0xFFFFE0B2.toInt(),
        "РВН" to 0xFFFFCDD2.toInt(),
        "8" to 0xFFC8E6C9.toInt(),
        "ОТ" to 0xFFFFF9C4.toInt(),
        "Б" to 0xFFF8BBD0.toInt(),
        "ВЫХ" to 0xFFE0E0E0.toInt(),
        KEY_EMPTY_DAY to 0xFFF5F5F5.toInt()
    )
}

fun shiftEditorPalette(): List<Int> {
    return listOf(
        0xFFBBDEFB.toInt(),
        0xFF90CAF9.toInt(),
        0xFFD1C4E9.toInt(),
        0xFFB39DDB.toInt(),
        0xFFFFE0B2.toInt(),
        0xFFFFCC80.toInt(),
        0xFFFFCDD2.toInt(),
        0xFFEF9A9A.toInt(),
        0xFFC8E6C9.toInt(),
        0xFFA5D6A7.toInt(),
        0xFFFFF9C4.toInt(),
        0xFFFFF59D.toInt(),
        0xFFF8BBD0.toInt(),
        0xFFF48FB1.toInt(),
        0xFFE0E0E0.toInt(),
        0xFFBDBDBD.toInt(),
        0xFFB2DFDB.toInt(),
        0xFF80CBC4.toInt(),
        0xFFFFE082.toInt(),
        0xFFFFCCBC.toInt()
    )
}
fun parseDouble(text: String, fallback: Double): Double {
    return text.replace(',', '.').toDoubleOrNull() ?: fallback
}

fun parseInt(text: String, fallback: Int): Int {
    return text.filter { it.isDigit() }.toIntOrNull() ?: fallback
}

fun normalizeHexColor(input: String): String {
    val cleaned = input.trim().uppercase()
    return when {
        cleaned.matches(Regex("^#[0-9A-F]{6}$")) -> cleaned
        cleaned.matches(Regex("^[0-9A-F]{6}$")) -> "#$cleaned"
        else -> "#E0E0E0"
    }
}

fun parseColorHex(input: String, fallback: Int): Int {
    return try {
        android.graphics.Color.parseColor(normalizeHexColor(input))
    } catch (_: Exception) {
        fallback
    }
}

fun colorIntToHex(colorInt: Int): String {
    return String.format("#%06X", 0xFFFFFF and colorInt)
}

fun hexToHsv(colorHex: String): FloatArray {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(
        parseColorHex(colorHex, 0xFF1E88E5.toInt()),
        hsv
    )
    return hsv
}