package com.vigilante.shiftsalaryplanner

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.ImageView
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatAlignLeft
import androidx.compose.material.icons.automirrored.rounded.FormatAlignRight
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FormatAlignCenter
import androidx.compose.material.icons.rounded.FormatAlignJustify
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatColorFill
import androidx.compose.material.icons.rounded.FormatColorText
import androidx.compose.material.icons.rounded.FormatItalic
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.FormatStrikethrough
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.vigilante.shiftsalaryplanner.settings.AppNote
import com.vigilante.shiftsalaryplanner.settings.AppNoteAttachment
import com.vigilante.shiftsalaryplanner.settings.AppNoteChecklistItem
import com.vigilante.shiftsalaryplanner.ui.theme.AppMonoFontFamily
import com.vigilante.shiftsalaryplanner.ui.theme.AppSansFontFamily
import com.vigilante.shiftsalaryplanner.ui.theme.AppSerifFontFamily
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.io.File
import kotlinx.coroutines.delay

private val notePalette = listOf(
    "#FFF3BF",
    "#FFD6A5",
    "#D8F3DC",
    "#BDE0FE",
    "#E7C6FF",
    "#F8D7DA",
    "#FFE5EC",
    "#FFC2D1",
    "#CDB4DB",
    "#A2D2FF",
    "#B8F2E6",
    "#CDEAC0",
    "#FCF6BD",
    "#FFD6BA",
    "#FEC5BB",
    "#D0F4DE",
    "#E4C1F9",
    "#90DBF4",
    "#F1F0C0",
    "#D8E2DC",
    "#B7E4C7",
    "#95D5B2",
    "#FDE2E4",
    "#E2ECE9",
    "#DEE2FF",
    "#C8B6FF",
    "#FFAFCC",
    "#BDE0FE",
    "#A3C4F3",
    "#CFBAF0"
)

private val noteBackgroundPatterns = listOf(
    "NONE" to "Чистый",
    "DOTS" to "Точки",
    "GRID" to "Сетка",
    "LINES" to "Линии",
    "BLOOM" to "Цветы",
    "WAVES" to "Волны",
    "STARS" to "Звёзды",
    "CONFETTI" to "Конфетти",
    "CHECKER" to "Шахматы",
    "NOTEBOOK" to "Тетрадь",
    "CORNERS" to "Уголки",
    "RAIN" to "Дождь"
)

private val noteMediaMarkerRegex = Regex("""\[\[media:([A-Za-z0-9-]+)]]""")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNotesTabScreen(
    notes: List<AppNote>,
    onAddNote: (LocalDate) -> Unit,
    onEditNote: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedDateIso by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val selectedDate = remember(selectedDateIso) {
        runCatching { LocalDate.parse(selectedDateIso) }.getOrDefault(LocalDate.now())
    }
    val groupedNotes = remember(notes) {
        notes
            .sortedWith(compareByDescending<AppNote> { it.date }.thenByDescending { it.updatedAtMillis })
            .groupBy { note -> runCatching { LocalDate.parse(note.date) }.getOrDefault(LocalDate.now()) }
    }
    val selectedDayNotes = remember(notes, selectedDate) {
        notes
            .filter { note -> note.date == selectedDate.toString() }
            .sortedByDescending { it.updatedAtMillis }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(appScreenPadding()),
        verticalArrangement = Arrangement.spacedBy(appBlockSpacing())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Заметки",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (notes.isEmpty()) {
                        "Дневник смен, задач и быстрых мыслей"
                    } else {
                        "${notes.size} записей по календарю"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = appListSecondaryTextColor()
                )
            }
            TextButton(onClick = appHapticAction { onAddNote(selectedDate) }) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Text("Новая")
            }
        }

        NotesDateSelector(
            selectedDate = selectedDate,
            noteCount = selectedDayNotes.size,
            onSelectDate = { date -> selectedDateIso = date.toString() },
            onOpenDatePicker = { showDatePicker = true },
            onAddNote = { onAddNote(selectedDate) }
        )

        if (selectedDayNotes.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))) {
                Text(
                    text = "Выбранный день",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                selectedDayNotes.forEach { note ->
                    NotePreviewCard(
                        note = note,
                        onClick = { onEditNote(note.id) }
                    )
                }
            }
        }

        if (notes.isEmpty()) {
            AppExpressiveSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(appCornerRadius(24.dp)),
                tone = AppExpressiveSurfaceTone.ACCENT,
                border = BorderStroke(1.dp, appPanelBorderColor())
            ) {
                Column(
                    modifier = Modifier.padding(appCardPadding()),
                    verticalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))
                ) {
                    Text(
                        text = "Пока пусто",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Заметки можно добавлять здесь, на экране “Сегодня” или по долгому тапу на день/смену в календаре.",
                        style = MaterialTheme.typography.bodySmall,
                        color = appListSecondaryTextColor()
                    )
                }
            }
        } else {
            Text(
                text = "Все заметки",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            groupedNotes.forEach { (date, dayNotes) ->
                Column(verticalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))) {
                    Text(
                        text = formatDate(date),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    dayNotes.forEach { note ->
                        NotePreviewCard(
                            note = note,
                            onClick = { onEditNote(note.id) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(appScaledSpacing(92.dp)))
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.toUtcDatePickerMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis
                            ?.let(::localDateFromUtcDatePickerMillis)
                            ?.let { date -> selectedDateIso = date.toString() }
                        showDatePicker = false
                    }
                ) {
                    Text("Выбрать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Отмена")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun NotesDateSelector(
    selectedDate: LocalDate,
    noteCount: Int,
    onSelectDate: (LocalDate) -> Unit,
    onOpenDatePicker: () -> Unit,
    onAddNote: () -> Unit
) {
    AppExpressiveSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(appCornerRadius(22.dp)),
        tone = AppExpressiveSurfaceTone.SOFT,
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Column(
            modifier = Modifier.padding(appCardPadding()),
            verticalArrangement = Arrangement.spacedBy(appScaledSpacing(10.dp))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = appHapticAction { onSelectDate(selectedDate.minusDays(1)) }) {
                    Text("‹")
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.clickable(onClick = appHapticAction(onAction = onOpenDatePicker)),
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.54f),
                        border = BorderStroke(1.dp, appPanelBorderColor().copy(alpha = 0.42f))
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = appScaledSpacing(11.dp),
                                vertical = appScaledSpacing(6.dp)
                            ),
                            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(7.dp)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = formatDate(selectedDate),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = if (noteCount == 0) "заметок нет" else "заметок: $noteCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = appListSecondaryTextColor()
                    )
                }
                TextButton(onClick = appHapticAction { onSelectDate(selectedDate.plusDays(1)) }) {
                    Text("›")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DateShortcutChip(
                    label = "Сегодня",
                    selected = selectedDate == LocalDate.now(),
                    onClick = { onSelectDate(LocalDate.now()) },
                    modifier = Modifier.weight(1f)
                )
                DateShortcutChip(
                    label = "Завтра",
                    selected = selectedDate == LocalDate.now().plusDays(1),
                    onClick = { onSelectDate(LocalDate.now().plusDays(1)) },
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = appHapticAction(onAction = onAddNote),
                    modifier = Modifier.weight(1.15f)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Text("На дату")
                }
            }
        }
    }
}

@Composable
private fun DateShortcutChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = appHapticAction(onAction = onClick)),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.54f),
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.42f) else appPanelBorderColor().copy(alpha = 0.45f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) MaterialTheme.colorScheme.primary else appListSecondaryTextColor()
            )
        }
    }
}

