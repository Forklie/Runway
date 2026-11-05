package com.example.runway.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.runway.ui.theme.Brand
import com.example.runway.ui.theme.Dimens
import com.example.runway.ui.theme.RunwayTheme
import com.example.runway.ui.theme.RunwayType
import com.example.runway.ui.theme.Spacing

/**
 * Outlined pill to choose from; fills when chosen.
 * Choice chip: default (radio behaviour). Filter chip: showCheck = true. Favourite chip: pass meta ("kottu 1,200").
 * onBrand = true on the Quick Add sheet and other azul grounds.
 * Takes plain values so it does not depend on A's Category model.
 */
@Composable
fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    meta: String? = null,
    onBrand: Boolean = false,
    showCheck: Boolean = false,
) {
    val c = RunwayTheme.colors
    val fill = when {
        selected && onBrand -> Brand.Blanco
        selected -> c.action
        else -> Color.Transparent
    }
    val content = when {
        selected && onBrand -> Brand.Azul
        selected -> c.onAction
        onBrand -> c.onBrand
        else -> c.ink
    }
    val border = when {
        selected && onBrand -> Brand.Blanco
        selected -> c.action
        onBrand -> c.onBrand
        else -> c.lineStrong
    }
    val interaction = remember { MutableInteractionSource() }
    val icon = if (showCheck && selected) Icons.Rounded.Check else leadingIcon

    Row(
        modifier = modifier
            .minimumInteractiveComponentSize() // 36dp chip, 48dp hit area
            .pressScale(interaction)
            .height(Dimens.chipHeight)
            .clip(CircleShape)
            .background(fill)
            .border(Dimens.stroke, border, CircleShape)
            .selectable(
                selected = selected,
                interactionSource = interaction,
                indication = ripple(color = content),
                role = if (showCheck) Role.Checkbox else Role.RadioButton,
                onClick = onClick,
            )
            .padding(horizontal = Spacing.s4),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = content)
        }
        Text(label, style = RunwayType.label, color = content, maxLines = 1)
        if (meta != null) {
            Text(
                text = meta,
                style = RunwayType.label.copy(fontWeight = FontWeight.SemiBold),
                color = content.copy(alpha = if (selected) 1f else 0.72f),
                maxLines = 1,
            )
        }
    }
}