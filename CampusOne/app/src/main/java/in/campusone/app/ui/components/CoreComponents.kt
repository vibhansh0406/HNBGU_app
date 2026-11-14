package `in`.campusone.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import `in`.campusone.app.ui.theme.C1Radius
import `in`.campusone.app.ui.theme.C1Spacing
import `in`.campusone.app.ui.theme.C1TouchTarget
import `in`.campusone.app.ui.theme.extendedColors

/** Visual intent of a [C1Button]. */
enum class C1ButtonIntent { PRIMARY, TONAL, OUTLINED, DANGER, GHOST }

/**
 * The single button primitive. 48dp tall, medium radius, no shadows —
 * hierarchy comes from fill, not elevation.
 */
@Composable
fun C1Button(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    intent: C1ButtonIntent = C1ButtonIntent.PRIMARY,
    enabled: Boolean = true
) {
    val colors = when (intent) {
        C1ButtonIntent.PRIMARY -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
        C1ButtonIntent.TONAL -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
        C1ButtonIntent.OUTLINED -> ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        )
        C1ButtonIntent.DANGER -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
        C1ButtonIntent.GHOST -> ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        )
    }

    when (intent) {
        C1ButtonIntent.OUTLINED -> OutlinedButton(
            onClick = onClick,
            modifier = modifier.heightIn(min = C1TouchTarget),
            enabled = enabled,
            shape = C1Radius.sm,
            colors = colors,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            contentPadding = PaddingValues(horizontal = C1Spacing.xl, vertical = C1Spacing.md)
        ) {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
        C1ButtonIntent.GHOST -> TextButton(
            onClick = onClick,
            modifier = modifier.heightIn(min = C1TouchTarget),
            enabled = enabled,
            shape = C1Radius.sm,
            colors = colors
        ) {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
        else -> Button(
            onClick = onClick,
            modifier = modifier.heightIn(min = C1TouchTarget),
            enabled = enabled,
            shape = C1Radius.sm,
            colors = colors,
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            contentPadding = PaddingValues(horizontal = C1Spacing.xl, vertical = C1Spacing.md)
        ) {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * Consistent outlined text input: pill-adjacent radius, theme-aware borders,
 * label and supporting text wired to error state.
 */
@Composable
fun C1TextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    val extended = extendedColors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        isError = isError,
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = C1Radius.sm,
        supportingText = supportingText?.let { { Text(it) } },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            errorBorderColor = MaterialTheme.colorScheme.error,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            disabledBorderColor = extended.hairline
        )
    )
}

/** Tone of a [C1StatusChip]. */
enum class C1Tone { SUCCESS, WARNING, DANGER, INFO, NEUTRAL }

/** Compact status pill for counts, states and health labels. */
@Composable
fun C1StatusChip(
    text: String,
    tone: C1Tone,
    modifier: Modifier = Modifier
) {
    val extended = extendedColors
    val (bg, fg) = when (tone) {
        C1Tone.SUCCESS -> extended.successContainer to extended.onSuccessContainer
        C1Tone.WARNING -> extended.warningContainer to extended.onWarningContainer
        C1Tone.DANGER -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        C1Tone.INFO -> extended.infoContainer to extended.onInfoContainer
        C1Tone.NEUTRAL -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = modifier,
        shape = C1Radius.pill,
        color = bg
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            modifier = Modifier.padding(horizontal = C1Spacing.md, vertical = C1Spacing.xs)
        )
    }
}

/** Section title with an optional trailing action, used to open content groups. */
@Composable
fun C1SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** Empty or placeholder state: icon in a tonal circle, title, one-line message. */
@Composable
fun C1EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(C1Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(C1Spacing.md)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = C1Spacing.lg)
        )
    }
}

/** Compact key-figure tile for dashboards and summaries. */
@Composable
fun C1StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    valueColor: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        modifier = modifier,
        shape = C1Radius.md,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = C1Spacing.lg, vertical = C1Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = valueColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.size(C1Spacing.md))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
    }
}

/** Inline informational or warning banner strip. */
@Composable
fun C1Banner(
    text: String,
    tone: C1Tone,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val extended = extendedColors
    val (bg, fg) = when (tone) {
        C1Tone.SUCCESS -> extended.successContainer to extended.onSuccessContainer
        C1Tone.WARNING -> extended.warningContainer to extended.onWarningContainer
        C1Tone.DANGER -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        C1Tone.INFO -> extended.infoContainer to extended.onInfoContainer
        C1Tone.NEUTRAL -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = C1Radius.sm,
        color = bg
    ) {
        Row(
            modifier = Modifier.padding(C1Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = fg
            )
        }
    }
}
