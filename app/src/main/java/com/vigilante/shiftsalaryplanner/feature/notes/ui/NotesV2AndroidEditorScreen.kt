package com.vigilante.shiftsalaryplanner.feature.notes.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

private fun Context.noteMediaToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
