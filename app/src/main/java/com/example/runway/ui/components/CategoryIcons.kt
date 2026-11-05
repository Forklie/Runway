package com.example.runway.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Category iconKey (stored in Firestore, seeded by data/seed/CategorySeed.kt) to a Material icon.
 * An unknown key falls back to a neutral icon instead of crashing. A: add new keys here.
 */
fun categoryIcon(iconKey: String?): ImageVector = when (iconKey) {
    "home" -> Icons.Rounded.Home
    "bolt" -> Icons.Rounded.Bolt
    "autorenew" -> Icons.Rounded.Autorenew
    "fitness_center" -> Icons.Rounded.FitnessCenter
    "restaurant" -> Icons.Rounded.Restaurant
    "directions_car" -> Icons.Rounded.DirectionsCar
    "shopping_bag" -> Icons.Rounded.ShoppingBag
    "movie" -> Icons.Rounded.Movie
    "more_horiz" -> Icons.Rounded.MoreHoriz
    else -> Icons.Rounded.Category
}
