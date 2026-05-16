package com.vigilante.shiftsalaryplanner.settings

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppWorkflowSettings(
    val actualAdvanceNet: Double = 0.0,
    val actualSalaryNet: Double = 0.0,
    val paymentDifferenceToleranceRub: Double = 100.0,
    val showQuickEraser: Boolean = true,
    val showQuickNormal: Boolean = true,
    val showQuickCycle: Boolean = true,
    val showQuickNewTemplate: Boolean = true,
    val showQuickClearMonth: Boolean = true,
    val showQuickClearRange: Boolean = true,
    val showQuickClearAll: Boolean = true,
    val shiftSwipeDuplicateEnabled: Boolean = true,
    val shiftSwipeDeleteEnabled: Boolean = true,
    val alarmSwipeDuplicateEnabled: Boolean = true,
    val alarmSwipeDeleteEnabled: Boolean = true,
    val lastCheckedVersionCode: Long = 0L,
    val quickStartDismissed: Boolean = false
)

class AppWorkflowSettingsStore(context: Context) {
    private val prefs = context.profileSharedPreferences(PREFS_NAME)
    private val _settingsFlow = MutableStateFlow(load())

    val settingsFlow: Flow<AppWorkflowSettings> = _settingsFlow.asStateFlow()

    fun save(settings: AppWorkflowSettings) {
        prefs.edit {
            putFloat(KEY_ACTUAL_ADVANCE_NET, settings.actualAdvanceNet.toFloat())
            putFloat(KEY_ACTUAL_SALARY_NET, settings.actualSalaryNet.toFloat())
            putFloat(KEY_PAYMENT_DIFFERENCE_TOLERANCE_RUB, settings.paymentDifferenceToleranceRub.toFloat())
            putBoolean(KEY_SHOW_QUICK_ERASER, settings.showQuickEraser)
            putBoolean(KEY_SHOW_QUICK_NORMAL, settings.showQuickNormal)
            putBoolean(KEY_SHOW_QUICK_CYCLE, settings.showQuickCycle)
            putBoolean(KEY_SHOW_QUICK_NEW_TEMPLATE, settings.showQuickNewTemplate)
            putBoolean(KEY_SHOW_QUICK_CLEAR_MONTH, settings.showQuickClearMonth)
            putBoolean(KEY_SHOW_QUICK_CLEAR_RANGE, settings.showQuickClearRange)
            putBoolean(KEY_SHOW_QUICK_CLEAR_ALL, settings.showQuickClearAll)
            putBoolean(KEY_SHIFT_SWIPE_DUPLICATE_ENABLED, settings.shiftSwipeDuplicateEnabled)
            putBoolean(KEY_SHIFT_SWIPE_DELETE_ENABLED, settings.shiftSwipeDeleteEnabled)
            putBoolean(KEY_ALARM_SWIPE_DUPLICATE_ENABLED, settings.alarmSwipeDuplicateEnabled)
            putBoolean(KEY_ALARM_SWIPE_DELETE_ENABLED, settings.alarmSwipeDeleteEnabled)
            putLong(KEY_LAST_CHECKED_VERSION_CODE, settings.lastCheckedVersionCode)
            putBoolean(KEY_QUICK_START_DISMISSED, settings.quickStartDismissed)
        }
        _settingsFlow.value = settings
    }

    fun update(transform: (AppWorkflowSettings) -> AppWorkflowSettings) {
        save(transform(_settingsFlow.value))
    }

    private fun load(): AppWorkflowSettings {
        return AppWorkflowSettings(
            actualAdvanceNet = prefs.getFloat(KEY_ACTUAL_ADVANCE_NET, 0f).toDouble(),
            actualSalaryNet = prefs.getFloat(KEY_ACTUAL_SALARY_NET, 0f).toDouble(),
            paymentDifferenceToleranceRub = prefs.getFloat(KEY_PAYMENT_DIFFERENCE_TOLERANCE_RUB, 100f)
                .toDouble()
                .coerceAtLeast(0.0),
            showQuickEraser = prefs.getBoolean(KEY_SHOW_QUICK_ERASER, true),
            showQuickNormal = prefs.getBoolean(KEY_SHOW_QUICK_NORMAL, true),
            showQuickCycle = prefs.getBoolean(KEY_SHOW_QUICK_CYCLE, true),
            showQuickNewTemplate = prefs.getBoolean(KEY_SHOW_QUICK_NEW_TEMPLATE, true),
            showQuickClearMonth = prefs.getBoolean(KEY_SHOW_QUICK_CLEAR_MONTH, true),
            showQuickClearRange = prefs.getBoolean(KEY_SHOW_QUICK_CLEAR_RANGE, true),
            showQuickClearAll = prefs.getBoolean(KEY_SHOW_QUICK_CLEAR_ALL, true),
            shiftSwipeDuplicateEnabled = prefs.getBoolean(KEY_SHIFT_SWIPE_DUPLICATE_ENABLED, true),
            shiftSwipeDeleteEnabled = prefs.getBoolean(KEY_SHIFT_SWIPE_DELETE_ENABLED, true),
            alarmSwipeDuplicateEnabled = prefs.getBoolean(KEY_ALARM_SWIPE_DUPLICATE_ENABLED, true),
            alarmSwipeDeleteEnabled = prefs.getBoolean(KEY_ALARM_SWIPE_DELETE_ENABLED, true),
            lastCheckedVersionCode = prefs.getLong(KEY_LAST_CHECKED_VERSION_CODE, 0L),
            quickStartDismissed = prefs.getBoolean(KEY_QUICK_START_DISMISSED, false)
        )
    }

    companion object {
        const val PREFS_NAME = "app_workflow_settings"

        private const val KEY_ACTUAL_ADVANCE_NET = "actual_advance_net"
        private const val KEY_ACTUAL_SALARY_NET = "actual_salary_net"
        private const val KEY_PAYMENT_DIFFERENCE_TOLERANCE_RUB = "payment_difference_tolerance_rub"
        private const val KEY_SHOW_QUICK_ERASER = "show_quick_eraser"
        private const val KEY_SHOW_QUICK_NORMAL = "show_quick_normal"
        private const val KEY_SHOW_QUICK_CYCLE = "show_quick_cycle"
        private const val KEY_SHOW_QUICK_NEW_TEMPLATE = "show_quick_new_template"
        private const val KEY_SHOW_QUICK_CLEAR_MONTH = "show_quick_clear_month"
        private const val KEY_SHOW_QUICK_CLEAR_RANGE = "show_quick_clear_range"
        private const val KEY_SHOW_QUICK_CLEAR_ALL = "show_quick_clear_all"
        private const val KEY_SHIFT_SWIPE_DUPLICATE_ENABLED = "shift_swipe_duplicate_enabled"
        private const val KEY_SHIFT_SWIPE_DELETE_ENABLED = "shift_swipe_delete_enabled"
        private const val KEY_ALARM_SWIPE_DUPLICATE_ENABLED = "alarm_swipe_duplicate_enabled"
        private const val KEY_ALARM_SWIPE_DELETE_ENABLED = "alarm_swipe_delete_enabled"
        private const val KEY_LAST_CHECKED_VERSION_CODE = "last_checked_version_code"
        private const val KEY_QUICK_START_DISMISSED = "quick_start_dismissed"
    }
}
