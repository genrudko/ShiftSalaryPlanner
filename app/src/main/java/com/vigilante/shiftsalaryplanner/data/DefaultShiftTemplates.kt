package com.vigilante.shiftsalaryplanner.data

object DefaultShiftTemplates {

    fun items(): List<ShiftTemplateEntity> {
        return listOf(
            ShiftTemplateEntity(
                code = "Д",
                title = "Дневная",
                iconKey = "SUN",
                totalHours = 11.5,
                breakHours = 0.75,
                nightHours = 0.0,
                colorHex = "#1E88E5",
                isWeekendPaid = false,
                active = true,
                sortOrder = 10
            ),
            ShiftTemplateEntity(
                code = "Н",
                title = "Ночная",
                iconKey = "MOON",
                totalHours = 11.5,
                breakHours = 0.75,
                nightHours = 8.0,
                colorHex = "#43A047",
                isWeekendPaid = false,
                active = true,
                sortOrder = 20
            ),
            ShiftTemplateEntity(
                code = "8",
                title = "8 СП",
                iconKey = "EIGHT",
                totalHours = 8.0,
                breakHours = 0.0,
                nightHours = 0.0,
                colorHex = "#EF5350",
                isWeekendPaid = false,
                active = true,
                sortOrder = 30
            ),
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
            ),
            ShiftTemplateEntity(
                code = "РВД",
                title = "Работа в выходной день",
                iconKey = "STAR",
                totalHours = 11.5,
                breakHours = 0.75,
                nightHours = 0.0,
                colorHex = "#66BB6A",
                isWeekendPaid = true,
                active = true,
                sortOrder = 70
            ),
            ShiftTemplateEntity(
                code = "РВН",
                title = "Работа в выходной день (ночь)",
                iconKey = "STAR",
                totalHours = 11.5,
                breakHours = 0.75,
                nightHours = 8.0,
                colorHex = "#BDBDBD",
                isWeekendPaid = true,
                active = true,
                sortOrder = 80
            )
        )
    }
}