package com.dov.cm

import com.dov.cm.commands.CommandHandler
import com.dov.cm.config.Config

import com.dov.cm.modules.ModuleManager
import com.dov.cm.modules.combat.*
import com.dov.cm.modules.render.*
import com.dov.cm.modules.utilities.FullBright
import com.dov.cm.modules.utilities.NoJumpDelay
import com.dov.cm.modules.utilities.ToggleSprint
import com.dov.cm.util.ModSounds
import com.dov.cm.web.WebServerManager
import com.logicalgeekboy.logical_zoom.java_event.EventBus
import com.logicalgeekboy.logical_zoom.java_modules.AimAssist
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.util.InputUtil

object CmKtClient : ClientModInitializer {
    // Make the eventBus accessible
    val eventBus = EventBus()



    override fun onInitializeClient() {
        val maceDive = MaceDive()
        println("CmKtClient initialized with EventBus: $eventBus")

        val moduleManager = ModuleManager()

        CommandHandler.onInitializeClient()
        TargetHUD().onInitializeClient()
        maceDive.init()
        EnhancedHitbox.init()
        GlowESP.init()
        WeaponSwapper.init()
        StorageESP.init()
        NoJumpDelay.init()
        FullBright.init()
        ToggleSprint.init()
        Backtrack.init()
        Triggerbot.init()
        WebServerManager.init()
        KillAura.init()
        PlayerESP.init()
        NametagRenderer.init()

        moduleManager.initModules()


        // Register a tick event to check key presses
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            // This is a simplified approach - you'll need to adapt to your needs
            if (client.player != null && Config.maceDiveEnabled && Config.maceDiveKey.isNotEmpty()) {
                val keyCode = Config.maceDiveKey[0].uppercaseChar().code
                if (InputUtil.isKeyPressed(client.window.handle, keyCode)) {
                    maceDive.onKeyPressed(Config.maceDiveKey[0])
                }
            }
        }



        ModSounds.registerSounds()
    }
}