package com.logicalgeekboy.logical_zoom.java_util.impl;

import com.logicalgeekboy.logical_zoom.java_util.MC;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.RaycastContext;
import com.logicalgeekboy.logical_zoom.skid;

public class RotationUtil implements MC {
    private static boolean DEBUG = true;
    public static RotationUtil INSTANCE = new RotationUtil();

    private static void debug(String message) {
        if (DEBUG) {
            System.out.println("[MathUtil Debug] " + message);
        }
    }

    public Rotation getNeededRotations(float f, float f2, float f3, float f4, float f5, float f6) {
        double d = f - f4;
        double d2 = f2 - f5;
        double d3 = f3 - f6;
        float[] fArray = new float[2];
        fArray[0] = getMc().player.getYaw();
        fArray[1] = getMc().player.getPitch();
        return new Rotation(getMc().player.getYaw() + wrap((float)Math.toDegrees(Math.atan2(d3, d)) - 90 - getMc().player.getYaw()), fArray[1] + wrap(-((float)Math.toDegrees(Math.atan2(d2, Math.sqrt(d * d + d3 * d3)))) - fArray[1]));
    }

    public Rotation getNeededRotations(float f, float f2, float f3) {
        Vec3d vec3d = getMc().player.getEyePos();
        double d = f - vec3d.x;
        double d2 = f2 - vec3d.y;
        double d3 = f3 - vec3d.z;
        float[] fArray = new float[2];
        fArray[0] = getMc().player.getYaw();
        fArray[1] = getMc().player.getPitch();
        return new Rotation(fArray[0] + wrap((float)Math.toDegrees(Math.atan2(d3, d)) - 90 - fArray[0]), fArray[1] + wrap((float)(-Math.toDegrees(Math.atan2(d2, Math.sqrt(d * d + d3 * d3)))) - fArray[1]));
    }

    public BlockHitResult blockRaycast(BlockPos blockPos) {
        return getMc().world.raycastBlock(getMc().player.getEyePos(), getMc().player.getEyePos().add(getPlayerLookVec(getMc().player).multiply(6)), blockPos, VoxelShapes.fullCube(), getMc().world.getBlockState(blockPos));
    }

    public HitResult playerRaycast(Rotation rotation) {
        return getMc().world.raycast(new RaycastContext(getMc().player.getEyePos(), getPlayerLookVec(rotation).multiply(6).add(getMc().player.getEyePos()), RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, getMc().player));
    }

    public Vec3d getPlayerLookVec(Rotation rotation) {
        float f = (float)Math.PI / 180;
        float f2 = (float)Math.PI;
        float f3 = -MathHelper.cos(-rotation.getPitch() * f);
        return new Vec3d(MathHelper.sin(-rotation.getYaw() * f - f2) * f3, MathHelper.sin(-rotation.getPitch() * f), MathHelper.cos(-rotation.getYaw() * f - f2) * f3).normalize();
    }

    public boolean setPitch(Rotation rotation, float f, float f2, float f3) {


        float msValue = skid.Companion.getRenderManager().getMs();
        float randomFactor = RandomUtil.INSTANCE.randomInRange(0.5f, 1);
        float interpolationFactor = msValue * (1.1f - f) * 5 / f3 * randomFactor;



        setRotation(new Rotation(getMc().player.getYaw(),
                MathUtil.interpolate(
                        getMc().player.getPitch(),
                        rotation.getPitch() + f2 / 2,
                        interpolationFactor
                )
        ));


        boolean success = getMc().player.getPitch() - 5 < rotation.getPitch() && getMc().player.getPitch() + 5 > rotation.getPitch();


        return success;
    }

    Vec3d getPlayerLookVec(PlayerEntity playerEntity) {
        float f = (float)Math.PI / 180;
        float f2 = (float)Math.PI;
        float f3 = -MathHelper.cos(-playerEntity.getPitch() * f);
        return new Vec3d(MathHelper.sin(-playerEntity.getYaw() * f - f2) * f3, MathHelper.sin(-playerEntity.getPitch() * f), MathHelper.cos(-playerEntity.getYaw() * f - f2) * f3).normalize();
    }

    public void setRotation(Rotation rotation) {
        getMc().player.setYaw(rotation.getYaw());
        getMc().player.setPitch(rotation.getPitch());

    }

    public BlockHitResult blockRaycast(BlockPos blockPos, PlayerEntity playerEntity) {
        return getMc().world.raycastBlock(playerEntity.getEyePos(), getPlayerLookVec(playerEntity).multiply(6).add(playerEntity.getEyePos()), blockPos, VoxelShapes.fullCube(), getMc().world.getBlockState(blockPos));
    }

