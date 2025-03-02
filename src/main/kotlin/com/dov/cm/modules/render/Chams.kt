package com.dov.cm.modules.render

import com.dov.cm.config.Config
import com.dov.cm.modules.UChat
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import org.lwjgl.opengl.GL11
import java.util.HashSet

/**
 * Chams module - Simple implementation that shows players through walls
 */
object Chams {
    private val mc: MinecraftClient = MinecraftClient.getInstance()
    private val processedEntities = HashSet<Entity>()

    fun init() {
        // Register tick event to reset the processed entities list
        ClientTickEvents.END_CLIENT_TICK.register { _ ->
            if (!Config.chamsEnabled) {
                resetRendering()
            }
        }

        // Log initialization
        UChat.mChat("Chams Module initialized")
    }

    private fun resetRendering() {
        // Reset any GL state that might have been changed
        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL)
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        processedEntities.clear()
    }

    // Called before entity rendering
    fun preRender(entity: Entity) {
        // Skip if not enabled
        if (!Config.chamsEnabled) return

        // Only apply to players
        if (entity is PlayerEntity && entity != mc.player) {
            // Mark this entity as being processed
            processedEntities.add(entity)

            // Apply the rendering changes for the "through walls" effect
            GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL)
            GL11.glPolygonOffset(1.0f, -2500000.0f)

            // Disable depth test to see through walls
            GL11.glDisable(GL11.GL_DEPTH_TEST)
        }
    }

    // Called after entity rendering
    fun postRender(entity: Entity) {
        // Skip if not enabled
        if (!Config.chamsEnabled) return

        // Check if this entity was processed in preRender
        if (processedEntities.contains(entity)) {
            // Remove from processed list
            processedEntities.remove(entity)

            // Reset the rendering changes
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL)
            GL11.glPolygonOffset(1.0f, 2500000.0f)

            // Re-enable depth test
            GL11.glEnable(GL11.GL_DEPTH_TEST)
        }
    }
}