package com.dov.cm.modules.combat

import com.dov.cm.config.Config
import com.dov.cm.modules.UChat
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.Box

/**
 * Enhanced Hitbox module - Expands entity hitboxes to make them easier to hit
 */
object EnhancedHitbox {
    private val mc: MinecraftClient = MinecraftClient.getInstance()

    // Tracking collections
    private var originalBoxes = HashMap<Int, Box>()
    private var lastExpandAmount = 0.0

    /**
     * Initialize the Enhanced Hitbox module
     */
    fun init() {
        // Register tick event to update hitboxes EVERY tick
        ClientTickEvents.START_CLIENT_TICK.register { _ ->
            if (Config.HitboxEnabled) {
                expandHitboxes()
            } else {
                resetHitboxes()
            }
        }

        UChat.mChat("Enhanced Hitbox module initialized")
    }

    /**
     * Expand hitboxes for all applicable entities
     */
    private fun expandHitboxes() {
        val world = mc.world ?: return
        val player = mc.player ?: return
        val expandAmount = Config.hitboxExpand.toDouble()

        // Reset boxes if expand amount changed
        if (expandAmount != lastExpandAmount) {
            resetHitboxes()
            lastExpandAmount = expandAmount
        }

        // Process all entities
        world.entities.forEach { entity ->
            if (entity != player && shouldExpandEntity(entity)) {
                val entityId = entity.id

                // Store original box if we haven't seen this entity
                if (!originalBoxes.containsKey(entityId)) {
                    originalBoxes[entityId] = entity.boundingBox
                }

                // Always update with fresh expansion
                val originalBox = originalBoxes[entityId] ?: entity.boundingBox
                entity.boundingBox = originalBox.expand(expandAmount)
            }
        }

        // Clean up entities that no longer exist
        val entitiesToRemove = ArrayList<Int>()
        originalBoxes.keys.forEach { id ->
            if (world.getEntityById(id) == null) {
                entitiesToRemove.add(id)
            }
        }

        entitiesToRemove.forEach { id ->
            originalBoxes.remove(id)
        }
    }

    /**
     * Reset all hitboxes to original size
     */
    private fun resetHitboxes() {
        val world = mc.world ?: return

        originalBoxes.forEach { (id, originalBox) ->
            val entity = world.getEntityById(id)
            entity?.boundingBox = originalBox
        }

        originalBoxes.clear()
    }

    /**
     * Check if an entity should have its hitbox expanded
     */
    private fun shouldExpandEntity(entity: Entity): Boolean {
        return when (// Players only mode
            Config.hitboxTargets) {
            0 -> entity is PlayerEntity

            // All entities mode
            1 -> entity is LivingEntity

            // Default fallback
            else -> false
        }
    }
}