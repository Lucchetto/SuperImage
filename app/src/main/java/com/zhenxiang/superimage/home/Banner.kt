package com.zhenxiang.superimage.home

import android.content.res.Configuration
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.zhenxiang.superimage.R
import com.zhenxiang.superimage.ui.theme.MonoTheme
import com.zhenxiang.superimage.ui.theme.border
import com.zhenxiang.superimage.ui.theme.spacing

@Composable
fun DesktopVersionBanner(modifier: Modifier = Modifier) = Row(
    modifier = modifier
        .border(MaterialTheme.border.regular, MaterialTheme.shapes.medium)
        .padding(MaterialTheme.spacing.level5)
) {
    Icon(
        painterResource(id = R.drawable.ic_laptop_24),
        modifier = Modifier.padding(end = MaterialTheme.spacing.level3),
        contentDescription = null
    )
    Text(
        stringResource(id = R.string.desktop_version_banner_text),
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun DesktopVersionBannerPreview() = MonoTheme {
    Surface {
        DesktopVersionBanner(Modifier.padding(MaterialTheme.spacing.level5))
    }
}
