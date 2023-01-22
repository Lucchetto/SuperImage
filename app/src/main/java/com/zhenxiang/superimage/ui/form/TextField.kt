@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.zhenxiang.superimage.ui.form

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import com.zhenxiang.superimage.ui.theme.Border.thickness
import com.zhenxiang.superimage.ui.theme.border

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonoTextField(value: String,
                  onValueChange: (String) -> Unit,
                  modifier: Modifier = Modifier,
                  enabled: Boolean = true,
                  readOnly: Boolean = false,
                  textStyle: TextStyle = LocalTextStyle.current,
                  label: @Composable (() -> Unit)? = null,
                  placeholder: @Composable (() -> Unit)? = null,
                  leadingIcon: @Composable (() -> Unit)? = null,
                  trailingIcon: @Composable (() -> Unit)? = null,
                  supportingText: @Composable (() -> Unit)? = null,
                  isError: Boolean = false,
                  visualTransformation: VisualTransformation = VisualTransformation.None,
                  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
                  keyboardActions: KeyboardActions = KeyboardActions.Default,
                  singleLine: Boolean = false,
                  maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
                  minLines: Int = 1,
                  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
                  shape: Shape = MaterialTheme.shapes.small,
                  colors: TextFieldColors = TextFieldDefaults.outlinedTextFieldColors()
) {
    // If color is not provided via the text style, use content color as a default
    val textColor = textStyle.color.takeOrElse {
        colors.textColor(enabled).value
    }
    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))

    CompositionLocalProvider(LocalTextSelectionColors provides colors.selectionColors) {
        @OptIn(ExperimentalMaterial3Api::class)
        BasicTextField(
            value = value,
            modifier = if (label != null) {
                modifier
                    // Merge semantics at the beginning of the modifier chain to ensure padding is
                    // considered part of the text field.
                    .semantics(mergeDescendants = true) {}
                    .padding(top = OutlinedTextFieldTopPadding)
            } else {
                modifier
            }
                .defaultMinSize(
                    minWidth = TextFieldDefaults.MinWidth,
                    minHeight = TextFieldDefaults.MinHeight
                ),
            onValueChange = onValueChange,
            enabled = enabled,
            readOnly = readOnly,
            textStyle = mergedTextStyle,
            cursorBrush = SolidColor(colors.cursorColor(isError).value),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interactionSource,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            decorationBox = @Composable { innerTextField ->
                TextFieldDefaults.OutlinedTextFieldDecorationBox(
                    value = value,
                    visualTransformation = visualTransformation,
                    innerTextField = innerTextField,
                    placeholder = placeholder,
                    label = label,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon,
                    supportingText = supportingText,
                    singleLine = singleLine,
                    enabled = enabled,
                    isError = isError,
                    interactionSource = interactionSource,
                    colors = colors,
                    container = {
                        TextFieldDefaults.OutlinedBorderContainerBox(
                            enabled,
                            isError,
                            interactionSource,
                            colors,
                            shape,
                            MaterialTheme.border.thickness.regular,
                            MaterialTheme.border.thickness.regular
                        )
                    }
                )
            }
        )
    }
}
