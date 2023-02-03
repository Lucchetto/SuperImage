package com.zhenxiang.superimage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.zhenxiang.superimage.navigation.RootNavigation
import com.zhenxiang.superimage.ui.theme.MonoTheme

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        // Handle the splash screen transition.
        installSplashScreen()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        setContent {
            /**
             * Due to this bug https://issuetracker.google.com/issues/227926002 when using
             * splash screen API on some devices with Compose Jetpack Navigation leads to a blank
             * screen. However wrapping the [NavHost] in a [Scaffold] solves this issue, so after
             * digging a bit, I figured out the following line is the workaround for the
             * blank screen bug
             */
            ScaffoldDefaults.contentWindowInsets
            MonoTheme {
                RootNavigation(rememberAnimatedNavController())
            }
        }
    }
}
