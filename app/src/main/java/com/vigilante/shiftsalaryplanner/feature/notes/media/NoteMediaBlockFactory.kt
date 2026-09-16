package com.vigilante.shiftsalaryplanner.feature.notes.media

import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteBlock
import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteMediaPresentation

enum class NoteMediaKind { IMAGE, VIDEO, AUDIO, FILE }

data class NoteMediaDescriptor(
    val uri: String,
    val displayName: String,
    val mimeType: String,
    val byteSize: Long,
    val aspectRatio: Float = 0f,
    val createdAtMillis: Long = 0L,
)

object NoteMediaBlockFactory {
    fun create(
        mediaId: String,
        descriptor: NoteMediaDescriptor,
        preferredKind: NoteMediaKind? = null,
    ): NoteBlock {
        val kind = preferredKind ?: kindForMime(descriptor.mimeType)
        val presentation = NoteMediaPresentation(
            aspectRatio = descriptor.aspectRatio
                .takeIf { it.isFinite() && it > 0f }
                ?.coerceIn(0.32f, 3.2f)
                ?: 0f,
            createdAtMillis = descriptor.createdAtMillis,
        )
        return when (kind) {
            NoteMediaKind.IMAGE -> NoteBlock.Image(
                id = mediaId,
                uri = descriptor.uri,
                label = descriptor.displayName,
                presentation = presentation,
            )
            NoteMediaKind.VIDEO -> NoteBlock.Video(
                id = mediaId,
                uri = descriptor.uri,
                label = descriptor.displayName,
                presentation = presentation,
            )
            NoteMediaKind.AUDIO -> NoteBlock.Audio(
                id = mediaId,
                uri = descriptor.uri,
                label = descriptor.displayName,
                presentation = presentation,
            )
            NoteMediaKind.FILE -> NoteBlock.FileAttachment(
                id = mediaId,
                uri = descriptor.uri,
                label = descriptor.displayName,
                presentation = presentation,
            )
        }
    }

    fun kindForMime(mimeType: String): NoteMediaKind {
        val mime = mimeType.trim().lowercase()
        return when {
            mime.startsWith("image/") -> NoteMediaKind.IMAGE
            mime.startsWith("video/") -> NoteMediaKind.VIDEO
            mime.startsWith("audio/") -> NoteMediaKind.AUDIO
            else -> NoteMediaKind.FILE
        }
    }
}
