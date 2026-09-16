package com.vigilante.shiftsalaryplanner.feature.notes.media

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteBlock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URLConnection
import java.util.UUID

class AndroidNoteMediaImporter(
    context: Context,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val appContext = context.applicationContext
    private val store = NoteMediaFileStore(File(appContext.filesDir, MEDIA_ROOT_NAME))

    suspend fun import(
        noteId: String,
        sourceUri: Uri,
        preferredKind: NoteMediaKind? = null,
        mediaId: String = UUID.randomUUID().toString(),
    ): NoteBlock = withContext(Dispatchers.IO) {
        val resolver = appContext.contentResolver
        val displayName = resolveDisplayName(sourceUri)
        val mimeType = resolver.getType(sourceUri)
            ?.takeIf { it.isNotBlank() }
            ?: URLConnection.guessContentTypeFromName(displayName)
            ?: preferredKind.defaultMimeType()
        val stored = resolver.openInputStream(sourceUri)?.use { input ->
            store.import(
                noteId = noteId,
                mediaId = mediaId,
                displayName = displayName,
                mimeType = mimeType,
                input = input,
            )
        } ?: error("Cannot open selected note attachment")

        val managedFile = store.resolveManagedFile(stored.uri)
            ?: error("Imported note attachment is not managed")
        val kind = preferredKind ?: NoteMediaBlockFactory.kindForMime(stored.mimeType)
        val ratio = when (kind) {
            NoteMediaKind.IMAGE -> resolveImageAspectRatio(managedFile)
            NoteMediaKind.VIDEO -> resolveVideoAspectRatio(managedFile)
            NoteMediaKind.AUDIO,
            NoteMediaKind.FILE -> 0f
        }
        NoteMediaBlockFactory.create(
            mediaId = mediaId,
            descriptor = NoteMediaDescriptor(
                uri = stored.uri,
                displayName = stored.displayName,
                mimeType = stored.mimeType,
                byteSize = stored.byteSize,
                aspectRatio = ratio,
                createdAtMillis = clock(),
            ),
            preferredKind = preferredKind,
        )
    }

    fun createImageCaptureTarget(
        noteId: String,
        mediaId: String = UUID.randomUUID().toString(),
    ): NoteImageCaptureTarget = NoteImageCaptureTarget(
        mediaId = mediaId,
        file = store.createImageCaptureTarget(noteId, mediaId),
    )

    fun completeImageCapture(target: NoteImageCaptureTarget): NoteBlock.Image {
        require(target.file.isFile && target.file.length() > 0L) { "Captured note image is empty" }
        return NoteMediaBlockFactory.create(
            mediaId = target.mediaId,
            descriptor = NoteMediaDescriptor(
                uri = target.file.toURI().toString(),
                displayName = "Фото.jpg",
                mimeType = "image/jpeg",
                byteSize = target.file.length(),
                aspectRatio = resolveImageAspectRatio(target.file),
                createdAtMillis = clock(),
            ),
            preferredKind = NoteMediaKind.IMAGE,
        ) as NoteBlock.Image
    }

    fun createRecordingTarget(
        noteId: String,
        mediaId: String = UUID.randomUUID().toString(),
    ): NoteAudioRecordingTarget = NoteAudioRecordingTarget(
        mediaId = mediaId,
        file = store.createRecordingTarget(noteId, mediaId),
    )

    fun completeRecording(target: NoteAudioRecordingTarget): NoteBlock.Audio {
        require(target.file.isFile && target.file.length() > 0L) { "Recorded note audio is empty" }
        return NoteMediaBlockFactory.create(
            mediaId = target.mediaId,
            descriptor = NoteMediaDescriptor(
                uri = target.file.toURI().toString(),
                displayName = "Голосовая запись.m4a",
                mimeType = "audio/mp4",
                byteSize = target.file.length(),
                createdAtMillis = clock(),
            ),
            preferredKind = NoteMediaKind.AUDIO,
        ) as NoteBlock.Audio
    }

    private fun resolveDisplayName(uri: Uri): String {
        appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    cursor.getString(index)?.takeIf { it.isNotBlank() }?.let { return it }
                }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "Вложение"
    }

    private fun resolveImageAspectRatio(file: File): Float = runCatching {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        if (options.outWidth > 0 && options.outHeight > 0) {
            options.outWidth.toFloat() / options.outHeight.toFloat()
        } else 0f
    }.getOrDefault(0f).normalizeRatio()

    private fun resolveVideoAspectRatio(file: File): Float = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toFloatOrNull() ?: 0f
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toFloatOrNull() ?: 0f
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            if (width <= 0f || height <= 0f) 0f
            else if (rotation == 90 || rotation == 270) height / width
            else width / height
        } finally {
            retriever.release()
        }
    }.getOrDefault(0f).normalizeRatio()

    private fun Float.normalizeRatio(): Float = takeIf { isFinite() && this > 0f }
        ?.coerceIn(0.32f, 3.2f)
        ?: 0f

    private fun NoteMediaKind?.defaultMimeType(): String = when (this) {
        NoteMediaKind.IMAGE -> "image/*"
        NoteMediaKind.VIDEO -> "video/*"
        NoteMediaKind.AUDIO -> "audio/*"
        NoteMediaKind.FILE,
        null -> "application/octet-stream"
    }

    companion object {
        const val MEDIA_ROOT_NAME = "note_media_v2"
    }
}

data class NoteImageCaptureTarget(
    val mediaId: String,
    val file: File,
)

data class NoteAudioRecordingTarget(
    val mediaId: String,
    val file: File,
)
