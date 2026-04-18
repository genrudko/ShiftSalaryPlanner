@file:Suppress("DEPRECATION")

package com.vigilante.shiftsalaryplanner

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import java.io.ByteArrayOutputStream

private const val GOOGLE_DRIVE_BACKUP_FILE_NAME = "shift_salary_planner_backup.json"

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
    val remoteFile: GoogleDriveBackupRemoteFile
)

fun uploadBackupToGoogleDriveAppData(
    context: Context,
    account: GoogleSignInAccount,
    backupJson: String
): GoogleDriveBackupUploadResult {
    val drive = buildGoogleDriveService(context, account)
    val mediaContent = ByteArrayContent.fromString("application/json", backupJson)
    val existingFile = findBackupFile(drive)

    val uploadedFile = if (existingFile != null) {
        drive.files()
            .update(existingFile.id, null, mediaContent)
            .setFields("id,name,modifiedTime")
            .execute()
    } else {
        val metadata = File().apply {
            name = GOOGLE_DRIVE_BACKUP_FILE_NAME
            parents = listOf("appDataFolder")
            mimeType = "application/json"
        }
        drive.files()
            .create(metadata, mediaContent)
            .setFields("id,name,modifiedTime")
            .execute()
    }

    return GoogleDriveBackupUploadResult(
        remoteFile = uploadedFile.toRemoteFile(),
        created = existingFile == null
    )
}

fun downloadBackupFromGoogleDriveAppData(
    context: Context,
    account: GoogleSignInAccount
): GoogleDriveBackupDownloadResult {
    val drive = buildGoogleDriveService(context, account)
    val targetFile = findBackupFile(drive)
        ?: throw IllegalStateException("В облаке ещё нет резервной копии")

    val output = ByteArrayOutputStream()
    drive.files()
        .get(targetFile.id)
        .executeMediaAndDownloadTo(output)

    return GoogleDriveBackupDownloadResult(
        backupJson = output.toString(Charsets.UTF_8.name()),
        remoteFile = targetFile.toRemoteFile()
    )
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

private fun findBackupFile(drive: Drive): File? {
    return drive.files()
        .list()
        .setSpaces("appDataFolder")
        .setPageSize(10)
        .setQ("name = '$GOOGLE_DRIVE_BACKUP_FILE_NAME' and trashed = false")
        .setOrderBy("modifiedTime desc")
        .setFields("files(id,name,modifiedTime)")
        .execute()
        .files
        ?.firstOrNull()
}

private fun File.toRemoteFile(): GoogleDriveBackupRemoteFile {
    return GoogleDriveBackupRemoteFile(
        id = id.orEmpty(),
        name = name.orEmpty(),
        modifiedAtMillis = modifiedTime?.value ?: 0L
    )
}
