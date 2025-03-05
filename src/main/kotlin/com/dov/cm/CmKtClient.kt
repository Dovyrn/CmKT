package com.dov.cm

import net.fabricmc.api.ClientModInitializer
import com.dov.cm.commands.CommandHandler
import com.dov.cm.config.Config
import com.dov.cm.modules.combat.*
import com.dov.cm.modules.render.*
import com.dov.cm.modules.combat.Hitbox
import com.dov.cm.modules.render.Chams
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.util.InputUtil
import com.dov.cm.modules.render.StorageESP



object CmKtClient : ClientModInitializer {
    override fun onInitializeClient() {
        val maceDive = MaceDive()
        println("CmKtClient initialized!")  // Debugging log
        CommandHandler.onInitializeClient() // Ensure CommandHandler is loaded
        TargetHUD().onInitializeClient() // Register TargetHUD
        maceDive.init()
        Hitbox.init()
        Chams.init()
        WeaponSwapper.init()
        StorageESP.init()
        RenderHandler.init()

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
    }
}