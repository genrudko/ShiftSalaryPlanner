package com.vigilante.shiftsalaryplanner.data

internal object AppDatabaseMigrationSql {
    val FROM_4_TO_5 = listOf(
        """
        ALTER TABLE shift_templates
        ADD COLUMN shiftPayAmount REAL NOT NULL DEFAULT 0
        """.trimIndent()
    )

    val FROM_5_TO_6 = listOf(
        "ALTER TABLE shift_days ADD COLUMN overrideStartTime TEXT",
        "ALTER TABLE shift_days ADD COLUMN overrideEndTime TEXT",
        "ALTER TABLE shift_days ADD COLUMN overrideTotalHours REAL",
        "ALTER TABLE shift_days ADD COLUMN overrideBreakHours REAL",
        "ALTER TABLE shift_days ADD COLUMN overrideNightHours REAL",
        "ALTER TABLE shift_days ADD COLUMN overridePaidHours REAL",
        "ALTER TABLE shift_days ADD COLUMN overrideShiftPayAmount REAL",
        "ALTER TABLE shift_days ADD COLUMN overrideNote TEXT"
    )
}
