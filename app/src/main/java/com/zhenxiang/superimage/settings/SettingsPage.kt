package com.zhenxiang.superimage.settings

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.zhenxiang.superimage.BuildConfig
import com.zhenxiang.superimage.R
import com.zhenxiang.superimage.common.Identifiable
import com.zhenxiang.superimage.navigation.RootComponent
import com.zhenxiang.superimage.ui.daynight.DayNightMode
import com.zhenxiang.superimage.ui.mono.*
import com.zhenxiang.superimage.ui.theme.MonoTheme
import com.zhenxiang.superimage.ui.theme.border
import com.zhenxiang.superimage.ui.theme.spacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(component: SettingsPageComponent) = Scaffold(
    topBar = { TopBar(component.navigation) },
    contentWindowInsets = WindowInsets.safeDrawing
) { padding ->
    LazyColumn(modifier = Modifier.padding(padding)) {
        item {
            SelectionPreferenceItem(
                state = component.viewModel.themeMode,
                mapToString = { stringResource(id = (DayNightMode.fromId(it) ?: DayNightMode.AUTO).stringRes) },
                values = DayNightMode.VALUES.toList(),
                valueToString = { stringResource(id = it.stringRes) },
                leadingIcon = {
                    if ((DayNightMode.fromId(it) ?: DayNightMode.AUTO).lightMode) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_sun_24),
                            contentDescription = stringResource(id = R.string.light_label)
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_moon_stars_24),
                            contentDescription = stringResource(id = R.string.dark_label)
                        )
                    }
                },
                label = { Text(stringResource(id = R.string.theme_title)) }
            )
        }
        item {
            val context = LocalContext.current
            ListItem(
                leadingIcon = {
                    Icon(painterResource(id = R.drawable.ic_telegram_24), contentDescription = null)
                },
                label = { Text(stringResource(id = R.string.telegram_group_title)) },
                content = { Text(stringResource(id = R.string.telegram_group_desc)) }
            ) {
                SettingsPageComponent.openTelegramGroup(context)
            }
        }
        item {
            val context = LocalContext.current
            ListItem(
                leadingIcon = {
                    Icon(painterResource(id = R.drawable.ic_github_24), contentDescription = null)
                },
                label = { Text(stringResource(id = R.string.project_page_title)) },
                content = { Text(stringResource(id = R.string.project_page_desc)) }
            ) {
                SettingsPageComponent.openGithubPage(context)
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
private fun TopBar(navigation: StackNavigation<RootComponent.Config>) = MonoAppBar(
    title = { Text(stringResource(id = R.string.settings)) },
    leadingIcon = {
        IconButton(
            onClick = { navigation.pop() }
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
private fun <T: Identifiable<Int>> SelectionPreferenceDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    state: IntPreferenceState,
    values: List<T>,
    valueToString: @Composable (T) -> String,
) {
    val coroutineScope = rememberCoroutineScope()
    val selectedValue by state.state

    MonoAlertDialog(
        onDismissRequest = onDismissRequest,
        title = title,
        content = { padding, _ ->
            LazyColumn(
                modifier = Modifier.padding(padding)
            ) {
                items(values) {
                    MonoRadioButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(),
                        label = { Text(valueToString(it)) },
                        selected = selectedValue == it.id
                    ) {
                        coroutineScope.launch { state.setValue(it.id) }
                        onDismissRequest()
                    }
                }
            }
        },
        buttons = {
            MonoCancelDialogButton(onClick = onDismissRequest)
        }
    )
}

@Composable
private fun <T: Identifiable<Int>> SelectionPreferenceItem(
    state: IntPreferenceState,
    mapToString: @Composable (Int) -> String,
    values: List<T>,
    valueToString: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable (Int) -> Unit)? = null,
    label: @Composable () -> Unit,
) {

    val value by state.state
    var dialogOpen by remember { mutableStateOf(false) }

    ListItem(
        modifier = modifier,
        leadingIcon = { leadingIcon?.invoke(value) },
        label = label,
        content = {
            Text(mapToString(value))
        }
    ) {
        dialogOpen = true
    }

    if (dialogOpen) {
        SelectionPreferenceDialog(
            onDismissRequest = { dialogOpen = false },
            title = label,
            state = state,
            values = values,
            valueToString = valueToString
        )
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
