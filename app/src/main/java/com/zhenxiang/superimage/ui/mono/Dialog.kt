package com.zhenxiang.superimage.ui.mono

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.DialogProperties
import com.zhenxiang.superimage.R
import com.zhenxiang.superimage.ui.theme.MonoTheme
import com.zhenxiang.superimage.ui.theme.border
import com.zhenxiang.superimage.ui.theme.spacing
import com.zhenxiang.superimage.ui.utils.RowSpacer

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun MonoAlertDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(
        dismissOnClickOutside = false,
        usePlatformDefaultWidth = false
    ),
    title: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.(PaddingValues) -> Unit,
    dismissButton: Boolean = true,
    buttons: @Composable RowScope.() -> Unit = { },
) {
    AlertDialog(
        modifier = Modifier.padding(MaterialTheme.spacing.level5),
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        Surface(
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.medium,
            border = MaterialTheme.border.thin
        ) {
            Column {
                title?.let {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBottomBorder(MaterialTheme.border.regular)
                            .padding(MaterialTheme.spacing.level5)
                    ) {
                        ProvideTextStyle(
                            value = MaterialTheme.typography.headlineSmall,
                            content = it
                        )
                    }
                }
                ProvideTextStyle(value = MaterialTheme.typography.labelLarge) {
                    content(
                        PaddingValues(
                            start = MaterialTheme.spacing.level5,
                            end = MaterialTheme.spacing.level5,
                            top = MaterialTheme.spacing.level5
                        )
                    )
                }
                Row(
                    modifier = Modifier
                        .padding(MaterialTheme.spacing.level5)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.level4, Alignment.End)
                ) {
                    if (dismissButton) {
                        MonoCloseDialogButton(onDismissRequest)
                        RowSpacer()
                    }
                    buttons()
                }
            }
        }
    }
}

@Composable
fun MonoCloseDialogButton(onClick: () -> Unit) = MonoButton(onClick = onClick) {
    MonoButtonIcon(
        Icons.Outlined.Close,
        contentDescription = stringResource(id = R.string.close)
    )
    Text(stringResource(id = R.string.close))
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Preview(showBackground = true, showSystemUi = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, showSystemUi = true)
@Composable
private fun MonoAlertDialogPreview() = MonoTheme {
    MonoAlertDialog(
        onDismissRequest = { },
        title = { Text("Dialog title") },
        content = {
            Box(
                modifier = Modifier.padding(it)
            ) {
                Text("Dialog content")
            }
        },
        buttons = {
            MonoButton(onClick = {}) {
                Text("Confirm")
            }
        }
    )
}
