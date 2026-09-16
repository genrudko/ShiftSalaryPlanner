package com.vigilante.shiftsalaryplanner.feature.notes.domain

object StyledTextMutations {
    fun replaceRange(
        styledText: StyledText,
        start: Int,
        endExclusive: Int,
        replacement: String,
    ): StyledText {
        val length = styledText.text.length
        val from = minOf(start, endExclusive).coerceIn(0, length)
        val to = maxOf(start, endExclusive).coerceIn(0, length)
        if (from == to && replacement.isEmpty()) return styledText
        if (styledText.text.substring(from, to) == replacement) return styledText

        val oldMarks = MutableList(length) { linkedSetOf<TextMark>() }
        styledText.spans.forEach { span ->
            val spanFrom = span.start.coerceIn(0, length)
            val spanTo = span.endExclusive.coerceIn(0, length)
            for (index in spanFrom until spanTo) oldMarks[index].addAll(span.marks)
        }

        val inheritedMarks: Set<TextMark> = when {
            replacement.isEmpty() -> emptySet()
            from < to && from < length -> oldMarks[from].toSet()
            from > 0 -> oldMarks[from - 1].toSet()
            from < length -> oldMarks[from].toSet()
            else -> emptySet()
        }

        val newText = buildString {
            append(styledText.text, 0, from)
            append(replacement)
            append(styledText.text, to, length)
        }
        val newMarks = ArrayList<Set<TextMark>>(newText.length)
        for (index in 0 until from) newMarks.add(oldMarks[index].toSet())
        repeat(replacement.length) { newMarks.add(inheritedMarks) }
        for (index in to until length) newMarks.add(oldMarks[index].toSet())

        return StyledText(newText, normalizeMarks(newMarks))
    }

    fun replaceText(styledText: StyledText, newText: String): StyledText {
        if (styledText.text == newText) return styledText
        val old = styledText.text
        val prefix = old.indices.zip(newText.indices).takeWhile { (a, b) -> old[a] == newText[b] }.size
        val maxSuffix = minOf(old.length - prefix, newText.length - prefix)
        var suffix = 0
        while (suffix < maxSuffix && old[old.lastIndex - suffix] == newText[newText.lastIndex - suffix]) suffix++
        return replaceRange(
            styledText = styledText,
            start = prefix,
            endExclusive = old.length - suffix,
            replacement = newText.substring(prefix, newText.length - suffix),
        )
    }

    private fun normalizeMarks(marks: List<Set<TextMark>>): List<TextSpan> {
        if (marks.isEmpty()) return emptyList()
        return buildList {
            var runStart = 0
            var runMarks = marks.first()
            for (index in 1..marks.size) {
                val next = if (index < marks.size) marks[index] else emptySet()
                if (index == marks.size || next != runMarks) {
                    if (runMarks.isNotEmpty()) add(TextSpan(runStart, index, runMarks))
                    runStart = index
                    runMarks = next
                }
            }
        }
    }

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
