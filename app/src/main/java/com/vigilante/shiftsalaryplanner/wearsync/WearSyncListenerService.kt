package com.vigilante.shiftsalaryplanner.wearsync

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class WearSyncListenerService : WearableListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val payload = messageEvent.data ?: ByteArray(0)
        val appContext = applicationContext
        serviceScope.launch {
            when (messageEvent.path) {
                WearSyncContract.PATH_REQUEST_SNAPSHOT -> {
                    WearSyncBridge.publishSnapshot(appContext)
                }
                WearSyncContract.PATH_ADD_NOTE -> {
                    WearSyncBridge.addNoteFromWear(appContext, payload)
                }
                WearSyncContract.PATH_TOGGLE_ALL_ALARMS -> {
                    WearSyncBridge.setAllAlarmsEnabledFromWear(appContext, payload)
                }
                WearSyncContract.PATH_TOGGLE_TEMPLATE_ALARM -> {
                    WearSyncBridge.setTemplateAlarmEnabledFromWear(appContext, payload)
                }
                WearSyncContract.PATH_ASSISTANT_PROMPT -> {
                    WearSyncBridge.askAssistantFromWear(appContext, payload)
                }
                WearSyncContract.PATH_ALARM_DISMISS_FROM_WEAR -> {
                    WearSyncBridge.dismissAlarmFromWear(appContext, payload)
                }
                WearSyncContract.PATH_ALARM_SNOOZE_FROM_WEAR -> {
                    WearSyncBridge.snoozeAlarmFromWear(appContext, payload)
                }
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
