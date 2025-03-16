package com.dov.cm.util

import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class RotationUtil {
    private val mc = net.minecraft.client.MinecraftClient.getInstance()

    fun calculateRotationToTarget(
        playerPos: Vec3d,
        targetPos: Vec3d,
        hitboxMode: Int = 0
    ): Rotation {
        // Adjust target position based on hitbox mode
        val adjustedTargetPos = when (hitboxMode) {
            0 -> targetPos // Eye level
            1 -> targetPos.add(0.0, targetPos.y / 2, 0.0) // Center
            else -> targetPos.add(0.0, targetPos.y, 0.0) // Bottom
        }

        // Calculate differences
        val dx = adjustedTargetPos.x - playerPos.x
        val dy = adjustedTargetPos.y - playerPos.y
        val dz = adjustedTargetPos.z - playerPos.z

        // Calculate yaw
        val yaw = Math.toDegrees(atan2(dz, dx)).toFloat() - 90f

        // Calculate pitch
        val horizontalDistance = sqrt(dx * dx + dz * dz)
        val pitch = -Math.toDegrees(atan2(dy, horizontalDistance)).toFloat()

        return Rotation(yaw, pitch)
    }

    fun smoothRotation(
        currentRotation: Rotation,
        targetRotation: Rotation,
        smoothingFactor: Float,
        randomValue: Float = 0f,
        mode: Int = 0
    ): Rotation {
        // Normalize yaw difference
        var yawDifference = targetRotation.yaw - currentRotation.yaw
        if (yawDifference > 180f) yawDifference -= 360f
        if (yawDifference < -180f) yawDifference += 360f

        // Normalize pitch difference
        var pitchDifference = targetRotation.pitch - currentRotation.pitch

        return when (mode) {
            2 -> { // Vertical only
                val smoothedPitch = currentRotation.pitch + (pitchDifference * smoothingFactor) + (randomValue / 2)
                Rotation(currentRotation.yaw, smoothedPitch)
            }
            1 -> { // Horizontal only
                val smoothedYaw = currentRotation.yaw + (yawDifference * smoothingFactor) + randomValue
                Rotation(smoothedYaw, currentRotation.pitch)
            }
            else -> { // Both
                val smoothedYaw = currentRotation.yaw + (yawDifference * smoothingFactor) + randomValue
                val smoothedPitch = currentRotation.pitch + (pitchDifference * smoothingFactor) + (randomValue / 2)
                Rotation(smoothedYaw, smoothedPitch)
            }
        }
    }

    fun applyRotation(rotation: Rotation) {
        mc.player?.let { player ->
            player.yaw = rotation.yaw
            player.pitch = rotation.pitch
        }
    }

    fun isInFOV(
        currentRotation: Rotation,
        targetRotation: Rotation,
        fovAngle: Float
    ): Boolean {
        return abs(targetRotation.yaw - currentRotation.yaw) < fovAngle &&
                abs(targetRotation.pitch - currentRotation.pitch) < fovAngle
    }

    companion object {
        val INSTANCE: RotationUtil = RotationUtil()
    }
}

