package com.example.runway.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.runway.R
import com.example.runway.ui.theme.Brand
import com.example.runway.ui.theme.Dimens
import com.example.runway.ui.theme.RunwayTheme

/** 64dp circle in action colour with a 32dp plus. Opens the expense keypad (A's sheet). */
@Composable
fun QuickAddButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RunwayTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val description = stringResource(R.string.quick_add_description)

    Box(
        modifier = modifier
            .pressScale(interaction, pressedScale = 0.94f)
            .size(Dimens.quickAdd)
            .shadow(6.dp, CircleShape, ambientColor = Brand.Midnight, spotColor = Brand.Midnight)
            .clip(CircleShape)
            .background(colors.action)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = colors.onAction),
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = colors.onAction,
        )
    }
}