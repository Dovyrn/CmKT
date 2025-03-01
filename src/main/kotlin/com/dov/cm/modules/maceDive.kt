package com.dov.cm.modules

import com.dov.cm.config.Config
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.util.Hand
import net.minecraft.util.math.Box
import net.minecraft.client.option.KeyBinding
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.entity.Entity
import net.minecraft.entity.mob.HostileEntity

class MaceDive {
    private val mc = MinecraftClient.getInstance()
    private var isFlying = false
    private var initialY = 0.0
    private var elytraSlot = -1
    private var chestplateSlot = -1
    private var maceSlot = -1

    /**
     * Initialize the Mace Dive module
     */
    fun init() {
        // Register event handlers for key pressing
        registerKeyPressHandler()
    }

    /**
     * Register the key press handler
     */
    private fun registerKeyPressHandler() {
        // This is a placeholder - you'll need to integrate this with your mod's event system
        // For example, using Fabric's KeyBinding system or a custom event handler
    }

    /**
     * Simulates a jump by pressing and releasing the jump key
     */
    private fun jump() {
        val jumpKey = mc.options.jumpKey
        GlobalScope.launch {
            KeyBinding.setKeyPressed(jumpKey.defaultKey, true)
            delay(5)
            KeyBinding.setKeyPressed(jumpKey.defaultKey, false)
        }
    }

    /**
     * Handle key press events
     */
    fun onKeyPressed(keyChar: Char) {
        if (!Config.maceDiveEnabled || keyChar.toString().uppercase() != Config.maceDiveKey.uppercase()) {
            return
        }

        val player = mc.player ?: return
        if (mc.currentScreen != null) return

        if (player.isOnGround) {
            // Player is on ground - launch sequence
            groundLaunchSequence()
        } else {
            // Player is in air - mid-air launch sequence
            airLaunchSequence()
        }
    }

    /**
     * Check if the player is wearing a chestplate
     */
    private fun isWearingChestplate(): Boolean {
        val chestStack = mc.player?.getEquippedStack(EquipmentSlot.CHEST) ?: return false
        val item = chestStack.item

        return item == Items.NETHERITE_CHESTPLATE ||
                item == Items.DIAMOND_CHESTPLATE ||
                item == Items.IRON_CHESTPLATE ||
                item == Items.GOLDEN_CHESTPLATE ||
                item == Items.CHAINMAIL_CHESTPLATE ||
                item == Items.LEATHER_CHESTPLATE
    }

    /**
     * Launch sequence when player is on ground
     */
    private fun groundLaunchSequence() {
        GlobalScope.launch {
            val player = mc.player ?: return@launch
            val interactionManager = mc.interactionManager ?: return@launch

            // Find equipment slots
            elytraSlot = findItemInHotbar(Items.ELYTRA)
            chestplateSlot = findChestplateSlot()
            maceSlot = findMaceSlot()

            if (elytraSlot == -1) return@launch

            val currentSlot = player.inventory.selectedSlot

            // Switch to elytra slot and equip
            if (isWearingChestplate()) {
                player.inventory.selectedSlot = elytraSlot
                interactionManager.interactItem(player, Hand.MAIN_HAND)
            }

            // First jump
            jump()

            delay(200)

            // Second jump
            jump()

            // Use firework to boost
            useFirework()

            // Return to original slot
            player.inventory.selectedSlot = currentSlot

            isFlying = true
            initialY = player.y

            // Start monitoring for fall
            monitorFlight()

            //monitorGroundProximity()
        }
    }

