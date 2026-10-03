package com.centralia.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Line-for-line port of the SwiftUI `FlowLayout: Layout` in
 * `SaveVideoComponents.swift`: children are laid out left to right at their
 * intrinsic size and wrap to a new row once the next one would cross the
 * trailing edge, with [spacing] between items and between rows.
 *
 * Compose ships `FlowRow`, but it was still an experimental API in the Compose
 * version this project targets, so the original algorithm is reproduced with a
 * plain [Layout] to keep the tag rows behaving exactly as they do on iOS.
 */
@Composable
fun FlowLayout(
    modifier: Modifier = Modifier,
    spacing: Dp = 8.dp,
    content: @Composable () -> Unit
) {
    Layout(modifier = modifier, content = content) { measurables, constraints ->
        val spacingPx = spacing.roundToPx()
        val maxWidth = constraints.maxWidth

        // Measure every child against its own intrinsic size, as
        // `subview.sizeThatFits(.unspecified)` does on iOS.
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }

        var x = 0
        var y = 0
        var rowHeight = 0
        var measuredWidth = 0
        val positions = ArrayList<Pair<Int, Int>>(placeables.size)

        placeables.forEach { placeable ->
            if (x > 0 && x + placeable.width > maxWidth) {
                x = 0
                y += rowHeight + spacingPx
                rowHeight = 0
            }
            positions += x to y
            measuredWidth = maxOf(measuredWidth, x + placeable.width)
            x += placeable.width + spacingPx
            rowHeight = maxOf(rowHeight, placeable.height)
        }

        val layoutWidth = if (constraints.hasBoundedWidth) maxWidth else measuredWidth
        val layoutHeight = (y + rowHeight).coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(layoutWidth.coerceAtLeast(constraints.minWidth), layoutHeight) {
            placeables.forEachIndexed { index, placeable ->
                val (itemX, itemY) = positions[index]
                placeable.placeRelative(itemX, itemY)
            }
        }
    }
}
