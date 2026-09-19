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
import com.centralia.app.domain.auth.AuthenticationProvider
import com.centralia.app.ui.components.CentraliaIcons
import com.centralia.app.ui.components.CenteredContent
import com.centralia.app.ui.components.semanticsHidden
import com.centralia.app.ui.theme.CentraliaColors
import com.centralia.app.ui.theme.CentraliaType
import com.centralia.app.ui.theme.ContentWidth
import com.centralia.app.ui.theme.Spacing

/**
 * `AuthenticationScaffold` — a scrolling canvas whose content is capped at 440
 * and whose footer is pushed to the bottom when the content is short.
 *
 * The SwiftUI version reads the viewport from a `GeometryReader` and applies
 * `minHeight: proxy.size.height - 32`. [BoxWithConstraints] plays the same role,
 * and `Arrangement.SpaceBetween` inside a column with that minimum height
 * reproduces the `Spacer(minLength:)` between content and footer.
 */
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

/** `CentraliaAuthHeader` — the 66dp mark above the Joan wordmark. */
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

/** `AuthenticationDivider` — a rule either side of a centred caption. */
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

/** `InlineAuthenticationError` — a red row with a leading warning glyph. */
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

/**
 * The leading glyph on a social sign-in button, showing a spinner while that
 * provider is mid-flight.
 *
 * The SwiftUI build draws `Image(systemName: "apple.logo")` and a "G" filled with
 * Google's four brand colours. Neither vendor's mark is reproduced here: on
 * Android the branded buttons ship with the vendors' own SDKs (Credential Manager
 * for Sign in with Google, and Apple's web flow for Sign in with Apple), and
 * their brand guidelines require using those supplied assets. Until the app is
 * wired to a real identity provider these neutral placeholders keep the layout
 * and metrics of the original, and the buttons still exercise the mock
 * repository's provider path.
 */
@Composable
fun ProviderMark(
    provider: AuthenticationProvider,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        CircularProgressIndicator(
            modifier = modifier.size(18.dp),
            color = CentraliaColors.Ink,
            strokeWidth = 2.dp
        )
        return
    }

    when (provider) {
        AuthenticationProvider.APPLE -> Icon(
            imageVector = CentraliaIcons.ProfileCard,
            contentDescription = null,
            tint = CentraliaColors.Ink,
            modifier = modifier.size(22.dp)
        )

        AuthenticationProvider.GOOGLE -> Text(
            text = "G",
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            color = CentraliaColors.Ink,
            modifier = modifier
        )
    }
}
