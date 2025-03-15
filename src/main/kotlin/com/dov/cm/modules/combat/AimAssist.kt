package com.dov.cm.modules.combat

import com.dov.cm.config.Config
import com.dov.cm.event.EventBus
import com.dov.cm.event.EventListener
import com.dov.cm.event.Render2DEvent
import com.dov.cm.managers.AntiBotManager
import com.dov.cm.modules.UChat
import com.dov.cm.util.RandomUtil
import com.dov.cm.util.Rotation
import com.dov.cm.util.RotationUtil
import com.dov.cm.util.TimerUtil
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.Entity
import net.minecraft.entity.decoration.EndCrystalEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.AxeItem
import net.minecraft.item.MaceItem
import net.minecraft.item.SwordItem
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext
import kotlin.math.sqrt

class AimAssist(private val eventBus: EventBus) {

    private var playerTarget = Config.aimAssistTargetPlayers
    private var crystalTarget = Config.aimAssistTargetCrystals
    private var mode = Config.aimAssistMode
    private var visibleTime = Config.aimAssistVisibleTime
    private var stopOnEdge = Config.stopOnEdge
    private var instantTarget = Config.aimAssistInstantTarget

    private var smoothing: Float = Config.aimAssistSmoothing
    private var fov = Config.aimAssistFOV
    private var range = Config.aimAssistRange
    private var random = Config.aimAssistRandom
    private var hitbox = Config.aimAssistHitbox
    private var weaponOnly = Config.aimAssistWeaponOnly
    private var oneTarget = Config.aimAssistStickyTarget
    private var randomTimer: TimerUtil = TimerUtil()
    private var visibleTimer: TimerUtil = TimerUtil()
    private var randomValue: Float = 0f
    var target: Entity? = null
    private var antiBotManager = AntiBotManager(eventBus)
    private var debugActive = true
    private var lastDebugMessage: String = ""

    val mc: MinecraftClient = MinecraftClient.getInstance()

    init {
        // Register immediately in constructor
        println("[AimAssist] Initializing and registering with eventBus: $eventBus")
        eventBus.registerListener(this)
        println("[AimAssist] Registered methods should include 'event'")
    }

    @EventListener
    fun event(event: Render2DEvent) {
        // ===== DEBUGGING =====
        println("[AimAssist] Event received! Aim assist enabled: ${Config.aimAssistEnabled}")

        // Check if aim assist is enabled
        if (!Config.aimAssistEnabled) {
            debug("Aim assist disabled")
            return
        }

        if (mc.currentScreen != null) {
            debug("Screen is open")
            return
        }

        if (!mc.isWindowFocused) {
            debug("Window not focused")
            return
        }

        if (mc.crosshairTarget?.type == HitResult.Type.ENTITY) {
            debug("Already targeting entity")
            return
        }

        if (weaponOnly) {
            val mainItem = mc.player?.mainHandStack?.item
            debug("Weapon check: ${mainItem?.javaClass?.simpleName}")

            if (mainItem !is SwordItem && mainItem !is AxeItem && mainItem !is MaceItem) {
                debug("Not holding weapon")
                return
            }
        }

        val rotation = Rotation(mc.player!!.yaw, mc.player!!.pitch)
        debug("Current rotation: ${rotation.yaw}, ${rotation.pitch}")

        // Target validation
        if (target != null) {
            val vec3d = target!!.pos
            val distance = sqrt(mc.player!!.squaredDistanceTo(vec3d.x, vec3d.y, vec3d.z))
            debug("Current target: ${target?.javaClass?.simpleName}, distance: $distance, range: $range")

            if (distance > range) {
                debug("Target out of range")
                target = null
            }

            val neededRotation = RotationUtil.INSTANCE.getNeededRotations(
                vec3d.x.toFloat(),
                (vec3d.y - getHeight(target!!.height)).toFloat(),
                vec3d.z.toFloat()
            )

            val inFovRange = RotationUtil.INSTANCE.inRange(rotation, neededRotation, fov.toFloat())
            debug("In FOV range: $inFovRange")

            if (!inFovRange) {
                debug("Target out of FOV")
                target = null
            }
        }

        // Find target if needed
        if (!oneTarget || target == null) {
            debug("Finding new target")
            target = getTarget(rotation)
            debug("Found target: ${target?.javaClass?.simpleName}")
        }

        if (target == null) {
            debug("No valid target")
            visibleTimer.reset()
            return
        }

        // Check line of sight
        val vec3d = target!!.eyePos
        val d = vec3d.y - getHeight(target!!.height)
        val raycast = mc.world!!.raycast(
            RaycastContext(
                mc.player!!.getCameraPosVec(mc.renderTickCounter.getTickDelta(true)),
                Vec3d(vec3d.x, d, vec3d.z),
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.ANY,
                mc.player
            )
        )

        if (raycast.type == HitResult.Type.BLOCK) {
            debug("Target obscured by block")
            visibleTimer.reset()
            return
        }

        // Check visibility timer
        val visTimerOk = randomTimer.delay(visibleTime.toFloat())
        debug("Visibility timer check: $visTimerOk")

        if (!visTimerOk) {
            return
        }

        // Get needed rotation
        val rotation3 = RotationUtil.INSTANCE.getNeededRotations(vec3d.x.toFloat(), d.toFloat(), vec3d.z.toFloat())
        debug("Target rotation: ${rotation3.yaw}, ${rotation3.pitch}")

        // Calculate randomness
        if (randomTimer.delay(1000 * smoothing)) {
            randomValue = if (random > 0) {
                -(random / 2) + RandomUtil.INSTANCE.random.nextFloat() * random
            } else {
                0f
            }
            randomTimer.reset()
            debug("New random value: $randomValue")
        }

        // Apply rotation
        debug("Applying rotation with mode: $mode")

        when (mode) {
            2 -> { // Vertical only
                val success = RotationUtil.INSTANCE.setPitch(rotation3, smoothing, randomValue, 2f)
                debug("Applied vertical rotation: $success")
            }
            1 -> { // Horizontal only
                debug("Horizontal mode not implemented correctly")
            }
            0 -> { // Both
                val success = RotationUtil.INSTANCE.setRotation(rotation3, smoothing, randomValue, 2f)
                debug("Applied full rotation: $success")
            }
        }
    }

