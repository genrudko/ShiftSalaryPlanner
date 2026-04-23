package com.vigilante.shiftsalaryplanner.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class AppProfile(
    val id: String,
    val name: String
)

data class AppProfilesState(
    val activeProfileId: String,
    val profiles: List<AppProfile>
) {
    val activeProfile: AppProfile?
        get() = profiles.firstOrNull { it.id == activeProfileId }
}

class AppProfileStore(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _stateFlow = MutableStateFlow(loadState())
    val stateFlow: Flow<AppProfilesState> = _stateFlow.asStateFlow()

    fun reload() {
        _stateFlow.value = loadState()
    }

    fun setActiveProfile(profileId: String): Boolean {
        val current = loadState()
        if (current.profiles.none { it.id == profileId }) return false
        saveState(current.copy(activeProfileId = profileId))
        return true
    }

    fun createProfile(name: String): AppProfile {
        val normalizedName = name.trim().ifBlank {
            "Профиль ${loadState().profiles.size + 1}"
        }
        val current = loadState()
        val nextProfile = AppProfile(
            id = generateProfileId(),
            name = normalizedName
        )
        saveState(
            current.copy(
                activeProfileId = nextProfile.id,
                profiles = (current.profiles + nextProfile).distinctBy { it.id }
            )
        )
        return nextProfile
    }

    fun renameProfile(profileId: String, newName: String): Boolean {
        val normalizedName = newName.trim()
        if (normalizedName.isBlank()) return false
        val current = loadState()
        var changed = false
        val updated = current.profiles.map { profile ->
            if (profile.id == profileId) {
                changed = true
                profile.copy(name = normalizedName)
            } else {
                profile
            }
        }
        if (!changed) return false
        saveState(current.copy(profiles = updated))
        return true
    }

    fun deleteProfile(profileId: String): Boolean {
        if (profileId == DEFAULT_PROFILE_ID) return false
        val current = loadState()
        if (current.profiles.none { it.id == profileId }) return false
        if (current.profiles.size <= 1) return false

        val updatedProfiles = current.profiles.filterNot { it.id == profileId }
        val nextActive = if (current.activeProfileId == profileId) {
            updatedProfiles.firstOrNull()?.id ?: DEFAULT_PROFILE_ID
        } else {
            current.activeProfileId
        }
        saveState(
            current.copy(
                activeProfileId = nextActive,
                profiles = updatedProfiles
            )
        )
        return true
    }

    fun clearProfileData(profileId: String) {
        if (profileId == DEFAULT_PROFILE_ID) return

        PROFILE_PREFS_BASE_NAMES.forEach { baseName ->
            val scopedName = scopedPrefsName(baseName, profileId)
            context.getSharedPreferences(scopedName, Context.MODE_PRIVATE).edit { clear() }
        }

        runCatching {
            context.deleteDatabase(scopedDatabaseName(profileId))
        }
    }

    private fun loadState(): AppProfilesState {
        val rawProfiles = prefs.getString(KEY_PROFILES_JSON, null)
        val parsedProfiles = parseProfiles(rawProfiles)
        val profiles = if (parsedProfiles.isEmpty()) {
            listOf(AppProfile(DEFAULT_PROFILE_ID, DEFAULT_PROFILE_NAME))
        } else {
            parsedProfiles.distinctBy { it.id }.sortedBy { if (it.id == DEFAULT_PROFILE_ID) 0 else 1 }
        }
        val activeIdRaw = prefs.getString(KEY_ACTIVE_PROFILE_ID, DEFAULT_PROFILE_ID).orEmpty()
        val activeId = if (profiles.any { it.id == activeIdRaw }) activeIdRaw else profiles.first().id
        return AppProfilesState(
            activeProfileId = activeId,
            profiles = profiles
        )
    }

    private fun saveState(state: AppProfilesState) {
        val safeProfiles = state.profiles.ifEmpty {
            listOf(AppProfile(DEFAULT_PROFILE_ID, DEFAULT_PROFILE_NAME))
        }
        val safeActive = if (safeProfiles.any { it.id == state.activeProfileId }) {
            state.activeProfileId
        } else {
            safeProfiles.first().id
        }
        prefs.edit {
            putString(KEY_ACTIVE_PROFILE_ID, safeActive)
            putString(KEY_PROFILES_JSON, serializeProfiles(safeProfiles))
        }
        _stateFlow.value = AppProfilesState(
            activeProfileId = safeActive,
            profiles = safeProfiles
        )
    }

    private fun parseProfiles(raw: String?): List<AppProfile> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optString("id").trim()
                    val name = item.optString("name").trim()
                    if (id.isBlank() || name.isBlank()) continue
                    add(AppProfile(id = id, name = name))
                }
            }
        }.getOrElse { emptyList() }
    }

    private fun serializeProfiles(profiles: List<AppProfile>): String {
        val array = JSONArray()
        profiles.forEach { profile ->
            array.put(
                JSONObject().apply {
                    put("id", profile.id)
                    put("name", profile.name)
                }
            )
        }
        return array.toString()
    }

    private fun generateProfileId(): String {
        return "p_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
    }

    companion object {
        private const val PREFS_NAME = "app_profiles_meta"
        private const val KEY_ACTIVE_PROFILE_ID = "active_profile_id"
        private const val KEY_PROFILES_JSON = "profiles_json"

        private val PROFILE_PREFS_BASE_NAMES = listOf(
            "appearance_settings",
            "payroll_settings",
            "payroll_ytd",
            "report_visibility_settings",
            "work_assignments",
            "workplace_payroll_salaries",
            "workplace_payroll_settings",
            "shift_alarm_settings",
            "shift_alarm_scheduler",
            "pattern_templates",
            "additional_payments",
            "deductions",
            "google_drive_sync_meta",
            "shift_colors",
            "shift_special_rules",
            "manual_holidays",
            "calendar_sync_meta",
            "widget_settings",
            "sick_limits_cache"
        )

        const val DEFAULT_PROFILE_ID: String = "default"
        const val DEFAULT_PROFILE_NAME: String = "Профиль 1"

        fun resolveActiveProfileId(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_ACTIVE_PROFILE_ID, DEFAULT_PROFILE_ID)
                ?.takeIf { it.isNotBlank() }
                ?: DEFAULT_PROFILE_ID
        }
    }
}

fun scopedPrefsName(baseName: String, profileId: String): String {
    return if (profileId == AppProfileStore.DEFAULT_PROFILE_ID) {
        baseName
    } else {
        "${baseName}__profile_${profileId}"
    }
}

fun scopedDatabaseName(profileId: String): String {
    return if (profileId == AppProfileStore.DEFAULT_PROFILE_ID) {
        "shift_salary_planner.db"
    } else {
        "shift_salary_planner_${profileId}.db"
    }
}

fun Context.profileSharedPreferences(
    baseName: String,
    profileId: String = AppProfileStore.resolveActiveProfileId(this)
): SharedPreferences {
    return getSharedPreferences(scopedPrefsName(baseName, profileId), Context.MODE_PRIVATE)
}
