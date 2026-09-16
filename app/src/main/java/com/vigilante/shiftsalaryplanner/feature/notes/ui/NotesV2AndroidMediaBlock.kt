package com.vigilante.shiftsalaryplanner.feature.notes.ui

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.ImageView
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.vigilante.shiftsalaryplanner.appHapticAction
import com.vigilante.shiftsalaryplanner.appListSecondaryTextColor
import com.vigilante.shiftsalaryplanner.appScaledSpacing
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteBlock
import com.vigilante.shiftsalaryplanner.feature.notes.media.AndroidNoteMediaImporter
import com.vigilante.shiftsalaryplanner.feature.notes.media.NoteMediaFileStore
import kotlinx.coroutines.delay
import java.io.File

@Composable
internal fun NotesV2AndroidMediaBlock(block: NoteBlock) {
    val context = LocalContext.current
    val mediaStore = remember(context.applicationContext) {
        NoteMediaFileStore(File(context.filesDir, AndroidNoteMediaImporter.MEDIA_ROOT_NAME))
    }
    when (block) {
        is NoteBlock.Image -> NotesV2ImageBlock(block)
        is NoteBlock.Video -> NotesV2VideoBlock(block)
        is NoteBlock.Audio -> NotesV2AudioBlock(block)
        is NoteBlock.FileAttachment -> NotesV2FileBlock(block, mediaStore)
        else -> Unit
    }
}

@Composable
private fun NotesV2ImageBlock(block: NoteBlock.Image) {
    val uri = remember(block.uri) { Uri.parse(block.uri) }
    var expanded by remember(block.id) { mutableStateOf(false) }
    val ratio = block.presentation.aspectRatio.takeIf { it.isFinite() && it > 0f }?.coerceIn(0.45f, 2.4f) ?: 4f / 3f
    Column(verticalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))) {
        AndroidView(
            factory = { context -> ImageView(context).apply {
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_CENTER
            } },
            update = { image -> if (image.tag != block.uri) { image.tag = block.uri; image.setImageURI(uri) } },
            modifier = Modifier.fillMaxWidth().aspectRatio(ratio),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = appHapticAction { expanded = true }) {
                Icon(Icons.Rounded.Fullscreen, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("На весь экран")
            }
        }
    }
    if (expanded) {
        Dialog(onDismissRequest = { expanded = false }) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { context -> ImageView(context).apply {
                            adjustViewBounds = true
                            scaleType = ImageView.ScaleType.FIT_CENTER
                            setImageURI(uri)
                        } },
                        update = { if (it.tag != block.uri) { it.tag = block.uri; it.setImageURI(uri) } },
                        modifier = Modifier.fillMaxSize(),
                    )
                    IconButton(onClick = { expanded = false }, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Закрыть")
                    }
                }
            }
        }
    }
}

@Composable
private fun NotesV2VideoBlock(block: NoteBlock.Video) {
    val uri = remember(block.uri) { Uri.parse(block.uri) }
    var expanded by remember(block.id) { mutableStateOf(false) }
    val ratio = block.presentation.aspectRatio.takeIf { it.isFinite() && it > 0f }?.coerceIn(0.45f, 2.4f) ?: 16f / 9f

    fun configure(view: VideoView) {
        if (view.tag == block.uri) return
        view.tag = block.uri
        val controller = MediaController(view.context)
        controller.setAnchorView(view)
        view.setMediaController(controller)
        view.setVideoURI(uri)
        view.setOnPreparedListener { player ->
            player.isLooping = false
            runCatching { view.seekTo(1) }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))) {
        AndroidView(
            factory = { context -> VideoView(context).also(::configure) },
            update = ::configure,
            modifier = Modifier.fillMaxWidth().aspectRatio(ratio),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = appHapticAction { expanded = true }) {
                Icon(Icons.Rounded.Fullscreen, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("На весь экран")
            }
        }
    }

    if (expanded) {
        Dialog(onDismissRequest = { expanded = false }) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { context -> VideoView(context).also(::configure) },
                        update = ::configure,
                        modifier = Modifier.fillMaxSize(),
                    )
                    IconButton(onClick = { expanded = false }, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Закрыть")
                    }
                }
            }
        }
    }
}

