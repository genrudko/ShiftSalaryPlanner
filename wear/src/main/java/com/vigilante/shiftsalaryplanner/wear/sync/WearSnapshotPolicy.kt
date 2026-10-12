package com.vigilante.shiftsalaryplanner.wear.sync

/** The wire schema is independent of the phone's domain/Room/Compose models. */
internal object WearSnapshotPolicy {
    const val SCHEMA_VERSION = 1

    /** Offline copies are useful, but they must not masquerade as today's data. */
    fun isStale(snapshot: WearSnapshot, today: String): Boolean =
        snapshot.generatedAt > 0L && snapshot.today != today

    /** A delayed Data Layer item must never replace more recent persisted Wear state. */
    fun shouldReplace(current: WearSnapshot?, incoming: WearSnapshot): Boolean =
        incoming.generatedAt > 0L &&
            (current == null || incoming.generatedAt > current.generatedAt)
}
