package com.ambient.launcher.ui.theme

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.util.Log
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.log2
import kotlin.math.pow

/**
 * One-shot ambient lux sampler using the rear camera.
 *
 * Used as a disambiguation signal when the front TYPE_LIGHT sensor reading is
 * suspected to be unreliable (e.g. occluded). NOT a continuous stream — opens
 * the camera, captures exactly one frame with its exposure metadata, closes.
 *
 * Expected cost per call: ~300–800ms wall time, ~50–80mW peak burst. A green
 * privacy indicator will flash for the duration. Caller must debounce hard.
 *
 * Lux is computed from the exposure triangle (time + ISO + aperture) rather
 * than pixel mean, because auto-exposure converges all frames toward middle
 * gray regardless of scene brightness.
 *
 *   EV₁₀₀ = log₂(aperture² / exposureSec) − log₂(iso / 100)
 *   lux   ≈ 2.5 × 2^EV₁₀₀
 */
internal object CameraLuxSampler {
    private const val TAG = "CameraLuxSampler"
    private const val SAMPLE_TIMEOUT_MS = 2_500L
    private const val INCIDENT_K_FACTOR = 2.5f  // Standard incident-light meter calibration

    fun isPermissionGranted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Captures a single frame and returns estimated scene lux, or null on any
     * failure (permission denied, no camera, timeout, missing metadata).
     */
    @SuppressLint("RestrictedApi", "UnsafeOptInUsageError")
    suspend fun sample(context: Context): Float? = withContext(Dispatchers.Main) {
        if (!isPermissionGranted(context)) return@withContext null

        val provider = try {
            ProcessCameraProvider.getInstance(context).get()
        } catch (t: Throwable) {
            Log.w(TAG, "Camera provider unavailable: ${t.message}")
            return@withContext null
        }

        val selector = pickSelector(provider) ?: run {
            Log.w(TAG, "No camera available")
            return@withContext null
        }

        val result = CompletableDeferred<Float?>()
        val owner = EphemeralLifecycleOwner()

        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .apply {
                Camera2Interop.Extender(this).setSessionCaptureCallback(
                    object : android.hardware.camera2.CameraCaptureSession.CaptureCallback() {
                        override fun onCaptureCompleted(
                            session: android.hardware.camera2.CameraCaptureSession,
                            request: CaptureRequest,
                            totalResult: TotalCaptureResult
                        ) {
                            if (result.isCompleted) return
                            val lux = luxFromCaptureResult(totalResult)
                            result.complete(lux)
                        }
                    }
                )
            }
            .build()

        analysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { image: ImageProxy ->
            // We don't use the pixels — exposure metadata drives the estimate.
            // Just close the frame promptly so the pipeline can flow.
            image.close()
        }

        return@withContext try {
            owner.start()
            provider.bindToLifecycle(owner, selector, analysis)
            withTimeoutOrNull(SAMPLE_TIMEOUT_MS) { result.await() }
        } catch (t: Throwable) {
            Log.w(TAG, "Sample failed: ${t.message}")
            null
        } finally {
            try { provider.unbind(analysis) } catch (_: Throwable) {}
            owner.stop()
        }
    }

    private fun pickSelector(provider: ProcessCameraProvider): CameraSelector? {
        // Rear preferred — points at ceiling/environment rather than user's face.
        if (provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
            return CameraSelector.DEFAULT_BACK_CAMERA
        }
        if (provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
            return CameraSelector.DEFAULT_FRONT_CAMERA
        }
        return null
    }

    private fun luxFromCaptureResult(r: TotalCaptureResult): Float? {
        val exposureNs = r.get(CaptureResult.SENSOR_EXPOSURE_TIME) ?: return null
        val iso = r.get(CaptureResult.SENSOR_SENSITIVITY) ?: return null
        val aperture = r.get(CaptureResult.LENS_APERTURE) ?: DEFAULT_APERTURE

        if (exposureNs <= 0L || iso <= 0) return null

        val exposureSec = exposureNs / 1_000_000_000.0
        val ev100 = log2((aperture * aperture) / exposureSec) - log2(iso / 100.0)
        val lux = INCIDENT_K_FACTOR * 2.0.pow(ev100)
        return lux.toFloat().coerceIn(0f, 200_000f)
    }

    private const val DEFAULT_APERTURE = 1.8f  // Typical phone camera when LENS_APERTURE absent
}

/**
 * Minimal LifecycleOwner used only to bind/unbind a single CameraX session.
 * Created, started, used for one sample, stopped, discarded.
 */
private class EphemeralLifecycleOwner : LifecycleOwner {
    private val registry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = registry
    fun start() {
        registry.currentState = Lifecycle.State.STARTED
    }
    fun stop() {
        registry.currentState = Lifecycle.State.DESTROYED
    }
}
