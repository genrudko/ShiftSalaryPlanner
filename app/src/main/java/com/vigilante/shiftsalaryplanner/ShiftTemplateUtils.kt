package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import java.util.Locale

fun isProtectedSystemTemplate(template: ShiftTemplateEntity?): Boolean {
    if (template == null) return false

    val code = template.code.trim().uppercase(Locale.ROOT)
    val title = template.title.trim().uppercase(Locale.ROOT)

    return code in setOf("ВЫХ", "ОТ", "Б") ||
            title in setOf("ВЫХОДНОЙ", "ОТПУСК", "БОЛЬНИЧНЫЙ")
}