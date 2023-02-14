package com.arkivanov.essenty.lifecycle.ext

import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.Lifecycle.State
import com.arkivanov.essenty.lifecycle.LifecycleOwner

enum class LifecycleEvent {
    /**
     * Constant for onCreate event of the [LifecycleOwner].
     */
    ON_CREATE,

    /**
     * Constant for onStart event of the [LifecycleOwner].
     */
    ON_START,

    /**
     * Constant for onResume event of the [LifecycleOwner].
     */
    ON_RESUME,

    /**
     * Constant for onPause event of the [LifecycleOwner].
     */
    ON_PAUSE,

    /**
     * Constant for onStop event of the [LifecycleOwner].
     */
    ON_STOP,

    /**
     * Constant for onDestroy event of the [LifecycleOwner].
     */
    ON_DESTROY;

    companion object {
        /**
         * Returns the [LifecycleEvent] that will be reported by a [Lifecycle]
         * leaving the specified [State] to a lower state, or `null`
         * if there is no valid event that can move down from the given state.
         *
         * @param state the higher state that the returned event will transition down from
         * @return the event moving down the lifecycle phases from state
         */
        @JvmStatic
        fun downFrom(state: State): LifecycleEvent? {
            return when (state) {
                State.CREATED -> ON_DESTROY
                State.STARTED -> ON_STOP
                State.RESUMED -> ON_PAUSE
                else -> null
            }
        }

        /**
         * Returns the [LifecycleEvent] that will be reported by a [Lifecycle]
         * entering the specified [State] from a lower state, or `null`
         * if there is no valid event that can move up to the given state.
         *
         * @param state the higher state that the returned event will transition up to
         * @return the event moving up the lifecycle phases to state
         */
        @JvmStatic
        fun upTo(state: State): LifecycleEvent? {
            return when (state) {
                State.CREATED -> ON_CREATE
                State.STARTED -> ON_START
                State.RESUMED -> ON_RESUME
                else -> null
            }
        }
    }
}