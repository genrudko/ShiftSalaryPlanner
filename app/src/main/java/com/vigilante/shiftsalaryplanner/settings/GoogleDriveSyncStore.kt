package com.vigilante.shiftsalaryplanner.settings

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val DEFAULT_AUTO_UPLOAD_INTERVAL_HOURS = 24

data class GoogleDriveSyncMeta(
    val accountEmail: String = "",
    val lastUploadAt: Long = 0L,
    val lastRestoreAt: Long = 0L,
    val lastCloudModifiedAt: Long = 0L,
    val autoUploadEnabled: Boolean = false,
    val autoUploadIntervalHours: Int = DEFAULT_AUTO_UPLOAD_INTERVAL_HOURS
)

class GoogleDriveSyncStore(context: Context) {

    private val prefs = context.profileSharedPreferences(PREFS_NAME)
    private val _metaFlow = MutableStateFlow(loadFromPrefs())
    val metaFlow: Flow<GoogleDriveSyncMeta> = _metaFlow.asStateFlow()

    fun setAccountEmail(email: String) {
        val normalized = email.trim()
        val current = _metaFlow.value
        save(
            current.copy(
                accountEmail = normalized
            )
        )
    }

    fun markUpload(
        nowMillis: Long = System.currentTimeMillis(),
        cloudModifiedAtMillis: Long = 0L
    ) {
        val current = _metaFlow.value
        save(
            current.copy(
                lastUploadAt = nowMillis,
                lastCloudModifiedAt = if (cloudModifiedAtMillis > 0L) {
                    cloudModifiedAtMillis
                } else {
                    current.lastCloudModifiedAt
                }
            )
        )
    }

    fun markRestore(
        nowMillis: Long = System.currentTimeMillis(),
        cloudModifiedAtMillis: Long = 0L
    ) {
        val current = _metaFlow.value
        save(
            current.copy(
                lastRestoreAt = nowMillis,
                lastCloudModifiedAt = cloudModifiedAtMillis
            )
        )
    }

    fun setAutoUploadEnabled(enabled: Boolean) {
        val current = _metaFlow.value
        save(current.copy(autoUploadEnabled = enabled))
    }

    fun setAutoUploadIntervalHours(hours: Int) {
        val normalized = normalizeAutoUploadIntervalHours(hours)
        val current = _metaFlow.value
        save(current.copy(autoUploadIntervalHours = normalized))
    }

    private fun save(meta: GoogleDriveSyncMeta) {
        prefs.edit {
            putString(KEY_ACCOUNT_EMAIL, meta.accountEmail)
            putLong(KEY_LAST_UPLOAD_AT, meta.lastUploadAt)
            putLong(KEY_LAST_RESTORE_AT, meta.lastRestoreAt)
            putLong(KEY_LAST_CLOUD_MODIFIED_AT, meta.lastCloudModifiedAt)
            putBoolean(KEY_AUTO_UPLOAD_ENABLED, meta.autoUploadEnabled)
            putInt(KEY_AUTO_UPLOAD_INTERVAL_HOURS, meta.autoUploadIntervalHours)
        }
        _metaFlow.value = meta
    }

    private fun loadFromPrefs(): GoogleDriveSyncMeta {
        return GoogleDriveSyncMeta(
            accountEmail = prefs.getString(KEY_ACCOUNT_EMAIL, "") ?: "",
            lastUploadAt = prefs.getLong(KEY_LAST_UPLOAD_AT, 0L),
            lastRestoreAt = prefs.getLong(KEY_LAST_RESTORE_AT, 0L),
            lastCloudModifiedAt = prefs.getLong(KEY_LAST_CLOUD_MODIFIED_AT, 0L),
            autoUploadEnabled = prefs.getBoolean(KEY_AUTO_UPLOAD_ENABLED, false),
            autoUploadIntervalHours = normalizeAutoUploadIntervalHours(
                prefs.getInt(
                    KEY_AUTO_UPLOAD_INTERVAL_HOURS,
                    DEFAULT_AUTO_UPLOAD_INTERVAL_HOURS
                )
            )
        )
    }

    private fun normalizeAutoUploadIntervalHours(hours: Int): Int {
        return hours.coerceIn(1, 24 * 365)
    }

    companion object {
        private const val PREFS_NAME = "google_drive_sync_meta"
        private const val KEY_ACCOUNT_EMAIL = "account_email"
        private const val KEY_LAST_UPLOAD_AT = "last_upload_at"
        private const val KEY_LAST_RESTORE_AT = "last_restore_at"
        private const val KEY_LAST_CLOUD_MODIFIED_AT = "last_cloud_modified_at"
        private const val KEY_AUTO_UPLOAD_ENABLED = "auto_upload_enabled"
        private const val KEY_AUTO_UPLOAD_INTERVAL_HOURS = "auto_upload_interval_hours"
    }
}
