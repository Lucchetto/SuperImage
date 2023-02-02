package com.zhenxiang.superimage.ui.mono

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.zhenxiang.superimage.R
import com.zhenxiang.superimage.ui.theme.MonoTheme
import com.zhenxiang.superimage.ui.theme.border
import com.zhenxiang.superimage.ui.theme.spacing
import com.zhenxiang.superimage.ui.utils.RowSpacer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonoAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    trailingIcons: @Composable RowScope.() -> Unit = { }
) = Row(
    modifier = modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.primaryContainer)
        .windowInsetsPadding(windowInsets)
        .drawBottomBorder(MaterialTheme.border.regular)
        .padding(MaterialTheme.spacing.level5),
    verticalAlignment = Alignment.CenterVertically
    ) {
    ProvideTextStyle(
        value = MaterialTheme.typography.displayMedium,
        content = title
    )
    RowSpacer()
    trailingIcons()
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
            ) {
                IconButton(
                    onClick = {}
                ) {
                    Icon(Icons.Default.Build, contentDescription = null)
                }
            }
        }
    ) {}
}
