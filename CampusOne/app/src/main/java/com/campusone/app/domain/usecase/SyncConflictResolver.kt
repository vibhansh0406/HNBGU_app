package com.campusone.app.domain.usecase

object SyncConflictResolver {
    fun shouldOverwriteLocal(localUpdatedAtMillis: Long, incomingUpdatedAtMillis: Long): Boolean {
        return incomingUpdatedAtMillis >= localUpdatedAtMillis
    }
}

