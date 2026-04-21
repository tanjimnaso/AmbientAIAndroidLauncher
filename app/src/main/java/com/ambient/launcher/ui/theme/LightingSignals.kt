package com.ambient.launcher.ui.theme

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.sqrt

/**
 * Fuses ambient lighting signals into an [AmbientMode] for the launcher theme.
 */
internal class LightingSignals(
    private val context: Context,
) {
    private var scope: CoroutineScope? = null
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    private val linearAccel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val luxHistory = ArrayDeque<Float>(LUX_HISTORY_SIZE + 1)
    @Volatile private var smoothedLux: Float = SEED_LUX

    // Accelerometer
    private val gravity = FloatArray(3)
    @Volatile private var lastMotionTimestamp: Long = 0L

    @Volatile private var lastCameraPollTimestamp: Long = 0L
    @Volatile private var cameraLuxOverride: Float? = null
    @Volatile private var cameraOverrideUntil: Long = 0L

    // Hysteresis state
    @Volatile private var currentMode: AmbientMode = resolveByClockOnly(LocalTime.now())
    @Volatile private var lastTransitionTimestamp: Long = 0L

    // Dim-sustain timer
    @Volatile private var dimStreakStartMs: Long = 0L

    private val _mode = MutableStateFlow(currentMode)
    val mode: StateFlow<AmbientMode> = _mode.asStateFlow()

    private val _lux = MutableStateFlow(SEED_LUX)
    val lux: StateFlow<Float> = _lux.asStateFlow()

    private var lightListener: SensorEventListener? = null
    private var motionListener: SensorEventListener? = null
    private var tickJob: Job? = null

    fun start() {
        if (lightListener != null) return
        val s = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope = s

        lightListener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent?) {
                val raw = e?.values?.firstOrNull() ?: return
                ingestLux(raw)
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }.also {
            if (lightSensor != null) {
                sensorManager.registerListener(it, lightSensor, SensorManager.SENSOR_DELAY_UI)
            }
        }

        motionListener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent?) {
                val v = e?.values ?: return
                ingestAccel(v)
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }.also {
            if (linearAccel != null) {
                sensorManager.registerListener(it, linearAccel, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }

        tickJob = s.launch {
            while (true) {
                delay(TICK_INTERVAL_MS)
                reevaluate()
            }
        }
    }

    fun stop() {
        lightListener?.let { sensorManager.unregisterListener(it) }
        motionListener?.let { sensorManager.unregisterListener(it) }
        lightListener = null
        motionListener = null
        tickJob?.cancel()
        tickJob = null
        scope?.cancel()
        scope = null
    }

    private fun ingestLux(raw: Float) {
        luxHistory.addLast(raw)
        while (luxHistory.size > LUX_HISTORY_SIZE) luxHistory.removeFirst()
        smoothedLux = luxHistory.average().toFloat()
        reevaluate()
    }

    private fun ingestAccel(v: FloatArray) {
        val linear = if (linearAccel?.type == Sensor.TYPE_LINEAR_ACCELERATION) {
            v
        } else {
            for (i in 0..2) gravity[i] = GRAVITY_ALPHA * gravity[i] + (1 - GRAVITY_ALPHA) * v[i]
            floatArrayOf(v[0] - gravity[0], v[1] - gravity[1], v[2] - gravity[2])
        }
        val mag = sqrt(linear[0] * linear[0] + linear[1] * linear[1] + linear[2] * linear[2])
        if (mag > MOTION_THRESHOLD) {
            lastMotionTimestamp = System.currentTimeMillis()
        }
    }

    private fun reevaluate() {
        val now = System.currentTimeMillis()

        val override = cameraLuxOverride
        if (override != null && now > cameraOverrideUntil) {
            cameraLuxOverride = null
        }

        val effectiveLux = cameraLuxOverride ?: smoothedLux
        
        val currentLux = _lux.value
        val change = kotlin.math.abs(effectiveLux - currentLux)
        val significant = if (currentLux > 0) (change / currentLux) > 0.15f else true
        
        val crossingThreshold = (effectiveLux <= DIM_ENTER && currentLux > DIM_ENTER) || 
                                (effectiveLux >= DIM_EXIT && currentLux < DIM_EXIT)

        if (significant || crossingThreshold) {
            _lux.value = effectiveLux
        }

        if (effectiveLux <= DIM_ENTER) {
            if (dimStreakStartMs == 0L) dimStreakStartMs = now
        } else if (effectiveLux >= DIM_EXIT) {
            dimStreakStartMs = 0L
        }

        val dimSustainedMs = if (dimStreakStartMs > 0L) now - dimStreakStartMs else 0L
        val time = LocalTime.now()
        val target = resolveMode(time, effectiveLux, currentMode, dimSustainedMs)

        val isUpTransition = target == AmbientMode.DAYLIGHT_OUTDOOR &&
            currentMode == AmbientMode.DAY_INTERIOR_DIM
        val dwellOk = if (isUpTransition) {
            now - lastTransitionTimestamp >= MIN_DWELL_MS
        } else {
            true
        }

        if (target != currentMode && dwellOk) {
            currentMode = target
            lastTransitionTimestamp = now
            _mode.value = target
        }

        maybeTriggerCameraPoll(now, time, dimSustainedMs)
    }

    private fun maybeTriggerCameraPoll(now: Long, time: LocalTime, dimSustainedMs: Long) {
        if (!CameraLuxSampler.isPermissionGranted(context)) return
        val windows = SolarCalculator.getWindowsForDate(LocalDate.now())
        if (!isDayWindow(time, windows)) return
        if (currentMode != AmbientMode.DAYLIGHT_OUTDOOR) return
        if (dimSustainedMs < (DIM_SUSTAIN_MS * 3 / 4)) return
        if (now - lastMotionTimestamp > MOTION_RECENT_MS) return
        if (now - lastCameraPollTimestamp < CAMERA_DEBOUNCE_MS) return

        lastCameraPollTimestamp = now
        scope?.launch(Dispatchers.Default) {
            val lux = CameraLuxSampler.sample(context)
            if (lux != null) {
                if (lux > DIM_EXIT) {
                    dimStreakStartMs = 0L
                }
                cameraLuxOverride = lux
                cameraOverrideUntil = System.currentTimeMillis() + CAMERA_OVERRIDE_WINDOW_MS
                reevaluate()
            }
        }
    }

    companion object {
        private const val TAG = "LightingSignals"

        private const val LUX_HISTORY_SIZE = 8
        private const val SEED_LUX = 400f

        private const val MOTION_THRESHOLD = 0.15f
        private const val MOTION_RECENT_MS = 5_000L
        private const val GRAVITY_ALPHA = 0.8f

        private const val CAMERA_DEBOUNCE_MS = 30_000L
        private const val CAMERA_OVERRIDE_WINDOW_MS = 12_000L

        private const val DIM_ENTER = 80f
        private const val DIM_EXIT = 150f
        private const val DIM_SUSTAIN_MS = 180_000L
        private const val MIN_DWELL_MS = 8_000L

        private const val TICK_INTERVAL_MS = 15_000L

        private fun isDayWindow(t: LocalTime, w: SolarCalculator.SolarWindows): Boolean =
            !t.isBefore(w.dayStart) && t.isBefore(w.duskStart)

        private fun isDuskWindow(t: LocalTime, w: SolarCalculator.SolarWindows): Boolean =
            !t.isBefore(w.duskStart) && t.isBefore(w.twilightStart)

        private fun isTwilightWindow(t: LocalTime, w: SolarCalculator.SolarWindows): Boolean =
            t.isBefore(w.dayStart) || !t.isBefore(w.twilightStart)

        private fun resolveByClockOnly(t: LocalTime): AmbientMode {
            val windows = SolarCalculator.getWindowsForDate(LocalDate.now())
            return when {
                isTwilightWindow(t, windows) -> AmbientMode.TWILIGHT
                isDuskWindow(t, windows) -> AmbientMode.DUSK
                else -> AmbientMode.DAYLIGHT_OUTDOOR
            }
        }

        fun resolveMode(
            time: LocalTime,
            lux: Float,
            current: AmbientMode,
            dimSustainedMs: Long,
        ): AmbientMode {
            val windows = SolarCalculator.getWindowsForDate(LocalDate.now())
            Log.d(TAG, "Resolving mode: time=$time, lux=$lux, current=$current, windows=$windows")
            
            if (isTwilightWindow(time, windows)) return AmbientMode.TWILIGHT
            if (isDuskWindow(time, windows)) return AmbientMode.DUSK

            // Day window logic:
            val sustainedDark = lux <= DIM_ENTER && dimSustainedMs >= DIM_SUSTAIN_MS

            return when (current) {
                AmbientMode.DAYLIGHT_OUTDOOR -> {
                    if (sustainedDark) AmbientMode.DAY_INTERIOR_DIM
                    else AmbientMode.DAYLIGHT_OUTDOOR
                }
                AmbientMode.DAY_INTERIOR_DIM -> {
                    if (lux >= DIM_EXIT) AmbientMode.DAYLIGHT_OUTDOOR
                    else AmbientMode.DAY_INTERIOR_DIM
                }
                else -> {
                    // Transitioning from DUSK or TWILIGHT back into DAY
                    if (sustainedDark) AmbientMode.DAY_INTERIOR_DIM
                    else AmbientMode.DAYLIGHT_OUTDOOR
                }
            }
        }
    }
}
