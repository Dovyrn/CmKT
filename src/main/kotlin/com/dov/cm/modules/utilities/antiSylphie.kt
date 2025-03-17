package com.dov.cm.modules.utilities

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.MinecraftClient

object AntiSylphie {
    private val mc = MinecraftClient.getInstance()
    private var enabled = true

    fun init() {
        ClientTickEvents.END_CLIENT_TICK.register { _ ->
            onTick()
        }
    }

    private fun onTick() {
        if (!enabled) return

        val player = mc.player ?: return
        val playerName = player.name.string.lowercase()

        if (playerName.contains("sylphie")) {
            println("name: $playerName")
            throw RuntimeException("How dare you steal the identity of sylphie")
        } else if (playerName.contains("inf")){
            println("name: $playerName")
            throw RuntimeException("Fuck you")
        }
    }


}