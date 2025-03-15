/*
when sex mod for prestige clarinet? - cloovey
*/
package com.dov.cm.managers

import com.dov.cm.event.EventBus
import com.dov.cm.event.EventListener
import com.dov.cm.event.Priority
import com.dov.cm.event.Render2DEvent

class RenderManager(private val eventBus: EventBus) {
    var ms: Long = 0
    var lastMs: Long = 0

    init {
        eventBus.registerListener(this)
    }

    @EventListener(priority = Priority.HIGHEST)
    fun event(event: Render2DEvent?) {
        ms = System.currentTimeMillis() - lastMs
        lastMs = System.currentTimeMillis()
        if (ms > 30) {
            ms = 0L
        }
    }

    fun getMs(): Float {
        return ms.toFloat() * 0.005f
    }

    companion object {
        private var INSTANCE: RenderManager? = null

        fun initialize(eventBus: EventBus) {
            if (INSTANCE == null) {
                INSTANCE = RenderManager(eventBus)
            }
        }

        fun getInstance(): RenderManager {
            return INSTANCE ?: throw IllegalStateException("RenderManager not initialized")
        }
    }
}