    private fun getTarget(rotation: Rotation): Entity? {
        var entity: Entity? = null
        var closestDistance = Double.MAX_VALUE
        var entitiesChecked = 0
        var potentialTargets = 0

        mc.world?.entities?.forEach { entity2 ->
            entitiesChecked++
            if (isEntityValid(entity2) && antiBotManager.isNotBot(entity2)) {
                potentialTargets++
                val vec3d = entity2.eyePos
                val d2 = vec3d.y - getHeight(entity2.height)
                val d3 = mc.player!!.squaredDistanceTo(vec3d.x, d2, vec3d.z)
                val actualDistance = sqrt(mc.player!!.squaredDistanceTo(vec3d.x, vec3d.y, vec3d.z))

                if (d3 < closestDistance &&
                    actualDistance <= range &&
                    RotationUtil.INSTANCE.inRange(
                        rotation,
                        RotationUtil.INSTANCE.getNeededRotations(vec3d.x.toFloat(), d2.toFloat(), vec3d.z.toFloat()),
                        fov.toFloat()
                    )
                ) {
                    entity = entity2
                    closestDistance = d3
                }
            }
        }

        debug("Checked $entitiesChecked entities, found $potentialTargets valid targets")
        return entity
    }

    private fun isEntityValid(entity: Entity): Boolean {
        // Don't target self
        if (entity == mc.player) {
            return false
        }

        // Check entity types
        when (entity) {
            is PlayerEntity -> return playerTarget
            is EndCrystalEntity -> return crystalTarget
            else -> return Config.aimAssistTargetEntities
        }
    }

    private fun getHeight(f: Float): Float {
        return when (hitbox) {
            0 -> 0f         // Eye level
            1 -> f / 2      // Center
            else -> f       // Bottom
        }
    }

    private fun debug(message: String) {
        if (debugActive && message != lastDebugMessage) {
            println("[AimAssist] $message")
            UChat.mChat("[AimAssist] $message")
            lastDebugMessage = message
        }
    }

    fun getTargetName(): String {
        return when (target) {
            null -> ""
            is PlayerEntity -> target!!.name.toString()
            else -> "Crystal"
        }
    }

    companion object {
        private var INSTANCE: AimAssist? = null

        fun init(eventBus: EventBus) {
            if (INSTANCE == null) {
                INSTANCE = AimAssist(eventBus)
            }
        }

        fun getInstance(): AimAssist {
            return INSTANCE ?: throw IllegalStateException("AimAssist not initialized. Call init() first.")
        }
    }
}