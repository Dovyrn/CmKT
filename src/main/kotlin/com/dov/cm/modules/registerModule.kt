package com.dov.cm.modules

import com.logicalgeekboy.logical_zoom.skid
import com.logicalgeekboy.logical_zoom.java_modules.* // Add this import
import com.dov.cm.config.Config // Make sure you import the correct Config class

class ModuleManager {
    fun registerModule(moduleClass: Any, enabled: Boolean) {
        if (enabled) {
            skid.Companion.getEventBus().registerListener(moduleClass)
        } else {
            skid.Companion.getEventBus().unregisterListener(moduleClass)
        }
    }

    // Add a method to initialize your modules
    fun initModules() {
        registerModule(AimAssist(), Config.aimAssistEnabled)
    }
}