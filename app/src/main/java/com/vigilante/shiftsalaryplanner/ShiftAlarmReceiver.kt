package com.vigilante.shiftsalaryplanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class ShiftAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ShiftAlarmScheduler.ensureNotificationChannel(context)
        ShiftAlarmRingingService.ensureRingingChannel(context)

        val serviceIntent = Intent(context, ShiftAlarmRingingService::class.java).apply {
            action = ShiftAlarmRingingService.ACTION_START_ALARM
            putExtras(intent)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
