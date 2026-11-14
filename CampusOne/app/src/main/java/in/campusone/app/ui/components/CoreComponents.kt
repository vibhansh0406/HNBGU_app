package `in`.campusone.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ripple
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import `in`.campusone.app.R
import `in`.campusone.app.ui.theme.C1Border
import `in`.campusone.app.ui.theme.C1Height
import `in`.campusone.app.ui.theme.C1Motion
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
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() }
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
                style = MaterialTheme.typography.titleLarge,
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

/** Centered loading state for full-screen or section-level waits. */
@Composable
fun C1LoadingState(
    modifier: Modifier = Modifier,
    label: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(C1Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(C1Spacing.md)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(28.dp),
            strokeWidth = 2.5.dp,
            color = MaterialTheme.colorScheme.primary
        )
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Consistent error state with an optional retry action. */
@Composable
fun C1ErrorState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    retryLabel: String? = "Retry",
    onRetry: (() -> Unit)? = null
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
                .background(MaterialTheme.colorScheme.errorContainer, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
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
        if (retryLabel != null && onRetry != null) {
            C1Button(
                text = retryLabel,
                onClick = onRetry,
                intent = C1ButtonIntent.TONAL
            )
        }
    }
}

/**
 * Flat content surface: hairline border, no shadow, optional press feedback.
 * Cards group content; they are never nested inside another card.
 */
@Composable
fun C1SurfaceCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(C1Spacing.lg),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = C1Radius.md
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) C1Motion.pressScale else 1f,
        animationSpec = tween(durationMillis = C1Motion.fastMillis),
        label = "cardPress"
    )
    val base = modifier
        .fillMaxWidth()
        .semantics(mergeDescendants = true) {}
    val clickable = if (onClick != null) {
        base.clickable(
            interactionSource = interactionSource,
            indication = ripple(),
            onClick = onClick
        )
    } else {
        base
    }
    Surface(
        modifier = clickable.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        shape = shape,
        color = containerColor,
        border = BorderStroke(C1Border.hairline, borderColor)
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

/** Hairline separator used between rows and sections. */
@Composable
fun C1Divider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outlineVariant
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(C1Border.hairline)
            .background(color)
    )
}

/** Initials avatar for people and authors. */
@Composable
fun C1Avatar(
    name: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 40.dp,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    val initial = name.trim().take(1).uppercase()
    Box(
        modifier = modifier
            .size(size)
            .background(containerColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}

/**
 * List row: leading element, title, optional subtitle, trailing slot.
 * The whole row is one TalkBack target.
 */
@Composable
fun C1ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val base = modifier
        .fillMaxWidth()
        .heightIn(min = C1Height.listRow)
        .semantics(mergeDescendants = true) {}
    val row = if (onClick != null) {
        base.clickable(
            interactionSource = interactionSource,
            indication = ripple(),
            onClick = onClick
        )
    } else {
        base
    }
    Row(
        modifier = row.padding(vertical = C1Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(C1Spacing.md)
    ) {
        if (leading != null) {
            leading()
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            trailing()
        }
    }
}

/** Selectable filter chip with a clear selected state. */
@Composable
fun C1FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.heightIn(min = C1Height.chip),
        shape = C1Radius.pill,
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        border = BorderStroke(
            C1Border.hairline,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        onClick = onClick
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = C1Spacing.md, vertical = C1Spacing.sm)
        )
    }
}

/** Screen title bar: optional back affordance, title, optional subtitle and actions. */
@Composable
fun C1TopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = C1Spacing.lg, vertical = C1Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(C1Spacing.md)
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.size(C1Height.touch)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() }
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (actions != null) {
            actions()
        }
    }
}

/** Shimmering placeholder block used while content loads. */
@Composable
fun C1Skeleton(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = C1Radius.sm
) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "skeletonProgress"
    )
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlight = MaterialTheme.colorScheme.surfaceContainerLow
    val brush = Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = androidx.compose.ui.geometry.Offset(progress * 400f - 200f, 0f),
        end = androidx.compose.ui.geometry.Offset(progress * 400f + 200f, 0f)
    )
    Box(modifier = modifier.background(brush, shape))
}

/** Skeleton list placeholder: a few rows shaped like [C1ListRow]. */
@Composable
fun C1SkeletonRows(
    modifier: Modifier = Modifier,
    rows: Int = 3
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(C1Spacing.md)
    ) {
        repeat(rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(C1Spacing.md)
            ) {
                C1Skeleton(modifier = Modifier.size(40.dp), shape = CircleShape)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(C1Spacing.sm)
                ) {
                    C1Skeleton(modifier = Modifier.fillMaxWidth(0.7f).height(14.dp))
                    C1Skeleton(modifier = Modifier.fillMaxWidth(0.45f).height(12.dp))
                }
            }
        }
    }
}

/**
 * Bottom navigation item with a filled/outlined icon swap, a subtle scale on
 * selection and an optional count badge.
 */
@Composable
fun C1NavItem(
    selected: Boolean,
    onClick: () -> Unit,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0
) {
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.92f,
        animationSpec = tween(durationMillis = C1Motion.fastMillis),
        label = "navIconScale"
    )
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        icon = {
            BadgedBox(
                badge = {
                    if (badgeCount > 0) {
                        Badge {
                            Text(if (badgeCount > 9) "9+" else badgeCount.toString())
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = if (selected) selectedIcon else unselectedIcon,
                    contentDescription = null,
                    modifier = Modifier.graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
                )
            }
        },
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        alwaysShowLabel = true,
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedTextColor = MaterialTheme.colorScheme.onSurface,
            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}
