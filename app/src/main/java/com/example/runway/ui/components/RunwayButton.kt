package com.example.runway.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.example.runway.R
import com.example.runway.ui.theme.Brand
import com.example.runway.ui.theme.Dimens
import com.example.runway.ui.theme.RunwayColors
import com.example.runway.ui.theme.RunwayTheme
import com.example.runway.ui.theme.RunwayType
import com.example.runway.ui.theme.Spacing

enum class ButtonStyle { Primary, Secondary, Tonal, Tertiary, OnBrand, OnBrandOutline, Danger }
enum class ButtonSize { Large, Medium, Small }

/**
 * Pills you press. One Primary per screen. Labels are verbs in sentence case.
 * Loading keeps the width and swaps the label for three dots (use only when the write leaves the device).
 */
@Composable
fun RunwayButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ButtonStyle = ButtonStyle.Primary,
    size: ButtonSize = ButtonSize.Medium,
    enabled: Boolean = true,
    loading: Boolean = false,
    fillWidth: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
) {
    val colors = RunwayTheme.colors
    val look = buttonLook(style, enabled, colors)
    val interaction = remember { MutableInteractionSource() }
    val savingDescription = stringResource(R.string.state_saving)
    val textStyle = if (size == ButtonSize.Small) RunwayType.label else RunwayType.labelLg
    val height = when (size) {
        ButtonSize.Large -> Dimens.buttonLg
        ButtonSize.Medium -> Dimens.buttonMd
        ButtonSize.Small -> Dimens.buttonSm
    }
    val sidePadding = if (style == ButtonStyle.Tertiary) {
        Spacing.s3
    } else {
        when (size) {
            ButtonSize.Large -> Spacing.s8
            ButtonSize.Medium -> Spacing.s6
            ButtonSize.Small -> Spacing.s4
        }
    }

    Box(
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .minimumInteractiveComponentSize() // hit area stays 48dp
            .pressScale(interaction)
            .height(height)
            .clip(CircleShape)
            .background(look.container)
            .then(
                if (look.border != null) Modifier.border(Dimens.stroke, look.border, CircleShape)
                else Modifier,
            )
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = look.content),
                enabled = enabled && !loading,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = sidePadding)
            .semantics { if (loading) stateDescription = savingDescription },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.alpha(if (loading) 0f else 1f),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(Dimens.iconSm), tint = look.content)
            }
            Text(
                text = text,
                style = textStyle,
                color = look.content,
                maxLines = 1,
                modifier = if (style == ButtonStyle.Tertiary) Modifier.highlightUnderline(colors.highlight) else Modifier,
            )
            if (trailingIcon != null) {
                Icon(trailingIcon, contentDescription = null, modifier = Modifier.size(Dimens.iconSm), tint = look.content)
            }
        }
        if (loading) LoadingDots(look.content)
    }
}

private data class ButtonLook(val container: Color, val content: Color, val border: Color?)

private fun buttonLook(style: ButtonStyle, enabled: Boolean, c: RunwayColors): ButtonLook =
    if (!enabled) {
        when (style) {
            ButtonStyle.Secondary, ButtonStyle.OnBrandOutline, ButtonStyle.Danger ->
                ButtonLook(Color.Transparent, c.inkDisabled, c.line)
            ButtonStyle.Tertiary -> ButtonLook(Color.Transparent, c.inkDisabled, null)
            else -> ButtonLook(c.surfaceSunken, c.inkDisabled, null)
        }
    } else {
        when (style) {
            ButtonStyle.Primary -> ButtonLook(c.action, c.onAction, null)
            ButtonStyle.Secondary -> ButtonLook(Color.Transparent, c.accent, c.accent)
            ButtonStyle.Tonal -> ButtonLook(c.accentSoft, c.accent, null)
            ButtonStyle.Tertiary -> ButtonLook(Color.Transparent, c.accent, null)
            ButtonStyle.OnBrand -> ButtonLook(Brand.Blanco, Brand.Azul, null)
            ButtonStyle.OnBrandOutline -> ButtonLook(Color.Transparent, c.onBrand, c.onBrand)
            ButtonStyle.Danger -> ButtonLook(Color.Transparent, c.danger, c.danger)
        }
    }

/** The yellow 3dp underline of tertiary buttons ("Skip for now"). */
private fun Modifier.highlightUnderline(color: Color): Modifier = drawBehind {
    drawRect(
        color = color,
        topLeft = Offset(0f, size.height + 2.dp.toPx()),
        size = Size(size.width, 3.dp.toPx()),
    )
}

@Composable
private fun LoadingDots(color: Color) {
    val transition = rememberInfiniteTransition(label = "dots")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(450, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 150),
                ),
                label = "dot$index",
            )
            Box(Modifier.size(6.dp).alpha(alpha).background(color, CircleShape))
        }
    }
}