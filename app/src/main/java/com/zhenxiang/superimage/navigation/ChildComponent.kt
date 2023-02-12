package com.zhenxiang.superimage.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

abstract class ChildComponent<T: ChildComponent.ViewModel>(
    componentContext: ComponentContext,
    val navigation: StackNavigation<RootComponent.Config>
): ComponentContext by componentContext {

    abstract val viewModel: T

    abstract class ViewModel: InstanceKeeper.Instance {

        private val coroutineScopeDelegate = lazy {
            CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        }

        val coroutineScope: CoroutineScope
            get() = coroutineScopeDelegate.value

        override fun onDestroy() {
            if (coroutineScopeDelegate.isInitialized()) {
                coroutineScope.cancel()
            }
        }
    }
}

inline fun <reified T : ChildComponent.ViewModel> ChildComponent<T>.getViewModel(
    factory: () -> T
): T = instanceKeeper.getOrCreate(T::class, factory)
