package com.yoline.autouprightrotating.utils

import kotlin.math.roundToInt
import kotlin.math.roundToLong

object DegreesUtils {

    fun Double.degreesShrink(
        startDegrees: Int = -180, endDegrees: Int = 180, includeStart: Boolean = false
    ): Double {
        return degreesShrink(this, startDegrees, endDegrees, includeStart)
    }

    fun Float.degreesShrink(
        startDegrees: Int = -180, endDegrees: Int = 180, includeStart: Boolean = false
    ): Float {
        return degreesShrink(this, startDegrees, endDegrees, includeStart).toFloat()
    }

    fun Long.degreesShrink(
        startDegrees: Int = -180, endDegrees: Int = 180, includeStart: Boolean = false
    ): Long {
        return degreesShrink(this, startDegrees, endDegrees, includeStart).roundToLong()
    }

    fun Int.degreesShrink(
        startDegrees: Int = -180, endDegrees: Int = 180, includeStart: Boolean = false
    ): Int {
        return degreesShrink(this, startDegrees, endDegrees, includeStart).roundToInt()
    }

    private fun <T : Number> degreesShrink(
        number: T, startDegrees: Int = -180, endDegrees: Int = 180, includeStart: Boolean = false
    ): Double {
        return (number.toDouble() % 360).let {
            val includeValue = (if (includeStart) startDegrees else endDegrees).toDouble()
            if ((it > startDegrees && it < endDegrees) || it == includeValue) number.toDouble()
            else (if (it > 0) it - 360 else it + 360).toDouble()
        }
    }
}