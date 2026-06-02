package com.vigilante.shiftsalaryplanner.wear.alarm

import android.app.PendingIntent
import android.content.Intent
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.vigilante.shiftsalaryplanner.wear.sync.WearSnapshotCache
import com.vigilante.shiftsalaryplanner.wear.sync.WearSyncContract

class WearAlarmListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            WearSyncContract.PATH_ALARM_RING -> openAlarmScreen(messageEvent.data ?: ByteArray(0))
            WearSyncContract.PATH_ALARM_STOP -> {
                val alarmKey = WearAlarmPayload.fromBytes(messageEvent.data ?: ByteArray(0)).alarmKey
                WearAlarmNotificationHelper.cancel(this, alarmKey)
                WearAlarmEvents.stop(alarmKey)
            }
            WearSyncContract.PATH_ALARM_SNOOZE -> {
                val alarmKey = WearAlarmPayload.fromBytes(messageEvent.data ?: ByteArray(0)).alarmKey
                WearAlarmNotificationHelper.cancel(this, alarmKey)
                WearAlarmEvents.snooze(alarmKey)
            }
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type != DataEvent.TYPE_CHANGED) return@forEach
            val item = event.dataItem
            if (item.uri.path != WearSyncContract.SNAPSHOT_PATH) return@forEach
            val raw = DataMapItem.fromDataItem(item)
                .dataMap
                .getString(WearSyncContract.KEY_SNAPSHOT_JSON)
                .orEmpty()
            WearSnapshotCache.save(this, raw)
        }
    }

    private fun openAlarmScreen(payload: ByteArray) {
        WearAlarmNotificationHelper.show(this, WearAlarmPayload.fromBytes(payload))
        val intent = Intent(this, WearAlarmRingActivity::class.java).apply {
            putExtra(WearAlarmRingActivity.EXTRA_ALARM_JSON, payload.toString(Charsets.UTF_8))
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pending = PendingIntent.getActivity(
            this,
            requestCodeFor(payload.toString(Charsets.UTF_8)),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val launched = runCatching { pending.send() }.isSuccess
        if (!launched) {
            runCatching { startActivity(intent) }
        }
    }

    private fun requestCodeFor(value: String): Int = value.hashCode() and 0x7fffffff
}
