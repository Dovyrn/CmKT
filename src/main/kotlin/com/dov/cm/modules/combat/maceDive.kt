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
import net.minecraft.item.ItemStack
import net.minecraft.screen.slot.SlotActionType


class MaceDive {
    private val mc = MinecraftClient.getInstance()
    private var isFlying = false
    private var attacked = false
    private var initialY = 0.0
    private var elytraSlot = -1
    private var chestplateSlot = -1
    private var maceSlot = -1
    private var fireworkSlot = -1
    private var lastAttackTime = 0L
    private var lastSwapAttempt = 0L
    // Debounce protection
    private var lastKeyPressTime = 0L
    private val DEBOUNCE_TIME = 500L // 500ms (adjust as needed)
    // For tracking active sessions
    private var activeSessionId = 0
    // New configuration for packet swapping
    private var swapPacket = Config.SwapPacket // Default to true for faster swapping

    /**
     * Initialize the Mace Dive module
     */
    fun init() {
        // Module initialization is handled by CmKtClient
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

        // If swapPacket is false, check if we have required items in hotbar before proceeding
        if (!swapPacket) {
            val hasElytra = findItemInHotbar(Items.ELYTRA) != -1 || isWearingElytra()
            val hasChestplate = findChestplateSlot() != -1 || isWearingChestplate()
            val hasWeapon = findMaceSlotLegacy() != -1

            // Need an elytra (or wearing one), a chestplate AND a weapon
            val hasRequiredItems = hasElytra && hasChestplate && hasWeapon

            if (!hasRequiredItems) {
                UChat.mChat("§cMissing required items in hotbar for Mace Dive")
                if (!hasElytra) UChat.mChat("§c- Missing Elytra")
                if (!hasChestplate) UChat.mChat("§c- Missing Chestplate")
                if (!hasWeapon) UChat.mChat("§c- Missing Weapon (Mace/Sword/Axe)")
                return
            }
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
     * Find an item in the hotbar only (legacy method, for fallback use)
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
     * Find a chestplate in the hotbar only (legacy method, for fallback use)
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
     * Finds a specific item in the player's inventory and instantly swaps it to the hotbar using packets.
     *
     * @param itemPredicate A predicate to match the desired item
     * @param hotbarSlot The hotbar slot to swap to (0-8)
     * @return True if the item was found and swapped, false otherwise
     */
    fun findAndSwapToHotbar(itemPredicate: (ItemStack) -> Boolean, hotbarSlot: Int = 0): Boolean {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return false
        val inventory = player.inventory

        // Ensure hotbar slot is valid (0-8)
        val targetHotbarSlot = hotbarSlot.coerceIn(0, 8)

        // Search for the item in the main inventory (excluding hotbar)
        // Main inventory slots are 9-35
        for (i in 9 until 36) {
            val stack = inventory.getStack(i)
            if (!stack.isEmpty && itemPredicate(stack)) {
                // Found the item, now swap it using a packet
                val screenHandler = player.currentScreenHandler
                client.interactionManager?.clickSlot(
                    screenHandler.syncId,      // Sync ID of current screen handler
                    i,                         // Slot where the item was found
                    targetHotbarSlot,          // Hotbar slot to swap with
                    SlotActionType.SWAP,       // SWAP action type for hotbar swap
                    player                     // The player
                )

                return true
            }
        }

        return false // Item not found
    }

    /**
     * Enhanced item switching that respects the swapPacket flag
     * @param predicate Function to identify the desired item
     * @return Slot number in hotbar, or -1 if not found
     */
    private fun findAndEquipItem(predicate: (ItemStack) -> Boolean): Int {
        val player = mc.player ?: return -1
        val currentSlot = player.inventory.selectedSlot

        // First check if the item is already in the hotbar
        for (i in 0..8) {
            val stack = player.inventory.getStack(i)
            if (!stack.isEmpty && predicate(stack)) {
                return i // Item found in hotbar
            }
        }

        // If swapPacket is true, try to swap from inventory using packets
        if (swapPacket) {
            if (findAndSwapToHotbar(predicate, currentSlot)) {
                UChat.mChat("§aSwapped item to current slot using packet")
                return currentSlot
            }
        } else {
            // If swapPacket is false, we should NOT look for items in the inventory
            // Only return hotbar items in this case
            UChat.mChat("§6Packet swapping disabled, only checking hotbar")
        }

        return -1 // Item not found in accessible inventory slots
    }

    /**
     * Find an axe in the hotbar
     */
    private fun findAxeSlot(): Int {
        return findAndEquipItem { it.item is AxeItem }
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

            // Get current slot so we can return to it later
            val currentSlot = player.inventory.selectedSlot

            // Check current equipment state
            val isWearingElytraAlready = isWearingElytra()

            // Handle elytra equipment if needed
            if (!isWearingElytraAlready && Config.autoEquipElytra) {
                if (swapPacket) {
                    // Use enhanced method with packet swapping
                    elytraSlot = findAndEquipItem { it.item == Items.ELYTRA }
                } else {
                    // Legacy hotbar-only method when packet swapping is disabled
                    elytraSlot = findItemInHotbar(Items.ELYTRA)
                }

                if (elytraSlot == -1) {
                    if (swapPacket) {
                        UChat.mChat("§cNo elytra found in inventory!")
                    } else {
                        UChat.mChat("§cNo elytra found in hotbar!")
                    }
                    return@launch
                }

                // Switch to elytra slot if different from current
                if (elytraSlot != player.inventory.selectedSlot) {
                    player.inventory.selectedSlot = elytraSlot
                }

                // Equip elytra
                interactionManager.interactItem(player, Hand.MAIN_HAND)
                delay(100) // Small delay for equipping

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
                player.inventory.selectedSlot = currentSlot
                return@launch
            }

            // Wait to fly
            delay(50)

            // Use firework to boost
            useFirework()

            // Return to original slot if we changed it
            if (player.inventory.selectedSlot != currentSlot) {
                player.inventory.selectedSlot = currentSlot
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

            // Check current equipment state
            val isWearingElytraAlready = isWearingElytra()
            val currentSlot = player.inventory.selectedSlot

            // Handle elytra equipment if needed
            if (!isWearingElytraAlready && Config.autoEquipElytra) {
                // Find elytra based on swapPacket setting
                if (swapPacket) {
                    elytraSlot = findAndEquipItem { it.item == Items.ELYTRA }
                } else {
                    elytraSlot = findItemInHotbar(Items.ELYTRA)
                }

                if (elytraSlot == -1) {
                    if (swapPacket) {
                        UChat.mChat("§cNo elytra found in inventory!")
                    } else {
                        UChat.mChat("§cNo elytra found in hotbar!")
                    }
                    return@launch
                }

                // Switch to elytra slot if needed
                if (elytraSlot != player.inventory.selectedSlot) {
                    player.inventory.selectedSlot = elytraSlot
                }

                interactionManager.interactItem(player, Hand.MAIN_HAND)
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
                player.inventory.selectedSlot = currentSlot
                return@launch
            }

            // Use firework to boost
            useFirework()

            // Return to original slot if we changed it
            if (player.inventory.selectedSlot != currentSlot) {
                player.inventory.selectedSlot = currentSlot
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
                delay(50) // Check every 50ms for more precision

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

                // Check if we've reached max height or started falling
                val reachedMaxHeight = currentY >= initialY + Config.maxHeight
                val reachedApex = verticalVelocity < 0.05 && currentY > initialY + 10 // We've peaked and started descending

                // Start monitoring for ground proximity when we reach max height or start falling from sufficient height
                if (reachedMaxHeight || reachedApex) {
                    UChat.mChat("§6Beginning descent from height ${(maxAltitudeReached - initialY).toInt()} blocks")

                    // Prepare chestplate in advance
                    if (Config.autoSwapChestplate && isWearingElytra()) {
                        // Find chestplate based on swapPacket setting
                        if (swapPacket) {
                            chestplateSlot = findAndEquipItem { stack ->
                                val item = stack.item
                                item == Items.NETHERITE_CHESTPLATE ||
                                        item == Items.DIAMOND_CHESTPLATE ||
                                        item == Items.IRON_CHESTPLATE ||
                                        item == Items.GOLDEN_CHESTPLATE ||
                                        item == Items.CHAINMAIL_CHESTPLATE ||
                                        item == Items.LEATHER_CHESTPLATE
                            }
                        } else {
                            chestplateSlot = findChestplateSlot() // Legacy hotbar-only method
                            if (chestplateSlot == -1) {
                                UChat.mChat("§cNo chestplate found in hotbar!")
                                UChat.mChat("§cContinuing without chestplate...")
                            }
                        }

                        if (chestplateSlot != -1) {
                            player.inventory.selectedSlot = chestplateSlot
                            mc.interactionManager?.interactItem(player, Hand.MAIN_HAND)
                            UChat.mChat("§6Swapping elytra for chestplate...")
                        } else if (swapPacket) {
                            UChat.mChat("§cNo chestplate found in inventory!")
                        }
                    }

                    monitorGroundProximity(Config.groundDetectionHeight.toDouble()) // Start monitoring for ground proximity
                    break
                }

                // Also check for ground proximity directly in case we miss the apex
                // Only do this once we're not in initial ascent phase and have gained some height
                if (!initialAltitudeCheck && isNearGround(Config.groundDetectionHeight.toDouble())) {
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

            // Swap to chestplate if enabled and we're wearing elytra
            if (Config.autoSwapChestplate && isWearingElytra()) {
                // Find chestplate based on swapPacket setting
                if (swapPacket) {
                    chestplateSlot = findAndEquipItem { stack ->
                        val item = stack.item
                        item == Items.NETHERITE_CHESTPLATE ||
                                item == Items.DIAMOND_CHESTPLATE ||
                                item == Items.IRON_CHESTPLATE ||
                                item == Items.GOLDEN_CHESTPLATE ||
                                item == Items.CHAINMAIL_CHESTPLATE ||
                                item == Items.LEATHER_CHESTPLATE
                    }
                } else {
                    chestplateSlot = findChestplateSlot() // Legacy hotbar-only method
                }

                if (chestplateSlot != -1) {
                    // Record the time of this attempt
                    lastSwapAttempt = System.currentTimeMillis()

                    UChat.mChat("§6Swapping to chestplate...")
                    player.inventory.selectedSlot = chestplateSlot

                    // Try multiple times to ensure it works
                    for (attempt in 1..3) {
                        interactionManager.interactItem(player, Hand.MAIN_HAND)
                        delay(25)
                    }
                } else {
                    if (swapPacket) {
                        UChat.mChat("§cNo chestplate found in inventory!")
                    } else {
                        UChat.mChat("§cNo chestplate found in hotbar!")
                        UChat.mChat("§cContinuing without chestplate...")
                    }
                }
            }

            // Find and swap to mace using the appropriate method
            if (swapPacket) {
                maceSlot = findMaceSlotEnhanced()
            } else {
                // Legacy method for finding mace in hotbar only
                maceSlot = findMaceSlotLegacy()
            }

            if (maceSlot != -1) {
                UChat.mChat("§6Equipping mace...")
                player.inventory.selectedSlot = maceSlot
            } else {
                if (swapPacket) {
                    UChat.mChat("§cNo mace or weapon found in inventory!")
                } else {
                    UChat.mChat("§cNo mace or weapon found in hotbar!")
                    UChat.mChat("§cTerminating attack sequence due to missing weapon")
                    isFlying = false
                    return@launch
                }
            }

            delay(50)

            // Find and swap to mace using new method
            maceSlot = findMaceSlotEnhanced()

            if (maceSlot != -1) {
                UChat.mChat("§6Equipping mace...")
                player.inventory.selectedSlot = maceSlot
            } else {
                UChat.mChat("§cNo mace or weapon found in inventory!")
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
     * Use firework for boost - respects swapPacket setting
     */
    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun useFirework() {
        val player = mc.player ?: return
        val interactionManager = mc.interactionManager ?: return
        val currentSlot = player.inventory.selectedSlot

        // Find firework based on swapPacket setting
        fireworkSlot = if (swapPacket) {
            findAndEquipItem { it.item == Items.FIREWORK_ROCKET }
        } else {
            findItemInHotbar(Items.FIREWORK_ROCKET)
        }

        if (fireworkSlot != -1) {
            // Check if we need to switch slots
            if (fireworkSlot != player.inventory.selectedSlot) {
                player.inventory.selectedSlot = fireworkSlot
            }

            // Use firework based on boost strength
            for (i in 1..Config.boostStrength) {
                interactionManager.interactItem(player, Hand.MAIN_HAND)
                delay(50) // Small delay between fireworks
            }

            // Switch back to original slot
            player.inventory.selectedSlot = currentSlot
        } else {
            if (swapPacket) {
                UChat.mChat("§cNo fireworks found in inventory!")
            } else {
                UChat.mChat("§cNo fireworks found in hotbar!")
            }
            // If swapPacket is false and we didn't find fireworks in hotbar, exit the launch sequence
            if (!swapPacket) {
                UChat.mChat("§cTerminating launch sequence due to missing firework rockets")
                isFlying = false
                return
            }
        }
    }

    /**
     * Legacy version of findMaceSlot that only checks the hotbar
     */
    private fun findMaceSlotLegacy(): Int {
        val player = mc.player ?: return -1

        // First priority: Find actual mace item
        for (i in 0..8) {
            val stack = player.inventory.getStack(i)
            if (stack.item.translationKey.contains("mace")) {
                return i
            }
        }

        // Second priority: Find any sword
        for (i in 0..8) {
            val stack = player.inventory.getStack(i)
            if (stack.item is SwordItem) {
                return i
            }
        }

        // Third priority: Find any axe
        for (i in 0..8) {
            val stack = player.inventory.getStack(i)
            if (stack.item is AxeItem) {
                return i
            }
        }

        return -1
    }

    /**
     * Enhanced version of findMaceSlot that uses packet swapping
     */
    private fun findMaceSlotEnhanced(): Int {
        // This method only uses inventory-wide search with packet swapping

        // First priority: Find actual mace item
        var slot = findAndEquipItem { stack ->
            stack.item.translationKey.contains("mace")
        }

        if (slot != -1) return slot

        // Second priority: Find any sword
        slot = findAndEquipItem { stack ->
            stack.item is SwordItem
        }

        if (slot != -1) return slot

        // Third priority: Find any axe
        slot = findAndEquipItem { stack ->
            stack.item is AxeItem
        }

        return slot
    }
}