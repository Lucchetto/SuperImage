package com.zhenxiang.superimage.ui.mono

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.zhenxiang.superimage.ui.theme.MonoTheme
import com.zhenxiang.superimage.ui.theme.border
import com.zhenxiang.superimage.ui.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonoAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(windowInsets)
            .drawBottomBorder(MaterialTheme.border.regular)
            .padding(MaterialTheme.spacing.level5)
    ) {
        ProvideTextStyle(
            value = MaterialTheme.typography.displayMedium,
            content = title
        )
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, showSystemUi = true)
@Preview(showBackground = true, showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MonoAppBarPreview() = MonoTheme {
    Scaffold(
        topBar = {
            MonoAppBar(
                title = { Text("AppBar") }
            )
        }
    ) {}
}
