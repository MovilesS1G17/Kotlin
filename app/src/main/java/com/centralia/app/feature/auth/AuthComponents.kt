package com.centralia.app.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centralia.app.R
import com.centralia.app.ui.components.CentraliaIcons
import com.centralia.app.ui.components.CenteredContent
import com.centralia.app.ui.components.semanticsHidden
import com.centralia.app.ui.theme.CentraliaColors
import com.centralia.app.ui.theme.CentraliaType
import com.centralia.app.ui.theme.ContentWidth
import com.centralia.app.ui.theme.Spacing
import com.centralia.app.ui.theme.semibold
import com.centralia.app.ui.components.CentraliaTextButton
import java.util.Locale


@Composable
fun AuthenticationScaffold(
    modifier: Modifier = Modifier,
    footer: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(CentraliaColors.Canvas)
    ) {
        val viewportHeight = maxHeight

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            CenteredContent(maxWidth = ContentWidth.auth) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = viewportHeight - 32.dp)
                        .padding(
                            horizontal = Spacing.large,
                            vertical = Spacing.medium
                        ),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        content()
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Spacing.xLarge)
                    ) {
                        footer()
                    }
                }
            }
        }
    }
}


@Composable
fun CentraliaAuthHeader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xSmall)
    ) {
        Image(
            painter = painterResource(R.drawable.centralia_mark),
            contentDescription = null,
            modifier = Modifier
                .size(66.dp)
                .semanticsHidden()
        )

        Text(
            text = "Centralia",
            style = CentraliaType.brand,
            color = CentraliaColors.Ink
        )
    }
}


@Composable
fun AuthenticationDivider(
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(CentraliaColors.Divider)
        )

        Text(
            text = text,
            style = CentraliaType.footnote,
            color = CentraliaColors.SecondaryText
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(CentraliaColors.Divider)
        )
    }
}


@Composable
fun InlineAuthenticationError(
    message: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        modifier = modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = CentraliaIcons.Warning,
            contentDescription = null,
            tint = CentraliaColors.Error,
            modifier = Modifier.size(18.dp)
        )

        Text(
            text = message,
            style = CentraliaType.footnote,
            color = CentraliaColors.Error
        )
    }
}


@Composable
fun InlineAuthenticationInfo(
    message: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        modifier = modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = CentraliaIcons.Envelope,
            contentDescription = null,
            tint = CentraliaColors.SecondaryText,
            modifier = Modifier.size(18.dp)
        )

        Text(
            text = message,
            style = CentraliaType.footnote,
            color = CentraliaColors.SecondaryText
        )
    }
}


@Composable
fun ResendCodeButton(
    secondsUntilResend: Int,
    isEnabled: Boolean,
    onResend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = if (secondsUntilResend > 0) {
        String.format(Locale.ROOT, "Send a new code in %d:%02d", secondsUntilResend / 60, secondsUntilResend % 60)
    } else {
        "Send a new code"
    }
    CentraliaTextButton(
        title = title,
        style = CentraliaType.subheadline.semibold(),
        isDisabled = !isEnabled || secondsUntilResend > 0,
        modifier = modifier,
        onClick = onResend
    )
}
