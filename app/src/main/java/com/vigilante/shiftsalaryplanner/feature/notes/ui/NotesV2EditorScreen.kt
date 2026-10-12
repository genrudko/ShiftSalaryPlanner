package com.vigilante.shiftsalaryplanner.feature.notes.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatItalic
import androidx.compose.material.icons.rounded.FormatStrikethrough
import androidx.compose.material.icons.rounded.FormatUnderlined
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.AppExpressiveSurface
import com.vigilante.shiftsalaryplanner.AppExpressiveSurfaceTone
import com.vigilante.shiftsalaryplanner.appBlockSpacing
import com.vigilante.shiftsalaryplanner.appCardPadding
import com.vigilante.shiftsalaryplanner.appCornerRadius
import com.vigilante.shiftsalaryplanner.appHapticAction
import com.vigilante.shiftsalaryplanner.appListSecondaryTextColor
import com.vigilante.shiftsalaryplanner.appPanelBorderColor
import com.vigilante.shiftsalaryplanner.appScaledSpacing
import com.vigilante.shiftsalaryplanner.appScreenPadding
import com.vigilante.shiftsalaryplanner.feature.notes.domain.ChecklistItem
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteAppearance
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteBlock
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteDocument
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteDocumentMutations
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteEditorHistory
import com.vigilante.shiftsalaryplanner.feature.notes.domain.StyledText
import com.vigilante.shiftsalaryplanner.feature.notes.domain.StyledTextMutations
import com.vigilante.shiftsalaryplanner.feature.notes.domain.TextMark
import java.util.UUID

