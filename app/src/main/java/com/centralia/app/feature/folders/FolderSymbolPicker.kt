package com.centralia.app.feature.folders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.centralia.app.domain.library.FolderSymbol
import com.centralia.app.ui.components.centraliaPressable
import com.centralia.app.ui.components.folderSymbolIcon
import com.centralia.app.ui.theme.CentraliaColors
import com.centralia.app.ui.theme.Radius
import com.centralia.app.ui.theme.Spacing

/**
 * `struct FolderSymbolPicker` — the twelve folder icons in a four-column grid.
 *
 * A `LazyVGrid` is unnecessary for a fixed dozen items, and a lazy grid nested in
 * a scrolling sheet needs a bounded height, so the rows are laid out directly.
 */
@Composable
fun FolderSymbolPicker(
    selection: FolderSymbol,
    onSelect: (FolderSymbol) -> Unit,
    modifier: Modifier = Modifier
) {
    val columns = 4

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.small)
    ) {
        FolderSymbol.entries.chunked(columns).forEach { rowSymbols ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                rowSymbols.forEach { symbol ->
                    val isSelected = selection == symbol

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .centraliaPressable(
                                onClickLabel = symbol.accessibilityName
                            ) { onSelect(symbol) }
                            .background(
                                color = if (isSelected) {
                                    CentraliaColors.Ink
                                } else {
                                    CentraliaColors.SoftSurface
                                },
                                shape = RoundedCornerShape(Radius.control)
                            )
                            .then(
                                if (isSelected) {
                                    Modifier
                                } else {
                                    Modifier.border(
                                        1.dp,
                                        CentraliaColors.Divider,
                                        RoundedCornerShape(Radius.control)
                                    )
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = folderSymbolIcon(symbol),
                            contentDescription = null,
                            tint = if (isSelected) {
                                CentraliaColors.Canvas
                            } else {
                                CentraliaColors.Ink
                            }
                        )
                    }
                }

                // Pads a short final row so the icons keep their column width.
                repeat(columns - rowSymbols.size) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
