package com.dov.cm.modules

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.MinecraftClient
import com.dov.cm.config.Config

object Hitboxes {
    private val mc: MinecraftClient = MinecraftClient.getInstance()

    // Settings
    private var enabled: Boolean = Config.HitboxEnabled
    private var expand: Float = Config.hitboxExpand

    fun init() {
        // Register event to modify player hitboxes every tick
        ClientTickEvents.END_CLIENT_TICK.register { onClientTick() }
    }

    private fun onClientTick() {
        if (!enabled) return

        mc.world?.players?.forEach { player ->
            if (player != mc.player) {
                player.boundingBox = player.boundingBox.expand(expand / 10.0)
            }
        }
    }

    fun toggle() {
        enabled = !enabled
    }

    fun setExpand(value: Float) {
        expand = value
    }
}
