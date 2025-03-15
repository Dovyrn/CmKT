package com.dov.cm.util

import com.dov.cm.managers.RenderManager
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.RaycastContext

class RotationUtil  {

    fun getNeededRotations(f: Float, f2: Float, f3: Float, f4: Float, f5: Float, f6: Float): Rotation {
        val d = (f - f4).toDouble()
        val d2 = (f2 - f5).toDouble()
        val d3 = (f3 - f6).toDouble()
        val fArray = FloatArray(2)
        fArray[0] = getMc().player!!.yaw
        fArray[1] = getMc().player!!.pitch
        return Rotation(
            getMc().player!!.yaw + wrap(Math.toDegrees(Math.atan2(d3, d)).toFloat() - 90 - getMc().player!!.yaw),
            fArray[1] + wrap(-Math.toDegrees(Math.atan2(d2, Math.sqrt(d * d + d3 * d3))).toFloat() - fArray[1])
        )
    }

    fun getNeededRotations(f: Float, f2: Float, f3: Float): Rotation {
        val vec3d = getMc().player!!.eyePos
        val d = f - vec3d.x
        val d2 = f2 - vec3d.y
        val d3 = f3 - vec3d.z
        val fArray = FloatArray(2)
        fArray[0] = getMc().player!!.yaw
        fArray[1] = getMc().player!!.pitch
        return Rotation(
            fArray[0] + wrap(Math.toDegrees(Math.atan2(d3, d)).toFloat() - 90 - fArray[0]),
            fArray[1] + wrap((-Math.toDegrees(Math.atan2(d2, Math.sqrt(d * d + d3 * d3)))).toFloat() - fArray[1])
        )
    }

    fun blockRaycast(blockPos: BlockPos): BlockHitResult? {
        return getMc().world!!.raycastBlock(
            getMc().player!!.eyePos,
            getMc().player!!.eyePos.add(getPlayerLookVec(getMc().player!!).multiply(6.0)),
            blockPos,
            VoxelShapes.fullCube(),
            getMc().world!!.getBlockState(blockPos)
        )
    }

    fun playerRaycast(rotation: Rotation): HitResult {
        return getMc().world!!.raycast(
            RaycastContext(
                getMc().player!!.eyePos,
                getPlayerLookVec(rotation).multiply(6.0).add(getMc().player!!.eyePos),
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                getMc().player
            )
        )
    }

    fun getPlayerLookVec(rotation: Rotation): Vec3d {
        val f = Math.PI.toFloat() / 180
        val f2 = Math.PI.toFloat()
        val f3 = -MathHelper.cos(-rotation.pitch * f)
        return Vec3d(
            (MathHelper.sin(-rotation.yaw * f - f2) * f3).toDouble(),
            MathHelper.sin(-rotation.pitch * f).toDouble(),
            (MathHelper.cos(-rotation.yaw * f - f2) * f3).toDouble()
        ).normalize()
    }

    fun setPitch(rotation: Rotation, f: Float, f2: Float, f3: Float): Boolean {
        setRotation(
            Rotation(
                getMc().player!!.yaw,
                MathUtil.interpolate(
                    getMc().player!!.pitch,
                    rotation.pitch + f2 / 2,
                    RenderManager.getInstance().getMs() * (1.1f - f) * 5 / f3 * RandomUtil.INSTANCE.randomInRange(0.5f, 1f)
                )
            )
        )
        return getMc().player!!.pitch - 5 < rotation.pitch && getMc().player!!.pitch + 5 > rotation.pitch
    }

    private fun getPlayerLookVec(playerEntity: PlayerEntity): Vec3d {
        val f = Math.PI.toFloat() / 180
        val f2 = Math.PI.toFloat()
        val f3 = -MathHelper.cos(-playerEntity.pitch * f)
        return Vec3d(
            (MathHelper.sin(-playerEntity.yaw * f - f2) * f3).toDouble(),
            MathHelper.sin(-playerEntity.pitch * f).toDouble(),
            (MathHelper.cos(-playerEntity.yaw * f - f2) * f3).toDouble()
        ).normalize()
    }

