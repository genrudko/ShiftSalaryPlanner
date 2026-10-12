package com.vigilante.shiftsalaryplanner.wear.sync

import android.content.Context

/** Privacy choice applies to both the Activity and passive Tile surfaces across restarts. */
object WearPrivacySettings {
    private const val PREFS = "wear_display_preferences"
    private const val KEY_HIDE_MONEY = "hide_money"

    fun hideMoney(context: Context): Boolean =
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_HIDE_MONEY, false)

    fun setHideMoney(context: Context, hidden: Boolean) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_HIDE_MONEY, hidden)
            .apply()
        WearSnapshotUpdateNotifier.onSnapshotChanged(context.applicationContext)
    }
}
