/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yannickpulver.perron

import android.Manifest
import android.util.Log
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

sealed class LocationResult {
    data class Success(val latitude: Double, val longitude: Double) : LocationResult()
    data class Error(val reason: String) : LocationResult()
}

interface LocationProvider {
    suspend fun getLocation(forceFresh: Boolean = false): LocationResult
}

class WearLocationProvider(private val context: Context) : LocationProvider {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var cachedLocation: Pair<LocationResult, Long>? = null
    private val cacheValidityMs = 5 * 60 * 1000L // 5 minutes

    fun invalidateCache() {
        cachedLocation = null
    }

    suspend fun getLocation(forceFresh: Boolean = false): LocationResult {
        Log.d(TAG, "getLocation() called, forceFresh=$forceFresh")

        // Check permission
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "Permissions: fine=$hasFine, coarse=$hasCoarse")

        if (!hasFine && !hasCoarse) {
            Log.d(TAG, "No location permission")
            return LocationResult.Error("Permission denied")
        }

        if (!forceFresh) {
            // Check cache
            cachedLocation?.let { (result, timestamp) ->
                if (System.currentTimeMillis() - timestamp < cacheValidityMs) {
                    Log.d(TAG, "Returning cached location: $result")
                    return result
                }
            }

            // Try last known location first
            try {
                Log.d(TAG, "Trying lastLocation...")
                val lastLocation = fusedLocationClient.lastLocation.await()
                if (lastLocation != null) {
                    Log.d(TAG, "lastLocation: ${lastLocation.latitude}, ${lastLocation.longitude}")
                    val result = LocationResult.Success(lastLocation.latitude, lastLocation.longitude)
                    cachedLocation = result to System.currentTimeMillis()
                    return result
                }
                Log.d(TAG, "lastLocation was null")
            } catch (e: Exception) {
                Log.e(TAG, "lastLocation error: ${e.message}")
            }
        }

        // Request fresh location with timeout
        Log.d(TAG, "Requesting fresh location...")
        val result = try {
            val cancellationToken = CancellationTokenSource()
            val task = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationToken.token
            )
            Log.d(TAG, "Task created, waiting...")
            val location = withTimeoutOrNull(10000L) {
                task.await()
            }
            Log.d(TAG, "Task completed, location=$location")

            if (location != null) {
                Log.d(TAG, "Fresh location: ${location.latitude}, ${location.longitude}")
                LocationResult.Success(location.latitude, location.longitude)
            } else {
                Log.d(TAG, "Fresh location was null (timeout or unavailable)")
                LocationResult.Error("Location unavailable")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fresh location error: ${e.javaClass.simpleName}: ${e.message}", e)
            LocationResult.Error("Location error: ${e.message}")
        }

        // Cache successful result
        if (result is LocationResult.Success) {
            cachedLocation = result to System.currentTimeMillis()
        }

        Log.d(TAG, "Returning: $result")
        return result
    }

    companion object {
        private const val TAG = "WearLocationProvider"
    }
}