@Composable
fun AppNoteEditorScreen(
    note: AppNote?,
    date: LocalDate,
    workplaceName: String?,
    shiftTitle: String?,
    workplaceId: String?,
    shiftCode: String?,
    onBack: () -> Unit,
    onSave: (AppNote) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var title by rememberSaveable(note?.id) { mutableStateOf(note?.title.orEmpty()) }
    var body by rememberSaveable(note?.id, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(note?.body.orEmpty().stripLegacyNoteMarkup()))
    }
    var colorHex by rememberSaveable(note?.id) { mutableStateOf(note?.colorHex ?: notePalette.first()) }
    var backgroundPattern by rememberSaveable(note?.id) { mutableStateOf(note?.backgroundPattern ?: "NONE") }
    var showBackgroundPicker by rememberSaveable(note?.id) { mutableStateOf(false) }
    var textSizeMode by rememberSaveable(note?.id) { mutableStateOf(note?.textSizeMode ?: "NORMAL") }
    var fontMode by rememberSaveable(note?.id) { mutableStateOf(note?.fontMode ?: "SANS") }
    var textColorHex by rememberSaveable(note?.id) { mutableStateOf(note?.textColorHex.orEmpty()) }
    var highlightColorHex by rememberSaveable(note?.id) { mutableStateOf(note?.highlightColorHex.orEmpty()) }
    var textAlignMode by rememberSaveable(note?.id) { mutableStateOf(note?.textAlignMode ?: "START") }
    var listMode by rememberSaveable(note?.id) { mutableStateOf(note?.listMode ?: "NONE") }
    var bulletListMode by rememberSaveable(note?.id) { mutableStateOf(note?.bulletListMode ?: "DOT") }
    var numberListMode by rememberSaveable(note?.id) { mutableStateOf(note?.numberListMode ?: "DECIMAL") }
    var bodyBold by rememberSaveable(note?.id) { mutableStateOf(note?.bodyBold ?: note?.body.orEmpty().hasLegacyBold()) }
    var bodyItalic by rememberSaveable(note?.id) { mutableStateOf(note?.bodyItalic ?: note?.body.orEmpty().hasLegacyItalic()) }
    var bodyHeading by rememberSaveable(note?.id) { mutableStateOf(note?.bodyHeading ?: note?.body.orEmpty().hasLegacyHeading()) }
    var bodyStrike by rememberSaveable(note?.id) { mutableStateOf(note?.bodyStrike ?: note?.body.orEmpty().hasLegacyStrike()) }
    var colorPickerTarget by rememberSaveable(note?.id) { mutableStateOf<String?>(null) }
    var attachmentSourceTarget by rememberSaveable(note?.id) { mutableStateOf<String?>(null) }
    var pendingAttachmentType by rememberSaveable(note?.id) { mutableStateOf<String?>(null) }
    var activeRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var activeRecordingFile by remember { mutableStateOf<File?>(null) }
    val checklist = remember(note?.id) {
        mutableStateListOf<AppNoteChecklistItem>().apply {
            addAll(note?.checklist.orEmpty())
        }
    }
    val attachments = remember(note?.id) {
        mutableStateListOf<AppNoteAttachment>().apply {
            addAll(note?.attachments.orEmpty())
        }
    }
    fun addAttachmentFromUri(uri: Uri, requestedType: String? = null) {
        context.takePersistableReadPermission(uri)
        val resolvedType = context.resolveNoteAttachmentType(uri)
        val type = when (requestedType) {
            "IMAGE", "VIDEO", "AUDIO" -> requestedType
            else -> resolvedType
        }
        val attachment = AppNoteAttachment(
            type = type,
            uri = uri.toString(),
            label = context.resolveNoteAttachmentLabel(uri),
            aspectRatio = context.resolveNoteAttachmentAspectRatio(uri, type)
        )
        attachments.add(
            attachment
        )
        if (type == "IMAGE" || type == "VIDEO") {
            body = body.insertNoteMediaMarker(attachment.id)
        }
    }
    val documentAttachmentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        val requestedType = pendingAttachmentType
        pendingAttachmentType = null
        uris.forEach { uri ->
            addAttachmentFromUri(uri, requestedType)
        }
    }
    val mediaAttachmentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val requestedType = pendingAttachmentType
        pendingAttachmentType = null
        uris.forEach { uri ->
            addAttachmentFromUri(uri, requestedType)
        }
    }
    fun startAudioRecording() {
        runCatching {
            val outputFile = context.createNoteAudioFile()
            val recorder = createNoteMediaRecorder(outputFile)
            recorder.prepare()
            recorder.start()
            activeRecordingFile = outputFile
            activeRecorder = recorder
        }
    }
    fun stopAudioRecording() {
        val recorder = activeRecorder ?: return
        val file = activeRecordingFile
        runCatching { recorder.stop() }
        runCatching { recorder.release() }
        activeRecorder = null
        activeRecordingFile = null
        if (file != null && file.exists() && file.length() > 0L) {
            attachments.add(
                AppNoteAttachment(
                    type = "AUDIO",
                    uri = Uri.fromFile(file).toString(),
                    label = file.nameWithoutExtension
                )
            )
        }
    }
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startAudioRecording()
    }
    DisposableEffect(Unit) {
        onDispose {
            activeRecorder?.let { recorder ->
                runCatching { recorder.stop() }
                runCatching { recorder.release() }
            }
        }
    }
    val contextLabel = buildString {
        append(formatDate(date))
        if (!workplaceName.isNullOrBlank()) append(" · ").append(workplaceName)
        if (!shiftTitle.isNullOrBlank()) append(" · ").append(shiftTitle)
    }
    val noteSurface = noteSurfaceColor(colorHex)
    val canSave = title.isNotBlank() || body.text.isNotBlank() || checklist.any { it.text.isNotBlank() } || attachments.isNotEmpty()
    val saveNote = {
        val now = System.currentTimeMillis()
        onSave(
            AppNote(
                id = note?.id ?: java.util.UUID.randomUUID().toString(),
                date = date.toString(),
                workplaceId = workplaceId,
                shiftCode = shiftCode,
                title = title,
                body = body.text,
                colorHex = colorHex,
                backgroundPattern = backgroundPattern,
                textSizeMode = textSizeMode,
                fontMode = fontMode,
                textColorHex = textColorHex,
                highlightColorHex = highlightColorHex,
                textAlignMode = textAlignMode,
                listMode = listMode,
                bulletListMode = bulletListMode,
                numberListMode = numberListMode,
                bodyBold = bodyBold,
                bodyItalic = bodyItalic,
                bodyHeading = bodyHeading,
                bodyStrike = bodyStrike,
                checklist = checklist.toList(),
                attachments = attachments.map { attachment ->
                    if (
                        body.text.contains("[[media:${attachment.id}]]") &&
                        attachment.type != "IMAGE" &&
                        attachment.type != "VIDEO"
                    ) {
                        attachment.copy(type = inferInlineNoteMediaType(attachment))
                    } else {
                        attachment
                    }
                },
                createdAtMillis = note?.createdAtMillis ?: now,
                updatedAtMillis = now
            )
        )
        onBack()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(appScreenPadding()),
        verticalArrangement = Arrangement.spacedBy(appBlockSpacing())
    ) {
        AppServiceScreenHeader(
            title = if (note == null) "Новая заметка" else "Заметка",
            subtitle = contextLabel,
            onBack = onBack,
            trailing = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(4.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (note != null) {
                        IconButton(
                            onClick = appHapticAction {
                                onDelete(note.id)
                                onBack()
                            }
                        ) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Удалить")
                        }
                    }
                    IconButton(
                        onClick = appHapticAction(onAction = saveNote),
                        enabled = canSave
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = "Сохранить")
                    }
                }
            }
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(appCornerRadius(26.dp)),
            color = noteSurface,
            border = BorderStroke(1.dp, noteBorderColor(colorHex))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .notePatternBackground(backgroundPattern, colorHex)
                    .padding(appScaledSpacing(16.dp)),
                verticalArrangement = Arrangement.spacedBy(appScaledSpacing(12.dp))
            ) {
                NoteContextStrip(
                    contextLabel = contextLabel,
                    colorHex = colorHex,
                    backgroundPattern = backgroundPattern,
                    onPickColor = { colorPickerTarget = "NOTE" },
                    onPickBackground = { showBackgroundPicker = true }
                )

                PlainNoteField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = "Заголовок",
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                NoteBodyEditor(
                    body = body,
                    onBodyChange = { body = it },
                    checklist = checklist,
                    attachments = attachments,
                    textSizeMode = textSizeMode,
                    fontMode = fontMode,
                    bodyBold = bodyBold,
                    bodyItalic = bodyItalic,
                    bodyHeading = bodyHeading,
                    bodyStrike = bodyStrike,
                    textColorHex = textColorHex,
                    highlightColorHex = highlightColorHex,
                    textAlignMode = textAlignMode,
                    listMode = listMode,
                    onToggleChecklistItem = { itemId, checked ->
                        val index = checklist.indexOfFirst { it.id == itemId }
                        if (index >= 0) checklist[index] = checklist[index].copy(checked = checked)
                    },
                    onChecklistTextChange = { itemId, text ->
                        val index = checklist.indexOfFirst { it.id == itemId }
                        if (index >= 0) checklist[index] = checklist[index].copy(text = text)
                    },
                    onRemoveChecklistItem = { itemId -> checklist.removeAll { it.id == itemId } },
                    onAddChecklistItem = { checklist.add(AppNoteChecklistItem()) },
                    onUpdateAttachment = { updated ->
                        val index = attachments.indexOfFirst { it.id == updated.id }
                        if (index >= 0) attachments[index] = updated
                    },
                    onRemoveAttachment = { attachmentId ->
                        attachments.removeAll { it.id == attachmentId }
                        body = body.removeNoteMediaMarker(attachmentId)
                    }
                )

                val fileAttachments = attachments.filterNot { it.type == "IMAGE" || it.type == "VIDEO" }
                if (fileAttachments.isNotEmpty()) {
                    NoteAttachmentsEditor(
                        attachments = fileAttachments,
                        onUpdate = { updated ->
                            val index = attachments.indexOfFirst { it.id == updated.id }
                            if (index >= 0) attachments[index] = updated
                        },
                        onRemove = { id -> attachments.removeAll { it.id == id } }
                    )
                }

                NoteBottomBar(
                    textSizeMode = textSizeMode,
                    fontMode = fontMode,
                    onTextSizeModeChange = { textSizeMode = it },
                    onFontModeChange = { fontMode = it },
                    bodyBold = bodyBold,
                    bodyItalic = bodyItalic,
                    bodyHeading = bodyHeading,
                    bodyStrike = bodyStrike,
                    textAlignMode = textAlignMode,
                    listMode = listMode,
                    bulletListMode = bulletListMode,
                    numberListMode = numberListMode,
                    onBold = { bodyBold = !bodyBold },
                    onItalic = { bodyItalic = !bodyItalic },
                    onToggleHeader = { bodyHeading = !bodyHeading },
                    onStrike = { bodyStrike = !bodyStrike },
                    onTextAlignModeChange = { textAlignMode = it },
                    onListModeChange = { nextMode ->
                        body = body.applyNoteListMode(
                            nextMode = nextMode,
                            bulletMode = bulletListMode,
                            numberMode = numberListMode
                        )
                        listMode = nextMode
                    },
                    onBulletListModeChange = { nextMode ->
                        bulletListMode = nextMode
                        if (listMode == "BULLET") {
                            body = body.applyNoteListMode(
                                nextMode = "BULLET",
                                bulletMode = nextMode,
                                numberMode = numberListMode
                            )
                        }
                    },
                    onNumberListModeChange = { nextMode ->
                        numberListMode = nextMode
                        if (listMode == "NUMBERED") {
                            body = body.applyNoteListMode(
                                nextMode = "NUMBERED",
                                bulletMode = bulletListMode,
                                numberMode = nextMode
                            )
                        }
                    },
                    onPickTextColor = { colorPickerTarget = "TEXT" },
                    onPickHighlightColor = { colorPickerTarget = "HIGHLIGHT" },
                    onAttachImage = { attachmentSourceTarget = "IMAGE" },
                    onAttachVideo = { attachmentSourceTarget = "VIDEO" },
                    onAttachAudio = { attachmentSourceTarget = "AUDIO" },
                    isRecordingAudio = activeRecorder != null,
                    onToggleRecordAudio = {
                        if (activeRecorder != null) {
                            stopAudioRecording()
                        } else if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            startAudioRecording()
                        } else {
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onAddChecklistItem = {
                        checklist.add(AppNoteChecklistItem())
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(appScaledSpacing(92.dp)))
    }

    if (colorPickerTarget != null) {
        NoteColorPickerDialog(
            title = when (colorPickerTarget) {
                "TEXT" -> "Цвет текста"
                "HIGHLIGHT" -> "Выделение"
                else -> "Цвет заметки"
            },
            selectedColorHex = when (colorPickerTarget) {
                "TEXT" -> textColorHex
                "HIGHLIGHT" -> highlightColorHex
                else -> colorHex
            },
            allowDefault = colorPickerTarget != "NOTE",
            onSelect = { picked ->
                when (colorPickerTarget) {
                    "TEXT" -> textColorHex = picked
                    "HIGHLIGHT" -> highlightColorHex = picked
                    else -> colorHex = picked
                }
                colorPickerTarget = null
            },
            onDismiss = { colorPickerTarget = null }
        )
    }

    if (showBackgroundPicker) {
        NoteBackgroundPickerDialog(
            selectedPattern = backgroundPattern,
            colorHex = colorHex,
            onSelect = { picked ->
                backgroundPattern = picked
                showBackgroundPicker = false
            },
            onDismiss = { showBackgroundPicker = false }
        )
    }

    if (attachmentSourceTarget != null) {
        NoteAttachmentSourceDialog(
            target = attachmentSourceTarget.orEmpty(),
            onPickMedia = {
                val target = attachmentSourceTarget
                pendingAttachmentType = target
                when (target) {
                    "IMAGE" -> mediaAttachmentPicker.launch("image/*")
                    "VIDEO" -> mediaAttachmentPicker.launch("video/*")
                    "AUDIO" -> mediaAttachmentPicker.launch("audio/*")
                }
                attachmentSourceTarget = null
            },
            onPickFiles = {
                val target = attachmentSourceTarget
                pendingAttachmentType = target
                when (target) {
                    "IMAGE" -> documentAttachmentPicker.launch(arrayOf("image/*"))
                    "VIDEO" -> documentAttachmentPicker.launch(arrayOf("video/*"))
                    "AUDIO" -> documentAttachmentPicker.launch(arrayOf("audio/*"))
                }
                attachmentSourceTarget = null
            },
            onDismiss = { attachmentSourceTarget = null }
        )
    }
}

@Composable
private fun NoteContextStrip(
    contextLabel: String,
    colorHex: String,
    backgroundPattern: String,
    onPickColor: () -> Unit,
    onPickBackground: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.weight(1f, fill = false),
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.50f),
            border = BorderStroke(1.dp, appPanelBorderColor().copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = appScaledSpacing(9.dp), vertical = appScaledSpacing(5.dp)),
                horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = contextLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = appListSecondaryTextColor()
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NoteMiniAppearanceButton(
                onClick = onPickColor
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(parseColorHex(colorHex, 0xFFFFF3BF.toInt())))
                        .border(1.dp, appPanelBorderColor(), CircleShape)
                )
            }
            NoteMiniAppearanceButton(
                onClick = onPickBackground
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(noteSurfaceColor(colorHex))
                        .notePatternBackground(backgroundPattern, colorHex)
                        .border(1.dp, appPanelBorderColor(), RoundedCornerShape(8.dp))
                )
            }
        }
    }
}

