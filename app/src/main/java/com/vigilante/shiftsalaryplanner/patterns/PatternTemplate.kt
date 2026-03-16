package com.vigilante.shiftsalaryplanner.patterns

import java.util.UUID

data class PatternTemplate(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val steps: List<String> = List(35) { "" }
) {
    fun normalizedSteps(): List<String> {
        return if (steps.size >= 35) {
            steps.take(35)
        } else {
            steps + List(35 - steps.size) { "" }
        }
    }

    fun usedLength(): Int {
        val idx = normalizedSteps().indexOfLast { it.isNotBlank() }
        return if (idx == -1) 0 else idx + 1
    }

    fun previewText(): String {
        return normalizedSteps()
            .take(usedLength())
            .filter { it.isNotBlank() }
            .take(6)
            .joinToString(" / ")
    }
}