package com.vigilante.shiftsalaryplanner.wear.sync

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WearSnapshotContractTest {
    private fun validSnapshot(time: Long = 100L): String = JSONObject()
        .put("schemaVersion", 1)
        .put("generatedAt", time)
        .put("today", "2026-10-12")
        .put("month", "2026-10")
        .put("todaySummary", "День 08:00")
        .put("tomorrowSummary", "Ночь 20:00")
        .put("calendar", org.json.JSONArray())
        .put("payroll", JSONObject().put("periodLabel", "Октябрь 2026"))
        .put("alarms", JSONObject().put("enabled", true))
        .put("notes", org.json.JSONArray())
        .toString()

    @Test fun v1RoundTripIncludesCriticalScreenContracts() {
        val snapshot = WearSnapshot.fromJson(validSnapshot())
        assertEquals(100L, snapshot.generatedAt)
        assertEquals("2026-10-12", snapshot.today)
        assertEquals("День 08:00", snapshot.todaySummary)
        assertEquals("Октябрь 2026", snapshot.payroll.periodLabel)
        assertTrue(snapshot.alarms.enabled)
    }

    @Test fun previousV1WithoutExplicitVersionRemainsReadable() {
        val json = JSONObject(validSnapshot()).apply { remove("schemaVersion") }
        assertEquals(100L, WearSnapshot.fromJson(json.toString()).generatedAt)
    }

    @Test fun unsupportedFutureSchemaCannotEraseCache() {
        val json = JSONObject(validSnapshot()).put("schemaVersion", 2)
        assertTrue(runCatching { WearSnapshot.fromJson(json.toString()) }.isFailure)
    }

    @Test fun invalidOrIncompletePayloadCannotEraseCache() {
        assertTrue(runCatching { WearSnapshot.fromJson("{}") }.isFailure)
        assertTrue(runCatching { WearSnapshot.fromJson("not json") }.isFailure)
        val json = JSONObject(validSnapshot()).apply { remove("calendar") }
        assertTrue(runCatching { WearSnapshot.fromJson(json.toString()) }.isFailure)
        val invalidDate = JSONObject(validSnapshot()).put("today", "bad-date")
        assertTrue(runCatching { WearSnapshot.fromJson(invalidDate.toString()) }.isFailure)
    }

    @Test fun staleYesterdaySnapshotIsRecognizedWithoutRejectingOfflineStorage() {
        val yesterday = WearSnapshot.fromJson(validSnapshot())
        assertFalse(WearSnapshotPolicy.isStale(yesterday, "2026-10-12"))
        assertTrue(WearSnapshotPolicy.isStale(yesterday, "2026-10-13"))
        assertFalse(WearSnapshotPolicy.isStale(WearSnapshot(), "2026-10-13"))
    }

    @Test fun outOfOrderDeliveryCannotRollbackCache() {
        val current = WearSnapshot.fromJson(validSnapshot(200L))
        assertFalse(WearSnapshotPolicy.shouldReplace(current, WearSnapshot.fromJson(validSnapshot(199L))))
        assertFalse(WearSnapshotPolicy.shouldReplace(current, WearSnapshot.fromJson(validSnapshot(200L))))
        assertTrue(WearSnapshotPolicy.shouldReplace(current, WearSnapshot.fromJson(validSnapshot(201L))))
        assertTrue(WearSnapshotPolicy.shouldReplace(null, WearSnapshot.fromJson(validSnapshot(100L))))
    }
}
