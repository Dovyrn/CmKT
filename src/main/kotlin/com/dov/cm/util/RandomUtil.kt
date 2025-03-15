package com.dov.cm.util

import java.util.*

class RandomUtil {
    var random: Random = Random()

    fun randomInRange(n: Int, n2: Int): Int {
        return n + random.nextInt(n2 - n + 1)
    }

    fun randomInRange(f: Float, f2: Float): Float {
        return MathUtil.findMiddleValue(f + random.nextFloat() * f2, f, f2)
    }

    companion object {
        var INSTANCE: RandomUtil = RandomUtil()
    }
}