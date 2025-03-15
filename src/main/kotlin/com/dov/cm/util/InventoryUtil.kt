package com.dov.cm.util

import net.minecraft.block.Block
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.PotionContentsComponent
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.screen.slot.SlotActionType

class InventoryUtil {
    /**
     * Finds all inventory slots containing a specific item.
     *
     * @param item The item to search for in the inventory
     * @param includeHotbar Whether to include hotbar slots (0-8) in the search
     * @return A list of slot IDs containing the specified item
     */
    fun findItemSlot(item: Item, includeHotbar: Boolean): ArrayList<Int> {
        val arrayList = ArrayList<Int>()
        for (i in (if (includeHotbar) 0 else 9)..35) {
            if (mc.player!!.inventory.getStack(i).item === item) {
                arrayList.add(i)
            }
        }
        return arrayList
    }

    fun setCurrentSlot(n: Int) {
        mc.player!!.inventory.selectedSlot = n
    }

    fun isHoldingItem(item: Item): Boolean {
        return mc.player!!.mainHandStack.item === item
    }

    fun findBlockSlot(block: Block): Int? {
        for (i in 0..8) {
            if (mc.player!!.inventory.getStack(i).item === block.asItem()) {
                return i
            }
        }
        return null
    }

    fun findPotion(n: Int, n2: Int, statusEffect: StatusEffect?): Int? {
        for (i in n until n2) {
            val itemStack = mc.player!!.inventory.getStack(i)
            if (itemStack.item === Items.SPLASH_POTION && (statusEffect == null || hasStatusEffect(
                    itemStack,
                    statusEffect
                ))
            ) {
                return i
            }
        }
        return null
    }

    fun hasStatusEffect(itemStack: ItemStack, statusEffect: StatusEffect): Boolean {
        val potionContents = itemStack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT)

        for (effectInstance in potionContents.effects) {
            if (effectInstance.effectType === statusEffect) {
                return true
            }
        }
        return false
    }

    fun performSlotAction(n: Int, n2: Int, slotActionType: SlotActionType?): Boolean {
        if (mc.currentScreen is InventoryScreen) {
            mc.interactionManager!!.clickSlot((mc.currentScreen as InventoryScreen).getScreenHandler().syncId, n, n2, slotActionType, mc.player)
            return true
        }
        return false
    }

    fun isHoldingOffhandItem(item: Item): Boolean {
        return mc.player!!.offHandStack.item === item
    }

    fun findItemInHotbar(item: Item): Int {
        for (i in 0..8) {
            if (mc.player!!.inventory.getStack(i).item === item) {
                return i
            }
        }
        return 0
    }

    val mc: MinecraftClient
        get() = MinecraftClient.getInstance()

    companion object {
        val INSTANCE: InventoryUtil = InventoryUtil()
    }
}