package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.app.ports.ProfileDataPort
import com.vigilante.shiftsalaryplanner.settings.AppProfile
import com.vigilante.shiftsalaryplanner.settings.AppProfilesState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileDataPortTest {
    @Test
    fun featureCanUseFakeProfilePortWithoutConcreteStore() {
        val initial = AppProfilesState("default", listOf(AppProfile("default", "Профиль 1")))
        val port = FakeProfileDataPort(initial)
        val created = port.createProfile("Work")
        assertTrue(port.setActiveProfile(created.id))
        assertTrue(port.renameProfile(created.id, "Renamed"))
        assertTrue(port.deleteProfile(created.id))
        port.clearProfileData(created.id)
        assertEquals(initial, port.initialState)
        assertEquals(
            listOf("create:Work", "active:p1", "rename:p1:Renamed", "delete:p1", "clear:p1"),
            port.calls
        )
    }

    private class FakeProfileDataPort(
        override val initialState: AppProfilesState
    ) : ProfileDataPort {
        override val state: Flow<AppProfilesState> = MutableStateFlow(initialState)
        val calls = mutableListOf<String>()
        override fun setActiveProfile(profileId: String): Boolean { calls += "active:$profileId"; return true }
        override fun createProfile(name: String): AppProfile { calls += "create:$name"; return AppProfile("p1", name) }
        override fun renameProfile(profileId: String, newName: String): Boolean { calls += "rename:$profileId:$newName"; return true }
        override fun deleteProfile(profileId: String): Boolean { calls += "delete:$profileId"; return true }
        override fun clearProfileData(profileId: String) { calls += "clear:$profileId" }
    }
}
