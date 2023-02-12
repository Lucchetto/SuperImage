package com.zhenxiang.superimage.navigation

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.Children
import com.zhenxiang.superimage.home.HomePage
import com.zhenxiang.superimage.settings.SettingsPage

@Composable
fun RootNavigation(rootComponent: RootComponent) = Children(rootComponent.childStack) {
    when (val child = it.instance) {
        is RootComponent.Child.Home -> HomePage(child.component)
        is RootComponent.Child.Settings -> SettingsPage(child.component)
    }
}
