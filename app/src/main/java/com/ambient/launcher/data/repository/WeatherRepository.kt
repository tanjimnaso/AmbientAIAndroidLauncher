package com.ambient.launcher.data.repository

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import com.ambient.launcher.ForecastDay
import com.ambient.launcher.HttpClient
import com.ambient.launcher.WeatherUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
import kotlin.coroutines.resume

internal class WeatherRepository(private val context: Context) {
    private val client = HttpClient.instance

    suspend fun fetchWeather(): WeatherUiState? = withContext(Dispatchers.IO) {
        val location = getLastKnownLocation()
        val (latitude, longitude, locationName) = if (location != null) {
            Triple(location.latitude, location.longitude, resolveLocationName(location))
        } else {
            Triple(-33.8688, 151.2093, "Sydney")
        }
        
        val url = buildString {
            append("https://api.open-meteo.com/v1/forecast?")
            append("latitude=$latitude")
            append("&longitude=$longitude")
            append("&current=temperature_2m,weather_code")
            append("&daily=temperature_2m_max,temperature_2m_min,weather_code")
            append("&forecast_days=4")
            append("&timezone=auto")
        }

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "AmbientLauncher/1.0")
            .build()

        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching null
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) return@runCatching null

                val root = JSONObject(body)
                val current = root.getJSONObject("current")
                val daily = root.getJSONObject("daily")

                val temp = current.getDouble("temperature_2m").toInt()
                val code = current.getInt("weather_code")
                
                val maxTemps = daily.getJSONArray("temperature_2m_max")
                val minTemps = daily.getJSONArray("temperature_2m_min")
                val weatherCodes = daily.getJSONArray("weather_code")
                val timeArray = daily.getJSONArray("time")

                val forecast = mutableListOf<ForecastDay>()
                for (i in 1 until 4) {
                    val date = java.time.LocalDate.parse(timeArray.getString(i))
                    forecast.add(
                        ForecastDay(
                            dayName = date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH),
                            maxTemp = maxTemps.getDouble(i).toInt(),
                            minTemp = minTemps.getDouble(i).toInt(),
                            weatherCode = weatherCodes.getInt(i)
                        )
                    )
                }

                WeatherUiState(
                    temperatureText = "$temp°",
                    rangeText = "H:${maxTemps.getDouble(0).toInt()}° L:${minTemps.getDouble(0).toInt()}°",
                    summary = weatherCodeToSummary(code),
                    locationName = locationName,
                    isAvailable = true,
                    currentCode = code,
                    forecast = forecast
                )
            }
        }.getOrNull()
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLastKnownLocation(): Location? {
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasCoarse && !hasFine) return null

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
                return suspendCancellableCoroutine { cont ->
                    val cancellationSignal = CancellationSignal()
                    cont.invokeOnCancellation { cancellationSignal.cancel() }
                    
                    locationManager.getCurrentLocation(
                        LocationManager.NETWORK_PROVIDER,
                        cancellationSignal,
                        context.mainExecutor
                    ) { location ->
                        cont.resume(location)
                    }
                } ?: getBestLastKnown(locationManager)
            } catch (e: Exception) {
                return getBestLastKnown(locationManager)
            }
        }

        return getBestLastKnown(locationManager)
    }

    @SuppressLint("MissingPermission")
    private fun getBestLastKnown(locationManager: LocationManager): Location? {
        val providers = locationManager.getProviders(true)
        var bestLocation: Location? = null
        for (provider in providers) {
            val l = locationManager.getLastKnownLocation(provider) ?: continue
            if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                bestLocation = l
            }
        }
        return bestLocation
    }

    private fun resolveLocationName(location: Location): String {
        val geocoder = Geocoder(context, Locale.getDefault())
        return runCatching {
            @Suppress("DEPRECATION")
            geocoder.getFromLocation(location.latitude, location.longitude, 1)
                ?.firstOrNull()
                ?.let { address ->
                    address.locality ?: address.subAdminArea ?: address.adminArea ?: address.countryName
                }
        }.getOrNull().orEmpty()
    }

    private fun weatherCodeToSummary(code: Int): String {
        return when (code) {
            0 -> "Clear"
            1, 2 -> "Mostly clear"
            3 -> "Cloudy"
            45, 48 -> "Fog"
            51, 53, 55 -> "Drizzle"
            56, 57 -> "Freezing Drizzle"
            61, 63, 65 -> "Rain"
            66, 67 -> "Freezing Rain"
            71, 73, 75 -> "Snow"
            77 -> "Snow grains"
            80, 81, 82 -> "Rain showers"
            85, 86 -> "Snow showers"
            95 -> "Thunderstorm"
            96, 99 -> "Thunderstorm with hail"
            else -> "Unknown"
        }
    }
}
