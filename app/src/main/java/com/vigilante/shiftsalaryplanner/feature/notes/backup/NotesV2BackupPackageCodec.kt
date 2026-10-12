package com.vigilante.shiftsalaryplanner.feature.notes.backup

import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object NotesV2BackupPackageCodec {
    private const val JSON_ENTRY = "backup.json"
    private const val MEDIA_PREFIX = "note_media_v2/"

    fun write(output: OutputStream, backupJson: String, mediaRoot: File) {
        val zip = ZipOutputStream(output)
        zip.putNextEntry(ZipEntry(JSON_ENTRY))
        zip.write(backupJson.toByteArray(Charsets.UTF_8))
        zip.closeEntry()

        if (mediaRoot.isDirectory) {
            val rootCanonical = mediaRoot.canonicalFile
            val rootPrefix = rootCanonical.path + File.separator
            mediaRoot.walkTopDown()
                .filter { it.isFile }
                .forEach { file ->
                    val canonical = file.canonicalFile
                    require(canonical.path.startsWith(rootPrefix)) { "Media file escapes Notes media root" }
                    val relative = file.relativeTo(mediaRoot).invariantSeparatorsPath
                    require(relative.isNotBlank() && relative.split('/').none { it == ".." }) {
                        "Unsafe Notes media path"
                    }
                    zip.putNextEntry(ZipEntry(MEDIA_PREFIX + relative))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
        }
        zip.finish()
        zip.flush()
    }

    fun readJsonAndRestoreMedia(
        input: InputStream,
        mediaRoot: File,
        validateJson: (String) -> Unit = {},
    ): String {
        val buffered = if (input is BufferedInputStream) input else BufferedInputStream(input)
        buffered.mark(4)
        val first = buffered.read()
        val second = buffered.read()
        buffered.reset()
        if (first != 'P'.code || second != 'K'.code) {
            return buffered.readBytes().toString(Charsets.UTF_8).also(validateJson)
        }
        return restoreZip(buffered, mediaRoot, validateJson)
    }

    private fun restoreZip(
        input: InputStream,
        mediaRoot: File,
        validateJson: (String) -> Unit,
    ): String {
        val parent = requireNotNull(mediaRoot.absoluteFile.parentFile) { "Notes media root needs a parent" }
        check(parent.exists() || parent.mkdirs()) { "Unable to create Notes media parent" }
        val staging = File(parent, ".${mediaRoot.name}.restore-${UUID.randomUUID()}")
        check(staging.mkdirs()) { "Unable to create Notes media restore staging directory" }
        var backupJson: String? = null

        try {
            val stagingCanonical = staging.canonicalFile
            val stagingPrefix = stagingCanonical.path + File.separator
            val zip = ZipInputStream(input)
            while (true) {
                val entry = zip.nextEntry ?: break
                when {
                    entry.name == JSON_ENTRY -> {
                        backupJson = zip.readBytes().toString(Charsets.UTF_8)
                    }
                    entry.name.startsWith(MEDIA_PREFIX) -> {
                        val relative = entry.name.removePrefix(MEDIA_PREFIX)
                        requireSafeRelativePath(relative)
                        if (relative.isNotBlank()) {
                            val destination = File(staging, relative).canonicalFile
                            require(destination.path.startsWith(stagingPrefix)) { "Unsafe Notes media backup path" }
                            if (entry.isDirectory) {
                                check(destination.exists() || destination.mkdirs()) { "Unable to create media directory" }
                            } else {
                                destination.parentFile?.let { dir ->
                                    check(dir.exists() || dir.mkdirs()) { "Unable to create media directory" }
                                }
                                destination.outputStream().use { zip.copyTo(it) }
                            }
                        }
                    }
                }
                zip.closeEntry()
            }
            val json = requireNotNull(backupJson) { "Backup package does not contain backup.json" }
            validateJson(json)
            replaceDirectory(staging, mediaRoot)
            return json
        } catch (error: Throwable) {
            staging.deleteRecursively()
            throw error
        }
    }

    private fun requireSafeRelativePath(path: String) {
        require(!path.startsWith('/')) { "Absolute Notes media path is not allowed" }
        require(path.split('/').none { it == ".." }) { "Notes media path traversal is not allowed" }
    }

    private fun replaceDirectory(staging: File, target: File) {
        val parent = requireNotNull(target.absoluteFile.parentFile)
        val previous = File(parent, ".${target.name}.previous-${UUID.randomUUID()}")
        val hadTarget = target.exists()
        if (hadTarget) {
            check(target.renameTo(previous)) { "Unable to stage previous Notes media directory" }
        }
        if (!staging.renameTo(target)) {
            if (hadTarget) previous.renameTo(target)
            error("Unable to install restored Notes media directory")
        }
        if (hadTarget) previous.deleteRecursively()
    }
}
