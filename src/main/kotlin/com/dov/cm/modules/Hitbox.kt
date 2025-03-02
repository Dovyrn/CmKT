package com.dov.cm.modules

import com.dov.cm.config.Config
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.Box

/**
 * Hitbox module that expands entity hitboxes to make them easier to hit
 */
object Hitbox {
    private val mc: MinecraftClient = MinecraftClient.getInstance()

    // Expansion settings
    private var expandAmount = 0.0
    private var originalBoxes = HashMap<Int, Box>()
    private var affectedEntities = HashSet<Int>()
    private var expandPlayers = true
    private var expandMobs = true

    // Original hitboxes for rendering - used to draw the expanded boxes
    private val renderBoxes = HashMap<Int, Box>()

    /**
     * Initialize the Hitbox module
     */
    fun init() {
        // Register tick event to update hitboxes
        ClientTickEvents.END_CLIENT_TICK.register { onClientTick() }

        // Register render event for debug visualizations if needed
        // HudRenderCallback.EVENT.register { context, _ -> onRender(context) }
    }

    /**
     * Update on each client tick
     */
    private fun onClientTick() {
        // Only update if the hitbox module is enabled
        if (!Config.HitboxEnabled) {
            resetAllHitboxes()
            return
        }

        // Update settings from Config
        updateSettings()

        // Get world and player
        val world = mc.world ?: return
        val player = mc.player ?: return

        // Track which entities we've seen this tick
        val currentEntities = HashSet<Int>()

        // Iterate through all loaded entities
        world.entities.forEach { entity ->
            // Skip the player
            if (entity == player) return@forEach

            // Check if this entity should have an expanded hitbox
            if (shouldExpandEntity(entity)) {
                // Get entity ID
                val entityId = entity.id
                currentEntities.add(entityId)

                // Only modify entities we haven't yet affected
                if (!affectedEntities.contains(entityId)) {
                    // Save original hitbox if we haven't already
                    if (!originalBoxes.containsKey(entityId)) {
                        originalBoxes[entityId] = entity.boundingBox
                        renderBoxes[entityId] = entity.boundingBox
                    }

                    // Create expanded box
                    val expandedBox = expandBox(entity.boundingBox, expandAmount)

                    // Update entity hitbox
                    entity.boundingBox = expandedBox

                    // Mark entity as affected
                    affectedEntities.add(entityId)
                }
            } else {
                // Reset this entity if it's been affected before
                resetEntityHitbox(entity)
            }
        }

        // Reset hitboxes for entities that no longer exist or are not in range
        val entitiesToReset = affectedEntities.filter { !currentEntities.contains(it) }
        for (entityId in entitiesToReset) {
            affectedEntities.remove(entityId)
            originalBoxes.remove(entityId)
            renderBoxes.remove(entityId)
        }
    }

    /**
     * Update settings from Config
     */
    private fun updateSettings() {
        expandAmount = Config.hitboxExpand.toDouble()
        expandPlayers = Config.hitboxPlayers
        expandMobs = Config.hitboxMobs
    }

    /**
     * Check if an entity should have its hitbox expanded
     */
    private fun shouldExpandEntity(entity: Entity): Boolean {
        return when (entity) {
            is PlayerEntity -> expandPlayers
            is LivingEntity -> expandMobs
            else -> false
        }
    }

    /**
     * Expand a bounding box by the given amount
     */
    private fun expandBox(box: Box, amount: Double): Box {
        return box.expand(amount)
    }

    /**
     * Reset a specific entity's hitbox to its original size
     */
    private fun resetEntityHitbox(entity: Entity) {
        val entityId = entity.id
        if (affectedEntities.contains(entityId)) {
            val originalBox = originalBoxes[entityId]
            if (originalBox != null) {
                entity.boundingBox = originalBox
            }
            affectedEntities.remove(entityId)
        }
    }

    /**
     * Reset all hitboxes to their original sizes
     */
    private fun resetAllHitboxes() {
        val world = mc.world ?: return

        // Reset all affected entities
        for (entityId in affectedEntities) {
            val originalBox = originalBoxes[entityId] ?: continue
            val entity = world.getEntityById(entityId) ?: continue
            entity.boundingBox = originalBox
        }

        // Clear the tracking collections
        affectedEntities.clear()
        originalBoxes.clear()
        renderBoxes.clear()
    }

}