@Composable
private fun NoteMiniAppearanceButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .clickable(onClick = appHapticAction(onAction = onClick)),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.58f),
        border = BorderStroke(1.dp, appPanelBorderColor().copy(alpha = 0.42f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Composable
fun NotePreviewCard(
    note: AppNote,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppExpressiveSurface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = appHapticAction(onAction = onClick)),
        shape = RoundedCornerShape(appCornerRadius(18.dp)),
        tone = AppExpressiveSurfaceTone.GLASS,
        border = BorderStroke(1.dp, noteBorderColor(note.colorHex))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(noteSurfaceColor(note.colorHex))
                .notePatternBackground(note.backgroundPattern, note.colorHex)
                .padding(appScaledSpacing(12.dp)),
            verticalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))
        ) {
            Text(
                text = note.title.ifBlank { "Без заголовка" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            if (note.body.isNotBlank()) {
                RichNoteText(
                    text = note.body,
                    textSizeMode = note.textSizeMode,
                    fontMode = note.fontMode,
                    bold = note.bodyBold,
                    italic = note.bodyItalic,
                    heading = note.bodyHeading,
                    strike = note.bodyStrike,
                    textColorHex = note.textColorHex,
                    highlightColorHex = note.highlightColorHex,
                    textAlignMode = note.textAlignMode,
                    listMode = note.listMode,
                    color = appListSecondaryTextColor(),
                    maxLines = 3
                )
            }
            if (note.checklist.isNotEmpty()) {
                val done = note.checklist.count { it.checked }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Чек-лист: $done/${note.checklist.size}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (note.attachments.isNotEmpty()) {
                Text(
                    text = "Вложения: ${note.attachments.size}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun RichNoteText(
    text: String,
    textSizeMode: String,
    fontMode: String,
    bold: Boolean = false,
    italic: Boolean = false,
    heading: Boolean = false,
    strike: Boolean = false,
    textColorHex: String = "",
    highlightColorHex: String = "",
    textAlignMode: String = "START",
    listMode: String = "NONE",
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    maxLines: Int = Int.MAX_VALUE
) {
    val baseStyle = noteBodyTextStyle(textSizeMode, fontMode, bold, italic, heading, strike).copy(
        color = if (textColorHex.isBlank()) color else noteTextColor(textColorHex),
        textAlign = noteTextAlign(textAlignMode),
        background = noteHighlightColor(highlightColorHex)
    )
    Text(
        text = remember(text, textSizeMode, fontMode, bold, italic, heading, strike, listMode) {
            buildRichNoteAnnotatedString(
                source = text.removeNoteMediaMarkers(),
                baseStyle = baseStyle,
                listMode = listMode
            )
        },
        modifier = modifier,
        style = baseStyle,
        maxLines = maxLines
    )
}

private sealed class NoteBodySegment {
    data class Text(val start: Int, val end: Int) : NoteBodySegment()
    data class Media(val attachment: AppNoteAttachment) : NoteBodySegment()
}

private fun buildNoteBodySegments(
    bodyText: String,
    mediaAttachments: List<AppNoteAttachment>
): List<NoteBodySegment> {
    val mediaById = mediaAttachments.associateBy { it.id }
    val usedIds = mutableSetOf<String>()
    val segments = mutableListOf<NoteBodySegment>()
    var cursor = 0
    noteMediaMarkerRegex.findAll(bodyText).forEach { match ->
        if (match.range.first > cursor) {
            segments += NoteBodySegment.Text(cursor, match.range.first)
        }
        val mediaId = match.groupValues.getOrNull(1).orEmpty()
        val attachment = mediaById[mediaId]
        if (attachment != null) {
            segments += NoteBodySegment.Media(attachment)
            usedIds += mediaId
        }
        cursor = match.range.last + 1
    }
    if (cursor < bodyText.length || segments.none { it is NoteBodySegment.Text }) {
        segments += NoteBodySegment.Text(cursor, bodyText.length)
    }
    mediaAttachments
        .filterNot { it.id in usedIds }
        .forEach { attachment -> segments += NoteBodySegment.Media(attachment) }
    return segments
}

private fun TextFieldValue.textSegmentValue(segment: NoteBodySegment.Text): TextFieldValue {
    val localText = text.substring(segment.start.coerceIn(0, text.length), segment.end.coerceIn(0, text.length))
    val globalStart = minOf(selection.start, selection.end)
    val globalEnd = maxOf(selection.start, selection.end)
    val localSelection = if (globalStart in segment.start..segment.end && globalEnd in segment.start..segment.end) {
        TextRange(globalStart - segment.start, globalEnd - segment.start)
    } else {
        TextRange(localText.length)
    }
    return TextFieldValue(localText, selection = localSelection)
}

private fun TextFieldValue.replaceTextSegment(
    segment: NoteBodySegment.Text,
    updated: TextFieldValue
): TextFieldValue {
    val safeStart = segment.start.coerceIn(0, text.length)
    val safeEnd = segment.end.coerceIn(safeStart, text.length)
    val nextText = text.replaceRange(safeStart, safeEnd, updated.text)
    return copy(
        text = nextText,
        selection = TextRange(
            safeStart + updated.selection.start.coerceIn(0, updated.text.length),
            safeStart + updated.selection.end.coerceIn(0, updated.text.length)
        )
    )
}

@Composable
private fun InlineNoteMediaBlock(
    attachment: AppNoteAttachment,
    onUpdate: (AppNoteAttachment) -> Unit,
    onRemove: (String) -> Unit
) {
    val context = LocalContext.current
    var fullScreenAttachment by remember { mutableStateOf<AppNoteAttachment?>(null) }
    var selected by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    var liveWidthFraction by remember(attachment.id) { mutableStateOf(noteAttachmentWidthFraction(attachment)) }
    var liveAspectRatio by remember(attachment.id) { mutableStateOf(noteAttachmentAspectRatio(attachment)) }
    LaunchedEffect(attachment.widthFraction, attachment.displaySizeMode) {
        liveWidthFraction = noteAttachmentWidthFraction(attachment)
    }
    LaunchedEffect(attachment.aspectRatio, attachment.uri, attachment.type) {
        val resolvedRatio = if (attachment.aspectRatio > 0f) {
            noteAttachmentAspectRatio(attachment)
        } else {
            context.resolveNoteAttachmentAspectRatio(Uri.parse(attachment.uri), attachment.type)
        }
        liveAspectRatio = resolvedRatio
        if (attachment.aspectRatio <= 0f && resolvedRatio > 0f) {
            onUpdate(attachment.copy(aspectRatio = resolvedRatio))
        }
    }
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val parentWidthPx = with(density) { maxWidth.toPx().coerceAtLeast(1f) }
        val minHeightPx = with(density) { 72.dp.toPx() }
        val maxHeightPx = parentWidthPx * 2.35f
        fun resizeMedia(drag: Offset, widthDirection: Float, heightDirection: Float) {
            val currentWidthPx = parentWidthPx * liveWidthFraction
            val currentHeightPx = currentWidthPx / liveAspectRatio.coerceAtLeast(0.32f)
            val nextWidthPx = (currentWidthPx + drag.x * widthDirection)
                .coerceIn(parentWidthPx * 0.28f, parentWidthPx)
            val nextHeightPx = (currentHeightPx + drag.y * heightDirection)
                .coerceIn(minHeightPx, maxHeightPx)
            val nextWidthFraction = (nextWidthPx / parentWidthPx).coerceIn(0.28f, 1f)
            val nextAspectRatio = (nextWidthPx / nextHeightPx).coerceIn(0.32f, 3.2f)
            liveWidthFraction = nextWidthFraction
            liveAspectRatio = nextAspectRatio
            onUpdate(
                attachment.copy(
                    widthFraction = nextWidthFraction,
                    aspectRatio = nextAspectRatio,
                    displaySizeMode = "CUSTOM"
                )
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth(liveWidthFraction)
                .align(Alignment.TopStart)
                .clip(RoundedCornerShape(appCornerRadius(8.dp)))
                .border(
                    width = if (selected) 2.dp else 0.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = RoundedCornerShape(appCornerRadius(8.dp))
                )
                .clickable(onClick = appHapticAction { selected = true }),
            contentAlignment = Alignment.TopEnd
        ) {
            when (attachment.type) {
                "IMAGE" -> EmbeddedNoteImage(
                    attachment = attachment,
                    aspectRatio = liveAspectRatio,
                    onOpen = { selected = true }
                )
                "VIDEO" -> EmbeddedNoteVideo(
                    attachment = attachment,
                    aspectRatio = liveAspectRatio,
                    onOpen = { selected = true }
                )
            }
            if (selected) {
                NoteResizeHandle(
                    modifier = Modifier.align(Alignment.TopStart),
                    onDrag = { drag -> resizeMedia(drag, widthDirection = -1f, heightDirection = -1f) }
                )
                NoteResizeHandle(
                    modifier = Modifier.align(Alignment.TopEnd),
                    onDrag = { drag -> resizeMedia(drag, widthDirection = 1f, heightDirection = -1f) }
                )
                NoteResizeHandle(
                    modifier = Modifier.align(Alignment.BottomStart),
                    onDrag = { drag -> resizeMedia(drag, widthDirection = -1f, heightDirection = 1f) }
                )
                NoteResizeHandle(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    onDrag = { drag -> resizeMedia(drag, widthDirection = 1f, heightDirection = 1f) }
                )
                Row(
                    modifier = Modifier.padding(appScaledSpacing(6.dp)),
                    horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(5.dp))
                ) {
                    NoteInlineMediaAction(
                        imageVector = Icons.Rounded.Fullscreen,
                        contentDescription = "На весь экран",
                        onClick = { fullScreenAttachment = attachment }
                    )
                    NoteInlineMediaAction(
                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = "Открыть внешне",
                        onClick = { context.openNoteAttachmentExternally(attachment) }
                    )
                    NoteInlineMediaAction(
                        imageVector = Icons.Rounded.RemoveCircleOutline,
                        contentDescription = "Удалить",
                        tint = MaterialTheme.colorScheme.error,
                        onClick = { onRemove(attachment.id) }
                    )
                }
            }
        }
    }

    fullScreenAttachment?.let { media ->
        FullScreenNoteMediaDialog(
            attachment = media,
            onDismiss = { fullScreenAttachment = null }
        )
    }
}

@Composable
private fun NoteResizeHandle(
    modifier: Modifier = Modifier,
    onDrag: (Offset) -> Unit
) {
    Surface(
        modifier = modifier
            .offset(x = 0.dp, y = 0.dp)
            .size(18.dp)
            .clip(CircleShape)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount)
                }
        },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    ) {
        Box(contentAlignment = Alignment.Center) {}
    }
}

@Composable
private fun NoteInlineMediaAction(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .clickable(onClick = appHapticAction(onAction = onClick)),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
        border = BorderStroke(1.dp, appPanelBorderColor().copy(alpha = 0.32f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
private fun FullScreenNoteMediaDialog(
    attachment: AppNoteAttachment,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.86f),
            shape = RoundedCornerShape(appCornerRadius(24.dp)),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (attachment.type) {
                    "IMAGE" -> AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            ImageView(context).apply {
                                scaleType = ImageView.ScaleType.FIT_CENTER
                                adjustViewBounds = true
                                setBackgroundColor(android.graphics.Color.BLACK)
                            }
                        },
                        update = { imageView ->
                            imageView.setImageURI(Uri.parse(attachment.uri))
                        }
                    )
                    "VIDEO" -> AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            VideoView(context).apply {
                                val controller = MediaController(context)
                                controller.setAnchorView(this)
                                setMediaController(controller)
                                setVideoURI(Uri.parse(attachment.uri))
                                start()
                            }
                        },
                        update = { videoView ->
                            if (videoView.tag != attachment.uri) {
                                videoView.tag = attachment.uri
                                videoView.setVideoURI(Uri.parse(attachment.uri))
                                videoView.start()
                            }
                        }
                    )
                }
                NoteInlineMediaAction(
                    imageVector = Icons.Rounded.RemoveCircleOutline,
                    contentDescription = "Закрыть",
                    onClick = onDismiss,
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(appScaledSpacing(12.dp))
                )
            }
        }
    }
}

