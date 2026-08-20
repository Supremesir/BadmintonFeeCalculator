package com.supremesir.badmintonfeecalculator.ui.liquid

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow

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
    keyboardType: KeyboardType = KeyboardType.Decimal
) {
    val focusRequester = remember { FocusRequester() }
    Column(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(28.dp) },
                effects = {
                    vibrancy()
                    blur(4f.dp.toPx())
                    lens(16f.dp.toPx(), 32f.dp.toPx(), depthEffect = true)
                },
                highlight = { Highlight.Default },
                innerShadow = { InnerShadow(radius = 6f.dp, alpha = 0.28f) },
                onDrawSurface = { drawRect(surfaceColor) }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { focusRequester.requestFocus() }
            .padding(horizontal = 8.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label.trimEnd('：', ':', ' '),
            color = mutedColor,
            fontSize = 12.sp,
            maxLines = 2,
            textAlign = TextAlign.Center
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .focusRequester(focusRequester),
            textStyle = TextStyle(
                color = textColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
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
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

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
    defaultValue: String = "5"
) {
    var editing by remember { mutableStateOf(false) }
    var hadFocus by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    fun finishEditing() {
        if (value.isBlank()) onValueChange(defaultValue)
        editing = false
        hadFocus = false
        keyboard?.hide()
        focusManager.clearFocus()
    }

    LaunchedEffect(editing) {
        if (editing) focusRequester.requestFocus()
    }

    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { editing = true }
            .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label.trimEnd('：', ':', ' '),
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            color = mutedColor,
            fontSize = 16.sp,
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
                            vibrancy()
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
                textStyle = TextStyle(
                    color = textColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
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
                onClick = { editing = true }
            )
        }
    }
}