    public BlockHitResult blockRaycastRotation(BlockPos blockPos, Rotation rotation) {
        return getMc().world.raycastBlock(getMc().player.getEyePos(), getPlayerLookVec(rotation).multiply(6), blockPos, VoxelShapes.fullCube(), getMc().world.getBlockState(blockPos));
    }

    float wrap(float f) {
        float f2 = f;
        if ((f2 %= 360) >= 180) {
            f2 -= 360;
        }
        if (f2 < -180) {
            f2 += 360;
        }
        return f2;
    }

    public Rotation getNeededRotations(Vec3d vec3d) {
        return this.getNeededRotations((float)vec3d.x, (float)vec3d.y, (float)vec3d.z);
    }

    public boolean setYaw(Rotation rotation, float smoothing, float randomValue, float divider) {
        debug("setYaw called: targetYaw=" + rotation.getYaw() +
                ", currentYaw=" + getMc().player.getYaw() +
                ", difference=" + Math.abs(rotation.getYaw() - getMc().player.getYaw()) +
                ", smoothing=" + smoothing + ", random=" + randomValue + ", divider=" + divider);

        float msValue = skid.Companion.getRenderManager().getMs();
        debug("RenderManager.getMs() value: " + msValue);

        float randomFactor = RandomUtil.INSTANCE.randomInRange(0.5f, 1);
        float interpolationFactor = msValue * (1.1f - smoothing) * 5 / divider * randomFactor;

        // Ensure we have at least some minimal movement
        if (interpolationFactor < 0.01f) {
            interpolationFactor = 0.01f;
            debug("Interpolation factor too small, using minimum value: 0.01");
        }

        debug("Interpolation values: randomFactor=" + randomFactor +
                ", interpolationFactor=" + interpolationFactor);

        // Get original yaw
        float originalYaw = getMc().player.getYaw();

        // FIXED: Only change yaw, not pitch - keep current pitch
        float newYaw = MathUtil.interpolate(
                getMc().player.getYaw(),
                rotation.getYaw() + randomValue / 2,
                interpolationFactor
        );

        debug("Interpolated yaw: " + newYaw + " (from " + getMc().player.getYaw() +
                " towards " + (rotation.getYaw() + randomValue / 2) + ")");

        // Only update yaw, keep the pitch the same
        setRotation(new Rotation(newYaw, getMc().player.getPitch()));

        // Check if we made any progress (any movement is success)
        float movement = Math.abs(getMc().player.getYaw() - originalYaw);
        boolean madeProgress = movement > 0.01f;

        // Check if we're close enough to target (within 5 degrees)
        boolean reachedTarget = Math.abs(getMc().player.getYaw() - rotation.getYaw()) < 5;

        debug("Yaw movement: " + movement + " degrees");
        debug("Progress made: " + madeProgress + ", reached target: " + reachedTarget);
        debug("Distance to target: " + Math.abs(getMc().player.getYaw() - rotation.getYaw()) + " degrees");

        // Consider it a success if we either made progress or reached the target
        return madeProgress || reachedTarget;
    }

    public boolean setRotation(Rotation rotation, float f, float f2, float f3) {


        float msValue = skid.Companion.getRenderManager().getMs();
        float randomFactor = RandomUtil.INSTANCE.randomInRange(0.5f, 1);
        float interpolationFactor = msValue * (1.1f - f) * 5 / f3 * randomFactor;


        float f4 = MathUtil.interpolate(getMc().player.getYaw(), rotation.getYaw() + f2, interpolationFactor);
        float f5 = MathUtil.interpolate(getMc().player.getPitch(), rotation.getPitch() + f2 / 2, interpolationFactor);


        setRotation(new Rotation(f4, f5));

        boolean success = Math.abs(getMc().player.getYaw() - rotation.getYaw()) < 5 &&
                Math.abs(getMc().player.getPitch() - rotation.getPitch()) < 5;



        return success;
    }

    public boolean inRange(Rotation rotation, Rotation rotation2, float f) {
        return Math.abs(rotation2.getYaw() - rotation.getYaw()) < f && Math.abs(rotation2.getPitch() - rotation.getPitch()) < f;
    }

    public void setRotation(float f, float f2) {
        this.setRotation(new Rotation(f, f2));
    }

    @Override
    public MinecraftClient getMc() {
        return MinecraftClient.getInstance();
    }
}