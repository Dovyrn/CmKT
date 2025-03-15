package com.dov.cm.modules.combat

import com.dov.cm.config.Config
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.client.MinecraftClient
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
import net.minecraft.item.SwordItem
import net.minecraft.item.AxeItem
import net.minecraft.util.hit.HitResult
import com.dov.cm.modules.UChat

class MaceDive {
    private val mc = MinecraftClient.getInstance()
    private var isFlying = false
    private var attacked = false
    private var initialY = 0.0
    private var elytraSlot = -1
    private var chestplateSlot = -1
    private var maceSlot = -1
    private var lastAttackTime = 0L
    private var lastSwapAttempt = 0L
    // Debounce protection
    private var lastKeyPressTime = 0L
    private val DEBOUNCE_TIME = 500L // 500ms (adjust as needed)
    // For tracking active sessions
    private var activeSessionId = 0

    /**
     * Initialize the Mace Dive module
     */
    fun init() {
        // Module initialization is handled by CmKtClient
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun swapToHotbarSlot(slot: Int ){
        val slotKey = mc.options.hotbarKeys[slot]
        GlobalScope.launch {
            KeyBinding.setKeyPressed(slotKey.defaultKey, true)
            delay(50)
            KeyBinding.setKeyPressed(slotKey.defaultKey, false)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun use_item(){
        val useKey = mc.options.useKey
        GlobalScope.launch {
            KeyBinding.setKeyPressed(useKey.defaultKey, true)
            delay(50)
            KeyBinding.setKeyPressed(useKey.defaultKey, false)
        }
    }


    /**
     * Simulates a jump by pressing and releasing the jump key
     */
    @OptIn(DelicateCoroutinesApi::class)
    private fun jump() {
        val jumpKey = mc.options.jumpKey
        GlobalScope.launch {
            KeyBinding.setKeyPressed(jumpKey.defaultKey, true)
            delay(50)
            KeyBinding.setKeyPressed(jumpKey.defaultKey, false)
        }
    }

    /**
     * Handle key press events
     */
    fun onKeyPressed(keyChar: Char) {
        // Continue with normal checks first to avoid unnecessary processing
        if (!Config.maceDiveEnabled ||
            (keyChar.toString().uppercase() != Config.maceDiveKey.uppercase())) {
            return
        }

        val player = mc.player ?: return
        if (mc.currentScreen != null) return

        // Check debounce - ignore if pressed too soon after previous press
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastKeyPressTime < DEBOUNCE_TIME) {
            UChat.mChat("§cMace Dive on cooldown (${((DEBOUNCE_TIME - (currentTime - lastKeyPressTime)) / 1000.0).toInt()}s)")
            return
        }

        // Update last key press time
        lastKeyPressTime = currentTime

        // Terminate any existing sessions
        if (isFlying) {
            UChat.mChat("§6Terminating previous Mace Dive session")
            isFlying = false
            // Increment session ID to invalidate any running loops
            activeSessionId++
        }

        UChat.mChat("§aMace Dive activated")

        if ((player.isOnGround || isNearGround(1.5)) && keyChar.toString().uppercase() == Config.maceDiveKey.uppercase()) {
            // Player is on ground - launch sequence
            UChat.mChat("Ground Launch Sequence")
            groundLaunchSequence()
            monitorFlight()
        }  else {
            // Player is in air - mid-air launch sequence
            UChat.mChat("Air Launch Sequence")
            airLaunchSequence()
        }
    }



    /**
     * Find a sword in the hotbar
     */


    /**
     * Find an axe in the hotbar
     */
    private fun findAxeSlot(): Int {
        val player = mc.player ?: return -1

        for (i in 0..8) {
            val stack = player.inventory.getStack(i)
            if (stack.item is AxeItem) {
                return i
            }
        }
        return -1
    }
    private fun isActivelyBlocking(entity: Entity): Boolean {
        if (entity !is PlayerEntity) return false

        // Check if the player is actively using an item
        if (entity.isUsingItem()) {
            // Get the item they're using
            val activeItem = if (entity.activeHand == Hand.MAIN_HAND) {
                entity.mainHandStack.item
            } else {
                entity.offHandStack.item
            }

            // Check if the item being used is a shield
            if (activeItem == Items.SHIELD) {
                return true
            }
        }

        return false
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
     * Check if the player is wearing elytra
     */
    private fun isWearingElytra(): Boolean {
        val chestStack = mc.player?.getEquippedStack(EquipmentSlot.CHEST) ?: return false
        return chestStack.item == Items.ELYTRA
    }


    /**
     * Launch sequence when player is on ground
     */
    @OptIn(DelicateCoroutinesApi::class)
    private fun groundLaunchSequence() {
        // Capture current session ID to check for termination
        val sessionId = ++activeSessionId

        GlobalScope.launch {
            val player = mc.player ?: return@launch
            val interactionManager = mc.interactionManager ?: return@launch

            // Check if this session has been terminated
            if (sessionId != activeSessionId) {
                UChat.mChat("§cMace Dive session terminated")
                return@launch
            }

            // Find equipment slots
            elytraSlot = findItemInHotbar(Items.ELYTRA)
            chestplateSlot = findChestplateSlot()
            maceSlot = findMaceSlot()

            // Check current equipment state
            val isWearingElytraAlready = isWearingElytra()
            isWearingChestplate()
            val currentSlot = player.inventory.selectedSlot

            // Handle elytra equipment if needed
            if (!isWearingElytraAlready && Config.autoEquipElytra) {
                if (elytraSlot == -1) {
                    UChat.mChat("§cNo elytra found in hotbar!")
                    return@launch
                }

                // Switch to elytra slot and equip
                swapToHotbarSlot(elytraSlot)
                use_item()

                UChat.mChat("§aEquipped elytra for flight")
            } else if (isWearingElytraAlready) {
                UChat.mChat("§aAlready wearing elytra")
            }
            if (isNearGround(1.5)){
                jump()
                // Wait before second jump
                delay(100)
            }
            // Second jump to activate elytra
            jump()

            // Verify elytra is equipped and active
            if (!isWearingElytra()) {
                UChat.mChat("§cFailed to equip/activate elytra!")
                swapToHotbarSlot(currentSlot)
                return@launch
            }
            // Wait to fly
            delay(50)
            // Use firework to boost
            useFirework()

            // Return to original slot if we changed it
            if (player.inventory.selectedSlot != currentSlot) {
                swapToHotbarSlot(currentSlot)
            }

            isFlying = true
            initialY = player.y

            UChat.mChat("§aLaunched! Monitoring flight...")

            // Start monitoring for fall
        }
    }

    /**
     * Launch sequence when player is already in air
     */
    @OptIn(DelicateCoroutinesApi::class)
    private fun airLaunchSequence() {
        // Capture current session ID to check for termination
        val sessionId = ++activeSessionId

        GlobalScope.launch {
            val player = mc.player ?: return@launch
            val interactionManager = mc.interactionManager ?: return@launch

            // Check if this session has been terminated
            if (sessionId != activeSessionId) {
                UChat.mChat("§cMace Dive session terminated")
                return@launch
            }

            // Find equipment slots
            elytraSlot = findItemInHotbar(Items.ELYTRA)
            chestplateSlot = findChestplateSlot()
            maceSlot = findMaceSlot()

            // Check current equipment state
            val isWearingElytraAlready = isWearingElytra()
            val currentSlot = player.inventory.selectedSlot

            // Handle elytra equipment if needed
            if (!isWearingElytraAlready && Config.autoEquipElytra) {
                if (elytraSlot == -1) {
                    UChat.mChat("§cNo elytra found in hotbar!")
                    return@launch
                }

                // Switch to elytra slot and equip
                swapToHotbarSlot(elytraSlot)
                use_item()
                delay(100) // Give time for equipping

                UChat.mChat("§aEquipped elytra for flight")
            } else if (isWearingElytraAlready) {
                UChat.mChat("§aAlready wearing elytra")
            }

            // Jump to activate elytra
            jump()

            delay(100) // Wait for elytra activation

            // Verify elytra is equipped
            if (!isWearingElytra()) {
                UChat.mChat("§cFailed to equip/activate elytra!")
                swapToHotbarSlot(elytraSlot)
                return@launch
            }

            // Use firework to boost
            useFirework()

            // Return to original slot if we changed it
            if (player.inventory.selectedSlot != currentSlot) {
                swapToHotbarSlot(currentSlot)
            }

            isFlying = true
            initialY = player.y

            UChat.mChat("§aLaunched! Monitoring ground proximity...")

            // Start monitoring for ground proximity immediately
            monitorGroundProximity(Config.maxHeight.toDouble())
        }
    }

    /**
     * Monitor flight to detect when to start diving
     */
    @OptIn(DelicateCoroutinesApi::class)
    private fun monitorFlight() {
        // Capture current session ID to check for termination
        val sessionId = activeSessionId

        GlobalScope.launch {
            var lastY = mc.player?.y ?: return@launch
            var maxAltitudeReached = initialY
            var initialAltitudeCheck = true

            // Add a short delay to allow player to gain some altitude first
            delay(400)

            // Check if this session has been terminated during the delay
            if (sessionId != activeSessionId || !isFlying) {
                UChat.mChat("§cMace Dive flight monitoring terminated")
                return@launch
            }

            while (isFlying && sessionId == activeSessionId) {
                delay(10) // Check every 50ms for more precision

                val player = mc.player
                if (player == null) {
                    isFlying = false
                    break
                }

                val currentY = player.y
                val verticalVelocity = currentY - lastY
                lastY = currentY

                // Initial altitude check to ensure we've gained some height before checking for ground
                if (initialAltitudeCheck) {
                    if (currentY > initialY + 5) {
                        // We've gained at least 5 blocks of height, safe to start monitoring ground
                        initialAltitudeCheck = false
                        UChat.mChat("§6Gained altitude, monitoring flight...")
                    } else {
                        // Skip ground checks until we've gained some altitude
                        continue
                    }
                }

                // Track maximum height reached
                if (currentY > maxAltitudeReached) {
                    maxAltitudeReached = currentY
                }

                // Update chestplate slot in case inventory changed
                chestplateSlot = findChestplateSlot()

                // Check if we've reached max height or started falling
                val reachedMaxHeight = currentY >= initialY + Config.maxHeight
                val reachedApex = verticalVelocity < 0.05 && currentY > initialY + 10 // We've peaked and started descending

                // Start monitoring for ground proximity when we reach max height or start falling from sufficient height
                if (reachedMaxHeight || reachedApex) {
                    UChat.mChat("§6Beginning descent from height ${(maxAltitudeReached - initialY).toInt()} blocks")

                    // Only swap to chestplate if player is wearing elytra
                    if (Config.autoSwapChestplate && chestplateSlot != -1 && isWearingElytra()) {
                        swapToHotbarSlot(chestplateSlot)
                        use_item()
                        UChat.mChat("§6Swapping elytra for chestplate...")
                    }

                    monitorGroundProximity(Config.groundDetectionHeight.toDouble()) // Start monitoring for ground proximity
                    break
                }

                // Also check for ground proximity directly in case we miss the apex
                // Only do this once we're not in initial ascent phase and have gained some height
                if (isNearGround(Config.groundDetectionHeight.toDouble())) {
                    UChat.mChat("§cGround detected during descent!")
                    prepareForAttack()
                    break
                }
            }
        }
    }

    /**
     * Monitor proximity to ground for dive attack
     */
    @OptIn(DelicateCoroutinesApi::class)
    private fun monitorGroundProximity(height: Double) {
        // Capture current session ID to check for termination
        val sessionId = activeSessionId

        GlobalScope.launch {
            while (isFlying && sessionId == activeSessionId) {
                delay(30) // Check very frequently when preparing to dive

                val player = mc.player
                if (player == null) {
                    isFlying = false
                    break
                }

                // Check if we're close to the ground
                if (isNearGround(height.toDouble())) {
                    UChat.mChat("§6Ground proximity detected! Preparing for attack...")
                    prepareForAttack()
                    break
                }
            }
        }
    }

    /**
     * Prepare for attack by swapping to chestplate and mace
     */
    @OptIn(DelicateCoroutinesApi::class)
    private fun prepareForAttack() {
        if (!isFlying) return
        attacked = false

        // Capture current session ID to check for termination
        val sessionId = activeSessionId

        GlobalScope.launch {
            // Check if this session has been terminated
            if (sessionId != activeSessionId) {
                UChat.mChat("§cMace Dive attack preparation terminated")
                return@launch
            }
            val player = mc.player ?: return@launch
            val interactionManager = mc.interactionManager ?: return@launch

            // Update equipment slots
            chestplateSlot = findChestplateSlot()
            maceSlot = findMaceSlot()

            // Swap to chestplate if enabled and we're wearing elytra
            if (Config.autoSwapChestplate && chestplateSlot != -1 && isWearingElytra()) {
                // Record the time of this attempt
                lastSwapAttempt = System.currentTimeMillis()

                UChat.mChat("§6Swapping to chestplate...")
                swapToHotbarSlot(chestplateSlot)

                // Try multiple times to ensure it works
                for (attempt in 1..3) {
                    use_item()
                    delay(25)
                }
            }

            // Swap to mace if available
            if (maceSlot != -1) {
                UChat.mChat("§6Equipping mace...")
                swapToHotbarSlot(maceSlot)
            }
            delay(50)

            // Attack based on selected mode
            when (Config.attackMode) {
                0 -> UChat.mChat("§aAttack mode: None")
                1 -> {
                    while (!player.isOnGround && !attacked) {
                        delay(10)
                        triggerBotAttack()
                    }
                }
                2 -> {
                    UChat.mChat("§aAttack mode: Silent")

                    while (!player.isOnGround && !attacked){
                        delay(10)
                        silentAttack()
                    }
                }
            }

            // Reset flying state
            isFlying = false
        }
    }

    /**
     * TriggerBot attack implementation - attacks entities when looking at them
     */
    private fun triggerBotAttack() {
        val player = mc.player ?: return
        val interactionManager = mc.interactionManager ?: return
        val world = mc.world ?: return

        // Check if attack cooldown is met
        if (System.currentTimeMillis() - lastAttackTime < 250) {
            return
        }

        // Get entities in a sphere around the player
        val range = 3 // Attack range in blocks
        val targetEntities = ArrayList<Entity>()

        // Get entities in range
        world.getEntitiesByClass(Entity::class.java,
            Box(player.x - range, player.y - range, player.z - range,
                player.x + range, player.y + range, player.z + range)
        ) { entity ->
            entity != player && isValidTarget(entity)
        }.forEach { targetEntities.add(it) }

        // Sort by distance
        targetEntities.sortBy { player.squaredDistanceTo(it) }

        // First try to attack what we're looking at
        if (mc.crosshairTarget?.type == HitResult.Type.ENTITY) {
            val hit = mc.crosshairTarget as EntityHitResult

            val entity = hit.entity

            if (entity != player && isValidTarget(entity)) {
                UChat.mChat("§aAttacking targeted entity: ${entity.type.toString().split(".").last()}")
                interactionManager.attackEntity(player, entity)
                player.swingHand(Hand.MAIN_HAND)
                lastAttackTime = System.currentTimeMillis()
                attacked = true
                return
            }
        }


    }

    /**
     * Check if an entity is a valid target
     */
    private fun isValidTarget(entity: Entity): Boolean {
        // Players
        if (entity is PlayerEntity && entity != mc.player) {
            return true
        }
        if (entity is LivingEntity) {
            return true
        }

        // Add other entity types as needed
        return false
    }

    private fun silentAttack() {
        val player = mc.player ?: return
        val networkHandler = mc.networkHandler ?: return
        val world = mc.world ?: return

        // Get all valid targets in range
        val range = 3.0 // Attack range
        val targets = world.getEntitiesByClass(Entity::class.java,
            Box(player.x - range, player.y - range, player.z - range,
                player.x + range, player.y + range, player.z + range)
        ) { entity ->
            entity != player && isValidTarget(entity)
        }.sortedBy { player.squaredDistanceTo(it) }

        // Attack closest target
        targets.firstOrNull()?.let { target ->
            UChat.mChat("§aSilently attacking: ${target.type.toString().split(".").last()}")
            attacked = true
            // Schedule rendering on the render thread
            targetToRender = target  // Store the target in a class field

            // Send attack packet directly without swinging arm visibly
            val packet = PlayerInteractEntityC2SPacket.attack(
                target,
                player.isSneaking
            )
            networkHandler.sendPacket(packet)
            lastAttackTime = System.currentTimeMillis()
        }
    }

    // Class field to store the target
    private var targetToRender: Entity? = null



    /**
     * Check if player is near ground with improved detection
     */
    private fun isNearGround(height: Double): Boolean {
        val player = mc.player ?: return false
        val world = mc.world ?: return false

        // Improved implementation with multiple checks
        // Check directly below
        for (y in 1..height.toInt()) {
            if (!world.getBlockState(player.blockPos.down(y)).isAir) {
                return true
            }
        }

        // Check slightly forward in movement direction to anticipate landing spot
        val velocity = player.velocity
        val lookAhead = 2.0 // Look ahead blocks
        val lookX = player.x + velocity.x * lookAhead
        val lookZ = player.z + velocity.z * lookAhead

        for (y in 1..height.toInt()) {
            val blockPos = player.blockPos.mutableCopy()
            blockPos.set(lookX.toInt(), blockPos.y - y, lookZ.toInt())
            if (!world.getBlockState(blockPos).isAir) {
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
            swapToHotbarSlot(fireworkSlot)

            // Use firework based on boost strength
            for (i in 1..Config.boostStrength) {
                use_item()
                delay(50) // Small delay between fireworks
            }

            swapToHotbarSlot(currentSlot)
        } else {
            UChat.mChat("§cNo fireworks found in hotbar!")
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
     * Find a mace or other weapon in the hotbar
     */
    private fun findMaceSlot(): Int {
        val player = mc.player ?: return -1

        // First priority: Find actual mace item
        for (i in 0..8) {
            val stack = player.inventory.getStack(i)
            if (stack.item.translationKey.contains("mace")) {
                return i
            }
        }

        // Second priority: Find any sword or axe
        for (i in 0..8) {
            val stack = player.inventory.getStack(i)
            if (stack.item is SwordItem || stack.item is AxeItem) {
                return i
            }
        }

        return -1
    }
}