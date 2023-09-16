package com.zhenxiang.superimage.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.zhenxiang.superimage.datastore.SETTINGS_DATA_STORE_QUALIFIER
import com.zhenxiang.superimage.navigation.ChildComponent
import com.zhenxiang.superimage.navigation.RootComponent
import com.zhenxiang.superimage.navigation.getViewModel
import com.zhenxiang.superimage.ui.daynight.DayNightManager
import com.zhenxiang.superimage.ui.daynight.DayNightMode
import com.zhenxiang.superimage.utils.IntentUtils
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SettingsPageComponent(
    componentContext: ComponentContext,
    navigation: StackNavigation<RootComponent.Config>
): ChildComponent<SettingsPageComponent.ViewModel>(componentContext, navigation) {

    override val viewModel = getViewModel(::ViewModel)
    class ViewModel: ChildComponent.ViewModel(), KoinComponent {

        private val dataStore by inject<DataStore<Preferences>>(SETTINGS_DATA_STORE_QUALIFIER)

        val themeMode = IntPreferenceState(dataStore, DayNightManager.THEME_MODE_KEY, DayNightMode.AUTO.id)
    }

    companion object {

        const val GITHUB_PAGE_URL = "github.com/Lucchetto/SuperImage"

        fun openGithubPage(context: Context) {
            context.startActivity(IntentUtils.openStringUriIntent("https://$GITHUB_PAGE_URL"))
        }

        fun openTelegramGroup(context: Context) {
            context.startActivity(IntentUtils.openStringUriIntent("https://t.me/super_image"))
        }

        fun openPatreonPage(context: Context) {
            context.startActivity(IntentUtils.openStringUriIntent("https://patreon.com/SuperImage"))
        }
    }
}