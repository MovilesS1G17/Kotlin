package com.centralia.app.feature.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.centralia.app.ui.components.CentraliaIcons
import com.centralia.app.ui.components.CentraliaSheet
import com.centralia.app.ui.components.ContentUnavailable
import com.centralia.app.ui.components.SheetAction
import com.centralia.app.ui.theme.CentraliaColors
import com.centralia.app.ui.theme.CentraliaType
import com.centralia.app.ui.theme.Spacing

/** `enum LegalDocument`. */
enum class LegalDocument(val title: String) {
    TERMS("Terms of Service"),
    PRIVACY("Privacy Policy")
}

/** `struct LegalDocumentSheet`. */
@Composable
fun LegalDocumentSheet(
    document: LegalDocument,
    onDismiss: () -> Unit
) {
    CentraliaSheet(
        title = document.title,
        onDismissRequest = onDismiss,
        trailingAction = SheetAction(title = "Done", onClick = onDismiss)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.large)
                .navigationBarsPadding()
        ) {
            ContentUnavailable(
                title = document.title,
                icon = CentraliaIcons.Document,
                description = "The approved document or destination URL has not been " +
                    "included in the design artifacts yet.",
                minHeight = 240.dp
            )
        }
    }
}

/**
 * `LegalAgreementText` — the caption with two tappable links.
 *
 * SwiftUI builds it from markdown with `centralia://` URLs and intercepts them
 * with an `OpenURLAction`. Compose has no markdown parser in the framework, so
 * the same sentence is assembled as an annotated string whose two ranges carry a
 * tag; tapping a tagged range calls [openDocument] instead of leaving the app.
 */
@Composable
fun LegalAgreementText(
    openDocument: (LegalDocument) -> Unit,
    modifier: Modifier = Modifier
) {
    val termsTitle = "Terms of Service"
    val privacyTitle = "Privacy Policy"

    val linkStyle = SpanStyle(
        color = CentraliaColors.Ink,
        fontWeight = FontWeight.SemiBold,
        textDecoration = TextDecoration.Underline
    )

    val annotatedText = buildAnnotatedString {
        append("By continuing, you agree to the ")

        pushStringAnnotation(tag = LEGAL_TAG, annotation = LegalDocument.TERMS.name)
        withStyle(linkStyle) { append(termsTitle) }
        pop()

        append(" and acknowledge the ")

        pushStringAnnotation(tag = LEGAL_TAG, annotation = LegalDocument.PRIVACY.name)
        withStyle(linkStyle) { append(privacyTitle) }
        pop()

        append(".")
    }

    ClickableText(
        text = annotatedText,
        style = CentraliaType.caption.copy(
            color = CentraliaColors.SecondaryText,
            textAlign = TextAlign.Center
        ),
        modifier = modifier.fillMaxWidth(),
        onClick = { offset ->
            annotatedText
                .getStringAnnotations(tag = LEGAL_TAG, start = offset, end = offset)
                .firstOrNull()
                ?.let { annotation ->
                    LegalDocument.entries
                        .firstOrNull { it.name == annotation.item }
                        ?.let(openDocument)
                }
        }
    )
}

private const val LEGAL_TAG = "legalDocument"
