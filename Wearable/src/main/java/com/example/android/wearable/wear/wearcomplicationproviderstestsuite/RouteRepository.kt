package com.example.android.wearable.wear.wearcomplicationproviderstestsuite

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.android.wearable.wear.json
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString

object RouteRepository {
    private val ROUTES_KEY = stringPreferencesKey("routes_json")

    suspend fun getRoutes(context: Context): List<Route> {
        return context.dataStore.data.map { prefs ->
            val jsonStr = prefs[ROUTES_KEY] ?: return@map emptyList()
            try {
                json.decodeFromString<List<Route>>(jsonStr)
            } catch (_: Exception) {
                emptyList()
            }
        }.first()
    }

    suspend fun saveRoutes(context: Context, routes: List<Route>) {
        context.dataStore.edit { prefs ->
            prefs[ROUTES_KEY] = json.encodeToString(routes)
        }
    }

    suspend fun addRoute(context: Context, route: Route) {
        val routes = getRoutes(context).toMutableList()
        routes.add(route)
        saveRoutes(context, routes)
    }

    suspend fun removeRoute(context: Context, routeId: String) {
        val routes = getRoutes(context).filter { it.id != routeId }
        saveRoutes(context, routes)
    }
}
