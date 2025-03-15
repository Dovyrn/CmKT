package com.dov.cm.util

import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

/**
 * Represents a position within a region of the Minecraft world.
 * A region is a larger unit than chunks.
 */
class RegionPos(val x: Int, val y: Int, val z: Int) {

    /**
     * Converts this region position to a Vec3d.
     */
    fun toVec3d(): Vec3d {
        return Vec3d(x.toDouble(), y.toDouble(), z.toDouble())
    }

    companion object {
        /**
         * Creates a RegionPos from a BlockPos.
         */
        fun fromBlockPos(pos: BlockPos): RegionPos {
            // This calculation depends on how you define regions
            // For simplicity, using direct mapping here
            return RegionPos(pos.x, pos.y, pos.z)
        }
    }
}