@Composable
fun NotesV2EditorScreen(
    initialDocument: NoteDocument,
    onBack: () -> Unit,
    onSave: (NoteDocument) -> Unit,
    onDelete: (String) -> Unit,
    onRequestImage: () -> Unit,
    onRequestVideo: () -> Unit,
    onRequestAudio: () -> Unit,
    onRequestFile: () -> Unit,
    onShare: (NoteDocument) -> Unit = {},
    onExport: (NoteDocument) -> Unit = {},
    onRequestReminder: () -> Unit = {},
    onClearReminder: () -> Unit = {},
    modifier: Modifier = Modifier,
    onDocumentChange: (NoteDocument) -> Unit = {},
    isRecordingAudio: Boolean = false,
    mediaBlockContent: @Composable (NoteBlock) -> Unit = { block -> DefaultNoteMediaBlock(block) },
) {
    var history by remember(initialDocument.id) { mutableStateOf(NoteEditorHistory.start(initialDocument)) }
    var activeBlockId by rememberSaveable(initialDocument.id) {
        mutableStateOf(initialDocument.blocks.firstOrNull { it is NoteBlock.Paragraph || it is NoteBlock.Heading }?.id)
    }
    var selectionStart by rememberSaveable(initialDocument.id) { mutableIntStateOf(0) }
    var selectionEnd by rememberSaveable(initialDocument.id) { mutableIntStateOf(0) }
    var labelDraft by rememberSaveable(initialDocument.id) { mutableStateOf("") }

    val document = history.current
    val now: () -> Long = { System.currentTimeMillis() }

    // Android media pickers/recorder live outside this renderer. External document updates
    // enter the same history so imported media remains undoable and does not fork state.
    LaunchedEffect(initialDocument) {
        if (initialDocument != history.current) {
            history = NoteEditorHistory.commit(history, initialDocument)
        }
    }

    fun commit(next: NoteDocument) {
        val updated = NoteEditorHistory.commit(history, next)
        if (updated != history) {
            history = updated
            onDocumentChange(updated.current)
        }
    }

    fun undo() {
        val updated = NoteEditorHistory.undo(history)
        if (updated != history) {
            history = updated
            onDocumentChange(updated.current)
        }
    }

    fun redo() {
        val updated = NoteEditorHistory.redo(history)
        if (updated != history) {
            history = updated
            onDocumentChange(updated.current)
        }
    }

    fun toggleMark(mark: TextMark) {
        val blockId = activeBlockId ?: return
        val block = document.blocks.firstOrNull { it.id == blockId } ?: return
        val styled = when (block) {
            is NoteBlock.Paragraph -> block.text
            is NoteBlock.Heading -> block.text
            else -> return
        }
        if (styled.text.isEmpty()) return
        val rawFrom = minOf(selectionStart, selectionEnd).coerceIn(0, styled.text.length)
        val rawTo = maxOf(selectionStart, selectionEnd).coerceIn(0, styled.text.length)
        val from = if (rawFrom == rawTo) 0 else rawFrom
        val to = if (rawFrom == rawTo) styled.text.length else rawTo
        val changed = StyledTextMutations.toggleMark(styled, from, to, mark)
        commit(NoteDocumentMutations.replaceBlockText(document, blockId, changed, now()))
    }

    val isDark = isSystemInDarkTheme()
    val sourceColor = remember(document.appearance.colorHex) { parseEditorColor(document.appearance.colorHex) }
    val noteColor = if (isDark) lerp(MaterialTheme.colorScheme.surface, sourceColor, 0.20f)
    else lerp(MaterialTheme.colorScheme.surface, sourceColor, 0.70f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(appScreenPadding()),
        verticalArrangement = Arrangement.spacedBy(appBlockSpacing()),
    ) {
        EditorHeader(
            document = document,
            canUndo = history.canUndo,
            canRedo = history.canRedo,
            onBack = onBack,
            onUndo = ::undo,
            onRedo = ::redo,
            onTogglePin = { commit(NoteDocumentMutations.setPinned(document, !document.pinned, now())) },
            onDelete = { onDelete(document.id) },
            onShare = { onShare(document) },
            onExport = { onExport(document) },
            onSave = { onSave(document) },
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(appCornerRadius(26.dp)),
            color = noteColor,
            border = BorderStroke(1.dp, appPanelBorderColor().copy(alpha = 0.55f)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .editorPattern(document.appearance.backgroundPattern, noteColor)
                    .padding(appCardPadding()),
                verticalArrangement = Arrangement.spacedBy(appScaledSpacing(12.dp)),
            ) {
                OutlinedTextField(
                    value = document.title,
                    onValueChange = { commit(NoteDocumentMutations.setTitle(document, it, now())) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Заголовок") },
                    singleLine = false,
                    textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                )

                EditorContext(document, onRequestReminder, onClearReminder)

                if (document.blocks.isEmpty()) {
                    AppExpressiveSurface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(appCornerRadius(20.dp)),
                        tone = AppExpressiveSurfaceTone.SOFT,
                        border = BorderStroke(1.dp, appPanelBorderColor().copy(alpha = 0.45f)),
                    ) {
                        Column(
                            modifier = Modifier.padding(appCardPadding()),
                            verticalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp)),
                        ) {
                            Text("Начните с текста, списка или вложения", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "В одной заметке можно смешивать текст, чек-листы, фото, видео, аудио и файлы.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = appListSecondaryTextColor(),
                            )
                            TextButton(onClick = appHapticAction {
                                val id = UUID.randomUUID().toString()
                                activeBlockId = id
                                commit(NoteDocumentMutations.insertBlock(document, NoteBlock.Paragraph(id, StyledText("")), null, now()))
                            }) {
                                Icon(Icons.Rounded.Add, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Начать текст")
                            }
                        }
                    }
                } else {
                    document.blocks.forEachIndexed { index, block ->
                        NoteBlockEditor(
                            block = block,
                            blockIndex = index,
                            blockCount = document.blocks.size,
                            activeBlockId = activeBlockId,
                            onActivate = { id, start, end ->
                                activeBlockId = id
                                selectionStart = start
                                selectionEnd = end
                            },
                            onReplaceText = { id, styled ->
                                commit(NoteDocumentMutations.replaceBlockText(document, id, styled, now()))
                            },
                            onToggleChecklist = { blockId, itemId ->
                                commit(NoteDocumentMutations.toggleChecklistItem(document, blockId, itemId, now()))
                            },
                            onChecklistText = { blockId, itemId, text ->
                                commit(NoteDocumentMutations.updateChecklistItemText(document, blockId, itemId, text, now()))
                            },
                            onAddChecklistItem = { blockId ->
                                commit(NoteDocumentMutations.appendChecklistItem(
                                    document,
                                    blockId,
                                    ChecklistItem(UUID.randomUUID().toString(), StyledText(""), checked = false),
                                    now(),
                                ))
                            },
                            onRemoveChecklistItem = { blockId, itemId ->
                                commit(NoteDocumentMutations.removeChecklistItem(document, blockId, itemId, now()))
                            },
                            onMoveUp = {
                                commit(NoteDocumentMutations.moveBlock(document, block.id, index - 1, now()))
                            },
                            onMoveDown = {
                                commit(NoteDocumentMutations.moveBlock(document, block.id, index + 1, now()))
                            },
                            onRemoveBlock = {
                                commit(NoteDocumentMutations.removeBlock(document, block.id, now()))
                                if (activeBlockId == block.id) activeBlockId = null
                            },
                            mediaBlockContent = mediaBlockContent,
                        )
                    }
                }

                EditorFormattingBar(
                    enabled = activeBlockId?.let { id -> document.blocks.any { it.id == id && (it is NoteBlock.Paragraph || it is NoteBlock.Heading) } } == true,
                    onBold = { toggleMark(TextMark.BOLD) },
                    onItalic = { toggleMark(TextMark.ITALIC) },
                    onUnderline = { toggleMark(TextMark.UNDERLINE) },
                    onStrike = { toggleMark(TextMark.STRIKETHROUGH) },
                    onToggleHeading = {
                        val id = activeBlockId ?: return@EditorFormattingBar
                        val current = document.blocks.firstOrNull { it.id == id } ?: return@EditorFormattingBar
                        val replacement = when (current) {
                            is NoteBlock.Paragraph -> NoteBlock.Heading(current.id, level = 2, text = current.text, presentation = current.presentation)
                            is NoteBlock.Heading -> NoteBlock.Paragraph(current.id, text = current.text, presentation = current.presentation)
                            else -> return@EditorFormattingBar
                        }
                        commit(NoteDocumentMutations.replaceBlock(document, replacement, now()))
                    },
                )

                EditorInsertBar(
                    onAddText = {
                        val id = UUID.randomUUID().toString()
                        val after = activeBlockId ?: document.blocks.lastOrNull()?.id
                        activeBlockId = id
                        commit(NoteDocumentMutations.insertBlock(document, NoteBlock.Paragraph(id, StyledText("")), after, now()))
                    },
                    onAddChecklist = {
                        val id = UUID.randomUUID().toString()
                        val after = activeBlockId ?: document.blocks.lastOrNull()?.id
                        commit(NoteDocumentMutations.insertBlock(
                            document,
                            NoteBlock.Checklist(id, listOf(ChecklistItem(UUID.randomUUID().toString(), StyledText(""), false))),
                            after,
                            now(),
                        ))
                    },
                    onImage = onRequestImage,
                    onVideo = onRequestVideo,
                    onAudio = onRequestAudio,
                    onFile = onRequestFile,
                    isRecordingAudio = isRecordingAudio,
                )
            }
        }

        EditorAppearance(
            document = document,
            labelDraft = labelDraft,
            onLabelDraftChange = { labelDraft = it },
            onAddLabel = {
                val label = labelDraft.trim().removePrefix("#")
                if (label.isNotBlank()) {
                    commit(NoteDocumentMutations.setLabels(document, document.labels + label, now()))
                    labelDraft = ""
                }
            },
            onRemoveLabel = { label -> commit(NoteDocumentMutations.setLabels(document, document.labels - label, now())) },
            onAppearance = { appearance -> commit(NoteDocumentMutations.setAppearance(document, appearance, now())) },
        )

        Spacer(Modifier.height(appScaledSpacing(80.dp)))
    }
}

