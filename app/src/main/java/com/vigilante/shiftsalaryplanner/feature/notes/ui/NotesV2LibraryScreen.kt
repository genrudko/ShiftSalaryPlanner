package com.vigilante.shiftsalaryplanner.feature.notes.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteBlock
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteContentType
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteDocument
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteLibraryLocation
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteLibraryQuery
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteLibraryQueryEngine
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException


enum class NotesV2LayoutMode { GRID, LIST }

@Composable
fun NotesV2LibraryScreen(
    documents: List<NoteDocument>,
    initialQueryText: String = "",
    initialLocation: NoteLibraryLocation = NoteLibraryLocation.ACTIVE,
    initialLayoutMode: NotesV2LayoutMode = NotesV2LayoutMode.GRID,
    onCreateNote: () -> Unit,
    onCreateChecklist: () -> Unit,
    onCreateAudio: () -> Unit,
    onAddImage: () -> Unit,
    onOpenNote: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var queryText by rememberSaveable { mutableStateOf(initialQueryText) }
    var locationName by rememberSaveable { mutableStateOf(initialLocation.name) }
    var layoutName by rememberSaveable { mutableStateOf(initialLayoutMode.name) }
    var contentTypeName by rememberSaveable { mutableStateOf<String?>(null) }

    val location = runCatching { NoteLibraryLocation.valueOf(locationName) }.getOrDefault(NoteLibraryLocation.ACTIVE)
    val layoutMode = runCatching { NotesV2LayoutMode.valueOf(layoutName) }.getOrDefault(NotesV2LayoutMode.GRID)
    val contentType = contentTypeName?.let { runCatching { NoteContentType.valueOf(it) }.getOrNull() }
    val filtered = remember(documents, queryText, location, contentType) {
        NoteLibraryQueryEngine.query(
            notes = documents,
            query = NoteLibraryQuery(
                text = queryText,
                contentTypes = contentType?.let(::setOf).orEmpty(),
                location = location,
            ),
        )
    }
    val pinned = remember(filtered) { filtered.filter { it.pinned } }
    val regular = remember(filtered) { filtered.filterNot { it.pinned } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = appScreenPadding()),
        verticalArrangement = Arrangement.spacedBy(appScaledSpacing(10.dp)),
    ) {
        Spacer(modifier = Modifier.height(appScaledSpacing(6.dp)))
        NotesV2Header(
            count = filtered.size,
            location = location,
            layoutMode = layoutMode,
            onLayoutModeChange = { layoutName = it.name },
        )
        OutlinedTextField(
            value = queryText,
            onValueChange = { queryText = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(appCornerRadius(22.dp)),
            placeholder = { Text("Поиск в заметках") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = if (queryText.isNotEmpty()) {
                {
                    IconButton(onClick = appHapticAction { queryText = "" }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Очистить поиск")
                    }
                }
            } else null,
        )

        val useLargeFontLocationLayout = LocalDensity.current.fontScale >= 1.2f
        if (useLargeFontLocationLayout) {
            Column(verticalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(7.dp)),
                ) {
                    LocationChip("Заметки", NoteLibraryLocation.ACTIVE, location, { locationName = it.name }, Modifier.weight(1f))
                    LocationChip("Архив", NoteLibraryLocation.ARCHIVE, location, { locationName = it.name }, Modifier.weight(1f))
                }
                LocationChip("Корзина", NoteLibraryLocation.TRASH, location, { locationName = it.name }, Modifier.fillMaxWidth())
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(7.dp)),
            ) {
                LocationChip("Заметки", NoteLibraryLocation.ACTIVE, location, { locationName = it.name }, Modifier.weight(1f))
                LocationChip("Архив", NoteLibraryLocation.ARCHIVE, location, { locationName = it.name }, Modifier.weight(1f))
                LocationChip("Корзина", NoteLibraryLocation.TRASH, location, { locationName = it.name }, Modifier.weight(1f))
            }
        }

        if (location == NoteLibraryLocation.ACTIVE) {
            QuickCreateRow(
                onCreateNote = onCreateNote,
                onCreateChecklist = onCreateChecklist,
                onCreateAudio = onCreateAudio,
                onAddImage = onAddImage,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(appScaledSpacing(5.dp))) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(7.dp)),
            ) {
                ContentFilterChip("Все", null, contentType, { contentTypeName = null }, Modifier.weight(1f))
                ContentFilterChip("Списки", NoteContentType.CHECKLIST, contentType, { contentTypeName = it?.name }, Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(7.dp)),
            ) {
                ContentFilterChip("Фото", NoteContentType.IMAGE, contentType, { contentTypeName = it?.name }, Modifier.weight(1f))
                ContentFilterChip("Аудио", NoteContentType.AUDIO, contentType, { contentTypeName = it?.name }, Modifier.weight(1f))
                ContentFilterChip("Видео", NoteContentType.VIDEO, contentType, { contentTypeName = it?.name }, Modifier.weight(1f))
            }
        }

        if (filtered.isEmpty()) {
            NotesEmptyState(
                location = location,
                searching = queryText.isNotBlank() || contentType != null,
                onCreateNote = onCreateNote,
                modifier = Modifier.weight(1f),
            )
        } else if (layoutMode == NotesV2LayoutMode.GRID) {
            NotesGrid(
                pinned = pinned,
                regular = regular,
                onOpenNote = onOpenNote,
                modifier = Modifier.weight(1f),
            )
        } else {
            NotesList(
                pinned = pinned,
                regular = regular,
                onOpenNote = onOpenNote,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun NotesV2Header(
    count: Int,
    location: NoteLibraryLocation,
    layoutMode: NotesV2LayoutMode,
    onLayoutModeChange: (NotesV2LayoutMode) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = when (location) {
                    NoteLibraryLocation.ACTIVE -> "Заметки"
                    NoteLibraryLocation.ARCHIVE -> "Архив"
                    NoteLibraryLocation.TRASH -> "Корзина"
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "$count ${noteCountWord(count)}",
                style = MaterialTheme.typography.bodySmall,
                color = appListSecondaryTextColor(),
            )
        }
        IconButton(onClick = appHapticAction { onLayoutModeChange(NotesV2LayoutMode.GRID) }) {
            Icon(
                Icons.Rounded.GridView,
                contentDescription = "Сетка",
                tint = if (layoutMode == NotesV2LayoutMode.GRID) MaterialTheme.colorScheme.primary else appListSecondaryTextColor(),
            )
        }
        IconButton(onClick = appHapticAction { onLayoutModeChange(NotesV2LayoutMode.LIST) }) {
            Icon(
                Icons.Rounded.ViewAgenda,
                contentDescription = "Список",
                tint = if (layoutMode == NotesV2LayoutMode.LIST) MaterialTheme.colorScheme.primary else appListSecondaryTextColor(),
            )
        }
    }
}

