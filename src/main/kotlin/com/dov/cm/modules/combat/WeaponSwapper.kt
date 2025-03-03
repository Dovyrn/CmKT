package com.dov.cm.modules.combat

import com.dov.cm.config.Config
import com.dov.cm.modules.UChat
import kotlinx.coroutines.DelicateCoroutinesApi
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.Entity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.util.Hand
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * Swaps weapons and attacks with proper cancellation
 */
object WeaponSwapper {
    private val mc: MinecraftClient = MinecraftClient.getInstance()
    private var isSwapping = false
    private var lastAttackTime = 0L
    private var pendingTarget: Entity? = null

    fun init() {
        // Register tick event
        ClientTickEvents.END_CLIENT_TICK.register {
            // Skip if weapon swapper is disabled
            if (!Config.weaponSwapper) return@register

            // Skip if we're already in the process of swapping
            if (isSwapping) return@register
        }

        // Log initialization
        UChat.mChat("Weapon Swapper module initialized")
    }

    /**
     * Check if an attack should be canceled
     * This is called from the mixin
     */
    fun shouldCancelAttack(target: Entity): Boolean {
        // Skip if weapon swapper is disabled
        if (!Config.weaponSwapper) return false

        // Skip if we're already in the process of swapping
        if (isSwapping) return false

        // Don't trigger too frequently
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAttackTime < 250) return false

        // Check if we're holding the first weapon type
        if (isHoldingWeaponType(Config.firstWeapon)) {
            // Mark that we should handle this attack
            lastAttackTime = currentTime
            pendingTarget = target

            // Start weapon swap sequence
            performWeaponSwap(target)

            // Cancel the original attack
            return true
        }

        // Don't cancel if not holding the first weapon type
        return false
    }

    /**
     * Check if the player is holding the specified weapon type
     */
    private fun isHoldingWeaponType(weaponType: Int): Boolean {
        val player = mc.player ?: return false
        val heldItem = player.mainHandStack

        return when (weaponType) {
            0 -> isSword(heldItem) // Sword
            1 -> isAxe(heldItem)   // Axe
            2 -> isMace(heldItem)  // Mace
            else -> false
        }
    }

    /**
     * Find the best weapon of the given type in the hotbar
     */
    private fun findWeaponInHotbar(weaponType: Int): Int {
        val player = mc.player ?: return -1

        for (i in 0..8) {
            val stack = player.inventory.getStack(i)
            val isMatchingType = when (weaponType) {
                0 -> isSword(stack) // Sword
                1 -> isAxe(stack)   // Axe
                2 -> isMace(stack)  // Mace
                else -> false
            }

            if (isMatchingType) {
                return i
            }
        }

        return -1 // Not found
    }

    /**
     * Perform the weapon swap and attack sequence
     */
    @OptIn(DelicateCoroutinesApi::class)
    private fun performWeaponSwap(target: Entity) {
        val player = mc.player ?: return
        val interactionManager = mc.interactionManager ?: return

        // Only proceed if we can find the second weapon
        val secondWeaponSlot = findWeaponInHotbar(Config.secondWeapon)
        if (secondWeaponSlot == -1) {
            UChat.mChat("§cCouldn't find second weapon in hotbar")
            return
        }

        // Remember the original slot
        val originalSlot = player.inventory.selectedSlot

        // Start the swapping process
        isSwapping = true

        GlobalScope.launch {
            try {


                // Swap to second weapon
                mc.execute {
                    player.inventory.selectedSlot = secondWeaponSlot
                }



                // Attack with second weapon
                mc.execute {
                    if (target.isAlive) {
                        interactionManager.attackEntity(player, target)
                        player.swingHand(Hand.MAIN_HAND)
                    }
                }

                // Wait for animation
                delay(150)

                // Swap back to original slot if swap back is enabled
                if (Config.weaponSwapBack) {
                    mc.execute {
                        player.inventory.selectedSlot = originalSlot
                    }
                }

            } finally {
                // Make sure we reset the swapping state
                mc.execute {
                    isSwapping = false
                    pendingTarget = null
                }
            }
        }
    }

    /**
     * Check if an item is a sword
     */
    private fun isSword(stack: ItemStack): Boolean {
        val item = stack.item
        return item == Items.WOODEN_SWORD ||
                item == Items.STONE_SWORD ||
                item == Items.IRON_SWORD ||
                item == Items.GOLDEN_SWORD ||
                item == Items.DIAMOND_SWORD ||
                item == Items.NETHERITE_SWORD
    }

    /**
     * Check if an item is an axe
     */
    private fun isAxe(stack: ItemStack): Boolean {
        val item = stack.item
        return item == Items.WOODEN_AXE ||
                item == Items.STONE_AXE ||
                item == Items.IRON_AXE ||
                item == Items.GOLDEN_AXE ||
                item == Items.DIAMOND_AXE ||
                item == Items.NETHERITE_AXE
    }

    /**
     * Check if an item is a mace (checking name)
     */
    private fun isMace(stack: ItemStack): Boolean {
        // For maces in 1.21.4, check the translation key since it's a new item
        return stack.item.translationKey.contains("mace")
    }
}