    /**
     * Launch sequence when player is already in air
     */
    private fun airLaunchSequence() {
        GlobalScope.launch {
            val player = mc.player ?: return@launch
            val interactionManager = mc.interactionManager ?: return@launch

            // Find equipment slots
            elytraSlot = findItemInHotbar(Items.ELYTRA)
            chestplateSlot = findChestplateSlot()
            maceSlot = findMaceSlot()

            if (elytraSlot == -1) return@launch

            val currentSlot = player.inventory.selectedSlot

            // Switch to elytra slot and equip
            player.inventory.selectedSlot = elytraSlot
            interactionManager.interactItem(player, Hand.MAIN_HAND)

            // Jump to activate elytra
            jump()

            // Use firework to boost
            useFirework()

            // Return to original slot
            player.inventory.selectedSlot = currentSlot

            isFlying = true
            initialY = player.y

            // Start monitoring for ground proximity immediately
            monitorGroundProximity()
        }
    }

    /**
     * Monitor flight to detect when to start diving
     */
    private fun monitorFlight() {
        GlobalScope.launch {
            var lastY = mc.player?.y ?: return@launch

            while (isFlying) {
                delay(50) // Check every 100ms

                val player = mc.player
                if (player == null) {
                    isFlying = false
                    break
                }

                val currentY = player.y
                val verticalVelocity = currentY - lastY
                lastY = currentY

                // Check if we've reached max height or started falling
                val reachedMaxHeight = currentY >= initialY + Config.maxHeight
                val losingMomentum = verticalVelocity < 0.05

                if (reachedMaxHeight) {
                    // Start monitoring for ground proximity
                    if (Config.autoSwapChestplate && chestplateSlot != -1) {
                        player.inventory.selectedSlot = chestplateSlot
                        mc.interactionManager?.interactItem(player, Hand.MAIN_HAND)
                    }
                    break
                }
            }
        }
    }

    /**
     * Monitor proximity to ground for dive attack
     */
    private fun monitorGroundProximity() {
        GlobalScope.launch {
            while (isFlying) {
                //delay(50) // Check more frequently when preparing to dive

                val player = mc.player
                if (player == null) {
                    isFlying = false
                    break
                }

                // Check if we're close to the ground
                if (isNearGround(Config.groundDetectionHeight.toDouble())) {
                    prepareForAttack()
                    break
                }
            }
        }
    }

    /**
     * Prepare for attack by swapping to chestplate and mace
     */
    private fun prepareForAttack() {
        if (!isFlying) return

        GlobalScope.launch {
            val player = mc.player ?: return@launch
            val interactionManager = mc.interactionManager ?: return@launch

            // Swap to chestplate if enabled
            if (Config.autoSwapChestplate && chestplateSlot != -1) {
                player.inventory.selectedSlot = chestplateSlot
                // Remove UChat.mChat call as it's not defined in the provided code
                interactionManager.interactItem(player, Hand.MAIN_HAND)
                //delay(500) // Short delay to ensure equipment change
            }

            // Swap to mace if available
            if (maceSlot != -1) {
                player.inventory.selectedSlot = maceSlot
            }

            // Attack based on selected mode
            when (Config.attackMode) {
                0 -> {} // None - do nothing
                1 -> triggerBotAttack() // TriggerBot
                2 -> silentAttack() // Silent
            }

            // Reset flying state
            isFlying = false
        }
    }

    /**
     * TriggerBot attack implementation - attacks any entity the cursor is over
     */
    /**
     * TriggerBot attack implementation - attacks entities when looking at them
     * Based on a more comprehensive implementation
     */
    private fun triggerBotAttack() {
        val player = mc.player ?: return
        val interactionManager = mc.interactionManager ?: return

        // Don't attack if a GUI screen is open
        if (mc.currentScreen is HandledScreen<*>) {
            return
        }

        // Get the entity the player is looking at
        if (mc.crosshairTarget !is EntityHitResult) {
            return
        }

        val hitResult = mc.crosshairTarget as EntityHitResult
        val entity = hitResult.entity

        // Don't attack self
        if (entity == player) {
            return
        }

        // Check if the entity is a valid target
        if (!isValidTarget(entity)) {
            return
        }

        // Check if the entity is within range (3 blocks)
        if (player.squaredDistanceTo(entity) > 3 * 3) {
            return
        }

        // Attack the entity
        interactionManager.attackEntity(player, entity)
        player.swingHand(Hand.MAIN_HAND)
    }

