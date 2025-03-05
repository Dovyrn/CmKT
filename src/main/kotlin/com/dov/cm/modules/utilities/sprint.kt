package com.dov.cm.modules.utilities

import com.dov.cm.config.Config
import com.dov.cm.modules.UChat
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.effect.StatusEffects

/**
 * ToggleSprint module - Automatically sprints without holding down the sprint key
 */
object ToggleSprint {
    private val mc: MinecraftClient = MinecraftClient.getInstance()


    /**
     * Initialize the ToggleSprint module
     */
    fun init() {
        // Register tick event to handle sprinting
        ClientTickEvents.END_CLIENT_TICK.register { _ ->
            handleSprinting()
        }

        UChat.mChat("ToggleSprint module initialized")
    }

    /**
     * Handle sprint toggling and automatic sprinting
     */
    private fun handleSprinting() {
        val player = mc.player ?: return

        // Only apply if the feature is enabled in config
        if (Config.sprint && mc.player!!.forwardSpeed > 0) {
            mc.options.sprintKey.isPressed = true
        }
    }


}