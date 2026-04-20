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
import java.time.LocalTime
import kotlin.math.sqrt

/**
 * Fuses ambient lighting signals into an [AmbientMode] for the launcher theme.
 *
 * Signal pipeline (cheap → expensive):
 *   TYPE_LIGHT (5Hz, 8-sample rolling average)
 *       │
 *       ▼ if smoothed lux enters an *ambiguous* zone for a day-hour decision
 *   TYPE_LINEAR_ACCELERATION (motion gate)
 *       │
 *       ├─ stationary → probably sensor occlusion, hold mode
 *       │
 *       ▼ phone moving (env. likely changed)
 *   CameraLuxSampler.sample()  — single frame, 30s debounce, permission-gated
 *       │
 *       ▼
 *   Hysteresis state machine (asymmetric bands + 8s min dwell)
 *       │
 *       ▼
 *   AmbientMode
 *
 * Clock is primary: dusk/twilight windows ignore lux entirely. Lux only
 * decides DAYLIGHT_OUTDOOR vs DAY_INTERIOR_DIM *within* the day window.
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

    // Accelerometer: used only as a binary "moved recently" gate.
    private val gravity = FloatArray(3)
    @Volatile private var lastMotionTimestamp: Long = 0L

    @Volatile private var lastCameraPollTimestamp: Long = 0L
    @Volatile private var cameraLuxOverride: Float? = null
    @Volatile private var cameraOverrideUntil: Long = 0L

    // Hysteresis state
    @Volatile private var currentMode: AmbientMode = resolveByClockOnly(LocalTime.now())
    @Volatile private var lastTransitionTimestamp: Long = 0L

    // Dim-sustain timer: when did lux first drop below DIM_ENTER in the current
    // continuous streak? 0 = not currently below. Any reading above DIM_EXIT
    // clears this to 0, starting the next streak from scratch.
    @Volatile private var dimStreakStartMs: Long = 0L

    private val _mode = MutableStateFlow(currentMode)
    val mode: StateFlow<AmbientMode> = _mode.asStateFlow()

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
        // If we got TYPE_LINEAR_ACCELERATION the values already exclude gravity.
        // If we got TYPE_ACCELEROMETER, low-pass-filter gravity out ourselves.
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

        // Expire stale camera override.
        val override = cameraLuxOverride
        if (override != null && now > cameraOverrideUntil) {
            cameraLuxOverride = null
        }

        val effectiveLux = cameraLuxOverride ?: smoothedLux

        // Maintain the dim-streak timer. This runs every reevaluate, independent
        // of the mode decision, so the streak reflects genuinely continuous dimness.
        if (effectiveLux <= DIM_ENTER) {
            if (dimStreakStartMs == 0L) dimStreakStartMs = now
        } else if (effectiveLux >= DIM_EXIT) {
            // Reset — we've seen a clearly-bright reading.
            dimStreakStartMs = 0L
        }
        // Between DIM_ENTER and DIM_EXIT: ambiguous, hold the streak as-is.

        val dimSustainedMs = if (dimStreakStartMs > 0L) now - dimStreakStartMs else 0L
        val time = LocalTime.now()
        val target = resolveMode(time, effectiveLux, currentMode, dimSustainedMs)

        // Min-dwell applies only to up-transitions (→ brighter). Down-transitions
        // are already gated by DIM_SUSTAIN_MS, which is far longer.
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

    /**
     * Camera poll gate — fires as CONFIRMATION that a sustained dim streak is real,
     * not a confirmation of every transient lux change. We only spend a poll when
     * we're genuinely close to transitioning to DIM, to make sure the rear camera
     * agrees with the front sensor before we commit.
     *
     * All conditions must hold:
     *   - CAMERA permission granted
     *   - Day window (the only window where lux matters)
     *   - Currently in OUTDOOR and dim-streak is ≥75% of the way to transitioning
     *   - Phone moved recently (environment likely changed, not occlusion)
     *   - ≥ CAMERA_DEBOUNCE_MS since the last poll
     */
    private fun maybeTriggerCameraPoll(now: Long, time: LocalTime, dimSustainedMs: Long) {
        if (!CameraLuxSampler.isPermissionGranted(context)) return
        if (!isDayWindow(time)) return
        if (currentMode != AmbientMode.DAYLIGHT_OUTDOOR) return
        if (dimSustainedMs < (DIM_SUSTAIN_MS * 3 / 4)) return
        if (now - lastMotionTimestamp > MOTION_RECENT_MS) return
        if (now - lastCameraPollTimestamp < CAMERA_DEBOUNCE_MS) return

        lastCameraPollTimestamp = now
        scope?.launch(Dispatchers.Default) {
            val lux = CameraLuxSampler.sample(context)
            if (lux != null) {
                Log.d(TAG, "Camera lux confirmation: $lux (front sensor: $smoothedLux, streak: ${dimSustainedMs}ms)")
                // If camera sees substantially more light than the front sensor,
                // the front sensor is almost certainly occluded — veto the transition
                // by resetting the streak.
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

        // Lux smoothing
        private const val LUX_HISTORY_SIZE = 8
        private const val SEED_LUX = 400f

        // Motion gate
        private const val MOTION_THRESHOLD = 0.15f   // m/s² linear acceleration
        private const val MOTION_RECENT_MS = 5_000L
        private const val GRAVITY_ALPHA = 0.8f

        // Camera poll
        private const val CAMERA_DEBOUNCE_MS = 30_000L
        private const val CAMERA_OVERRIDE_WINDOW_MS = 12_000L

        // Hysteresis bands for day-hour mode split.
        // Only two day-hour modes in play: DAYLIGHT_OUTDOOR (default) and DAY_INTERIOR_DIM.
        //
        // Asymmetric thresholds AND asymmetric timing:
        //   - Bright is the default. Going dim requires *sustained* low light (DIM_SUSTAIN_MS).
        //   - Returning to bright is fast (MIN_DWELL_MS) — a sunny moment should flip back immediately.
        //   - A single reading above DIM_EXIT resets the dim-dwell timer to zero.
        private const val OUTDOOR_ENTER = 500f       // Enter outdoor if lux ≥ 500 (fast)
        private const val OUTDOOR_EXIT = 400f        // Stay in outdoor unless lux drops below 400
        private const val DIM_ENTER = 80f            // Only consider dim when lux ≤ 80 (truly dark)
        private const val DIM_EXIT = 150f            // Leave dim fast if lux climbs above 150
        private const val DIM_SUSTAIN_MS = 180_000L  // Lux must stay ≤ DIM_ENTER for 3 min to transition
        private const val MIN_DWELL_MS = 8_000L      // Generic min-dwell to suppress flicker (up-transitions)

        // Clock windows
        private val DAY_START = LocalTime.of(6, 30)
        private val DUSK_START = LocalTime.of(16, 30)
        private val TWILIGHT_START = LocalTime.of(18, 30)

        // Loop tick (re-evaluates clock + expires overrides even when sensors silent)
        private const val TICK_INTERVAL_MS = 15_000L

        private fun isDayWindow(t: LocalTime): Boolean =
            !t.isBefore(DAY_START) && t.isBefore(DUSK_START)

        private fun isDuskWindow(t: LocalTime): Boolean =
            !t.isBefore(DUSK_START) && t.isBefore(TWILIGHT_START)

        private fun resolveByClockOnly(t: LocalTime): AmbientMode = when {
            isDayWindow(t) -> AmbientMode.DAYLIGHT_OUTDOOR
            isDuskWindow(t) -> AmbientMode.DUSK
            else -> AmbientMode.TWILIGHT
        }

        /**
         * Combines clock window + hysteresis bands + sustained-dim timer to pick
         * the target mode. Dusk and twilight windows are time-only. Day window
         * defaults to OUTDOOR and only transitions to DIM after lux has been
         * below DIM_ENTER *continuously* for DIM_SUSTAIN_MS. Transitioning back
         * to OUTDOOR is fast (first reading ≥ DIM_EXIT).
         */
        fun resolveMode(
            time: LocalTime,
            lux: Float,
            current: AmbientMode,
            dimSustainedMs: Long,
        ): AmbientMode {
            if (isDuskWindow(time)) return AmbientMode.DUSK
            if (!isDayWindow(time)) return AmbientMode.TWILIGHT

            return when (current) {
                AmbientMode.DAYLIGHT_OUTDOOR -> {
                    // Only transition to DIM if lux is truly dark AND it's been
                    // dark continuously for the sustain window. A cloud passing
                    // over, a hand shadow, or a brief walk through a dark hallway
                    // will NOT trigger the switch.
                    val sustainedDark = lux <= DIM_ENTER && dimSustainedMs >= DIM_SUSTAIN_MS
                    if (sustainedDark) AmbientMode.DAY_INTERIOR_DIM
                    else AmbientMode.DAYLIGHT_OUTDOOR
                }

                AmbientMode.DAY_INTERIOR_DIM -> {
                    // Fast return to bright: any reading clearly above DIM_EXIT flips back.
                    if (lux >= DIM_EXIT) AmbientMode.DAYLIGHT_OUTDOOR
                    else AmbientMode.DAY_INTERIOR_DIM
                }

                // First entry into day window from DUSK/TWILIGHT: default bright.
                // We don't cheat the sustain requirement on mode entry — the user
                // is opening the phone at the start of the day, give them bright.
                else -> AmbientMode.DAYLIGHT_OUTDOOR
            }
        }
    }
}
