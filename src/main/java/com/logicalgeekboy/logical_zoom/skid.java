package com.logicalgeekboy.logical_zoom;

import com.logicalgeekboy.logical_zoom.java_event.EventBus;
import com.logicalgeekboy.logical_zoom.java_manaagers.AntiBotManager;
import com.logicalgeekboy.logical_zoom.java_manaagers.RenderManager;

import com.logicalgeekboy.logical_zoom.java_manaagers.*;

import net.fabricmc.api.ModInitializer;

public class skid implements ModInitializer {

    public static Companion Companion = new Companion();
    private static final EventBus eventBus = new EventBus();
    private static final RenderManager renderManager = new RenderManager();
    private static final AntiBotManager antiBotManager = new AntiBotManager();
    private static final RotationManager rotationManager = new RotationManager();
    @Override
    public void onInitialize() {
    }

    public static class Companion {

        public static EventBus getEventBus() {
            return eventBus;
        }

        public static RenderManager getRenderManager() {
            return renderManager;
        }

        public static AntiBotManager getAntiBotManager() {
            return antiBotManager;
        }

        public static RotationManager getRotationManager() {
            return rotationManager;
        }

    }
}
