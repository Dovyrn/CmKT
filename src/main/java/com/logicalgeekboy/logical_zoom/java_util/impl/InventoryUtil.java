package com.logicalgeekboy.logical_zoom.java_util.impl;

import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.screen.slot.SlotActionType;
import com.logicalgeekboy.logical_zoom.java_util.MC;
import java.util.ArrayList;

public class InventoryUtil implements MC {

    public static InventoryUtil INSTANCE = new InventoryUtil();

    public ArrayList<Integer> findItemSlot(Item item, boolean bl) {
        ArrayList<Integer> arrayList = new ArrayList<>();
        for (int i = bl ? 0 : 9; i < 36; i++) {
            if (getMc().player.getInventory().getStack(i).getItem() == item) {
                arrayList.add(i);
            }
        }
        return arrayList;
    }

    public void setCurrentSlot(int n) {
        getMc().player.getInventory().selectedSlot = n;
    }

    public boolean isHoldingItem(Item item) {
        return getMc().player.getMainHandStack().getItem() == item;
    }

    public Integer findBlockSlot(Block block) {
        for (int i = 0; i < 9; i++) {
            if (getMc().player.getInventory().getStack(i).getItem() == block.asItem()) {
                return i;
            }
        }
        return null;
    }

    public Integer findPotion(int startSlot, int endSlot, boolean healthPotion) {
        for (int i = startSlot; i < endSlot; i++) {
            ItemStack itemStack = getMc().player.getInventory().getStack(i);

            // Skip if not a splash potion
            if (itemStack.getItem() != Items.SPLASH_POTION) {
                continue;
            }

            // If not looking for health potions specifically, return any splash potion
            if (!healthPotion) {
                return i;
            }

            // Get the potion contents component
            PotionContentsComponent contents = itemStack.getOrDefault(
                    DataComponentTypes.POTION_CONTENTS,
                    PotionContentsComponent.DEFAULT
            );

            // Check each effect for instant health
            for (StatusEffectInstance effect : contents.getEffects()) {
                String effectString = effect.getEffectType().toString();

                // Check if this is an instant health effect based on the reference string
                if (effectString.contains("minecraft:instant_health")) {
                    return i;
                }
            }
        }

        return null;
    }


    private boolean hasStatusEffect(ItemStack itemStack, StatusEffect statusEffect) {
        PotionContentsComponent potionContents = itemStack.getOrDefault(
                DataComponentTypes.POTION_CONTENTS,
                PotionContentsComponent.DEFAULT
        );

        for (StatusEffectInstance effectInstance : potionContents.getEffects()) {
            if (effectInstance.getEffectType() == statusEffect) {
                return true;
            }
        }
        return false;
    }

    public boolean performSlotAction(int n, int n2, SlotActionType slotActionType) {
        if (getMc().currentScreen instanceof InventoryScreen screen) {
            getMc().interactionManager.clickSlot(screen.getScreenHandler().syncId, n, n2, slotActionType, getMc().player);
            return true;
        }
        return true;
    }

    public boolean isHoldingOffhandItem(Item item) {
        return getMc().player.getOffHandStack().getItem() == item;
    }

    public Integer findItemInHotbar(Item item) {
        for (int i = 0; i < 9; i++) {
            if (getMc().player.getInventory().getStack(i).getItem() == item) {
                return i;
            }
        }
        return 0;
    }

    @Override
    public MinecraftClient getMc() {
        return MinecraftClient.getInstance();
    }
}