@file:Suppress("DEPRECATION")

package com.vigilante.shiftsalaryplanner

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File


private const val GOOGLE_DRIVE_BACKUP_FILE_NAME = "shift_salary_planner_backup.zip"
private const val GOOGLE_DRIVE_LEGACY_BACKUP_FILE_NAME = "shift_salary_planner_backup.json"

data class GoogleDriveBackupRemoteFile(
    val id: String,
    val name: String,
    val modifiedAtMillis: Long
)

data class GoogleDriveBackupUploadResult(
    val remoteFile: GoogleDriveBackupRemoteFile,
    val created: Boolean
)

data class GoogleDriveBackupDownloadResult(
    val backupJson: String,
    val remoteFile: GoogleDriveBackupRemoteFile,
    val pendingMedia: com.vigilante.shiftsalaryplanner.feature.notes.backup.NotesV2BackupPackageCodec.PreparedMediaRestore? = null,
)

fun uploadBackupToGoogleDriveAppData(
    context: Context,
    account: GoogleSignInAccount,
    backupJson: String
): GoogleDriveBackupUploadResult {
    val drive = buildGoogleDriveService(context, account)
    // Notes media may include large videos: never materialize the entire ZIP in RAM.
    val packageFile = java.io.File.createTempFile("ssp-drive-upload-", ".zip", context.cacheDir)
    try {
        val mediaRoot = java.io.File(
            context.filesDir,
            com.vigilante.shiftsalaryplanner.feature.notes.media.AndroidNoteMediaImporter.MEDIA_ROOT_NAME,
        )
        packageFile.outputStream().use { output ->
            com.vigilante.shiftsalaryplanner.feature.notes.backup.NotesV2BackupPackageCodec.write(
                output = output,
                backupJson = backupJson,
                mediaRoot = mediaRoot,
            )
        }
        val mediaContent = FileContent("application/zip", packageFile)
        val existingFile = findBackupFile(drive, setOf(GOOGLE_DRIVE_BACKUP_FILE_NAME))

        val uploadedFile = if (existingFile != null) {
            drive.files()
                .update(existingFile.id, null, mediaContent)
                .setFields("id,name,modifiedTime")
                .execute()
        } else {
            val metadata = File().apply {
                name = GOOGLE_DRIVE_BACKUP_FILE_NAME
                parents = listOf("appDataFolder")
                mimeType = "application/zip"
            }
            drive.files()
                .create(metadata, mediaContent)
                .setFields("id,name,modifiedTime")
                .execute()
        }

        return GoogleDriveBackupUploadResult(
            remoteFile = uploadedFile.toRemoteFile(),
            created = existingFile == null,
        )
    } finally {
        packageFile.delete()
    }
}

fun downloadBackupFromGoogleDriveAppData(
    context: Context,
    account: GoogleSignInAccount
): GoogleDriveBackupDownloadResult {
    val drive = buildGoogleDriveService(context, account)
    val targetFile = findBackupFile(
        drive,
        setOf(GOOGLE_DRIVE_BACKUP_FILE_NAME, GOOGLE_DRIVE_LEGACY_BACKUP_FILE_NAME),
    ) ?: throw IllegalStateException("В облаке ещё нет резервной копии")

    val packageFile = java.io.File.createTempFile("ssp-drive-download-", ".zip", context.cacheDir)
    try {
        packageFile.outputStream().use { output ->
            drive.files().get(targetFile.id).executeMediaAndDownloadTo(output)
        }
        val mediaRoot = java.io.File(
            context.filesDir,
            com.vigilante.shiftsalaryplanner.feature.notes.media.AndroidNoteMediaImporter.MEDIA_ROOT_NAME,
        )
        val prepared = packageFile.inputStream().use { input ->
            com.vigilante.shiftsalaryplanner.feature.notes.backup.NotesV2BackupPackageCodec.prepareJsonAndMedia(
                input = input,
                mediaRoot = mediaRoot,
                validateJson = { parseAppBackupJson(it) },
            )
        }
        return GoogleDriveBackupDownloadResult(
            backupJson = prepared.backupJson,
            remoteFile = targetFile.toRemoteFile(),
            pendingMedia = prepared,
        )
    } finally {
        packageFile.delete()
    }
}

private fun buildGoogleDriveService(
    context: Context,
    account: GoogleSignInAccount
): Drive {
    val googleAccount = account.account
        ?: throw IllegalStateException("Google-аккаунт не выбран")
    val credential = GoogleAccountCredential.usingOAuth2(
        context,
        listOf(DriveScopes.DRIVE_APPDATA)
    ).apply {
        selectedAccount = googleAccount
    }

    return Drive.Builder(
        NetHttpTransport(),
        GsonFactory.getDefaultInstance(),
        credential
    )
        .setApplicationName("ShiftSalaryPlanner")
        .build()
}

internal data class GoogleDriveBackupPage(
    val files: List<File>,
    val nextPageToken: String?,
)

/** Scan all returned pages; newer unrelated appData files must not hide the backup. */
internal fun findBackupFileInPages(
    acceptedNames: Set<String>,
    fetchPage: (String?) -> GoogleDriveBackupPage,
): File? {
    var pageToken: String? = null
    val seenTokens = mutableSetOf<String>()
    while (true) {
        val page = fetchPage(pageToken)
        page.files.firstOrNull { it.name in acceptedNames }?.let { return it }
        val next = page.nextPageToken?.takeIf { it.isNotBlank() } ?: return null
        check(seenTokens.add(next)) { "Google Drive returned a repeated page token" }
        pageToken = next
    }
}

private fun findBackupFile(drive: Drive, acceptedNames: Set<String>): File? =
    findBackupFileInPages(acceptedNames) { pageToken ->
        val request = drive.files().list()
            .setSpaces("appDataFolder")
            .setPageSize(100)
            .setQ("trashed = false")
            .setOrderBy("modifiedTime desc")
            .setFields("nextPageToken,files(id,name,modifiedTime)")
        if (pageToken != null) request.setPageToken(pageToken)
        val response = request.execute()
        GoogleDriveBackupPage(response.files.orEmpty(), response.nextPageToken)
    }

private fun File.toRemoteFile(): GoogleDriveBackupRemoteFile {
    return GoogleDriveBackupRemoteFile(
        id = id.orEmpty(),
        name = name.orEmpty(),
        modifiedAtMillis = modifiedTime?.value ?: 0L
    )
}
