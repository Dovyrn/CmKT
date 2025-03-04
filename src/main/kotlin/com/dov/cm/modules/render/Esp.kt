package com.dov.cm.modules.render

import com.dov.cm.config.Config
import com.dov.cm.util.EntityEspUtil
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.client.util.math.MatrixStack
import java.awt.Color

/**
 * PlayerESP - Highlights all players in the world with ESP boxes
 */
class PlayerESP {
    private val mc = MinecraftClient.getInstance()
    private var enabled = false

    // Default ESP settings
    private val defaultEspColor = Color(255, 0, 0, 255) // Red with full opacity

    /**
     * Initialize the module
     */
    fun init() {
        // Module initialization will be handled by your client
        enabled = Config.espEnabled // Assuming you have this config option
    }



    /**
     * Render the player ESP - call this from your world render event handler
     */
    fun onRender(matrixStack: MatrixStack, tickDelta: Float) {
        if (!enabled || mc.world == null) return

        // Get the configured ESP color from your config
        val espColor = Config.espPlayerColor ?: defaultEspColor

        // Render ESP for all players except the client player
        mc.world?.players?.forEach { player ->
            if (player != mc.player && shouldRenderPlayer(player)) {
                EntityEspUtil.renderEntity(
                    entity = player,
                    color = espColor,
                    lineWidth = 2f,
                    filled = true,
                    outlined = true,
                    fillOpacity = 0.2f,
                    expand = 0f
                )
            }
        }
    }

    /**
     * Determine if a player should be rendered with ESP
     */
    private fun shouldRenderPlayer(player: PlayerEntity): Boolean {
        // You can add additional filtering criteria here, such as:
        // - Team checks
        // - Friend list checks
        // - Distance checks
        // - Visibility checks

        // For now, render all players
        return player.isInvisible // Don't render invisible players (optional)
    }


}