package com.vigilante.shiftsalaryplanner.app.ports

import android.content.Context
import com.vigilante.shiftsalaryplanner.settings.AppProfile
import com.vigilante.shiftsalaryplanner.settings.AppProfileStore
import com.vigilante.shiftsalaryplanner.settings.AppProfilesState
import kotlinx.coroutines.flow.Flow

interface ProfileDataPort {
    val state: Flow<AppProfilesState>
    val initialState: AppProfilesState
    fun setActiveProfile(profileId: String): Boolean
    fun createProfile(name: String): AppProfile
    fun renameProfile(profileId: String, newName: String): Boolean
    fun deleteProfile(profileId: String): Boolean
    fun clearProfileData(profileId: String)
}

class DefaultProfileDataPort(context: Context) : ProfileDataPort {
    private val store = AppProfileStore(context)
    override val state: Flow<AppProfilesState> = store.stateFlow
    override val initialState = AppProfilesState(
        activeProfileId = AppProfileStore.resolveActiveProfileId(context),
        profiles = listOf(
            AppProfile(
                id = AppProfileStore.DEFAULT_PROFILE_ID,
                name = AppProfileStore.DEFAULT_PROFILE_NAME
            )
        )
    )

    override fun setActiveProfile(profileId: String) = store.setActiveProfile(profileId)
    override fun createProfile(name: String) = store.createProfile(name)
    override fun renameProfile(profileId: String, newName: String) = store.renameProfile(profileId, newName)
    override fun deleteProfile(profileId: String) = store.deleteProfile(profileId)
    override fun clearProfileData(profileId: String) = store.clearProfileData(profileId)
}
