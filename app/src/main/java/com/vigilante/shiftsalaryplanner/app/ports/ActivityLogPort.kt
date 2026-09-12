package com.vigilante.shiftsalaryplanner.app.ports

import com.vigilante.shiftsalaryplanner.settings.AppEventLogItem
import com.vigilante.shiftsalaryplanner.settings.AppEventLogStore
import kotlinx.coroutines.flow.Flow

interface ActivityLogPort {
    val events: Flow<List<AppEventLogItem>>
    fun add(title: String, message: String = "", category: String = "INFO")
    fun clear()
}

class DefaultActivityLogPort(
    private val store: AppEventLogStore
) : ActivityLogPort {
    override val events: Flow<List<AppEventLogItem>> = store.eventsFlow
    override fun add(title: String, message: String, category: String) = store.add(title, message, category)
    override fun clear() = store.clear()
}
