package com.vigilante.shiftsalaryplanner.wear.alarm

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed interface WearAlarmControlEvent {
    val alarmKey: String

    data class Stop(override val alarmKey: String) : WearAlarmControlEvent
    data class Snooze(override val alarmKey: String) : WearAlarmControlEvent
}

object WearAlarmEvents {
    private val _events = MutableSharedFlow<WearAlarmControlEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<WearAlarmControlEvent> = _events.asSharedFlow()

    fun stop(alarmKey: String) {
        _events.tryEmit(WearAlarmControlEvent.Stop(alarmKey))
    }

    fun snooze(alarmKey: String) {
        _events.tryEmit(WearAlarmControlEvent.Snooze(alarmKey))
    }
}
