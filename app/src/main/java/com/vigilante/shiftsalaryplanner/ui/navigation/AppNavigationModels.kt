package com.vigilante.shiftsalaryplanner

import androidx.compose.ui.graphics.vector.ImageVector

enum class BottomTab(
    val label: String,
    val icon: ImageVector,
    val contentDescription: String = label
) {
    CALENDAR("Календарь", appSemanticIcon(AppSemanticIconKey.CALENDAR)),
    TODAY("Сегодня", appSemanticIcon(AppSemanticIconKey.TODAY)),
    ASSISTANT("ИИ", appSemanticIcon(AppSemanticIconKey.ASSISTANT), "ИИ-ассистент"),
    NOTES("Заметки", appSemanticIcon(AppSemanticIconKey.NOTES)),
    FINANCE("Финансы", appSemanticIcon(AppSemanticIconKey.FINANCE)),
    ALARMS("Будильники", appSemanticIcon(AppSemanticIconKey.ALARMS)),
    SHIFTS("Смены", appSemanticIcon(AppSemanticIconKey.SHIFT_TEMPLATES)),
    SETTINGS("Ещё", appSemanticIcon(AppSemanticIconKey.MORE))
}

val primaryBottomTabs: List<BottomTab> = listOf(
    BottomTab.CALENDAR,
    BottomTab.FINANCE,
    BottomTab.SETTINGS
)

fun primaryTabFor(tab: BottomTab): BottomTab = when (tab) {
    BottomTab.CALENDAR, BottomTab.TODAY -> BottomTab.CALENDAR
    BottomTab.FINANCE -> BottomTab.FINANCE
    else -> BottomTab.SETTINGS
}
