package com.centralia.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.centralia.app.ui.theme.ContentWidth

/** `.accessibilityLabel(…)` on a view that has no text of its own. */
fun Modifier.semanticsLabel(label: String?): Modifier =
    if (label == null) {
        this
    } else {
        this.semantics { contentDescription = label }
    }

/** `.accessibilityHidden(true)` — removes decorative content from the tree. */
fun Modifier.semanticsHidden(): Modifier = this.clearAndSetSemantics { }

/**
 * `.frame(maxWidth: 700).frame(maxWidth: .infinity)` — the SwiftUI screens cap
 * their content and centre it so a tablet or unfolded device does not stretch
 * the layout across the full width.
 */
@Composable
fun CenteredContent(
    modifier: Modifier = Modifier,
    maxWidth: Dp = ContentWidth.standard,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(modifier = Modifier.widthIn(max = maxWidth).fillMaxWidth()) {
            content()
        }
    }
}
