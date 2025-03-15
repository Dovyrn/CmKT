package com.dov.cm.event

import com.dov.cm.event.Phase

open class Event @JvmOverloads constructor(initialPhase: Phase = Phase.NONE) {
    // Use completely different method name to avoid conflict
    private var _phase: Phase = initialPhase

    // Property for Kotlin access
    open var phase: Phase
        get() = _phase
        set(value) { _phase = value }

    // Use a different method name for Java interop
    open fun retrievePhase(): Phase {
        return _phase
    }

    var isCancelled: Boolean = false

    open fun invoke(): Boolean {
        return false
    }

    fun setCancelled() {
        this.isCancelled = true
    }
}
