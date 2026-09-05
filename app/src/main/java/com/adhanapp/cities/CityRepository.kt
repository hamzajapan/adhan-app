package com.adhanapp.cities

import android.content.Context
import org.json.JSONArray
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class City(
    val nameAr: String,
    val nameEn: String,
    val country: String,
    val lat: Double,
    val lng: Double,
    val timezone: String,
)

class CityRepository(context: Context) {

    val cities: List<City> by lazy {
        val json = context.assets.open("cities.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            City(
                nameAr = o.getString("name_ar"),
                nameEn = o.getString("name_en"),
                country = o.getString("country"),
                lat = o.getDouble("lat"),
                lng = o.getDouble("lng"),
                timezone = o.getString("timezone"),
            )
        }
    }

    /** بحث فوري بالعربية أو الإنجليزية */
    fun search(query: String): List<City> {
        val q = query.trim()
        if (q.isEmpty()) return cities
        return cities.filter {
            it.nameAr.contains(q) || it.nameEn.contains(q, ignoreCase = true)
        }
    }

    /** أقرب مدينة لإحداثيات معيّنة (دون إنترنت) */
    fun nearest(lat: Double, lng: Double): City =
        cities.minByOrNull { distanceKm(lat, lng, it.lat, it.lng) } ?: cities.first()

    companion object {
        fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
            val r = 6371.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLng = Math.toRadians(lng2 - lng1)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2) * sin(dLng / 2)
            return 2 * r * atan2(sqrt(a), sqrt(1 - a))
        }
    }
}