@Composable
private fun NotesV2AudioBlock(block: NoteBlock.Audio) {
    val context = LocalContext.current
    var player by remember(block.id, block.uri) { mutableStateOf<MediaPlayer?>(null) }
    var prepared by remember(block.id, block.uri) { mutableStateOf(false) }
    var playing by remember(block.id, block.uri) { mutableStateOf(false) }
    var position by remember(block.id, block.uri) { mutableIntStateOf(0) }
    var duration by remember(block.id, block.uri) { mutableIntStateOf(0) }
    var failed by remember(block.id, block.uri) { mutableStateOf(false) }

    fun release() {
        player?.let { runCatching { it.stop() }; runCatching { it.release() } }
        player = null
        prepared = false
        playing = false
        position = 0
    }

    fun toggle() {
        val existing = player
        if (existing != null && prepared) {
            if (playing) {
                runCatching { existing.pause() }
                playing = false
            } else {
                runCatching { existing.start() }.onSuccess { playing = true }.onFailure { failed = true }
            }
            return
        }
        failed = false
        runCatching {
            MediaPlayer().also { media ->
                player = media
                media.setDataSource(context, Uri.parse(block.uri))
                media.setOnPreparedListener {
                    prepared = true
                    duration = it.duration.coerceAtLeast(0)
                    it.start()
                    playing = true
                }
                media.setOnCompletionListener {
                    playing = false
                    position = duration
                }
                media.setOnErrorListener { _, _, _ ->
                    failed = true
                    playing = false
                    true
                }
                media.prepareAsync()
            }
        }.onFailure {
            failed = true
            release()
        }
    }

    DisposableEffect(block.id, block.uri) { onDispose { release() } }
    LaunchedEffect(playing) {
        while (playing) {
            position = player?.currentPosition?.coerceAtLeast(0) ?: 0
            delay(250)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(appScaledSpacing(7.dp))) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))) {
            IconButton(onClick = appHapticAction(onAction = ::toggle)) {
                Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = if (playing) "Пауза" else "Воспроизвести")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(block.label.ifBlank { "Голосовая запись" }, style = MaterialTheme.typography.bodyMedium)
                Text(
                    if (failed) "Не удалось воспроизвести" else "${position.asTime()} / ${duration.asTime()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (failed) MaterialTheme.colorScheme.error else appListSecondaryTextColor(),
                )
            }
        }
        if (!failed) {
            LinearProgressIndicator(
                progress = { if (duration > 0) (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        block.transcript?.text?.takeIf { it.isNotBlank() }?.let { transcript ->
            Text(transcript, style = MaterialTheme.typography.bodySmall, color = appListSecondaryTextColor())
        }
    }
}

@Composable
private fun NotesV2FileBlock(block: NoteBlock.FileAttachment, store: NoteMediaFileStore) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp)),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(block.label.ifBlank { "Вложение" }, style = MaterialTheme.typography.bodyMedium)
            Text("Файл", style = MaterialTheme.typography.labelMedium, color = appListSecondaryTextColor())
        }
        IconButton(onClick = appHapticAction { context.openNotesV2Attachment(block, store) }) {
            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = "Открыть")
        }
    }
}

private fun Context.openNotesV2Attachment(block: NoteBlock.FileAttachment, store: NoteMediaFileStore) {
    val raw = Uri.parse(block.uri)
    val openUri = when (raw.scheme) {
        "file" -> {
            val managed = store.resolveManagedFile(block.uri)
            val legacyRoot = File(filesDir, "note_audio").canonicalFile
            val rawFile = runCatching { File(raw.path.orEmpty()).canonicalFile }.getOrNull()
            val file = managed ?: rawFile?.takeIf { candidate ->
                candidate.path.startsWith(legacyRoot.path + File.separator)
            }
            if (file == null || !file.isFile) {
                Toast.makeText(this, "Файл недоступен", Toast.LENGTH_SHORT).show()
                return
            }
            runCatching {
                FileProvider.getUriForFile(this, "$packageName.notes-files", file)
            }.getOrElse {
                Toast.makeText(this, "Файл нельзя открыть вне приложения", Toast.LENGTH_SHORT).show()
                return
            }
        }
        else -> raw
    }
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(openUri, block.mimeTypeHint())
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { startActivity(Intent.createChooser(intent, "Открыть вложение")) }
        .onFailure { Toast.makeText(this, "Нет приложения для открытия файла", Toast.LENGTH_SHORT).show() }
}

private fun NoteBlock.FileAttachment.mimeTypeHint(): String {
    val extension = label.substringAfterLast('.', "").lowercase()
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
}

private fun Int.asTime(): String {
    val totalSeconds = (this / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