    fun setRotation(rotation: Rotation) {
        getMc().player!!.yaw = rotation.yaw
        getMc().player!!.pitch = rotation.pitch
    }

    fun blockRaycast(blockPos: BlockPos, playerEntity: PlayerEntity): BlockHitResult? {
        return getMc().world!!.raycastBlock(
            playerEntity.eyePos,
            getPlayerLookVec(playerEntity).multiply(6.0).add(playerEntity.eyePos),
            blockPos,
            VoxelShapes.fullCube(),
            getMc().world!!.getBlockState(blockPos)
        )
    }

    fun blockRaycastRotation(blockPos: BlockPos, rotation: Rotation): BlockHitResult? {
        return getMc().world!!.raycastBlock(
            getMc().player!!.eyePos,
            getPlayerLookVec(rotation).multiply(6.0),
            blockPos,
            VoxelShapes.fullCube(),
            getMc().world!!.getBlockState(blockPos)
        )
    }

    private fun wrap(f: Float): Float {
        var f2 = f
        f2 %= 360f
        if (f2 >= 180) {
            f2 -= 360f
        }
        if (f2 < -180) {
            f2 += 360f
        }
        return f2
    }

    fun getNeededRotations(vec3d: Vec3d): Rotation {
        return getNeededRotations(vec3d.x.toFloat(), vec3d.y.toFloat(), vec3d.z.toFloat())
    }

    fun setYaw(rotation: Rotation, f: Float, f2: Float, f3: Float): Boolean {
        val f4 = MathUtil.interpolate(
            getMc().player!!.yaw,
            rotation.yaw + f2 / 2,
            RenderManager.getInstance().getMs() * (1.1f - f) * 5 / f3 * RandomUtil.INSTANCE.randomInRange(0.5f, 1f)
        )
        val f5 = MathUtil.interpolate(
            getMc().player!!.pitch,
            rotation.pitch + f2 / 2,
            RenderManager.getInstance().getMs() * (1.1f - f) * 5 / f3 * RandomUtil.INSTANCE.randomInRange(0.5f, 1f)
        )
        setRotation(Rotation(f4, f5))
        return getMc().player!!.yaw - 5 < rotation.yaw && getMc().player!!.yaw + 5 > rotation.yaw
    }

    fun setRotation(rotation: Rotation, f: Float, f2: Float, f3: Float): Boolean {
        val f4 = MathUtil.interpolate(
            getMc().player!!.yaw,
            rotation.yaw + f2,
            RenderManager.getInstance().getMs() * (1.1f - f) * 5 / f3 * RandomUtil.INSTANCE.randomInRange(0.5f, 1f)
        )
        val f5 = MathUtil.interpolate(
            getMc().player!!.pitch,
            rotation.pitch + f2 / 2,
            RenderManager.getInstance().getMs() * (1.1f - f) * 5 / f3 * RandomUtil.INSTANCE.randomInRange(0.5f, 1f)
        )
        setRotation(Rotation(f4, f5))
        return getMc().player!!.yaw - 5 < rotation.yaw && getMc().player!!.yaw + 5 > rotation.yaw &&
                getMc().player!!.pitch - 5 < rotation.pitch && getMc().player!!.pitch + 5 > rotation.pitch
    }

    fun inRange(rotation: Rotation, rotation2: Rotation, f: Float): Boolean {
        return Math.abs(rotation2.yaw - rotation.yaw) < f && Math.abs(rotation2.pitch - rotation.pitch) < f
    }

    fun setRotation(f: Float, f2: Float) {
        setRotation(Rotation(f, f2))
    }

    fun getMc(): MinecraftClient {
        return MinecraftClient.getInstance()
    }

    companion object {
        val INSTANCE: RotationUtil = RotationUtil()
    }
}