@Composable
private fun NoteBodyEditor(
    body: TextFieldValue,
    onBodyChange: (TextFieldValue) -> Unit,
    checklist: List<AppNoteChecklistItem>,
    attachments: List<AppNoteAttachment>,
    textSizeMode: String,
    fontMode: String,
    bodyBold: Boolean,
    bodyItalic: Boolean,
    bodyHeading: Boolean,
    bodyStrike: Boolean,
    textColorHex: String,
    highlightColorHex: String,
    textAlignMode: String,
    listMode: String,
    onToggleChecklistItem: (String, Boolean) -> Unit,
    onChecklistTextChange: (String, String) -> Unit,
    onRemoveChecklistItem: (String) -> Unit,
    onAddChecklistItem: () -> Unit,
    onUpdateAttachment: (AppNoteAttachment) -> Unit,
    onRemoveAttachment: (String) -> Unit
) {
    val bodyStyle = noteBodyTextStyle(
        textSizeMode = textSizeMode,
        fontMode = fontMode,
        bold = bodyBold,
        italic = bodyItalic,
        heading = bodyHeading,
        strike = bodyStrike
    ).copy(
        color = noteTextColor(textColorHex),
        textAlign = noteTextAlign(textAlignMode),
        background = noteHighlightColor(highlightColorHex)
    )
    val checklistStyle = noteBodyTextStyle(
        textSizeMode = textSizeMode,
        fontMode = fontMode,
        bold = bodyBold,
        italic = bodyItalic,
        heading = false,
        strike = bodyStrike
    ).copy(
        color = noteTextColor(textColorHex),
        textAlign = noteTextAlign(textAlignMode),
        background = noteHighlightColor(highlightColorHex)
    )

    val mediaAttachments = remember(attachments, body.text) {
        attachments
            .filter { it.type == "IMAGE" || it.type == "VIDEO" || body.text.contains("[[media:${it.id}]]") }
            .map { attachment ->
                if (attachment.type == "IMAGE" || attachment.type == "VIDEO") {
                    attachment
                } else {
                    attachment.copy(type = inferInlineNoteMediaType(attachment))
                }
            }
    }
    val segments = remember(body.text, mediaAttachments) {
        buildNoteBodySegments(body.text, mediaAttachments)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 190.dp)
            .padding(vertical = appScaledSpacing(2.dp)),
        verticalArrangement = Arrangement.spacedBy(appScaledSpacing(7.dp))
    ) {
        segments.forEach { segment ->
            when (segment) {
                is NoteBodySegment.Text -> {
                    PlainNoteValueField(
                        value = body.textSegmentValue(segment),
                        onValueChange = { updated ->
                            onBodyChange(body.replaceTextSegment(segment, updated))
                        },
                        placeholder = "Заметка",
                        minHeight = if (body.text.isBlank() && checklist.isEmpty() && mediaAttachments.isEmpty()) 170.dp else 0.dp,
                        textStyle = bodyStyle
                    )
                }
                is NoteBodySegment.Media -> {
                    InlineNoteMediaBlock(
                        attachment = segment.attachment,
                        onUpdate = onUpdateAttachment,
                        onRemove = onRemoveAttachment
                    )
                }
            }
        }
        if (checklist.isNotEmpty()) {
            NoteChecklistEditor(
                items = checklist,
                onToggle = onToggleChecklistItem,
                onTextChange = onChecklistTextChange,
                onRemove = onRemoveChecklistItem,
                onAdd = onAddChecklistItem,
                textStyle = checklistStyle
            )
        }
    }
}

@Composable
private fun PlainNoteValueField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    placeholder: String,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    minHeight: androidx.compose.ui.unit.Dp = 0.dp
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight),
        textStyle = textStyle,
        placeholder = {
            Text(
                text = placeholder,
                style = textStyle,
                color = appListSecondaryTextColor().copy(alpha = 0.72f)
            )
        },
        colors = noteTransparentTextFieldColors()
    )
}

@Composable
private fun PlainNoteField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    minHeight: androidx.compose.ui.unit.Dp = 0.dp
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight),
        singleLine = singleLine,
        textStyle = textStyle,
        placeholder = {
            Text(
                text = placeholder,
                style = textStyle,
                color = appListSecondaryTextColor().copy(alpha = 0.72f)
            )
        },
        colors = noteTransparentTextFieldColors()
    )
}

@Composable
private fun noteTransparentTextFieldColors() = TextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    errorContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    errorIndicatorColor = Color.Transparent
)

@Composable
private fun NoteChecklistEditor(
    items: List<AppNoteChecklistItem>,
    onToggle: (String, Boolean) -> Unit,
    onTextChange: (String, String) -> Unit,
    onRemove: (String) -> Unit,
    onAdd: () -> Unit,
    textStyle: TextStyle
) {
    Column(verticalArrangement = Arrangement.spacedBy(appScaledSpacing(5.dp))) {
        items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .border(
                            width = 1.dp,
                            color = if (item.checked) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                appPanelBorderColor()
                            },
                            shape = CircleShape
                        )
                        .background(
                            if (item.checked) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            } else {
                                Color.Transparent
                            }
                        )
                        .clickable(onClick = appHapticAction { onToggle(item.id, !item.checked) }),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.checked) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                PlainNoteField(
                    value = item.text,
                    onValueChange = { text -> onTextChange(item.id, text) },
                    placeholder = "Пункт списка",
                    singleLine = true,
                    textStyle = textStyle.copy(
                        textDecoration = if (item.checked) TextDecoration.LineThrough else textStyle.textDecoration
                    ),
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = appHapticAction { onRemove(item.id) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RemoveCircleOutline,
                        contentDescription = "Удалить пункт",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        TextButton(
            onClick = appHapticAction(onAction = onAdd),
            modifier = Modifier.align(Alignment.Start)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Text("Пункт")
        }
    }
}

