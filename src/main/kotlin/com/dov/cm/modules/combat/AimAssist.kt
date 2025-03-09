package com.dov.cm.modules.combat

import com.dov.cm.config.Config
import com.dov.cm.modules.UChat
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.Entity
import net.minecraft.entity.decoration.EndCrystalEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.AxeItem
import net.minecraft.item.SwordItem
import net.minecraft.item.MaceItem
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext
import kotlin.math.sqrt

/**
 * AimAssist module - Helps with aiming at targets with smoother, time-based rotation
 */
object AimAssist {
    private val mc: MinecraftClient = MinecraftClient.getInstance()

    // Target tracking
    private var target: Entity? = null
    private var randomValue: Float = 0f

    // Timing and smoothing utilities
    private val randomTimer = TimerUtil()
    private val visibleTimer = TimerUtil()
    private val rotationTimer = TimerUtil()

    // Smooth rotation tracking
    private var targetRotation: Rotation? = null
    private var currentRotation: Rotation? = null
    private var rotationStartTime: Long = 0
    private var rotationDuration: Long = 0
    var aimAssistStopOnEdge: Boolean = Config.stopOnEdge


    /**
     * Initialize the AimAssist module
     */
    fun init() {
        // Use ClientTickEvents instead of HudRenderCallback to avoid potential null issues
        ClientTickEvents.END_CLIENT_TICK.register { _ ->
            if (Config.aimAssistEnabled) {
                try {
                    onTick()
                } catch (e: Exception) {
                    // Prevent crashes
                }
            }
        }

        UChat.mChat("AimAssist module initialized")
    }

    /**
     * Main handler for aim assist - handles the core logic
     */
    private fun onTick() {
        // Skip if screen is open or window not focused
        if (mc.currentScreen != null || !mc.isWindowFocused) {
            return
        }

        // Safety check for player and world
        val player = mc.player ?: return
        val world = mc.world ?: return

        // Update smooth rotation
        updateRotation()

        // Skip if already looking at an entity
        if (mc.crosshairTarget?.type == HitResult.Type.ENTITY && aimAssistStopOnEdge) {
            return
        }

        // Skip if weapon-only is enabled and not holding a weapon
        if (Config.aimAssistWeaponOnly) {
            val mainHandItem = player.mainHandStack.item
            if (mainHandItem !is SwordItem && mainHandItem !is AxeItem && mainHandItem !is MaceItem) {
                return
            }
        }

        // Get current rotation
        val rotation = Rotation(player.yaw, player.pitch)

        // Check if current target is still valid
        if (target != null) {
            // Make sure target still exists and is loaded
            if (!target!!.isAlive) {
                target = null
                resetRotation()
            } else {
                val pos = target!!.pos

                // Check distance
                if (sqrt(player.squaredDistanceTo(pos)) > Config.aimAssistRange) {
                    target = null
                    resetRotation()
                } else {
                    // Check if target is still in FOV
                    val height = getHeight(target!!.height)

                    val neededRot = getNeededRotations(
                        pos.x.toFloat(),
                        (pos.y - height).toFloat(),
                        pos.z.toFloat()
                    )

                    if (!inRange(rotation, neededRot, Config.aimAssistFOV.toFloat())) {
                        target = null
                        resetRotation()
                    }
                }
            }
        }

        // Find new target if needed
        if (!Config.aimAssistStickyTarget || target == null) {
            target = getTarget(rotation)
        }

        // If no target, reset and return
        if (target == null) {
            visibleTimer.reset()
            resetRotation()
            return
        }

        // Safety check that target still exists
        if (!target!!.isAlive) {
            target = null
            visibleTimer.reset()
            resetRotation()
            return
        }

        // Get eye position and adjust for hitbox setting
        val eyePos = target!!.eyePos
        val height = getHeight(target!!.height)
        val targetY = eyePos.y - height

        // Check visibility with raycast
        val raycast = world.raycast(RaycastContext(
            player.getCameraPosVec(mc.renderTickCounter.getTickDelta(true)),
            Vec3d(eyePos.x, targetY, eyePos.z),
            RaycastContext.ShapeType.OUTLINE,
            RaycastContext.FluidHandling.ANY,
            player
        ))

        // If target is behind a block, reset and return
        if (raycast.type == HitResult.Type.BLOCK) {
            visibleTimer.reset()
            resetRotation()
            return
        }

        // Check if target has been visible long enough
        if (!visibleTimer.delay(Config.aimAssistVisibleTime.toFloat())) {
            return
        }

        // Calculate needed rotation to aim at target
        val neededRotation = getNeededRotations(
            eyePos.x.toFloat(),
            targetY.toFloat(),
            eyePos.z.toFloat()
        )

        // Update randomness value periodically
        if (randomTimer.delay(1000 * Config.aimAssistSmoothing)) {
            randomValue = if (Config.aimAssistRandom > 0) {
                -(Config.aimAssistRandom / 2) + (Math.random().toFloat() * Config.aimAssistRandom)
            } else {
                0f
            }
            randomTimer.reset()
        }

        // Determine rotation mode
        val finalRotation = when (Config.aimAssistMode) {
            0 -> neededRotation // Both axes
            1 -> Rotation(neededRotation.yaw, player.pitch) // Horizontal only
            2 -> Rotation(player.yaw, neededRotation.pitch) // Vertical only
            else -> neededRotation
        }

        // Smooth rotation handling
        if (rotationTimer.delay(50f)) { // Approximately 20 times per second
            // Initiate smooth rotation
            smoothRotate(finalRotation, Config.aimAssistSmoothing)
            rotationTimer.reset()
        }
    }

