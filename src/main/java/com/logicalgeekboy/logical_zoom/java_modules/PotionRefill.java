package com.logicalgeekboy.logical_zoom.java_modules;


import com.dov.cm.config.Config;
import com.logicalgeekboy.logical_zoom.java_event.EventListener;
import com.logicalgeekboy.logical_zoom.java_event.impl.Render2DEvent;
import com.logicalgeekboy.logical_zoom.java_util.impl.InventoryUtil;
import com.logicalgeekboy.logical_zoom.java_util.impl.TimerUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

public class PotionRefill  {

    public int delay;
    public boolean healthPot;
    public TimerUtil timer;

    public PotionRefill() {
        delay = 30;
        timer = new TimerUtil();
    }
    private MinecraftClient getMc() {
        return MinecraftClient.getInstance();
    }

    @EventListener
    public void event(Render2DEvent event) {
        // Skip if player or interaction manager is null
        if (getMc().player == null || getMc().interactionManager == null) return;

        // Get configuration from Config
        healthPot = Config.INSTANCE.getPotRefill();

        // Check if player is in inventory
        if (getMc().currentScreen instanceof InventoryScreen screen) {
            // Check for empty slot in hotbar and if timer has passed
            Integer emptySlot = hasEmptySlot();
            if (emptySlot != null && timer.delay(delay)) {
                // We're looking for InstantHealth potions if healthPot is true
                // Find the potion in inventory
                Integer potionSlot = InventoryUtil.INSTANCE.findPotion(9, 35, healthPot);

                // If found, move to hotbar
                if (potionSlot != null) {
                    getMc().interactionManager.clickSlot(
                            screen.getScreenHandler().syncId,
                            potionSlot,
                            0,
                            SlotActionType.QUICK_MOVE,
                            getMc().player
                    );
                    timer.reset();
                }
            }
        } else {
            timer.reset();
        }
    }


    private Integer hasEmptySlot() {
        for (int i = 0; i < 9; ++i) {
            if (getMc().player.getInventory().getStack(i).getItem() == Items.AIR) {
                return i;
            }
        }
        return null;
    }
}