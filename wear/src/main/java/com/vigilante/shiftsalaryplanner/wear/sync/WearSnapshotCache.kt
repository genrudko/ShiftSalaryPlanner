package com.vigilante.shiftsalaryplanner.wear.sync

import android.content.Context
object WearSnapshotCache {
    private const val PREFS = "wear_snapshot_cache"
    private const val KEY_RAW = "raw_snapshot"
    private const val KEY_UPDATED_AT = "updated_at"

    /**
     * The background listener and foreground repository can receive the same DataItem.
     * Serialize their writes, reject stale/unsupported snapshots and notify passive
     * Wear surfaces only after a real, durable change.
     */
    @Synchronized
    fun save(context: Context, raw: String): Boolean {
        val incoming = runCatching { WearSnapshot.fromJson(raw) }.getOrNull() ?: return false
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = prefs.getString(KEY_RAW, null)
            ?.let { runCatching { WearSnapshot.fromJson(it) }.getOrNull() }
        if (!WearSnapshotPolicy.shouldReplace(current, incoming)) return false

        val committed = prefs.edit()
            .putString(KEY_RAW, raw)
            .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
            .commit()
        if (committed) WearSnapshotUpdateNotifier.onSnapshotChanged(appContext)
        return committed
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
