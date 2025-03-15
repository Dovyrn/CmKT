package com.dov.cm.modules.combat

import com.dov.cm.config.Config
import com.dov.cm.modules.UChat
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.AxeItem
import net.minecraft.item.MaceItem
import net.minecraft.item.SwordItem
import net.minecraft.item.TridentItem
import net.minecraft.util.math.Box
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt

/**
 * Backtrack module - Delays packet processing for players moving out of range
 * Allows landing hits on players who would otherwise be out of reach
 */
object Backtrack {
    private val mc: MinecraftClient = MinecraftClient.getInstance()

    // Track player positions
    private val trackedPlayers = ConcurrentHashMap<Int, TrackedPlayer>()

    // Track module state
    private var active = false
    private var lastDeactivationTime = 0L
    private var currentTarget: PlayerEntity? = null
    private var currentDelay = 0L

    // Data class to store player tracking information
    data class TrackedPlayer(
        val entityId: Int,
        var originalBox: Box,           // The real position of the player
        var lastValidPosition: Box,     // The last position when player was in valid range
        var lastUpdateTime: Long,       // When this player's position was last updated
        var trackedSince: Long,         // When we started tracking this player
        var hurtTime: Int = 0           // Current hurt time of the player
    )

    /**
     * Initialize the Backtrack module
     */
    fun init() {
        // Register tick event to manage tracking
        ClientTickEvents.END_CLIENT_TICK.register { _ ->
            if (Config.backtrackEnabled) {
                tick()
            } else if (active) {
                // If module was just disabled, clear tracking
                deactivate()
            }
        }

        UChat.mChat("Backtrack module initialized")
    }

    /**
     * Main tick handler - called every game tick when module is enabled
     */
    private fun tick() {
        val player = mc.player ?: return
        val world = mc.world ?: return

        // Check weapon condition if enabled
        if (Config.backtrackWeaponOnly && !isHoldingWeapon()) {
            if (active) deactivate()
            return
        }

        // Process all players in the world
        world.players.forEach { target ->
            // Skip self and non-living entities
            if (target == player || !target.isAlive) {
                return@forEach
            }

            // Get current distance to target
            val distance = sqrt(target.squaredDistanceTo(player))

            // Check if target is within valid distance range
            val inRange = distance >= Config.backtrackMinDistance &&
                    distance <= Config.backtrackMaxDistance

            // Get entity ID for tracking
            val entityId = target.id

            // If player exists in our tracking, update their data
            if (trackedPlayers.containsKey(entityId)) {
                val tracked = trackedPlayers[entityId]!!

                // Update hurt time
                tracked.hurtTime = target.hurtTime

                // Always update their real position
                tracked.originalBox = target.boundingBox
                tracked.lastUpdateTime = System.currentTimeMillis()

                // If player is in valid range, update their valid position
                if (inRange && target.hurtTime <= Config.backtrackMaxHurtTime) {
                    tracked.lastValidPosition = target.boundingBox
                }

                // If player has been tracked for too long, remove them
                if (System.currentTimeMillis() - tracked.trackedSince > 10000) {
                    trackedPlayers.remove(entityId)
                }
            } else {
                // Start tracking this player
                trackedPlayers[entityId] = TrackedPlayer(
                    entityId = entityId,
                    originalBox = target.boundingBox,
                    lastValidPosition = target.boundingBox,
                    lastUpdateTime = System.currentTimeMillis(),
                    trackedSince = System.currentTimeMillis(),
                    hurtTime = target.hurtTime
                )
            }

            // Handle backtracking for this target
            handleBacktrack(target)
        }

        // Clean up tracking for players no longer in the world
        val toRemove = mutableListOf<Int>()
        trackedPlayers.forEach { (id, _) ->
            if (world.getEntityById(id) == null) {
                toRemove.add(id)
            }
        }
        toRemove.forEach { trackedPlayers.remove(it) }
    }

    /**
     * Handle backtracking for a specific target
     */
    private fun handleBacktrack(target: PlayerEntity) {
        val player = mc.player ?: return

        // Get current distance
        val distance = sqrt(target.squaredDistanceTo(player))

        // Is player now out of our max range?
        val outOfRange = distance > Config.backtrackMaxDistance

        // Get tracked player data
        val tracked = trackedPlayers[target.id] ?: return

        // Only backtrack if:
        // 1. Player is out of range
        // 2. We have a valid position stored
        // 3. Player's hurt time is below our threshold
        // 4. Cooldown period has elapsed since last deactivation
        val cooldownElapsed = System.currentTimeMillis() - lastDeactivationTime > (Config.backtrackCooldown * 1000)

        if (outOfRange && tracked.hurtTime <= Config.backtrackMaxHurtTime && cooldownElapsed) {
            // Determine if we should backtrack this player
            val timeSinceUpdate = System.currentTimeMillis() - tracked.lastUpdateTime

            if (timeSinceUpdate < Config.backtrackMaxDelay && !active) {
                // Start backtracking
                active = true
                currentTarget = target
                currentDelay = 0L

                // Set entity position to last valid position
                target.boundingBox = tracked.lastValidPosition

                // Debug message
                if (Config.developerMode) {
                    UChat.mChat("§aBacktracking player: ${target.name.string}")
                }
            } else if (active && currentTarget == target) {
                // Continue backtracking up to max delay
                currentDelay = timeSinceUpdate

                if (currentDelay < Config.backtrackMaxDelay) {
                    // Maintain the backtracked position
                    target.boundingBox = tracked.lastValidPosition
                } else {
                    // Max delay reached, stop backtracking
                    deactivate()
                }
            }
        } else if (active && currentTarget == target &&
            (!outOfRange || tracked.hurtTime > Config.backtrackMaxHurtTime)) {
            // Target came back in range or got hit, stop backtracking
            deactivate()
        }
    }

    /**
     * Deactivate backtracking and reset state
     */
    private fun deactivate() {
        if (!active) return

        active = false
        lastDeactivationTime = System.currentTimeMillis()
        currentTarget = null
        currentDelay = 0L

        // Reset all entities to their real positions
        val world = mc.world ?: return

        trackedPlayers.forEach { (id, tracked) ->
            val entity = world.getEntityById(id) as? PlayerEntity ?: return@forEach
            entity.boundingBox = tracked.originalBox
        }

        // Debug message
        if (Config.developerMode) {
            UChat.mChat("§cBacktracking deactivated")
        }
    }

    /**
     * Check if player got hit (for Disable on Hit feature)
     */
    fun onPlayerDamaged() {
        if (Config.backtrackDisableOnHit && active) {
            deactivate()
        }
    }

    /**
     * Check if player is holding a weapon
     */
    private fun isHoldingWeapon(): Boolean {
        val player = mc.player ?: return false
        val heldItem = player.mainHandStack.item

        return heldItem is SwordItem ||
                heldItem is AxeItem ||
                heldItem is TridentItem ||
                heldItem is MaceItem ||
                heldItem.translationKey.contains("mace")
    }
}