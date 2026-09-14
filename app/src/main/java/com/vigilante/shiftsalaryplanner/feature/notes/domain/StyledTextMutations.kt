package com.vigilante.shiftsalaryplanner.feature.notes.domain

object StyledTextMutations {
    fun toggleMark(styledText: StyledText, start: Int, endExclusive: Int, mark: TextMark): StyledText {
        if (styledText.text.isEmpty()) return styledText
        val from = start.coerceIn(0, styledText.text.length)
        val to = endExclusive.coerceIn(0, styledText.text.length)
        if (from >= to) return styledText
        val marks = MutableList(styledText.text.length) { linkedSetOf<TextMark>() }
        styledText.spans.forEach { span ->
            val spanFrom = span.start.coerceIn(0, styledText.text.length)
            val spanTo = span.endExclusive.coerceIn(0, styledText.text.length)
            for (index in spanFrom until spanTo) marks[index].addAll(span.marks)
        }
        val remove = (from until to).all { mark in marks[it] }
        for (index in from until to) if (remove) marks[index].remove(mark) else marks[index].add(mark)
        val spans = buildList {
            var runStart = 0
            var runMarks: Set<TextMark> = marks.first().toSet()
            for (index in 1..marks.size) {
                val next = if (index < marks.size) marks[index].toSet() else emptySet()
                if (index == marks.size || next != runMarks) {
                    if (runMarks.isNotEmpty()) add(TextSpan(runStart, index, runMarks))
                    runStart = index
                    runMarks = next
                }
            }
        }
        return styledText.copy(spans = spans)
    }
}
