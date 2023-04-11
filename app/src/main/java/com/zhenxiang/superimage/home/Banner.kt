package com.zhenxiang.superimage.home

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.zhenxiang.superimage.R
import com.zhenxiang.superimage.settings.SettingsPageComponent
import com.zhenxiang.superimage.ui.mono.*
import com.zhenxiang.superimage.ui.theme.MonoTheme
import com.zhenxiang.superimage.ui.theme.spacing

@Composable
fun DesktopVersionBanner(modifier: Modifier = Modifier) = Row(modifier = modifier) {
    Icon(
        painterResource(id = R.drawable.ic_laptop_24),
        modifier = Modifier.padding(end = MaterialTheme.spacing.level3),
        contentDescription = null
    )
    Column {
        Text(
            stringResource(id = R.string.desktop_version_banner_text),
            style = MaterialTheme.typography.bodyMedium
        )
        val context = LocalContext.current
        MonoButton(
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = MaterialTheme.spacing.level4),
            onClick = {
                SettingsPageComponent.openPatreonPage(context)
            }
        ) {
            MonoButtonIcon(
                painter = painterResource(id = R.drawable.ic_patreon_24),
                contentDescription = null
            )
            EllipsisText(text = stringResource(id = R.string.join_patreon_label))
        }
    }
}

@Composable
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun DesktopVersionBannerPreview() = MonoTheme {
    Surface {
        DesktopVersionBanner(Modifier.padding(MaterialTheme.spacing.level5))
    }
}
