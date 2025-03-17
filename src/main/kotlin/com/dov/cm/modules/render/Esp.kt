/*
 * MIT License
 *
 * Copyright (c) 2022-2025
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.dov.cm.modules.render

import com.dov.cm.config.Config
import com.dov.cm.modules.UChat
import com.dov.cm.util.RenderUtils
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.player.PlayerEntity

/**
 * PlayerESP Module - Renders ESP boxes and tracers around other players
 */
object PlayerESP {
    private val mc: MinecraftClient = MinecraftClient.getInstance()

    /**
     * Initialize the PlayerESP module
     */
    fun init() {
        // Register render event for main ESP rendering
        WorldRenderEvents.AFTER_ENTITIES.register { context ->
            if (Config.espEnabled) {
                renderPlayerESP(context.matrixStack())
            }
        }

        UChat.mChat("PlayerESP module initialized")
    }

    /**
     * Render ESP boxes and tracers around players
     */
    private fun renderPlayerESP(matrixStack: MatrixStack?) {
        if (matrixStack == null || !Config.espEnabled || !Config.espRenderPlayers)  return

        val world = mc.world ?: return
        val player = mc.player ?: return
        val region = RenderUtils.getCameraRegionPos()

        // Setup rendering
        RenderUtils.setupRenderWithShader(matrixStack)

        // Process all players in the world for ESP
        world.players.forEach { target ->
            // Skip rendering the client player
            if (target != player && target.isAlive) {
                // Extract color components from config
                val configColor = Config.espPlayerColor
                val red = configColor.red / 255f
                val green = configColor.green / 255f
                val blue = configColor.blue / 255f
                val alpha = configColor.alpha / 255f

                // Render box around player
                RenderUtils.renderEntityBox(
                    matrixStack,
                    mc.renderTickCounter.getTickDelta(true),
                    target,
                    red, green, blue, alpha,
                    region
                )

                // Only render tracers if ESP is enabled
                if (Config.espTracerEnabled) {
                    val buffer = RenderUtils.getBufferBuilder()
                    val matrix = matrixStack.peek().positionMatrix

                    // Draw tracer from crosshair to player
                    RenderUtils.drawSingleLine(
                        buffer,
                        matrix,
                        mc.renderTickCounter.getTickDelta(true),
                        target,
                        red, green, blue, alpha
                    )

                    RenderUtils.drawBuffer(buffer)
                }
            }
        }

        // Clean up rendering state
        RenderUtils.cleanupRender(matrixStack)
    }

    /**
     * Add this method to CmKtClient initialization:
     *
     * fun init() {
     *     PlayerESP.init()
     * }
     */
}