@Composable
private fun LocationChip(
    label: String,
    value: NoteLibraryLocation,
    selected: NoteLibraryLocation,
    onSelect: (NoteLibraryLocation) -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        modifier = modifier,
        selected = value == selected,
        onClick = appHapticAction { onSelect(value) },
        label = { Text(label) },
        leadingIcon = when (value) {
            NoteLibraryLocation.ACTIVE -> ({ Icon(Icons.Rounded.Notes, contentDescription = null, modifier = Modifier.size(18.dp)) })
            NoteLibraryLocation.ARCHIVE -> ({ Icon(Icons.Rounded.Archive, contentDescription = null, modifier = Modifier.size(18.dp)) })
            NoteLibraryLocation.TRASH -> ({ Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp)) })
        },
    )
}

@Composable
private fun ContentFilterChip(
    label: String,
    value: NoteContentType?,
    selected: NoteContentType?,
    onSelect: (NoteContentType?) -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        modifier = modifier,
        selected = value == selected,
        onClick = appHapticAction { onSelect(value) },
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
    )
}

@Composable
private fun QuickCreateRow(
    onCreateNote: () -> Unit,
    onCreateChecklist: () -> Unit,
    onCreateAudio: () -> Unit,
    onAddImage: () -> Unit,
) {
    AppExpressiveSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(appCornerRadius(22.dp)),
        tone = AppExpressiveSurfaceTone.SOFT,
        border = BorderStroke(1.dp, appPanelBorderColor()),
    ) {
        Column(
            modifier = Modifier.padding(appScaledSpacing(8.dp)),
            verticalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp)),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp)),
            ) {
                QuickCreateAction("Текст", Icons.Rounded.Add, onCreateNote, Modifier.weight(1f))
                QuickCreateAction("Список", Icons.Rounded.CheckBox, onCreateChecklist, Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp)),
            ) {
                QuickCreateAction("Голос", Icons.Rounded.Mic, onCreateAudio, Modifier.weight(1f))
                QuickCreateAction("Фото", Icons.Rounded.Image, onAddImage, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun QuickCreateAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = appHapticAction(onAction = onClick)),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.64f),
        border = BorderStroke(1.dp, appPanelBorderColor().copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = appScaledSpacing(11.dp), vertical = appScaledSpacing(7.dp)),
            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun NotesGrid(
    pinned: List<NoteDocument>,
    regular: List<NoteDocument>,
    onOpenNote: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 158.dp),
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(10.dp)),
        verticalArrangement = Arrangement.spacedBy(appScaledSpacing(10.dp)),
    ) {
        if (pinned.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle("Закреплённые") }
            items(pinned, key = { "pin-${it.id}" }) { note ->
                NoteDocumentCard(note, onOpenNote, compact = true)
            }
        }
        if (regular.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle(if (pinned.isEmpty()) "Все заметки" else "Остальные") }
            items(regular, key = { it.id }) { note ->
                NoteDocumentCard(note, onOpenNote, compact = true)
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(appScaledSpacing(96.dp))) }
    }
}

