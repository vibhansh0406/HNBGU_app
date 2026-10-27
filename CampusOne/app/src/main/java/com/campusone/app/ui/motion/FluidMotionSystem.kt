package com.campusone.app.ui.motion

import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.campusone.app.domain.usecase.MotionSpecEngine

/**
 * Centralized production-grade fluid motion system for CampusOne.
 * Defines all durations, easing curves, spring specifications, directional screen transitions,
 * reduced-motion fallbacks, and subtle haptic feedback helpers.
 */
object FluidMotion {

    // Durations (ms)
    const val DURATION_MICRO: Int = MotionSpecEngine.MICRO_DURATION_MS      // 140 ms (100-180 ms tier)
    const val DURATION_STANDARD: Int = MotionSpecEngine.STANDARD_DURATION_MS // 260 ms (200-320 ms tier)
    const val DURATION_LARGE: Int = MotionSpecEngine.LARGE_DURATION_MS       // 340 ms (280-400 ms tier)

    // Easing Curves
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    val StandardEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    // Controlled, Non-Bouncy Springs
    val MicroSpring: FiniteAnimationSpec<Float> = spring(
        dampingRatio = 0.90f,
        stiffness = 650f
    )

    val SpatialSpring: FiniteAnimationSpec<Float> = spring(
        dampingRatio = 0.86f,
        stiffness = 420f
    )

    val ContentSizeSpring: FiniteAnimationSpec<IntSize> = spring(
        dampingRatio = 0.88f,
        stiffness = 440f
    )

    val PlacementSpring: FiniteAnimationSpec<IntOffset> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    fun <T> microTween(): FiniteAnimationSpec<T> = tween(
        durationMillis = DURATION_MICRO,
        easing = FastOutSlowInEasing
    )

    fun <T> standardTween(reducedMotion: Boolean = false): FiniteAnimationSpec<T> = tween(
        durationMillis = MotionSpecEngine.effectiveDurationMs(DURATION_STANDARD, reducedMotion),
        easing = StandardEasing
    )

    fun <T> largeTween(reducedMotion: Boolean = false): FiniteAnimationSpec<T> = tween(
        durationMillis = MotionSpecEngine.effectiveDurationMs(DURATION_LARGE, reducedMotion),
        easing = EmphasizedDecelerate
    )

    /**
     * Directional top-level screen enter transition.
     * Uses a restrained 10% horizontal slide combined with a clean fade.
     */
    fun tabEnterTransition(direction: Int, reducedMotion: Boolean): EnterTransition {
        if (reducedMotion || direction == 0) {
            return fadeIn(animationSpec = tween(durationMillis = 120))
        }
        return slideInHorizontally(
            animationSpec = tween(durationMillis = DURATION_STANDARD, easing = EmphasizedDecelerate),
            initialOffsetX = { fullWidth -> (fullWidth * 0.10f * direction).toInt() }
        ) + fadeIn(
            animationSpec = tween(durationMillis = DURATION_STANDARD, easing = StandardEasing)
        )
    }

    /**
     * Directional top-level screen exit transition.
     */
    fun tabExitTransition(direction: Int, reducedMotion: Boolean): ExitTransition {
        if (reducedMotion || direction == 0) {
            return fadeOut(animationSpec = tween(durationMillis = 100))
        }
        return slideOutHorizontally(
            animationSpec = tween(durationMillis = 200, easing = EmphasizedAccelerate),
            targetOffsetX = { fullWidth -> -(fullWidth * 0.07f * direction).toInt() }
        ) + fadeOut(
            animationSpec = tween(durationMillis = 180, easing = FastOutLinearInEasing)
        )
    }

    /**
     * Directional AnimatedContent transition for day-selector and sub-tab changes.
     */
    fun directionalContentTransform(direction: Int, reducedMotion: Boolean): ContentTransform {
        if (reducedMotion || direction == 0) {
            return fadeIn(tween(120)) togetherWith fadeOut(tween(90))
        }
        val enter = slideInHorizontally(
            animationSpec = tween(DURATION_STANDARD, easing = EmphasizedDecelerate),
            initialOffsetX = { w -> (w * 0.08f * direction).toInt() }
        ) + fadeIn(tween(DURATION_STANDARD))
        val exit = slideOutHorizontally(
            animationSpec = tween(180, easing = EmphasizedAccelerate),
            targetOffsetX = { w -> -(w * 0.06f * direction).toInt() }
        ) + fadeOut(tween(150))
        return enter togetherWith exit
    }

    /**
     * Vertical subtle state transition for counters, advice text, and Next Class cards.
     */
    fun verticalStateTransform(reducedMotion: Boolean = false): ContentTransform {
        if (reducedMotion) {
            return fadeIn(tween(90)) togetherWith fadeOut(tween(90))
        }
        val enter = slideInVertically(
            animationSpec = tween(DURATION_STANDARD, easing = EmphasizedDecelerate),
            initialOffsetY = { h -> (h * 0.25f).toInt() }
        ) + fadeIn(tween(DURATION_STANDARD))
        val exit = slideOutVertically(
            animationSpec = tween(160, easing = EmphasizedAccelerate),
            targetOffsetY = { h -> -(h * 0.20f).toInt() }
        ) + fadeOut(tween(140))
        return enter togetherWith exit
    }

    fun expandCollapseEnter(reducedMotion: Boolean = false): EnterTransition {
        if (reducedMotion) return fadeIn(tween(90))
        return expandVertically(
            expandFrom = Alignment.Top,
            animationSpec = tween(DURATION_STANDARD, easing = EmphasizedDecelerate)
        ) + fadeIn(tween(DURATION_STANDARD))
    }

    fun expandCollapseExit(reducedMotion: Boolean = false): ExitTransition {
        if (reducedMotion) return fadeOut(tween(90))
        return shrinkVertically(
            shrinkTowards = Alignment.Top,
            animationSpec = tween(200, easing = EmphasizedAccelerate)
        ) + fadeOut(tween(160))
    }

    fun modalEnter(reducedMotion: Boolean = false): EnterTransition {
        if (reducedMotion) return fadeIn(tween(90))
        return scaleIn(
            initialScale = 0.94f,
            animationSpec = tween(DURATION_STANDARD, easing = EmphasizedDecelerate)
        ) + fadeIn(tween(DURATION_MICRO))
    }

    fun modalExit(reducedMotion: Boolean = false): ExitTransition {
        if (reducedMotion) return fadeOut(tween(90))
        return scaleOut(
            targetScale = 0.95f,
            animationSpec = tween(180, easing = EmphasizedAccelerate)
        ) + fadeOut(tween(150))
    }

    fun performTickHaptic(view: View, reducedMotion: Boolean = false) {
        if (reducedMotion) return
        try {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        } catch (_: Exception) {
        }
    }
}

@Composable
fun rememberReducedMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            scale <= 0.05f
        } catch (_: Exception) {
            false
        }
    }
}