@Composable
private fun EditorHeader(
    document: NoteDocument,
    canUndo: Boolean,
    canRedo: Boolean,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onExport: () -> Unit,
    onSave: () -> Unit,
) {
    var overflowExpanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = appHapticAction(onAction = onBack)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (document.title.isBlank()) "Новая заметка" else "Редактирование",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text("Изменения сохраняются в документе v2", style = MaterialTheme.typography.labelMedium, color = appListSecondaryTextColor())
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(4.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = appHapticAction(onAction = onUndo), enabled = canUndo) { Icon(Icons.AutoMirrored.Rounded.Undo, "Отменить") }
            IconButton(onClick = appHapticAction(onAction = onRedo), enabled = canRedo) { Icon(Icons.AutoMirrored.Rounded.Redo, "Повторить") }
            IconButton(
                onClick = appHapticAction(onAction = onTogglePin),
                modifier = Modifier.semantics { selected = document.pinned },
            ) {
                Icon(Icons.Rounded.PushPin, if (document.pinned) "Открепить заметку" else "Закрепить заметку", tint = if (document.pinned) MaterialTheme.colorScheme.primary else appListSecondaryTextColor())
            }
            Box {
                IconButton(onClick = appHapticAction { overflowExpanded = true }) {
                    Icon(Icons.Rounded.MoreVert, "Ещё")
                }
                DropdownMenu(
                    expanded = overflowExpanded,
                    onDismissRequest = { overflowExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Поделиться") },
                        onClick = { overflowExpanded = false; onShare() },
                    )
                    DropdownMenuItem(
                        text = { Text("Экспорт JSON") },
                        onClick = { overflowExpanded = false; onExport() },
                    )
                    DropdownMenuItem(
                        text = { Text("Удалить") },
                        onClick = { overflowExpanded = false; onDelete() },
                    )
                }
            }
            Surface(
                modifier = Modifier.clickable(onClick = appHapticAction(onAction = onSave)),
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = appScaledSpacing(14.dp), vertical = appScaledSpacing(9.dp)),
                    horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Сохранить", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EditorContext(
    document: NoteDocument,
    onRequestReminder: () -> Unit,
    onClearReminder: () -> Unit,
) {
    val parts = buildList {
        document.context.dateIso?.takeIf { it.isNotBlank() }?.let { add(it) }
        document.context.workplaceId?.takeIf { it.isNotBlank() }?.let { add("Работа: $it") }
        document.context.shiftCode?.takeIf { it.isNotBlank() }?.let { add("Смена $it") }
    }
    Column(verticalArrangement = Arrangement.spacedBy(appScaledSpacing(5.dp))) {
        if (parts.isNotEmpty()) {
            Text(parts.joinToString(" · "), style = MaterialTheme.typography.labelMedium, color = appListSecondaryTextColor())
        }
        if (document.labels.isNotEmpty()) {
            Text(document.labels.joinToString(" · ") { "#$it" }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        document.reminderAtMillis?.let { reminder ->
            val formatted = remember(reminder) {
                java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT)
                    .format(java.util.Date(reminder))
            }
            Text("Напомнить: $formatted", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))) {
            TextButton(onClick = appHapticAction(onAction = onRequestReminder)) {
                Text(if (document.reminderAtMillis == null) "Напомнить" else "Изменить напоминание")
            }
            if (document.reminderAtMillis != null) {
                TextButton(onClick = appHapticAction(onAction = onClearReminder)) { Text("Убрать") }
            }
        }
    }
}

