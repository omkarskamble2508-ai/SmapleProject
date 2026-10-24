package com.example.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import kotlin.coroutines.resume

@JsonClass(generateAdapter = false)
data class OpenMeteoResponse(
    val current: OpenMeteoCurrent? = null,
    val daily: OpenMeteoDaily? = null
)

@JsonClass(generateAdapter = false)
data class OpenMeteoCurrent(
    val precipitation: Double? = null,
    val rain: Double? = null
)

@JsonClass(generateAdapter = false)
data class OpenMeteoDaily(
    val precipitation_sum: List<Double>? = null
)

interface OpenMeteoApi {
    @GET("v1/forecast")
    suspend fun getRainfallForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "precipitation,rain",
        @Query("daily") daily: String = "precipitation_sum",
        @Query("timezone") timezone: String = "auto"
    ): OpenMeteoResponse
}

class WeatherAndGeoService(private val context: Context) {

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val weatherApi: OpenMeteoApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenMeteoApi::class.java)
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentDeviceGpsLocation(): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        try {
            suspendCancellableCoroutine { cont ->
                val tokenSource = CancellationTokenSource()
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    tokenSource.token
                ).addOnSuccessListener { location: Location? ->
                    if (cont.isActive) {
                        if (location != null) {
                            cont.resume(location.latitude to location.longitude)
                        } else {
                            cont.resume(null)
                        }
                    }
                }.addOnFailureListener {
                    if (cont.isActive) cont.resume(null)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchLiveRainfallMm(latitude: Double, longitude: Double): Double? =
        withContext(Dispatchers.IO) {
            try {
                val response = weatherApi.getRainfallForecast(latitude, longitude)
                val todaySum = response.daily?.precipitation_sum?.firstOrNull()
                val currentRain = response.current?.precipitation
                when {
                    todaySum != null && todaySum > 0.0 -> todaySum
                    currentRain != null -> currentRain * 24.0
                    else -> null
                }
            } catch (e: Exception) {
                null
            }
        }
}
