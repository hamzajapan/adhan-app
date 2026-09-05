package com.adhanapp.qibla

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** حساب اتجاه القبلة (زاوية من الشمال الحقيقي باتجاه عقارب الساعة) دون إنترنت */
object QiblaMath {
    private const val KAABA_LAT = 21.4225
    private const val KAABA_LNG = 39.8262

    fun bearing(lat: Double, lng: Double): Double {
        val phi1 = Math.toRadians(lat)
        val phi2 = Math.toRadians(KAABA_LAT)
        val dLambda = Math.toRadians(KAABA_LNG - lng)
        val y = sin(dLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLambda)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }
}
