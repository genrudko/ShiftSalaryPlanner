package com.vigilante.shiftsalaryplanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vigilante.shiftsalaryplanner.data.AppDatabase
import com.vigilante.shiftsalaryplanner.settings.ShiftAlarmStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ShiftAlarmSystemReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        if (
            action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != Intent.ACTION_TIME_CHANGED &&
            action != Intent.ACTION_TIMEZONE_CHANGED
        ) {
            return
        }

        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val settings = ShiftAlarmStore(context).settingsFlow.first()
                val savedDays = db.shiftDayDao().observeAll().first()
                val templates = db.shiftTemplateDao().observeAll().first()

                ShiftAlarmScheduler.reschedule(
                    context = context,
                    settings = settings,
                    savedDays = savedDays,
                    templateMap = templates.associateBy { it.code }
                )
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
                scope.cancel()
            }
        }
    }
}