@Composable
private fun NoteChecklistPreview(
    items: List<AppNoteChecklistItem>,
    textStyle: TextStyle
) {
    Column(verticalArrangement = Arrangement.spacedBy(appScaledSpacing(5.dp))) {
        items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .border(
                            width = 1.dp,
                            color = if (item.checked) MaterialTheme.colorScheme.primary else appPanelBorderColor(),
                            shape = CircleShape
                        )
                        .background(
                            if (item.checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.checked) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = item.text.ifBlank { "Пункт списка" },
                    style = textStyle.copy(
                        textDecoration = if (item.checked) TextDecoration.LineThrough else textStyle.textDecoration
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NoteBottomBar(
    textSizeMode: String,
    fontMode: String,
    onTextSizeModeChange: (String) -> Unit,
    onFontModeChange: (String) -> Unit,
    bodyBold: Boolean,
    bodyItalic: Boolean,
    bodyHeading: Boolean,
    bodyStrike: Boolean,
    textAlignMode: String,
    listMode: String,
    bulletListMode: String,
    numberListMode: String,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onToggleHeader: () -> Unit,
    onStrike: () -> Unit,
    onTextAlignModeChange: (String) -> Unit,
    onListModeChange: (String) -> Unit,
    onBulletListModeChange: (String) -> Unit,
    onNumberListModeChange: (String) -> Unit,
    onPickTextColor: () -> Unit,
    onPickHighlightColor: () -> Unit,
    onAttachImage: () -> Unit,
    onAttachVideo: () -> Unit,
    onAttachAudio: () -> Unit,
    isRecordingAudio: Boolean,
    onToggleRecordAudio: () -> Unit,
    onAddChecklistItem: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(appScaledSpacing(7.dp))
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .horizontalScroll(rememberScrollState()),
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.58f),
            border = BorderStroke(1.dp, appPanelBorderColor().copy(alpha = 0.45f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NoteToolIconButton(Icons.Rounded.FormatBold, selected = bodyBold, contentDescription = "Жирный", onClick = onBold)
                NoteToolIconButton(Icons.Rounded.FormatItalic, selected = bodyItalic, contentDescription = "Курсив", onClick = onItalic)
                NoteToolIconButton(Icons.Rounded.Title, selected = bodyHeading, contentDescription = "Заголовок", onClick = onToggleHeader)
                NoteToolIconButton(Icons.Rounded.FormatStrikethrough, selected = bodyStrike, contentDescription = "Зачёркнутый", onClick = onStrike)
                NoteToolIconButton(Icons.AutoMirrored.Rounded.FormatListBulleted, selected = listMode == "BULLET", contentDescription = "Маркированный список") {
                    onListModeChange(if (listMode == "BULLET") "NONE" else "BULLET")
                }
                NoteToolIconButton(Icons.Rounded.FormatListNumbered, selected = listMode == "NUMBERED", contentDescription = "Нумерованный список") {
                    onListModeChange(if (listMode == "NUMBERED") "NONE" else "NUMBERED")
                }
                NoteToolIconButton(Icons.AutoMirrored.Rounded.FormatAlignLeft, selected = textAlignMode == "START", contentDescription = "По левому краю") {
                    onTextAlignModeChange("START")
                }
                NoteToolIconButton(Icons.Rounded.FormatAlignCenter, selected = textAlignMode == "CENTER", contentDescription = "По центру") {
                    onTextAlignModeChange("CENTER")
                }
                NoteToolIconButton(Icons.AutoMirrored.Rounded.FormatAlignRight, selected = textAlignMode == "END", contentDescription = "По правому краю") {
                    onTextAlignModeChange("END")
                }
                NoteToolIconButton(Icons.Rounded.FormatAlignJustify, selected = textAlignMode == "JUSTIFY", contentDescription = "По ширине") {
                    onTextAlignModeChange("JUSTIFY")
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .horizontalScroll(rememberScrollState()),
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.58f),
            border = BorderStroke(1.dp, appPanelBorderColor().copy(alpha = 0.45f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NoteToolIconButton(Icons.Rounded.FormatColorText, selected = false, contentDescription = "Цвет текста", onClick = onPickTextColor)
                NoteToolIconButton(Icons.Rounded.FormatColorFill, selected = false, contentDescription = "Цвет выделения", onClick = onPickHighlightColor)
                NoteToolIconButton(Icons.Rounded.CheckCircle, selected = false, contentDescription = "Чек-лист", onClick = onAddChecklistItem)
                NoteToolIconButton(Icons.Rounded.Image, selected = false, contentDescription = "Фото", onClick = onAttachImage)
                NoteToolIconButton(Icons.Rounded.Videocam, selected = false, contentDescription = "Видео", onClick = onAttachVideo)
                NoteToolIconButton(Icons.Rounded.AudioFile, selected = false, contentDescription = "Аудиофайл", onClick = onAttachAudio)
                NoteToolIconButton(
                    imageVector = if (isRecordingAudio) Icons.Rounded.Stop else Icons.Rounded.Mic,
                    selected = isRecordingAudio,
                    contentDescription = if (isRecordingAudio) "Остановить запись" else "Записать аудио",
                    onClick = onToggleRecordAudio
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))
        ) {
            if (listMode == "BULLET") {
                SegmentedNoteTools(
                    options = listOf("DOT" to "•", "DASH" to "-", "STAR" to "*", "CHECK" to "☐"),
                    selected = bulletListMode,
                    onSelect = onBulletListModeChange,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (listMode == "NUMBERED") {
                SegmentedNoteTools(
                    options = listOf("DECIMAL" to "1.", "PAREN" to "1)", "LETTER" to "A.", "ROMAN" to "I."),
                    selected = numberListMode,
                    onSelect = onNumberListModeChange,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            NoteTextSizeControl(
                textSizeMode = textSizeMode,
                onTextSizeModeChange = onTextSizeModeChange
            )
            SegmentedNoteTools(
                options = listOf("SANS" to "Sans", "SERIF" to "Serif", "MONO" to "Mono", "CUSTOM" to "Свой"),
                selected = fontMode,
                onSelect = onFontModeChange,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun NoteTextSizeControl(
    textSizeMode: String,
    onTextSizeModeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedPt = noteTextSizePt(textSizeMode)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.48f),
        border = BorderStroke(1.dp, appPanelBorderColor().copy(alpha = 0.40f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = appScaledSpacing(8.dp), vertical = appScaledSpacing(4.dp)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Размер",
                style = MaterialTheme.typography.labelSmall,
                color = appListSecondaryTextColor(),
                modifier = Modifier.padding(start = appScaledSpacing(6.dp))
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(4.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactSizeButton(
                    label = "−",
                    enabled = selectedPt > MIN_NOTE_FONT_PT,
                    onClick = {
                        onTextSizeModeChange((selectedPt - 1).coerceAtLeast(MIN_NOTE_FONT_PT).toString())
                    }
                )
                Text(
                    text = "$selectedPt пт",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                CompactSizeButton(
                    label = "+",
                    enabled = selectedPt < MAX_NOTE_FONT_PT,
                    onClick = {
                        onTextSizeModeChange((selectedPt + 1).coerceAtMost(MAX_NOTE_FONT_PT).toString())
                    }
                )
            }
        }
    }
}

@Composable
private fun CompactSizeButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = appHapticAction(onAction = onClick)),
        shape = CircleShape,
        color = if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.13f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (enabled) MaterialTheme.colorScheme.primary else appListSecondaryTextColor().copy(alpha = 0.55f)
            )
        }
    }
}

@Composable
private fun NoteToolIconButton(
    imageVector: ImageVector,
    selected: Boolean,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .clickable(onClick = appHapticAction(onAction = onClick)),
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent,
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun CompactTextToolButton(
    text: String,
    selected: Boolean,
    fontWeight: FontWeight = FontWeight.SemiBold,
    italic: Boolean = false,
    strike: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = appHapticAction(onAction = onClick)),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent,
        border = if (selected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.34f))
        } else {
            null
        }
    ) {
        Box(
            modifier = Modifier.padding(horizontal = appScaledSpacing(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = fontWeight,
                    fontStyle = if (italic) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                    textDecoration = if (strike) TextDecoration.LineThrough else TextDecoration.None
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SegmentedNoteTools(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.48f),
        border = BorderStroke(1.dp, appPanelBorderColor().copy(alpha = 0.40f))
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { (value, label) ->
                val active = value == selected
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .clickable(onClick = appHapticAction { onSelect(value) }),
                    shape = RoundedCornerShape(999.dp),
                    color = if (active) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.17f)
                    } else {
                        Color.Transparent
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (active) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                appListSecondaryTextColor()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun noteBodyTextStyle(
    textSizeMode: String,
    fontMode: String,
    bold: Boolean = false,
    italic: Boolean = false,
    heading: Boolean = false,
    strike: Boolean = false
): TextStyle {
    val baseFontSize = noteTextSizePt(textSizeMode).sp
    val fontSize = baseFontSize * if (heading) 1.20f else 1f
    val fontFamily = when (fontMode) {
        "SERIF" -> AppSerifFontFamily
        "MONO" -> AppMonoFontFamily
        "CUSTOM" -> MaterialTheme.typography.bodyLarge.fontFamily ?: FontFamily.Default
        else -> AppSansFontFamily
    }
    return MaterialTheme.typography.bodyLarge.copy(
        fontSize = fontSize,
        lineHeight = fontSize * 1.34f,
        fontFamily = fontFamily,
        fontWeight = if (bold || heading) FontWeight.Bold else FontWeight.Normal,
        fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
        textDecoration = if (strike) TextDecoration.LineThrough else TextDecoration.None
    )
}

private const val MIN_NOTE_FONT_PT = 8
private const val MAX_NOTE_FONT_PT = 32

private fun noteTextSizePt(textSizeMode: String): Int {
    return when (textSizeMode) {
        "SMALL" -> 14
        "NORMAL" -> 16
        "LARGE" -> 19
        "XL" -> 22
        else -> textSizeMode.toIntOrNull()?.coerceIn(MIN_NOTE_FONT_PT, MAX_NOTE_FONT_PT) ?: 16
    }
}

private fun buildRichNoteAnnotatedString(
    source: String,
    baseStyle: TextStyle,
    listMode: String = "NONE"
): AnnotatedString {
    return buildAnnotatedString {
        var renderedNumber = 1
        source.lines().forEachIndexed { index, rawLine ->
            if (index > 0) append('\n')
            val line = rawLine.trimEnd()
            val trimmedStart = line.trimStart()
            val numberedMatch = Regex("""^([0-9]+[.)]|[A-Za-z][.)]|[IVXLCDMivxlcdm]+[.)])\s+(.+)$""")
                .find(trimmedStart)
            when {
                trimmedStart.startsWith("- ") ||
                    trimmedStart.startsWith("* ") ||
                    trimmedStart.startsWith("• ") ||
                    trimmedStart.startsWith("☐ ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                        append(trimmedStart.takeWhile { it != ' ' })
                        append(' ')
                    }
                    appendRichInline(trimmedStart.dropWhile { it != ' ' }.trimStart())
                }
                numberedMatch != null -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                        append(numberedMatch.groupValues[1])
                        append(' ')
                    }
                    appendRichInline(numberedMatch.groupValues[2])
                }
                listMode == "BULLET" && line.isNotBlank() -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                        append("• ")
                    }
                    appendRichInline(line.removePrefix("- ").removePrefix("• "))
                }
                listMode == "NUMBERED" && line.isNotBlank() -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                        append("${renderedNumber++}. ")
                    }
                    appendRichInline(line.replace(Regex("^\\d+[.)]\\s+"), ""))
                }
                line.startsWith("## ") -> {
                    withStyle(
                        SpanStyle(
                            fontSize = baseStyle.fontSize * 1.18f,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        appendRichInline(line.removePrefix("## "))
                    }
                }
                line.startsWith("# ") -> {
                    withStyle(
                        SpanStyle(
                            fontSize = baseStyle.fontSize * 1.28f,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        appendRichInline(line.removePrefix("# "))
                    }
                }
                line.startsWith("- ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                        append("• ")
                    }
                    appendRichInline(line.removePrefix("- "))
                }
                else -> appendRichInline(line)
            }
        }
    }
}

private fun AnnotatedString.Builder.appendRichInline(source: String) {
    var index = 0
    while (index < source.length) {
        when {
            source.startsWith("**", index) -> {
                val close = source.indexOf("**", startIndex = index + 2)
                if (close > index + 2) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        appendRichInline(source.substring(index + 2, close))
                    }
                    index = close + 2
                } else {
                    append(source[index])
                    index += 1
                }
            }
            source[index] == '_' -> {
                val close = source.indexOf('_', startIndex = index + 1)
                if (close > index + 1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        appendRichInline(source.substring(index + 1, close))
                    }
                    index = close + 1
                } else {
                    append(source[index])
                    index += 1
                }
            }
            source.startsWith("~~", index) -> {
                val close = source.indexOf("~~", startIndex = index + 2)
                if (close > index + 2) {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        appendRichInline(source.substring(index + 2, close))
                    }
                    index = close + 2
                } else {
                    append(source[index])
                    index += 1
                }
            }
            else -> {
                append(source[index])
                index += 1
            }
        }
    }
}

private fun TextFieldValue.toggleNoteInlineMarkup(prefix: String, suffix: String): TextFieldValue {
    val start = minOf(selection.start, selection.end).coerceIn(0, text.length)
    val end = maxOf(selection.start, selection.end).coerceIn(0, text.length)
    if (start == end) {
        val nextText = text.replaceRange(start, end, prefix + suffix)
        val cursor = start + prefix.length
        return copy(text = nextText, selection = androidx.compose.ui.text.TextRange(cursor))
    }

    val selected = text.substring(start, end)
    val hasWrapper = selected.startsWith(prefix) && selected.endsWith(suffix) && selected.length >= prefix.length + suffix.length
    return if (hasWrapper) {
        val unwrapped = selected.removePrefix(prefix).removeSuffix(suffix)
        val nextText = text.replaceRange(start, end, unwrapped)
        copy(
            text = nextText,
            selection = androidx.compose.ui.text.TextRange(start, start + unwrapped.length)
        )
    } else {
        val wrapped = "$prefix$selected$suffix"
        val nextText = text.replaceRange(start, end, wrapped)
        copy(
            text = nextText,
            selection = androidx.compose.ui.text.TextRange(start + prefix.length, start + prefix.length + selected.length)
        )
    }
}

private fun TextFieldValue.insertNoteMediaMarker(attachmentId: String): TextFieldValue {
    val cursor = maxOf(selection.start, selection.end).coerceIn(0, text.length)
    val marker = "[[media:$attachmentId]]"
    val prefix = if (cursor == 0 || text.getOrNull(cursor - 1) == '\n') "" else "\n"
    val suffix = if (cursor >= text.length || text.getOrNull(cursor) == '\n') "\n" else "\n"
    val insertion = "$prefix$marker$suffix"
    val nextText = text.replaceRange(cursor, cursor, insertion)
    val nextCursor = cursor + insertion.length
    return copy(text = nextText, selection = TextRange(nextCursor))
}

private fun TextFieldValue.removeNoteMediaMarker(attachmentId: String): TextFieldValue {
    val escaped = Regex.escape("[[media:$attachmentId]]")
    val nextText = text
        .replace(Regex("""\n?$escaped\n?"""), "\n")
        .replace(Regex("""\n{3,}"""), "\n\n")
        .trim('\n')
    val nextCursor = minOf(selection.start, nextText.length)
    return copy(text = nextText, selection = TextRange(nextCursor))
}

