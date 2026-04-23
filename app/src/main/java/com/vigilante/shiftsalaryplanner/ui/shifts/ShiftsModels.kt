package com.vigilante.shiftsalaryplanner

import androidx.core.content.edit
import com.vigilante.shiftsalaryplanner.payroll.SpecialDayCompensation
import com.vigilante.shiftsalaryplanner.payroll.SpecialDayType

fun defaultShiftSpecialRule(fallbackWeekendPaid: Boolean): ShiftSpecialRule {
    return if (fallbackWeekendPaid) {
        ShiftSpecialRule(
            specialDayTypeName = SpecialDayType.WEEKEND_HOLIDAY.name,
            specialDayCompensationName = SpecialDayCompensation.DOUBLE_PAY.name
        )
    } else {
        ShiftSpecialRule(
            specialDayTypeName = SpecialDayType.NONE.name,
            specialDayCompensationName = SpecialDayCompensation.NONE.name
        )
    }
}

fun readShiftSpecialRule(
    prefs: android.content.SharedPreferences,
    code: String,
    fallbackWeekendPaid: Boolean
): ShiftSpecialRule {
    val fallback = defaultShiftSpecialRule(fallbackWeekendPaid)
    return ShiftSpecialRule(
        specialDayTypeName = prefs.getString("special_day_type_$code", fallback.specialDayTypeName) ?: fallback.specialDayTypeName,
        specialDayCompensationName = prefs.getString("special_day_compensation_$code", fallback.specialDayCompensationName)
            ?: fallback.specialDayCompensationName,
        isSystemStatus = prefs.getBoolean("special_system_status_$code", fallback.isSystemStatus)
    )
}

fun writeShiftSpecialRule(
    prefs: android.content.SharedPreferences,
    code: String,
    rule: ShiftSpecialRule
) {
    prefs.edit {
        putString("special_day_type_$code", rule.specialDayTypeName)
        putString("special_day_compensation_$code", rule.specialDayCompensationName)
        putBoolean("special_system_status_$code", rule.isSystemStatus)
    }
}

fun deleteShiftSpecialRule(
    prefs: android.content.SharedPreferences,
    code: String
) {
    prefs.edit {
        remove("special_day_type_$code")
        remove("special_day_compensation_$code")
        remove("special_system_status_$code")
    }
}

fun resolveSpecialDayType(
    rule: ShiftSpecialRule?,
    fallbackWeekendPaid: Boolean
): SpecialDayType {
    return runCatching {
        SpecialDayType.valueOf(rule?.specialDayTypeName ?: defaultShiftSpecialRule(fallbackWeekendPaid).specialDayTypeName)
    }.getOrElse {
        if (fallbackWeekendPaid) SpecialDayType.WEEKEND_HOLIDAY else SpecialDayType.NONE
    }
}

fun resolveSpecialDayCompensation(
    rule: ShiftSpecialRule?,
    fallbackWeekendPaid: Boolean
): SpecialDayCompensation {
    return runCatching {
        SpecialDayCompensation.valueOf(
            rule?.specialDayCompensationName ?: defaultShiftSpecialRule(fallbackWeekendPaid).specialDayCompensationName
        )
    }.getOrElse {
        if (fallbackWeekendPaid) SpecialDayCompensation.DOUBLE_PAY else SpecialDayCompensation.NONE
    }
}

fun legacyWeekendPaidFlag(
    specialDayType: SpecialDayType,
    specialDayCompensation: SpecialDayCompensation
): Boolean {
    return when (specialDayType) {
        SpecialDayType.NONE -> false
        SpecialDayType.WEEKEND_HOLIDAY -> true
        SpecialDayType.RVD -> specialDayCompensation == SpecialDayCompensation.DOUBLE_PAY
    }
}

fun specialShiftRuleLabel(
    rule: ShiftSpecialRule?,
    fallbackWeekendPaid: Boolean
): String {
    val specialDayType = resolveSpecialDayType(rule, fallbackWeekendPaid)
    val specialDayCompensation = resolveSpecialDayCompensation(rule, fallbackWeekendPaid)
    return when (specialDayType) {
        SpecialDayType.NONE -> "Обычная"
        SpecialDayType.WEEKEND_HOLIDAY -> "Выходная / праздничная"
        SpecialDayType.RVD -> when (specialDayCompensation) {
            SpecialDayCompensation.DOUBLE_PAY -> "РВД • двойная оплата"
            SpecialDayCompensation.SINGLE_PAY_WITH_DAY_OFF -> "РВД • одинарная + отгул"
            SpecialDayCompensation.NONE -> "РВД"
        }
    }
}
