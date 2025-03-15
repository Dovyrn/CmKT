package com.dov.cm.commands

import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents
import net.minecraft.client.MinecraftClient
import com.dov.cm.config.Config
import com.dov.cm.util.ModSounds
import kotlin.concurrent.thread


object CommandHandler {
    fun onInitializeClient() {
        ClientSendMessageEvents.ALLOW_CHAT.register { message ->
            if (message.startsWith(".")) {
                handleCommand(message.substring(1))
                false
            } else {
                true
            }
        }
    }

    private fun handleCommand(command: String) {
        val args = command.split(" ")
        val initCommand = args[0].lowercase()

        if (initCommand == "gui") {
            thread {
                Thread.sleep(1) // Run in a separate thread to avoid freezing the game
                println("After sleep")


                MinecraftClient.getInstance().execute {
                    MinecraftClient.getInstance().setScreen(Config.gui())
                }
            }
        } else if (initCommand == "test"){
            Config.sprint = false
        } else if (initCommand == "inf" && Config.killauraInfMode){
            val player = MinecraftClient.getInstance().player
            player?.playSound(ModSounds.LOVE_IS_WAR, 1.0f, 1.0f)
        }
    }
}
