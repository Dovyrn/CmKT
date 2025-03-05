package com.dov.cm.modules.utilities

import com.dov.cm.config.Config
import com.dov.cm.modules.UChat
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.LivingEntity

/**
 * NoJumpDelay module - Removes the delay between jumps
 *
 * This is implemented in two ways:
 * 1. Using a mixin to modify the jump delay directly (most effective)
 * 2. Using a tick event as a fallback to modify the jump timer
 */
object NoJumpDelay {
    private val mc: MinecraftClient = MinecraftClient.getInstance()

    /**
     * Initialize the NoJumpDelay module
     */
    fun init() {
        // Register tick event as a fallback method
        ClientTickEvents.END_CLIENT_TICK.register { _ ->
            handleJumpDelay()
        }

        UChat.mChat("NoJumpDelay module initialized")
    }

    /**
     * Handle jump delay by resetting the jump timer
     * This is a fallback method - the primary method uses mixins
     */
    private fun handleJumpDelay() {
        val player = mc.player ?: return

        // Skip if not enabled
        if (!Config.noJumpDelay) return

        if(mc.options.jumpKey.isPressed && mc.player!!.isOnGround) {
            mc.player!!.jump()
        }


    }
}