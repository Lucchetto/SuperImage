package com.zhenxiang.superimage.home

import android.Manifest
import android.app.Activity
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.zhenxiang.superimage.R
import com.zhenxiang.superimage.ui.mono.MonoAlertDialog
import com.zhenxiang.superimage.ui.mono.MonoButton
import com.zhenxiang.superimage.ui.mono.MonoButtonIcon
import com.zhenxiang.superimage.ui.mono.MonoCancelDialogButton
import com.zhenxiang.superimage.ui.theme.MonoTheme
import com.zhenxiang.superimage.ui.utils.RowSpacer
import com.zhenxiang.superimage.utils.IntentUtils
import com.zhenxiang.superimage.utils.writeStoragePermission

@Composable
internal fun UpscaleButton(enabled: Boolean, onClick: () -> Unit) {

    var retryRequestPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) {
            /**
             * If permission has been granted, notify click
             */
            onClick()
        } else {
            retryRequestPermission = true
        }
    }

    val context = LocalContext.current
    MonoButton(
        enabled = enabled,
        onClick = {
            /**
             * Since Android Q is no longer necessary to request any permission to write files in public directories
             */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || context.writeStoragePermission) {
                onClick()
            } else {
                permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        },
    ) {
        MonoButtonIcon(
            painterResource(id = R.drawable.outline_auto_awesome_24),
            contentDescription = null
        )
        Text(stringResource(id = R.string.upscale_label))
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        /**
         * We should monitor if the permission has been granted when the app is brought back to foreground
         */
        val observer = LifecycleEventObserver { _, event ->
            if (retryRequestPermission && event == Lifecycle.Event.ON_RESUME && context.writeStoragePermission) {
                /**
                 * If permission has been granted after resuming from background, notify click
                 */
                retryRequestPermission = false
                onClick()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (retryRequestPermission) {
        PermissionRequestDialog(
            showPermissionRationale = (context as Activity).shouldShowRequestPermissionRationale(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            permissionLauncher = permissionLauncher
        ) {
            retryRequestPermission = false
        }
    }
}

/**
 * @param showPermissionRationale Indicates whether we can retry requesting permission directly
 */
@Composable
private fun PermissionRequestDialog(
    showPermissionRationale: Boolean,
    permissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
    onDismissRequest: () -> Unit
) = MonoAlertDialog(
    onDismissRequest = onDismissRequest,
    title = { Text(stringResource(id = R.string.permission_denied_title)) },
    content = { padding, _ ->
        Text(
            modifier = Modifier.padding(padding),
            text = stringResource(
                id = if (showPermissionRationale) {
                    R.string.storage_permission_denied_desc
                } else {
                    R.string.storage_permission_denied_forever_desc
                }
            )
        )
    },
    buttons = {
        MonoCancelDialogButton(onDismissRequest)
        RowSpacer()
        if (showPermissionRationale) {
            MonoButton(
                onClick = {
                    /**
                     * Dismiss dialog and request permission again
                     */
                    onDismissRequest()
                    permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            ) {
                Text(
                    stringResource(id = R.string.grant_permission_label),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            val context = LocalContext.current
            MonoButton(
                onClick = {
                    /**
                     * Redirect user to app settings page hoping they can figure it out
                     */
                    context.startActivity(IntentUtils.appSettingsIntent(context))
                }
            ) {
                MonoButtonIcon(
                    painter = painterResource(id = R.drawable.ic_gear_24),
                    contentDescription = null
                )
                Text(stringResource(id = R.string.open_settings_label))
            }
        }
    }
)


@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PermissionRequestDialogRationalePreview() = MonoTheme {
    PermissionRequestDialog(
        showPermissionRationale = true,
        permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    ) { }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PermissionRequestDialogPreview() = MonoTheme {
    PermissionRequestDialog(
        showPermissionRationale = false,
        permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    ) { }
}
