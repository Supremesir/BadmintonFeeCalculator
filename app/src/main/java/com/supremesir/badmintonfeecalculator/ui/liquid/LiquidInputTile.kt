package com.supremesir.badmintonfeecalculator.ui.liquid

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.supremesir.badmintonfeecalculator.ui.theme.LiquidType

@Composable
fun LiquidInputTile(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    backdrop: Backdrop,
    textColor: Color,
    mutedColor: Color,
    surfaceColor: Color,
    accent: Color,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Decimal,
    readOnly: Boolean = false,
    onReadOnlyClick: (() -> Unit)? = null,
    onAccessoryClick: (() -> Unit)? = null,
    accessoryActive: Boolean = false
) {
    val focusRequester = remember { FocusRequester() }
    val isLightTheme = !isSystemInDarkTheme()
    Box(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(28.dp) },
                effects = {
                    liquidColorControls(isLightTheme)
                    blur(4f.dp.toPx())
                    lens(16f.dp.toPx(), 32f.dp.toPx(), depthEffect = true)
                },
                highlight = { Highlight.Default },
                innerShadow = { InnerShadow(radius = 6f.dp, alpha = 0.28f) },
                onDrawSurface = { drawRect(surfaceColor) }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = LocalIndication.current
                ) {
                    if (readOnly) onReadOnlyClick?.invoke() else focusRequester.requestFocus()
                }
                .padding(horizontal = 8.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label.trimEnd('：', ':', ' '),
                    modifier = Modifier.padding(horizontal = if (onAccessoryClick != null) 30.dp else 0.dp),
                    color = mutedColor,
                    style = LiquidType.caption,
                    maxLines = 2,
                    textAlign = TextAlign.Center
                )
                if (onAccessoryClick != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(width = 28.dp, height = 22.dp)
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { RoundedCornerShape(50) },
                                effects = {
                                    liquidColorControls(isLightTheme)
                                    blur(2f.dp.toPx())
                                    lens(6f.dp.toPx(), 12f.dp.toPx())
                                },
                                onDrawSurface = {
                                    drawRect(
                                        if (accessoryActive) {
                                            accent.copy(alpha = 0.42f)
                                        } else {
                                            surfaceColor
                                        }
                                    )
                                }
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = LocalIndication.current,
                                role = Role.Button,
                                onClick = onAccessoryClick
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (accessoryActive) "桶" else "+",
                            color = if (accessoryActive) Color.White else textColor,
                            style = LiquidType.caption
                        )
                    }
                }
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .focusRequester(focusRequester),
                enabled = !readOnly,
                readOnly = readOnly,
                textStyle = LiquidType.number.copy(
                    color = textColor,
                    textAlign = TextAlign.Center
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                cursorBrush = SolidColor(accent),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.Center) {
                        if (value.isEmpty()) {
                            Text(
                                text = "0",
                                color = mutedColor.copy(alpha = 0.45f),
                                style = LiquidType.number,
                                textAlign = TextAlign.Center
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiquidTapToEditRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    backdrop: Backdrop,
    textColor: Color,
    mutedColor: Color,
    surfaceColor: Color,
    accent: Color,
    modifier: Modifier = Modifier,
    defaultValue: String = "5",
    editing: Boolean,
    onEditingChange: (Boolean) -> Unit,
    onBoundsInWindow: (Rect) -> Unit = {}
) {
    var hadFocus by remember { mutableStateOf(false) }
    var imeWasVisible by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val imeVisible = WindowInsets.isImeVisible
    val isLightTheme = !isSystemInDarkTheme()

    fun finishEditing() {
        if (!editing) return
        if (value.isBlank()) onValueChange(defaultValue)
        onEditingChange(false)
        hadFocus = false
        imeWasVisible = false
        keyboard?.hide()
        focusManager.clearFocus(force = true)
    }

    BackHandler(enabled = editing) { finishEditing() }

    LaunchedEffect(editing) {
        if (editing) {
            focusRequester.requestFocus()
        } else {
            imeWasVisible = false
        }
    }

    LaunchedEffect(editing, imeVisible) {
        if (!editing) {
            imeWasVisible = false
            return@LaunchedEffect
        }
        if (imeVisible) {
            imeWasVisible = true
        } else if (imeWasVisible) {
            finishEditing()
        }
    }

    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .onGloballyPositioned { onBoundsInWindow(it.boundsInWindow()) }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current
            ) { onEditingChange(true) }
            .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label.trimEnd('：', ':', ' '),
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            color = mutedColor,
            style = LiquidType.body,
            maxLines = 1
        )
        if (editing) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .widthIn(min = 72.dp)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedCornerShape(12.dp) },
                        effects = {
                            liquidColorControls(isLightTheme)
                            blur(4f.dp.toPx())
                            lens(8f.dp.toPx(), 16f.dp.toPx())
                        },
                        onDrawSurface = { drawRect(surfaceColor) }
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            hadFocus = true
                        } else if (hadFocus) {
                            finishEditing()
                        }
                    },
                textStyle = LiquidType.chip.copy(
                    color = textColor,
                    textAlign = TextAlign.Center
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { finishEditing() }),
                cursorBrush = SolidColor(accent)
            )
        } else {
            LiquidGlassChip(
                text = value.ifBlank { defaultValue },
                backdrop = backdrop,
                textColor = textColor,
                surfaceColor = surfaceColor,
                onClick = { onEditingChange(true) }
            )
        }
    }
}
