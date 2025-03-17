package com.dov.cm.modules

import com.logicalgeekboy.logical_zoom.skid
import com.logicalgeekboy.logical_zoom.java_modules.AimAssist
import com.dov.cm.config.Config
import com.logicalgeekboy.logical_zoom.java_modules.PotionRefill

class ModuleManager {
    fun registerModule(moduleClass: Any, enabled: Boolean) {

        if (enabled) {
            skid.Companion.getEventBus().registerListener(moduleClass)
        } else {
            skid.Companion.getEventBus().unregisterListener(moduleClass)
        }
    }
    val aimAssist = AimAssist()
    val potRefill = PotionRefill()
    private var aimAssistRegistered = false
    private var potRefillRegistered = false
    fun initModules() {


        val aimAssistEnabled = Config.aimAssistEnabled
        val potRefillEnabled = Config.potRefill






        // Register the module
        registerModule(aimAssist, aimAssistEnabled)
        registerModule(potRefill, potRefillEnabled)

        // Verify registration by checking event bus listeners
        val eventBus = skid.Companion.getEventBus()
    }
    fun updateModuleRegistrations() {
        val aimAssistEnabled = Config.aimAssistEnabled
        val potRefillEnabled = Config.potRefill

        // Only register/unregister if status has changed
        if (aimAssistEnabled != aimAssistRegistered) {
            registerModule(aimAssist, aimAssistEnabled)
            aimAssistRegistered = aimAssistEnabled
        }
        if (potRefillEnabled != aimAssistRegistered){
            registerModule(potRefill, potRefillEnabled)
            aimAssistRegistered = aimAssistEnabled
        }


    }
}