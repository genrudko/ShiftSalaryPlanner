package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.settings.WORKPLACE_MAIN_ID

private const val WORKPLACE_TEMPLATE_PREFIX = "wp:"
private const val WORKPLACE_TEMPLATE_SEPARATOR = "::"

fun workplaceIdFromShiftCode(code: String): String {
    if (!isWorkplaceScopedShiftCode(code)) return WORKPLACE_MAIN_ID
    val withoutPrefix = code.removePrefix(WORKPLACE_TEMPLATE_PREFIX)
    val separatorIndex = withoutPrefix.indexOf(WORKPLACE_TEMPLATE_SEPARATOR)
    if (separatorIndex <= 0) return WORKPLACE_MAIN_ID
    return withoutPrefix.substring(0, separatorIndex).ifBlank { WORKPLACE_MAIN_ID }
}

fun workplaceScopedShiftCode(workplaceId: String, baseCode: String): String {
    if (workplaceId == WORKPLACE_MAIN_ID) return baseCode
    if (isWorkplaceScopedShiftCode(baseCode)) return baseCode
    return "$WORKPLACE_TEMPLATE_PREFIX$workplaceId$WORKPLACE_TEMPLATE_SEPARATOR$baseCode"
}

fun isWorkplaceScopedShiftCode(code: String): Boolean {
    return code.startsWith(WORKPLACE_TEMPLATE_PREFIX) && code.contains(WORKPLACE_TEMPLATE_SEPARATOR)
}

fun isShiftCodeForWorkplace(code: String, workplaceId: String): Boolean {
    if (workplaceId == WORKPLACE_MAIN_ID) return !isWorkplaceScopedShiftCode(code)
    return code.startsWith("$WORKPLACE_TEMPLATE_PREFIX$workplaceId$WORKPLACE_TEMPLATE_SEPARATOR")
}

fun stripWorkplaceScopeFromShiftCode(code: String): String {
    if (!isWorkplaceScopedShiftCode(code)) return code
    val separatorIndex = code.indexOf(WORKPLACE_TEMPLATE_SEPARATOR)
    if (separatorIndex < 0) return code
    return code.substring(separatorIndex + WORKPLACE_TEMPLATE_SEPARATOR.length)
}