    private fun isValidTarget(entity: Entity): Boolean {
        // Add your entity filtering logic here
        // Example: Only attack hostile mobs and players
        return when (entity) {
            is HostileEntity -> true
            is PlayerEntity -> entity != mc.player
            else -> false
        }
    }



    /**
     * Silent attack implementation - sends attack packet to nearest player
     */
    private fun silentAttack() {
        val player = mc.player ?: return
        val networkHandler = mc.networkHandler ?: return

        val nearestPlayer = findNearestPlayer()
        if (nearestPlayer != null) {
            // Send attack packet directly without swinging arm visibly
            val packet = PlayerInteractEntityC2SPacket.attack(
                nearestPlayer,
                player.isSneaking
            )
            networkHandler.sendPacket(packet)
        }
    }

    /**
     * Find nearest player within range
     */
    private fun findNearestPlayer(): PlayerEntity? {
        val range = 6.0 // Attack range
        val player = mc.player ?: return null
        val world = mc.world ?: return null

        // Create a box around the player
        val box = Box(
            player.x - range, player.y - range, player.z - range,
            player.x + range, player.y + range, player.z + range
        )

        // Find all players in range
        val nearbyPlayers = world.getEntitiesByClass(
            PlayerEntity::class.java,
            box
        ) { it != player }

        // Find nearest player
        return nearbyPlayers.minByOrNull { it.squaredDistanceTo(player) }
    }

    /**
     * Check if player is near ground with improved detection
     */
    private fun isNearGround(height: Double): Boolean {
        val player = mc.player ?: return false
        val world = mc.world ?: return false

        // Improved implementation using ray tracing
        for (y in 1..height.toInt()) {
            if (!world.getBlockState(player.blockPos.down(y)).isAir) {
                return true
            }
        }
        return false
    }

    /**
     * Use firework for boost
     */
    private suspend fun useFirework() {
        val player = mc.player ?: return
        val interactionManager = mc.interactionManager ?: return

        val fireworkSlot = findItemInHotbar(Items.FIREWORK_ROCKET)
        if (fireworkSlot != -1) {
            val currentSlot = player.inventory.selectedSlot
            player.inventory.selectedSlot = fireworkSlot

            // Use firework based on boost strength
            for (i in 1..Config.boostStrength) {
                interactionManager.interactItem(player, Hand.MAIN_HAND)
                delay(50) // Small delay between fireworks
            }

            player.inventory.selectedSlot = currentSlot
        }
    }

    /**
     * Find an item in the hotbar
     */
    private fun findItemInHotbar(item: net.minecraft.item.Item): Int {
        val player = mc.player ?: return -1

        for (i in 0..8) {
            val stack = player.inventory.getStack(i)
            if (stack.item == item) {
                return i
            }
        }
        return -1
    }

    /**
     * Find a chestplate in the hotbar
     */
    private fun findChestplateSlot(): Int {
        val player = mc.player ?: return -1

        for (i in 0..8) {
            val stack = player.inventory.getStack(i)
            val item = stack.item
            if (item == Items.NETHERITE_CHESTPLATE ||
                item == Items.DIAMOND_CHESTPLATE ||
                item == Items.IRON_CHESTPLATE ||
                item == Items.GOLDEN_CHESTPLATE ||
                item == Items.CHAINMAIL_CHESTPLATE ||
                item == Items.LEATHER_CHESTPLATE) {
                return i
            }
        }
        return -1
    }

    /**
     * Find a mace in the hotbar
     */
    private fun findMaceSlot(): Int {
        val player = mc.player ?: return -1

        for (i in 0..8) {
            val stack = player.inventory.getStack(i)
            if (stack.item.translationKey.contains("mace")) {
                return i
            }
        }
        return -1
    }
}