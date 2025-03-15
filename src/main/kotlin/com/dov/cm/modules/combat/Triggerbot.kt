package com.dov.cm.modules.combat

import com.dov.cm.config.Config
import com.dov.cm.modules.UChat
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.AxeItem
import net.minecraft.item.MaceItem
import net.minecraft.item.SwordItem
import net.minecraft.util.Hand
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.util.hit.HitResult

/**
 * Triggerbot module - Automatically attacks targets in crosshair
 */
object Triggerbot {
    private val mc: MinecraftClient = MinecraftClient.getInstance()
    private var targetAcquiredTime: Long = 0
    private var lastAttackTime: Long = 0

    /**
     * Initialize the Triggerbot module
     */
    fun init() {
        // Register tick event to handle triggerbot logic
        ClientTickEvents.END_CLIENT_TICK.register { _ ->
            if (Config.triggerbotEnabled) {
                try {
                    onTick()
                } catch (e: Exception) {
                    // Prevent crashes
                    UChat.mChat("§cTriggerbot error: ${e.message}")
                }
            }
        }

        UChat.mChat("Triggerbot module initialized")
    }

    /**
     * Main tick handler for triggerbot
     */
    private fun onTick() {
        val player = mc.player ?: return
        mc.world ?: return

        // Check weapon-only condition
        if (Config.triggerbotWeaponOnly && !isHoldingWeapon()) {
            return
        }

        // Get current crosshair target
        val crosshairTarget = mc.crosshairTarget

        // Only proceed if targeting an entity
        if (crosshairTarget?.type != HitResult.Type.ENTITY) {
            targetAcquiredTime = 0
            return
        }

        // Cast to entity hit result
        val entityHit = crosshairTarget as EntityHitResult
        val targetEntity = entityHit.entity

        // Validate target
        if (!isValidTarget(targetEntity)) {
            targetAcquiredTime = 0
            return
        }

        // Track when target was first acquired
        val currentTime = System.currentTimeMillis()
        if (targetAcquiredTime == 0L) {
            targetAcquiredTime = currentTime
        }

        // Check critical hit conditions if enabled
        if (Config.triggerbotCritical && !isCriticalCondition()) {
            return
        }

        // Check delay and weapon cooldown
        if (currentTime - targetAcquiredTime >= Config.triggerbotDelay &&
            isWeaponReady(player)) {
            // Attack if not on cooldown
            attackTarget(player, targetEntity)
            lastAttackTime = currentTime
        }
    }

    /**
     * Check if the weapon is ready to attack
     * Uses attackCooldownProgressPerTick to determine weapon readiness
     */
    private fun isWeaponReady(player: ClientPlayerEntity): Boolean {
        // Get the item's attack cooldown progress per tick
        // This will be 1.0 when fully ready to attack
        val cooldownProgress = player.getAttackCooldownProgress(1f)
        //UChat.mChat(cooldownProgress.toString())
        // Return true if cooldown is complete (progress is 1 or very close to 1)
        return cooldownProgress >= 0.99f
    }

    /**
     * Check if the player is in a critical hit condition
     */
    private fun isCriticalCondition(): Boolean {
        val player = mc.player ?: return false

        // Critical conditions: falling, jumping just began, not on ground
        return !player.isOnGround && player.velocity.y < 0
    }

    /**
     * Check if the target is valid for attack
     */
    private fun isValidTarget(entity: Entity?): Boolean {
        // Null check
        if (entity == null) return false

        // Can't attack self
        if (entity == mc.player) return false

        // Check if entity is living and alive
        if (entity !is LivingEntity || !entity.isAlive) return false

        // Additional checks for specific entity types
        if (entity is PlayerEntity) {
            // Optionally prevent attacking other players if needed
            return true
        }

        return true
    }

    /**
     * Perform the attack on the target
     */
    @OptIn(DelicateCoroutinesApi::class)
    private fun attackTarget(player: ClientPlayerEntity, target: Entity) {
        try {
            // Global scope coroutine to handle delayed attack
            GlobalScope.launch {
                // Primary attack method
                mc.interactionManager?.attackEntity(player, target)
                player.swingHand(Hand.MAIN_HAND)

                // Log for debugging
                UChat.mChat("§aTriggerbotted: ${target.type.toString().split(".").last()}")
            }
        } catch (e: Exception) {
            UChat.mChat("§cTriggerbot attack error: ${e.message}")
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
                heldItem is MaceItem ||
                heldItem.translationKey.contains("mace")
    }
}