    /**
     * Find the best target based on distance and FOV
     */
    private fun getTarget(rotation: Rotation): Entity? {
        val world = mc.world ?: return null
        val player = mc.player ?: return null

        var bestTarget: Entity? = null
        var closestDistance = Double.MAX_VALUE

        world.entities.forEach { entity ->
            if (isEntityValid(entity)) {
                try {
                    val eyePos = entity.eyePos
                    val height = getHeight(entity.height)
                    val targetY = eyePos.y - height

                    val distanceSq = player.squaredDistanceTo(eyePos.x, targetY, eyePos.z)
                    val distance = sqrt(player.squaredDistanceTo(eyePos))

                    if (distanceSq < closestDistance &&
                        distance <= Config.aimAssistRange) {

                        // Check FOV
                        val neededRot = getNeededRotations(
                            eyePos.x.toFloat(),
                            targetY.toFloat(),
                            eyePos.z.toFloat()
                        )

                        if (inRange(rotation, neededRot, Config.aimAssistFOV.toFloat())) {
                            bestTarget = entity
                            closestDistance = distanceSq
                        }
                    }
                } catch (e: Exception) {
                    // Skip any entities that cause problems
                }
            }
        }

        return bestTarget
    }

    /**
     * Check if an entity is a valid target
     */
    private fun isEntityValid(entity: Entity): Boolean {
        if (!entity.isAlive) {
            return false
        }

        // Only target players and crystals
        if (entity !is EndCrystalEntity && entity !is PlayerEntity) {
            return false
        }

        // Don't target self
        if (entity == mc.player) {
            return false
        }

        // Check target settings
        if (entity is PlayerEntity && !Config.aimAssistTargetPlayers) {
            return false
        }

        if (entity is EndCrystalEntity && !Config.aimAssistTargetCrystals) {
            return false
        }

        return true
    }

    /**
     * Get the height offset based on hitbox setting
     */
    private fun getHeight(height: Float): Float {
        return when (Config.aimAssistHitbox) {
            0 -> 0f            // Eye
            1 -> height / 2    // Center
            2 -> height        // Bottom
            else -> 0f
        }
    }

