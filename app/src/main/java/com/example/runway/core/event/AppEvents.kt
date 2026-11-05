package com.example.runway.core.event

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Things that happen away from any screen but the person should hear about (Doc 2 §6.5, §9). */
sealed interface AppEvent {
    /** The server refused a write that was already shown as saved on this phone (for example a rules rejection). */
    data object WriteRejected : AppEvent
}

/**
 * The app-wide event channel. An `object` on purpose: the failure listener in TaskLogging.kt is a plain
 * extension function with no way to receive an injected instance. RunwayShell collects [events] and shows a snackbar.
 * No replay: an event nobody is listening for is dropped, never shown late.
 */
object AppEvents {
    private val _events = MutableSharedFlow<AppEvent>(extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val events: SharedFlow<AppEvent> = _events.asSharedFlow()

    fun emit(event: AppEvent) {
        _events.tryEmit(event)
    }
}
