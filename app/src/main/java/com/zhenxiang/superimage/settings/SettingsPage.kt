package com.zhenxiang.superimage.settings

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import com.zhenxiang.superimage.BuildConfig
import com.zhenxiang.superimage.R
import com.zhenxiang.superimage.ui.mono.MonoAppBar
import com.zhenxiang.superimage.ui.mono.drawBottomBorder
import com.zhenxiang.superimage.ui.theme.MonoTheme
import com.zhenxiang.superimage.ui.theme.border
import com.zhenxiang.superimage.ui.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(viewModel: SettingsPageViewModel, navController: NavHostController) = Scaffold(
    topBar = { TopBar(navController) }
) {
    LazyColumn(modifier = Modifier.padding(it)) {
        item {
            val context = LocalContext.current
            ListItem(
                leadingIcon = {
                    Icon(painterResource(id = R.drawable.ic_github_24), contentDescription = null)
                },
                label = { Text(stringResource(id = R.string.project_page_title)) },
                content = { Text(stringResource(id = R.string.project_page_desc)) }
            ) {
                SettingsPageViewModel.openGithubPage(context)
            }
        }
        item {
            ListItem(
                leadingIcon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                label = { Text(stringResource(id = R.string.version_title)) },
                content = { Text(BuildConfig.VERSION_NAME) }
            )
        }
    }
}

@Composable
private fun TopBar(navController: NavHostController) = MonoAppBar(
    title = { Text(stringResource(id = R.string.settings)) },
    leadingIcon = {
        IconButton(
            onClick = { navController.popBackStack() }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_back_24),
                contentDescription = stringResource(id = R.string.back)
            )
        }
    }
)

@Composable
private fun ListItem(
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    label: @Composable () -> Unit,
    content: @Composable () -> Unit,
    onClick: () -> Unit = {}
) = Row(
    modifier = modifier
        .drawBottomBorder(MaterialTheme.border.thin)
        .fillMaxWidth()
        .clickable(onClick = onClick)
        .padding(MaterialTheme.spacing.level5),
    verticalAlignment = Alignment.CenterVertically
) {
    val columnModifier = leadingIcon?.let {
        it()
        Modifier.padding(start = MaterialTheme.spacing.level4)
    } ?: run {
        Modifier
    }
    Column(modifier = columnModifier) {
        ProvideTextStyle(
            value = MaterialTheme.typography.headlineSmall,
            content = label
        )
        content()
    }
}

@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun ListItemPreview() = MonoTheme {
    Surface {
        ListItem(
            leadingIcon = { Icon(Icons.Default.List, contentDescription = null) },
            label = { Text("Item label") },
            content = { Text("Item content") }
        )
    }
}
