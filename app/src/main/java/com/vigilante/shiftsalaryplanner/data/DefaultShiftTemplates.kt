package com.vigilante.shiftsalaryplanner.data

object DefaultShiftTemplates {

    fun items(): List<ShiftTemplateEntity> {
        return listOf(
            ShiftTemplateEntity(
                code = "ВЫХ",
                title = "Выходной",
                iconKey = "HOME",
                totalHours = 0.0,
                breakHours = 0.0,
                nightHours = 0.0,
                colorHex = "#F9A825",
                isWeekendPaid = false,
                active = true,
                sortOrder = 40
            ),
            ShiftTemplateEntity(
                code = "ОТ",
                title = "Отпуск",
                iconKey = "OT",
                totalHours = 0.0,
                breakHours = 0.0,
                nightHours = 0.0,
                colorHex = "#26A69A",
                isWeekendPaid = false,
                active = true,
                sortOrder = 50
            ),
            ShiftTemplateEntity(
                code = "Б",
                title = "Больничный",
                iconKey = "SICK",
                totalHours = 0.0,
                breakHours = 0.0,
                nightHours = 0.0,
                colorHex = "#EC407A",
                isWeekendPaid = false,
                active = true,
                sortOrder = 60
            )
        )
    }
}
