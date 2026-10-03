package com.centralia.app.platform

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.centralia.app.domain.sensor.ShakeDetector


@Composable
fun OnShake(onShake: () -> Unit) {
    val context = LocalContext.current
    val currentOnShake by rememberUpdatedState(onShake)

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sensorManager == null || accelerometer == null) {
            return@DisposableEffect onDispose {}
        }

        val vibrator = DeviceVibrator(context)
        val detector = ShakeDetector()
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // m/s² → g, so the numbers match iOS (Core Motion reports g).
                val g = SensorManager.GRAVITY_EARTH.toDouble()
                val shaken = detector.register(
                    x = event.values[0] / g,
                    y = event.values[1] / g,
                    z = event.values[2] / g,
                    atMillis = event.timestamp / 1_000_000L
                )
                if (shaken) {
                    vibrator.shakeDetected()
                    currentOnShake()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        // No handler: callbacks arrive on the main thread, where the UI can be updated.
        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensorManager.unregisterListener(listener) }
    }
}
