package com.vigilante.shiftsalaryplanner.wear.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vigilante.shiftsalaryplanner.wear.sync.WearSyncContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject

class WearAlarmActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val raw = intent.getStringExtra(EXTRA_ALARM_JSON).orEmpty()
        val payload = runCatching { WearAlarmPayload.fromJson(JSONObject(raw)) }
            .getOrDefault(WearAlarmPayload())
        WearAlarmNotificationHelper.cancel(context, payload.alarmKey)
        when (intent.action) {
            ACTION_DISMISS -> WearAlarmEvents.stop(payload.alarmKey)
            ACTION_SNOOZE -> WearAlarmEvents.snooze(payload.alarmKey)
        }
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching {
                WearAlarmCommandSender.send(
                    context = context,
                    path = if (intent.action == ACTION_SNOOZE) {
                        WearSyncContract.PATH_ALARM_SNOOZE_FROM_WEAR
                    } else {
                        WearSyncContract.PATH_ALARM_DISMISS_FROM_WEAR
                    },
                    payload = payload.toJson()
                )
            }
            pendingResult.finish()
        }
    }

    companion object {
        const val ACTION_DISMISS = "com.vigilante.shiftsalaryplanner.wear.action.ALARM_DISMISS"
        const val ACTION_SNOOZE = "com.vigilante.shiftsalaryplanner.wear.action.ALARM_SNOOZE"
        const val EXTRA_ALARM_JSON = "extra_alarm_json"
    }
}
