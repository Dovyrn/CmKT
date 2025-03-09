package com.dov.cm.modules.render

import com.dov.cm.config.Config
import com.dov.cm.modules.UChat
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
object GlowESP {
    private val mc: MinecraftClient = MinecraftClient.getInstance()



    /**
     * Initialize the GlowESP module
     */
    fun init() {
        // Register client tick event to handle glow effect
        ClientTickEvents.END_CLIENT_TICK.register { _ ->
            if (Config.chamsEnabled) {
                try {
                    updateGlowEffect()
                } catch (e: Exception) {
                    // Prevent crashes
                    e.printStackTrace()
                }
            }
        }

        UChat.mChat("GlowESP module initialized")
    }

    /**
     * Update glow effect for players
     */
    private fun updateGlowEffect() {
        val player = mc.player ?: return
        val world = mc.world ?: return

        // Check weapon-only condition if enabled
        val shouldApplyGlow = true

        // Iterate through all entities in the world
        world.entities.forEach { entity ->
            if (shouldApplyGlow && isValidTarget(entity)) {
                // Apply glow effect client-side
                entity.isGlowing = true
            } else {
                // Remove glow effect for non-targets
                entity.isGlowing = false
            }
        }
    }

    /**
     * Check if the player is holding a weapon
     */


    /**
     * Check if an entity is a valid glow target
     */
    private fun isValidTarget(entity: Entity): Boolean {
        // Only glow player entities
        if (entity is PlayerEntity) {
            return true
        }

        // Don't glow the local player
        if (entity == mc.player) {
            return false
        }
        return true
    }

    /**
     * Reset glow effect when module is disabled
     */
    fun resetGlowEffect() {
        val world = mc.world ?: return

        // Remove glow from all entities
        world.entities.forEach { entity ->
            entity.isGlowing = false
        }
    }

    /**
     * Cleanup method to ensure glow is removed when module is unloaded
     */
    fun onDisable() {
        resetGlowEffect()
    }
}