package com.campusone.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout. width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campusone.app.domain.usecase.MotionSpecEngine
import com.campusone.app.ui.motion.FluidMotion
import com.campusone.app.ui.motion.rememberReducedMotionEnabled
import kotlinx.coroutines.delay

data class FluidNavDestination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
    val badgeCount: Int = 0
)

/**
 * Performant simulated glassmorphism card with:
 * - GPU-accelerated press scale feedback (0.98f on press via graphicsLayer)
 * - Smooth tonal background and border color interpolation when active/highlighted
 * - Built-in animateContentSize for fluid expansion and collapse
 */
@Composable
fun InteractiveGlassCard(
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    containerColor: Color = if (isHighlighted) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    },
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val view = LocalView.current
    val reducedMotion = rememberReducedMotionEnabled()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null && !reducedMotion) 0.982f else 1.0f,
        animationSpec = FluidMotion.MicroSpring,
        label = "glass_card_scale"
    )

    val animatedContainerColor by animateColorAsState(
        targetValue = containerColor,
        animationSpec = FluidMotion.standardTween(reducedMotion),
        label = "glass_card_bg"
    )

    val animatedBorderColor by animateColorAsState(
        targetValue = if (isHighlighted) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.11f)
        },
        animationSpec = FluidMotion.standardTween(reducedMotion),
        label = "glass_card_border"
    )

    val animatedElevation by animateDpAsState(
        targetValue = if (isPressed) 1.dp else if (isHighlighted) 4.dp else 2.dp,
        animationSpec = tween(FluidMotion.DURATION_MICRO),
        label = "glass_card_elevation"
    )

    val cardModifier = modifier
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .animateContentSize(animationSpec = FluidMotion.ContentSizeSpring)
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = {
                        FluidMotion.performTickHaptic(view, reducedMotion)
                        onClick()
                    }
                )
            } else {
                Modifier
            }
        )

    Card(
        modifier = cardModifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = animatedContainerColor),
        border = BorderStroke(1.dp, animatedBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = animatedElevation)
    ) {
        Column(content = content)
    }
}

/**
 * Staggered one-shot entrance wrapper.
 * Plays a subtle fade + 12.dp upward slide when [shouldAnimate] is true,
 * and stays immediately visible on subsequent recompositions or tab returns.
 */
@Composable
fun StaggeredEntranceSection(
    sectionIndex: Int,
    shouldAnimate: Boolean,
    onAnimationConsumed: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val reducedMotion = rememberReducedMotionEnabled()
    var visible by remember { mutableStateOf(!shouldAnimate || reducedMotion) }

    LaunchedEffect(shouldAnimate, reducedMotion) {
        if (shouldAnimate && !reducedMotion && !visible) {
            val delayMs = MotionSpecEngine.computeStaggerDelayMs(sectionIndex, reducedMotion)
            if (delayMs > 0) delay(delayMs.toLong())
            visible = true
            onAnimationConsumed()
        } else {
            visible = true
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = FluidMotion.largeTween(reducedMotion),
        label = "stagger_alpha_$sectionIndex"
    )
    val translationY by animateFloatAsState(
        targetValue = if (visible) 0f else 24f,
        animationSpec = FluidMotion.SpatialSpring,
        label = "stagger_y_$sectionIndex"
    )

    Box(
        modifier = Modifier.graphicsLayer {
            this.alpha = alpha
            this.translationY = translationY
        }
    ) {
        content()
    }
}

/**
 * Premium Glassmorphic Bottom Navigation Bar with:
 * - Smoothly sliding active background pill across destinations
 * - Restrained icon scale (1.0f -> 1.08f) and tint transition via updateTransition
 * - Synchronized unread notice badge with scaleIn/scaleOut
 * - WindowInsets.navigationBars padding so scrollable content is never obscured
 */
@Composable
fun FluidGlassBottomBar(
    destinations: List<FluidNavDestination>,
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val view = LocalView.current
    val reducedMotion = rememberReducedMotionEnabled()
    val selectedIndex = destinations.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = if (reducedMotion) tween(90) else FluidMotion.SpatialSpring,
        label = "nav_pill_index"
    )

    val barBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f)
        )
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
        tonalElevation = 4.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.09f))
    ) {
        Box(
            modifier = Modifier
                .background(barBrush)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .height(60.dp)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                val count = destinations.size.coerceAtLeast(1)
                val tabWidth: Dp = maxWidth / count
                val pillOffset: Dp = tabWidth * animatedIndex

                // Sliding Glass Pill Indicator
                Box(
                    modifier = Modifier
                        .offset(x = pillOffset)
                        .width(tabWidth)
                        .fillMaxHeight()
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.70f)
                                )
                            )
                        )
                )

                Row(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    destinations.forEach { dest ->
                        val isSelected = dest.route == currentRoute
                        val transition = updateTransition(
                            targetState = isSelected,
                            label = "nav_item_${dest.route}"
                        )
                        val iconScale by transition.animateFloat(
                            transitionSpec = { FluidMotion.MicroSpring },
                            label = "icon_scale"
                        ) { selected -> if (selected && !reducedMotion) 1.08f else 1.0f }

                        val contentColor by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            animationSpec = FluidMotion.microTween(),
                            label = "nav_tint_${dest.route}"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(20.dp))
                                .clickable(
                                    role = Role.Tab,
                                    onClick = {
                                        if (!isSelected) {
                                            FluidMotion.performTickHaptic(view, reducedMotion)
                                            onNavigate(dest.route)
                                        }
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(contentAlignment = Alignment.TopEnd) {
                                    Icon(
                                        imageVector = dest.icon,
                                        contentDescription = stringResource(dest.labelRes),
                                        tint = contentColor,
                                        modifier = Modifier
                                            .size(22.dp)
                                            .graphicsLayer {
                                                scaleX = iconScale
                                                scaleY = iconScale
                                            }
                                    )
                                    this@Column.AnimatedVisibility(
                                        visible = dest.badgeCount > 0,
                                        enter = scaleIn(FluidMotion.MicroSpring) + fadeIn(tween(120)),
                                        exit = scaleOut(tween(100)) + fadeOut(tween(100))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .offset(x = 8.dp, y = (-4).dp)
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.error),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = dest.badgeCount.coerceAtMost(9).toString(),
                                                color = MaterialTheme.colorScheme.onError,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(dest.labelRes),
                                    color = contentColor,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Inline animated validation error banner for forms and dialogs.
 */
@Composable
fun AnimatedFormValidationBanner(
    visible: Boolean,
    message: String
) {
    val reducedMotion = rememberReducedMotionEnabled()
    AnimatedVisibility(
        visible = visible,
        enter = FluidMotion.expandCollapseEnter(reducedMotion),
        exit = FluidMotion.expandCollapseExit(reducedMotion)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

/**
 * Smooth state switcher between Loading, Empty, and Content states.
 */
@Composable
fun <T> FluidContentStateBox(
    stateKey: String,
    data: T,
    content: @Composable (T) -> Unit
) {
    val reducedMotion = rememberReducedMotionEnabled()
    AnimatedContent(
        targetState = Pair(stateKey, data),
        transitionSpec = { FluidMotion.verticalStateTransform(reducedMotion) },
        contentKey = { it.first },
        label = "fluid_content_state"
    ) { (_, currentData) ->
        content(currentData)
    }
}
