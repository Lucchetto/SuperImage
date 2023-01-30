@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.zhenxiang.superimage.ui.form

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.internal.ExposedDropdownMenuPopup
import androidx.compose.material3.tokens.MenuTokens
import androidx.compose.material3.tokens.ShapeKeyTokens
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.zhenxiang.superimage.R
import com.zhenxiang.superimage.ui.mono.drawTopBorder
import com.zhenxiang.superimage.ui.theme.border
import com.zhenxiang.superimage.ui.theme.elevation

@Suppress("ModifierParameter")
@Composable
internal fun DropdownMenuContent(
    expandedStates: MutableTransitionState<Boolean>,
    transformOriginState: MutableState<TransformOrigin>,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    // Menu open/close animation.
    val transition = updateTransition(expandedStates, "DropDownMenu")

    val scale by transition.animateFloat(
        transitionSpec = {
            if (false isTransitioningTo true) {
                // Dismissed to expanded
                tween(
                    durationMillis = InTransitionDuration,
                    easing = LinearOutSlowInEasing
                )
            } else {
                // Expanded to dismissed.
                tween(
                    durationMillis = 1,
                    delayMillis = OutTransitionDuration - 1
                )
            }
        }
    ) {
        if (it) {
            // Menu is expanded.
            1f
        } else {
            // Menu is dismissed.
            0.8f
        }
    }

    val alpha by transition.animateFloat(
        transitionSpec = {
            if (false isTransitioningTo true) {
                // Dismissed to expanded
                tween(durationMillis = 30)
            } else {
                // Expanded to dismissed.
                tween(durationMillis = OutTransitionDuration)
            }
        }
    ) {
        if (it) {
            // Menu is expanded.
            1f
        } else {
            // Menu is dismissed.
            0f
        }
    }
    Surface(
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
            transformOrigin = transformOriginState.value
        },
        shape = ShapeKeyTokens.CornerSmall.toShape(),
        color = MaterialTheme.colorScheme.fromToken(MenuTokens.ContainerColor),
        tonalElevation = MaterialTheme.elevation.container,
        shadowElevation = MaterialTheme.elevation.container,
        border = MaterialTheme.border.regular
    ) {
        Column(
            modifier = modifier
                .width(IntrinsicSize.Max)
                .verticalScroll(rememberScrollState()),
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExposedDropdownMenuBoxScope.MonoExposedDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    // TODO(b/202810604): use DropdownMenu when PopupProperties constructor is stable
    // return DropdownMenu(
    //     expanded = expanded,
    //     onDismissRequest = onDismissRequest,
    //     modifier = modifier.exposedDropdownSize(),
    //     properties = ExposedDropdownMenuDefaults.PopupProperties,
    //     content = content
    // )

    val expandedStates = remember { MutableTransitionState(false) }
    expandedStates.targetState = expanded

    if (expandedStates.currentState || expandedStates.targetState) {
        val transformOriginState = remember { mutableStateOf(TransformOrigin.Center) }
        val density = LocalDensity.current
        val popupPositionProvider = DropdownMenuPositionProvider(
            DpOffset.Zero,
            density
        ) { anchorBounds, menuBounds ->
            transformOriginState.value = calculateTransformOrigin(anchorBounds, menuBounds)
        }

        ExposedDropdownMenuPopup(
            onDismissRequest = onDismissRequest,
            popupPositionProvider = popupPositionProvider
        ) {
            DropdownMenuContent(
                expandedStates = expandedStates,
                transformOriginState = transformOriginState,
                modifier = modifier.exposedDropdownSize(),
                content = content
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> MonoDropDownMenu(
    modifier: Modifier = Modifier,
    value: T,
    label: @Composable () -> Unit,
    options: Array<T>,
    toStringAdapter: @Composable (T) -> String,
    onValueChange: (T) -> Unit
) {

    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        modifier = modifier,
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
        }
    ) {
        MonoTextField(
            modifier = Modifier
                .menuAnchor()
                .onFocusChanged {
                    if (it.isFocused) {
                        expanded = true
                    }
                },
            readOnly = true,
            value = toStringAdapter(value),
            onValueChange = { },
            label = label,
            trailingIcon = {
                MonoDropDownMenuDefaults.ExpandIcon(expanded)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        MonoExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            options.forEachIndexed { index, selectionOption ->
                DropdownMenuItem(
                    modifier = if (index > 0) Modifier.drawTopBorder(MaterialTheme.border.thin) else Modifier,
                    text = {
                        Text(text = toStringAdapter(selectionOption))
                    },
                    onClick = {
                        onValueChange(selectionOption)
                        expanded = false
                    }
                )
            }
        }
    }
}

object MonoDropDownMenuDefaults {

    @Composable
    fun ExpandIcon(expanded: Boolean) {
        Icon(
            painterResource(id = R.drawable.round_keyboard_arrow_down_24),
            null,
            Modifier.rotate(if (expanded) 180f else 0f)
        )
    }
}
