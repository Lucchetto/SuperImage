package com.arkivanov.essenty.lifecycle.ext

import com.arkivanov.essenty.lifecycle.Lifecycle
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

inline fun observeEvents(crossinline stateObserver: (LifecycleEvent) -> Unit) = object : Lifecycle.Callbacks {

    override fun onCreate() = stateObserver(LifecycleEvent.ON_CREATE)

    override fun onStart() = stateObserver(LifecycleEvent.ON_START)

    override fun onResume() = stateObserver(LifecycleEvent.ON_RESUME)

    override fun onPause() = stateObserver(LifecycleEvent.ON_PAUSE)

    override fun onStop() = stateObserver(LifecycleEvent.ON_STOP)

    override fun onDestroy() = stateObserver(LifecycleEvent.ON_DESTROY)
}

suspend fun Lifecycle.repeatOnLifecycle(
    targetState: Lifecycle.State,
    block: suspend CoroutineScope.() -> Unit
) {
    require(targetState !== Lifecycle.State.INITIALIZED) {
        "repeatOnLifecycle cannot start work with the INITIALIZED lifecycle state."
    }

    if (state === Lifecycle.State.DESTROYED) {
        return
    }

    // This scope is required to preserve context before we move to Dispatchers.Main
    coroutineScope {
        withContext(Dispatchers.Main.immediate) {
            // Check the current state of the lifecycle as the previous check is not guaranteed
            // to be done on the main thread.
            if (state === Lifecycle.State.DESTROYED) return@withContext

            // Instance of the running repeating coroutine
            var launchedJob: Job? = null

            // Registered observer
            var observer: Lifecycle.Callbacks? = null
            try {
                // Suspend the coroutine until the lifecycle is destroyed or
                // the coroutine is cancelled
                suspendCancellableCoroutine<Unit> { cont ->
                    // Lifecycle observers that executes `block` when the lifecycle reaches certain state, and
                    // cancels when it falls below that state.
                    val startWorkEvent = LifecycleEvent.upTo(targetState)
                    val cancelWorkEvent = LifecycleEvent.downFrom(targetState)
                    val mutex = Mutex()
                    observer = observeEvents { event ->
                        if (event == startWorkEvent) {
                            // Launch the repeating work preserving the calling context
                            launchedJob = this@coroutineScope.launch {
                                // Mutex makes invocations run serially,
                                // coroutineScope ensures all child coroutines finish
                                mutex.withLock {
                                    coroutineScope {
                                        block()
                                    }
                                }
                            }
                            return@observeEvents
                        }
                        if (event == cancelWorkEvent) {
                            launchedJob?.cancel()
                            launchedJob = null
                        }
                        if (event == LifecycleEvent.ON_DESTROY) {
                            cont.resume(Unit)
                        }
                    }.also { subscribe(it) }
                }
            } finally {
                launchedJob?.cancel()
                observer?.let {
                    this@repeatOnLifecycle.unsubscribe(it)
                }
            }
        }
    }
}
