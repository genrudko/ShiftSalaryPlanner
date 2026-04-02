package com.vigilante.shiftsalaryplanner

fun iconGlyph(iconKey: String, fallbackCode: String): String {
    return when {
        iconKey.startsWith("EMOJI:") -> {
            iconKey.removePrefix("EMOJI:").ifBlank { fallbackCode }
        }

        iconKey == "SUN" -> "☀"
        iconKey == "MOON" -> "☾"
        iconKey == "EIGHT" -> "8"
        iconKey == "HOME" -> "⌂"
        iconKey == "OT" -> "ОТ"
        iconKey == "SICK" -> "✚"
        iconKey == "STAR" -> "★"
        iconKey == "TEXT" -> fallbackCode
        else -> fallbackCode
    }
}
fun shiftGlyphFontSize(glyph: String) = when {
    glyph.length <= 2 -> 18
    glyph.length == 3 -> 14
    else -> 12
}