private fun TextFieldValue.toggleNoteLinePrefix(prefix: String): TextFieldValue {
    return transformSelectedNoteLines { lines ->
        val nonBlank = lines.filter { it.isNotBlank() }
        val removePrefix = nonBlank.isNotEmpty() && nonBlank.all { it.trimStart().startsWith(prefix) }
        lines.map { line ->
            when {
                line.isBlank() -> line
                removePrefix -> line.replaceFirst(prefix, "")
                else -> prefix + line.removePrefix("# ").removePrefix("## ")
            }
        }
    }
}

private fun TextFieldValue.applyNoteListMode(
    nextMode: String,
    bulletMode: String,
    numberMode: String
): TextFieldValue {
    return transformSelectedNoteLines { lines ->
        val cleanedLines = lines.map { line -> line.removeNoteListPrefix() }
        when (nextMode) {
            "BULLET" -> cleanedLines.map { line ->
                if (line.isBlank()) line else "${bulletPrefix(bulletMode)} $line"
            }
            "NUMBERED" -> {
                var number = 1
                cleanedLines.map { line ->
                    if (line.isBlank()) {
                        line
                    } else {
                        "${numberPrefix(number++, numberMode)} $line"
                    }
                }
            }
            else -> cleanedLines
        }
    }
}

private fun TextFieldValue.transformSelectedNoteLines(transform: (List<String>) -> List<String>): TextFieldValue {
    val start = minOf(selection.start, selection.end).coerceIn(0, text.length)
    val end = maxOf(selection.start, selection.end).coerceIn(0, text.length)
    val lineStart = if (start <= 0) {
        0
    } else {
        text.lastIndexOf('\n', start - 1).let { if (it < 0) 0 else it + 1 }
    }
    val lineEnd = text.indexOf('\n', end).let { if (it < 0) text.length else it }
    val block = text.substring(lineStart, lineEnd)
    val transformed = transform(block.split('\n')).joinToString("\n")
    val nextText = text.replaceRange(lineStart, lineEnd, transformed)
    return copy(
        text = nextText,
        selection = androidx.compose.ui.text.TextRange(lineStart, lineStart + transformed.length)
    )
}

private fun String.removeNoteListPrefix(): String {
    return trimStart()
        .removePrefix("- ")
        .removePrefix("* ")
        .removePrefix("• ")
        .removePrefix("☐ ")
        .replace(Regex("""^([0-9]+[.)]|[A-Za-z][.)]|[IVXLCDMivxlcdm]+[.)])\s+"""), "")
}

private fun bulletPrefix(mode: String): String {
    return when (mode) {
        "DASH" -> "-"
        "STAR" -> "*"
        "CHECK" -> "☐"
        else -> "•"
    }
}

private fun numberPrefix(number: Int, mode: String): String {
    return when (mode) {
        "PAREN" -> "$number)"
        "LETTER" -> "${numberToLetters(number)}."
        "ROMAN" -> "${numberToRoman(number)}."
        else -> "$number."
    }
}

private fun numberToLetters(number: Int): String {
    var value = number.coerceAtLeast(1)
    val result = StringBuilder()
    while (value > 0) {
        value -= 1
        result.insert(0, ('A'.code + value % 26).toChar())
        value /= 26
    }
    return result.toString()
}

private fun numberToRoman(number: Int): String {
    var value = number.coerceIn(1, 3999)
    val map = listOf(
        1000 to "M",
        900 to "CM",
        500 to "D",
        400 to "CD",
        100 to "C",
        90 to "XC",
        50 to "L",
        40 to "XL",
        10 to "X",
        9 to "IX",
        5 to "V",
        4 to "IV",
        1 to "I"
    )
    val result = StringBuilder()
    map.forEach { (amount, symbol) ->
        while (value >= amount) {
            result.append(symbol)
            value -= amount
        }
    }
    return result.toString()
}

private fun inferInlineNoteMediaType(attachment: AppNoteAttachment): String {
    val source = "${attachment.label} ${attachment.uri}".lowercase()
    return when {
        source.contains("image/") -> "IMAGE"
        source.contains("photo") || source.contains("image") -> "IMAGE"
        source.endsWith(".jpg") || source.endsWith(".jpeg") || source.endsWith(".png") || source.endsWith(".webp") || source.endsWith(".gif") -> "IMAGE"
        else -> "VIDEO"
    }
}

private fun String.stripLegacyNoteMarkup(): String {
    return lines()
        .joinToString("\n") { rawLine ->
            rawLine
                .removePrefix("## ")
                .removePrefix("# ")
                .removePrefix("- ")
                .replace("**", "")
                .replace("_", "")
                .replace("~~", "")
        }
}

private fun String.removeNoteMediaMarkers(): String {
    return replace(noteMediaMarkerRegex, "")
        .replace(Regex("""\n{3,}"""), "\n\n")
        .trim()
}

private fun String.hasLegacyBold(): Boolean = contains("**")

private fun String.hasLegacyItalic(): Boolean = contains("_")

private fun String.hasLegacyStrike(): Boolean = contains("~~")

private fun String.hasLegacyHeading(): Boolean = lines().any { line ->
    line.startsWith("# ") || line.startsWith("## ")
}

private fun LocalDate.toUtcDatePickerMillis(): Long {
    return atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
}

private fun localDateFromUtcDatePickerMillis(millis: Long): LocalDate {
    return Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
}

