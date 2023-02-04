package com.zhenxiang.superimage.ui.mono

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.zhenxiang.superimage.ui.theme.MonoTheme
import com.zhenxiang.superimage.ui.theme.spacing

@Composable
fun MonoRadioButton(
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit
) = Row(
    modifier = modifier
        .clickable(onClick = onClick)
        .padding(start = MaterialTheme.spacing.level4),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
) {
    label()
    RadioButton(selected = selected, onClick = onClick)
}

@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun MonoRadioButtonPreview() = MonoTheme {
    Surface {
        MonoRadioButton(
            label = { Text("Radio button") },
            selected = true,
        ) {}
    }
}
