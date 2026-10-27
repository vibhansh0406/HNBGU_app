package com.campusone.app.domain

import com.campusone.app.domain.usecase.MotionSpecEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class MotionSpecEngineTest {

    @Test
    fun computeTabSlideDirection_returnsPositiveWhenMovingRightAndNegativeWhenMovingLeft() {
        assertEquals(1, MotionSpecEngine.computeTabSlideDirection("home", "attendance"))
        assertEquals(1, MotionSpecEngine.computeTabSlideDirection("attendance", "notices"))
        assertEquals(1, MotionSpecEngine.computeTabSlideDirection("notices", "more"))
        assertEquals(-1, MotionSpecEngine.computeTabSlideDirection("more", "timetable"))
        assertEquals(-1, MotionSpecEngine.computeTabSlideDirection("timetable", "home"))
        assertEquals(0, MotionSpecEngine.computeTabSlideDirection("home", "home"))
    }

    @Test
    fun effectiveDurationAndStagger_respectReducedMotion() {
        assertEquals(80, MotionSpecEngine.effectiveDurationMs(340, reducedMotion = true))
        assertEquals(340, MotionSpecEngine.effectiveDurationMs(340, reducedMotion = false))
        assertEquals(0, MotionSpecEngine.computeStaggerDelayMs(3, reducedMotion = true))
        assertEquals(135, MotionSpecEngine.computeStaggerDelayMs(3, reducedMotion = false))
    }
}

