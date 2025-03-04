package com.dov.cm.modules.render

import com.dov.cm.modules.UChat
import com.dov.cm.util.EntityEspUtil
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.entity.Entity
import java.awt.Color
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Centralized handler for all rendering tasks that need to happen during the render cycle
 */
object RenderHandler {
    // Thread-safe queue to store entities that need ESP rendering
    private val espRenderQueue = ConcurrentLinkedQueue<RenderTask>()

    // Data class to store render tasks
    data class RenderTask(
        val entity: Entity,
        val color: Color,
        val lineWidth: Float = 2f,
        val filled: Boolean = false,
        val outlined: Boolean = true,
        val fillOpacity: Float = 0.25f,
        val expand: Float = 0f
    )

    /**
     * Initialize the render handler by registering event hooks
     */
    fun init() {
        // Register for the after translucent phase, which is a good spot for ESP rendering
        WorldRenderEvents.AFTER_TRANSLUCENT.register { context ->
            processRenderQueue(context)
        }

        UChat.mChat("§aRender Handler initialized")
    }

    /**
     * Queue an entity for ESP rendering during the next render cycle
     */
    fun queueEntityForRendering(
        entity: Entity,
        color: Color,
        lineWidth: Float = 2f,
        filled: Boolean = false,
        outlined: Boolean = true,
        fillOpacity: Float = 0.25f,
        expand: Float = 0f
    ) {
        espRenderQueue.add(
            RenderTask(
                entity = entity,
                color = color,
                lineWidth = lineWidth,
                filled = filled,
                outlined = outlined,
                fillOpacity = fillOpacity,
                expand = expand
            )
        )
    }

    /**
     * Process all queued render tasks
     */
    private fun processRenderQueue(context: WorldRenderContext) {
        try {
            // Create a snapshot of the current queue to work with
            val tasks = espRenderQueue.toList()

            // Clear the queue before processing to avoid re-processing
            espRenderQueue.clear()

            // Process each task
            tasks.forEach { task ->
                try {
                    // Only render if the entity is still valid/loaded
                    if (task.entity.isAlive) {
                        EntityEspUtil.renderEntity(
                            task.entity,
                            task.color,
                            task.lineWidth,
                            task.filled,
                            task.outlined,
                            task.fillOpacity,
                            task.expand
                        )
                    }
                } catch (e: Exception) {
                    // Silently handle any errors in individual renders
                }
            }
        } catch (e: Exception) {
            // Catch any errors during rendering to prevent crashes
        }
    }
}