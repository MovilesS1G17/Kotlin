package com.centralia.app

import com.centralia.app.domain.sensor.ShakeDetector
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test


class ShakeDetectorTest {
    @Test
    fun phoneLyingStillNeverShakes() {
        val detector = ShakeDetector()
        for (t in 0L..3000L step 20L) assertFalse(detector.register(0.0, 0.0, 1.0, t))
    }

    @Test
    fun twoStrongJoltsWithinASecondAreAShake() {
        val detector = ShakeDetector()
        assertFalse(detector.register(3.0, 0.5, 1.0, 0))
        assertFalse(detector.register(3.0, 0.5, 1.0, 40)) // same jolt
        assertTrue(detector.register(-3.0, 0.0, 1.0, 300))
    }

    @Test
    fun aSingleBumpOrSlowJoltsDontCount() {
        val detector = ShakeDetector()
        assertFalse(detector.register(3.0, 0.0, 0.0, 0))
        assertFalse(detector.register(3.0, 0.0, 0.0, 1500)) // too late: starts over
        assertFalse(detector.register(2.0, 1.0, 1.0, 1700)) // ~2.4 g: too weak
    }

    @Test
    fun oneShakeUndoesOnlyOnce() {
        val detector = ShakeDetector()
        detector.register(3.0, 0.0, 0.0, 0)
        assertTrue(detector.register(3.0, 0.0, 0.0, 200))
        assertFalse(detector.register(3.0, 0.0, 0.0, 400))
        assertFalse(detector.register(3.0, 0.0, 0.0, 600))
        detector.register(3.0, 0.0, 0.0, 2000)
        assertTrue(detector.register(3.0, 0.0, 0.0, 2200))
    }
}
