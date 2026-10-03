package com.centralia.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.centralia.app.ui.theme.CentraliaColors
import com.centralia.app.ui.theme.CentraliaType
import com.centralia.app.ui.theme.Radius
import com.centralia.app.ui.theme.Spacing
import com.centralia.app.ui.theme.semibold

/**
 * The iOS fields are plain `TextField`s wrapped in a rounded rectangle the app
 * draws itself, so Material's `OutlinedTextField` — with its own container,
 * floating label and indicator — would not match. Every field below is a
 * [BasicTextField] inside the same hand-drawn decoration as the original.
 */

/**
 * `AuthenticationTextField`: a semibold label, a 52dp-tall control with a
 * 14dp-radius surface and hairline border that turns red on error, an optional
 * reveal toggle for secure entry, and a caption-sized error line.
 */
@Composable
fun CentraliaLabeledTextField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    isSecure: Boolean = false,
    onSubmit: () -> Unit = {}
) {
    var revealsSecureText by remember { mutableStateOf(false) }
    val hidesText = isSecure && !revealsSecureText

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.small)
    ) {
        Text(
            text = label,
            style = CentraliaType.subheadline.semibold(),
            color = CentraliaColors.Ink
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
                .background(CentraliaColors.Surface, RoundedCornerShape(Radius.control))
                .border(
                    width = 1.dp,
                    color = if (errorMessage == null) {
                        CentraliaColors.Divider
                    } else {
                        CentraliaColors.Error
                    },
                    shape = RoundedCornerShape(Radius.control)
                )
                .padding(
                    start = Spacing.medium,
                    end = if (isSecure) 4.dp else Spacing.medium
                )
        ) {
            CentraliaBasicField(
                value = value,
                onValueChange = onValueChange,
                placeholder = placeholder,
                modifier = Modifier.weight(1f),
                keyboardType = keyboardType,
                imeAction = imeAction,
                capitalization = capitalization,
                visualTransformation = if (hidesText) {
                    PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                },
                onSubmit = onSubmit
            )

            if (isSecure) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .centraliaPressable(
                            onClickLabel = if (revealsSecureText) {
                                "Hide password"
                            } else {
                                "Show password"
                            }
                        ) { revealsSecureText = !revealsSecureText },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (revealsSecureText) CentraliaIcons.Hide else CentraliaIcons.Show,
                        contentDescription = null,
                        tint = CentraliaColors.Ink
                    )
                }
            }
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = CentraliaType.caption,
                color = CentraliaColors.Error,
                modifier = Modifier.semanticsLabel("Error: $errorMessage")
            )
        }
    }
}

/**
 * The search control shared by Global Search, the folder list and folder detail:
 * a leading magnifier, a clear affordance once there is text, and a border that
 * thickens to 2dp ink while focused.
 */
@Composable
fun CentraliaSearchField(
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = 58.dp,
    cornerRadius: Dp = 18.dp,
    focusRequester: FocusRequester? = null,
    onFocusChange: (Boolean) -> Unit = {},
    onSubmit: () -> Unit = {}
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = minHeight)
            .background(CentraliaColors.Surface, RoundedCornerShape(cornerRadius))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) CentraliaColors.Ink else CentraliaColors.Divider,
                shape = RoundedCornerShape(cornerRadius)
            )
            .padding(horizontal = Spacing.medium)
    ) {
        Icon(
            imageVector = CentraliaIcons.Search,
            contentDescription = null,
            tint = CentraliaColors.Ink,
            modifier = Modifier.size(24.dp)
        )

        CentraliaBasicField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            modifier = Modifier
                .weight(1f)
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                    onFocusChange(focusState.isFocused)
                },
            imeAction = ImeAction.Search,
            onSubmit = onSubmit
        )

        if (value.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .centraliaPressable(onClickLabel = "Clear search") { onValueChange("") },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CentraliaIcons.ClearCircle,
                    contentDescription = null,
                    tint = CentraliaColors.SecondaryText
                )
            }
        }
    }
}

/**
 * A single-line field in a plain bordered box, used for folder names, new tags
 * and the pasted link. [borderColor] and [borderWidth] carry the save form's
 * always-ink 2dp treatment.
 */
@Composable
fun CentraliaBorderedTextField(
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = 56.dp,
    cornerRadius: Dp = 16.dp,
    borderWidth: Dp = 1.dp,
    borderColor: Color = CentraliaColors.Divider,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    focusRequester: FocusRequester? = null,
    onSubmit: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = minHeight)
            .background(CentraliaColors.Surface, RoundedCornerShape(cornerRadius))
            .border(borderWidth, borderColor, RoundedCornerShape(cornerRadius))
            .padding(horizontal = Spacing.medium),
        contentAlignment = Alignment.CenterStart
    ) {
        CentraliaBasicField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
            keyboardType = keyboardType,
            imeAction = imeAction,
            capitalization = capitalization,
            onSubmit = onSubmit
        )
    }
}

/**
 * `TextEditor` with the `ZStack` placeholder the save and note sheets overlay on
 * top of it.
 */
@Composable
fun CentraliaTextArea(
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = 124.dp,
    cornerRadius: Dp = 16.dp,
    focusRequester: FocusRequester? = null,
    contentDescription: String? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = minHeight)
            .background(CentraliaColors.Surface, RoundedCornerShape(cornerRadius))
            .border(1.dp, CentraliaColors.Divider, RoundedCornerShape(cornerRadius))
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                style = CentraliaType.body,
                color = CentraliaColors.SecondaryText,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 17.dp)
            )
        }

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = minHeight)
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .padding(10.dp)
                .semanticsLabel(contentDescription),
            textStyle = CentraliaType.body.copy(color = CentraliaColors.Ink),
            cursorBrush = SolidColor(CentraliaColors.Ink)
        )
    }
}

/** The shared single-line [BasicTextField] with placeholder handling. */
@Composable
private fun CentraliaBasicField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = CentraliaType.body,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    // The iOS fields set `.textInputAutocapitalization(.never)`; the folder-name
    // fields override it with `.words`.
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onSubmit: () -> Unit = {}
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = textStyle.copy(color = CentraliaColors.Ink),
        singleLine = true,
        cursorBrush = SolidColor(CentraliaColors.Ink),
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(
            capitalization = capitalization,
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onDone = { onSubmit() },
            onSearch = { onSubmit() },
            onGo = { onSubmit() }
        ),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = textStyle,
                        color = CentraliaColors.SecondaryText
                    )
                }
                innerTextField()
            }
        }
    )
}
