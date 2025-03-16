package com.dov.cm.modules

import com.logicalgeekboy.logical_zoom.skid
import com.logicalgeekboy.logical_zoom.java_modules.AimAssist
import com.dov.cm.config.Config
import com.logicalgeekboy.logical_zoom.java_modules.Hitboxes

class ModuleManager {
    fun registerModule(moduleClass: Any, enabled: Boolean) {

        if (enabled) {
            skid.Companion.getEventBus().registerListener(moduleClass)
        } else {
            skid.Companion.getEventBus().unregisterListener(moduleClass)
        }
    }
    val aimAssist = AimAssist()
    val hitbox = Hitboxes()
    private var aimAssistRegistered = false
    private var hitboxRegistered = false
    fun initModules() {


        val aimAssistEnabled = Config.aimAssistEnabled
        val hitboxEnabled = Config.HitboxEnabled





        // Register the module
        registerModule(hitbox, hitboxEnabled)
        registerModule(aimAssist, aimAssistEnabled)

        // Verify registration by checking event bus listeners
        val eventBus = skid.Companion.getEventBus()
    }
    fun updateModuleRegistrations() {
        val aimAssistEnabled = Config.aimAssistEnabled
        val hitboxEnabled = Config.HitboxEnabled

        // Only register/unregister if status has changed
        if (aimAssistEnabled != aimAssistRegistered) {
            registerModule(aimAssist, aimAssistEnabled)
            aimAssistRegistered = aimAssistEnabled
        }

        if (hitboxEnabled != hitboxRegistered) {
            registerModule(hitbox, hitboxEnabled)
            hitboxRegistered = hitboxEnabled
        }
    }
}