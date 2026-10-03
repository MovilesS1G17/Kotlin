package com.centralia.app.domain.sensor

import kotlin.math.sqrt


class ShakeDetector {
    companion object {

        const val THRESHOLD_G = 2.7


        const val JOLT_SPACING_MS = 100L


        const val WINDOW_MS = 1000L

        const val JOLTS_NEEDED = 2


        const val COOLDOWN_MS = 1500L
    }

    private var jolts = 0
    private var firstJoltAt = 0L
    private var lastJoltAt: Long? = null
    private var lastShakeAt: Long? = null


    fun register(x: Double, y: Double, z: Double, atMillis: Long): Boolean {
        val force = sqrt(x * x + y * y + z * z)
        if (force < THRESHOLD_G) return false
        lastShakeAt?.let { if (atMillis - it < COOLDOWN_MS) return false }
        lastJoltAt?.let { if (atMillis - it < JOLT_SPACING_MS) return false }

        if (jolts == 0 || atMillis - firstJoltAt > WINDOW_MS) {
            jolts = 0
            firstJoltAt = atMillis
        }
        jolts += 1
        lastJoltAt = atMillis

        if (jolts >= JOLTS_NEEDED) {
            jolts = 0
            lastJoltAt = null
            lastShakeAt = atMillis
            return true
        }
        return false
    }
}
