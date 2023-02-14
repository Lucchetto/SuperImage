package com.zhenxiang.superimage.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.zhenxiang.superimage.home.HomePageComponent
import com.zhenxiang.superimage.settings.SettingsPageComponent

class RootComponent(componentContext: ComponentContext): ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()

    private val _childStack = childStack(
        source = navigation,
        initialConfiguration = Config.Home,
        handleBackButton = true, // Pop the back stack on back button press
        childFactory = { config, componentContext ->
            when (config) {
                Config.Home -> Child.Home(HomePageComponent(componentContext, navigation))
                Config.Settings -> Child.Settings(SettingsPageComponent(componentContext, navigation))
            }
        },
    )

    val childStack: Value<ChildStack<*, Child>> = _childStack

    sealed interface Child {

        val component: ChildComponent<*>

        class Home(override val component: HomePageComponent) : Child

        class Settings(override val component: SettingsPageComponent) : Child
    }

    sealed interface Config : Parcelable {

        @Parcelize
        object Home : Config

        @Parcelize
        object Settings : Config
    }
}