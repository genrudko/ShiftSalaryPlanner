package com.vigilante.shiftsalaryplanner.data

object FederalHolidaySeed {

    fun federal2026(): List<HolidayEntity> {
        val result = mutableListOf<HolidayEntity>()

        fun addHoliday(
            date: String,
            title: String,
            kind: String = HolidayKinds.HOLIDAY
        ) {
            result.add(
                HolidayEntity(
                    id = "RU-FED|$date|$kind",
                    date = date,
                    title = title,
                    scopeCode = "RU-FED",
                    kind = kind,
                    isNonWorking = true
                )
            )
        }

        addHoliday("2026-01-01", "Новогодние каникулы")
        addHoliday("2026-01-02", "Новогодние каникулы")
        addHoliday("2026-01-03", "Новогодние каникулы")
        addHoliday("2026-01-04", "Новогодние каникулы")
        addHoliday("2026-01-05", "Новогодние каникулы")
        addHoliday("2026-01-06", "Новогодние каникулы")
        addHoliday("2026-01-07", "Рождество Христово")
        addHoliday("2026-01-08", "Новогодние каникулы")

        addHoliday(
            date = "2026-01-09",
            title = "Перенесённый выходной (с 3 января)",
            kind = HolidayKinds.TRANSFERRED_DAY_OFF
        )

        addHoliday("2026-02-23", "День защитника Отечества")
        addHoliday("2026-03-08", "Международный женский день")

        addHoliday(
            date = "2026-03-09",
            title = "Перенесённый выходной после 8 марта",
            kind = HolidayKinds.TRANSFERRED_DAY_OFF
        )

        addHoliday("2026-05-01", "Праздник Весны и Труда")
        addHoliday("2026-05-09", "День Победы")

        addHoliday(
            date = "2026-05-11",
            title = "Перенесённый выходной после 9 мая",
            kind = HolidayKinds.TRANSFERRED_DAY_OFF
        )

        addHoliday("2026-06-12", "День России")
        addHoliday("2026-11-04", "День народного единства")

        addHoliday(
            date = "2026-12-31",
            title = "Перенесённый выходной (с 4 января)",
            kind = HolidayKinds.TRANSFERRED_DAY_OFF
        )

        return result
    }
}
