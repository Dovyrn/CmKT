package com.dov.cm.util

import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d

class Rotation(val pitch: Float, val yaw: Float) {

    fun asLookVec(): Vec3d {
        val radiansPerDegree = MathHelper.RADIANS_PER_DEGREE
        val pi = MathHelper.PI

        val newPitch = -MathHelper.wrapDegrees(pitch) * radiansPerDegree
        val cosPitch = -MathHelper.cos(newPitch)
        val sinPitch = MathHelper.sin(newPitch)

        val newYaw = -MathHelper.wrapDegrees(yaw) * radiansPerDegree - pi
        val cosYaw = MathHelper.cos(newYaw)
        val sinYaw = MathHelper.sin(newYaw)

        return Vec3d(
            sinYaw * cosPitch.toDouble(),
            sinPitch.toDouble(),
            cosYaw * cosPitch.toDouble()
        )
    }

    // Optional: Override equals and hashCode for proper comparison
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Rotation

        return pitch == other.pitch && yaw == other.yaw
    }

    override fun hashCode(): Int {
        var result = pitch.hashCode()
        result = 31 * result + yaw.hashCode()
        return result
    }

    // Optional: toString for easier debugging
    override fun toString(): String {
        return "Rotation(pitch=$pitch, yaw=$yaw)"
    }
}