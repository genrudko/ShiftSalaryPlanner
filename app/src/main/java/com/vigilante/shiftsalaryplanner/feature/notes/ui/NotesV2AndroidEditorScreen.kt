package com.vigilante.shiftsalaryplanner.feature.notes.ui

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.ui.platform.LocalContext
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteBlock
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteDocument
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteDocumentMutations
import com.vigilante.shiftsalaryplanner.feature.notes.media.AndroidNoteMediaImporter
import com.vigilante.shiftsalaryplanner.feature.notes.media.NoteAudioRecordingTarget
import com.vigilante.shiftsalaryplanner.feature.notes.media.NoteImageCaptureTarget
import com.vigilante.shiftsalaryplanner.feature.notes.media.NoteMediaKind
import com.vigilante.shiftsalaryplanner.feature.notes.persistence.NoteDocumentJsonCodec
import com.vigilante.shiftsalaryplanner.feature.notes.share.NoteShareFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlinx.coroutines.withContext

enum class NotesV2InitialMediaAction { IMAGE, AUDIO }

/** Android launcher/permission/storage adapter around the pure Notes v2 editor renderer. */
@Composable
fun NotesV2AndroidEditorScreen(
    initialDocument: NoteDocument,
    onBack: () -> Unit,
    onSave: (NoteDocument) -> Unit,
    onDelete: (String) -> Unit,
    onAutosave: (NoteDocument) -> Boolean = { true },
    initialMediaAction: NotesV2InitialMediaAction? = null,
    onInitialMediaActionConsumed: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val importer = remember(context.applicationContext) { AndroidNoteMediaImporter(context) }
    var document by remember(initialDocument.id) { mutableStateOf(initialDocument) }
    var lastPersistedDocument by remember(initialDocument.id) { mutableStateOf(initialDocument) }
    var recording by remember { mutableStateOf<ActiveNoteRecording?>(null) }
    var pendingImageCapture by remember { mutableStateOf<NoteImageCaptureTarget?>(null) }
    var showPhotoSourceDialog by remember { mutableStateOf(false) }
    var pendingExportDocument by remember { mutableStateOf<NoteDocument?>(null) }

    fun autosaveCurrent(): Boolean {
        if (document == lastPersistedDocument) return true
        val saved = onAutosave(document)
        if (saved) {
            lastPersistedDocument = document
        } else {
            context.noteMediaToast("Не удалось автосохранить заметку")
        }
        return saved
    }

    fun appendBlock(block: NoteBlock) {
        document = NoteDocumentMutations.insertBlock(
            document = document,
            block = block,
            afterBlockId = document.blocks.lastOrNull()?.id,
            updatedAtMillis = System.currentTimeMillis(),
        )
    }

    fun importSelected(uris: List<android.net.Uri>, preferredKind: NoteMediaKind?) {
        if (uris.isEmpty()) return
        scope.launch {
            uris.forEach { uri ->
                runCatching {
                    importer.import(document.id, uri, preferredKind)
                }.onSuccess(::appendBlock)
                    .onFailure { context.noteMediaToast("Не удалось добавить вложение") }
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        importSelected(uris, NoteMediaKind.IMAGE)
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val target = pendingImageCapture
        pendingImageCapture = null
        if (saved && target != null && target.file.isFile && target.file.length() > 0L) {
            runCatching { importer.completeImageCapture(target) }
                .onSuccess(::appendBlock)
                .onFailure {
                    target.file.delete()
                    context.noteMediaToast("Не удалось сохранить фото")
                }
        } else {
            target?.file?.delete()
        }
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        importSelected(uris, NoteMediaKind.VIDEO)
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        importSelected(uris, null)
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) context.noteMediaToast("Без разрешения уведомление не будет показано")
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val target = pendingExportDocument
        pendingExportDocument = null
        if (uri != null && target != null) {
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        val raw = NoteDocumentJsonCodec.encodeList(listOf(target)).toString(2)
                        context.contentResolver.openOutputStream(uri)?.use { output ->
                            output.write(raw.toByteArray(Charsets.UTF_8))
                        } ?: error("Cannot open note export target")
                    }
                }.onSuccess {
                    context.noteMediaToast("Заметка экспортирована")
                }.onFailure {
                    context.noteMediaToast("Не удалось экспортировать заметку")
                }
            }
        }
    }

    fun launchCameraCapture() {
        runCatching {
            val target = importer.createImageCaptureTarget(document.id)
            pendingImageCapture?.file?.delete()
            pendingImageCapture = target
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.notes-files",
                target.file,
            )
            cameraLauncher.launch(uri)
        }.onFailure {
            pendingImageCapture?.file?.delete()
            pendingImageCapture = null
            context.noteMediaToast("Не удалось открыть камеру")
        }
    }

    fun setReminder(atMillis: Long?) {
        document = NoteDocumentMutations.setReminder(document, atMillis, System.currentTimeMillis())
        if (atMillis != null && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun showReminderPicker() {
        val base = Calendar.getInstance().apply {
            timeInMillis = document.reminderAtMillis?.takeIf { it > System.currentTimeMillis() } ?: (System.currentTimeMillis() + 60 * 60 * 1000L)
        }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val selected = (base.clone() as Calendar).apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, day)
                }
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        selected.set(Calendar.HOUR_OF_DAY, hour)
                        selected.set(Calendar.MINUTE, minute)
                        selected.set(Calendar.SECOND, 0)
                        selected.set(Calendar.MILLISECOND, 0)
                        if (selected.timeInMillis > System.currentTimeMillis()) setReminder(selected.timeInMillis)
                        else context.noteMediaToast("Выберите время в будущем")
                    },
                    base.get(Calendar.HOUR_OF_DAY),
                    base.get(Calendar.MINUTE),
                    true,
                ).show()
            },
            base.get(Calendar.YEAR),
            base.get(Calendar.MONTH),
            base.get(Calendar.DAY_OF_MONTH),
        ).apply { datePicker.minDate = System.currentTimeMillis() - 1_000L }.show()
    }

    fun startRecording() {
        if (recording != null) return
        runCatching {
            val target = importer.createRecordingTarget(document.id)
            val recorder = createNotesV2MediaRecorder(target)
            recorder.prepare()
            recorder.start()
            recording = ActiveNoteRecording(target, recorder)
        }.onFailure {
            recording?.releaseAndDelete()
            recording = null
            pendingImageCapture?.file?.delete()
            pendingImageCapture = null
            context.noteMediaToast("Не удалось начать запись")
        }
    }

    fun stopRecording() {
        val active = recording ?: return
        recording = null
        val stopped = runCatching {
            active.recorder.stop()
            true
        }.getOrDefault(false)
        runCatching { active.recorder.release() }
        if (stopped && active.target.file.isFile && active.target.file.length() > 0L) {
            runCatching { importer.completeRecording(active.target) }
                .onSuccess(::appendBlock)
                .onFailure {
                    active.target.file.delete()
                    context.noteMediaToast("Не удалось сохранить запись")
                }
        } else {
            active.target.file.delete()
            context.noteMediaToast("Запись не сохранена")
        }
    }

    val audioPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecording() else context.noteMediaToast("Нужен доступ к микрофону")
    }

    fun requestAudioRecording() {
        if (recording != null) {
            stopRecording()
        } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startRecording()
        } else {
            audioPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(document) {
        if (document == lastPersistedDocument) return@LaunchedEffect
        delay(NOTES_V2_AUTOSAVE_DELAY_MS)
        autosaveCurrent()
    }

    BackHandler(enabled = true) {
        if (autosaveCurrent()) onBack()
    }

    LaunchedEffect(initialMediaAction, initialDocument.id) {
        when (initialMediaAction) {
            NotesV2InitialMediaAction.IMAGE -> showPhotoSourceDialog = true
            NotesV2InitialMediaAction.AUDIO -> requestAudioRecording()
            null -> return@LaunchedEffect
        }
        onInitialMediaActionConsumed()
    }

    DisposableEffect(Unit) {
        onDispose {
            recording?.releaseAndDelete()
            recording = null
        }
    }

    if (showPhotoSourceDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoSourceDialog = false },
            title = { Text("Добавить фото") },
            text = { Text("Снять новое фото или выбрать существующее из галереи?") },
            confirmButton = {
                TextButton(onClick = {
                    showPhotoSourceDialog = false
                    launchCameraCapture()
                }) { Text("Камера") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPhotoSourceDialog = false
                    imagePicker.launch("image/*")
                }) { Text("Галерея") }
            },
        )
    }

    NotesV2EditorScreen(
        initialDocument = document,
        onBack = { if (autosaveCurrent()) onBack() },
        onSave = onSave,
        onDelete = onDelete,
        onRequestImage = { showPhotoSourceDialog = true },
        onRequestVideo = { videoPicker.launch("video/*") },
        onRequestAudio = ::requestAudioRecording,
        onRequestFile = { filePicker.launch(arrayOf("*/*")) },
        onShare = { note ->
            runCatching { context.shareNotesV2Document(note) }
                .onFailure { context.noteMediaToast("Не удалось поделиться заметкой") }
        },
        onExport = { note ->
            pendingExportDocument = note
            exportLauncher.launch("ShiftSalaryPlanner_note_${note.id}.json")
        },
        modifier = modifier,
        onDocumentChange = { document = it },
        isRecordingAudio = recording != null,
        mediaBlockContent = { block -> NotesV2AndroidMediaBlock(block) },
    )
}

