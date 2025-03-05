package com.dov.cm.modules.utilities

import com.dov.cm.config.Config
import com.dov.cm.modules.UChat
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects

/**
 * FullBright module - Gives player night vision effect client-side
 */
object FullBright {
    private val mc: MinecraftClient = MinecraftClient.getInstance()
    private var isActive: Boolean = false

    fun init() {
        ClientTickEvents.END_CLIENT_TICK.register { _ ->
            updateNightVision()
        }
    }

    /**
     * Apply client-side night vision effect
     */
    private fun updateNightVision() {
        val player = mc.player ?: return

        if (Config.fullBright) {
            if (!isActive) {
                isActive = true
                UChat.mChat("§aFullBright enabled")
            }

            // Apply night vision effect client-side only
            // Duration is set to 16 seconds (320 ticks) with amplifier 0
            // ShowParticles is set to false to avoid particle effects
            player.addStatusEffect(StatusEffectInstance(StatusEffects.NIGHT_VISION, 320, 0, false, false, true))
        } else if (isActive) {
            // Remove the effect when module is disabled
            player.removeStatusEffect(StatusEffects.NIGHT_VISION)
            isActive = false
            UChat.mChat("§cFullBright disabled")
        }
    }
}