@Composable
private fun NoteBackgroundPickerDialog(
    selectedPattern: String,
    colorHex: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pickedPattern by rememberSaveable(selectedPattern) { mutableStateOf(selectedPattern) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(appCornerRadius(28.dp)),
        containerColor = appPanelColor(),
        tonalElevation = 0.dp,
        title = { Text("Фоновый рисунок") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(appScaledSpacing(10.dp))) {
                noteBackgroundPatterns.chunked(2).forEach { rowPatterns ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(10.dp))
                    ) {
                        rowPatterns.forEach { (pattern, label) ->
                            val selected = pattern == pickedPattern
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(76.dp)
                                    .clickable(onClick = appHapticAction { pickedPattern = pattern }),
                                shape = RoundedCornerShape(appCornerRadius(18.dp)),
                                color = noteSurfaceColor(colorHex),
                                border = BorderStroke(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else appPanelBorderColor()
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .notePatternBackground(pattern, colorHex)
                                        .padding(appScaledSpacing(8.dp)),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        if (rowPatterns.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelect(pickedPattern) }) {
                Text("Готово")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}

@Composable
private fun NoteAttachmentSourceDialog(
    target: String,
    onPickMedia: () -> Unit,
    onPickFiles: () -> Unit,
    onDismiss: () -> Unit
) {
    val mediaLabel = when (target) {
        "AUDIO" -> "Аудиотека"
        else -> "Галерея"
    }
    val title = when (target) {
        "IMAGE" -> "Добавить фото"
        "VIDEO" -> "Добавить видео"
        "AUDIO" -> "Добавить аудио"
        else -> "Добавить файл"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(appCornerRadius(28.dp)),
        containerColor = appPanelColor(),
        tonalElevation = 0.dp,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(appScaledSpacing(10.dp))) {
                NoteSourceOption(
                    title = mediaLabel,
                    subtitle = if (target == "AUDIO") "Открыть системный выбор аудио" else "Открыть медиапикер устройства",
                    onClick = onPickMedia
                )
                NoteSourceOption(
                    title = "Файлы",
                    subtitle = "Выбрать через проводник или облачное хранилище",
                    onClick = onPickFiles
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}

@Composable
private fun NoteSourceOption(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = appHapticAction(onAction = onClick)),
        shape = RoundedCornerShape(appCornerRadius(18.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
        border = BorderStroke(1.dp, appPanelBorderColor().copy(alpha = 0.42f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = appScaledSpacing(14.dp), vertical = appScaledSpacing(10.dp)),
            verticalArrangement = Arrangement.spacedBy(appScaledSpacing(2.dp))
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = appListSecondaryTextColor()
            )
        }
    }
}

@Composable
private fun NoteColorPickerDialog(
    title: String,
    selectedColorHex: String,
    allowDefault: Boolean,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedHex by rememberSaveable(title, selectedColorHex) {
        mutableStateOf(selectedColorHex.ifBlank { if (allowDefault) "" else notePalette.first() })
    }
    val initialColorInt = parseColorHex(selectedHex.ifBlank { "#202124" }, 0xFF202124.toInt())
    val initialHsv = remember(title, selectedColorHex) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initialColorInt, it) }
    }
    var hue by rememberSaveable(title, selectedColorHex) { mutableStateOf(initialHsv[0]) }
    var saturation by rememberSaveable(title, selectedColorHex) { mutableStateOf(initialHsv[1].coerceIn(0.18f, 1f)) }
    var value by rememberSaveable(title, selectedColorHex) { mutableStateOf(initialHsv[2].coerceIn(0.18f, 1f)) }
    val previewHex = selectedHex.ifBlank { if (allowDefault) "#202124" else notePalette.first() }
    val previewColor = Color(parseColorHex(previewHex, 0xFF202124.toInt()))
    val pickerColors = remember(allowDefault) {
        val base = if (allowDefault) {
            listOf(
                "#202124", "#5F6368", "#D93025", "#E37400", "#F9AB00", "#188038",
                "#129EAF", "#1A73E8", "#673AB7", "#D81B60", "#FFFFFF", "#000000"
            )
        } else {
            emptyList()
        }
        (base + notePalette).distinctBy { it.uppercase() }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(appCornerRadius(28.dp)),
        containerColor = appPanelColor(),
        tonalElevation = 0.dp,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(appScaledSpacing(12.dp))) {
                if (allowDefault) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = appHapticAction { selectedHex = "" }),
                        shape = RoundedCornerShape(appCornerRadius(16.dp)),
                        color = if (selectedHex.isBlank()) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
                        },
                        border = BorderStroke(
                            width = if (selectedHex.isBlank()) 2.dp else 1.dp,
                            color = if (selectedHex.isBlank()) MaterialTheme.colorScheme.primary else appPanelBorderColor()
                        )
                    ) {
                        Text(
                            text = "По умолчанию",
                            modifier = Modifier.padding(horizontal = appScaledSpacing(14.dp), vertical = appScaledSpacing(10.dp)),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(appCornerRadius(18.dp)),
                    color = previewColor,
                    border = BorderStroke(1.dp, appPanelBorderColor())
                ) {
                    Text(
                        text = if (allowDefault && selectedHex.isBlank()) "Цвет темы" else "Выбранный цвет",
                        modifier = Modifier.padding(appScaledSpacing(14.dp)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (previewColor.luminance() > 0.55f) Color.Black else Color.White
                    )
                }
                NoteHsvSlider("Тон", hue, 0f..360f) {
                    hue = it
                    selectedHex = hsvToHex(hue, saturation, value)
                }
                NoteHsvSlider("Насыщ.", saturation, 0f..1f) {
                    saturation = it
                    selectedHex = hsvToHex(hue, saturation, value)
                }
                NoteHsvSlider("Яркость", value, 0f..1f) {
                    value = it
                    selectedHex = hsvToHex(hue, saturation, value)
                }
                Column(verticalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))) {
                    pickerColors.chunked(6).forEach { rowColors ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            rowColors.forEach { colorHex ->
                                val selected = colorHex.equals(selectedHex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(if (selected) 34.dp else 30.dp)
                                        .clip(CircleShape)
                                        .background(Color(parseColorHex(colorHex, 0xFFFFF3BF.toInt())))
                                        .border(
                                            width = if (selected) 3.dp else 1.dp,
                                            color = if (selected) MaterialTheme.colorScheme.primary else appPanelBorderColor(),
                                            shape = CircleShape
                                        )
                                        .clickable(onClick = appHapticAction {
                                            selectedHex = colorHex
                                            val picked = parseColorHex(colorHex, 0xFF202124.toInt())
                                            FloatArray(3).also { hsv ->
                                                android.graphics.Color.colorToHSV(picked, hsv)
                                                hue = hsv[0]
                                                saturation = hsv[1].coerceIn(0.18f, 1f)
                                                value = hsv[2].coerceIn(0.18f, 1f)
                                            }
                                        })
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelect(selectedHex) }) {
                Text("Готово")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}

@Composable
private fun NoteHsvSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(10.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = appListSecondaryTextColor(),
            modifier = Modifier.weight(0.34f)
        )
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun hsvToHex(hue: Float, saturation: Float, value: Float): String {
    val color = android.graphics.Color.HSVToColor(
        floatArrayOf(
            hue.coerceIn(0f, 360f),
            saturation.coerceIn(0f, 1f),
            value.coerceIn(0f, 1f)
        )
    )
    return "#%02X%02X%02X".format(
        android.graphics.Color.red(color),
        android.graphics.Color.green(color),
        android.graphics.Color.blue(color)
    )
}

@Composable
private fun NoteAttachmentsEditor(
    attachments: List<AppNoteAttachment>,
    onUpdate: (AppNoteAttachment) -> Unit,
    onRemove: (String) -> Unit
) {
    val context = LocalContext.current
    var activePlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var playingAttachmentId by remember { mutableStateOf<String?>(null) }
    var playbackPositionMs by remember { mutableStateOf(0) }
    var playbackDurationMs by remember { mutableStateOf(0) }
    var pendingAudioSave by remember { mutableStateOf<AppNoteAttachment?>(null) }
    val saveAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("audio/mp4")
    ) { uri ->
        val attachment = pendingAudioSave
        pendingAudioSave = null
        if (uri != null && attachment != null) {
            context.copyNoteAttachmentToUri(attachment, uri)
        }
    }

    fun stopPlayback() {
        activePlayer?.let { player ->
            runCatching { player.stop() }
            runCatching { player.release() }
        }
        activePlayer = null
        playingAttachmentId = null
        playbackPositionMs = 0
        playbackDurationMs = 0
    }

    fun playAttachment(attachment: AppNoteAttachment) {
        if (playingAttachmentId == attachment.id) {
            stopPlayback()
            return
        }
        stopPlayback()
        runCatching {
            MediaPlayer().apply {
                setDataSource(context, Uri.parse(attachment.uri))
                setOnCompletionListener { stopPlayback() }
                prepare()
                playbackDurationMs = duration.coerceAtLeast(0)
                playbackPositionMs = 0
                start()
                activePlayer = this
                playingAttachmentId = attachment.id
            }
        }.onFailure {
            stopPlayback()
        }
    }

    DisposableEffect(Unit) {
        onDispose { stopPlayback() }
    }

    LaunchedEffect(playingAttachmentId, activePlayer) {
        while (activePlayer != null && playingAttachmentId != null) {
            activePlayer?.let { player ->
                if (runCatching { player.isPlaying }.getOrDefault(false)) {
                    playbackPositionMs = runCatching { player.currentPosition }.getOrDefault(playbackPositionMs)
                    playbackDurationMs = runCatching { player.duration }.getOrDefault(playbackDurationMs).coerceAtLeast(playbackDurationMs)
                }
            }
            delay(250)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))) {
        attachments.forEach { attachment ->
            val isPlaying = playingAttachmentId == attachment.id
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(appCornerRadius(14.dp)),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.42f),
                border = BorderStroke(1.dp, appPanelBorderColor().copy(alpha = 0.38f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = appScaledSpacing(10.dp), vertical = appScaledSpacing(7.dp)),
                    verticalArrangement = Arrangement.spacedBy(appScaledSpacing(4.dp))
                ) {
                    when (attachment.type) {
                        "IMAGE" -> EmbeddedNoteImage(
                            attachment = attachment,
                            aspectRatio = noteAttachmentAspectRatio(attachment),
                            onOpen = { context.openNoteAttachmentExternally(attachment) }
                        )
                        "VIDEO" -> EmbeddedNoteVideo(
                            attachment = attachment,
                            aspectRatio = noteAttachmentAspectRatio(attachment),
                            onOpen = { context.openNoteAttachmentExternally(attachment) }
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (attachment.type) {
                                "IMAGE" -> Icons.Rounded.Image
                                "VIDEO" -> Icons.Rounded.Videocam
                                "AUDIO" -> Icons.Rounded.AudioFile
                                else -> Icons.Rounded.AttachFile
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = attachment.label.ifBlank { attachment.type.lowercase() },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Text(
                                text = when (attachment.type) {
                                    "IMAGE" -> "Фото"
                                    "VIDEO" -> "Видео"
                                    "AUDIO" -> "Аудио"
                                    else -> "Файл"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = appListSecondaryTextColor()
                            )
                        }
                        if (attachment.type == "AUDIO") {
                            IconButton(
                                onClick = appHapticAction { playAttachment(attachment) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                    contentDescription = if (isPlaying) "Остановить" else "Прослушать",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = appHapticAction {
                                    pendingAudioSave = attachment
                                    saveAudioLauncher.launch(attachment.suggestedAudioFileName())
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Save,
                                    contentDescription = "Сохранить аудио",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                        if (attachment.type == "IMAGE" || attachment.type == "VIDEO") {
                            IconButton(
                                onClick = appHapticAction { context.openNoteAttachmentExternally(attachment) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.OpenInNew,
                                    contentDescription = "Открыть",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = appHapticAction { onRemove(attachment.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Rounded.RemoveCircleOutline,
                                contentDescription = "Удалить вложение",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (attachment.type == "IMAGE" || attachment.type == "VIDEO") {
                        NoteAttachmentSizePicker(
                            selected = attachment.displaySizeMode,
                            onSelect = { sizeMode -> onUpdate(attachment.copy(displaySizeMode = sizeMode)) }
                        )
                    }
                    if (attachment.type == "AUDIO" && isPlaying) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatAudioPosition(playbackPositionMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = appListSecondaryTextColor()
                            )
                            Slider(
                                value = playbackPositionMs.toFloat().coerceIn(0f, playbackDurationMs.coerceAtLeast(1).toFloat()),
                                onValueChange = { position ->
                                    val nextPosition = position.toInt()
                                    playbackPositionMs = nextPosition
                                    activePlayer?.let { player ->
                                        runCatching { player.seekTo(nextPosition) }
                                    }
                                },
                                valueRange = 0f..playbackDurationMs.coerceAtLeast(1).toFloat(),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = formatAudioPosition(playbackDurationMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = appListSecondaryTextColor()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmbeddedNoteImage(
    attachment: AppNoteAttachment,
    aspectRatio: Float,
    onOpen: () -> Unit
) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio.coerceIn(0.32f, 3.2f))
            .clip(RoundedCornerShape(appCornerRadius(14.dp)))
            .clickable(onClick = appHapticAction(onAction = onOpen)),
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                adjustViewBounds = true
            }
        },
        update = { imageView ->
            imageView.setImageURI(Uri.parse(attachment.uri))
        }
    )
}

@Composable
private fun EmbeddedNoteVideo(
    attachment: AppNoteAttachment,
    aspectRatio: Float,
    onOpen: () -> Unit
) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio.coerceIn(0.32f, 3.2f))
            .clip(RoundedCornerShape(appCornerRadius(14.dp)))
            .clickable(onClick = appHapticAction(onAction = onOpen)),
        factory = { context ->
            VideoView(context).apply {
                val controller = MediaController(context)
                controller.setAnchorView(this)
                setMediaController(controller)
                setVideoURI(Uri.parse(attachment.uri))
                seekTo(1)
            }
        },
        update = { videoView ->
            val parsedUri = Uri.parse(attachment.uri)
            if (videoView.tag != attachment.uri) {
                videoView.tag = attachment.uri
                videoView.setVideoURI(parsedUri)
                videoView.seekTo(1)
            }
        }
    )
}

@Composable
private fun NoteAttachmentSizePicker(
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Размер",
            style = MaterialTheme.typography.labelSmall,
            color = appListSecondaryTextColor()
        )
        listOf("SMALL" to "S", "MEDIUM" to "M", "LARGE" to "L").forEach { (value, label) ->
            Surface(
                modifier = Modifier
                    .height(28.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = appHapticAction { onSelect(value) }),
                shape = RoundedCornerShape(999.dp),
                color = if (selected == value) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.48f),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (selected == value) MaterialTheme.colorScheme.primary.copy(alpha = 0.42f) else appPanelBorderColor().copy(alpha = 0.38f)
                )
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = appScaledSpacing(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (selected == value) MaterialTheme.colorScheme.primary else appListSecondaryTextColor()
                    )
                }
            }
        }
    }
}

private fun noteAttachmentWidthFraction(attachment: AppNoteAttachment): Float {
    if (attachment.widthFraction > 0f) return attachment.widthFraction.coerceIn(0.42f, 1f)
    return when (attachment.displaySizeMode) {
        "SMALL" -> 0.58f
        "LARGE" -> 1f
        else -> 0.82f
    }
}

private fun noteAttachmentAspectRatio(attachment: AppNoteAttachment): Float {
    return attachment.aspectRatio.takeIf { it.isFinite() && it > 0f }
        ?.coerceIn(0.32f, 3.2f)
        ?: (16f / 9f)
}

@Composable
private fun noteTextColor(colorHex: String): Color {
    return if (colorHex.isBlank()) {
        MaterialTheme.colorScheme.onSurface
    } else {
        Color(parseColorHex(colorHex, MaterialTheme.colorScheme.onSurface.toArgb()))
    }
}

@Composable
private fun noteHighlightColor(colorHex: String): Color {
    return if (colorHex.isBlank()) {
        Color.Transparent
    } else {
        Color(parseColorHex(colorHex, 0x00FFFFFF)).copy(alpha = 0.34f)
    }
}

private fun noteTextAlign(mode: String): TextAlign {
    return when (mode) {
        "CENTER" -> TextAlign.Center
        "END" -> TextAlign.End
        "JUSTIFY" -> TextAlign.Justify
        else -> TextAlign.Start
    }
}

private fun formatAudioPosition(positionMs: Int): String {
    val totalSeconds = (positionMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun AppNoteAttachment.suggestedAudioFileName(): String {
    val baseName = label
        .ifBlank { "note_audio" }
        .replace(Regex("""[\\/:*?"<>|]"""), "_")
        .trim()
        .ifBlank { "note_audio" }
    val extension = when {
        baseName.endsWith(".m4a", ignoreCase = true) -> ""
        baseName.endsWith(".mp4", ignoreCase = true) -> ""
        baseName.endsWith(".aac", ignoreCase = true) -> ""
        else -> ".m4a"
    }
    return "$baseName$extension"
}

private fun Context.copyNoteAttachmentToUri(attachment: AppNoteAttachment, targetUri: Uri) {
    runCatching {
        contentResolver.openInputStream(Uri.parse(attachment.uri))?.use { input ->
            contentResolver.openOutputStream(targetUri)?.use { output ->
                input.copyTo(output)
            }
        }
    }
}

private fun Context.openNoteAttachmentExternally(attachment: AppNoteAttachment) {
    val uri = Uri.parse(attachment.uri)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, attachment.mimeType())
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching {
        startActivity(Intent.createChooser(intent, "Открыть"))
    }
}

private fun AppNoteAttachment.mimeType(): String {
    return when (type) {
        "IMAGE" -> "image/*"
        "VIDEO" -> "video/*"
        "AUDIO" -> "audio/*"
        else -> "*/*"
    }
}

private fun Context.createNoteAudioFile(): File {
    val directory = File(filesDir, "note_audio").apply { mkdirs() }
    return File(directory, "note_audio_${System.currentTimeMillis()}.m4a")
}

@Suppress("DEPRECATION")
private fun createNoteMediaRecorder(outputFile: File): MediaRecorder {
    return MediaRecorder().apply {
        setAudioSource(MediaRecorder.AudioSource.MIC)
        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        setAudioSamplingRate(44_100)
        setAudioEncodingBitRate(96_000)
        setOutputFile(outputFile.absolutePath)
    }
}

private fun Context.takePersistableReadPermission(uri: Uri) {
    runCatching {
        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

private fun Context.resolveNoteAttachmentType(uri: Uri): String {
    val mime = contentResolver.getType(uri).orEmpty()
    return when {
        mime.startsWith("image/") -> "IMAGE"
        mime.startsWith("video/") -> "VIDEO"
        mime.startsWith("audio/") -> "AUDIO"
        else -> "FILE"
    }
}

private fun Context.resolveNoteAttachmentAspectRatio(uri: Uri, type: String): Float {
    val ratio = when (type) {
        "IMAGE" -> resolveImageAspectRatio(uri)
        "VIDEO" -> resolveVideoAspectRatio(uri)
        else -> 0f
    }
    return ratio.takeIf { it.isFinite() && it > 0f }?.coerceIn(0.32f, 3.2f) ?: (16f / 9f)
}

private fun Context.resolveImageAspectRatio(uri: Uri): Float {
    return runCatching {
        contentResolver.openInputStream(uri)?.use { input ->
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(input, null, options)
            val width = options.outWidth
            val height = options.outHeight
            if (width > 0 && height > 0) width.toFloat() / height.toFloat() else 0f
        } ?: 0f
    }.getOrDefault(0f)
}

private fun Context.resolveVideoAspectRatio(uri: Uri): Float {
    return runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(this, uri)
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toFloatOrNull()
                ?: 0f
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toFloatOrNull()
                ?: 0f
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                ?.toIntOrNull()
                ?: 0
            if (width <= 0f || height <= 0f) {
                0f
            } else if (rotation == 90 || rotation == 270) {
                height / width
            } else {
                width / height
            }
        } finally {
            retriever.release()
        }
    }.getOrDefault(0f)
}

private fun Context.resolveNoteAttachmentLabel(uri: Uri): String {
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) return cursor.getString(index).orEmpty().ifBlank { "Вложение" }
        }
    }
    return uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { null } ?: "Вложение"
}

private fun Modifier.notePatternBackground(pattern: String, colorHex: String): Modifier {
    if (pattern == "NONE") return this
    return drawBehind {
        val baseColor = Color(parseColorHex(colorHex, 0xFFFFF3BF.toInt()))
        val patternInk = if (baseColor.luminance() > 0.55f) Color.Black else Color.White
        val ink = patternInk.copy(alpha = 0.12f)
        val strongInk = patternInk.copy(alpha = 0.18f)
        when (pattern) {
            "DOTS" -> {
                val step = 18.dp.toPx()
                val radius = 1.7.dp.toPx()
                var y = step / 2f
                while (y < size.height) {
                    var x = step / 2f
                    while (x < size.width) {
                        drawCircle(color = ink, radius = radius, center = Offset(x, y))
                        x += step
                    }
                    y += step
                }
            }
            "GRID" -> {
                val step = 22.dp.toPx()
                var x = step
                while (x < size.width) {
                    drawLine(color = ink, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 1.dp.toPx())
                    x += step
                }
                var y = step
                while (y < size.height) {
                    drawLine(color = ink, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1.dp.toPx())
                    y += step
                }
            }
            "LINES" -> {
                val step = 24.dp.toPx()
                var startX = -size.height
                while (startX < size.width) {
                    drawLine(
                        color = ink,
                        start = Offset(startX, size.height),
                        end = Offset(startX + size.height, 0f),
                        strokeWidth = 2.dp.toPx()
                    )
                    startX += step
                }
            }
            "BLOOM" -> {
                val blooms = listOf(
                    Offset(size.width * 0.18f, size.height * 0.18f),
                    Offset(size.width * 0.82f, size.height * 0.28f),
                    Offset(size.width * 0.28f, size.height * 0.78f),
                    Offset(size.width * 0.76f, size.height * 0.82f)
                )
                blooms.forEachIndexed { index, center ->
                    val radius = if (index % 2 == 0) 16.dp.toPx() else 12.dp.toPx()
                    repeat(6) { petal ->
                        val angle = Math.PI * 2.0 * petal / 6.0
                        val petalCenter = Offset(
                            x = center.x + kotlin.math.cos(angle).toFloat() * radius * 0.58f,
                            y = center.y + kotlin.math.sin(angle).toFloat() * radius * 0.58f
                        )
                        drawCircle(color = ink, radius = radius * 0.38f, center = petalCenter)
                    }
                    drawCircle(color = strongInk, radius = radius * 0.22f, center = center)
                }
            }
            "WAVES" -> {
                val amplitude = 5.dp.toPx()
                val stepX = 10.dp.toPx()
                val rowStep = 22.dp.toPx()
                var y = rowStep / 2f
                while (y < size.height) {
                    var x = 0f
                    var previous = Offset(0f, y)
                    while (x <= size.width + stepX) {
                        val next = Offset(
                            x = x,
                            y = y + kotlin.math.sin((x / stepX) * 0.8f).toFloat() * amplitude
                        )
                        drawLine(color = ink, start = previous, end = next, strokeWidth = 1.5.dp.toPx())
                        previous = next
                        x += stepX
                    }
                    y += rowStep
                }
            }
            "STARS" -> {
                val step = 34.dp.toPx()
                val radius = 5.dp.toPx()
                var y = step / 2f
                var row = 0
                while (y < size.height) {
                    var x = step / 2f + if (row % 2 == 0) 0f else step / 2f
                    while (x < size.width) {
                        drawLine(color = ink, start = Offset(x - radius, y), end = Offset(x + radius, y), strokeWidth = 1.5.dp.toPx())
                        drawLine(color = ink, start = Offset(x, y - radius), end = Offset(x, y + radius), strokeWidth = 1.5.dp.toPx())
                        drawLine(color = strongInk, start = Offset(x - radius * 0.6f, y - radius * 0.6f), end = Offset(x + radius * 0.6f, y + radius * 0.6f), strokeWidth = 1.dp.toPx())
                        drawLine(color = strongInk, start = Offset(x + radius * 0.6f, y - radius * 0.6f), end = Offset(x - radius * 0.6f, y + radius * 0.6f), strokeWidth = 1.dp.toPx())
                        x += step
                    }
                    row += 1
                    y += step
                }
            }
            "CONFETTI" -> {
                val step = 26.dp.toPx()
                val colors = listOf(ink, strongInk, patternInk.copy(alpha = 0.08f))
                var y = step / 2f
                var row = 0
                while (y < size.height) {
                    var x = step / 2f
                    var column = 0
                    while (x < size.width) {
                        val shift = ((row * 17 + column * 11) % 9).dp.toPx()
                        val center = Offset(x + shift * 0.45f, y - shift * 0.35f)
                        val color = colors[(row + column) % colors.size]
                        if ((row + column) % 2 == 0) {
                            drawCircle(color = color, radius = 2.5.dp.toPx(), center = center)
                        } else {
                            drawLine(
                                color = color,
                                start = Offset(center.x - 4.dp.toPx(), center.y - 2.dp.toPx()),
                                end = Offset(center.x + 4.dp.toPx(), center.y + 2.dp.toPx()),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                        column += 1
                        x += step
                    }
                    row += 1
                    y += step
                }
            }
            "CHECKER" -> {
                val cell = 22.dp.toPx()
                var y = 0f
                var row = 0
                while (y < size.height) {
                    var x = 0f
                    var column = 0
                    while (x < size.width) {
                        if ((row + column) % 2 == 0) {
                            drawRect(
                                color = patternInk.copy(alpha = 0.06f),
                                topLeft = Offset(x, y),
                                size = Size(cell, cell)
                            )
                        }
                        column += 1
                        x += cell
                    }
                    row += 1
                    y += cell
                }
            }
            "NOTEBOOK" -> {
                val lineStep = 24.dp.toPx()
                var y = lineStep
                while (y < size.height) {
                    drawLine(color = ink, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1.dp.toPx())
                    y += lineStep
                }
                val margin = 28.dp.toPx()
                drawLine(color = strongInk, start = Offset(margin, 0f), end = Offset(margin, size.height), strokeWidth = 1.dp.toPx())
            }
            "CORNERS" -> {
                val gap = 8.dp.toPx()
                val length = 34.dp.toPx()
                val stroke = 2.dp.toPx()
                val corners = listOf(
                    Offset(gap, gap) to Pair(1f, 1f),
                    Offset(size.width - gap, gap) to Pair(-1f, 1f),
                    Offset(gap, size.height - gap) to Pair(1f, -1f),
                    Offset(size.width - gap, size.height - gap) to Pair(-1f, -1f)
                )
                corners.forEach { (origin, direction) ->
                    drawLine(
                        color = strongInk,
                        start = origin,
                        end = Offset(origin.x + length * direction.first, origin.y),
                        strokeWidth = stroke
                    )
                    drawLine(
                        color = strongInk,
                        start = origin,
                        end = Offset(origin.x, origin.y + length * direction.second),
                        strokeWidth = stroke
                    )
                }
            }
            "RAIN" -> {
                val step = 24.dp.toPx()
                var y = -step
                var row = 0
                while (y < size.height + step) {
                    var x = if (row % 2 == 0) step / 2f else 0f
                    while (x < size.width) {
                        drawLine(
                            color = ink,
                            start = Offset(x, y),
                            end = Offset(x - 7.dp.toPx(), y + 14.dp.toPx()),
                            strokeWidth = 1.6.dp.toPx()
                        )
                        x += step
                    }
                    row += 1
                    y += step
                }
            }
        }
    }
}

@Composable
private fun noteSurfaceColor(colorHex: String): Color {
    val color = Color(parseColorHex(colorHex, 0xFFFFF3BF.toInt()))
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.45f
    return if (dark) color.copy(alpha = 0.20f) else color.copy(alpha = 0.96f)
}

@Composable
private fun noteBorderColor(colorHex: String): Color {
    val color = Color(parseColorHex(colorHex, 0xFFFFF3BF.toInt()))
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.45f
    return if (dark) color.copy(alpha = 0.46f) else color.copy(alpha = 0.86f)
}