@Composable
private fun NoteBlockEditor(
    block: NoteBlock,
    blockIndex: Int,
    blockCount: Int,
    activeBlockId: String?,
    onActivate: (String, Int, Int) -> Unit,
    onReplaceText: (String, StyledText) -> Unit,
    onToggleChecklist: (String, String) -> Unit,
    onChecklistText: (String, String, StyledText) -> Unit,
    onAddChecklistItem: (String) -> Unit,
    onRemoveChecklistItem: (String, String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemoveBlock: () -> Unit,
    mediaBlockContent: @Composable (NoteBlock) -> Unit,
) {
    val active = activeBlockId == block.id
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(appCornerRadius(18.dp)),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else appPanelBorderColor().copy(alpha = 0.45f)),
    ) {
        Column(
            modifier = Modifier.padding(appScaledSpacing(10.dp)),
            verticalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp)),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(blockTypeLabel(block), style = MaterialTheme.typography.labelMedium, color = appListSecondaryTextColor(), modifier = Modifier.weight(1f))
                IconButton(onClick = appHapticAction(onAction = onMoveUp), enabled = blockIndex > 0, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Rounded.KeyboardArrowUp, "Выше", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = appHapticAction(onAction = onMoveDown), enabled = blockIndex < blockCount - 1, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Rounded.KeyboardArrowDown, "Ниже", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = appHapticAction(onAction = onRemoveBlock), modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Rounded.Close, "Удалить блок", modifier = Modifier.size(18.dp))
                }
            }

            when (block) {
                is NoteBlock.Paragraph -> RichTextField(
                    blockId = block.id,
                    styledText = block.text,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    placeholder = "Текст заметки",
                    onActivate = onActivate,
                    onChange = { onReplaceText(block.id, it) },
                )
                is NoteBlock.Heading -> RichTextField(
                    blockId = block.id,
                    styledText = block.text,
                    textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    placeholder = "Заголовок раздела",
                    onActivate = onActivate,
                    onChange = { onReplaceText(block.id, it) },
                )
                is NoteBlock.Checklist -> {
                    block.items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(4.dp)),
                        ) {
                            if (item.indent > 0) Spacer(Modifier.width((item.indent * 12).dp))
                            Checkbox(
                                checked = item.checked,
                                onCheckedChange = { onToggleChecklist(block.id, item.id) },
                                modifier = Modifier.semantics {
                                    contentDescription = "Пункт списка: " + item.text.text.ifBlank { "без текста" }
                                },
                            )
                            OutlinedTextField(
                                value = item.text.text,
                                onValueChange = { onChecklistText(block.id, item.id, StyledTextMutations.replaceText(item.text, it)) },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Пункт") },
                                singleLine = false,
                            )
                            IconButton(onClick = appHapticAction { onRemoveChecklistItem(block.id, item.id) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Rounded.Close, "Удалить пункт", modifier = Modifier.size(17.dp))
                            }
                        }
                    }
                    TextButton(onClick = appHapticAction { onAddChecklistItem(block.id) }) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Добавить пункт")
                    }
                }
                is NoteBlock.Image,
                is NoteBlock.Video,
                is NoteBlock.Audio,
                is NoteBlock.FileAttachment -> mediaBlockContent(block)
                is NoteBlock.Divider -> HorizontalDivider()
            }
        }
    }
}