@Composable
private fun NotesList(
    pinned: List<NoteDocument>,
    regular: List<NoteDocument>,
    onOpenNote: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(appScaledSpacing(9.dp)),
    ) {
        if (pinned.isNotEmpty()) {
            item { SectionTitle("Закреплённые") }
            items(pinned, key = { "pin-${it.id}" }) { note -> NoteDocumentCard(note, onOpenNote, compact = false) }
        }
        if (regular.isNotEmpty()) {
            item { SectionTitle(if (pinned.isEmpty()) "Все заметки" else "Остальные") }
            items(regular, key = { it.id }) { note -> NoteDocumentCard(note, onOpenNote, compact = false) }
        }
        item { Spacer(Modifier.height(appScaledSpacing(96.dp))) }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(top = appScaledSpacing(4.dp), bottom = appScaledSpacing(2.dp)),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = appListSecondaryTextColor(),
    )
}

@Composable
private fun NoteDocumentCard(
    note: NoteDocument,
    onOpenNote: (String) -> Unit,
    compact: Boolean,
) {
    val isDark = isSystemInDarkTheme()
    val sourceColor = remember(note.appearance.colorHex) { parseNoteColor(note.appearance.colorHex) }
    val cardColor = if (isDark) {
        lerp(MaterialTheme.colorScheme.surface, sourceColor, 0.2f)
    } else {
        lerp(MaterialTheme.colorScheme.surface, sourceColor, 0.72f)
    }
    val preview = remember(note.blocks) { note.previewText() }
    val checklist = note.blocks.filterIsInstance<NoteBlock.Checklist>().flatMap { it.items }
    val mediaKinds = remember(note.blocks) { note.mediaKinds() }
    val contextText = remember(note.context) { note.contextSummary() }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .notePattern(note.appearance.backgroundPattern, cardColor)
            .clickable(onClick = appHapticAction { onOpenNote(note.id) }),
        shape = RoundedCornerShape(appCornerRadius(20.dp)),
        color = cardColor,
        tonalElevation = if (note.pinned) 2.dp else 0.dp,
        border = BorderStroke(1.dp, appPanelBorderColor().copy(alpha = 0.38f)),
    ) {
        Column(
            modifier = Modifier.padding(appCardPadding()),
            verticalArrangement = Arrangement.spacedBy(appScaledSpacing(if (compact) 7.dp else 9.dp)),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp)),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = note.title.ifBlank { "Без названия" },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = if (compact) 2 else 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (note.pinned) {
                    Icon(Icons.Rounded.PushPin, contentDescription = "Закреплено", modifier = Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }

            if (preview.isNotBlank()) {
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (compact) 4 else 6,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (checklist.isNotEmpty()) {
                checklist.take(if (compact) 3 else 4).forEach { item ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp)),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            if (item.checked) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp),
                            tint = if (item.checked) MaterialTheme.colorScheme.primary else appListSecondaryTextColor(),
                        )
                        Text(
                            item.text.text,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (checklist.size > (if (compact) 3 else 4)) {
                    Text(
                        "+${checklist.size - (if (compact) 3 else 4)} пунктов",
                        style = MaterialTheme.typography.labelSmall,
                        color = appListSecondaryTextColor(),
                    )
                }
            }

            if (mediaKinds.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))) {
                    mediaKinds.forEach { (icon, label) ->
                        Row(horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(3.dp)), verticalAlignment = Alignment.CenterVertically) {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(label, style = MaterialTheme.typography.labelSmall, color = appListSecondaryTextColor())
                        }
                    }
                }
            }

            if (note.labels.isNotEmpty()) {
                Text(
                    text = note.labels.take(if (compact) 2 else 4).joinToString(" · ") { "#$it" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (contextText.isNotBlank()) {
                Text(
                    text = contextText,
                    style = MaterialTheme.typography.labelSmall,
                    color = appListSecondaryTextColor(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun NotesEmptyState(
    location: NoteLibraryLocation,
    searching: Boolean,
    onCreateNote: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        AppExpressiveSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(appCornerRadius(26.dp)),
            tone = AppExpressiveSurfaceTone.ACCENT,
            border = BorderStroke(1.dp, appPanelBorderColor()),
        ) {
            Column(
                modifier = Modifier.padding(appCardPadding()),
                verticalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp)),
            ) {
                Icon(
                    if (searching) Icons.Rounded.Search else Icons.Rounded.Notes,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = when {
                        searching -> "Ничего не найдено"
                        location == NoteLibraryLocation.ARCHIVE -> "Архив пуст"
                        location == NoteLibraryLocation.TRASH -> "Корзина пуста"
                        else -> "Здесь появятся ваши заметки"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = when {
                        searching -> "Измените запрос или снимите фильтр типа содержимого."
                        location == NoteLibraryLocation.ACTIVE -> "Текст, списки, фото, видео и голосовые записи можно смешивать в одной заметке."
                        else -> "Заметки из этого раздела появятся здесь автоматически."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = appListSecondaryTextColor(),
                )
                if (!searching && location == NoteLibraryLocation.ACTIVE) {
                    QuickCreateAction("Создать заметку", Icons.Rounded.Add, onCreateNote)
                }
            }
        }
    }
}

private fun Modifier.notePattern(pattern: String, baseColor: Color): Modifier {
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
            else -> {
                drawLine(lineColor, Offset(0f, size.height * 0.72f), Offset(size.width, size.height * 0.58f), 2.dp.toPx())
                drawLine(lineColor, Offset(size.width * 0.15f, size.height), Offset(size.width, size.height * 0.82f), 2.dp.toPx())
            }
        }
    }
}

private fun parseNoteColor(hex: String): Color = runCatching {
    Color(android.graphics.Color.parseColor(hex))
}.getOrDefault(Color(0xFFFFF3BF))

private fun NoteDocument.previewText(): String = blocks.asSequence().mapNotNull { block ->
    when (block) {
        is NoteBlock.Paragraph -> block.text.text.takeIf { it.isNotBlank() }
        is NoteBlock.Heading -> block.text.text.takeIf { it.isNotBlank() }
        is NoteBlock.Audio -> block.transcript?.text?.takeIf { it.isNotBlank() }
        else -> null
    }
}.joinToString("\n")

private fun NoteDocument.mediaKinds(): List<Pair<ImageVector, String>> {
    val result = mutableListOf<Pair<ImageVector, String>>()
    val images = blocks.count { it is NoteBlock.Image }
    val videos = blocks.count { it is NoteBlock.Video }
    val audio = blocks.count { it is NoteBlock.Audio }
    val files = blocks.count { it is NoteBlock.FileAttachment }
    if (images > 0) result += Icons.Rounded.Image to images.toString()
    if (videos > 0) result += Icons.Rounded.Videocam to videos.toString()
    if (audio > 0) result += Icons.Rounded.AudioFile to audio.toString()
    if (files > 0) result += Icons.Rounded.AttachFile to files.toString()
    return result
}

private fun NoteDocument.contextSummary(): String {
    val parts = mutableListOf<String>()
    context.dateIso?.let { iso ->
        parts += try {
            LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        } catch (_: DateTimeParseException) {
            iso
        }
    }
    context.workplaceId?.takeIf { it.isNotBlank() }?.let { parts += "Работа: $it" }
    context.shiftCode?.takeIf { it.isNotBlank() }?.let { parts += "Смена $it" }
    return parts.joinToString(" · ")
}

private fun noteCountWord(count: Int): String {
    val mod100 = count % 100
    val mod10 = count % 10
    return when {
        mod100 in 11..14 -> "заметок"
        mod10 == 1 -> "заметка"
        mod10 in 2..4 -> "заметки"
        else -> "заметок"
    }
}
