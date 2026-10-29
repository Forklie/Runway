package com.example.runway.data.seed

/**
 * BR-26 default categories. Written as plain maps with the Doc 2 §6.2 field names
 * (name, iconKey, kind, sortOrder, archived), so this does not depend on A's Category model.
 * A: your CategoryDto must read these same field names, and iconKey values must exist in your icon map.
 */
internal data class SeedCategory(
    val id: String,
    val name: String,
    val iconKey: String,
    val kind: String, // COMMITTED or DISCRETIONARY
)

internal val DEFAULT_CATEGORIES = listOf(
    // Committed
    SeedCategory("rent", "Rent", "home", "COMMITTED"),
    SeedCategory("utilities", "Utilities", "bolt", "COMMITTED"),
    SeedCategory("subscriptions", "Subscriptions", "autorenew", "COMMITTED"),
    SeedCategory("gym", "Gym", "fitness_center", "COMMITTED"),
    // Discretionary
    SeedCategory("food-coffee", "Food and Coffee", "restaurant", "DISCRETIONARY"),
    SeedCategory("transport", "Transport", "directions_car", "DISCRETIONARY"),
    SeedCategory("shopping", "Shopping", "shopping_bag", "DISCRETIONARY"),
    SeedCategory("entertainment", "Entertainment", "movie", "DISCRETIONARY"),
    SeedCategory("other", "Other", "more_horiz", "DISCRETIONARY"),
)

internal fun SeedCategory.toFields(sortOrder: Int): Map<String, Any> = mapOf(
    "name" to name,
    "iconKey" to iconKey,
    "kind" to kind,
    "sortOrder" to sortOrder,
    "archived" to false,
)
