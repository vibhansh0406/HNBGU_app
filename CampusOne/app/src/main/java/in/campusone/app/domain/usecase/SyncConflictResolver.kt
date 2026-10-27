package `in`.campusone.app.domain.usecase

/**
 * Deterministic Last-Write-Wins (LWW) conflict resolver used when merging backup JSON records
 * or future cloud sync records.
 */
object SyncConflictResolver {
    fun shouldOverwriteLocal(localUpdatedAtMillis: Long, incomingUpdatedAtMillis: Long): Boolean {
        return incomingUpdatedAtMillis >= localUpdatedAtMillis
    }
}
