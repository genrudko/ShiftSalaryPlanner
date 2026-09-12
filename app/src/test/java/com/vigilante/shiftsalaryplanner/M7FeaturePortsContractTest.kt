package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.app.ports.ActivityLogPort
import com.vigilante.shiftsalaryplanner.app.ports.NotesDataPort
import com.vigilante.shiftsalaryplanner.app.ports.SettingsDataPort
import com.vigilante.shiftsalaryplanner.patterns.PatternTemplate
import com.vigilante.shiftsalaryplanner.settings.AppEventLogItem
import com.vigilante.shiftsalaryplanner.settings.AppNote
import com.vigilante.shiftsalaryplanner.settings.AppWorkflowSettings
import com.vigilante.shiftsalaryplanner.settings.AssistantAiSettings
import com.vigilante.shiftsalaryplanner.settings.TodayLayoutSettings
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test

class M7FeaturePortsContractTest {
    @Test
    fun notesSettingsAndActivityCanBeUsedWithoutConcreteStores() {
        val notes = FakeNotesDataPort()
        val settings = FakeSettingsDataPort()
        val log = FakeActivityLogPort()
        val note = AppNote(date = "2026-09-12", title = "N")
        val pattern = PatternTemplate(id = "p", name = "P")

        notes.save(note)
        notes.notesForDate(LocalDate.of(2026, 9, 12))
        notes.delete(note.id)
        settings.saveWorkflowSettings(AppWorkflowSettings(quickStartDismissed = true))
        settings.saveAssistantAiSettings(AssistantAiSettings(advancedModeEnabled = true))
        settings.saveTodayLayoutSettings(TodayLayoutSettings())
        settings.upsertPattern(pattern)
        settings.deletePattern("p")
        log.add("Title", "Message", "TEST")
        log.clear()

        assertEquals(listOf("save:${note.id}", "date:2026-09-12", "delete:${note.id}"), notes.calls)
        assertEquals(
            listOf("workflow:true", "assistant:true", "today", "upsert:p", "delete:p"),
            settings.calls
        )
        assertEquals(listOf("add:Title:Message:TEST", "clear"), log.calls)
    }

    private class FakeNotesDataPort : NotesDataPort {
        override val notes: Flow<List<AppNote>> = MutableStateFlow(emptyList())
        val calls = mutableListOf<String>()
        override fun notesForDate(date: LocalDate): List<AppNote> { calls += "date:$date"; return emptyList() }
        override fun save(note: AppNote) { calls += "save:${note.id}" }
        override fun delete(id: String) { calls += "delete:$id" }
    }

    private class FakeSettingsDataPort : SettingsDataPort {
        override val workflowSettings: Flow<AppWorkflowSettings> = MutableStateFlow(AppWorkflowSettings())
        override val assistantAiSettings: Flow<AssistantAiSettings> = MutableStateFlow(AssistantAiSettings())
        override val todayLayoutSettings: Flow<TodayLayoutSettings> = MutableStateFlow(TodayLayoutSettings())
        override val patterns: Flow<List<PatternTemplate>> = MutableStateFlow(emptyList())
        val calls = mutableListOf<String>()
        override fun saveWorkflowSettings(settings: AppWorkflowSettings) { calls += "workflow:${settings.quickStartDismissed}" }
        override fun saveAssistantAiSettings(settings: AssistantAiSettings) { calls += "assistant:${settings.advancedModeEnabled}" }
        override fun saveTodayLayoutSettings(settings: TodayLayoutSettings) { calls += "today" }
        override fun upsertPattern(pattern: PatternTemplate) { calls += "upsert:${pattern.id}" }
        override fun deletePattern(id: String) { calls += "delete:$id" }
    }

    private class FakeActivityLogPort : ActivityLogPort {
        override val events: Flow<List<AppEventLogItem>> = MutableStateFlow(emptyList())
        val calls = mutableListOf<String>()
        override fun add(title: String, message: String, category: String) { calls += "add:$title:$message:$category" }
        override fun clear() { calls += "clear" }
    }
}
