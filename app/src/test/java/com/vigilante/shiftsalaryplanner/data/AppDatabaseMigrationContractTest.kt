package com.vigilante.shiftsalaryplanner.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.sql.Connection
import java.sql.DriverManager

class AppDatabaseMigrationContractTest {
    @Test
    fun migration4To5_preservesTemplateAndDefaultsShiftPayToZero() = withDatabase { db ->
        db.createStatement().use { statement ->
            statement.execute(
                """
                CREATE TABLE shift_templates (
                    code TEXT NOT NULL PRIMARY KEY,
                    title TEXT NOT NULL,
                    totalHours REAL NOT NULL,
                    breakHours REAL NOT NULL,
                    nightHours REAL NOT NULL,
                    colorHex TEXT NOT NULL,
                    isWeekendPaid INTEGER NOT NULL,
                    active INTEGER NOT NULL,
                    sortOrder INTEGER NOT NULL,
                    iconKey TEXT NOT NULL DEFAULT 'TEXT'
                )
                """.trimIndent()
            )
            statement.execute(
                """
                INSERT INTO shift_templates
                (code, title, totalHours, breakHours, nightHours, colorHex, isWeekendPaid, active, sortOrder, iconKey)
                VALUES ('D', 'Day', 12.0, 0.5, 0.0, '#112233', 0, 1, 10, 'SUN')
                """.trimIndent()
            )
            AppDatabaseMigrationSql.FROM_4_TO_5.forEach { sql -> statement.execute(sql) }
        }

        db.createStatement().use { statement ->
            statement.executeQuery("SELECT code, title, shiftPayAmount FROM shift_templates WHERE code='D'").use { rs ->
                assertTrue(rs.next())
                assertEquals("D", rs.getString("code"))
                assertEquals("Day", rs.getString("title"))
                assertEquals(0.0, rs.getDouble("shiftPayAmount"), 0.0)
            }
        }
    }

    @Test
    fun migration5To6_preservesShiftDayAndAddsRecoveredOverrideColumnsAsNull() = withDatabase { db ->
        db.createStatement().use { statement ->
            statement.execute(
                """
                CREATE TABLE shift_days (
                    date TEXT NOT NULL PRIMARY KEY,
                    shiftCode TEXT NOT NULL
                )
                """.trimIndent()
            )
            statement.execute("INSERT INTO shift_days (date, shiftCode) VALUES ('2026-06-07', 'D')")
            AppDatabaseMigrationSql.FROM_5_TO_6.forEach { sql -> statement.execute(sql) }
        }

        val expectedColumns = setOf(
            "overrideStartTime",
            "overrideEndTime",
            "overrideTotalHours",
            "overrideBreakHours",
            "overrideNightHours",
            "overridePaidHours",
            "overrideShiftPayAmount",
            "overrideNote"
        )
        val actualColumns = buildSet {
            db.createStatement().use { statement ->
                statement.executeQuery("PRAGMA table_info(shift_days)").use { rs ->
                    while (rs.next()) add(rs.getString("name"))
                }
            }
        }
        assertTrue(actualColumns.containsAll(expectedColumns))

        db.createStatement().use { statement ->
            statement.executeQuery("SELECT * FROM shift_days WHERE date='2026-06-07'").use { rs ->
                assertTrue(rs.next())
                assertEquals("D", rs.getString("shiftCode"))
                expectedColumns.forEach { column -> assertNull("$column must default to NULL", rs.getObject(column)) }
            }
        }
    }

    private fun withDatabase(block: (Connection) -> Unit) {
        Class.forName("org.sqlite.JDBC")
        DriverManager.getConnection("jdbc:sqlite::memory:").use(block)
    }
}
