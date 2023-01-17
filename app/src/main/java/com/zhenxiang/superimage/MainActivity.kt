package com.zhenxiang.superimage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhenxiang.superimage.home.HomePage
import com.zhenxiang.superimage.ui.theme.SuperImageTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SuperImageTheme {
                HomePage(viewModel())
            }
        }
    }
}
