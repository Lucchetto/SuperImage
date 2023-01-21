package com.zhenxiang.superimage.ui.mono

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.zhenxiang.superimage.R
import com.zhenxiang.superimage.ui.theme.border
import com.zhenxiang.superimage.ui.theme.spacing

@Composable
fun MonoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        colors = colors,
        border = if (enabled) {
            MaterialTheme.border.regular
        } else {
            MaterialTheme.border.RegularWithAlpha(MonoButtonDefaults.DisableBorderColourOpacity)
        },
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
fun MonoButtonIcon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) = Icon(
    painter,
    contentDescription = contentDescription,
    modifier = modifier
        .padding(end = MaterialTheme.spacing.level3)
        .size(MonoButtonDefaults.IconSize)
)

object MonoButtonDefaults {

    val IconSize = 18.dp

    /**
     * Matches [OutlinedButtonTokens.DisabledLabelTextOpacity]'s value
     * Fuck Google for making every constant internal
     */
    val DisableBorderColourOpacity = 0.38f
}
