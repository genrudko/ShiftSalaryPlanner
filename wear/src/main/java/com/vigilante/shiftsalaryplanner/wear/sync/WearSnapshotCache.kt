package com.vigilante.shiftsalaryplanner.wear.sync

import android.content.Context
import androidx.core.content.edit

object WearSnapshotCache {
    private const val PREFS = "wear_snapshot_cache"
    private const val KEY_RAW = "raw_snapshot"
    private const val KEY_UPDATED_AT = "updated_at"

    fun save(context: Context, raw: String) {
        if (raw.isBlank()) return
        if (runCatching { WearSnapshot.fromJson(raw) }.isFailure) return
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit(commit = true) {
            putString(KEY_RAW, raw)
            putLong(KEY_UPDATED_AT, System.currentTimeMillis())
        }
    }

    fun loadRaw(context: Context): String {
        return context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_RAW, null)
            .orEmpty()
    }

    fun load(context: Context): WearSnapshot {
        return runCatching { WearSnapshot.fromJson(loadRaw(context)) }
            .getOrDefault(WearSnapshot())
    }

    fun updatedAt(context: Context): Long {
        return context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_UPDATED_AT, 0L)
    }
}
