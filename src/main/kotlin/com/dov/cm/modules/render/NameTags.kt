package com.dov.cm.modules.render

import com.dov.cm.config.Config
import com.dov.cm.modules.UChat
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import java.awt.Color

object NametagRenderer {
    private val mc = MinecraftClient.getInstance()

    fun init() {
        // Register world render event for drawing nametags
        WorldRenderEvents.AFTER_ENTITIES.register { context ->
            if (!Config.nametagsEnabled) return@register

            val matrixStack = context.matrixStack()
            val world = mc.world ?: return@register
            val player = mc.player ?: return@register

            // Render nametags for players
            world.players.forEach { entity ->
                if (entity != player) {
                    if (matrixStack != null) {
                        renderNametag(matrixStack, entity)
                    }
                }
            }
        }

        UChat.mChat("Nametag Renderer module initialized")
    }

    private fun renderNametag(matrixStack: MatrixStack, entity: Entity) {
        val client = MinecraftClient.getInstance()
        val camera = client.gameRenderer.camera
        val textRenderer = client.textRenderer

        // Begin matrix transformations
        matrixStack.push()

        // Position relative to camera
        val renderPos = entity.pos.add(0.0, entity.height + 0.5, 0.0)
        val x = renderPos.x - camera.pos.x
        val y = renderPos.y - camera.pos.y
        val z = renderPos.z - camera.pos.z
        matrixStack.translate(x, y, z)

        // Face the camera
        matrixStack.multiply(camera.rotation)

        // Scale down
        val scale = 0.025f
        matrixStack.scale(-scale, -scale, scale)

        // Prepare rendering
        val playerEntity = entity as PlayerEntity
        val text = playerEntity.name.string
        val health = playerEntity.health.toInt()

        // Rendering setup
        val backgroundColor = Color(0, 0, 0, 128).rgb
        val textColor = Color.WHITE.rgb

        // Render
        val vertexConsumers = client.bufferBuilders.entityVertexConsumers
        val backgroundConsumer = vertexConsumers.getBuffer(RenderLayer.getGui())
        val textWidth = textRenderer.getWidth(text).toFloat()

        // Background
        val x1 = -textWidth / 2 - 4
        val x2 = textWidth / 2 + 4
        val y1 = -10f
        val y2 = 0f

        backgroundConsumer.vertex(matrixStack.peek().positionMatrix, x1, y2, 0f).color(backgroundColor)
        backgroundConsumer.vertex(matrixStack.peek().positionMatrix, x2, y2, 0f).color(backgroundColor)
        backgroundConsumer.vertex(matrixStack.peek().positionMatrix, x2, y1, 0f).color(backgroundColor)
        backgroundConsumer.vertex(matrixStack.peek().positionMatrix, x1, y1, 0f).color(backgroundColor)

        // Name text
        textRenderer.draw(
            text,
            -textWidth / 2,
            -9f,
            textColor,
            false,
            matrixStack.peek().positionMatrix,
            vertexConsumers,
            TextRenderer.TextLayerType.SEE_THROUGH,
            0,
            15728880
        )

        // Clean up matrix
        matrixStack.pop()
    }
}