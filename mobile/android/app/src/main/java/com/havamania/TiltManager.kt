package com.havamania

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart

data class TiltState(val pitch: Float = 0f, val roll: Float = 0f)

@Composable
fun rememberTiltState(enabled: Boolean = true): State<TiltState> {
    val context = LocalContext.current
    val tiltState = remember { mutableStateOf(TiltState()) }

    if (!enabled) return tiltState

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return

                // Low-pass filter to smooth out the movement
                val x = event.values[0]
                val y = event.values[1]

                // Map sensor values to a reasonable tilt range (-1.0 to 1.0)
                // Normalize by typical max tilt (approx 5.0 for subtle feel)
                val newRoll = (x / 5.0f).coerceIn(-1f, 1f)
                val newPitch = (y / 5.0f).coerceIn(-1f, 1f)

                tiltState.value = TiltState(
                    pitch = tiltState.value.pitch * 0.9f + newPitch * 0.1f,
                    roll = tiltState.value.roll * 0.9f + newRoll * 0.1f
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    return tiltState
}
