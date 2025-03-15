package com.dov.cm.modules.combat

import com.dov.cm.config.Config
import com.dov.cm.modules.UChat
import com.dov.cm.modules.render.TargetHUD
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.Hand
import net.minecraft.util.math.Box
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * KillAura module - Automatically attacks entities within a specified range
 */
object KillAura {
    private val mc: MinecraftClient = MinecraftClient.getInstance()
    private var lastAttackTime: Long = 0
    var currentTarget: Entity? = null
    private var originalYaw: Float = 0f
    private var originalPitch: Float = 0f

    /**
     * Initialize the KillAura module
     */
    fun init() {
        // Register tick event to handle KillAura logic
        ClientTickEvents.END_CLIENT_TICK.register { _ ->
            if (Config.killAuraEnabled) {
                try {
                    onTick()
                } catch (e: Exception) {
                    // Prevent crashes
                    UChat.mChat("§cKillAura error: ${e.message}")
                }
            } else {
                // Reset current target when disabled
                currentTarget = null
            }
        }

        UChat.mChat("KillAura module initialized")
    }

    /**
     * Main tick handler for KillAura
     */
    private fun onTick() {
        val player = mc.player ?: return
        val world = mc.world ?: return

        // Skip if player is in a GUI
        if (mc.currentScreen != null) {
            return
        }


        // Check for crit-only setting
        if (Config.killAuraCritOnly && (player.isOnGround || player.velocity.y  >= 0)) {
            return
        }

        // Get the cooldown progress


        // Find target
        val target = findTarget(player)

        if (target != null) {
            currentTarget = target

            // Handle rotation
            if (Config.killAuraRotation == 1) { // Silent rotation
                val rotations = calculateRotations(target)
                sendRotationPacket(rotations.first, rotations.second)
                player.headYaw = rotations.first
                player.bodyYaw = rotations.first
            }
            val cooldownProgress = player.getAttackCooldownProgress(0f)
            if (cooldownProgress < 0.9f) {
                return
            }
            // Attack the target
            attackTarget(player, target)
        } else {
            currentTarget = null
        }
    }

    /**
     * Find the best target within reach
     */
    private fun findTarget(player: ClientPlayerEntity): Entity? {
        val world = mc.world ?: return null

        // Create a box around the player with the configured reach
        val reach = Config.killAuraReach.toDouble()
        val box = Box(
            player.x - reach, player.y - reach, player.z - reach,
            player.x + reach, player.y + reach, player.z + reach
        )

        // Get all potential entities
        val potentialTargets = mutableListOf<Entity>()

        if (Config.killAuraTargets == 0) { // Players only
            world.players.forEach { entity ->
                if (entity != player && isValidTarget(entity) && player.distanceTo(entity) <= reach) {
                    potentialTargets.add(entity)
                }
            }
        } else { // All entities
            world.getEntitiesByClass(LivingEntity::class.java, box) { entity ->
                entity != player && isValidTarget(entity)
            }.forEach { entity ->
                potentialTargets.add(entity)
            }
        }

        // Find the closest entity
        return potentialTargets.minByOrNull { player.distanceTo(it) }
    }

    /**
     * Check if an entity is a valid target
     */
    private fun isValidTarget(entity: Entity): Boolean {
        // Check if entity is alive
        if (entity !is LivingEntity || !entity.isAlive) {
            return false
        }

        // Player-specific checks
        if (entity is PlayerEntity) {
            // Additional checks for players can be added here
            return true
        }

        // If targeting all entities and entity is living
        return Config.killAuraTargets == 1
    }

    /**
     * Calculate rotations to target entity
     */
    private fun calculateRotations(entity: Entity): Pair<Float, Float> {
        val player = mc.player ?: return Pair(0f, 0f)

        // Get positions
        val playerPos = player.eyePos
        val targetPos = entity.pos.add(0.0, entity.height * 0.5, 0.0)

        // Calculate differences
        val diffX = targetPos.x - playerPos.x
        val diffY = targetPos.y - playerPos.y
        val diffZ = targetPos.z - playerPos.z

        // Calculate distance
        val distance = sqrt(diffX * diffX + diffZ * diffZ)

        // Calculate yaw and pitch
        val yaw = MathHelper.wrapDegrees((atan2(diffZ, diffX) * 180 / PI).toFloat() - 90f)
        val pitch = MathHelper.wrapDegrees(-(atan2(diffY, distance) * 180 / PI).toFloat())

        return Pair(yaw, pitch)
    }

    /**
     * Send rotation packet for silent rotation
     */
    private fun sendRotationPacket(yaw: Float, pitch: Float) {
        val player = mc.player ?: return
        val networkHandler = mc.networkHandler ?: return

        // Store original rotations
        originalYaw = player.yaw
        originalPitch = player.pitch

        // Send rotation packet
        val packet = PlayerMoveC2SPacket.Full(
            player.x, player.y, player.z,
            yaw, pitch,
            player.isOnGround,
            player.horizontalCollision
        )

        networkHandler.sendPacket(packet)

        // Update visual rotation (player model head)
        player.yaw = yaw
        player.pitch = pitch

        // Reset back to original rotation in next tick
        mc.execute {
            player.yaw = originalYaw
            player.pitch = originalPitch
        }
    }

    /**
     * Attack the target entity
     */
    private fun attackTarget(player: ClientPlayerEntity, target: Entity) {
        val interactionManager = mc.interactionManager ?: return

        try {
            // Check attack cooldown
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastAttackTime < 50) { // Limit to 20 attacks per second max
                return
            }

            // Attack entity
            interactionManager.attackEntity(player, target)
            player.swingHand(Hand.MAIN_HAND)
            lastAttackTime = currentTime

            if (Config.developerMode && Config.debugMessages) {
                UChat.mChat("§aKillAura attacked: ${target.type.toString().split(".").last()}")
            }
        } catch (e: Exception) {
            UChat.mChat("§cKillAura attack error: ${e.message}")
        }
    }
}