@Composable
private fun RichTextField(
    blockId: String,
    styledText: StyledText,
    textStyle: androidx.compose.ui.text.TextStyle,
    placeholder: String,
    onActivate: (String, Int, Int) -> Unit,
    onChange: (StyledText) -> Unit,
) {
    var value by remember(blockId) {
        mutableStateOf(TextFieldValue(styledText.toAnnotatedString(), TextRange(styledText.text.length)))
    }
    LaunchedEffect(styledText) {
        val start = value.selection.start.coerceIn(0, styledText.text.length)
        val end = value.selection.end.coerceIn(0, styledText.text.length)
        value = TextFieldValue(styledText.toAnnotatedString(), TextRange(start, end))
    }
    OutlinedTextField(
        value = value,
        onValueChange = { next ->
            val changed = StyledTextMutations.replaceText(styledText, next.text)
            value = TextFieldValue(changed.toAnnotatedString(), next.selection, next.composition)
            onActivate(blockId, next.selection.start, next.selection.end)
            onChange(changed)
        },
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { if (it.isFocused) onActivate(blockId, value.selection.start, value.selection.end) },
        placeholder = { Text(placeholder) },
        textStyle = textStyle,
        minLines = 2,
    )
}

@Composable
private fun DefaultNoteMediaBlock(block: NoteBlock) {
    when (block) {
        is NoteBlock.Image -> MediaBlock(Icons.Rounded.Image, "Фото", block.label.ifBlank { "Изображение" })
        is NoteBlock.Video -> MediaBlock(Icons.Rounded.Videocam, "Видео", block.label.ifBlank { "Видео" })
        is NoteBlock.Audio -> MediaBlock(Icons.Rounded.Mic, "Аудио", block.label.ifBlank { "Голосовая запись" }, block.transcript?.text)
        is NoteBlock.FileAttachment -> MediaBlock(Icons.Rounded.AttachFile, "Файл", block.label.ifBlank { "Вложение" })
        else -> Unit
    }
}

@Composable
private fun MediaBlock(icon: androidx.compose.ui.graphics.vector.ImageVector, kind: String, label: String, secondary: String? = null) {
    AppExpressiveSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(appCornerRadius(16.dp)),
        tone = AppExpressiveSurfaceTone.SOFT,
        border = BorderStroke(1.dp, appPanelBorderColor().copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(appScaledSpacing(12.dp)),
            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(10.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(26.dp), tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(kind, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                if (!secondary.isNullOrBlank()) Text(secondary, style = MaterialTheme.typography.bodySmall, color = appListSecondaryTextColor(), maxLines = 3)
            }
        }
    }
}