    /**
     * Calculate the rotation needed to aim at a position
     */
    private fun getNeededRotations(x: Float, y: Float, z: Float): Rotation {
        val player = mc.player ?: return Rotation(0f, 0f)
        val eyePos = player.eyePos

        val deltaX = x - eyePos.x.toFloat()
        val deltaY = y - eyePos.y.toFloat()
        val deltaZ = z - eyePos.z.toFloat()

        val horizontalDistance = sqrt(deltaX * deltaX + deltaZ * deltaZ)

        val yaw = MathHelper.wrapDegrees(
            Math.toDegrees(Math.atan2(deltaZ.toDouble(), deltaX.toDouble())).toFloat() - 90f
        )
        val pitch = MathHelper.wrapDegrees(
            -Math.toDegrees(Math.atan2(deltaY.toDouble(), horizontalDistance.toDouble())).toFloat()
        )

        return Rotation(yaw, pitch)
    }

    /**
     * Check if a rotation is within FOV
     */
    private fun inRange(current: Rotation, needed: Rotation, fov: Float): Boolean {
        return Math.abs(wrap(needed.yaw - current.yaw)) < fov &&
                Math.abs(wrap(needed.pitch - current.pitch)) < fov
    }

    /**
     * Wrap angle to -180..180 range
     */
    private fun wrap(value: Float): Float {
        var wrapped = value % 360f
        if (wrapped >= 180f) wrapped -= 360f
        if (wrapped < -180f) wrapped += 360f
        return wrapped
    }

    /**
     * Smoothly rotate towards the target rotation
     */


    /**
     * Smoothly rotate towards the target rotation
     */
    private fun smoothRotate(targetRot: Rotation, smoothing: Float) {
        val player = mc.player ?: return

        GlobalScope.launch {
            repeat(50) { // Perform the rotation 50 times
                val currentYaw = player.yaw
                val currentPitch = player.pitch

                // Calculate maximum rotation change per step
                val maxYawChange = (0.75f * (1f - smoothing / 1.1f)) / 2.5f
                val maxPitchChange = (0.3f * (1f - smoothing / 1.1f)) / 2.5f

                // Calculate needed rotation changes
                val yawDiff = wrap(targetRot.yaw - currentYaw)
                val pitchDiff = wrap(targetRot.pitch - currentPitch)

                // Add vertical precision check
                // Only rotate horizontally if vertical aim is very close to target
                val shouldRotateHorizontally = Math.abs(pitchDiff) < 5f

                // Limit rotation change
                val adjustedYawChange = if (shouldRotateHorizontally) {
                    when {
                        Math.abs(yawDiff) <= maxYawChange -> yawDiff
                        yawDiff > 0 -> maxYawChange
                        else -> -maxYawChange
                    }
                } else {
                    0f
                }

                val adjustedPitchChange = when {
                    Math.abs(pitchDiff) <= maxPitchChange -> pitchDiff
                    pitchDiff > 0 -> maxPitchChange
                    else -> -maxPitchChange
                }

                // Apply rotation
                player.yaw = currentYaw + adjustedYawChange
                player.pitch = MathHelper.clamp(currentPitch + adjustedPitchChange, -90f, 90f)

                // Check if we're close enough to the target rotation
                if (Math.abs(yawDiff) <= 0.1f && Math.abs(pitchDiff) <= 0.1f) {
                    // Directly set to target if very close
                    player.yaw = targetRot.yaw
                    player.pitch = targetRot.pitch
                    resetRotation()
                    return@launch // Stop early if we're at the target
                }

                delay(1L) // Wait 1ms before next step
            }
        }
    }


    /**
     * Apply interpolated rotation each tick
     */
    private fun updateRotation() {
        // This method is now a no-op as rotation is handled directly in smoothRotate
    }

    /**
     * Reset rotation tracking
     */
    private fun resetRotation() {
        targetRotation = null
        currentRotation = null
    }

    /**
     * Interpolate between two values
     */
    private fun interpolate(start: Float, end: Float, delta: Float): Float {
        return start + (end - start) * delta
    }

    /**
     * Simple timer utility
     */
    class TimerUtil {
        private var lastMs = System.currentTimeMillis()

        fun reset() {
            lastMs = System.currentTimeMillis()
        }

        fun delay(ms: Float): Boolean {
            return System.currentTimeMillis() - lastMs > ms
        }
    }

    /**
     * Simple rotation class
     */
    class Rotation(var yaw: Float, var pitch: Float)
}