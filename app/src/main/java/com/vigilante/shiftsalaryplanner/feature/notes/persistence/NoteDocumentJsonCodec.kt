package com.vigilante.shiftsalaryplanner.feature.notes.persistence

import com.vigilante.shiftsalaryplanner.feature.notes.domain.ChecklistItem
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteAppearance
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteBlock
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteContext
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteDocument
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteMediaPresentation
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteTextPresentation
import com.vigilante.shiftsalaryplanner.feature.notes.domain.StyledText
import com.vigilante.shiftsalaryplanner.feature.notes.domain.TextMark
import com.vigilante.shiftsalaryplanner.feature.notes.domain.TextSpan
import org.json.JSONArray
import org.json.JSONObject

object NoteDocumentJsonCodec {
    fun encodeList(documents: List<NoteDocument>): JSONArray = JSONArray().apply {
        documents.forEach { put(it.toJson()) }
    }

    fun decodeList(raw: String): List<NoteDocument> = runCatching {
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                if (item.optInt("schemaVersion", -1) != NoteDocument.CURRENT_SCHEMA_VERSION) continue
                runCatching { item.toDocument() }.getOrNull()?.let(::add)
            }
        }
    }.getOrDefault(emptyList())

    private fun NoteDocument.toJson() = JSONObject().apply {
        put("id", id)
        put("schemaVersion", schemaVersion)
        put("title", title)
        put("blocks", blocks.toJsonArray { it.toJson() })
        put("labels", labels.toJsonArray { it })
        put("appearance", appearance.toJson())
        put("context", context.toJson())
        put("pinned", pinned)
        put("archived", archived)
        putNullable("deletedAtMillis", deletedAtMillis)
        putNullable("reminderAtMillis", reminderAtMillis)
        put("createdAtMillis", createdAtMillis)
        put("updatedAtMillis", updatedAtMillis)
    }

    private fun JSONObject.toDocument() = NoteDocument(
        id = getString("id"),
        schemaVersion = getInt("schemaVersion"),
        title = getString("title"),
        blocks = getJSONArray("blocks").mapObjects { it.toBlock() },
        labels = getJSONArray("labels").let { labels -> labels.mapValues { labels.getString(it) }.toSet() },
        appearance = getJSONObject("appearance").toAppearance(),
        context = getJSONObject("context").toContext(),
        pinned = getBoolean("pinned"),
        archived = getBoolean("archived"),
        deletedAtMillis = nullableLong("deletedAtMillis"),
        reminderAtMillis = nullableLong("reminderAtMillis"),
        createdAtMillis = getLong("createdAtMillis"),
        updatedAtMillis = getLong("updatedAtMillis"),
    )

    private fun NoteBlock.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        when (this@toJson) {
            is NoteBlock.Paragraph -> {
                put("type", "paragraph"); put("text", text.toJson()); put("presentation", presentation.toJson())
            }
            is NoteBlock.Heading -> {
                put("type", "heading"); put("level", level); put("text", text.toJson()); put("presentation", presentation.toJson())
            }
            is NoteBlock.Checklist -> {
                put("type", "checklist"); put("items", items.toJsonArray { it.toJson() })
            }
            is NoteBlock.Image -> putMedia("image", uri, label, presentation)
            is NoteBlock.Video -> putMedia("video", uri, label, presentation)
            is NoteBlock.Audio -> {
                putMedia("audio", uri, label, presentation); putNullable("transcript", transcript?.toJson())
            }
            is NoteBlock.FileAttachment -> putMedia("file", uri, label, presentation)
            is NoteBlock.Divider -> put("type", "divider")
        }
    }

    private fun JSONObject.toBlock(): NoteBlock {
        val id = getString("id")
        return when (getString("type")) {
            "paragraph" -> NoteBlock.Paragraph(id, getJSONObject("text").toStyledText(), getJSONObject("presentation").toTextPresentation())
            "heading" -> NoteBlock.Heading(id, getInt("level"), getJSONObject("text").toStyledText(), getJSONObject("presentation").toTextPresentation())
            "checklist" -> NoteBlock.Checklist(id, getJSONArray("items").mapObjects { it.toChecklistItem() })
            "image" -> NoteBlock.Image(id, getString("uri"), getString("label"), getJSONObject("presentation").toMediaPresentation())
            "video" -> NoteBlock.Video(id, getString("uri"), getString("label"), getJSONObject("presentation").toMediaPresentation())
            "audio" -> NoteBlock.Audio(id, getString("uri"), getString("label"), getJSONObject("presentation").toMediaPresentation(), nullableObject("transcript")?.toStyledText())
            "file" -> NoteBlock.FileAttachment(id, getString("uri"), getString("label"), getJSONObject("presentation").toMediaPresentation())
            "divider" -> NoteBlock.Divider(id)
            else -> error("Unsupported note block type")
        }
    }

    private fun StyledText.toJson() = JSONObject().apply {
        put("text", text)
        put("spans", spans.toJsonArray { it.toJson() })
    }

    private fun JSONObject.toStyledText() = StyledText(
        text = getString("text"),
        spans = getJSONArray("spans").mapObjects { span ->
            TextSpan(span.getInt("start"), span.getInt("endExclusive"), span.getJSONArray("marks").mapValues { span.getJSONArray("marks").getString(it) }.map(TextMark::valueOf).toSet())
        },
    )

    private fun TextSpan.toJson() = JSONObject().apply {
        put("start", start); put("endExclusive", endExclusive); put("marks", marks.toJsonArray { it.name })
    }

    private fun ChecklistItem.toJson() = JSONObject().apply {
        put("id", id); put("text", text.toJson()); put("checked", checked); put("indent", indent)
    }

    private fun JSONObject.toChecklistItem() = ChecklistItem(getString("id"), getJSONObject("text").toStyledText(), getBoolean("checked"), getInt("indent"))

    private fun NoteAppearance.toJson() = JSONObject().apply { put("colorHex", colorHex); put("backgroundPattern", backgroundPattern) }
    private fun JSONObject.toAppearance() = NoteAppearance(getString("colorHex"), getString("backgroundPattern"))

    private fun NoteContext.toJson() = JSONObject().apply {
        putNullable("dateIso", dateIso); putNullable("workplaceId", workplaceId); putNullable("shiftCode", shiftCode)
    }
    private fun JSONObject.toContext() = NoteContext(nullableString("dateIso"), nullableString("workplaceId"), nullableString("shiftCode"))

    private fun NoteTextPresentation.toJson() = JSONObject().apply {
        put("textSizeMode", textSizeMode); put("fontMode", fontMode); put("textColorHex", textColorHex)
        put("highlightColorHex", highlightColorHex); put("textAlignMode", textAlignMode); put("listMode", listMode)
        put("bulletListMode", bulletListMode); put("numberListMode", numberListMode)
    }
    private fun JSONObject.toTextPresentation() = NoteTextPresentation(
        getString("textSizeMode"), getString("fontMode"), getString("textColorHex"), getString("highlightColorHex"),
        getString("textAlignMode"), getString("listMode"), getString("bulletListMode"), getString("numberListMode"),
    )

    private fun NoteMediaPresentation.toJson() = JSONObject().apply {
        put("displaySizeMode", displaySizeMode); put("widthFraction", widthFraction.toDouble())
        put("aspectRatio", aspectRatio.toDouble()); put("createdAtMillis", createdAtMillis)
    }
    private fun JSONObject.toMediaPresentation() = NoteMediaPresentation(
        getString("displaySizeMode"), getDouble("widthFraction").toFloat(), getDouble("aspectRatio").toFloat(), getLong("createdAtMillis"),
    )

    private fun JSONObject.putMedia(type: String, uri: String, label: String, presentation: NoteMediaPresentation) {
        put("type", type); put("uri", uri); put("label", label); put("presentation", presentation.toJson())
    }

    private fun JSONObject.putNullable(key: String, value: Any?) { put(key, value ?: JSONObject.NULL) }
    private fun JSONObject.nullableString(key: String): String? = if (isNull(key)) null else getString(key)
    private fun JSONObject.nullableLong(key: String): Long? = if (isNull(key)) null else getLong(key)
    private fun JSONObject.nullableObject(key: String): JSONObject? = if (isNull(key)) null else getJSONObject(key)

    private inline fun <T> Iterable<T>.toJsonArray(transform: (T) -> Any): JSONArray = JSONArray().also { array -> forEach { array.put(transform(it)) } }
    private inline fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> = List(length()) { transform(getJSONObject(it)) }
    private inline fun <T> JSONArray.mapValues(transform: (Int) -> T): List<T> = List(length(), transform)
}
