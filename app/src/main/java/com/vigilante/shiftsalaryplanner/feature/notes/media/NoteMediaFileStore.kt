package com.vigilante.shiftsalaryplanner.feature.notes.media

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/** App-owned durable media storage for Notes v2. */
data class StoredNoteMedia(
    val uri: String,
    val displayName: String,
    val mimeType: String,
    val byteSize: Long,
)

class NoteMediaFileStore(rootDirectory: File) {
    private val root: File = rootDirectory.canonicalFile

    init {
        require(root.exists() || root.mkdirs()) { "Cannot create note media root: $root" }
        require(root.isDirectory) { "Note media root is not a directory: $root" }
    }

    fun import(
        noteId: String,
        mediaId: String,
        displayName: String,
        mimeType: String,
        input: InputStream,
    ): StoredNoteMedia {
        val noteDirectory = managedNoteDirectory(noteId)
        require(noteDirectory.exists() || noteDirectory.mkdirs()) { "Cannot create note media directory" }

        val safeMediaId = sanitizeSegment(mediaId).ifBlank { "media" }
        val extension = safeExtension(displayName, mimeType)
        val finalFile = File(noteDirectory, safeMediaId + extension).canonicalFile
        require(isUnderRoot(finalFile)) { "Media target escapes managed root" }
        val partFile = File(noteDirectory, ".$safeMediaId${extension}.part").canonicalFile
        require(isUnderRoot(partFile)) { "Media temp target escapes managed root" }

        partFile.delete()
        try {
            FileOutputStream(partFile).use { output ->
                input.copyTo(output, DEFAULT_BUFFER_SIZE)
                output.fd.sync()
            }
            if (finalFile.exists() && !finalFile.delete()) {
                error("Cannot replace existing note media")
            }
            if (!partFile.renameTo(finalFile)) {
                error("Cannot finalize note media import")
            }
        } catch (error: Throwable) {
            partFile.delete()
            throw error
        }

        return StoredNoteMedia(
            uri = finalFile.toURI().toString(),
            displayName = displayName.substringAfterLast('/').ifBlank { finalFile.name },
            mimeType = mimeType.ifBlank { "application/octet-stream" },
            byteSize = finalFile.length(),
        )
    }

    fun createRecordingTarget(noteId: String, mediaId: String): File {
        val noteDirectory = managedNoteDirectory(noteId)
        require(noteDirectory.exists() || noteDirectory.mkdirs()) { "Cannot create note media directory" }
        val safeMediaId = sanitizeSegment(mediaId).ifBlank { "audio" }
        val target = File(noteDirectory, "$safeMediaId.m4a").canonicalFile
        require(isUnderRoot(target)) { "Recording target escapes managed root" }
        return target
    }

    fun isManagedUri(uri: String): Boolean = resolveManagedFile(uri) != null

    fun resolveManagedFile(uri: String): File? {
        val file = runCatching {
            val parsed = java.net.URI(uri)
            if (parsed.scheme != "file") return null
            File(parsed).canonicalFile
        }.getOrNull() ?: return null
        return file.takeIf(::isUnderRoot)
    }

    fun deleteManagedUri(uri: String): Boolean {
        val file = resolveManagedFile(uri) ?: return false
        if (!file.exists()) return false
        val parent = file.parentFile
        val deleted = file.delete()
        if (deleted && parent != null && parent != root && isUnderRoot(parent)) {
            parent.listFiles()?.takeIf { it.isEmpty() }?.let { parent.delete() }
        }
        return deleted
    }

    fun deleteNoteMedia(noteId: String): Boolean {
        val directory = managedNoteDirectory(noteId)
        if (!directory.exists()) return false
        require(isUnderRoot(directory)) { "Note directory escapes managed root" }
        return directory.deleteRecursively()
    }

    private fun managedNoteDirectory(noteId: String): File {
        val safeNoteId = sanitizeSegment(noteId).ifBlank { "note" }
        val directory = File(root, safeNoteId).canonicalFile
        require(isUnderRoot(directory)) { "Note directory escapes managed root" }
        return directory
    }

    private fun isUnderRoot(file: File): Boolean {
        val rootPath = root.path
        val filePath = file.path
        return filePath == rootPath || filePath.startsWith(rootPath + File.separator)
    }

    private fun sanitizeSegment(value: String): String = buildString(value.length.coerceAtMost(96)) {
        value.take(96).forEach { char ->
            append(if (char.isLetterOrDigit() || char == '-' || char == '_') char else '_')
        }
    }.trim('_')

    private fun safeExtension(displayName: String, mimeType: String): String {
        val fromName = displayName.substringAfterLast('/').substringAfterLast('.', "")
            .lowercase()
            .takeIf { it.length in 1..10 && it.all { ch -> ch.isLetterOrDigit() } }
        val extension = fromName ?: when (mimeType.lowercase()) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            "video/mp4" -> "mp4"
            "audio/mp4", "audio/m4a", "audio/x-m4a" -> "m4a"
            "audio/mpeg" -> "mp3"
            "application/pdf" -> "pdf"
            else -> "bin"
        }
        return ".$extension"
    }
}
