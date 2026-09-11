package com.vigilante.shiftsalaryplanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.vigilante.shiftsalaryplanner.excel.ExcelImportPreview
import com.vigilante.shiftsalaryplanner.excel.ExcelPersonCandidate

class ServiceWorkflowState(
    showPostUpdateCheckDialog: Boolean = false,
    excelImportStatusMessage: String? = null,
    pendingExcelFileName: String? = null,
    backupRestoreStatusMessage: String? = null,
    autoUploadCheckedForAccount: String = "",
    initialGoogleSignedInAccount: GoogleSignInAccount? = null
) {
    var showPostUpdateCheckDialog by mutableStateOf(showPostUpdateCheckDialog)
        private set
    var excelImportStatusMessage by mutableStateOf(excelImportStatusMessage)
    var pendingExcelFileName by mutableStateOf(pendingExcelFileName)
        private set
    var backupRestoreStatusMessage by mutableStateOf(backupRestoreStatusMessage)
    var autoUploadCheckedForAccount by mutableStateOf(autoUploadCheckedForAccount)
        private set

    var pendingBackupJsonContent by mutableStateOf<String?>(null)
        private set
    var pendingBackupFileName by mutableStateOf("ShiftSalaryPlanner_backup.json")
        private set
    var pendingExcelFileBytes by mutableStateOf<ByteArray?>(null)
        private set
    var excelImportPreview by mutableStateOf<ExcelImportPreview?>(null)
        private set
    var excelImportCandidates by mutableStateOf<List<ExcelPersonCandidate>>(emptyList())
        private set
    var googleSignedInAccount by mutableStateOf(initialGoogleSignedInAccount)
        private set

    fun openPostUpdateCheck() { showPostUpdateCheckDialog = true }
    fun closePostUpdateCheck() { showPostUpdateCheckDialog = false }
    fun updateExcelStatus(message: String?) { excelImportStatusMessage = message }
    fun updateBackupStatus(message: String?) { backupRestoreStatusMessage = message }
    fun markAutoUploadChecked(accountKey: String) { autoUploadCheckedForAccount = accountKey }
    fun clearAutoUploadCheck() { autoUploadCheckedForAccount = "" }

    fun stageBackupExport(content: String, fileName: String) {
        pendingBackupJsonContent = content
        pendingBackupFileName = fileName
    }

    fun clearBackupPayload() { pendingBackupJsonContent = null }

    fun stageExcelFile(bytes: ByteArray, fileName: String, statusMessage: String) {
        pendingExcelFileBytes = bytes
        pendingExcelFileName = fileName
        excelImportPreview = null
        excelImportCandidates = emptyList()
        excelImportStatusMessage = statusMessage
    }

    fun failExcelFile(statusMessage: String) {
        pendingExcelFileBytes = null
        pendingExcelFileName = null
        excelImportPreview = null
        excelImportCandidates = emptyList()
        excelImportStatusMessage = statusMessage
    }

    fun stageExcelCandidates(candidates: List<ExcelPersonCandidate>, statusMessage: String) {
        excelImportCandidates = candidates
        excelImportPreview = null
        excelImportStatusMessage = statusMessage
    }

    fun stageExcelPreview(preview: ExcelImportPreview, statusMessage: String) {
        excelImportCandidates = emptyList()
        excelImportPreview = preview
        excelImportStatusMessage = statusMessage
    }

    fun clearExcelParseState() {
        excelImportPreview = null
        excelImportCandidates = emptyList()
    }

    fun finishExcelImport(statusMessage: String) {
        excelImportStatusMessage = statusMessage
        clearExcelParseState()
    }

    fun setSignedInAccount(account: GoogleSignInAccount?) { googleSignedInAccount = account }

    fun disconnectSignedInAccount(statusMessage: String) {
        googleSignedInAccount = null
        autoUploadCheckedForAccount = ""
        backupRestoreStatusMessage = statusMessage
    }
}

private fun encodeNullableServiceValue(value: String?): String = if (value == null) "0" else "1$value"
private fun decodeNullableServiceValue(value: String?): String? = when {
    value == null || value == "0" -> null
    value.startsWith("1") -> value.substring(1)
    else -> null
}

internal fun ServiceWorkflowState.toSaveableStrings(): ArrayList<String> = arrayListOf(
    if (showPostUpdateCheckDialog) "1" else "0",
    encodeNullableServiceValue(excelImportStatusMessage),
    encodeNullableServiceValue(pendingExcelFileName),
    encodeNullableServiceValue(backupRestoreStatusMessage),
    autoUploadCheckedForAccount
)

internal fun restoreServiceWorkflowState(saved: List<String>): ServiceWorkflowState = ServiceWorkflowState(
    showPostUpdateCheckDialog = saved.getOrNull(0) == "1",
    excelImportStatusMessage = decodeNullableServiceValue(saved.getOrNull(1)),
    pendingExcelFileName = decodeNullableServiceValue(saved.getOrNull(2)),
    backupRestoreStatusMessage = decodeNullableServiceValue(saved.getOrNull(3)),
    autoUploadCheckedForAccount = saved.getOrNull(4).orEmpty()
)

val ServiceWorkflowStateSaver: Saver<ServiceWorkflowState, ArrayList<String>> = Saver(
    save = { it.toSaveableStrings() },
    restore = { restoreServiceWorkflowState(it) }
)

@Composable
fun rememberServiceWorkflowState(initialGoogleSignedInAccount: GoogleSignInAccount?): ServiceWorkflowState {
    val saver = remember(initialGoogleSignedInAccount) {
        Saver<ServiceWorkflowState, ArrayList<String>>(
            save = { it.toSaveableStrings() },
            restore = { saved ->
                restoreServiceWorkflowState(saved).also { state ->
                    state.setSignedInAccount(initialGoogleSignedInAccount)
                }
            }
        )
    }
    return rememberSaveable(saver = saver) {
        ServiceWorkflowState(initialGoogleSignedInAccount = initialGoogleSignedInAccount)
    }
}
