/*
masta codda - exotic
*/
package com.dov.cm.util

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.min

object MathUtil {
    fun findMiddleValue(f: Float, f2: Float, f3: Float): Float {
        return if (f < f2) f2 else min(f.toDouble(), f3.toDouble()).toFloat()
    }

    fun interpolate(n: Float, n2: Float, n3: Float): Float {
        return n - (n - n2) * findMiddleValue(n3, 0f, 1f)
    }

    fun scaleAndRoundFloat(n: Float, newScale: Int): Float {
        if (newScale >= 0) {
            return BigDecimal.valueOf(n.toDouble()).setScale(newScale, RoundingMode.FLOOR).toFloat()
        }
        return -1f
    }
}