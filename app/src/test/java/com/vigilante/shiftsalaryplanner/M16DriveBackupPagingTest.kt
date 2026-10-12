package com.vigilante.shiftsalaryplanner

import com.google.api.services.drive.model.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class M16DriveBackupPagingTest {
    private fun item(name: String, id: String): File =
        File().setName(name).setId(id)

    @Test
    fun findsBackupBeyondFirstPageOfUnrelatedAppData() {
        val visited = mutableListOf<String?>()
        val result = findBackupFileInPages(setOf("shift_salary_planner_backup.zip", "shift_salary_planner_backup.json")) { token ->
            visited += token
            when (token) {
                null -> GoogleDriveBackupPage((1..100).map { item("other-$it", "$it") }, "p2")
                "p2" -> GoogleDriveBackupPage(
                    listOf(item("shift_salary_planner_backup.zip", "back1")),
                    null,
                )
                else -> error("Unexpected page token: $token")
            }
        }
        assertEquals("back1", result?.id)
        assertEquals(listOf(null, "p2"), visited)
    }

    @Test
    fun firstMatchingFileInDescendingModifiedOrderWins() {
        var calls = 0
        val result = findBackupFileInPages(setOf("shift_salary_planner_backup.zip", "shift_salary_planner_backup.json")) {
            calls++
            GoogleDriveBackupPage(listOf(
                item("unrelated", "0"),
                item("shift_salary_planner_backup.json", "latest"),
                item("shift_salary_planner_backup.zip", "older"),
            ), "unnecessary-next")
        }
        assertEquals("latest", result?.id)
        assertEquals(1, calls)
    }

    @Test
    fun missingBackupExhaustsPagesWithoutFabricatingResult() {
        var calls = 0
        val result = findBackupFileInPages(setOf("shift_salary_planner_backup.zip")) { token ->
            calls++
            if (token == null) GoogleDriveBackupPage(listOf(item("other", "a")), "p2")
            else GoogleDriveBackupPage(emptyList(), null)
        }
        assertNull(result)
        assertEquals(2, calls)
    }

    @Test
    fun repeatedPaginationTokenFailsClosed() {
        val result = runCatching {
            findBackupFileInPages(setOf("shift_salary_planner_backup.zip")) {
                GoogleDriveBackupPage(emptyList(), "loop")
            }
        }
        assertTrue(result.isFailure)
    }
}
