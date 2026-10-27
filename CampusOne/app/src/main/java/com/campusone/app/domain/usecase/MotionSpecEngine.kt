package com.campusone.app.domain.usecase

/**
 * Pure Kotlin motion decision engine (unit-tested in MotionSpecEngineTest).
 * Computes deterministic directional slide polarity, reduced-motion scaling,
 * and staggered entrance delays so transitions never conflict or replay erratically.
 */
object MotionSpecEngine {

    const val MICRO_DURATION_MS: Int = 140
    const val STANDARD_DURATION_MS: Int = 260
    const val LARGE_DURATION_MS: Int = 340

    private val ROUTE_ORDER: Map<String, Int> = mapOf(
        "home" to 0,
        "attendance" to 1,
        "timetable" to 2,
        "notices" to 3,
        "more" to 4,
        "study" to 4,
        "campus" to 4
    )

    /**
     * Returns +1 when navigating forward (left-to-right tab order),
     * -1 when navigating backward (right-to-left tab order),
     * or 0 when staying on the same ordinal tier.
     */
    fun computeTabSlideDirection(fromRoute: String?, toRoute: String?): Int {
        val fromIdx = ROUTE_ORDER[fromRoute ?: "home"] ?: 0
        val toIdx = ROUTE_ORDER[toRoute ?: "home"] ?: 0
        return when {
            toIdx > fromIdx -> 1
            toIdx < fromIdx -> -1
            else -> 0
        }
    }

    /**
     * Scales duration tokens based on system animator duration scale and reduced-motion preference.
     */
    fun effectiveDurationMs(baseDurationMs: Int, reducedMotion: Boolean): Int {
        if (reducedMotion) return 80
        return baseDurationMs.coerceIn(80, 600)
    }

    /**
     * Computes staggered entrance delay in milliseconds for dashboard sections,
     * capped at 180 ms so content never feels sluggish.
     */
    fun computeStaggerDelayMs(sectionIndex: Int, reducedMotion: Boolean): Int {
        if (reducedMotion) return 0
        return (sectionIndex.coerceIn(0, 6) * 45).coerceAtMost(180)
    }
}
