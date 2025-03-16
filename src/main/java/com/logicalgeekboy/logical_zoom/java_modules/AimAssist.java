package com.logicalgeekboy.logical_zoom.java_modules;

import com.dov.cm.config.Config;




import com.dov.cm.util.TimerUtil;
import com.logicalgeekboy.logical_zoom.java_event.EventListener;
import com.logicalgeekboy.logical_zoom.java_event.impl.Render2DEvent;
import com.logicalgeekboy.logical_zoom.java_util.impl.RandomUtil;
import com.logicalgeekboy.logical_zoom.java_util.impl.Rotation;
import com.logicalgeekboy.logical_zoom.java_util.impl.RotationUtil;
import com.logicalgeekboy.logical_zoom.skid;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class AimAssist {

    public boolean targetCrystal;
    public boolean targetPlayer;
    public int mode;
    public int visibleTime;
    public float smoothing;
    public float fov;
    public float range;
    public float random;
    public int hitbox;
    public boolean weaponOnly;
    public boolean oneTarget;
    public TimerUtil randomTimer;
    public TimerUtil visibleTimer;
    public float randomValue;
    public Entity target;
    private Config config = Config.INSTANCE;




    private MinecraftClient getMc(){
        return MinecraftClient.getInstance();
    }


    public AimAssist() {
        targetCrystal = config.getAimAssistTargetCrystals();
        targetPlayer = config.getAimAssistTargetPlayers();
        mode = config.getAimAssistMode();
        visibleTime = config.getAimAssistVisibleTime();
        smoothing = config.getAimAssistSmoothing();
        fov = config.getAimAssistFOV();
        range = config.getAimAssistRange();
        random = config.getAimAssistRandom();
        hitbox = config.getAimAssistHitbox();
        weaponOnly = config.getAimAssistWeaponOnly();
        oneTarget = config.getAimAssistStickyTarget();
        randomTimer = new TimerUtil();
        visibleTimer = new TimerUtil();
    }

    @EventListener
    public void event(Render2DEvent event) {
        if (getMc().currentScreen != null || !getMc().isWindowFocused()) {
            return;
        }
        if (getMc().crosshairTarget != null) {
            HitResult hitResult = getMc().crosshairTarget;
            if (hitResult.getType() == HitResult.Type.ENTITY) {
                return;
            }
        }
        if (weaponOnly) {
            if (!(getMc().player.getMainHandStack().getItem() instanceof SwordItem) && !(getMc().player.getMainHandStack().getItem() instanceof AxeItem)) {
                return;
            }
        }
        Rotation rotation = new Rotation(getMc().player.getYaw(), getMc().player.getPitch());
        if (target != null) {
            Vec3d vec3d = target.getPos();
            ClientPlayerEntity clientPlayerEntity3 = getMc().player;
            if (Math.sqrt(clientPlayerEntity3.squaredDistanceTo(vec3d.x, vec3d.y, vec3d.z)) > range) {
                target = null;
            }
            if (!RotationUtil.INSTANCE.inRange(rotation, RotationUtil.INSTANCE.getNeededRotations((float) vec3d.x, (float) (vec3d.y - getHeight(target.getHeight())), (float) vec3d.z), fov)) {
                target = null;
            }
        }
        if (!oneTarget || target == null) {
            target = getTarget(rotation);
        }
        if (target == null) {
            visibleTimer.reset();
            return;
        }
        Vec3d vec3d = target.getEyePos();
        double d = vec3d.y - getHeight(target.getHeight());
        BlockHitResult raycast = getMc().world.raycast(new RaycastContext(getMc().player.getCameraPosVec(getMc().getRenderTickCounter().getTickDelta(true)), new Vec3d(vec3d.x, d, vec3d.z), RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.ANY, getMc().player));
        if (raycast.getType() == HitResult.Type.BLOCK) {
            visibleTimer.reset();
            return;
        }
        if (!randomTimer.delay(visibleTime)) {
            return;
        }
        Rotation rotation3 = RotationUtil.INSTANCE.getNeededRotations((float)vec3d.x, (float)d, (float)vec3d.z);
        if (randomTimer.delay(1000 * smoothing)) {
            randomValue = random > 0 ? -(random / 2) + RandomUtil.INSTANCE.getRandom().nextFloat() * random : 0;
            randomTimer.reset();
        }
        switch (mode) {
            case 2 -> RotationUtil.INSTANCE.setPitch(rotation3, smoothing, randomValue, 2);
            case 1 -> {}
            case 0 -> RotationUtil.INSTANCE.setRotation(rotation3, smoothing, randomValue, 2);
        }
    }

    private Entity getTarget(Rotation rotation) {
        Entity entity = null;
        double d = Double.MAX_VALUE;
        for (Entity entity2 : getMc().world.getEntities()) {
            if (isEntityValid(entity2) && skid.Companion.getAntiBotManager().isNotBot(entity2) && (entity2 instanceof PlayerEntity player)) {
                Vec3d vec3d = entity2.getEyePos();
                double d2 = vec3d.y - getHeight(entity2.getHeight());
                double d3 = getMc().player.squaredDistanceTo(vec3d.x, d2, vec3d.z);
                if (d3 < d && Math.sqrt(getMc().player.squaredDistanceTo(vec3d.x, vec3d.y, vec3d.z)) <= range && RotationUtil.INSTANCE.inRange(rotation, RotationUtil.INSTANCE.getNeededRotations((float) vec3d.x, (float) d2, (float) vec3d.z), fov)) {
                    entity = entity2;
                    d = d3;
                }
            }
        }
        return entity;
    }

    boolean isEntityValid(Entity entity) {
        if (!(entity instanceof EndCrystalEntity) && !(entity instanceof PlayerEntity)) {
            return false;
        }
        if (entity == getMc().player || entity instanceof PlayerEntity && (targetPlayer || !skid.Companion.getAntiBotManager().isNotBot(entity))) {
            return false;
        }
        return !(entity instanceof EndCrystalEntity) || targetCrystal;
    }

    private float getHeight(float f) {
        return hitbox == 0 ? 0 : (hitbox == 1 ? f / 2 : f);
    }


}
