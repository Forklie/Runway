package com.example.runway.data.remote

import com.google.android.gms.tasks.Task
import timber.log.Timber

/**
 * Firestore writes are NOT awaited (Doc 2 §5.8): offline, the Task only completes after the server
 * acknowledges, which would freeze the UI. The local cache is already updated, so we only log a
 * late failure (for example a rules rejection after reconnect).
 */
fun Task<*>.logWriteFailure(what: String) {
    addOnFailureListener { Timber.e(it, "Async write failed: %s", what) }
}