@Composable
private fun EditorFormattingBar(
    enabled: Boolean,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onUnderline: () -> Unit,
    onStrike: () -> Unit,
    onToggleHeading: () -> Unit,
) {
    AppExpressiveSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(appCornerRadius(18.dp)),
        tone = AppExpressiveSurfaceTone.SOFT,
        border = BorderStroke(1.dp, appPanelBorderColor().copy(alpha = 0.45f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(appScaledSpacing(6.dp)),
            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(2.dp)),
        ) {
            EditorTool(Icons.Rounded.FormatBold, "Жирный", enabled, onBold)
            EditorTool(Icons.Rounded.FormatItalic, "Курсив", enabled, onItalic)
            EditorTool(Icons.Rounded.FormatUnderlined, "Подчёркивание", enabled, onUnderline)
            EditorTool(Icons.Rounded.FormatStrikethrough, "Зачёркивание", enabled, onStrike)
            EditorTool(Icons.Rounded.Title, "Заголовок", enabled, onToggleHeading)
        }
    }
}

@Composable
private fun EditorTool(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = appHapticAction(onAction = onClick), enabled = enabled) {
        Icon(icon, contentDescription = description)
    }
}

@Composable
private fun EditorInsertBar(
    onAddText: () -> Unit,
    onAddChecklist: () -> Unit,
    onImage: () -> Unit,
    onVideo: () -> Unit,
    onAudio: () -> Unit,
    onFile: () -> Unit,
    isRecordingAudio: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(appScaledSpacing(5.dp))) {
        Text("Добавить", style = MaterialTheme.typography.labelMedium, color = appListSecondaryTextColor())
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp)),
        ) {
            InsertAction(Icons.Rounded.Add, "Текст", onAddText, Modifier.weight(1f))
            InsertAction(Icons.Rounded.CheckBox, "Список", onAddChecklist, Modifier.weight(1f))
            InsertAction(Icons.Rounded.Image, "Фото", onImage, Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp)),
        ) {
            InsertAction(Icons.Rounded.Videocam, "Видео", onVideo, Modifier.weight(1f))
            InsertAction(Icons.Rounded.Mic, if (isRecordingAudio) "Стоп" else "Голос", onAudio, Modifier.weight(1f))
            InsertAction(Icons.Rounded.AttachFile, "Файл", onFile, Modifier.weight(1f))
        }
    }
}

@Composable
private fun InsertAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = appHapticAction(onAction = onClick)),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, appPanelBorderColor().copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = appScaledSpacing(11.dp), vertical = appScaledSpacing(8.dp)),
            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(5.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun EditorAppearance(
    document: NoteDocument,
    labelDraft: String,
    onLabelDraftChange: (String) -> Unit,
    onAddLabel: () -> Unit,
    onRemoveLabel: (String) -> Unit,
    onAppearance: (NoteAppearance) -> Unit,
) {
    AppExpressiveSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(appCornerRadius(22.dp)),
        tone = AppExpressiveSurfaceTone.PANEL,
        border = BorderStroke(1.dp, appPanelBorderColor()),
    ) {
        Column(
            modifier = Modifier.padding(appCardPadding()),
            verticalArrangement = Arrangement.spacedBy(appScaledSpacing(10.dp)),
        ) {
            Text("Оформление и метки", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Цвет заметки", style = MaterialTheme.typography.labelMedium, color = appListSecondaryTextColor())
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp)),
            ) {
                listOf("#FFF3BF", "#D8F3DC", "#BDE0FE", "#E7C6FF", "#FFD6A5", "#FFFFFF").forEach { hex ->
                    val color = parseEditorColor(hex)
                    Surface(
                        modifier = Modifier.size(38.dp).clickable(onClick = appHapticAction { onAppearance(document.appearance.copy(colorHex = hex)) }),
                        shape = CircleShape,
                        color = color,
                        border = BorderStroke(if (document.appearance.colorHex.equals(hex, true)) 3.dp else 1.dp, if (document.appearance.colorHex.equals(hex, true)) MaterialTheme.colorScheme.primary else appPanelBorderColor()),
                    ) {}
                }
            }
            Text("Фон", style = MaterialTheme.typography.labelMedium, color = appListSecondaryTextColor())
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp)),
            ) {
                listOf("NONE" to "Нет", "GRID" to "Сетка", "DOTS" to "Точки", "LINES" to "Линии").forEach { (value, label) ->
                    FilterChip(
                        selected = document.appearance.backgroundPattern == value,
                        onClick = appHapticAction { onAppearance(document.appearance.copy(backgroundPattern = value)) },
                        label = { Text(label) },
                    )
                }
            }
            if (document.labels.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp)),
                ) {
                    document.labels.forEach { label ->
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        ) {
                            Row(
                                modifier = Modifier.padding(start = appScaledSpacing(10.dp), end = appScaledSpacing(3.dp), top = appScaledSpacing(4.dp), bottom = appScaledSpacing(4.dp)),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("#$label", style = MaterialTheme.typography.labelMedium)
                                IconButton(onClick = appHapticAction { onRemoveLabel(label) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Rounded.Close, "Удалить метку $label", modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = labelDraft,
                    onValueChange = onLabelDraftChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Новая метка") },
                    singleLine = true,
                )
                TextButton(onClick = appHapticAction(onAction = onAddLabel), enabled = labelDraft.trim().isNotBlank()) { Text("Добавить") }
            }
        }
    }
}

