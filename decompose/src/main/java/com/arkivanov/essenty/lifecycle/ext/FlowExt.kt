package com.arkivanov.essenty.lifecycle.ext

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import com.arkivanov.decompose.extensions.compose.LocalLifecycleOwner
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@Composable
fun <T> StateFlow<T>.collectAsStateWithLifecycle(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    context: CoroutineContext = EmptyCoroutineContext
): State<T> = collectAsStateWithLifecycle(
    initialValue = this.value,
    lifecycleOwner = lifecycleOwner,
    minActiveState = minActiveState,
    context = context
)

@Composable
fun <T> Flow<T>.collectAsStateWithLifecycle(
    initialValue: T,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    context: CoroutineContext = EmptyCoroutineContext
): State<T> = lifecycleOwner.lifecycle.let {
    return produceState(initialValue, this, it, minActiveState, context) {
        it.repeatOnLifecycle(minActiveState) {
            if (context == EmptyCoroutineContext) {
                this@collectAsStateWithLifecycle.collect { this@produceState.value = it }
            } else withContext(context) {
                this@collectAsStateWithLifecycle.collect { this@produceState.value = it }
            }
        }
    }
}
