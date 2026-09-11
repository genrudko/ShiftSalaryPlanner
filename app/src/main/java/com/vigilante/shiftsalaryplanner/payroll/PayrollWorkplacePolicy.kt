package com.vigilante.shiftsalaryplanner.payroll

import com.vigilante.shiftsalaryplanner.data.ShiftDayEntity
import com.vigilante.shiftsalaryplanner.isSystemStatusCode
import com.vigilante.shiftsalaryplanner.settings.WORKPLACE_MAIN_ID
import com.vigilante.shiftsalaryplanner.workplaceIdFromShiftCode
import java.time.LocalDate

/**
 * Preserves the v7.1 payroll-settings selection policy for the "all workplaces" filter.
 * This is intentionally a pure policy seam so future UI/architecture changes cannot silently
 * alter which workplace's payroll settings drive a period calculation.
 */
fun resolvePayrollSettingsWorkplaceId(
    selectedWorkplaceId: String,
    allWorkplacesId: String,
    periodStart: LocalDate,
    periodEnd: LocalDate,
    savedDays: List<ShiftDayEntity>,
    extraAssignmentsByDate: Map<LocalDate, Map<String, String>>,
    systemStatusCodes: Set<String>
): String {
    if (selectedWorkplaceId != allWorkplacesId) return selectedWorkplaceId

    val periodWorkplaceIds = buildSet {
        savedDays.forEach { day ->
            val date = runCatching { LocalDate.parse(day.date) }.getOrNull() ?: return@forEach
            if (
                !date.isBefore(periodStart) &&
                !date.isAfter(periodEnd) &&
                day.shiftCode.isNotBlank() &&
                !isSystemStatusCode(day.shiftCode, systemStatusCodes)
            ) {
                add(workplaceIdFromShiftCode(day.shiftCode))
            }
        }
        extraAssignmentsByDate.forEach { (date, assignments) ->
            if (date.isBefore(periodStart) || date.isAfter(periodEnd)) return@forEach
            assignments.forEach { (workplaceId, code) ->
                if (code.isNotBlank() && !isSystemStatusCode(code, systemStatusCodes)) {
                    add(workplaceId.trim().ifBlank { WORKPLACE_MAIN_ID })
                }
            }
        }
    }

    return periodWorkplaceIds.singleOrNull() ?: allWorkplacesId
}
