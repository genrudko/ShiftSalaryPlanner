package com.vigilante.shiftsalaryplanner.settings

import android.content.Context
import android.content.Intent
import androidx.core.content.edit
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class AppNote(
    val id: String = UUID.randomUUID().toString(),
    val date: String,
    val workplaceId: String? = null,
    val shiftCode: String? = null,
    val title: String = "",
    val body: String = "",
    val colorHex: String = "#FFF3BF",
    val backgroundPattern: String = "NONE",
    val textSizeMode: String = "NORMAL",
    val fontMode: String = "SANS",
    val textColorHex: String = "",
    val highlightColorHex: String = "",
    val textAlignMode: String = "START",
    val listMode: String = "NONE",
    val bulletListMode: String = "DOT",
    val numberListMode: String = "DECIMAL",
    val bodyBold: Boolean = false,
    val bodyItalic: Boolean = false,
    val bodyHeading: Boolean = false,
    val bodyStrike: Boolean = false,
    val checklist: List<AppNoteChecklistItem> = emptyList(),
    val attachments: List<AppNoteAttachment> = emptyList(),
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

data class AppNoteChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val checked: Boolean = false
)

data class AppNoteAttachment(
    val id: String = UUID.randomUUID().toString(),
    val type: String = "FILE",
    val uri: String = "",
    val label: String = "",
    val displaySizeMode: String = "MEDIUM",
    val widthFraction: Float = 0f,
    val aspectRatio: Float = 0f,
    val createdAtMillis: Long = System.currentTimeMillis()
)

class AppNotesStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.profileSharedPreferences(PREFS_NAME)
    private val _notesFlow = MutableStateFlow(load())

    val notesFlow: Flow<List<AppNote>> = _notesFlow.asStateFlow()

    fun save(note: AppNote) {
        val trimmed = note.copy(
            title = note.title.trim(),
            body = note.body.trim(),
            checklist = note.checklist
                .map { item -> item.copy(text = item.text.trim()) }
                .filter { item -> item.text.isNotBlank() },
            attachments = note.attachments
                .filter { item -> item.uri.isNotBlank() },
            updatedAtMillis = System.currentTimeMillis()
        )
        val next = _notesFlow.value
            .filterNot { it.id == trimmed.id }
            .plus(trimmed)
            .sortedWith(compareByDescending<AppNote> { it.date }.thenByDescending { it.updatedAtMillis })
        persist(next)
    }

    fun delete(id: String) {
        persist(_notesFlow.value.filterNot { it.id == id })
    }

    fun notesForDate(date: LocalDate): List<AppNote> {
        return _notesFlow.value
            .filter { it.date == date.toString() }
            .sortedByDescending { it.updatedAtMillis }
    }

    fun allNotesSnapshot(): List<AppNote> = _notesFlow.value

    private fun persist(notes: List<AppNote>) {
        prefs.edit { putString(KEY_NOTES_JSON, serialize(notes).toString()) }
        _notesFlow.value = notes
        appContext.sendBroadcast(
            Intent(ACTION_APP_NOTES_CHANGED).setPackage(appContext.packageName)
        )
    }

    private fun load(): List<AppNote> {
        return runCatching {
            val array = JSONArray(prefs.getString(KEY_NOTES_JSON, "[]") ?: "[]")
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val date = item.optString("date").takeIf { it.isNotBlank() } ?: continue
                    add(
                        AppNote(
                            id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                            date = date,
                            workplaceId = item.optString("workplaceId").takeIf { it.isNotBlank() },
                            shiftCode = item.optString("shiftCode").takeIf { it.isNotBlank() },
                            title = item.optString("title"),
                            body = item.optString("body"),
                            colorHex = item.optString("colorHex").ifBlank { "#FFF3BF" },
                            backgroundPattern = item.optString("backgroundPattern").ifBlank { "NONE" },
                            textSizeMode = item.optString("textSizeMode").ifBlank { "NORMAL" },
                            fontMode = item.optString("fontMode").ifBlank { "SANS" },
                            textColorHex = item.optString("textColorHex"),
                            highlightColorHex = item.optString("highlightColorHex"),
                            textAlignMode = item.optString("textAlignMode").ifBlank { "START" },
                            listMode = item.optString("listMode").ifBlank { "NONE" },
                            bulletListMode = item.optString("bulletListMode").ifBlank { "DOT" },
                            numberListMode = item.optString("numberListMode").ifBlank { "DECIMAL" },
                            bodyBold = item.optBoolean("bodyBold", false),
                            bodyItalic = item.optBoolean("bodyItalic", false),
                            bodyHeading = item.optBoolean("bodyHeading", false),
                            bodyStrike = item.optBoolean("bodyStrike", false),
                            checklist = parseChecklist(item.optJSONArray("checklist")),
                            attachments = parseAttachments(item.optJSONArray("attachments")),
                            createdAtMillis = item.optLong("createdAtMillis", System.currentTimeMillis()),
                            updatedAtMillis = item.optLong("updatedAtMillis", System.currentTimeMillis())
                        )
                    )
                }
            }.sortedWith(compareByDescending<AppNote> { it.date }.thenByDescending { it.updatedAtMillis })
        }.getOrDefault(emptyList())
    }

    private fun serialize(notes: List<AppNote>): JSONArray {
        val array = JSONArray()
        notes.forEach { note ->
            array.put(
                JSONObject().apply {
                    put("id", note.id)
                    put("date", note.date)
                    put("workplaceId", note.workplaceId.orEmpty())
                    put("shiftCode", note.shiftCode.orEmpty())
                    put("title", note.title)
                    put("body", note.body)
                    put("colorHex", note.colorHex)
                    put("backgroundPattern", note.backgroundPattern)
                    put("textSizeMode", note.textSizeMode)
                    put("fontMode", note.fontMode)
                    put("textColorHex", note.textColorHex)
                    put("highlightColorHex", note.highlightColorHex)
                    put("textAlignMode", note.textAlignMode)
                    put("listMode", note.listMode)
                    put("bulletListMode", note.bulletListMode)
                    put("numberListMode", note.numberListMode)
                    put("bodyBold", note.bodyBold)
                    put("bodyItalic", note.bodyItalic)
                    put("bodyHeading", note.bodyHeading)
                    put("bodyStrike", note.bodyStrike)
                    put("checklist", serializeChecklist(note.checklist))
                    put("attachments", serializeAttachments(note.attachments))
                    put("createdAtMillis", note.createdAtMillis)
                    put("updatedAtMillis", note.updatedAtMillis)
                }
            )
        }
        return array
    }

    private fun parseChecklist(array: JSONArray?): List<AppNoteChecklistItem> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val text = item.optString("text")
                if (text.isBlank()) continue
                add(
                    AppNoteChecklistItem(
                        id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                        text = text,
                        checked = item.optBoolean("checked", false)
                    )
                )
            }
        }
    }

    private fun serializeChecklist(checklist: List<AppNoteChecklistItem>): JSONArray {
        val array = JSONArray()
        checklist.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("text", item.text)
                    put("checked", item.checked)
                }
            )
        }
        return array
    }

    private fun parseAttachments(array: JSONArray?): List<AppNoteAttachment> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val uri = item.optString("uri")
                if (uri.isBlank()) continue
                add(
                    AppNoteAttachment(
                        id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                        type = item.optString("type").ifBlank { "FILE" },
                        uri = uri,
                        label = item.optString("label").ifBlank { "Вложение" },
                        displaySizeMode = item.optString("displaySizeMode").ifBlank { "MEDIUM" },
                        widthFraction = item.optDouble("widthFraction", 0.0).toFloat(),
                        aspectRatio = item.optDouble("aspectRatio", 0.0).toFloat(),
                        createdAtMillis = item.optLong("createdAtMillis", System.currentTimeMillis())
                    )
                )
            }
        }
    }

    private fun serializeAttachments(attachments: List<AppNoteAttachment>): JSONArray {
        val array = JSONArray()
        attachments.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("type", item.type)
                    put("uri", item.uri)
                    put("label", item.label)
                    put("displaySizeMode", item.displaySizeMode)
                    put("widthFraction", item.widthFraction)
                    put("aspectRatio", item.aspectRatio)
                    put("createdAtMillis", item.createdAtMillis)
                }
            )
        }
        return array
    }

    companion object {
        const val PREFS_NAME = "app_notes"
        const val ACTION_APP_NOTES_CHANGED = "com.vigilante.shiftsalaryplanner.notes.ACTION_APP_NOTES_CHANGED"

        private const val KEY_NOTES_JSON = "notes_json"
    }
}
