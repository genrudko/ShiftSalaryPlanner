package com.vigilante.shiftsalaryplanner.wear.sync

import android.content.ComponentName
import android.content.Context
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.vigilante.shiftsalaryplanner.wear.complication.ShiftComplicationService
import com.vigilante.shiftsalaryplanner.wear.tile.TodayTileService

/**
 * Tile and complication data come from the durable cache, not the foreground Activity.
 * Invalidate both after a new phone snapshot is committed, including background delivery.
 */
internal object WearSnapshotUpdateNotifier {
    fun onSnapshotChanged(context: Context) {
        runCatching {
            TileService.getUpdater(context).requestUpdate(TodayTileService::class.java)
        }
        runCatching {
            ComplicationDataSourceUpdateRequester.create(
                context,
                ComponentName(context, ShiftComplicationService::class.java)
            ).requestUpdateAll()
        }
    }
}