private fun blockTypeLabel(block: NoteBlock): String = when (block) {
    is NoteBlock.Paragraph -> "Текст"
    is NoteBlock.Heading -> "Заголовок"
    is NoteBlock.Checklist -> "Чек-лист"
    is NoteBlock.Image -> "Фото"
    is NoteBlock.Video -> "Видео"
    is NoteBlock.Audio -> "Аудио"
    is NoteBlock.FileAttachment -> "Файл"
    is NoteBlock.Divider -> "Разделитель"
}

private fun StyledText.toAnnotatedString(): AnnotatedString = buildAnnotatedString {
    append(text)
    spans.forEach { span ->
        val decorations = buildList {
            if (TextMark.UNDERLINE in span.marks) add(TextDecoration.Underline)
            if (TextMark.STRIKETHROUGH in span.marks) add(TextDecoration.LineThrough)
        }
        addStyle(
            SpanStyle(
                fontWeight = if (TextMark.BOLD in span.marks) FontWeight.Bold else null,
                fontStyle = if (TextMark.ITALIC in span.marks) FontStyle.Italic else null,
                fontFamily = if (TextMark.CODE in span.marks) FontFamily.Monospace else null,
                textDecoration = when (decorations.size) {
                    0 -> null
                    1 -> decorations.first()
                    else -> TextDecoration.combine(decorations)
                },
            ),
            start = span.start.coerceIn(0, text.length),
            end = span.endExclusive.coerceIn(0, text.length),
        )
    }
}

private fun Modifier.editorPattern(pattern: String, baseColor: Color): Modifier {
    if (pattern == "NONE") return this
    return drawBehind {
        val lineColor = Color.Black.copy(alpha = if (baseColor.red + baseColor.green + baseColor.blue > 1.5f) 0.055f else 0.09f)
        when (pattern) {
            "GRID" -> {
                val step = 26.dp.toPx()
                var x = step
                while (x < size.width) { drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx()); x += step }
                var y = step
                while (y < size.height) { drawLine(lineColor, Offset(0f, y), Offset(size.width, y), 1.dp.toPx()); y += step }
            }
            "LINES", "NOTEBOOK" -> {
                val step = 27.dp.toPx()
                var y = step
                while (y < size.height) { drawLine(lineColor, Offset(0f, y), Offset(size.width, y), 1.dp.toPx()); y += step }
            }
            "DOTS" -> {
                val step = 22.dp.toPx()
                var x = step
                while (x < size.width) {
                    var y = step
                    while (y < size.height) { drawCircle(lineColor, 1.2.dp.toPx(), Offset(x, y)); y += step }
                    x += step
                }
            }
        }
    }
}

private fun parseEditorColor(hex: String): Color = runCatching {
    Color(android.graphics.Color.parseColor(hex))
}.getOrDefault(Color(0xFFFFF3BF))
