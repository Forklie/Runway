package com.example.runway.data.remote

import com.example.runway.core.event.AppEvent
import com.example.runway.core.event.AppEvents
import com.example.runway.core.result.AppError
import com.example.runway.core.result.toAppError
import com.google.android.gms.tasks.Task
import timber.log.Timber

/**
 * Firestore writes are NOT awaited (Doc 2 §5.8): offline, the Task only completes after the server
 * acknowledges, which would freeze the UI. The local cache is already updated, so we only react to a
 * late failure. Being offline never fails a write (it queues), so a failure that is not a network
 * problem means the server refused it (for example a rules rejection after reconnect):
 * log it and tell the person through the app-wide event channel (NFR-03).
 */
fun Task<*>.logWriteFailure(what: String) {
    addOnFailureListener {
        Timber.e(it, "Async write failed: %s", what)
        if (it.toAppError() != AppError.Network) AppEvents.emit(AppEvent.WriteRejected)
    }
}