private const val NOTES_V2_AUTOSAVE_DELAY_MS = 800L

private data class ActiveNoteRecording(
    val target: NoteAudioRecordingTarget,
    val recorder: MediaRecorder,
) {
    fun releaseAndDelete() {
        runCatching { recorder.stop() }
        runCatching { recorder.release() }
        target.file.delete()
    }
}

@Suppress("DEPRECATION")
private fun createNotesV2MediaRecorder(target: NoteAudioRecordingTarget): MediaRecorder = MediaRecorder().apply {
    setAudioSource(MediaRecorder.AudioSource.MIC)
    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
    setAudioSamplingRate(44_100)
    setAudioEncodingBitRate(96_000)
    setOutputFile(target.file.absolutePath)
}

private fun Context.shareNotesV2Document(document: NoteDocument) {
    val text = NoteShareFormatter.toPlainText(document)
    val streams = document.blocks.mapNotNull { block ->
        val raw = when (block) {
            is NoteBlock.Image -> block.uri
            is NoteBlock.Video -> block.uri
            is NoteBlock.Audio -> block.uri
            is NoteBlock.FileAttachment -> block.uri
            else -> null
        } ?: return@mapNotNull null
        runCatching {
            val file = java.io.File(java.net.URI(raw))
            if (!file.isFile) return@runCatching null
            FileProvider.getUriForFile(this, "$packageName.notes-files", file)
        }.getOrNull()
    }
    val action = if (streams.size > 1) Intent.ACTION_SEND_MULTIPLE else Intent.ACTION_SEND
    val intent = Intent(action).apply {
        type = if (streams.isEmpty()) "text/plain" else "*/*"
        putExtra(Intent.EXTRA_TEXT, text)
        when (streams.size) {
            0 -> Unit
            1 -> putExtra(Intent.EXTRA_STREAM, streams.single())
            else -> putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList<Uri>(streams))
        }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(intent, "Поделиться заметкой"))
}

private fun Context.noteMediaToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
