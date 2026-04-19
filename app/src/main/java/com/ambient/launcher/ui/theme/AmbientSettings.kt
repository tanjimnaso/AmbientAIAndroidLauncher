package com.ambient.launcher.ui.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Application-scoped design flags that live outside the time-of-day palette system.
 *
 * - [monochrome]: when true, `clusterIntelligence/Utility/Communication/Assistant/Health`
 *   collapse to `inkColor`. Icons and accents become positional, not chromatic —
 *   a Kindle/Muji-style drop-out that still respects the daily palette transitions.
 */
internal object AmbientSettings {
    private const val PREFS = "ambient_design_prefs"
    private const val KEY_MONOCHROME = "monochrome_ink"

    private val _monochrome = MutableStateFlow(false)
    val monochrome: StateFlow<Boolean> = _monochrome.asStateFlow()

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).also {
            _monochrome.value = it.getBoolean(KEY_MONOCHROME, false)
        }
    }

    fun setMonochrome(value: Boolean) {
        _monochrome.value = value
        prefs?.edit()?.putBoolean(KEY_MONOCHROME, value)?.apply()
    }
}
