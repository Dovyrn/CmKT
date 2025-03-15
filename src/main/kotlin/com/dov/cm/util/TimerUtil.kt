package com.dov.cm.util

class TimerUtil {
    var lastMs: Long = System.currentTimeMillis()

    fun delay(f: Float): Boolean {
        return (System.currentTimeMillis() - lastMs).toFloat() > f
    }


    val elapsedTime: Float
        get() = (System.currentTimeMillis() - this.lastMs).toFloat()

    fun reset() {
        this.lastMs = System.currentTimeMillis()
    }
}
